package com.pvp_utils.client.NeteaseMusic;

import com.pvp_utils.client.render.skia.SkijaUi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

public final class LyricWordTimeline {
    private final List<Word> words;

    public LyricWordTimeline(LyricLine line, String fragment, int fragmentStart, float size, boolean bold) {
        this(line, fragment, fragmentStart, value -> measure(value, size, bold));
    }

    LyricWordTimeline(LyricLine line, String fragment, int fragmentStart, ToDoubleFunction<String> measure) {
        List<Word> measured = new ArrayList<>();
        if (line.hasWords()) {
            String text = line.text() == null ? "" : line.text();
            int cursor = 0;
            int fragmentEnd = fragmentStart + fragment.length();
            for (LyricWord word : line.words()) {
                if (word.text() == null || word.text().isEmpty()) continue;
                int start = text.indexOf(word.text(), cursor);
                if (start < 0) start = cursor;
                int end = Math.min(text.length(), start + word.text().length());
                cursor = end;
                if (end <= fragmentStart || start >= fragmentEnd) continue;
                int localStart = Math.max(0, start - fragmentStart);
                int localEnd = Math.min(fragment.length(), end - fragmentStart);
                float from = (float) measure.applyAsDouble(fragment.substring(0, localStart));
                float to = (float) measure.applyAsDouble(fragment.substring(0, localEnd));
                long duration = Math.max(1L, word.durationMs());
                float segmentStart = (Math.max(start, fragmentStart) - start) / (float) Math.max(1, end - start);
                float segmentEnd = (Math.min(end, fragmentEnd) - start) / (float) Math.max(1, end - start);
                measured.add(new Word(word.startMs() + Math.round(duration * segmentStart),
                        Math.max(1L, Math.round(duration * (segmentEnd - segmentStart))), from, to));
            }
        }
        words = List.copyOf(measured);
    }

    public boolean isEmpty() {
        return words.isEmpty();
    }

    public float widthAt(long positionMs) {
        float completed = 0f;
        for (Word word : words) {
            if (positionMs < word.startMs()) return completed;
            float fraction = Math.clamp((positionMs - word.startMs()) / (float) word.durationMs(), 0f, 1f);
            completed = word.from() + (word.to() - word.from()) * fraction;
            if (fraction < 1f) return completed;
        }
        return completed;
    }

    private static float measure(String value, float size, boolean bold) {
        return bold ? SkijaUi.boldTextWidth(value, size) : SkijaUi.textWidth(value, size);
    }

    private record Word(long startMs, long durationMs, float from, float to) {
    }
}
