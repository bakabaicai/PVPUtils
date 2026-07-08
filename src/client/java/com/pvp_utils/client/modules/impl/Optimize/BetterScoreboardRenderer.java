package com.pvp_utils.client.modules.impl.Optimize;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Render.HudEditOverlay;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaBlurRenderer;
import com.pvp_utils.client.render.skia.SkiaRenderer;
import io.github.humbleui.skija.Canvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;

import java.util.ArrayList;
import java.util.List;

public final class BetterScoreboardRenderer {
    private static final BetterScoreboardRenderer INSTANCE = new BetterScoreboardRenderer();
    private static final float EXTRA_PAD = 7.0f;
    private static final float TITLE_SIZE = 10.0f;
    private static final float ROW_SIZE = 9.0f;
    private static final float ROW_HEIGHT = 9.0f;
    private static final float TITLE_HEIGHT = 10.0f;
    private static final float RADIUS = 9.0f;
    private static final float VISUAL_Y_OFFSET = 3.0f;
    private static final float TITLE_CENTER_FIX = -1.5f;

    private BetterScoreboardRenderer() {
    }

    public static BetterScoreboardRenderer getInstance() {
        return INSTANCE;
    }

    public void render(GuiGraphics graphics) {
        if (!Config.betterScoreboard || !Config.betterScoreboardVisualImprovement) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.options.hideGui) {
            return;
        }

        Objective objective = client.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
        boolean editActive = HudEditOverlay.getInstance().isActive();
        if (client.screen != null && !editActive) {
            return;
        }
        if (objective == null && !editActive) {
            return;
        }

        Component titleComponent = objective == null ? Component.literal("Better Scoreboard") : objective.getDisplayName();
        String title = objective == null ? "Better Scoreboard" : BetterScoreboardManager.encodeStyled(titleComponent);
        boolean titleFormatted = objective != null && BetterScoreboardManager.needsNativeRendering(titleComponent);
        List<BetterScoreboardManager.Row> rows = objective == null
                ? List.of(new BetterScoreboardManager.Row("Player", "10"), new BetterScoreboardManager.Row("Kills", "2"))
                : BetterScoreboardManager.getRows(objective);
        if (editActive && rows.isEmpty()) {
            rows = List.of(new BetterScoreboardManager.Row("Player", "10"), new BetterScoreboardManager.Row("Kills", "2"));
        }
        if (rows.isEmpty() && !editActive) {
            return;
        }

        int guiW = client.getWindow().getGuiScaledWidth();
        int guiH = client.getWindow().getGuiScaledHeight();
        BetterScoreboardManager.Rect base = BetterScoreboardManager.getCurrentRect(guiW, guiH);
        float scale = BetterScoreboardManager.getScale();
        float contentX = base.x() + Config.betterScoreboardX;
        float contentY = base.y() + Config.betterScoreboardY + VISUAL_Y_OFFSET * scale;
        float contentW = Math.max(96.0f, base.w()) * scale;
        float contentH = Math.max(28.0f, base.h()) * scale;
        float bgPad = EXTRA_PAD * scale;
        float w = contentW + bgPad * 2.0f;
        float h = contentH + bgPad * 2.0f;
        float x = clamp(contentX - bgPad, 0.0f, Math.max(0.0f, guiW - w));
        float y = clamp(contentY - bgPad, 0.0f, Math.max(0.0f, guiH - h));

        SkiaBlurRenderer.getInstance().render(client, x, y, w, h, RADIUS * scale, Config.skiaBlurTintColor(), Config.skiaBlurStrength);
        Canvas canvas = SkiaRenderer.beginRegion((int) Math.floor(x), (int) Math.floor(y), (int) Math.ceil(w), (int) Math.ceil(h));
        if (canvas == null) {
            return;
        }

