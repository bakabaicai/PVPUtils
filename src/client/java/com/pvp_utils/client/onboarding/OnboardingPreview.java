package com.pvp_utils.client.onboarding;

import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.theme.UiTheme;
import com.pvp_utils.client.render.chroma.ChromaDraw;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.FontMetrics;
import io.github.humbleui.skija.PaintStrokeCap;
import io.github.humbleui.skija.PaintStrokeJoin;
import io.github.humbleui.skija.Path;
import io.github.humbleui.skija.PathBuilder;
import io.github.humbleui.types.RRect;

import java.util.LinkedHashMap;
import java.util.Map;

public final class OnboardingPreview implements AutoCloseable {
    public enum Mode {
        KEYBOARD,
        INVENTORY,
        HUD
    }

    private static final float DESIGN_WIDTH = 440f;
    private static final float DESIGN_HEIGHT = 260f;
    private static final int MAX_TEXTS = 96;
    private static final String PREVIEW_FONT = ChromaDraw.CLIENT_FONT;
    private static final String[] TOP_KEYS = {"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"};
    private static final String[] HOME_KEYS = {"A", "S", "D", "F", "G", "H", "J", "K", "L"};
    private static final String[] LOWER_KEYS = {"Z", "X", "C", "V", "B", "N", "M"};
    private final Map<TextKey, CachedText> texts = new LinkedHashMap<>();
    private long fontRevision = -1;
    private String language;
    private Path pointer;

