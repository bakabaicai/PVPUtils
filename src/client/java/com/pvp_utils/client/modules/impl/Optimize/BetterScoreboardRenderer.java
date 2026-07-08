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

        ArrayList<NativeGlyph> nativeGlyphs = new ArrayList<>();
        try {
            drawCard(canvas, nativeGlyphs, x, y, w, h, bgPad, title, rows, scale);
        } finally {
            SkiaRenderer.endRegion(graphics);
        }
        drawNativeGlyphs(graphics, client, nativeGlyphs);
    }

    private void drawCard(Canvas canvas, List<NativeGlyph> nativeGlyphs, float x, float y, float w, float h, float bgPad, String title, List<BetterScoreboardManager.Row> rows, float scale) {
        int titleColor = Config.hudPrimaryTextColor();
        int textColor = Config.hudMutedTextColor();
        int scoreColor = Config.hudTheme == Config.HudTheme.LIGHT ? 0xFFE44848 : 0xFFFF6B6B;

        float contentX = x + bgPad;
        float contentY = y + bgPad;
        float contentW = w - bgPad * 2.0f;
        float textSize = ROW_SIZE * scale;
        float titleSize = TITLE_SIZE * scale;
        String displayTitle = trimFormattedToWidth(title, contentW, titleSize);
        float titleW = measureMixedText(displayTitle, titleSize);
        drawMixedText(canvas, nativeGlyphs, displayTitle, contentX + (contentW - titleW) * 0.5f + TITLE_CENTER_FIX * scale, contentY + 5.0f * scale, titleSize, titleColor);

        float rowY = contentY + TITLE_HEIGHT * scale + 6.0f * scale;
        int maxRows = Math.min(rows.size(), 15);
        for (int i = 0; i < maxRows; i++) {
            BetterScoreboardManager.Row row = rows.get(i);
            String score = row.score();
            float scoreW = Config.betterScoreboardHideScores ? 0.0f : measureMixedText(score, textSize);
            float availableNameW = contentW - scoreW - (Config.betterScoreboardHideScores ? 0.0f : 8.0f * scale);
            drawMixedText(canvas, nativeGlyphs, trimFormattedToWidth(row.name(), availableNameW, textSize), contentX, rowY, textSize, textColor);
            if (!Config.betterScoreboardHideScores) {
                drawMixedText(canvas, nativeGlyphs, score, contentX + contentW - scoreW, rowY, textSize, scoreColor);
            }
            rowY += ROW_HEIGHT * scale;
        }
    }

    private void drawNativeFormattedText(GuiGraphics graphics, Minecraft client, float x, float y, float w, float bgPad,
                                         Component titleComponent, String titleText, boolean titleFormatted,
                                         List<BetterScoreboardManager.Row> rows, float scale) {
        float contentX = x + bgPad;
        float contentY = y + bgPad;
        float contentW = w - bgPad * 2.0f;
        if (titleFormatted) {
            Component title = nativeComponent(titleComponent, titleText, Config.hudPrimaryTextColor());
            float titleW = client.font.width(title) * scale;
            drawNativeComponent(graphics, client, title, contentX + (contentW - titleW) * 0.5f + TITLE_CENTER_FIX * scale, contentY + 5.0f * scale, scale, Config.hudPrimaryTextColor());
        }

        float rowY = contentY + TITLE_HEIGHT * scale + 6.0f * scale;
        int maxRows = Math.min(rows.size(), 15);
        for (int i = 0; i < maxRows; i++) {
            BetterScoreboardManager.Row row = rows.get(i);
            Component scoreComponent = nativeComponent(row.scoreComponent(), row.score(), Config.hudTheme == Config.HudTheme.LIGHT ? 0xFFE44848 : 0xFFFF6B6B);
            float scoreW = Config.betterScoreboardHideScores ? 0.0f : client.font.width(scoreComponent) * scale;
            float availableNameW = contentW - scoreW - (Config.betterScoreboardHideScores ? 0.0f : 8.0f * scale);
            if (row.nameFormatted()) {
                Component name = nativeComponent(row.nameComponent(), trimFormattedToWidth(row.name(), availableNameW, ROW_SIZE * scale), Config.hudMutedTextColor());
                drawNativeComponent(graphics, client, name, contentX, rowY, scale, Config.hudMutedTextColor());
            }
            if (!Config.betterScoreboardHideScores && row.scoreFormatted()) {
                drawNativeComponent(graphics, client, scoreComponent, contentX + contentW - scoreW, rowY, scale, Config.hudTheme == Config.HudTheme.LIGHT ? 0xFFE44848 : 0xFFFF6B6B);
            }
            rowY += ROW_HEIGHT * scale;
        }
    }

    private void drawMixedText(Canvas canvas, List<NativeGlyph> nativeGlyphs, String text, float x, float y, float size, int fallbackColor) {
        float cursor = x;
        int color = fallbackColor;
        StringBuilder buffer = new StringBuilder();
        for (int offset = 0; offset < text.length(); ) {
            int cp = text.codePointAt(offset);
            int charCount = Character.charCount(cp);
            if ((cp == '\u00A7' || cp == '&') && offset + charCount < text.length()) {
                int nextOffset = offset + charCount;
                char code = text.charAt(nextOffset);
                if (Character.toLowerCase(code) == 'x') {
                    Integer hexColor = parseHexColor(text, offset);
                    if (hexColor != null) {
                        cursor = flushMixedText(canvas, buffer, cursor, y, size, color);
                        color = hexColor;
                        offset += 14;
                        continue;
                    }
                }
                Integer parsed = minecraftColor(code, fallbackColor);
                if (parsed != null) {
                    cursor = flushMixedText(canvas, buffer, cursor, y, size, color);
                    color = parsed;
                }
                offset = nextOffset + 1;
                continue;
            }
            if (isNativeSymbol(cp)) {
                cursor = flushMixedText(canvas, buffer, cursor, y, size, color);
                String symbol = new String(Character.toChars(cp));
                nativeGlyphs.add(new NativeGlyph(Component.literal(symbol).withStyle(Style.EMPTY.withColor(color & 0xFFFFFF)), cursor, y, size / 9.0f, color));
                cursor += Minecraft.getInstance().font.width(symbol) * (size / 9.0f);
            } else {
                buffer.appendCodePoint(cp);
            }
            offset += charCount;
        }
        flushMixedText(canvas, buffer, cursor, y, size, color);
    }

    private float flushMixedText(Canvas canvas, StringBuilder buffer, float x, float y, float size, int color) {
        if (buffer.isEmpty()) {
            return x;
        }
        String text = buffer.toString();
        FontRenderer.drawText(canvas, text, x, y, size, color);
        buffer.setLength(0);
        return x + FontRenderer.measureTextWidth(text, size);
    }

    private float measureMixedText(String text, float size) {
        float width = 0.0f;
        StringBuilder buffer = new StringBuilder();
        for (int offset = 0; offset < text.length(); ) {
            int cp = text.codePointAt(offset);
            int charCount = Character.charCount(cp);
            if ((cp == '\u00A7' || cp == '&') && offset + charCount < text.length()) {
                int nextOffset = offset + charCount;
                if (Character.toLowerCase(text.charAt(nextOffset)) == 'x' && parseHexColor(text, offset) != null) {
                    offset += 14;
                } else {
                    offset = nextOffset + 1;
                }
                continue;
            }
            if (isNativeSymbol(cp)) {
                if (!buffer.isEmpty()) {
                    width += FontRenderer.measureTextWidth(buffer.toString(), size);
                    buffer.setLength(0);
                }
                String symbol = new String(Character.toChars(cp));
                width += Minecraft.getInstance().font.width(symbol) * (size / 9.0f);
            } else {
                buffer.appendCodePoint(cp);
            }
            offset += charCount;
        }
        if (!buffer.isEmpty()) {
            width += FontRenderer.measureTextWidth(buffer.toString(), size);
        }
        return width;
    }

    private void drawNativeGlyphs(GuiGraphics graphics, Minecraft client, List<NativeGlyph> glyphs) {
        for (NativeGlyph glyph : glyphs) {
            drawNativeComponent(graphics, client, glyph.component(), glyph.x(), glyph.y(), glyph.scale(), glyph.fallbackColor());
        }
    }

    private boolean isNativeSymbol(int codePoint) {
        return Character.isSupplementaryCodePoint(codePoint)
                || Character.UnicodeBlock.of(codePoint) == Character.UnicodeBlock.PRIVATE_USE_AREA;
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
        if (measureMixedText(text, size) <= maxWidth) {
            return text;
        }
        String result = stripLegacyCodes(text);
        while (result.length() > 1 && FontRenderer.measureTextWidth(result + "...", size) > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "...";
    }

    private List<TextSegment> parseLegacySegments(String text, int fallbackColor) {
        ArrayList<TextSegment> segments = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return segments;
        }
        StringBuilder current = new StringBuilder();
        int color = fallbackColor;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if ((ch == '\u00A7' || ch == '&') && i + 1 < text.length()) {
                if (Character.toLowerCase(text.charAt(i + 1)) == 'x') {
                    Integer hexColor = parseHexColor(text, i);
                    if (hexColor != null) {
                        flushSegment(segments, current, color);
                        color = hexColor;
                        i += 13;
                        continue;
                    }
                }
                Integer parsedColor = minecraftColor(text.charAt(++i), fallbackColor);
                if (parsedColor != null) {
                    flushSegment(segments, current, color);
                    color = parsedColor;
                }
                // Style codes are intentionally ignored so all scoreboard text stays on the custom standard font.
                continue;
            }
            current.append(ch);
        }
        flushSegment(segments, current, color);
        return segments;
    }

    private void flushSegment(List<TextSegment> segments, StringBuilder text, int color) {
        if (text.isEmpty()) {
            return;
        }
        segments.add(new TextSegment(text.toString(), color));
        text.setLength(0);
    }

    private void drawSegments(Canvas canvas, List<TextSegment> segments, float x, float y, float size) {
        float cursor = x;
        for (TextSegment segment : segments) {
            FontRenderer.drawText(canvas, segment.text(), cursor, y, size, segment.color());
            cursor += FontRenderer.measureTextWidth(segment.text(), size);
        }
    }

    private float measureSegments(List<TextSegment> segments, float size) {
        float width = 0.0f;
        for (TextSegment segment : segments) {
            width += FontRenderer.measureTextWidth(segment.text(), size);
        }
        return width;
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

    private record TextSegment(String text, int color) {
    }

    private record NativeGlyph(Component component, float x, float y, float scale, int fallbackColor) {
    }
}