        ArrayList<NativeText> nativeTexts = new ArrayList<>();
        try {
            drawCard(canvas, nativeTexts, x, y, w, h, bgPad, titleComponent, title, titleFormatted, rows, scale);
        } finally {
            SkiaRenderer.endRegion(graphics);
        }
        drawNativeTexts(graphics, client, nativeTexts);
    }

    private void drawCard(Canvas canvas, List<NativeText> nativeTexts, float x, float y, float w, float h, float bgPad,
                          Component titleComponent, String title, boolean titleFormatted,
                          List<BetterScoreboardManager.Row> rows, float scale) {
        int titleColor = Config.hudPrimaryTextColor();
        int textColor = Config.hudMutedTextColor();
        int scoreColor = Config.hudTheme == Config.HudTheme.LIGHT ? 0xFFE44848 : 0xFFFF6B6B;

        float contentX = x + bgPad;
        float contentY = y + bgPad;
        float contentW = w - bgPad * 2.0f;
        float textSize = ROW_SIZE * scale;
        float titleSize = TITLE_SIZE * scale;
        String displayTitle = trimFormattedToWidth(title, contentW, titleSize);
        Component nativeTitle = nativeComponent(titleComponent, displayTitle, titleColor);
        float titleW = measureText(nativeTitle, displayTitle, titleSize, titleFormatted);
        drawText(canvas, nativeTexts, nativeTitle, displayTitle, titleFormatted,
                contentX + (contentW - titleW) * 0.5f + TITLE_CENTER_FIX * scale,
                contentY + 5.0f * scale, titleSize, titleColor);

        float rowY = contentY + TITLE_HEIGHT * scale + 6.0f * scale;
        int maxRows = Math.min(rows.size(), 15);
        for (int i = 0; i < maxRows; i++) {
            BetterScoreboardManager.Row row = rows.get(i);
            String score = row.score();
            boolean nameNative = shouldUseNative(row.nameComponent(), row.name(), row.nameFormatted());
            boolean scoreNative = shouldUseNative(row.scoreComponent(), score, row.scoreFormatted());
            Component nativeName = nativeComponent(row.nameComponent(), row.name(), textColor);
            Component nativeScore = nativeComponent(row.scoreComponent(), score, scoreColor);
            float scoreW = Config.betterScoreboardHideScores ? 0.0f : measureText(nativeScore, score, textSize, scoreNative);
            float availableNameW = contentW - scoreW - (Config.betterScoreboardHideScores ? 0.0f : 8.0f * scale);
            String displayName = nameNative ? row.name() : trimFormattedToWidth(row.name(), availableNameW, textSize);
            drawText(canvas, nativeTexts, nativeName, displayName, nameNative, contentX, rowY, textSize, textColor);
            if (!Config.betterScoreboardHideScores) {
                drawText(canvas, nativeTexts, nativeScore, score, scoreNative, contentX + contentW - scoreW, rowY, textSize, scoreColor);
            }
            rowY += ROW_HEIGHT * scale;
        }
    }

    private void drawText(Canvas canvas, List<NativeText> nativeTexts, Component nativeComponent, String text, boolean nativeRender,
                          float x, float y, float size, int color) {
        if (nativeRender) {
            nativeTexts.add(new NativeText(nativeComponent, x, y, size / 9.0f, color));
        } else {
            FontRenderer.drawText(canvas, stripLegacyCodes(text), x, y, size, color);
        }
    }

    private float measureText(Component nativeComponent, String text, float size, boolean nativeRender) {
        if (nativeRender) {
            return Minecraft.getInstance().font.width(nativeComponent) * (size / 9.0f);
        }
        return FontRenderer.measureTextWidth(stripLegacyCodes(text), size);
    }

    private boolean shouldUseNative(Component component, String text, boolean formatted) {
        return formatted
                || BetterScoreboardManager.containsLegacyCodes(text)
                || BetterScoreboardManager.needsNativeRendering(component);
    }

    private void drawNativeTexts(GuiGraphics graphics, Minecraft client, List<NativeText> texts) {
        for (NativeText text : texts) {
            drawNativeComponent(graphics, client, text.component(), text.x(), text.y(), text.scale(), text.fallbackColor());
        }
    }

    private void drawNativeComponent(GuiGraphics graphics, Minecraft client, Component component, float x, float y, float scale, int fallbackColor) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.drawString(client.font, component, 0, 0, fallbackColor, false);
        graphics.pose().popMatrix();
    }

    private Component nativeComponent(Component original, String legacyText, int fallbackColor) {
        if (BetterScoreboardManager.containsLegacyCodes(legacyText)) {
            return legacyToComponent(legacyText, fallbackColor);
        }
        return original == null ? Component.literal(stripLegacyCodes(legacyText)).withStyle(Style.EMPTY.withColor(fallbackColor)) : original;
    }

    private Component legacyToComponent(String text, int fallbackColor) {
        MutableComponent root = Component.empty();
        StringBuilder current = new StringBuilder();
        Style style = Style.EMPTY.withColor(fallbackColor);
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if ((ch == '\u00A7' || ch == '&') && i + 1 < text.length()) {
                if (Character.toLowerCase(text.charAt(i + 1)) == 'x') {
                    Integer hexColor = parseHexColor(text, i);
                    if (hexColor != null) {
                        appendNativeSegment(root, current, style);
                        style = style.withColor(hexColor & 0xFFFFFF);
                        i += 13;
                        continue;
                    }
                }
                char code = Character.toLowerCase(text.charAt(++i));
                Integer color = minecraftColor(code, fallbackColor);
                if (color != null) {
                    appendNativeSegment(root, current, style);
                    style = Style.EMPTY.withColor(color & 0xFFFFFF);
                    continue;
                }
                appendNativeSegment(root, current, style);
                style = switch (code) {
                    case 'l' -> style.withBold(true);
                    case 'o' -> style.withItalic(true);
                    case 'n' -> style.withUnderlined(true);
                    case 'm' -> style.withStrikethrough(true);
                    case 'k' -> style.withObfuscated(true);
                    case 'r' -> Style.EMPTY.withColor(fallbackColor);
                    default -> style;
                };
                continue;
            }
            current.append(ch);
        }
        appendNativeSegment(root, current, style);
        return root;
    }

    private void appendNativeSegment(MutableComponent root, StringBuilder text, Style style) {
        if (text.isEmpty()) return;
        root.append(Component.literal(text.toString()).withStyle(style));
        text.setLength(0);
    }

    private String trimFormattedToWidth(String text, float maxWidth, float size) {
        if (FontRenderer.measureTextWidth(stripLegacyCodes(text), size) <= maxWidth) {
            return text;
        }
        String result = stripLegacyCodes(text);
        while (result.length() > 1 && FontRenderer.measureTextWidth(result + "...", size) > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "...";
    }

    private String stripLegacyCodes(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder clean = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if ((ch == '\u00A7' || ch == '&') && i + 1 < text.length()) {
                if (Character.toLowerCase(text.charAt(i + 1)) == 'x' && parseHexColor(text, i) != null) {
                    i += 13;
                } else {
                    i++;
                }
                continue;
            }
            clean.append(ch);
        }
        return clean.toString();
    }

    private Integer parseHexColor(String text, int sectionIndex) {
        if (sectionIndex + 13 >= text.length()) return null;
        char marker = text.charAt(sectionIndex);
        StringBuilder hex = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            int prefix = sectionIndex + 2 + i * 2;
            int value = prefix + 1;
            if (text.charAt(prefix) != marker || !isHex(text.charAt(value))) {
                return null;
            }
            hex.append(text.charAt(value));
        }
        try {
            return 0xFF000000 | Integer.parseInt(hex.toString(), 16);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean isHex(char value) {
        return (value >= '0' && value <= '9') || (value >= 'a' && value <= 'f') || (value >= 'A' && value <= 'F');
    }

    private Integer minecraftColor(char code, int fallbackColor) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> 0xFF000000;
            case '1' -> 0xFF0000AA;
            case '2' -> 0xFF00AA00;
            case '3' -> 0xFF00AAAA;
            case '4' -> 0xFFAA0000;
            case '5' -> 0xFFAA00AA;
            case '6' -> 0xFFFFAA00;
            case '7' -> 0xFFAAAAAA;
            case '8' -> 0xFF555555;
            case '9' -> 0xFF5555FF;
            case 'a' -> 0xFF55FF55;
            case 'b' -> 0xFF55FFFF;
            case 'c' -> 0xFFFF5555;
            case 'd' -> 0xFFFF55FF;
            case 'e' -> 0xFFFFFF55;
            case 'f' -> 0xFFFFFFFF;
            case 'r' -> fallbackColor;
            default -> null;
        };
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private record NativeText(Component component, float x, float y, float scale, int fallbackColor) {
    }
}