    public void draw(Canvas canvas, float x, float y, float width, float height,
                     Mode mode, double seconds, UiTheme theme, int accent) {
        if (canvas == null || !Float.isFinite(x) || !Float.isFinite(y)
                || !Float.isFinite(width) || !Float.isFinite(height) || width <= 0f || height <= 0f) return;
        UiTheme selectedTheme = theme == null ? UiTheme.DARK : theme;
        Palette palette = new Palette(selectedTheme.background(), selectedTheme.panel(),
                selectedTheme.primaryText(), selectedTheme.secondaryText(), accent, selectedTheme.isDark());
        Mode selectedMode = mode == null ? Mode.KEYBOARD : mode;
        double time = Double.isFinite(seconds) ? Math.max(0d, seconds) : 0d;
        float scale = Math.min(width / DESIGN_WIDTH, height / DESIGN_HEIGHT);
        int save = canvas.save();
        try {
            canvas.clipRRect(RRect.makeXYWH(x, y, width, height, Math.min(14f, Math.min(width, height) * 0.1f)), true);
            ChromaDraw.gradientDiagonal(canvas, x, y, width, height,
                    mix(palette.background(), palette.panel(), 0.45f), palette.background(), 0f);
            canvas.translate(x + (width - DESIGN_WIDTH * scale) * 0.5f,
                    y + (height - DESIGN_HEIGHT * scale) * 0.5f);
            canvas.scale(scale, scale);
            ChromaDraw.withFont(PREVIEW_FONT, () -> {
                ensureResources();
                drawBackdrop(canvas, palette);
                switch (selectedMode) {
                    case KEYBOARD -> drawKeyboard(canvas, cycle(time, 8d), palette);
                    case INVENTORY -> drawInventory(canvas, cycle(time, 8d), palette);
                    case HUD -> drawHud(canvas, cycle(time, 10d), palette);
                }
            });
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void drawBackdrop(Canvas canvas, Palette palette) {
        for (int row = 0; row < 6; row++) {
            for (int column = 0; column < 12; column++) {
                ChromaDraw.rounded(canvas, 24f + column * 35f, 48f + row * 33f,
                        1.5f, 1.5f, 0.75f, alpha(palette.secondary(), 0.12f));
            }
        }
        ChromaDraw.rounded(canvas, 24f, 23f, 5f, 5f, 2.5f, palette.accent());
    }

    private void drawKeyboard(Canvas canvas, float time, Palette palette) {
        text(canvas, UiText.t("右 Shift 打开设置", "Right Shift opens settings"),
                36f, 16f, 20f, 12f, true, palette.primary());
        text(canvas, "01", 391f, 18f, 16f, 9f, true, alpha(palette.secondary(), 0.7f));
        float press = rise(time, 0.85f, 1.2f) * (1f - rise(time, 6.9f, 7.5f));
        ChromaDraw.dropShadowRounded(canvas, 22f, 124f, 326f, 94f,
                12f, 3f, 12f, alpha(palette.shadow(), 0.18f));
        ChromaDraw.rounded(canvas, 22f, 124f, 326f, 94f, 12f, palette.panel());
        ChromaDraw.outline(canvas, 22f, 124f, 326f, 94f, 12f, 1f,
                alpha(palette.secondary(), 0.18f));
        for (int i = 0; i < TOP_KEYS.length; i++) {
            drawKey(canvas, TOP_KEYS[i], 31f + i * 31f, 134f, 27f, 16f, 0f, palette);
        }
        for (int i = 0; i < HOME_KEYS.length; i++) {
            drawKey(canvas, HOME_KEYS[i], 42f + i * 31f, 154f, 27f, 16f, 0f, palette);
        }
        for (int i = 0; i < LOWER_KEYS.length; i++) {
            drawKey(canvas, LOWER_KEYS[i], 53f + i * 29f, 174f, 26f, 16f, 0f, palette);
        }
        drawKey(canvas, "R Shift", 267f, 174f, 71f, 16f, press, palette);
        drawKey(canvas, "Ctrl", 31f, 194f, 35f, 14f, 0f, palette);
        drawKey(canvas, "Alt", 70f, 194f, 30f, 14f, 0f, palette);
        drawKey(canvas, "", 104f, 194f, 153f, 14f, 0f, palette);
        drawKey(canvas, "Alt", 261f, 194f, 30f, 14f, 0f, palette);
        drawKey(canvas, "Ctrl", 295f, 194f, 43f, 14f, 0f, palette);
        float pulse = rise(time, 1f, 1.2f) * (1f - rise(time, 1.25f, 1.85f));
        if (pulse > 0f) {
            ChromaDraw.outline(canvas, 263f - pulse * 3f, 170f - pulse * 3f,
                    79f + pulse * 6f, 24f + pulse * 6f, 7f, 1.6f, alpha(palette.accent(), pulse * 0.6f));
        }
        float visible = rise(time, 1.3f, 1.85f) * (1f - rise(time, 6.6f, 7.25f));
        drawMiniGui(canvas, 224f, 49f + (1f - visible) * 6f, 192f, 114f,
                visible, 1f, palette);
        text(canvas, UiText.t("键盘", "Keyboard"), 29f, 99f, 18f, 10f, false, palette.secondary());
        ChromaDraw.line(canvas, 126f, 107f, 199f, 107f, 1f, alpha(palette.accent(), press * 0.6f));
        drawArrow(canvas, 199f, 107f, 207f, 99f, alpha(palette.accent(), press * 0.6f));
        drawFooter(canvas, UiText.t("按下右 Shift", "Press Right Shift"), "ClickGUI", palette);
    }

    private void drawKey(Canvas canvas, String label, float x, float y, float width, float height,
                         float active, Palette palette) {
        int base = mix(palette.background(), palette.primary(), palette.dark() ? 0.05f : 0.025f);
        int surface = mix(base, palette.accent(), active * 0.86f);
        ChromaDraw.rounded(canvas, x, y + 1.5f, width, height, 3f, alpha(palette.secondary(), 0.16f));
        ChromaDraw.rounded(canvas, x, y + active, width, height - active, 3f, surface);
        ChromaDraw.outline(canvas, x, y + active, width, height - active, 3f, 0.65f,
                mix(alpha(palette.secondary(), 0.22f), palette.accent(), active));
        centered(canvas, label, x, y + active, width, height - active,
                8f, active > 0.5f, active > 0.5f ? UiTheme.readableText(palette.primary(), surface) : palette.secondary());
    }

    private void drawInventory(Canvas canvas, float time, Palette palette) {
        text(canvas, UiText.t("背包内也能打开", "Open from your inventory"),
                36f, 16f, 20f, 12f, true, palette.primary());
        text(canvas, "02", 391f, 18f, 16f, 9f, true, alpha(palette.secondary(), 0.7f));
        panel(canvas, 22f, 54f, 396f, 166f, 12f, 1f, palette);
        text(canvas, UiText.t("物品栏", "Inventory"), 35f, 65f, 20f, 11f, true, palette.primary());
        float hover = rise(time, 1.35f, 1.85f) * (1f - rise(time, 6.65f, 7.25f));
        int buttonColor = mix(palette.background(), palette.accent(), 0.12f + hover * 0.68f);
        ChromaDraw.rounded(canvas, 326f, 64f, 81f, 22f, 7f, buttonColor);
        ChromaDraw.outline(canvas, 326f, 64f, 81f, 22f, 7f, 1f, alpha(palette.accent(), 0.5f + hover * 0.5f));
        centered(canvas, "PVPUtils", 326f, 64f, 81f, 22f, 10f, true,
                UiTheme.readableText(palette.primary(), buttonColor));
        ChromaDraw.rounded(canvas, 35f, 93f, 76f, 96f, 7f, palette.background());
        for (int i = 0; i < 4; i++) drawSlot(canvas, 40f, 98f + i * 21f, 16f, 17f, palette);
        ChromaDraw.rounded(canvas, 74f, 105f, 18f, 18f, 3f, mix(palette.secondary(), palette.panel(), 0.35f));
        ChromaDraw.rounded(canvas, 70f, 126f, 26f, 31f, 3f, mix(palette.accent(), palette.panel(), 0.24f));
        ChromaDraw.rounded(canvas, 72f, 159f, 10f, 19f, 2f, palette.secondary());
        ChromaDraw.rounded(canvas, 85f, 159f, 10f, 19f, 2f, palette.secondary());
        ChromaDraw.line(canvas, 66f, 130f, 66f, 150f, 5f, alpha(palette.secondary(), 0.7f), PaintStrokeCap.ROUND);
        ChromaDraw.line(canvas, 100f, 130f, 100f, 150f, 5f, alpha(palette.secondary(), 0.7f), PaintStrokeCap.ROUND);
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 2; column++) drawSlot(canvas, 229f + column * 21f, 85f + row * 21f, 18f, 18f, palette);
        }
        drawArrow(canvas, 277f, 105f, 292f, 105f, alpha(palette.secondary(), 0.6f));
        drawSlot(canvas, 301f, 96f, 23f, 23f, palette);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                float slotX = 117f + column * 32f;
                float slotY = 131f + row * 22f;
                drawSlot(canvas, slotX, slotY, 28f, 19f, palette);
                if ((row == 0 && column == 0) || (row == 1 && column == 3) || (row == 2 && column == 7)) {
                    drawItem(canvas, slotX + 8f, slotY + 4f, (row + column) % 3, palette);
                }
            }
        }
        for (int column = 0; column < 9; column++) {
            float slotX = 117f + column * 32f;
            drawSlot(canvas, slotX, 198f, 28f, 16f, palette);
            if (column < 3) drawItem(canvas, slotX + 8f, 201f, column, palette);
        }
        float approach = rise(time, 0.45f, 1.8f);
        float pointerX = lerp(279f, 365f, approach);
        float pointerY = lerp(179f, 75f, approach);
        float click = rise(time, 1.9f, 2.1f) * (1f - rise(time, 2.15f, 2.65f));
        drawPointer(canvas, pointerX, pointerY, 1f - rise(time, 6.8f, 7.4f), click, palette);
        float visible = rise(time, 2.15f, 2.7f) * (1f - rise(time, 6.6f, 7.25f));
        drawMiniGui(canvas, 194f, 96f + (1f - visible) * 6f, 204f, 118f, visible, 1f, palette);
        drawFooter(canvas, UiText.t("点击右上角 PVPUtils", "Click PVPUtils at the top right"), "ClickGUI", palette);
    }

    private void drawSlot(Canvas canvas, float x, float y, float width, float height, Palette palette) {
        ChromaDraw.rounded(canvas, x, y, width, height, 3f, palette.background());
        ChromaDraw.outline(canvas, x, y, width, height, 3f, 0.8f, alpha(palette.secondary(), 0.2f));
    }

    private void drawItem(Canvas canvas, float x, float y, int kind, Palette palette) {
        int color = mix(palette.accent(), palette.primary(), 0.2f);
        if (kind == 0) {
            ChromaDraw.line(canvas, x + 2f, y + 10f, x + 10f, y + 2f, 2.6f, color, PaintStrokeCap.ROUND);
            ChromaDraw.line(canvas, x + 1f, y + 6f, x + 6f, y + 11f, 2f, palette.secondary(), PaintStrokeCap.ROUND);
        } else if (kind == 1) {
            ChromaDraw.rounded(canvas, x + 1f, y + 1f, 10f, 10f, 3f, color);
            ChromaDraw.line(canvas, x + 3f, y + 3f, x + 8f, y + 3f, 1f, alpha(palette.primary(), 0.8f));
        } else {
            ChromaDraw.rounded(canvas, x + 1f, y + 3f, 11f, 8f, 4f, mix(color, palette.secondary(), 0.35f));
            ChromaDraw.line(canvas, x + 7f, y + 3f, x + 9f, y, 1.8f, color, PaintStrokeCap.ROUND);
        }
    }

    private void drawHud(Canvas canvas, float time, Palette palette) {
        text(canvas, UiText.t("聊天界面编辑 HUD", "Edit HUD while chat is open"),
                36f, 16f, 20f, 12f, true, palette.primary());
        text(canvas, "03", 391f, 18f, 16f, 9f, true, alpha(palette.secondary(), 0.7f));
        panel(canvas, 22f, 54f, 396f, 166f, 12f, 1f, palette);
        for (int i = 0; i < 4; i++) {
            ChromaDraw.line(canvas, 33f, 86f + i * 29f, 407f, 86f + i * 29f,
                    0.7f, alpha(palette.secondary(), 0.07f));
        }
        ChromaDraw.line(canvas, 214f, 123f, 226f, 123f, 1f, alpha(palette.secondary(), 0.25f));
        ChromaDraw.line(canvas, 220f, 117f, 220f, 129f, 1f, alpha(palette.secondary(), 0.25f));
        float drag = rise(time, 0.7f, 3f);
        float resize = rise(time, 3.5f, 5.1f);
        float capsuleX = lerp(43f, 105f, drag);
        float capsuleY = lerp(78f, 116f, drag);
        float capsuleWidth = lerp(138f, 171f, resize);
        float capsuleHeight = lerp(28f, 35f, resize);
        float guiVisible = rise(time, 5.9f, 6.45f) * (1f - rise(time, 8.45f, 9f));
        float clockEnabled = rise(time, 7f, 7.45f);
        drawScoreboard(canvas, palette);
        drawClock(canvas, clockEnabled, palette);
        if (drag > 0f && drag < 1f) {
            ChromaDraw.outline(canvas, 43f, 78f, 138f, 28f, 14f, 1f, alpha(palette.accent(), 0.18f));
            drawArrow(canvas, 100f, 107f, 144f, 127f, alpha(palette.accent(), 0.3f));
        }
        int capsuleColor = mix(palette.background(), palette.accent(), 0.09f);
        ChromaDraw.dropShadowRounded(canvas, capsuleX, capsuleY, capsuleWidth, capsuleHeight,
                capsuleHeight * 0.5f, 2f, 10f, alpha(palette.shadow(), 0.12f));
        ChromaDraw.rounded(canvas, capsuleX, capsuleY, capsuleWidth, capsuleHeight,
                capsuleHeight * 0.5f, capsuleColor);
        if ((drag > 0f && drag < 1f) || (resize > 0f && resize < 1f)) {
            drawDashedBounds(canvas, capsuleX - 2f, capsuleY - 2f, capsuleWidth + 4f, capsuleHeight + 4f,
                    alpha(palette.accent(), 0.7f * (1f - guiVisible)));
        }
        text(canvas, UiText.t("灵动岛", "Dynamic Island"), capsuleX, capsuleY - 17f, 14f, 8f, false,
                alpha(palette.secondary(), 1f - guiVisible));
        ChromaDraw.rounded(canvas, capsuleX + 10f, capsuleY + capsuleHeight * 0.5f - 3f,
                6f, 6f, 3f, palette.accent());
        text(canvas, "PVPUtils", capsuleX + 23f, capsuleY, capsuleHeight, 10f, true, palette.primary());
        text(canvas, "144 FPS", capsuleX + capsuleWidth - 52f, capsuleY, capsuleHeight, 8f, false, palette.secondary());
        drawResizeGrip(canvas, capsuleX + capsuleWidth - 15f, capsuleY + capsuleHeight - 15f,
                alpha(palette.accent(), 0.8f * (1f - guiVisible)));
        String action = time < 3.4f ? UiText.t("拖动位置", "Drag to move") : UiText.t("拖动右下角弧线调整大小", "Drag the bottom-right arc to resize");
        text(canvas, action, 34f, 176f, 16f, 9f, false, alpha(palette.secondary(), 1f - guiVisible));
        ChromaDraw.rounded(canvas, 32f, 197f, 376f, 16f, 4f, palette.background());
        ChromaDraw.outline(canvas, 32f, 197f, 376f, 16f, 4f, 0.8f, alpha(palette.accent(), 0.35f));
        text(canvas, UiText.t("聊天输入…", "Chat message…"), 39f, 197f, 16f, 8f, false, palette.secondary());
        ChromaDraw.line(canvas, 398f, 201f, 398f, 209f, 1f,
                alpha(palette.accent(), (float) (0.45d + Math.sin(time * 4d) * 0.35d)));
        drawMiniGui(canvas, 33f, 65f + (1f - guiVisible) * 5f, 203f, 116f,
                guiVisible, clockEnabled, palette);
        float pointerX;
        float pointerY;
        if (time < 3.4f) {
            pointerX = capsuleX + 42f;
            pointerY = capsuleY + 14f;
        } else if (time < 5.9f) {
            float approach = rise(time, 3.25f, 3.5f);
            pointerX = lerp(capsuleX + 42f, capsuleX + capsuleWidth - 5f, approach);
            pointerY = lerp(capsuleY + 14f, capsuleY + capsuleHeight - 5f, approach);
        } else {
            float approach = rise(time, 5.9f, 6.8f);
            pointerX = lerp(capsuleX + capsuleWidth - 5f, 214f, approach);
            pointerY = lerp(capsuleY + capsuleHeight - 5f, 153f, approach);
        }
        float click = rise(time, 6.95f, 7.1f) * (1f - rise(time, 7.15f, 7.65f));
        drawPointer(canvas, pointerX, pointerY, 1f - rise(time, 8.7f, 9.4f), click, palette);
        centered(canvas, UiText.t("已关闭的 HUD → 在 ClickGUI 重新开启", "Disabled HUD → enable it again in ClickGUI"),
                20f, 230f, 400f, 18f, 10f, false, palette.secondary());
    }

    private void drawScoreboard(Canvas canvas, Palette palette) {
        ChromaDraw.rounded(canvas, 299f, 108f, 106f, 76f, 8f, palette.background());
        ChromaDraw.rounded(canvas, 299f, 108f, 106f, 20f, 8f, alpha(palette.accent(), 0.13f));
        text(canvas, UiText.t("计分板", "Scoreboard"), 309f, 108f, 20f, 9f, true, palette.primary());
        text(canvas, UiText.t("击杀", "Kills"), 309f, 132f, 16f, 8f, false, palette.secondary());
        text(canvas, "12", 383f, 132f, 16f, 8f, true, palette.accent());
        text(canvas, UiText.t("连胜", "Streak"), 309f, 148f, 16f, 8f, false, palette.secondary());
        text(canvas, "3", 389f, 148f, 16f, 8f, true, palette.accent());
        ChromaDraw.line(canvas, 310f, 174f, 389f, 174f, 2f, alpha(palette.secondary(), 0.2f), PaintStrokeCap.ROUND);
    }

    private void drawClock(Canvas canvas, float enabled, Palette palette) {
        ChromaDraw.rounded(canvas, 299f, 69f, 106f, 29f, 8f, palette.background());
        ChromaDraw.outline(canvas, 299f, 69f, 106f, 29f, 8f, 0.8f,
                alpha(palette.secondary(), (1f - enabled) * 0.22f));
        text(canvas, UiText.t("时钟", "Clock"), 309f, 69f, 29f, 9f, false,
                alpha(palette.secondary(), 1f - enabled));
        text(canvas, UiText.t("已关闭", "Off"), 370f, 69f, 29f, 8f, false,
                alpha(palette.secondary(), (1f - enabled) * 0.6f));
        text(canvas, "14:32", 310f, 69f, 29f, 12f, true, alpha(palette.primary(), enabled));
        ChromaDraw.rounded(canvas, 384f, 80f, 6f, 6f, 3f, alpha(palette.accent(), enabled));
    }

    private void drawMiniGui(Canvas canvas, float x, float y, float width, float height,
                             float visibility, float clockEnabled, Palette palette) {
        if (visibility <= 0f) return;
        panel(canvas, x, y, width, height, 10f, visibility, palette);
        ChromaDraw.rounded(canvas, x + 9f, y + 9f, 5f, 5f, 2.5f, alpha(palette.accent(), visibility));
        text(canvas, "PVPUtils", x + 19f, y + 5f, 15f, 9f, true, alpha(palette.primary(), visibility));
        text(canvas, "ClickGUI", x + width - 51f, y + 5f, 15f, 8f, false, alpha(palette.secondary(), visibility));
        ChromaDraw.line(canvas, x + 9f, y + 25f, x + width - 9f, y + 25f,
                0.8f, alpha(palette.secondary(), visibility * 0.15f));
        ChromaDraw.rounded(canvas, x + 8f, y + 33f, 39f, height - 41f, 6f,
                alpha(palette.background(), visibility));
        centered(canvas, "HUD", x + 8f, y + 34f, 39f, 21f, 8f, true, alpha(palette.accent(), visibility));
        centered(canvas, UiText.t("显示", "Render"), x + 8f, y + 57f, 39f, 18f,
                8f, false, alpha(palette.secondary(), visibility * 0.65f));
        centered(canvas, UiText.t("设置", "Settings"), x + 8f, y + 77f, 39f, 18f,
                8f, false, alpha(palette.secondary(), visibility * 0.65f));
        drawGuiRow(canvas, UiText.t("灵动岛", "Dynamic Island"), x + 56f, y + 33f,
                width - 65f, 1f, visibility, palette);
        drawGuiRow(canvas, UiText.t("计分板", "Scoreboard"), x + 56f, y + 57f,
                width - 65f, 1f, visibility, palette);
        drawGuiRow(canvas, UiText.t("时钟", "Clock"), x + 56f, y + 81f,
                width - 65f, clockEnabled, visibility, palette);
    }

    private void drawGuiRow(Canvas canvas, String label, float x, float y, float width,
                            float enabled, float visibility, Palette palette) {
        text(canvas, label, x, y, 18f, 8f, false, alpha(palette.primary(), visibility));
        int track = mix(alpha(palette.secondary(), 0.2f), palette.accent(), enabled);
        ChromaDraw.rounded(canvas, x + width - 21f, y + 4f, 20f, 10f, 5f, alpha(track, visibility));
        ChromaDraw.rounded(canvas, x + width - 19f + enabled * 9f, y + 6f,
                6f, 6f, 3f, alpha(UiTheme.readableText(palette.primary(), track), visibility));
        ChromaDraw.line(canvas, x, y + 22f, x + width, y + 22f, 0.7f,
                alpha(palette.secondary(), visibility * 0.1f));
    }

    private void panel(Canvas canvas, float x, float y, float width, float height,
                       float radius, float visibility, Palette palette) {
        ChromaDraw.dropShadowRounded(canvas, x, y, width, height, radius, 3f, 12f,
                alpha(palette.shadow(), visibility * 0.17f));
        ChromaDraw.rounded(canvas, x, y, width, height, radius, alpha(palette.panel(), visibility));
        ChromaDraw.outline(canvas, x, y, width, height, radius, 0.9f,
                alpha(palette.secondary(), visibility * 0.17f));
    }

    private void drawFooter(Canvas canvas, String first, String second, Palette palette) {
        CachedText firstText = prepared(first, 10f, false);
        CachedText secondText = prepared(second, 10f, true);
        float firstWidth = firstText.value().width() + 20f;
        float secondWidth = secondText.value().width() + 20f;
        float x = (DESIGN_WIDTH - firstWidth - secondWidth - 27f) * 0.5f;
        ChromaDraw.rounded(canvas, x, 230f, firstWidth, 21f, 10.5f, alpha(palette.secondary(), 0.07f));
        firstText.value().drawTop(canvas, x + 10f, 230f + (21f - firstText.height()) * 0.5f, palette.secondary());
        drawArrow(canvas, x + firstWidth + 6f, 240.5f, x + firstWidth + 20f, 240.5f, palette.accent());
        float secondX = x + firstWidth + 27f;
        ChromaDraw.rounded(canvas, secondX, 230f, secondWidth, 21f, 10.5f, alpha(palette.accent(), 0.13f));
        secondText.value().drawTop(canvas, secondX + 10f, 230f + (21f - secondText.height()) * 0.5f,
                UiTheme.readableText(palette.accent(), palette.background()));
    }

    private void drawArrow(Canvas canvas, float startX, float startY, float endX, float endY, int color) {
        ChromaDraw.line(canvas, startX, startY, endX, endY, 1.2f, color, PaintStrokeCap.ROUND);
        double angle = Math.atan2(endY - startY, endX - startX);
        float backX = endX - (float) Math.cos(angle) * 4f;
        float backY = endY - (float) Math.sin(angle) * 4f;
        float sideX = (float) Math.sin(angle) * 3f;
        float sideY = (float) Math.cos(angle) * 3f;
        ChromaDraw.line(canvas, endX, endY, backX + sideX, backY - sideY, 1.2f, color, PaintStrokeCap.ROUND);
        ChromaDraw.line(canvas, endX, endY, backX - sideX, backY + sideY, 1.2f, color, PaintStrokeCap.ROUND);
    }

    private void drawResizeGrip(Canvas canvas, float x, float y, int color) {
        float radius = 9f;
        for (int segment = 0; segment < 8; segment++) {
            double first = segment * Math.PI / 16d;
            double second = (segment + 1) * Math.PI / 16d;
            ChromaDraw.line(canvas, x + (float) Math.cos(first) * radius, y + (float) Math.sin(first) * radius,
                    x + (float) Math.cos(second) * radius, y + (float) Math.sin(second) * radius,
                    1.1f, color, PaintStrokeCap.ROUND);
        }
    }

    private void drawDashedBounds(Canvas canvas, float x, float y, float width, float height, int color) {
        for (float offset = 0f; offset < width; offset += 8f) {
            float end = Math.min(width, offset + 4f);
            ChromaDraw.line(canvas, x + offset, y, x + end, y, 0.9f, color);
            ChromaDraw.line(canvas, x + offset, y + height, x + end, y + height, 0.9f, color);
        }
        for (float offset = 0f; offset < height; offset += 8f) {
            float end = Math.min(height, offset + 4f);
            ChromaDraw.line(canvas, x, y + offset, x, y + end, 0.9f, color);
            ChromaDraw.line(canvas, x + width, y + offset, x + width, y + end, 0.9f, color);
        }
    }

    private void drawPointer(Canvas canvas, float x, float y, float visibility, float click, Palette palette) {
        if (visibility <= 0f) return;
        if (click > 0f) {
            float radius = 7f + (1f - click) * 8f;
            ChromaDraw.outline(canvas, x - radius, y - radius, radius * 2f, radius * 2f,
                    radius, 1.3f, alpha(palette.accent(), click * visibility * 0.65f));
        }
        int save = canvas.save();
        try {
            canvas.translate(x, y);
            ChromaDraw.fillPath(canvas, pointer, alpha(palette.primary(), visibility));
            ChromaDraw.strokePath(canvas, pointer, 1f, alpha(palette.panel(), visibility),
                    PaintStrokeCap.ROUND, PaintStrokeJoin.ROUND);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void text(Canvas canvas, String value, float x, float top, float height,
                      float size, boolean bold, int color) {
        if (color >>> 24 == 0 || value.isEmpty()) return;
        CachedText label = prepared(value, size, bold);
        label.value().drawTop(canvas, x, top + (height - label.height()) * 0.5f, color);
    }

    private void centered(Canvas canvas, String value, float x, float top, float width, float height,
                          float size, boolean bold, int color) {
        if (color >>> 24 == 0 || value.isEmpty()) return;
        CachedText label = prepared(value, size, bold);
        label.value().drawTop(canvas, x + (width - label.value().width()) * 0.5f,
                top + (height - label.height()) * 0.5f, color);
    }

    private CachedText prepared(String value, float size, boolean bold) {
        TextKey key = new TextKey(value, size, bold, PREVIEW_FONT, fontRevision, language);
        CachedText cached = texts.get(key);
        if (cached != null) return cached;
        if (texts.size() >= MAX_TEXTS) {
            var entries = texts.entrySet().iterator();
            Map.Entry<TextKey, CachedText> oldest = entries.next();
            oldest.getValue().value().close();
            entries.remove();
        }
        FontMetrics metrics = ChromaDraw.textMetrics(size);
        CachedText created = new CachedText(ChromaDraw.prepareText(value, size, bold),
                metrics.getDescent() - metrics.getAscent());
        texts.put(key, created);
        return created;
    }

    private void ensureResources() {
        String currentLanguage = UiText.t("预览", "Preview");
        long currentRevision = ChromaDraw.textFontRevision();
        if (fontRevision != currentRevision || !currentLanguage.equals(language)) {
            clearText();
            fontRevision = currentRevision;
            language = currentLanguage;
        }
        if (pointer == null) {
            try (PathBuilder builder = new PathBuilder()) {
                builder.moveTo(0f, 0f).lineTo(0f, 15f).lineTo(4f, 11f)
                        .lineTo(7f, 18f).lineTo(10f, 16.5f).lineTo(7f, 10f)
                        .lineTo(13f, 10f).closePath();
                pointer = builder.detach();
            }
        }
    }

    private void clearText() {
        for (CachedText cached : texts.values()) cached.value().close();
        texts.clear();
    }

    @Override
    public void close() {
        clearText();
        if (pointer != null) pointer.close();
        pointer = null;
        fontRevision = -1;
        language = null;
    }

    private static float cycle(double seconds, double duration) {
        return (float) (seconds % duration);
    }

    private static float rise(float value, float start, float end) {
        float progress = Math.clamp((value - start) / (end - start), 0f, 1f);
        return progress * progress * (3f - 2f * progress);
    }

    private static float lerp(float first, float second, float progress) {
        return first + (second - first) * progress;
    }

    private static int alpha(int color, float opacity) {
        return color & 0x00FFFFFF | Math.round((color >>> 24) * Math.clamp(opacity, 0f, 1f)) << 24;
    }

    private static int mix(int first, int second, float amount) {
        float progress = Math.clamp(amount, 0f, 1f);
        int a = Math.round(lerp(first >>> 24, second >>> 24, progress));
        int r = Math.round(lerp(first >> 16 & 255, second >> 16 & 255, progress));
        int g = Math.round(lerp(first >> 8 & 255, second >> 8 & 255, progress));
        int b = Math.round(lerp(first & 255, second & 255, progress));
        return a << 24 | r << 16 | g << 8 | b;
    }

    private record TextKey(String value, float size, boolean bold, String font, long revision, String language) {
    }

    private record CachedText(ChromaDraw.PreparedText value, float height) {
    }

    private record Palette(int background, int panel, int primary, int secondary, int accent, boolean dark) {
        private int shadow() {
            return dark ? 0xFF000000 : 0xFF1C2430;
        }
    }
}
