package com.pvp_utils.client.modules.impl.Render.DynamicIsland;

import com.pvp_utils.client.render.skia.SkijaUi;
import io.github.humbleui.skija.Canvas;

import java.util.LinkedHashMap;
import java.util.Map;

final class IslandTextCache implements AutoCloseable {
    private static final int CAPACITY = 256;
    private final LinkedHashMap<Key, SkijaUi.PreparedText> entries = new LinkedHashMap<>(32, 0.75f, true);
    private long fontRevision = -1L;

    float width(String text, float size) {
        return get(text, size, -1f).width();
    }

    void draw(Canvas canvas, String text, float x, float centerY, float size, int color, float maxWidth, float alignment) {
        if (maxWidth <= 0f || text == null || text.isEmpty()) return;
        SkijaUi.PreparedText prepared = get(text, size, maxWidth);
        float baseline = centerY - (SkijaUi.textMetrics(size).getAscent() + SkijaUi.textMetrics(size).getDescent()) * 0.5f;
        prepared.draw(canvas, x + (maxWidth - prepared.width()) * alignment, baseline, color);
    }

    private SkijaUi.PreparedText get(String value, float size, float maxWidth) {
        long revision = SkijaUi.textFontRevision();
        if (revision != fontRevision) {
            close();
            fontRevision = revision;
        }
        String text = value == null ? "" : value;
        Key fullKey = new Key(text, size, -1f);
        SkijaUi.PreparedText full = entries.get(fullKey);
        if (full == null) {
            full = SkijaUi.prepareText(text, size, false);
            put(fullKey, full);
        }
        if (maxWidth < 0f || full.width() <= maxWidth + 0.01f) return full;
        float width = maxWidth < 0f ? -1f : (float) Math.floor(maxWidth * 2f) * 0.5f;
        Key key = new Key(text, size, width);
        SkijaUi.PreparedText cached = entries.get(key);
        if (cached != null) return cached;
        String display = width < 0f ? text : fit(text, size, width);
        SkijaUi.PreparedText prepared = SkijaUi.prepareText(display, size, false);
        put(key, prepared);
        return prepared;
    }

    private void put(Key key, SkijaUi.PreparedText text) {
        entries.put(key, text);
        while (entries.size() > CAPACITY) {
            Map.Entry<Key, SkijaUi.PreparedText> eldest = entries.pollFirstEntry();
            if (eldest != null) eldest.getValue().close();
        }
    }

    private String fit(String text, float size, float width) {
        if (SkijaUi.textWidth(text, size) <= width) return text;
        String suffix = "…";
        if (SkijaUi.textWidth(suffix, size) > width) return "";
        int[] points = text.codePoints().toArray();
        int low = 0;
        int high = points.length;
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            if (SkijaUi.textWidth(new String(points, 0, middle) + suffix, size) <= width) low = middle;
            else high = middle - 1;
        }
        return new String(points, 0, low) + suffix;
    }

    @Override
    public void close() {
        for (SkijaUi.PreparedText prepared : entries.values()) prepared.close();
        entries.clear();
        fontRevision = -1L;
    }

    private record Key(String text, float size, float width) {
    }
}
