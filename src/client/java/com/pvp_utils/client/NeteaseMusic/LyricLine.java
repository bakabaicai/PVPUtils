package com.pvp_utils.client.NeteaseMusic;

public record LyricLine(String text, String translation, long timeMs) {
    public LyricLine(String text, long timeMs) {
        this(text, "", timeMs);
    }
}
