package com.pvp_utils.client.NeteaseMusic;

import java.util.List;

public record LyricLine(String text, String translation, long timeMs, List<LyricWord> words) {
    public LyricLine(String text, String translation, long timeMs) {
        this(text, translation, timeMs, List.of());
    }

    public LyricLine(String text, long timeMs) {
        this(text, "", timeMs, List.of());
    }

    public boolean hasWords() {
        return words != null && !words.isEmpty();
    }
}
