package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.chroma.ChromaDraw;
import com.pvp_utils.client.skin.SkinManager;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Path;
import io.github.humbleui.skija.PathBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

public class ArraylistRenderer {
    private static final ArraylistRenderer INSTANCE = new ArraylistRenderer();
    private static final long START_TIME_NANOS = System.nanoTime();
    private static final int LINE_HEIGHT = 10;
    private static final int PADDING_X = 2;
    private static final int BACKGROUND_PADDING_X = 3;
    private static final int BACKGROUND_TOP_PADDING = 1;
    private static final int BACKGROUND_COLOR = 0x66000000;
    private static final int PREVIEW_WIDTH = 104;
    private static final int PREVIEW_HEIGHT = 52;
    private static final float NEW_LINE_HEIGHT = 13f;
    private static final float NEW_TEXT_SIZE = 9f;
    private static final float NEW_PADDING = 4f;
    private static final float NEW_ACCENT_WIDTH = 2f;
    private static final float NEW_CORNER_RADIUS = 6f;
    private static final float NEW_BOLD_OFFSET = 0.4f;
    private static final int NEW_SUFFIX_COLOR = 0xFFAAAAAA;
    private static final int NEW_BACKGROUND_COLOR = 0x700D0B10;

    private static final List<Entry> ENTRIES = List.of(
            new Entry("Auto GG", () -> Config.autoGG),
            new Entry("Auto Main Hand", () -> Config.mainHandAssist),
            new Entry("Auto Sprint", () -> Config.autoSprint),
            new Entry("Projectile Trajectory Predict", () -> Config.projectileTrajectoryPredict),
            new Entry("Arraylist", () -> Config.arraylist),
            new Entry("Armor HUD", () -> Config.armorHud),
            new Entry("Attack Effects", () -> Config.attackEffectsCritParticles || Config.attackEffectsSharpnessParticles || Config.attackEffectsFlameParticles || Config.attackEffectsBloodParticles || Config.attackEffectsLightning),
            new Entry("Better Chat", () -> Config.betterChat),
            new Entry("Better Mouse Logic", () -> Config.betterMouseLogic),
            new Entry("Better Ping Display", () -> Config.betterPingDisplay),
            new Entry("Better Scoreboard", () -> Config.betterScoreboard),
            new Entry("Block Count Display", () -> Config.blockCountDisplay),
            new Entry("Custom Cape", () -> Config.customCape),
            new Entry("Damage Numbers", () -> Config.damageNumbers),
            new Entry("Digging Status", () -> Config.diggingStatus),
            new Entry("Dynamic Island", () -> Config.dynamicIsland),
            new Entry("Dynamic Motion Blur", () -> Config.dynamicMotionBlur),
            new Entry("Elytra Improvements", () -> Config.elytraAssist),
            new Entry("Fall Damage Predict", () -> Config.fallDamagePredict),
            new Entry("Fake FPS", () -> Config.fakeFps),
            new Entry("Food Info", () -> Config.foodInfo),
            new Entry("Freelook", () -> Config.freelook),
            new Entry("Gamma Override", () -> Config.gammaOverride),
            new Entry("Hit Color", () -> Config.hitColor),
            new Entry("Hit Marker", () -> Config.hitMarker),
            new Entry("Hit Sound", () -> Config.hitSound),
            new Entry("Item Physics", () -> Config.itemPhysics),
            new Entry("Dropped Item 2D Render", () -> Config.item2DRender),
            new Entry("Item Use Status", () -> Config.itemUseStatus),
            new Entry("Keystrokes", () -> Config.keystrokes),
            new Entry("Low Health Warning", () -> Config.lowHealthNotify),
            new Entry("Motion camera", () -> Config.motionCamera),
            new Entry("Music Info HUD", () -> Config.musicInfoHud),
            new Entry("Name Tag", () -> Config.nameTag),
            new Entry("No Swimming", () -> Config.noSwimming),
            new Entry("Potion Status", () -> Config.potionStatus),
            new Entry("Rainbow Enchantment Glint", () -> Config.customEnchantmentGlint),
            new Entry("Remove Container Background", () -> Config.removeContainerBackground),
            new Entry("Remove Attack Cooldown Animation", () -> Config.noAttackCooldownAnimation),
            new Entry("Sneak Animation Adjustment", () -> Config.noSneakAnimation),
            new Entry("Sword Blocking Animation", () -> Config.swordBlock && !SkinManager.hasSwordSkinActive()),
            new Entry("Time Change", () -> Config.timeChange),
            new Entry("Victory Sound", () -> Config.victorySound),
            new Entry("Weather Change", () -> Config.weatherChange),
            new Entry("Zoom", () -> Config.zoom)
    );

