package com.pvp_utils.client.NeteaseMusic;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.skia.SkijaUi;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.types.Rect;

import java.util.ArrayList;
import java.util.List;

public final class MusicLyricsView implements AutoCloseable {
    private static final long SCROLL_DURATION_NANOS = 650_000_000L;
    private static final float CHAIN_STIFFNESS = 100f;
    private static final float CHAIN_DAMPING = 12f;
    private static final float CHAIN_COUPLING = 0.65f;
    private static final float CHAIN_FALLOFF = 0.25f;
    private static final float CHAIN_MIN_RESPONSE = 0.35f;
    private static final float BLUR_PER_LINE = 0.04f;
    private final MusicPlaybackService player = MusicPlaybackService.INSTANCE;
    private MusicBlurredCover background = new MusicBlurredCover();
    private MusicBlurredCover previousBackground = new MusicBlurredCover();
    private final List<Row> rows = new ArrayList<>();
    private final List<HitRow> hitRows = new ArrayList<>();
    private LyricRasterCache rasterCache = new LyricRasterCache();
    private boolean closed;
    private int retainedFirst;
    private int retainedLast = -1;
    private List<LyricLine> cachedLyrics = List.of();
    private float cachedWidth = -1f;
    private long cachedFontRevision = -1L;
    private boolean cachedTranslation;
    private boolean translationVisible = true;
    private boolean closeRequested;
    private boolean draggingProgress;
    private boolean draggingVolume;
    private boolean positioned;
    private long songId = Long.MIN_VALUE;
    private long lastFrameNanos;
    private long manualUntil;
    private String coverUrl;
    private String previousCoverUrl;
    private float backgroundBlend = 1f;
    private float coverScale = 1f;
    private float scroll;
    private float scrollTarget;
    private float scrollFrom;
    private long scrollStartNanos;
    private boolean scrollTweening;
    private float manualBlur = 1f;
    private float manualTarget;
    private float pendingProgress;
    private float pendingVolume;
    private float lastVolume = 0.7f;
    private float width;
    private float height;
    private float lyricsX;
    private float lyricsWidth;
    private float lyricsTop;
    private float lyricsBottom;
    private float lyricSize;
    private float translationSize;
    private float contentHeight;
    private float leftX;
    private float leftWidth;
    private float coverY;
    private float progressY;
    private float controlsY;
    private float volumeX;
    private float volumeY;
    private float volumeWidth;

    public void setTranslationVisible(boolean visible) {
        translationVisible = visible;
    }

    public boolean wantsClose() {
        return closeRequested;
    }

    @Override
    public void close() {
        if (closed) return;
        background.close();
        previousBackground.close();
        clearRows();
        rasterCache.close();
        closed = true;
        hitRows.clear();
        cachedLyrics = List.of();
        cachedWidth = -1f;
        positioned = false;
        lastFrameNanos = 0L;
    }

