package com.pvp_utils.client.modules.impl.Render.DynamicIsland;

import com.pvp_utils.client.NeteaseMusic.LyricFilter;
import com.pvp_utils.client.NeteaseMusic.LyricLine;
import com.pvp_utils.client.NeteaseMusic.LyricLineProcessor;
import com.pvp_utils.client.NeteaseMusic.MusicPlaybackService;
import com.pvp_utils.client.NeteaseMusic.Song;

import java.util.List;

public final class DynamicIslandLyrics {
    private static final long PAUSE_FADE_DELAY_MS = 5000L;
    public static final LyricsCard HIDDEN = new LyricsCard(false, "", 0f, 0L);

    private static long lastObservedPositionMs = -1L;
    private static long lastPositionChangeMs = 0L;
    private static long pausedSinceMs = 0L;
    private static float displayAlpha = 1f;
    private static String currentText = "";

    private DynamicIslandLyrics() {
    }

    public record LyricsCard(boolean visible, String text, float alpha, long durationMs) {}

    public static LyricsCard snapshot() {
        MusicPlaybackService player = MusicPlaybackService.INSTANCE;
        Song song = player.currentSong();
        if (song == null) {
            reset();
            return HIDDEN;
        }
        long positionMs = player.positionMs();
        long now = System.currentTimeMillis();
        if (lastObservedPositionMs < 0L || Math.abs(positionMs - lastObservedPositionMs) > 80L) {
            lastObservedPositionMs = positionMs;
            lastPositionChangeMs = now;
        }
        boolean progressMoving = now - lastPositionChangeMs < 1200L;
        boolean explicitlyPaused = isExplicitlyPaused(player.status());
        if (player.isPlaying() || progressMoving || !explicitlyPaused) {
            pausedSinceMs = 0L;
            displayAlpha += (1f - displayAlpha) * 0.2f;
        } else {
            if (pausedSinceMs == 0L) {
                pausedSinceMs = now;
            }
            float target = now - pausedSinceMs >= PAUSE_FADE_DELAY_MS ? 0f : 1f;
            displayAlpha += (target - displayAlpha) * 0.12f;
        }
        if (displayAlpha <= 0.02f) {
            return HIDDEN;
        }
        List<LyricLine> lyrics = player.lyricsSnapshot();
        if (lyrics.isEmpty()) {
            return new LyricsCard(true, "", displayAlpha, 0L);
        }
        int index = Math.max(0, LyricLineProcessor.currentIndex(lyrics, positionMs));
        String songName = song.name();
        String filteredText = null;
        for (int i = index; i >= 0 && i >= index - 5; i--) {
            LyricLine line = lyrics.get(Math.min(i, lyrics.size() - 1));
            String text = line.text();
            if (text != null && !text.isBlank() && !LyricFilter.shouldFilter(text, songName)) {
                filteredText = text.trim();
                index = i;
                break;
            }
        }
        if (filteredText == null) {
            return new LyricsCard(true, "", displayAlpha, 0L);
        }
        if (!filteredText.equals(currentText)) {
            currentText = filteredText;
        }
        long durationMs = 0L;
        if (index + 1 < lyrics.size()) {
            durationMs = lyrics.get(index + 1).timeMs() - lyrics.get(index).timeMs();
        }
        if (durationMs <= 0L) {
            durationMs = 3000L;
        }
        return new LyricsCard(true, currentText, displayAlpha, durationMs);
    }

    public static void reset() {
        lastObservedPositionMs = -1L;
        lastPositionChangeMs = 0L;
        pausedSinceMs = 0L;
        displayAlpha = 1f;
        currentText = "";
    }

    private static boolean isExplicitlyPaused(String status) {
        if (status == null) return false;
        return status.equalsIgnoreCase("Paused")
                || status.equalsIgnoreCase("Stopped")
                || status.equalsIgnoreCase("Ended");
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