    public static ArraylistRenderer getInstance() {
        return INSTANCE;
    }

    public void render(GuiGraphics graphics) {
        if (isNewStyle()) return;
        Minecraft client = Minecraft.getInstance();
        if (!Config.arraylist) return;
        if (client.player == null || client.options.hideGui) return;
        if (client.screen != null && !(client.screen instanceof ChatScreen) && !HudEditOverlay.getInstance().isActive()) return;

        List<String> names = activeNames();
        if (names.isEmpty() && HudEditOverlay.getInstance().isActive()) {
            names = previewNames();
        }
        if (names.isEmpty()) return;

        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        float scale = getScale();
        float x = getRenderX(screenW);
        float y = getRenderY(screenH);
        boolean alignRight = x + getEditWidth() * 0.5f >= screenW * 0.5f;

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);

        int shadowColor = com.pvp_utils.client.gui.clickgui.theme.UiTheme.current().isDark() ? 0x66000000 : 0x33000000;
        int width = Math.round(baseWidth(names));
        drawBackground(graphics, client, names, width, alignRight);
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            int textW = textWidth(name);
            int textX = alignRight ? width - textW - PADDING_X : PADDING_X;
            int textY = i * LINE_HEIGHT;
            int color = textColor(i, names.size());
            graphics.drawString(client.font, name, textX + 1, textY + 1, shadowColor, false);
            graphics.drawString(client.font, name, textX, textY, color, false);
        }

        graphics.pose().popMatrix();
    }

    public void renderChroma(Canvas canvas) {
        if (!Config.arraylist || !isNewStyle()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.options.hideGui) return;
        if (client.screen != null && !(client.screen instanceof ChatScreen)
                && !HudEditOverlay.getInstance().isActive()) return;
        List<NewRow> rows = newRows();
        if (rows.isEmpty()) return;
        drawNew(canvas, rows, newWidth(rows),
                getRenderX(client.getWindow().getGuiScaledWidth()),
                getRenderY(client.getWindow().getGuiScaledHeight()), getScale());
    }

    public float getEditWidth() {
        if (isNewStyle()) return newWidth(newRows()) * getScale();
        return (baseWidth(activeOrPreviewNames()) + borderOutset() * 2f) * getScale();
    }

    public float getEditHeight() {
        if (isNewStyle()) return newRows().size() * NEW_LINE_HEIGHT * getScale();
        return (Math.max(PREVIEW_HEIGHT, activeOrPreviewNames().size() * LINE_HEIGHT) + borderOutset() * 2f) * getScale();
    }

    public float getEditX(int screenW) {
        return getRenderX(screenW) - getScaledBorderOutset();
    }

    public float getEditY(int screenH) {
        return getRenderY(screenH) - getScaledBorderOutset();
    }

    public float getScaledBorderOutset() {
        return borderOutset() * getScale();
    }

    public float getDefaultX(int screenW) {
        if (isNewStyle()) return screenW - getEditWidth() - 3f;
        return 8f;
    }

    public float getDefaultY() {
        if (isNewStyle()) return 3f;
        return 28f;
    }

    public float getRenderX(int screenW) {
        if (isNewStyle()) return clamp(getDefaultX(screenW) + Config.arraylistX, 0f, Math.max(0f, screenW - getEditWidth()));
        float scale = getScale();
        float contentW = baseWidth(activeOrPreviewNames()) * scale;
        float outset = borderOutset() * scale;
        return clamp(getDefaultX(screenW) + Config.arraylistX, outset, Math.max(outset, screenW - contentW - outset));
    }

    public float getRenderY(int screenH) {
        if (isNewStyle()) return clamp(getDefaultY() + Config.arraylistY, 0f, Math.max(0f, screenH - getEditHeight()));
        float scale = getScale();
        float contentH = Math.max(PREVIEW_HEIGHT, activeOrPreviewNames().size() * LINE_HEIGHT) * scale;
        float outset = borderOutset() * scale;
        return clamp(getDefaultY() + Config.arraylistY, outset, Math.max(outset, screenH - contentH - outset));
    }

    private List<String> activeNames() {
        ArrayList<String> names = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            if (entry.enabled().getAsBoolean()) {
                names.add(entry.name());
            }
        }
        names.sort(Comparator.comparingInt(this::textWidth).reversed().thenComparing(String::compareTo));
        return names;
    }

    private List<String> activeOrPreviewNames() {
        List<String> names = activeNames();
        return names.isEmpty() ? previewNames() : names;
    }

    private List<String> previewNames() {
        return List.of("Dynamic Motion Blur", "Better Scoreboard", "Keystrokes", "Zoom");
    }

    private float baseWidth(List<String> names) {
        int max = PREVIEW_WIDTH;
        for (String name : names) {
            max = Math.max(max, textWidth(name) + PADDING_X * 2);
        }
        return max;
    }

    private int textWidth(String text) {
        return Minecraft.getInstance().font.width(text);
    }

    private void drawBackground(GuiGraphics graphics, Minecraft client, List<String> names, int width, boolean alignRight) {
        if (Config.arraylistBackground) {
            for (int i = 0; i < names.size(); i++) {
                Bounds bounds = innerBoundsFor(client, names, width, alignRight, i);
                graphics.fill(bounds.x1(), bounds.y1(), bounds.x2(), bounds.y2(), BACKGROUND_COLOR);
            }
        }
        drawBorder(graphics, client, names, width, alignRight);
    }

    private void drawBorder(GuiGraphics graphics, Minecraft client, List<String> names, int width, boolean alignRight) {
        if (!Config.arraylistBorder) return;
        int borderWidth = borderOutset();
        for (int i = 0; i < names.size(); i++) {
            Bounds outer = boundsFor(client, names, width, alignRight, i);
            Bounds inner = innerBoundsFor(client, names, width, alignRight, i);
            int color = textColor(i, names.size());

            fillRect(graphics, outer.x1(), outer.y1(), inner.x1(), outer.y2(), color);
            fillRect(graphics, inner.x2(), outer.y1(), outer.x2(), outer.y2(), color);
            if (i == 0) {
                fillRect(graphics, inner.x1(), outer.y1(), inner.x2(), inner.y1(), color);
            }
            if (i == names.size() - 1) {
                fillRect(graphics, inner.x1(), inner.y2(), inner.x2(), outer.y2(), color);
            }
            if (i + 1 < names.size()) {
                Bounds nextOuter = boundsFor(client, names, width, alignRight, i + 1);
                Bounds nextInner = innerBoundsFor(client, names, width, alignRight, i + 1);
                int sharedY1 = inner.y2() - borderWidth;
                int sharedY2 = inner.y2();
                fillRect(graphics, Math.min(inner.x1(), nextInner.x1()), sharedY1, Math.max(inner.x1(), nextInner.x1()), sharedY2, color);
                fillRect(graphics, Math.min(inner.x2(), nextInner.x2()), sharedY1, Math.max(inner.x2(), nextInner.x2()), sharedY2, color);
                fillRect(graphics, Math.min(outer.x1(), nextOuter.x1()), sharedY1, Math.max(outer.x1(), nextOuter.x1()), sharedY2, color);
                fillRect(graphics, Math.min(outer.x2(), nextOuter.x2()), sharedY1, Math.max(outer.x2(), nextOuter.x2()), sharedY2, color);
            }
        }
    }

    private void fillRect(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        if (x2 <= x1 || y2 <= y1) return;
        graphics.fill(x1, y1, x2, y2, color);
    }

    private Bounds boundsFor(Minecraft client, List<String> names, int width, boolean alignRight, int index) {
        int outset = borderOutset();
        String name = names.get(index);
        int textW = textWidth(name);
        int textX = alignRight ? width - textW - PADDING_X : PADDING_X;
        int bgX1 = textX - BACKGROUND_PADDING_X - outset;
        int bgX2 = textX + textW + BACKGROUND_PADDING_X + outset;
        int bgY1 = index * LINE_HEIGHT - (index == 0 ? BACKGROUND_TOP_PADDING + outset : 0);
        int bgY2 = (index + 1) * LINE_HEIGHT + (index == names.size() - 1 ? outset : 0);
        return new Bounds(bgX1, bgY1, bgX2, bgY2);
    }

    private Bounds innerBoundsFor(Minecraft client, List<String> names, int width, boolean alignRight, int index) {
        Bounds outer = boundsFor(client, names, width, alignRight, index);
        int borderWidth = borderOutset();
        if (borderWidth <= 0) {
            return outer;
        }
        int x1 = outer.x1() + borderWidth;
        int x2 = outer.x2() - borderWidth;
        int y1 = outer.y1() + (index == 0 ? borderWidth : 0);
        int y2 = outer.y2() - (index == names.size() - 1 ? borderWidth : 0);
        return new Bounds(x1, y1, x2, y2);
    }

    private int borderOutset() {
        if (isNewStyle()) return 0;
        return Config.arraylistBorder ? Math.max(1, Math.round(Config.arraylistBorderWidth)) : 0;
    }

    private boolean isNewStyle() {
        return Config.arraylistStyle == Config.ArraylistStyle.NEW;
    }

    private List<NewRow> newRows() {
        List<NewRow> rows = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            if (entry.enabled().getAsBoolean()) rows.add(newRow(entry.name(), suffixFor(entry.name())));
        }
        if (rows.isEmpty() && HudEditOverlay.getInstance().isActive()) {
            for (String name : previewNames()) rows.add(newRow(name, suffixFor(name)));
        }
        rows.sort(Comparator.comparingDouble(NewRow::width).reversed().thenComparing(NewRow::name));
        return rows;
    }

    private NewRow newRow(String name, String suffix) {
        String detail = suffix.isEmpty() ? "" : " " + suffix;
        float nameWidth = ChromaDraw.boldTextWidth(name, NEW_TEXT_SIZE);
        return new NewRow(name, detail, nameWidth,
                nameWidth + ChromaDraw.textWidth(detail, NEW_TEXT_SIZE));
    }

    private float newWidth(List<NewRow> rows) {
        float width = 0f;
        for (NewRow row : rows) width = Math.max(width, row.width());
        return (float) Math.ceil(width + NEW_PADDING * 2f + NEW_ACCENT_WIDTH);
    }

    private String suffixFor(String name) {
        return switch (name) {
            case "Arraylist" -> "New";
            case "Armor HUD" -> modeLabel(Config.armorHudMode);
            case "Item Use Status" -> modeLabel(Config.itemUseStatusMode);
            case "Keystrokes" -> modeLabel(Config.keystrokesMode);
            case "Music Info HUD" -> modeLabel(Config.musicInfoHudMode);
            case "Potion Status" -> modeLabel(Config.potionStatusStyle);
            case "Weather Change" -> modeLabel(Config.weatherMode);
            case "Freelook" -> modeLabel(Config.freelookTriggerMode);
            case "Time Change" -> Integer.toString(Config.clientTime);
            case "Zoom" -> Config.zoomAmount + "x";
            case "Auto GG" -> Config.autoGGDelayTicks * 50 + " ms";
            case "Auto Main Hand" -> Config.mainHandAssistSwitchDelayTicks * 50 + " ms";
            case "Gamma Override" -> String.format(Locale.ROOT, "%.1f", Config.gammaValue);
            case "Sword Blocking Animation" -> switch (Config.animationMode) {
                case MODE_1_7 -> "1.7";
                case MODE_PUSH -> "Push";
                case MODE_1_7_PLUS -> "1.7+";
                case MODE_NEW -> "New";
            };
            default -> "";
        };
    }

    private String modeLabel(Enum<?> mode) {
        if (mode == null) return "";
        String value = mode.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private void drawNew(Canvas canvas, List<NewRow> rows, float width, float x, float y, float scale) {
        boolean alignRight = x + width * scale * 0.5f
                >= Minecraft.getInstance().getWindow().getGuiScaledWidth() * 0.5f;
        int save = canvas.save();
        try {
            canvas.translate(x, y);
            canvas.scale(scale, scale);
            float right = width - NEW_ACCENT_WIDTH;
            int[] colors = new int[rows.size()];
            for (int i = 0; i < rows.size(); i++) {
                colors[i] = textColor(i, rows.size());
            }
            if (Config.arraylistBackground) {
                int backgroundSave = canvas.save();
                try {
                    if (!alignRight) {
                        canvas.translate(width, 0f);
                        canvas.scale(-1f, 1f);
                    }
                    drawNewBackground(canvas, rows, right);
                } finally {
                    canvas.restoreToCount(backgroundSave);
                }
            }
            if (Config.arraylistTextGlow) {
                ChromaDraw.glowLayer(canvas, 0f, 0f, width, rows.size() * NEW_LINE_HEIGHT,
                        5f, 5, () -> drawNewText(canvas, rows, width, alignRight, colors, true));
            }
            drawNewText(canvas, rows, width, alignRight, colors, false);
            float accentX = alignRight ? right : 0f;
            for (int i = 0; i < rows.size(); i++) {
                ChromaDraw.gradient(canvas, accentX, i * NEW_LINE_HEIGHT, NEW_ACCENT_WIDTH,
                        NEW_LINE_HEIGHT, textColor(i - 0.5f, rows.size()),
                        textColor(i + 0.5f, rows.size()), true, 0f);
            }
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void drawNewBackground(Canvas canvas, List<NewRow> rows, float right) {
        if (rows.isEmpty()) return;
        if (!Config.arraylistBackgroundRounded) {
            for (int i = 0; i < rows.size(); i++) {
                float rowWidth = rows.get(i).width() + NEW_PADDING * 2f;
                ChromaDraw.fill(canvas, right - rowWidth, i * NEW_LINE_HEIGHT,
                        rowWidth, NEW_LINE_HEIGHT, NEW_BACKGROUND_COLOR);
            }
            return;
        }
        List<float[]> corners = new ArrayList<>();
        corners.add(new float[]{right, 0f});
        corners.add(new float[]{right, rows.size() * NEW_LINE_HEIGHT});
        for (int i = rows.size() - 1; i >= 0; i--) {
            float left = right - rows.get(i).width() - NEW_PADDING * 2f;
            corners.add(new float[]{left, (i + 1) * NEW_LINE_HEIGHT});
            corners.add(new float[]{left, i * NEW_LINE_HEIGHT});
        }
        try (PathBuilder builder = new PathBuilder()) {
            builder.moveTo(corners.getFirst()[0], corners.getFirst()[1]);
            for (int i = 1; i < corners.size(); i++) {
                float[] previous = corners.get(i - 1);
                float[] corner = corners.get(i);
                float[] next = corners.get((i + 1) % corners.size());
                float before = (float) Math.hypot(previous[0] - corner[0], previous[1] - corner[1]);
                float after = (float) Math.hypot(next[0] - corner[0], next[1] - corner[1]);
                boolean lineSide = Math.abs(corner[0] - right) < 0.001f;
                float radius = lineSide ? 0f
                        : Math.min(NEW_CORNER_RADIUS, Math.min(before, after) * 0.5f);
                if (radius <= 0.01f) {
                    builder.lineTo(corner[0], corner[1]);
                    continue;
                }
                float startX = corner[0] + (previous[0] - corner[0]) * radius / Math.max(before, 0.001f);
                float startY = corner[1] + (previous[1] - corner[1]) * radius / Math.max(before, 0.001f);
                float endX = corner[0] + (next[0] - corner[0]) * radius / Math.max(after, 0.001f);
                float endY = corner[1] + (next[1] - corner[1]) * radius / Math.max(after, 0.001f);
                builder.lineTo(startX, startY);
                builder.quadTo(corner[0], corner[1], endX, endY);
            }
            try (Path path = builder.closePath().detach()) {
                ChromaDraw.fillPath(canvas, path, NEW_BACKGROUND_COLOR);
            }
        }
    }

    private void drawNewText(Canvas canvas, List<NewRow> rows, float width, boolean alignRight,
                             int[] colors, boolean glowOnly) {
        for (int i = 0; i < rows.size(); i++) {
            NewRow row = rows.get(i);
            float top = i * NEW_LINE_HEIGHT;
            float textX = alignRight ? width - NEW_ACCENT_WIDTH - NEW_PADDING - row.width()
                    : NEW_ACCENT_WIDTH + NEW_PADDING;
            if (glowOnly) {
                ChromaDraw.boldText(canvas, row.name(), textX, top, NEW_LINE_HEIGHT,
                        colors[i], NEW_TEXT_SIZE);
                continue;
            }
            drawBoldText(canvas, row.name(), textX, top, colors[i]);
            if (!row.suffix().isEmpty()) {
                ChromaDraw.text(canvas, row.suffix(), textX + row.nameWidth(), top,
                        NEW_LINE_HEIGHT, NEW_SUFFIX_COLOR, NEW_TEXT_SIZE);
            }
        }
    }

    private void drawBoldText(Canvas canvas, String text, float x, float top, int color) {
        if (text.isEmpty()) return;
        ChromaDraw.boldText(canvas, text, x, top, NEW_LINE_HEIGHT, color, NEW_TEXT_SIZE);
        ChromaDraw.boldText(canvas, text, x + NEW_BOLD_OFFSET, top, NEW_LINE_HEIGHT,
                color, NEW_TEXT_SIZE);
    }

    private int textColor(float index, int count) {
        int first = rgb(Config.arraylistColorRed, Config.arraylistColorGreen, Config.arraylistColorBlue);
        if (!Config.arraylistGradient) {
            return first;
        }
        int second = rgb(Config.arraylistGradientRed, Config.arraylistGradientGreen, Config.arraylistGradientBlue);
        float speed = Math.max(0.0f, Config.arraylistGradientSpeed);
        float y = index * LINE_HEIGHT + LINE_HEIGHT * 0.5f;
        float period = Math.max(LINE_HEIGHT * 6.0f, count * LINE_HEIGHT * 0.75f);
        float seconds = (System.nanoTime() - START_TIME_NANOS) / 1_000_000_000.0f;
        float offset = seconds * speed * LINE_HEIGHT * 3.0f;
        float phase = positiveModulo(y + offset, period) / period;
        float t = phase < 0.5f ? phase * 2.0f : (1.0f - phase) * 2.0f;
        t = smoothstep(t);
        return lerpColor(first, second, t);
    }

    private float positiveModulo(float value, float modulo) {
        float result = value % modulo;
        return result < 0f ? result + modulo : result;
    }

    private float smoothstep(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    private int rgb(int red, int green, int blue) {
        return 0xFF000000
                | (clampColor(red) << 16)
                | (clampColor(green) << 8)
                | clampColor(blue);
    }

    private int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int r = Math.round(ar + (br - ar) * t);
        int g = Math.round(ag + (bg - ag) * t);
        int bl = Math.round(ab + (bb - ab) * t);
        return rgb(r, g, bl);
    }

    private int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private float getScale() {
        return Math.max(0.5f, Config.arraylistScale);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Entry(String name, BooleanSupplier enabled) {
    }

    private record NewRow(String name, String suffix, float nameWidth, float width) {
    }

    private record Bounds(int x1, int y1, int x2, int y2) {
    }
}