    public void render(Canvas canvas, float w, float h, float mouseX, float mouseY, float opacity) {
        if (w <= 0f || h <= 0f) return;
        if (closed) {
            rasterCache = new LyricRasterCache();
            closed = false;
        }
        layout(w, h);
        long now = System.nanoTime();
        float delta = lastFrameNanos == 0L ? 1f / 60f : Math.min(0.1f, (now - lastFrameNanos) / 1_000_000_000f);
        lastFrameNanos = now;
        Song song = player.currentSong();
        long currentSongId = song == null ? Long.MIN_VALUE : song.id();
        if (currentSongId != songId) {
            songId = currentSongId;
            previousBackground.close();
            MusicBlurredCover reusable = previousBackground;
            previousBackground = background;
            background = reusable;
            previousCoverUrl = coverUrl;
            coverUrl = song == null ? null : song.image();
            backgroundBlend = previousCoverUrl == null ? 1f : 0f;
            manualUntil = 0L;
            positioned = false;
            draggingProgress = false;
            cachedLyrics = List.of();
            cachedWidth = -1f;
        }
        backgroundBlend = approach(backgroundBlend, 1f, delta, 3f);
        if (backgroundBlend >= 0.995f && previousCoverUrl != null) {
            previousCoverUrl = null;
            previousBackground.close();
        }
        coverScale = approach(coverScale, player.isPlaying() ? 1f : 0.8f, delta, 7f);
        float alpha = clamp(opacity);
        int save = canvas.save();
        try {
            canvas.clipRect(Rect.makeWH(w, h));
            drawBackground(canvas, alpha);
            drawControls(canvas, song, mouseX, mouseY, alpha);
            drawLyrics(canvas, mouseX, mouseY, alpha, delta, now);
            boolean backHovered = inside(mouseX, mouseY, 10f, 8f, 30f, 28f);
            if (backHovered) MusicUi.rounded(canvas, 10f, 8f, 30f, 28f, 6f, MusicUi.alpha(0x22FFFFFF, alpha));
            MusicUi.icon(canvas, "chevron_down", 17f, 14f, 16f, MusicUi.alpha(MusicUi.TEXT, alpha));
            MusicUi.text(canvas, "PVPUtils Music", 47f, 16f, 9f, MusicUi.alpha(MusicUi.TEXT, alpha * 0.58f));
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void layout(float w, float h) {
        width = w;
        height = h;
        lyricsX = w * 0.49f;
        lyricsWidth = Math.max(40f, w - lyricsX - 28f);
        lyricsTop = 48f;
        lyricsBottom = Math.max(lyricsTop + 20f, h - 20f);
        lyricSize = Math.round(Math.min(32.5f, Math.max(17f, lyricsWidth / 12f)) * 2f) * 0.5f;
        translationSize = Math.round(lyricSize * 0.52f * 2f) * 0.5f;
        float minimumCoverY = 42f;
        float controlsHeight = 66f + 27f + 45f;
        float bottomPadding = 16f;
        leftWidth = Math.max(40f, Math.min(w * 0.37f,
                Math.min(h * 0.5f, h - minimumCoverY - controlsHeight - bottomPadding)));
        leftX = (w * 0.45f - leftWidth) * 0.5f;
        coverY = Math.max(minimumCoverY, (h - leftWidth - controlsHeight - bottomPadding) * 0.38f);
        progressY = coverY + leftWidth + 66f;
        controlsY = progressY + 27f;
        volumeWidth = Math.max(24f, leftWidth - 48f);
        volumeX = leftX + 24f;
        volumeY = controlsY + 45f;
    }

    private void drawBackground(Canvas canvas, float alpha) {
        MusicUi.rounded(canvas, 0f, 0f, width, height, 0f, MusicUi.alpha(MusicUi.BG, alpha));
        Image current = background.image(NeteaseMusicCovers.skiaImage(coverUrl), width, height);
        Image previous = backgroundBlend < 0.995f
                ? previousBackground.image(NeteaseMusicCovers.skiaImage(previousCoverUrl), width, height) : null;
        if (current != null || previous != null) {
            try (Paint paint = new Paint()) {
                if (previous != null) drawBackgroundImage(canvas, previous, paint, alpha * (1f - backgroundBlend));
                if (current != null) drawBackgroundImage(canvas, current, paint, alpha * backgroundBlend);
            }
            MusicUi.rounded(canvas, 0f, 0f, width, height, 0f, MusicUi.alpha(0xA30E0F12, alpha));
        }
        MusicUi.line(canvas, lyricsX - 18f, height * 0.16f, lyricsX - 18f, height * 0.84f,
                0.75f, MusicUi.alpha(0x10FFFFFF, alpha));
    }

    private void drawBackgroundImage(Canvas canvas, Image image, Paint paint, float alpha) {
        if (image.isClosed() || image.getWidth() <= 0 || image.getHeight() <= 0 || alpha <= 0.001f) return;
        float scale = Math.max(width / image.getWidth(), height / image.getHeight()) * 1.2f;
        float imageWidth = image.getWidth() * scale;
        float imageHeight = image.getHeight() * scale;
        double phase = System.nanoTime() / 18_000_000_000.0;
        float x = (width - imageWidth) * 0.5f + (float) Math.sin(phase) * width * 0.035f;
        float y = (height - imageHeight) * 0.5f + (float) Math.cos(phase * 0.8) * height * 0.035f;
        paint.setAlphaf(clamp(alpha));
        canvas.drawImageRect(image, Rect.makeWH(image.getWidth(), image.getHeight()),
                Rect.makeXYWH(x, y, imageWidth, imageHeight), SamplingMode.LINEAR, paint, true);
    }

    private void drawControls(Canvas canvas, Song song, float mouseX, float mouseY, float alpha) {
        float renderedSize = leftWidth * coverScale;
        float coverX = leftX + (leftWidth - renderedSize) * 0.5f;
        float renderedY = coverY + (leftWidth - renderedSize) * 0.5f;
        MusicUi.cover(canvas, NeteaseMusicCovers.skiaImage(coverUrl), coverX, renderedY,
                renderedSize, 7f, Math.round(alpha * 255f));
        float textY = coverY + leftWidth + 14f;
        String title = song == null ? localized("未在播放", "Not playing") : song.name();
        String artist = song == null ? localized("选择一首歌曲开始播放", "Choose a song to start") : song.displayArtist();
        MusicUi.bold(canvas, fitBold(title, leftWidth, 14f), leftX, textY, 14f, MusicUi.alpha(MusicUi.TEXT, alpha));
        MusicUi.text(canvas, MusicUi.fit(artist, leftWidth, 10f), leftX, textY + 24f, 10f,
                MusicUi.alpha(MusicUi.TEXT, alpha * 0.72f));
        float progress = draggingProgress ? pendingProgress : progress();
        boolean progressHovered = inside(mouseX, mouseY, leftX - 4f, progressY - 7f, leftWidth + 8f, 14f);
        drawSlider(canvas, leftX, progressY, leftWidth, progress, draggingProgress || progressHovered, alpha);
        long total = player.totalDurationMs();
        long position = draggingProgress ? Math.round(total * pendingProgress) : player.positionMs();
        MusicUi.text(canvas, time(position), leftX, progressY + 7f, 7f, MusicUi.alpha(MusicUi.TEXT, alpha * 0.55f));
        String remaining = "-" + time(Math.max(0L, total - position));
        MusicUi.text(canvas, remaining, leftX + leftWidth - MusicUi.width(remaining, 7f), progressY + 7f, 7f,
                MusicUi.alpha(MusicUi.TEXT, alpha * 0.55f));
        float center = leftX + leftWidth * 0.5f;
        drawButton(canvas, modeIcon(), center - 80f, controlsY + 2f, 22f, mouseX, mouseY, alpha);
        drawButton(canvas, "previous", center - 48f, controlsY, 26f, mouseX, mouseY, alpha);
        drawButton(canvas, player.isPlaying() ? "pause" : "play", center - 13f, controlsY, 26f, mouseX, mouseY, alpha);
        drawButton(canvas, "next", center + 22f, controlsY, 26f, mouseX, mouseY, alpha);
        float volume = draggingVolume ? pendingVolume : player.volume();
        drawButton(canvas, volume <= 0.001f ? "mute" : "volume", leftX - 1f, volumeY - 8f,
                16f, mouseX, mouseY, alpha * 0.68f);
        boolean volumeHovered = inside(mouseX, mouseY, volumeX - 3f, volumeY - 7f, volumeWidth + 6f, 14f);
        drawSlider(canvas, volumeX, volumeY, volumeWidth, volume, volumeHovered || draggingVolume, alpha * 0.75f);
        String volumeLabel = Math.round(volume * 100f) + "%";
        MusicUi.text(canvas, volumeLabel, volumeX + volumeWidth + 7f, volumeY - 4f, 7f,
                MusicUi.alpha(MusicUi.TEXT, alpha * 0.52f));
    }

    private void drawButton(Canvas canvas, String icon, float x, float y, float size,
                            float mouseX, float mouseY, float alpha) {
        boolean hovered = inside(mouseX, mouseY, x - 3f, y - 3f, size + 6f, size + 6f);
        if (hovered) MusicUi.rounded(canvas, x - 3f, y - 3f, size + 6f, size + 6f, 6f,
                MusicUi.alpha(0x19FFFFFF, alpha));
        MusicUi.icon(canvas, icon, x, y, size, MusicUi.alpha(MusicUi.TEXT, alpha * (hovered ? 1f : 0.84f)));
    }

    private void drawSlider(Canvas canvas, float x, float y, float w, float value, boolean active, float alpha) {
        float barHeight = active ? 5f : 3f;
        MusicUi.rounded(canvas, x, y - barHeight * 0.5f, w, barHeight, barHeight * 0.5f,
                MusicUi.alpha(0x38FFFFFF, alpha));
        float filledWidth = w * clamp(value);
        if (filledWidth > 0f) MusicUi.rounded(canvas, x, y - barHeight * 0.5f, filledWidth, barHeight,
                barHeight * 0.5f, MusicUi.alpha(MusicUi.TEXT, alpha));
        if (active) MusicUi.rounded(canvas, x + filledWidth - 3.5f, y - 3.5f, 7f, 7f, 3.5f,
                MusicUi.alpha(MusicUi.TEXT, alpha));
    }

    private void drawLyrics(Canvas canvas, float mouseX, float mouseY, float alpha, float delta, long now) {
        List<LyricLine> lyrics = player.lyricsSnapshot();
        rebuildRows(lyrics);
        hitRows.clear();
        if (rows.isEmpty()) {
            String message = localized("暂无歌词", "No lyrics available");
            MusicUi.bold(canvas, message, lyricsX, height * 0.42f, lyricSize * 0.8f,
                    MusicUi.alpha(MusicUi.TEXT, alpha * 0.55f));
            return;
        }
        long playbackPosition = draggingProgress
                ? Math.round(player.totalDurationMs() * pendingProgress) : player.positionMs();
        int activeIndex = LyricLineProcessor.currentIndex(lyrics, playbackPosition);
        int focusedIndex = Math.max(0, Math.min(rows.size() - 1, activeIndex));
        float anchor = Math.max(lyricsTop + 12f, height * 0.25f);
        float automaticTarget = rows.get(focusedIndex).top() - anchor;
        boolean manual = now < manualUntil;
        float target = manual ? manualTarget : automaticTarget;
        float previousScroll = scroll;
        boolean instant = !positioned || draggingProgress;
        if (instant) {
            scroll = target;
            scrollTarget = target;
            scrollTweening = false;
            positioned = true;
        } else if (manual) {
            scrollTarget = target;
            scrollTweening = false;
            scroll = approach(scroll, target, delta, 9f);
        } else {
            if (scrollTarget != target || (!scrollTweening && Math.abs(scroll - target) > 0.1f)) {
                scrollFrom = scroll;
                scrollTarget = target;
                scrollStartNanos = now;
                scrollTweening = true;
            }
            if (scrollTweening) {
                float progress = Math.min(1f, (now - scrollStartNanos) / (float) SCROLL_DURATION_NANOS);
                scroll = scrollFrom + (scrollTarget - scrollFrom) * fastOutSlowIn(progress);
                if (progress >= 1f) scrollTweening = false;
            }
        }
        manualBlur = approach(manualBlur, manual ? 0f : 1f, delta, manual ? 14f : 8f);
        float buffer = Math.max(80f, lyricsBottom - lyricsTop);
        int first = rowAt(scroll + lyricsTop - buffer);
        int last = Math.min(rows.size() - 1, rowAt(scroll + lyricsBottom + buffer) + 1);
        releaseOutside(first, last);
        updateRowSprings(scroll - previousScroll, delta, focusedIndex, !instant && !manual, first, last);
        float blurUnit = lyricSize * BLUR_PER_LINE;
        for (int i = first; i <= last; i++) {
            Row row = rows.get(i);
            float focusTarget = i == focusedIndex ? 1f : 0f;
            if (row.focus < 0f) {
                row.focus = focusTarget;
                row.focusFrom = focusTarget;
                row.focusTarget = focusTarget;
                row.focusStart = now;
            } else if (row.focusTarget != focusTarget) {
                row.focusFrom = row.focus;
                row.focusTarget = focusTarget;
                row.focusStart = now;
            }
            float duration = row.focusTarget > 0.5f ? 0.6f : 0.3f;
            float focusProgress = Math.min(1f, (now - row.focusStart) / (duration * 1_000_000_000f));
            float eased = row.focusTarget > 0.5f ? linearOutSlowIn(focusProgress) : easeInOut(focusProgress);
            row.focus = row.focusFrom + (row.focusTarget - row.focusFrom) * eased;
            float distance = Math.abs(i - focusedIndex);
            float blurTarget = Math.min(distance * blurUnit, lyricSize * 0.5f) * manualBlur;
            if (row.blur < 0f) row.blur = blurTarget;
            else row.blur = approach(row.blur, blurTarget, delta, 12.5f);
            float alphaTarget = i == focusedIndex ? 1f : 0.4f;
            if (row.alpha < 0f) row.alpha = alphaTarget;
            else row.alpha = approach(row.alpha, alphaTarget, delta, 16f);
        }
        int save = canvas.save();
        try {
            canvas.clipRect(Rect.makeXYWH(lyricsX - 8f, lyricsTop, lyricsWidth + 16f, lyricsBottom - lyricsTop));
            for (int i = first; i <= last; i++) {
                Row row = rows.get(i);
                float y = row.top() - scroll + row.springOffset;
                if (y > lyricsBottom) break;
                if (y + row.height() < lyricsTop) continue;
                float visibleY = Math.max(lyricsTop, y);
                float visibleBottom = Math.min(lyricsBottom, y + row.height());
                hitRows.add(new HitRow(row.line(), visibleY, Math.max(0f, visibleBottom - visibleY)));
                boolean hovered = inside(mouseX, mouseY, lyricsX - 6f, visibleY, lyricsWidth + 12f, visibleBottom - visibleY);
                float rowAlpha = row.alpha;
                if (hovered) rowAlpha = Math.max(rowAlpha, 0.8f);
                if (hovered) MusicUi.rounded(canvas, lyricsX - 7f, y - 4f, lyricsWidth + 14f,
                        row.height() + 8f, 7f, MusicUi.alpha(0x13FFFFFF, alpha));
                float scale = 0.98f + 0.02f * row.focus;
                float sigma = row.blur;
                int rowSave = canvas.save();
                try {
                    if (scale < 0.999f) {
                        float centerY = y + row.height() * 0.5f;
                        canvas.translate(lyricsX, centerY);
                        canvas.scale(scale, scale);
                        canvas.translate(-lyricsX, -centerY);
                    }
                    float lineY = y;
                    for (Fragment line : row.primary()) {
                        float edge = edgeAlpha(lineY, lyricSize * 1.25f);
                        drawFragment(canvas, row.line(), line, lineY, lyricSize,
                                MusicUi.alpha(MusicUi.TEXT, alpha * rowAlpha * edge), sigma,
                                playbackPosition, i == activeIndex);
                        lineY += lyricSize * 1.3f;
                    }
                    if (!row.translated().isEmpty()) lineY += 6f;
                    for (Fragment line : row.translated()) {
                        float edge = edgeAlpha(lineY, translationSize * 1.25f);
                        drawFragment(canvas, row.line(), line, lineY, translationSize,
                                MusicUi.alpha(MusicUi.TEXT, alpha * rowAlpha * edge * 0.66f), sigma,
                                playbackPosition, false);
                        lineY += translationSize * 1.35f;
                    }
                } finally {
                    canvas.restoreToCount(rowSave);
                }
            }
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void updateRowSprings(float scrollDelta, float delta, int focusedIndex, boolean chain, int first, int last) {
        if (!chain) {
            for (int i = first; i <= last; i++) {
                Row row = rows.get(i);
                row.springOffset = 0f;
                row.springVelocity = 0f;
            }
            return;
        }
        for (int i = Math.max(first, focusedIndex + 1); i <= last; i++) rows.get(i).springOffset += scrollDelta;
        int steps = Math.max(1, (int) Math.ceil(delta / (1f / 240f)));
        float step = delta / steps;
        for (int s = 0; s < steps; s++) {
            for (int i = last; i >= first; i--) {
                Row row = rows.get(i);
                int distance = Math.max(0, i - focusedIndex);
                float response = Math.max(CHAIN_MIN_RESPONSE, Math.min(1f, 1f - distance * CHAIN_FALLOFF));
                float stiffness = CHAIN_STIFFNESS * response;
                float damping = CHAIN_DAMPING * (float) Math.pow(response, 0.3f);
                float target = i > focusedIndex ? rows.get(i - 1).springOffset * CHAIN_COUPLING : 0f;
                float acceleration = -stiffness * (row.springOffset - target) - damping * row.springVelocity;
                row.springVelocity += acceleration * step;
                row.springOffset += row.springVelocity * step;
            }
        }
        float limit = Math.max(40f, (lyricsBottom - lyricsTop) * 1.5f);
        for (int i = first; i <= last; i++) {
            Row row = rows.get(i);
            if (Math.abs(row.springOffset) > limit) {
                row.springOffset = Math.signum(row.springOffset) * limit;
                row.springVelocity = 0f;
            }
        }
    }

    private static float fastOutSlowIn(float t) {
        return cubicBezier(t, 0.4f, 0f, 0.2f, 1f);
    }

    private static float linearOutSlowIn(float t) {
        return cubicBezier(t, 0f, 0f, 0.2f, 1f);
    }

    private static float easeInOut(float t) {
        return cubicBezier(t, 0.42f, 0f, 0.58f, 1f);
    }

    private static float cubicBezier(float t, float x1, float y1, float x2, float y2) {
        float u = t;
        for (int i = 0; i < 8; i++) {
            float x = bezierCoordinate(u, x1, x2);
            float slope = bezierSlope(u, x1, x2);
            if (Math.abs(slope) < 1e-5f) break;
            u -= (x - t) / slope;
            if (u < 0f) u = 0f;
            if (u > 1f) u = 1f;
        }
        return bezierCoordinate(u, y1, y2);
    }

    private static float bezierCoordinate(float u, float p1, float p2) {
        float v = 1f - u;
        return 3f * v * v * u * p1 + 3f * v * u * u * p2 + u * u * u;
    }

    private static float bezierSlope(float u, float p1, float p2) {
        float v = 1f - u;
        return 3f * v * v * p1 + 6f * v * u * (p2 - p1) + 3f * u * u * (1f - p2);
    }

    private void drawFragment(Canvas canvas, LyricLine owner, Fragment fragment, float top, float size,
                              int color, float sigma, long positionMs, boolean active) {
        if ((color >>> 24) == 0) return;
        if (fragment.prepared == null) fragment.prepared = MusicUi.prepareBold(fragment.value, size);
        if (active && owner.hasWords() && fragment.timeline == null) {
            fragment.timeline = new LyricWordTimeline(owner, fragment.value, fragment.start, size, true);
        }
        boolean timed = active && fragment.timeline != null && !fragment.timeline.isEmpty();
        int base = timed ? MusicUi.alpha(color, 0.4f) : color;
        rasterCache.draw(canvas, fragment.prepared, size, lyricsX, top, base, sigma, 2f);
        if (timed) {
            float highlighted = fragment.timeline.widthAt(positionMs);
            if (highlighted <= 0f) return;
            int save = canvas.save();
            try {
                canvas.clipRect(Rect.makeXYWH(lyricsX, top - 4f, highlighted, size * 2f));
                rasterCache.draw(canvas, fragment.prepared, size, lyricsX, top, color, 0f, 2f);
            } finally {
                canvas.restoreToCount(save);
            }
        }
    }

    private int rowAt(float position) {
        int low = 0;
        int high = rows.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (rows.get(middle).top() + rows.get(middle).height() < position) low = middle + 1;
            else high = middle;
        }
        return Math.min(rows.size() - 1, low);
    }

    private void releaseOutside(int first, int last) {
        for (int i = retainedFirst; i <= retainedLast && i < rows.size(); i++) {
            if (i >= first && i <= last) continue;
            Row row = rows.get(i);
            row.primary().forEach(Fragment::close);
            row.translated().forEach(Fragment::close);
            row.springOffset = 0f;
            row.springVelocity = 0f;
            row.focus = -1f;
            row.blur = -1f;
            row.alpha = -1f;
        }
        retainedFirst = first;
        retainedLast = last;
    }

    private float edgeAlpha(float y, float lineHeight) {
        float center = y + lineHeight * 0.5f;
        return clamp(Math.min((center - lyricsTop) / 32f, (lyricsBottom - center) / 40f));
    }

    private void rebuildRows(List<LyricLine> lyrics) {
        long fontRevision = SkijaUi.textFontRevision();
        if (lyrics == cachedLyrics && Math.abs(cachedWidth - lyricsWidth) < 0.1f
                && cachedTranslation == translationVisible && cachedFontRevision == fontRevision) return;
        cachedLyrics = lyrics;
        cachedWidth = lyricsWidth;
        cachedTranslation = translationVisible;
        cachedFontRevision = fontRevision;
        clearRows();
        float y = 0f;
        for (LyricLine line : lyrics) {
            String primary = line.text() == null || line.text().isBlank() ? "· · ·" : line.text().trim();
            List<Fragment> text = prepareLines(primary, lyricsWidth, lyricSize, Math.max(0, line.text() == null ? 0 : line.text().indexOf(primary)));
            List<Fragment> translated = !translationVisible || line.translation() == null || line.translation().isBlank()
                    ? List.of() : prepareLines(line.translation().trim(), lyricsWidth, translationSize, 0);
            float h = text.size() * lyricSize * 1.3f
                    + (translated.isEmpty() ? 0f : 6f + translated.size() * translationSize * 1.35f);
            rows.add(new Row(line, text, translated, y, h));
            y += h + 20f;
        }
        contentHeight = Math.max(0f, y - 20f);
        positioned = false;
    }

    private void clearRows() {
        rasterCache.clear();
        retainedFirst = 0;
        retainedLast = -1;
        for (Row row : rows) {
            row.primary().forEach(Fragment::close);
            row.translated().forEach(Fragment::close);
        }
        rows.clear();
    }

    private static List<Fragment> prepareLines(String value, float maxWidth, float size, int offset) {
        List<Fragment> result = new ArrayList<>();
        int paragraphStart = 0;
        for (String paragraph : value.split("\\R", -1)) {
            int cursor = 0;
            if (paragraph.isEmpty()) result.add(new Fragment("", paragraphStart + offset));
            while (cursor < paragraph.length()) {
                while (cursor < paragraph.length() && Character.isWhitespace(paragraph.codePointAt(cursor))) {
                    cursor += Character.charCount(paragraph.codePointAt(cursor));
                }
                if (cursor >= paragraph.length()) break;
                int count = paragraph.codePointCount(cursor, paragraph.length());
                int low = 1;
                int high = count;
                while (low < high) {
                    int middle = (low + high + 1) >>> 1;
                    int end = paragraph.offsetByCodePoints(cursor, middle);
                    if (MusicUi.boldWidth(paragraph.substring(cursor, end), size) <= maxWidth) low = middle;
                    else high = middle - 1;
                }
                int end = paragraph.offsetByCodePoints(cursor, low);
                if (end < paragraph.length()) {
                    int space = paragraph.lastIndexOf(' ', end - 1);
                    if (space > cursor) end = space;
                }
                result.add(new Fragment(paragraph.substring(cursor, end).stripTrailing(), paragraphStart + cursor + offset));
                cursor = end;
            }
            paragraphStart += paragraph.length();
            if (paragraphStart < value.length()) {
                if (value.charAt(paragraphStart) == '\r') paragraphStart++;
                if (paragraphStart < value.length() && value.charAt(paragraphStart) == '\n') paragraphStart++;
            }
        }
        return result;
    }

    public boolean mouseClicked(float x, float y, int button) {
        if (button != 0) return false;
        if (inside(x, y, 10f, 8f, 30f, 28f)) {
            closeRequested = true;
            return true;
        }
        if (inside(x, y, leftX - 4f, progressY - 7f, leftWidth + 8f, 14f) && player.currentSong() != null) {
            draggingProgress = true;
            pendingProgress = clamp((x - leftX) / leftWidth);
            manualUntil = 0L;
            return true;
        }
        if (inside(x, y, volumeX - 3f, volumeY - 7f, volumeWidth + 6f, 14f)) {
            draggingVolume = true;
            pendingVolume = clamp((x - volumeX) / volumeWidth);
            player.setVolume(pendingVolume, false);
            return true;
        }
        if (inside(x, y, leftX - 4f, volumeY - 11f, 22f, 22f)) {
            if (player.volume() > 0.001f) {
                lastVolume = player.volume();
                player.setVolume(0f);
            } else {
                player.setVolume(lastVolume);
            }
            return true;
        }
        float center = leftX + leftWidth * 0.5f;
        if (inside(x, y, center - 83f, controlsY - 1f, 28f, 28f)) {
            player.cyclePlaybackMode();
            return true;
        }
        if (inside(x, y, center - 51f, controlsY - 3f, 32f, 32f)) {
            player.playPrevious();
            return true;
        }
        if (inside(x, y, center - 16f, controlsY - 3f, 32f, 32f)) {
            player.toggle();
            return true;
        }
        if (inside(x, y, center + 19f, controlsY - 3f, 32f, 32f)) {
            player.playNext();
            return true;
        }
        if (x >= lyricsX - 6f && x <= lyricsX + lyricsWidth + 6f && player.totalDurationMs() > 0L) {
            for (HitRow hit : hitRows) {
                if (y >= hit.y() && y <= hit.y() + hit.height()) {
                    player.seekToProgress(clamp(hit.line().timeMs() / (float) player.totalDurationMs()));
                    manualUntil = 0L;
                    return true;
                }
            }
        }
        return false;
    }

    public void mouseDragged(float x, float y) {
        if (draggingProgress) pendingProgress = clamp((x - leftX) / leftWidth);
        if (draggingVolume) {
            pendingVolume = clamp((x - volumeX) / volumeWidth);
            player.setVolume(pendingVolume, false);
        }
    }

    public void mouseReleased() {
        if (draggingProgress) {
            draggingProgress = false;
            player.seekToProgress(pendingProgress);
            manualUntil = 0L;
        }
        if (draggingVolume) {
            draggingVolume = false;
            player.setVolume(pendingVolume);
        }
    }

    public boolean mouseScrolled(float x, float y, float amount) {
        if (!inside(x, y, lyricsX - 8f, lyricsTop, lyricsWidth + 16f, lyricsBottom - lyricsTop) || rows.isEmpty()) return false;
        long now = System.nanoTime();
        if (now >= manualUntil) manualTarget = scrollTarget;
        float anchor = Math.max(lyricsTop + 12f, height * 0.25f);
        manualTarget = Math.max(-anchor, Math.min(Math.max(-anchor, contentHeight - anchor), manualTarget - amount * 40f));
        manualUntil = now + 3_000_000_000L;
        return true;
    }

    private float progress() {
        long total = player.totalDurationMs();
        return total <= 0L ? 0f : clamp(player.positionMs() / (float) total);
    }

    private String modeIcon() {
        return switch (player.playbackMode()) {
            case LIST -> "repeat";
            case LOOP -> "repeat_one";
            case RANDOM -> "shuffle";
        };
    }

    private static String fitBold(String value, float width, float size) {
        String text = value == null ? "" : value;
        if (MusicUi.boldWidth(text, size) <= width) return text;
        while (!text.isEmpty() && MusicUi.boldWidth(text + "…", size) > width) {
            text = text.substring(0, text.offsetByCodePoints(text.length(), -1));
        }
        return text + "…";
    }

    private static String time(long millis) {
        long seconds = Math.max(0L, millis) / 1000L;
        return seconds / 60L + ":" + (seconds % 60L < 10L ? "0" : "") + seconds % 60L;
    }

    private static String localized(String chinese, String english) {
        return Config.isChinese ? chinese : english;
    }

    private static float approach(float current, float target, float delta, float speed) {
        return current + (target - current) * (1f - (float) Math.exp(-delta * speed));
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static boolean inside(float x, float y, float left, float top, float w, float h) {
        return x >= left && x <= left + w && y >= top && y <= top + h;
    }

    private static final class Fragment implements AutoCloseable {
        private final String value;
        private final int start;
        private SkijaUi.PreparedText prepared;
        private LyricWordTimeline timeline;

        private Fragment(String value, int start) {
            this.value = value;
            this.start = start;
        }

        @Override
        public void close() {
            if (prepared != null) prepared.close();
            prepared = null;
            timeline = null;
        }
    }

    private static final class Row {
        private final LyricLine line;
        private final List<Fragment> primary;
        private final List<Fragment> translated;
        private final float top;
        private final float height;
        private float focus = -1f;
        private float focusFrom;
        private float focusTarget;
        private long focusStart;
        private float blur = -1f;
        private float alpha = -1f;
        private float springOffset;
        private float springVelocity;

        private Row(LyricLine line, List<Fragment> primary,
                    List<Fragment> translated, float top, float height) {
            this.line = line;
            this.primary = primary;
            this.translated = translated;
            this.top = top;
            this.height = height;
        }

        private LyricLine line() {
            return line;
        }

        private List<Fragment> primary() {
            return primary;
        }

        private List<Fragment> translated() {
            return translated;
        }

        private float top() {
            return top;
        }

        private float height() {
            return height;
        }
    }

    private record HitRow(LyricLine line, float y, float height) {
    }
}
