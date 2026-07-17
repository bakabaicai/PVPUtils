package com.pvp_utils.client.modules.impl.Render;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pvp_utils.Config;
import com.pvp_utils.PVPUtils;
import com.pvp_utils.client.NeteaseMusic.LyricLine;
import com.pvp_utils.client.NeteaseMusic.LyricLineProcessor;
import com.pvp_utils.client.NeteaseMusic.MusicPlaybackService;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaGlBackend;
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

    private final SkiaGlBackend glBackend = new SkiaGlBackend();
    private boolean nativeLoaded = false;
    private boolean pendingFrame = false;
    private float pendingX;
    private float pendingY;
    private float pendingScale;
    private List<LyricLine> pendingLyrics = List.of();
    private long pendingPositionMs;
    private float pendingAlpha = 1f;
    private float visualIndex = 0f;
    private float displayAlpha = 1f;
    private long lastFrameMs = 0L;
    private long lastSongId = Long.MIN_VALUE;
    private long pausedSinceMs = 0L;
    private long lastObservedPositionMs = -1L;
    private long lastPositionChangeMs = 0L;
    private long lastDebugMs = 0L;
    private String lastDebugKey = "";

    public static LyricsDisplayRenderer getInstance() {
        return INSTANCE;
    }

    public void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        if (!Config.lyricsDisplay) {
            debug("render-skip", "disabled");
            clearState();
            return;
        }
        if (client.options.hideGui || shouldSkipScreen(client)) {
            debug("render-skip", "hideGui=" + client.options.hideGui + " screen=" + screenName(client));
            clearPendingFrame();
            return;
        }

        MusicPlaybackService player = MusicPlaybackService.INSTANCE;
        if (HudEditOverlay.getInstance().isActive()) {
            debug("render-skip", "hudEditActive screen=" + screenName(client));
            clearPendingFrame();
            return;
        }
        List<LyricLine> lyrics = lyricsOrFallback(player);
        if (lyrics.isEmpty()) {
            debug("render-skip", playerState(player) + " lyrics=0 fallback=false screen=" + screenName(client));
            clearPendingFrame();
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
            debug("render-skip", playerState(player) + " lyrics=" + lyrics.size() + " alpha=" + fmt(alpha) + " pausedSince=" + pausedSinceMs);
            clearPendingFrame();
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

        pendingX = x;
        pendingY = y;
        pendingScale = scale;
        pendingLyrics = lyrics;
        pendingPositionMs = positionMs;
        pendingAlpha = alpha;
        pendingFrame = true;
        debug("render-ready", playerState(player)
                + " lyrics=" + lyrics.size()
                + " pos=" + positionMs
                + " currentIndex=" + currentIndex
                + " visualIndex=" + fmt(visualIndex)
                + " alpha=" + fmt(alpha)
                + " x=" + fmt(x)
                + " y=" + fmt(y)
                + " scale=" + fmt(scale)
                + " screen=" + screenName(client));
    }

    public void renderFrameEnd() {
        if (!pendingFrame) {
            debug("frameEnd-skip", "pendingFrame=false");
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (!Config.lyricsDisplay || client.options.hideGui || shouldSkipScreen(client)) {
            debug("frameEnd-skip", "enabled=" + Config.lyricsDisplay + " hideGui=" + client.options.hideGui + " screen=" + screenName(client));
            clearPendingFrame();
            return;
        }
        ensureNativeLoaded();
        int framebufferId = mainFramebufferId(client);
        Canvas canvas = glBackend.begin(framebufferId);
        if (canvas == null) {
            debug("frameEnd-fail", "canvas=null framebuffer=" + framebufferId + " lyrics=" + pendingLyrics.size());
            return;
        }
        try {
            canvas.save();
            canvas.translate(pendingX, pendingY);
            canvas.scale(pendingScale, pendingScale);
            DrawStats stats = drawLyrics(canvas, pendingLyrics, pendingPositionMs, pendingAlpha);
            canvas.restore();
            debug("frameEnd-draw", "framebuffer=" + framebufferId
                    + " lyrics=" + pendingLyrics.size()
                    + " drawn=" + stats.drawn
                    + " blankSkipped=" + stats.blankSkipped
                    + " distanceSkipped=" + stats.distanceSkipped
                    + " alpha=" + fmt(pendingAlpha)
                    + " pos=" + pendingPositionMs
                    + " x=" + fmt(pendingX)
                    + " y=" + fmt(pendingY)
                    + " scale=" + fmt(pendingScale));
        } finally {
            glBackend.end();
            clearPendingFrame();
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

    private DrawStats drawLyrics(Canvas canvas, List<LyricLine> lyrics, long positionMs, float globalAlpha) {
        int currentIndex = Math.max(0, LyricLineProcessor.currentIndex(lyrics, positionMs));
        float centerX = BASE_W * 0.5f;
        float centerY = BASE_H * 0.5f + 7f;
        int from = Math.max(0, currentIndex - 3);
        int to = Math.min(lyrics.size() - 1, currentIndex + 3);
        int drawn = 0;
        int blankSkipped = 0;
        int distanceSkipped = 0;
        for (int i = from; i <= to; i++) {
            LyricLine line = lyrics.get(i);
            String text = displayText(line.text());
            if (text.isBlank()) {
                blankSkipped++;
                continue;
            }
            float distance = Math.abs(i - visualIndex);
            if (distance > 3.2f) {
                distanceSkipped++;
                continue;
            }
            float focus = clamp(1f - distance / 2.6f);
            float size = SIDE_SIZE + (CURRENT_SIZE - SIDE_SIZE) * focus;
            int alpha = Math.round((54 + 201 * focus) * globalAlpha);
            int color = focus > 0.72f ? 0xFFFFFF : 0xC8CDD8;
            float y = centerY + (i - visualIndex) * LINE_SPACING;
            drawCenteredShadowed(canvas, text, centerX, y, size, withAlpha(color, alpha));
            drawn++;
        }
        return new DrawStats(drawn, blankSkipped, distanceSkipped);
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

    private int mainFramebufferId(Minecraft client) {
        if (client.getMainRenderTarget().getColorTexture() instanceof GlTexture texture
                && RenderSystem.getDevice() instanceof GlDevice device) {
            return texture.getFbo(device.directStateAccess(), client.getMainRenderTarget().getDepthTexture());
        }
        return 0;
    }

    private void ensureNativeLoaded() {
        if (!nativeLoaded) {
            Library.staticLoad();
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
        clearPendingFrame();
    }

    private void clearPendingFrame() {
        pendingFrame = false;
        pendingLyrics = List.of();
    }

    private void debug(String stage, String message) {
        long now = System.currentTimeMillis();
        String key = stage + "|" + message;
        if (key.equals(lastDebugKey) && now - lastDebugMs < 1000L) {
            return;
        }
        if (!key.equals(lastDebugKey) || now - lastDebugMs >= 1000L) {
            PVPUtils.LOGGER.info("[LyricsDisplay] {} {}", stage, message);
            lastDebugKey = key;
            lastDebugMs = now;
        }
    }

    private String playerState(MusicPlaybackService player) {
        String song = player.currentSong() == null
                ? "null"
                : player.currentSong().id() + ":" + player.currentSong().name() + " / " + player.currentSong().displayArtist();
        return "song=" + song + " status=" + player.status() + " playing=" + player.isPlaying();
    }

    private String screenName(Minecraft client) {
        return client.screen == null ? "null" : client.screen.getClass().getName();
    }

    private boolean shouldSkipScreen(Minecraft client) {
        return client.screen instanceof SkiaScreen
                || (client.screen != null && !(client.screen instanceof ChatScreen));
    }

    private String fmt(float value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private record DrawStats(int drawn, int blankSkipped, int distanceSkipped) {
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
