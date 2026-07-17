package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import com.pvp_utils.client.NeteaseMusic.LyricLine;
import com.pvp_utils.client.NeteaseMusic.LyricLineProcessor;
import com.pvp_utils.client.NeteaseMusic.MusicPlaybackService;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaRenderer;
import com.pvp_utils.client.render.skia.SkiaScreen;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.impl.Library;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;

import java.util.List;

public class LyricsDisplayRenderer {
    private static final LyricsDisplayRenderer INSTANCE = new LyricsDisplayRenderer();
    private static final float BASE_W = 460f;
    private static final float BASE_H = 138f;
    private static final float LINE_SPACING = 27f;
    private static final float CURRENT_SIZE = 21f;
    private static final float SIDE_SIZE = 15f;

    private boolean nativeLoaded = false;
    private float visualIndex = 0f;
    private float displayAlpha = 1f;
    private long lastFrameMs = 0L;
    private long lastSongId = Long.MIN_VALUE;
    private long pausedSinceMs = 0L;
    private long lastObservedPositionMs = -1L;
    private long lastPositionChangeMs = 0L;

    public static LyricsDisplayRenderer getInstance() {
        return INSTANCE;
    }

    public void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        if (!Config.lyricsDisplay) {
            clearState();
            return;
        }
        if (client.options.hideGui || shouldSkipScreen(client)) {
            return;
        }

        MusicPlaybackService player = MusicPlaybackService.INSTANCE;
        if (HudEditOverlay.getInstance().isActive()) {
            return;
        }
        List<LyricLine> lyrics = lyricsOrFallback(player);
        if (lyrics.isEmpty()) {
            return;
        }

        if (player.currentSong() != null && player.currentSong().id() != lastSongId) {
            lastSongId = player.currentSong().id();
            visualIndex = Math.max(0, LyricLineProcessor.currentIndex(lyrics, player.positionMs()));
            displayAlpha = 1f;
            pausedSinceMs = 0L;
        }

        long now = System.currentTimeMillis();
        float dt = lastFrameMs == 0L ? 0.016f : Math.min((now - lastFrameMs) / 1000f, 0.05f);
        lastFrameMs = now;
        long positionMs = player.positionMs();
        float alpha = updateDisplayAlpha(player, positionMs, now, dt);
        if (alpha <= 0.01f) {
            return;
        }
        int currentIndex = Math.max(0, LyricLineProcessor.currentIndex(lyrics, positionMs));
        if (Math.abs(currentIndex - visualIndex) > 4.0f) {
            visualIndex = currentIndex;
        }
        visualIndex += (currentIndex - visualIndex) * Math.min(1f, dt * 8.5f);

        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        float x = getRenderX(screenW);
        float y = getRenderY(screenH);
        float scale = getScale();

