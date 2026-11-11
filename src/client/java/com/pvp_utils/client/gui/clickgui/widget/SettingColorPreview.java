package com.pvp_utils.client.gui.clickgui.widget;

import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.theme.ClickGuiThemeColors;
import com.pvp_utils.client.render.chroma.ChromaDraw;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.types.Rect;
import io.github.humbleui.types.RRect;

import java.awt.Color;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class SettingColorPreview extends SettingWidget {
    private static final float WIDTH = 184f;
    private static final float CONTROL_HEIGHT = 24f;
    private static final float SWATCH_WIDTH = 64f;
    private static final float PICKER_TOP = 36f;
    private static final float PICKER_HEIGHT = 100f;
    private static final float BAR_WIDTH = 12f;
    private static final float BAR_GAP = 8f;
    private static final float EXPANDED_HEIGHT = 136f;
    private static final int[] HUE_COLORS = {
            0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF,
            0xFF0000FF, 0xFFFF00FF, 0xFFFF0000
    };

    private final IntSupplier colorSupplier;
    private final IntConsumer colorSetter;
    private final IntSupplier secondColorSupplier;
    private final BooleanSupplier splitSupplier;
    private final boolean allowAlpha;
    private final Runnable onCommit;
    private final float[] hsv = new float[3];
    private boolean expanded;
    private float expandProgress;
    private int dragTarget;
    private int color;
    private int observedColor;
    private boolean initialized;
    private boolean pendingCommit;
    private String hexText = "";
    private String alphaText = "";

    public SettingColorPreview(IntSupplier colorSupplier) {
        this(colorSupplier, null, true, () -> {}, colorSupplier, () -> false);
    }

    public SettingColorPreview(IntSupplier colorSupplier, IntSupplier secondColorSupplier, BooleanSupplier splitSupplier) {
        this(colorSupplier, null, true, () -> {}, secondColorSupplier, splitSupplier);
    }

    public SettingColorPreview(IntSupplier colorSupplier, IntConsumer colorSetter, boolean allowAlpha) {
        this(colorSupplier, colorSetter, allowAlpha, () -> {});
    }

    public SettingColorPreview(IntSupplier colorSupplier, IntConsumer colorSetter, boolean allowAlpha, Runnable onCommit) {
        this(colorSupplier, colorSetter, allowAlpha, onCommit, colorSupplier, () -> false);
    }

    private SettingColorPreview(IntSupplier colorSupplier, IntConsumer colorSetter, boolean allowAlpha,
                                Runnable onCommit, IntSupplier secondColorSupplier, BooleanSupplier splitSupplier) {
        this.colorSupplier = Objects.requireNonNull(colorSupplier);
        this.colorSetter = colorSetter;
        this.allowAlpha = allowAlpha;
        this.onCommit = Objects.requireNonNull(onCommit);
        this.secondColorSupplier = Objects.requireNonNull(secondColorSupplier);
        this.splitSupplier = Objects.requireNonNull(splitSupplier);
    }

    @Override
    public float getWidth() {
        return WIDTH;
    }

    @Override
    public float getHeight() {
        return CONTROL_HEIGHT + EXPANDED_HEIGHT * expandProgress;
    }

    @Override
    public float getControlHeight() {
        return CONTROL_HEIGHT;
    }

    @Override
    public void update(float dt) {
        syncColor();
        float target = expanded ? 1f : 0f;
        expandProgress += (target - expandProgress) * (1f - (float) Math.exp(-18f * Math.max(0f, dt)));
        if (Math.abs(expandProgress - target) < 0.001f) expandProgress = target;
    }

    @Override
    public boolean isAnimating() {
        return Math.abs((expanded ? 1f : 0f) - expandProgress) > 0.001f;
    }

    @Override
    public boolean isDragging() {
        return dragTarget != 0;
    }

    @Override
    public void releaseDrag() {
        dragTarget = 0;
        if (pendingCommit) {
            pendingCommit = false;
            onCommit.run();
        }
    }

    @Override
    public void draw(Canvas canvas, float x, float y, float alpha) {
        syncColor();
        ClickGuiThemeColors theme = ClickGuiThemeColors.current();
        float swatchX = x + WIDTH - SWATCH_WIDTH;
        drawSwatch(canvas, swatchX, y, SWATCH_WIDTH, CONTROL_HEIGHT, color, alpha, theme);
        ChromaDraw.text(canvas, hexText, x, y, CONTROL_HEIGHT, withAlpha(theme.secondaryText, alpha), 10f);
        if (colorSetter != null) {
            int markColor = contrastingColor(color);
            ChromaDraw.icon(canvas, expanded ? "\uE5CE" : "\uE5CF", swatchX + SWATCH_WIDTH - 19f,
                    y, CONTROL_HEIGHT, multiplyAlpha(markColor, alpha), 13f, ChromaDraw.IconSet.MATERIAL_SYMBOLS);
        }
        if (expandProgress <= 0.001f) return;
        int save = canvas.save();
        try {
            canvas.clipRect(Rect.makeXYWH(x - 4f, y + CONTROL_HEIGHT, WIDTH + 8f, getHeight() - CONTROL_HEIGHT));
            drawPalette(canvas, x, y + PICKER_TOP, alpha, theme);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void drawPalette(Canvas canvas, float x, float y, float alpha, ClickGuiThemeColors theme) {
        float squareWidth = squareWidth();
        int hueColor = Color.HSBtoRGB(hsv[0], 1f, 1f);
        ChromaDraw.gradient(canvas, x, y, squareWidth, PICKER_HEIGHT,
                multiplyAlpha(0xFFFFFFFF, alpha), multiplyAlpha(hueColor, alpha), false, 5f);
        ChromaDraw.gradient(canvas, x, y, squareWidth, PICKER_HEIGHT,
                0x00000000, multiplyAlpha(0xFF000000, alpha), true, 5f);
        float markerX = x + hsv[1] * squareWidth;
        float markerY = y + (1f - hsv[2]) * PICKER_HEIGHT;
        drawMarker(canvas, markerX, markerY, 3.5f, alpha);

        float hueX = x + squareWidth + BAR_GAP;
        int hueSave = canvas.save();
        try {
            canvas.clipRRect(RRect.makeXYWH(hueX, y, BAR_WIDTH, PICKER_HEIGHT, 4f), true);
            float segmentHeight = PICKER_HEIGHT / 6f;
            for (int i = 0; i < 6; i++) {
                ChromaDraw.gradient(canvas, hueX, y + segmentHeight * i, BAR_WIDTH, segmentHeight + 0.25f,
                        multiplyAlpha(HUE_COLORS[i], alpha), multiplyAlpha(HUE_COLORS[i + 1], alpha), true, 0f);
            }
        } finally {
            canvas.restoreToCount(hueSave);
        }
        drawBarMarker(canvas, hueX, y + hsv[0] * PICKER_HEIGHT, alpha);

        if (allowAlpha) {
            float alphaX = hueX + BAR_WIDTH + BAR_GAP;
            int alphaSave = canvas.save();
            try {
                canvas.clipRRect(RRect.makeXYWH(alphaX, y, BAR_WIDTH, PICKER_HEIGHT, 4f), true);
                drawCheckerboard(canvas, alphaX, y, BAR_WIDTH, PICKER_HEIGHT, alpha, theme);
                ChromaDraw.gradient(canvas, alphaX, y, BAR_WIDTH, PICKER_HEIGHT,
                        multiplyAlpha(color | 0xFF000000, alpha), color & 0x00FFFFFF, true, 0f);
            } finally {
                canvas.restoreToCount(alphaSave);
            }
            drawBarMarker(canvas, alphaX, y + (1f - (color >>> 24) / 255f) * PICKER_HEIGHT, alpha);
        }
        ChromaDraw.text(canvas, UiText.t("饱和度 / 明度", "Saturation / Value"), x, y + PICKER_HEIGHT + 8f,
                14f, withAlpha(theme.secondaryText, alpha), 9f);
        if (allowAlpha) {
            float textWidth = ChromaDraw.textWidth(alphaText, 9f);
            ChromaDraw.text(canvas, alphaText, x + WIDTH - textWidth, y + PICKER_HEIGHT + 8f,
                    14f, withAlpha(theme.secondaryText, alpha), 9f);
        }
    }

    private void drawSwatch(Canvas canvas, float x, float y, float width, float height,
                            int argb, float alpha, ClickGuiThemeColors theme) {
        int save = canvas.save();
        try {
            canvas.clipRRect(RRect.makeXYWH(x, y, width, height, 6f), true);
            drawCheckerboard(canvas, x, y, width, height, alpha, theme);
            if (splitSupplier.getAsBoolean()) {
                ChromaDraw.fill(canvas, x, y, width * 0.5f, height, multiplyAlpha(argb, alpha));
                ChromaDraw.fill(canvas, x + width * 0.5f, y, width * 0.5f, height,
                        multiplyAlpha(secondColorSupplier.getAsInt(), alpha));
            } else {
                ChromaDraw.fill(canvas, x, y, width, height, multiplyAlpha(argb, alpha));
            }
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void drawCheckerboard(Canvas canvas, float x, float y, float width, float height,
                                  float alpha, ClickGuiThemeColors theme) {
        int base = theme.dark ? 0xFF343841 : 0xFFE4E7EC;
        int alternate = theme.dark ? 0xFF4A505B : 0xFFC2C8D1;
        ChromaDraw.fill(canvas, x, y, width, height, multiplyAlpha(base, alpha));
        float cell = 6f;
        int rows = (int) Math.ceil(height / cell);
        int columns = (int) Math.ceil(width / cell);
        for (int row = 0; row < rows; row++) {
            for (int column = row & 1; column < columns; column += 2) {
                ChromaDraw.fill(canvas, x + column * cell, y + row * cell,
                        Math.min(cell, width - column * cell), Math.min(cell, height - row * cell),
                        multiplyAlpha(alternate, alpha));
            }
        }
    }

    private void drawMarker(Canvas canvas, float x, float y, float radius, float alpha) {
        ChromaDraw.outline(canvas, x - radius - 0.7f, y - radius - 0.7f,
                radius * 2f + 1.4f, radius * 2f + 1.4f, radius + 0.7f, 2.5f,
                multiplyAlpha(0x99000000, alpha));
        ChromaDraw.outline(canvas, x - radius, y - radius, radius * 2f, radius * 2f, radius, 1.6f,
                multiplyAlpha(0xFFFFFFFF, alpha));
    }

    private void drawBarMarker(Canvas canvas, float x, float y, float alpha) {
        ChromaDraw.rounded(canvas, x - 2f, y - 2f, BAR_WIDTH + 4f, 4f, 2f,
                multiplyAlpha(0x99000000, alpha));
        ChromaDraw.rounded(canvas, x - 1f, y - 1f, BAR_WIDTH + 2f, 2f, 1f,
                multiplyAlpha(0xFFFFFFFF, alpha));
    }

    @Override
    public boolean onClick(float mx, float my, float x, float y, int button) {
        if (colorSetter == null || (button != 0 && button != 1)) return false;
        if (contains(mx, my, x + WIDTH - SWATCH_WIDTH, y, SWATCH_WIDTH, CONTROL_HEIGHT)) {
            releaseDrag();
            expanded = !expanded;
            return true;
        }
        if (button != 0 || !expanded || my > y + getHeight()) return false;
        float paletteY = y + PICKER_TOP;
        float squareWidth = squareWidth();
        if (contains(mx, my, x, paletteY, squareWidth, PICKER_HEIGHT)) {
            dragTarget = 1;
        } else if (contains(mx, my, x + squareWidth + BAR_GAP - 2f, paletteY,
                BAR_WIDTH + 4f, PICKER_HEIGHT)) {
            dragTarget = 2;
        } else if (allowAlpha && contains(mx, my, x + WIDTH - BAR_WIDTH - 2f, paletteY,
                BAR_WIDTH + 4f, PICKER_HEIGHT)) {
            dragTarget = 3;
        } else {
            return false;
        }
        applyMouse(mx, my, x, y);
        return true;
    }

    @Override
    public boolean onDrag(float mx, float my, float x, float y) {
        if (dragTarget == 0 || !expanded) return false;
        applyMouse(mx, my, x, y);
        return true;
    }

    private void applyMouse(float mx, float my, float x, float y) {
        syncColor();
        float vertical = clamp((my - y - PICKER_TOP) / PICKER_HEIGHT);
        int opacity = color >>> 24;
        switch (dragTarget) {
            case 1 -> {
                hsv[1] = clamp((mx - x) / squareWidth());
                hsv[2] = 1f - vertical;
            }
            case 2 -> hsv[0] = vertical;
            case 3 -> opacity = Math.round((1f - vertical) * 255f);
            default -> {
                return;
            }
        }
        int nextColor = (Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]) & 0xFFFFFF)
                | ((allowAlpha ? opacity : 255) << 24);
        if (nextColor == color) return;
        color = nextColor;
        observedColor = nextColor;
        updateLabels();
        colorSetter.accept(nextColor);
        pendingCommit = true;
    }

    private void syncColor() {
        int supplied = colorSupplier.getAsInt();
        if (!allowAlpha) supplied |= 0xFF000000;
        if (initialized && (isDragging() || supplied == observedColor)) return;
        int red = (supplied >>> 16) & 0xFF;
        int green = (supplied >>> 8) & 0xFF;
        int blue = supplied & 0xFF;
        float previousHue = hsv[0];
        Color.RGBtoHSB(red, green, blue, hsv);
        if (initialized && hsv[1] == 0f) hsv[0] = previousHue;
        color = supplied;
        observedColor = supplied;
        initialized = true;
        updateLabels();
    }

    private void updateLabels() {
        hexText = allowAlpha ? String.format(Locale.ROOT, "#%08X", color)
                : String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF);
        alphaText = "A " + Math.round((color >>> 24) * 100f / 255f) + "%";
    }

    private float squareWidth() {
        return WIDTH - (BAR_WIDTH + BAR_GAP) * (allowAlpha ? 2f : 1f);
    }

    private static boolean contains(float mx, float my, float x, float y, float width, float height) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }

    private static int multiplyAlpha(int argb, float alpha) {
        return (Math.round((argb >>> 24) * clamp(alpha)) << 24) | (argb & 0xFFFFFF);
    }

    private static int contrastingColor(int argb) {
        float luminance = ((argb >>> 16) & 0xFF) * 0.2126f + ((argb >>> 8) & 0xFF) * 0.7152f
                + (argb & 0xFF) * 0.0722f;
        return luminance > 155f && (argb >>> 24) > 160 ? 0xFF20242A : 0xFFFFFFFF;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
