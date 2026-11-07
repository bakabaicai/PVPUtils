package com.pvp_utils.client.modules.impl.Render.DynamicIsland;

import com.pvp_utils.client.NeteaseMusic.LyricFilter;
import com.pvp_utils.client.NeteaseMusic.LyricLine;
import com.pvp_utils.client.NeteaseMusic.LyricLineProcessor;
import com.pvp_utils.client.NeteaseMusic.MusicPlaybackService;
import com.pvp_utils.client.NeteaseMusic.Song;

import java.util.List;

public final class DynamicIslandLyrics {
    public static final LyricsCard HIDDEN = new LyricsCard(false, "", -1L, -1L);
    private static long songId = -1L;
    private static long nextRefreshMs;
    private static long pausedSinceMs;
    private static LyricsCard current = HIDDEN;

    private DynamicIslandLyrics() {
    }

    public record LyricsCard(boolean visible, String text, long songId, long lineTimeMs, long lineDurationMs, LyricLine line) {
        public LyricsCard(boolean visible, String text, long songId, long lineTimeMs) {
            this(visible, text, songId, lineTimeMs, 6000L, null);
        }
    }

    public static LyricsCard snapshot() {
        MusicPlaybackService player = MusicPlaybackService.INSTANCE;
        Song song = player.currentSong();
        if (song == null) {
            reset();
            return HIDDEN;
        }
        long now = System.currentTimeMillis();
        if (song.id() != songId) {
            reset();
            songId = song.id();
        }
        if (player.isPlaying()) pausedSinceMs = 0L;
        else if (pausedSinceMs == 0L) pausedSinceMs = now;
        if (pausedSinceMs > 0L && now - pausedSinceMs > 5000L) return HIDDEN;
        if (now < nextRefreshMs) return current;
        nextRefreshMs = now + 50L;
        long position = player.positionMs();
        List<LyricLine> lyrics = player.lyricsSnapshot();
        int index = lyrics.isEmpty() ? -1 : LyricLineProcessor.currentIndex(lyrics, position);
        String lineText = song.name() == null ? "" : song.name();
        long lineTime = -1L;
        long duration = 6000L;
        LyricLine selectedLine = null;
        for (int i = Math.min(index, lyrics.size() - 1); i >= 0 && i >= index - 5; i--) {
            LyricLine line = lyrics.get(i);
            if (line.text() != null && !line.text().isBlank() && !LyricFilter.shouldFilter(line.text(), song.name())) {
                lineText = line.text().trim();
                lineTime = line.timeMs();
                selectedLine = line;
                duration = i + 1 < lyrics.size() ? Math.max(1L, lyrics.get(i + 1).timeMs() - lineTime)
                        : Math.max(1L, player.totalDurationMs() - lineTime);
                break;
            }
        }
        current = new LyricsCard(true, lineText, song.id(), lineTime, duration, selectedLine);
        return current;
    }

    public static void reset() {
        songId = -1L;
        nextRefreshMs = 0L;
        pausedSinceMs = 0L;
        current = HIDDEN;
    }
}
