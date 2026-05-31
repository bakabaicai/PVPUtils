package com.pvp_utils.client.render.font;

import io.github.humbleui.skija.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class FontRenderer {

    public static final String DEFAULT = "harmony";
    public static final String ICON = "icon";

    private static final Map<String, Typeface> typefaces = new HashMap<>();

    static {
        registerFromResources(DEFAULT, "/fonts/harmony.ttf");
        registerFromResources(ICON, "/fonts/icon.ttf");
    }

    public static void registerFromResources(String name, String resourcePath) {
        try (InputStream is = FontRenderer.class.getResourceAsStream(resourcePath)) {
            if (is == null) throw new IOException("Font not found: " + resourcePath);
            byte[] data = is.readAllBytes();
            typefaces.put(name, Typeface.makeFromData(Data.makeFromBytes(data)));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load font: " + resourcePath, e);
        }
    }

    public static void registerFromFile(String name, Path path) {
        typefaces.put(name, Typeface.makeFromFile(path.toAbsolutePath().toString()));
    }

    private static Font makeFont(String fontName, float size) {
        Typeface typeface = typefaces.getOrDefault(fontName, typefaces.get(DEFAULT));
        Font font = new Font(typeface, size);
        font.setSubpixel(true);
        font.setHinting(FontHinting.SLIGHT);
        font.setEdging(FontEdging.SUBPIXEL_ANTI_ALIAS);
        return font;
    }

    public static void drawText(Canvas canvas, String text, float x, float y, float size, int argb) {
        drawText(canvas, text, x, y, size, argb, DEFAULT);
    }

    public static void drawText(Canvas canvas, String text, float x, float y, float size, int argb, String fontName) {
        try (Font font = makeFont(fontName, size); Paint paint = new Paint()) {
            paint.setColor(argb);
            paint.setAntiAlias(true);
            canvas.drawString(text, x, y, font, paint);
        }
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
        try (Font font = makeFont(fontName, size)) {
            return font.measureTextWidth(text);
        }
    }

    public static float getLineHeight(float size) {
        return getLineHeight(size, DEFAULT);
    }

    public static float getLineHeight(float size, String fontName) {
        try (Font font = makeFont(fontName, size)) {
            FontMetrics m = font.getMetrics();
            return -m.getAscent() + m.getDescent();
        }
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