        renderRegion(graphics, x, y, scale, lyrics, positionMs, alpha);
    }

    private void renderRegion(GuiGraphics graphics, float x, float y, float scale, List<LyricLine> lyrics, long positionMs, float alpha) {
        ensureNativeLoaded();
        int regionX = Math.max(0, (int) Math.floor(x - 2f));
        int regionY = Math.max(0, (int) Math.floor(y - 2f));
        int regionW = Math.max(1, (int) Math.ceil(BASE_W * scale + 4f));
        int regionH = Math.max(1, (int) Math.ceil(BASE_H * scale + 4f));
        Canvas canvas = SkiaRenderer.beginRegion(regionX, regionY, regionW, regionH);
        if (canvas == null) return;
        try {
            canvas.save();
            canvas.translate(x, y);
            canvas.scale(scale, scale);
            drawLyrics(canvas, lyrics, positionMs, alpha);
            canvas.restore();
        } finally {
            SkiaRenderer.endRegion(graphics);
        }
    }

    public float getEditWidth() {
        return BASE_W * getScale();
    }

    public float getEditHeight() {
        return BASE_H * getScale();
    }

    public float getRenderX(int screenW) {
        return clamp(getDefaultX(screenW) + Config.lyricsDisplayX, 0f, Math.max(0f, screenW - getEditWidth()));
    }

    public float getRenderY(int screenH) {
        return clamp(getDefaultY(screenH) + Config.lyricsDisplayY, 0f, Math.max(0f, screenH - getEditHeight()));
    }

    public float getDefaultX(int screenW) {
        return (screenW - getEditWidth()) * 0.5f;
    }

    public float getDefaultY(int screenH) {
        return screenH * 0.42f - getEditHeight() * 0.5f;
    }

    private void drawLyrics(Canvas canvas, List<LyricLine> lyrics, long positionMs, float globalAlpha) {
        int currentIndex = Math.max(0, LyricLineProcessor.currentIndex(lyrics, positionMs));
        float centerX = BASE_W * 0.5f;
        float centerY = BASE_H * 0.5f + 7f;
        int from = Math.max(0, currentIndex - 3);
        int to = Math.min(lyrics.size() - 1, currentIndex + 3);
        for (int i = from; i <= to; i++) {
            LyricLine line = lyrics.get(i);
            String text = displayText(line.text());
            if (text.isBlank()) continue;
            float distance = Math.abs(i - visualIndex);
            if (distance > 3.2f) continue;
            float focus = clamp(1f - distance / 2.6f);
            float size = SIDE_SIZE + (CURRENT_SIZE - SIDE_SIZE) * focus;
            int alpha = Math.round((54 + 201 * focus) * globalAlpha);
            int color = focus > 0.72f ? 0xFFFFFF : 0xC8CDD8;
            float y = centerY + (i - visualIndex) * LINE_SPACING;
            drawCenteredShadowed(canvas, text, centerX, y, size, withAlpha(color, alpha));
        }
    }

    private float updateDisplayAlpha(MusicPlaybackService player, long positionMs, long now, float dt) {
        if (lastObservedPositionMs < 0L || Math.abs(positionMs - lastObservedPositionMs) > 80L) {
            lastObservedPositionMs = positionMs;
            lastPositionChangeMs = now;
        }
        boolean progressMoving = now - lastPositionChangeMs < 1200L;
        boolean explicitlyPaused = isExplicitlyPaused(player.status());
        if (player.isPlaying() || progressMoving || !explicitlyPaused) {
            pausedSinceMs = 0L;
            displayAlpha += (1f - displayAlpha) * Math.min(1f, dt * 10f);
            return displayAlpha;
        }
        if (pausedSinceMs == 0L) {
            pausedSinceMs = now;
        }
        float target = now - pausedSinceMs >= 5000L ? 0f : 1f;
        displayAlpha += (target - displayAlpha) * Math.min(1f, dt * 5f);
        return displayAlpha;
    }

    private boolean isExplicitlyPaused(String status) {
        if (status == null) return false;
        return status.equalsIgnoreCase("Paused")
                || status.equalsIgnoreCase("Stopped")
                || status.equalsIgnoreCase("Ended");
    }

    private void drawCenteredShadowed(Canvas canvas, String text, float centerX, float baselineY, float size, int argb) {
        text = trimToWidth(text, BASE_W - 32f, size);
        float w = FontRenderer.measureTextWidth(text, size);
        float x = centerX - w * 0.5f;
        int alpha = (argb >>> 24) & 0xFF;
        FontRenderer.drawText(canvas, text, x + 1.2f, baselineY + 1.2f, size, alpha << 24);
        FontRenderer.drawText(canvas, text, x, baselineY, size, argb);
    }

    private String displayText(String text) {
        if (text == null || text.isBlank()) {
            return Config.isChinese ? "纯音乐，请欣赏" : "Instrumental";
        }
        return text.trim();
    }

    private String trimToWidth(String text, float maxWidth, float size) {
        if (FontRenderer.measureTextWidth(text, size) <= maxWidth) return text;
        String ellipsis = "...";
        while (text.length() > 1 && FontRenderer.measureTextWidth(text + ellipsis, size) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + ellipsis;
    }

    private List<LyricLine> lyricsOrFallback(MusicPlaybackService player) {
        List<LyricLine> lyrics = player.lyricsSnapshot();
        if (!lyrics.isEmpty()) {
            return lyrics;
        }
        if (player.currentSong() == null) {
            return List.of();
        }
        return List.of(
                new LyricLine(player.currentSong().name(), 0L),
                new LyricLine(player.currentSong().displayArtist(), 60_000L)
        );
    }

    private float getScale() {
        return Math.max(0.5f, Config.lyricsDisplayScale);
    }

    private void ensureNativeLoaded() {
        if (!nativeLoaded) {
            Library.load();
            nativeLoaded = true;
        }
    }

    private void clearState() {
        visualIndex = 0f;
        displayAlpha = 1f;
        lastFrameMs = 0L;
        lastSongId = Long.MIN_VALUE;
        pausedSinceMs = 0L;
        lastObservedPositionMs = -1L;
        lastPositionChangeMs = 0L;
    }

    private boolean shouldSkipScreen(Minecraft client) {
        return client.screen instanceof SkiaScreen
                || (client.screen != null && !(client.screen instanceof ChatScreen));
    }

    private float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0xFFFFFF);
    }
}
