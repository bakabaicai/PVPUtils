package com.pvp_utils.client.render.font;

import com.pvp_utils.client.render.skia.SkiaRenderer;
import io.github.humbleui.skija.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class FontRenderer {

    public static final String DEFAULT = "harmony";
    public static final String ICON = "icon";
    public static final String MATERIAL_SYMBOLS = "material_symbols";

    private static final Map<String, Typeface> typefaces = new HashMap<>();
    private static final FontMgr fontMgr = FontMgr.getDefault();
    private static final Map<String, Font> fonts = new HashMap<>();
    private static final Map<String, Float> widthCache = new HashMap<>();
    private static final Paint textPaint = new Paint().setAntiAlias(true);

    static {
        registerFromResources(DEFAULT, "/fonts/harmony.ttf");
        registerFromResources(ICON, "/fonts/icon.ttf");
        registerFromResources(MATERIAL_SYMBOLS, "/fonts/MaterialSymbolsRounded.ttf");
    }

    public static void registerFromResources(String name, String resourcePath) {
        try (InputStream is = FontRenderer.class.getResourceAsStream(resourcePath)) {
            if (is == null) throw new IOException("Font not found: " + resourcePath);
            byte[] data = is.readAllBytes();
            typefaces.put(name, fontMgr.makeFromData(Data.makeFromBytes(data)));
            clearCaches(name);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load font: " + resourcePath, e);
        }
    }

    public static void registerFromFile(String name, Path path) {
        typefaces.put(name, fontMgr.makeFromFile(path.toAbsolutePath().toString()));
        clearCaches(name);
    }

    private static Font makeFont(String fontName, float size) {
        String key = fontKey(fontName, size);
        Font font = fonts.get(key);
        if (font != null) return font;

        Typeface typeface = typefaces.getOrDefault(fontName, typefaces.get(DEFAULT));
        font = new Font(typeface, size);
        font.setSubpixel(true);
        font.setHinting(FontHinting.SLIGHT);
        font.setEdging(FontEdging.SUBPIXEL_ANTI_ALIAS);
        fonts.put(key, font);
        return font;
    }

    public static void drawText(Canvas canvas, String text, float x, float y, float size, int argb) {
        drawText(canvas, text, x, y, size, argb, DEFAULT);
    }

    public static void drawText(Canvas canvas, String text, float x, float y, float size, int argb, String fontName) {
        Font font = makeFont(fontName, size);
        textPaint.setColor(argb);
        canvas.drawString(text, x, y, font, textPaint);
        SkiaRenderer.feedRegionContent(text, x, y, size, argb, fontName);
    }

    public static void drawSegmented(Canvas canvas, Segment[] segments, float x, float y) {
        float cursor = x;
        for (Segment seg : segments) {
            drawText(canvas, seg.text, cursor, y, seg.size, seg.argb, seg.fontName);
            cursor += measureTextWidth(seg.text, seg.size, seg.fontName) + seg.gap;
        }
    }

    public static float measureTextWidth(String text, float size) {
        return measureTextWidth(text, size, DEFAULT);
    }

    public static float measureTextWidth(String text, float size, String fontName) {
        if (text.length() <= 32) {
            String key = widthKey(fontName, size, text);
            Float cached = widthCache.get(key);
            if (cached != null) return cached;
            float width = makeFont(fontName, size).measureTextWidth(text);
            widthCache.put(key, width);
            return width;
        }
        return makeFont(fontName, size).measureTextWidth(text);
    }

    public static float getLineHeight(float size) {
        return getLineHeight(size, DEFAULT);
    }

    public static float getLineHeight(float size, String fontName) {
        FontMetrics m = makeFont(fontName, size).getMetrics();
        return -m.getAscent() + m.getDescent();
    }

    private static String fontKey(String fontName, float size) {
        return fontName + '#' + Float.floatToIntBits(size);
    }

    private static String widthKey(String fontName, float size, String text) {
        return fontKey(fontName, size) + '#' + text;
    }

    private static void clearCaches(String fontName) {
        fonts.keySet().removeIf(key -> key.startsWith(fontName + '#'));
        widthCache.keySet().removeIf(key -> key.startsWith(fontName + '#'));
    }

    public static class Segment {
        public final String text;
        public final float size;
        public final int argb;
        public final String fontName;
        public final float gap;

        public Segment(String text, float size, int argb) { this(text, size, argb, DEFAULT, 0f); }
        public Segment(String text, float size, int argb, String fontName) { this(text, size, argb, fontName, 0f); }
        public Segment(String text, float size, int argb, String fontName, float gap) {
            this.text = text; this.size = size; this.argb = argb; this.fontName = fontName; this.gap = gap;
        }
    }
}
