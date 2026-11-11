package com.pvp_utils.client.modules.impl.Render.DynamicIsland;

import com.pvp_utils.client.render.chroma.ChromaDraw;
import com.pvp_utils.client.NeteaseMusic.LyricLine;
import com.pvp_utils.client.NeteaseMusic.LyricWordTimeline;
import io.github.humbleui.skija.Canvas;

import java.util.LinkedHashMap;
import java.util.Map;

final class IslandTextCache implements AutoCloseable {
    private static final int CAPACITY = 256;
    private final LinkedHashMap<Key, ChromaDraw.PreparedText> entries = new LinkedHashMap<>(32, 0.75f, true);
    private final LinkedHashMap<WordKey, LyricWordTimeline> timelines = new LinkedHashMap<>(16, 0.75f, true);
    private long fontRevision = -1L;

    float width(String text, float size) {
        return get(text, size, -1f).width();
    }

    void draw(Canvas canvas, String text, float x, float centerY, float size, int color, float maxWidth, float alignment) {
        if (maxWidth <= 0f || text == null || text.isEmpty()) return;
        ChromaDraw.PreparedText prepared = get(text, size, maxWidth);
        float baseline = centerY - (ChromaDraw.textMetrics(size).getAscent() + ChromaDraw.textMetrics(size).getDescent()) * 0.5f;
        prepared.draw(canvas, x + (maxWidth - prepared.width()) * alignment, baseline, color);
    }

    void drawScrolling(Canvas canvas, String text, float x, float centerY, float size, int color,
                       float availableWidth, long elapsedMs, long durationMs) {
        drawScrolling(canvas, text, x, centerY, size, color, availableWidth, elapsedMs, durationMs, null);
    }

    void drawScrolling(Canvas canvas, String text, float x, float centerY, float size, int color,
                       float availableWidth, long elapsedMs, long durationMs, LyricLine timing) {
        if (availableWidth <= 0f || text == null || text.isEmpty()) return;
        ChromaDraw.PreparedText prepared = get(text, size, -1f);
        LyricWordTimeline timeline = null;
        if (timing != null && timing.hasWords()) {
            WordKey key = new WordKey(timing, size);
            timeline = timelines.get(key);
            if (timeline == null) {
                timeline = new LyricWordTimeline(timing, text, Math.max(0, timing.text().indexOf(text)), size, false);
                timelines.put(key, timeline);
                if (timelines.size() > 64) timelines.pollFirstEntry();
            }
            if (timeline.isEmpty()) timeline = null;
        }
        float highlighted = timeline == null ? 0f : timeline.widthAt(timing.timeMs() + elapsedMs);
        float overflow = Math.max(0f, prepared.width() - availableWidth);
        float progress = Math.clamp((elapsedMs - 500L) / (float) Math.max(1000L, durationMs - 1000L), 0f, 1f);
        float offset = timeline == null ? overflow * progress : Math.clamp(highlighted - availableWidth * 0.55f, 0f, overflow);
        float drawX = overflow > 0f ? x - offset : x + (availableWidth - prepared.width()) * 0.5f;
        float baseline = centerY - (ChromaDraw.textMetrics(size).getAscent() + ChromaDraw.textMetrics(size).getDescent()) * 0.5f;
        int save = canvas.save();
        try {
            canvas.clipRect(io.github.humbleui.types.Rect.makeXYWH(x, centerY - size, availableWidth, size * 2f));
            int base = timeline == null ? color : Math.round((color >>> 24) * 0.4f) << 24 | color & 0xFFFFFF;
            prepared.draw(canvas, drawX, baseline, base);
            if (timeline != null && highlighted > 0f) {
                canvas.clipRect(io.github.humbleui.types.Rect.makeXYWH(drawX, centerY - size, highlighted, size * 2f));
                prepared.draw(canvas, drawX, baseline, color);
            }
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private ChromaDraw.PreparedText get(String value, float size, float maxWidth) {
        long revision = ChromaDraw.textFontRevision();
        if (revision != fontRevision) {
            close();
            fontRevision = revision;
        }
        String text = value == null ? "" : value;
        Key fullKey = new Key(text, size, -1f);
        ChromaDraw.PreparedText full = entries.get(fullKey);
        if (full == null) {
            full = ChromaDraw.prepareText(text, size, false);
            put(fullKey, full);
        }
        if (maxWidth < 0f || full.width() <= maxWidth + 0.01f) return full;
        float width = maxWidth < 0f ? -1f : (float) Math.floor(maxWidth * 2f) * 0.5f;
        Key key = new Key(text, size, width);
        ChromaDraw.PreparedText cached = entries.get(key);
        if (cached != null) return cached;
        String display = width < 0f ? text : fit(text, size, width);
        ChromaDraw.PreparedText prepared = ChromaDraw.prepareText(display, size, false);
        put(key, prepared);
        return prepared;
    }

    private void put(Key key, ChromaDraw.PreparedText text) {
        entries.put(key, text);
        while (entries.size() > CAPACITY) {
            Map.Entry<Key, ChromaDraw.PreparedText> eldest = entries.pollFirstEntry();
            if (eldest != null) eldest.getValue().close();
        }
    }

    private String fit(String text, float size, float width) {
        if (ChromaDraw.textWidth(text, size) <= width) return text;
        String suffix = "…";
        if (ChromaDraw.textWidth(suffix, size) > width) return "";
        int[] points = text.codePoints().toArray();
        int low = 0;
        int high = points.length;
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            if (ChromaDraw.textWidth(new String(points, 0, middle) + suffix, size) <= width) low = middle;
            else high = middle - 1;
        }
        return new String(points, 0, low) + suffix;
    }

    @Override
    public void close() {
        for (ChromaDraw.PreparedText prepared : entries.values()) prepared.close();
        entries.clear();
        timelines.clear();
        fontRevision = -1L;
    }

    private record WordKey(LyricLine line, float size) {
    }

    private record Key(String text, float size, float width) {
    }
}
