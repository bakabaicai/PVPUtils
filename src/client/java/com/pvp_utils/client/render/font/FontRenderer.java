package com.pvp_utils.client.render.font;

import io.github.humbleui.skija.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class FontRenderer {

    public static final String DEFAULT = "harmony";

    private static final Map<String, Typeface> typefaces = new HashMap<>();
    private static final FontMgr fontMgr = FontMgr.getDefault();

    static {
        registerFromResources(DEFAULT, "/fonts/harmony.ttf");
    }

    public static void registerFromResources(String name, String resourcePath) {
        try (InputStream is = FontRenderer.class.getResourceAsStream(resourcePath)) {
            if (is == null) throw new IOException("Font not found: " + resourcePath);
            byte[] data = is.readAllBytes();
            Typeface typeface = Typeface.makeFromData(Data.makeFromBytes(data));
            typefaces.put(name, typeface);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load font: " + resourcePath, e);
        }
    }

    public static void registerFromFile(String name, Path path) {
        Typeface typeface = Typeface.makeFromFile(path.toAbsolutePath().toString());
        typefaces.put(name, typeface);
    }

    public static void drawText(Canvas canvas, String text, float x, float y, float size, int argb) {
        drawText(canvas, text, x, y, size, argb, DEFAULT);
    }

    public static void drawText(Canvas canvas, String text, float x, float y, float size, int argb, String fontName) {
        Typeface typeface = typefaces.getOrDefault(fontName, typefaces.get(DEFAULT));
        try (Font font = new Font(typeface, size);
             Paint paint = new Paint()) {
            paint.setColor(argb);
            canvas.drawString(text, x, y, font, paint);
        }
    }

    public static float measureTextWidth(String text, float size) {
        return measureTextWidth(text, size, DEFAULT);
    }

    public static float measureTextWidth(String text, float size, String fontName) {
        Typeface typeface = typefaces.getOrDefault(fontName, typefaces.get(DEFAULT));
        try (Font font = new Font(typeface, size)) {
            return font.measureTextWidth(text);
        }
    }

    public static float getLineHeight(float size) {
        return getLineHeight(size, DEFAULT);
    }

    public static float getLineHeight(float size, String fontName) {
        Typeface typeface = typefaces.getOrDefault(fontName, typefaces.get(DEFAULT));
        try (Font font = new Font(typeface, size)) {
            FontMetrics metrics = font.getMetrics();
            return -metrics.getAscent() + metrics.getDescent();
        }
    }

    public static void drawSegmented(Canvas canvas, Segment[] segments, float x, float y) {
        float cursor = x;
        for (Segment seg : segments) {
            drawText(canvas, seg.text, cursor, y, seg.size, seg.argb, seg.fontName);
            cursor += measureTextWidth(seg.text, seg.size, seg.fontName);
            if (seg.gap > 0) cursor += seg.gap;
        }
    }

    public static class Segment {
        public final String text;
        public final float size;
        public final int argb;
        public final String fontName;
        public final float gap;

        public Segment(String text, float size, int argb) {
            this(text, size, argb, DEFAULT, 0f);
        }

        public Segment(String text, float size, int argb, String fontName) {
            this(text, size, argb, fontName, 0f);
        }

        public Segment(String text, float size, int argb, String fontName, float gap) {
            this.text = text;
            this.size = size;
            this.argb = argb;
            this.fontName = fontName;
            this.gap = gap;
        }
    }
}