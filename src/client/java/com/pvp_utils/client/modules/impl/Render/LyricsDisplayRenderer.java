package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import com.pvp_utils.client.NeteaseMusic.LyricLine;
import com.pvp_utils.client.NeteaseMusic.LyricLineProcessor;
import com.pvp_utils.client.NeteaseMusic.MusicPlaybackService;
import com.pvp_utils.client.NeteaseMusic.LyricRasterCache;
import com.pvp_utils.client.NeteaseMusic.LyricWordTimeline;
import com.pvp_utils.client.render.chroma.ChromaGraphics;
import com.pvp_utils.client.render.chroma.ChromaScreen;
import com.pvp_utils.client.render.chroma.ChromaDraw;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LyricsDisplayRenderer {
    private static final LyricsDisplayRenderer INSTANCE = new LyricsDisplayRenderer();
    private static final float BASE_W = 460f;
    private static final float BASE_H = 196f;
    private static final float LINE_SPACING = 34f;
    private static final float TRANSLATION_LINE_SPACING = 54f;
    private static final float CURRENT_SIZE = 21f;
    private static final float SIDE_SIZE = 15f;
    private static final float TRANSLATION_SIZE = 14f;
    private static final float MAX_TEXT_WIDTH = BASE_W - 32f;
    private static final float FADE_RADIUS = 2.6f;
    private static final float BLUR_PER_LINE = CURRENT_SIZE * 0.045f;
    private static final float MAX_BLUR = 4.2f;
    private static final int MAX_PREPARED_LINES = 256;
    private static final long TWEEN_DURATION_MS = 650L;
    private static final float SPRING_STIFFNESS = 100f;
    private static final float SPRING_DAMPING = 12f;
    private static final float SPRING_COUPLING = 0.65f;
    private static final float SPRING_FALLOFF = 0.25f;
    private static final float SPRING_MIN_RESPONSE = 0.35f;
    private static final long PREVIEW_SCROLL_MS = 650L;
    private static final long PREVIEW_CYCLE_MS = 1850L;

    private final Map<TextKey, ChromaDraw.PreparedText> preparedLines = new LinkedHashMap<>(32, 0.75f, true);
    private final LyricRasterCache rasterCache = new LyricRasterCache();
    private final Map<LyricLine, LyricWordTimeline> wordTimelines = new LinkedHashMap<>(32, 0.75f, true);
    private int springFirst;
    private int springLast = -1;
    private float rasterResolution = 2f;

    private float visualIndex = 0f;
    private float previousVisualIndex = 0f;
    private float tweenFrom = 0f;
    private float tweenTo = 0f;
    private long tweenStartMs = 0L;
    private boolean tweening = false;
    private float[] springOffsets = new float[0];
    private float[] springVelocities = new float[0];
    private float displayAlpha = 1f;
    private long lastFrameMs = 0L;
    private long lastFramePositionMs = -1L;
    private long lastSongId = Long.MIN_VALUE;
    private long pausedSinceMs = 0L;
    private long lastObservedPositionMs = -1L;
    private long lastPositionChangeMs = 0L;
    private long cachedFontRevision = -1L;
    private long previewStartNanos = 0L;

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
            clearState();
            return;
        }

        if (player.currentSong() != null && player.currentSong().id() != lastSongId) {
            resetForSong(player, lyrics);
        }

        long now = System.currentTimeMillis();
        float dt = lastFrameMs == 0L ? 0.016f : Math.min((now - lastFrameMs) / 1000f, 0.05f);
        lastFrameMs = now;
        long positionMs = player.positionMs();
        float alpha = updateDisplayAlpha(player, positionMs, now, dt);
        if (alpha <= 0.01f) {
            return;
        }

        ensureSprings(lyrics.size());
        int currentIndex = Math.max(0, LyricLineProcessor.currentIndex(lyrics, positionMs));
        boolean seeked = lastFramePositionMs >= 0L && Math.abs(positionMs - lastFramePositionMs) > 600L;
        lastFramePositionMs = positionMs;
        boolean snap = seeked || Math.abs(currentIndex - visualIndex) > 4.5f;
        updateVisualIndex(currentIndex, now, snap);

        float spacing = Config.lyricsDisplayTranslation ? TRANSLATION_LINE_SPACING : LINE_SPACING;
        float scrollDelta = (visualIndex - previousVisualIndex) * spacing;
        previousVisualIndex = visualIndex;
        updateSprings(scrollDelta, dt, currentIndex, !snap);

        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        float x = getRenderX(screenW);
        float y = getRenderY(screenH);
        float scale = getScale();

        float frameVisualIndex = visualIndex;
        float resolution = (float) client.getWindow().getGuiScale() * scale;
        ChromaGraphics.submit(canvas -> {
            refreshFont();
            rasterResolution = resolution;
            canvas.translate(x, y);
            canvas.scale(scale, scale);
            canvas.clipRect(Rect.makeXYWH(16f, 0f, MAX_TEXT_WIDTH, BASE_H));
            drawLines(canvas, lyrics, frameVisualIndex, currentIndex, alpha, spacing, positionMs);
        });
    }

    public void drawPreview(Canvas canvas, float originX, float originY, float scale, float progress) {
        refreshFont();
        rasterResolution = Math.max(1f, (float) Minecraft.getInstance().getWindow().getGuiScale() * scale);
        long now = System.nanoTime();
        if (previewStartNanos == 0L) {
            previewStartNanos = now;
        }
        long elapsedMs = (now - previewStartNanos) / 1_000_000L;
        long cycle = elapsedMs / PREVIEW_CYCLE_MS;
        long phase = elapsedMs % PREVIEW_CYCLE_MS;
        float visual = cycle + (phase <= PREVIEW_SCROLL_MS
                ? easeFastOutSlowIn(phase / (float) PREVIEW_SCROLL_MS) : 1f);
        int anchorIndex = (int) Math.floor(visual + 0.5f);
        float anchorDelta = visual - anchorIndex;
        List<LyricLine> lines = previewLines();
        float spacing = Config.lyricsDisplayTranslation ? TRANSLATION_LINE_SPACING : LINE_SPACING;
        float centerX = BASE_W * 0.5f;
        float centerY = BASE_H * 0.5f;

        int save = canvas.save();
        try {
            canvas.translate(originX, originY);
            canvas.scale(scale, scale);
            canvas.clipRect(Rect.makeXYWH(16f, 0f, MAX_TEXT_WIDTH, BASE_H));
            for (int k = -2; k <= 2; k++) {
                float distance = Math.abs(k - anchorDelta);
                float focus = clamp(1f - distance / FADE_RADIUS);
                if (focus <= 0f) continue;
                LyricLine line = lines.get(Math.floorMod(anchorIndex + k, lines.size()));
                float rowCenter = centerY + (k - anchorDelta) * spacing;
                float sigma = Math.min(distance * BLUR_PER_LINE, MAX_BLUR);
                int color = withAlpha(lyricColor(focus),
                        Math.round((0.35f + 0.65f * focus) * progress * 255f));
                drawStyledLine(canvas, line, centerX, rowCenter, focus, color, sigma, elapsedMs % PREVIEW_CYCLE_MS, PREVIEW_CYCLE_MS, false);
                if (Config.lyricsDisplayTranslation) {
                    String translation = line.translation();
                    if (translation != null && !translation.isBlank()) {
                        drawTranslationLine(canvas, translation.trim(), centerX,
                                rowCenter + CURRENT_SIZE * 0.5f + 4f, progress * (0.35f + 0.65f * focus), sigma, 0L, PREVIEW_CYCLE_MS);
                    }
                }
            }
        } finally {
            canvas.restoreToCount(save);
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

    private void resetForSong(MusicPlaybackService player, List<LyricLine> lyrics) {
        clearPreparedLines();
        lastSongId = player.currentSong().id();
        visualIndex = Math.max(0, LyricLineProcessor.currentIndex(lyrics, player.positionMs()));
        previousVisualIndex = visualIndex;
        tweenFrom = visualIndex;
        tweenTo = visualIndex;
        tweening = false;
        for (int i = 0; i < springOffsets.length; i++) {
            springOffsets[i] = 0f;
            springVelocities[i] = 0f;
        }
        lastFramePositionMs = -1L;
        displayAlpha = 1f;
        pausedSinceMs = 0L;
    }

    private void ensureSprings(int size) {
        if (springOffsets.length == size) return;
        springOffsets = new float[size];
        springVelocities = new float[size];
    }

    private void updateVisualIndex(int currentIndex, long now, boolean snap) {
        if (snap) {
            visualIndex = currentIndex;
            tweenFrom = currentIndex;
            tweenTo = currentIndex;
            tweening = false;
            return;
        }
        if (currentIndex != tweenTo) {
            tweenFrom = visualIndex;
            tweenTo = currentIndex;
            tweenStartMs = now;
            tweening = true;
        }
        if (!tweening) {
            visualIndex = tweenTo;
            return;
        }
        float progress = Math.min(1f, (now - tweenStartMs) / (float) TWEEN_DURATION_MS);
        visualIndex = tweenFrom + (tweenTo - tweenFrom) * easeFastOutSlowIn(progress);
        if (progress >= 1f) {
            tweening = false;
        }
    }

    private void updateSprings(float scrollDelta, float dt, int focusedIndex, boolean chain) {
        int size = springOffsets.length;
        if (size == 0) return;
        if (!chain) {
            for (int i = 0; i < size; i++) {
                springOffsets[i] = 0f;
                springVelocities[i] = 0f;
            }
            return;
        }
        int first = Math.max(0, focusedIndex - 4);
        int last = Math.min(size - 1, focusedIndex + 4);
        for (int i = springFirst; i <= springLast && i < size; i++) {
            if (i < first || i > last) {
                springOffsets[i] = 0f;
                springVelocities[i] = 0f;
            }
        }
        springFirst = first;
        springLast = last;
        for (int i = focusedIndex + 1; i <= last; i++) {
            springOffsets[i] += scrollDelta;
        }
        int steps = Math.max(1, (int) Math.ceil(dt / (1f / 240f)));
        float step = dt / steps;
        for (int s = 0; s < steps; s++) {
            for (int i = last; i >= first; i--) {
                int distance = Math.max(0, i - focusedIndex);
                float response = Math.max(SPRING_MIN_RESPONSE,
                        Math.min(1f, 1f - distance * SPRING_FALLOFF));
                float stiffness = SPRING_STIFFNESS * response;
                float damping = SPRING_DAMPING * (float) Math.pow(response, 0.3f);
                float target = i > focusedIndex ? springOffsets[i - 1] * SPRING_COUPLING : 0f;
                float acceleration = -stiffness * (springOffsets[i] - target)
                        - damping * springVelocities[i];
                springVelocities[i] += acceleration * step;
                springOffsets[i] += springVelocities[i] * step;
            }
        }
        float limit = BASE_H * 1.5f;
        for (int i = first; i <= last; i++) {
            if (Math.abs(springOffsets[i]) > limit) {
                springOffsets[i] = Math.signum(springOffsets[i]) * limit;
                springVelocities[i] = 0f;
            }
        }
    }

    private void drawLines(Canvas canvas, List<LyricLine> lyrics, float visual, int currentIndex,
            float globalAlpha, float spacing, long positionMs) {
        float centerX = BASE_W * 0.5f;
        float centerY = BASE_H * 0.5f;
        int from = Math.max(0, currentIndex - 3);
        int to = Math.min(lyrics.size() - 1, currentIndex + 3);
        for (int i = from; i <= to; i++) {
            LyricLine line = lyrics.get(i);
            float distance = Math.abs(i - visual);
            if (distance > 3.05f) continue;
            float focus = clamp(1f - distance / FADE_RADIUS);
            float offset = i < springOffsets.length ? springOffsets[i] : 0f;
            float rowCenter = centerY + (i - visual) * spacing + offset;
            float sigma = Math.min(distance * BLUR_PER_LINE, MAX_BLUR);
            int color = withAlpha(lyricColor(focus),
                    Math.round((0.35f + 0.65f * focus) * globalAlpha * 255f));
            long duration = i + 1 < lyrics.size() ? Math.max(1L, lyrics.get(i + 1).timeMs() - line.timeMs()) : 6000L;
            drawStyledLine(canvas, line, centerX, rowCenter, focus, color, sigma, positionMs, duration, i == currentIndex);
            if (Config.lyricsDisplayTranslation) {
                String translation = line.translation();
                if (translation != null && !translation.isBlank()) {
                    drawTranslationLine(canvas, translation.trim(), centerX,
                            rowCenter + CURRENT_SIZE * 0.5f + 4f, globalAlpha * (0.35f + 0.65f * focus), sigma,
                            Math.max(0L, positionMs - line.timeMs()), duration);
                }
            }
        }
    }

    private int lyricColor(float focus) {
        return Config.lyricsAppearance.customText ? Config.lyricsAppearance.textColor
                : focus > 0.72f ? 0xFFFFFF : 0xC8CDD8;
    }

    private void drawStyledLine(Canvas canvas, LyricLine line, float centerX, float centerY,
            float focus, int color, float sigma, long positionMs, long durationMs, boolean active) {
        if ((color >>> 24) == 0) return;
        String text = displayText(line.text());
        ChromaDraw.PreparedText prepared = preparedText(text, CURRENT_SIZE);
        LyricWordTimeline timeline = active && line.hasWords() ? timeline(line, text) : null;
        if (timeline != null && timeline.isEmpty()) timeline = null;
        float highlighted = timeline == null ? 0f : timeline.widthAt(positionMs);
        float fontScale = (SIDE_SIZE + (CURRENT_SIZE - SIDE_SIZE) * focus) / CURRENT_SIZE;
        float available = MAX_TEXT_WIDTH / fontScale;
        float x = horizontalX(prepared.width(), centerX, available, Math.max(0L, positionMs - line.timeMs()), durationMs);
        if (timeline != null && prepared.width() > available) {
            x = centerX - available * 0.5f - Math.clamp(highlighted - available * 0.55f, 0f, prepared.width() - available);
        }
        float top = centerY - lineHeight(CURRENT_SIZE) * 0.5f;
        int save = canvas.save();
        try {
            canvas.translate(centerX, centerY);
            canvas.scale(fontScale, fontScale);
            canvas.translate(-centerX, -centerY);
            int base = timeline == null ? color : withAlpha(color, Math.round((color >>> 24) * 0.4f));
            rasterCache.draw(canvas, prepared, CURRENT_SIZE, x, top, base, sigma, rasterResolution);
            if (timeline != null && highlighted > 0f) {
                canvas.clipRect(Rect.makeXYWH(x, top - 4f, highlighted, CURRENT_SIZE * 2f));
                rasterCache.draw(canvas, prepared, CURRENT_SIZE, x, top, color, 0f, rasterResolution);
            }
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void drawTranslationLine(Canvas canvas, String text, float centerX, float top,
            float globalAlpha, float sigma, long elapsedMs, long durationMs) {
        ChromaDraw.PreparedText prepared = preparedText(text, TRANSLATION_SIZE);
        int alpha = Math.round(0.62f * globalAlpha * 255f);
        if (alpha <= 0) return;
        float x = horizontalX(prepared.width(), centerX, MAX_TEXT_WIDTH, elapsedMs, durationMs);
        rasterCache.draw(canvas, prepared, TRANSLATION_SIZE, x, top, withAlpha(lyricColor(0f), alpha), sigma, rasterResolution);
    }

    private LyricWordTimeline timeline(LyricLine line, String text) {
        LyricWordTimeline timeline = wordTimelines.get(line);
        if (timeline != null) return timeline;
        timeline = new LyricWordTimeline(line, text, Math.max(0, line.text().indexOf(text)), CURRENT_SIZE, false);
        wordTimelines.put(line, timeline);
        if (wordTimelines.size() > MAX_PREPARED_LINES) {
            var oldest = wordTimelines.entrySet().iterator();
            oldest.next();
            oldest.remove();
        }
        return timeline;
    }

    private static float horizontalX(float width, float centerX, float available, long elapsedMs, long durationMs) {
        if (width <= available) return centerX - width * 0.5f;
        float progress = Math.clamp((elapsedMs - 500L) / (float) Math.max(1000L, durationMs - 1000L), 0f, 1f);
        return centerX - available * 0.5f - (width - available) * progress;
    }

    private void refreshFont() {
        long revision = ChromaDraw.textFontRevision();
        if (revision == cachedFontRevision) return;
        clearPreparedLines();
        cachedFontRevision = revision;
    }

    private ChromaDraw.PreparedText preparedText(String text, float size) {
        int sizeSteps = Math.round(size * 4f);
        TextKey key = new TextKey(text, sizeSteps);
        ChromaDraw.PreparedText prepared = preparedLines.get(key);
        if (prepared != null) {
            return prepared;
        }
        float fontSize = sizeSteps * 0.25f;
        prepared = ChromaDraw.prepareText(text, fontSize, false);
        preparedLines.put(key, prepared);
        if (preparedLines.size() > MAX_PREPARED_LINES) {
            var oldest = preparedLines.entrySet().iterator();
            oldest.next().getValue().close();
            oldest.remove();
        }
        return prepared;
    }

    private float lineHeight(float size) {
        var metrics = ChromaDraw.textMetrics(size);
        return metrics.getDescent() - metrics.getAscent();
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

    private List<LyricLine> previewLines() {
        if (Config.isChinese) {
            return List.of(
            new LyricLine("第一行歌词", "First line of lyrics", 0L),
            new LyricLine("第二行歌词", "Second line of lyrics", 0L),
            new LyricLine("第三行歌词", "Third line of lyrics", 0L),
            new LyricLine("第四行歌词", "Fourth line of lyrics", 0L),
            new LyricLine("第五行歌词", "Fifth line of lyrics", 0L)
            );
        }
        return List.of(
        new LyricLine("First line of lyrics", "第一行歌词", 0L),
        new LyricLine("Second line of lyrics", "第二行歌词", 0L),
        new LyricLine("Third line of lyrics", "第三行歌词", 0L),
        new LyricLine("Fourth line of lyrics", "第四行歌词", 0L),
        new LyricLine("Fifth line of lyrics", "第五行歌词", 0L)
        );
    }

    private String displayText(String text) {
        if (text == null || text.isBlank()) {
            return Config.isChinese ? "纯音乐，请欣赏" : "Instrumental";
        }
        return text.trim();
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

    private void clearState() {
        clearPreparedLines();
        visualIndex = 0f;
        previousVisualIndex = 0f;
        tweenFrom = 0f;
        tweenTo = 0f;
        tweenStartMs = 0L;
        tweening = false;
        for (int i = 0; i < springOffsets.length; i++) {
            springOffsets[i] = 0f;
            springVelocities[i] = 0f;
        }
        displayAlpha = 1f;
        lastFrameMs = 0L;
        lastFramePositionMs = -1L;
        lastSongId = Long.MIN_VALUE;
        pausedSinceMs = 0L;
        lastObservedPositionMs = -1L;
        lastPositionChangeMs = 0L;
    }

    private static float easeFastOutSlowIn(float t) {
        float low = 0f;
        float high = 1f;
        for (int i = 0; i < 20; i++) {
            float mid = (low + high) * 0.5f;
            if (bezierAxis(mid, 0.4f, 0.2f) < t) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return bezierAxis((low + high) * 0.5f, 0f, 1f);
    }

    private static float bezierAxis(float t, float p1, float p2) {
        float u = 1f - t;
        return 3f * u * u * t * p1 + 3f * u * t * t * p2 + t * t * t;
    }

    private void clearPreparedLines() {
        rasterCache.clear();
        wordTimelines.clear();
        preparedLines.values().forEach(ChromaDraw.PreparedText::close);
        preparedLines.clear();
        cachedFontRevision = -1L;
    }

    private record TextKey(String text, int sizeSteps) {
    }

    private boolean shouldSkipScreen(Minecraft client) {
        return client.screen instanceof ChromaScreen
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
