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
    private final MusicPlaybackService player = MusicPlaybackService.INSTANCE;
    private MusicBlurredCover background = new MusicBlurredCover();
    private MusicBlurredCover previousBackground = new MusicBlurredCover();
    private final List<Row> rows = new ArrayList<>();
    private final List<HitRow> hitRows = new ArrayList<>();
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
        background.close();
        previousBackground.close();
        clearRows();
        hitRows.clear();
        cachedLyrics = List.of();
        cachedWidth = -1f;
        positioned = false;
        lastFrameNanos = 0L;
    }

    public void render(Canvas canvas, float w, float h, float mouseX, float mouseY, float opacity) {
        if (w <= 0f || h <= 0f) return;
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
        lyricSize = Math.min(32.5f, Math.max(17f, lyricsWidth / 12f));
        translationSize = lyricSize * 0.52f;
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
        scrollTarget = now < manualUntil ? manualTarget : automaticTarget;
        if (!positioned || draggingProgress) {
            scroll = scrollTarget;
            positioned = true;
        } else {
            scroll = approach(scroll, scrollTarget, delta, 9f);
        }
        int save = canvas.save();
        try {
            canvas.clipRect(Rect.makeXYWH(lyricsX - 8f, lyricsTop, lyricsWidth + 16f, lyricsBottom - lyricsTop));
            for (int i = 0; i < rows.size(); i++) {
                Row row = rows.get(i);
                float y = row.top() - scroll;
                if (y > lyricsBottom) break;
                if (y + row.height() < lyricsTop) continue;
                float visibleY = Math.max(lyricsTop, y);
                float visibleBottom = Math.min(lyricsBottom, y + row.height());
                hitRows.add(new HitRow(row.line(), visibleY, Math.max(0f, visibleBottom - visibleY)));
                boolean hovered = inside(mouseX, mouseY, lyricsX - 6f, visibleY, lyricsWidth + 12f, visibleBottom - visibleY);
                boolean active = i == activeIndex;
                float distance = Math.abs(i - focusedIndex);
                float focus = active ? 1f : hovered ? 0.8f : Math.max(0.2f, 0.46f - distance * 0.045f);
                if (hovered) MusicUi.rounded(canvas, lyricsX - 7f, y - 4f, lyricsWidth + 14f,
                        row.height() + 8f, 7f, MusicUi.alpha(0x13FFFFFF, alpha));
                float lineY = y;
                for (SkijaUi.PreparedText line : row.primary()) {
                    float edge = edgeAlpha(lineY, lyricSize * 1.25f);
                    line.drawTop(canvas, lyricsX, lineY, MusicUi.alpha(MusicUi.TEXT, alpha * focus * edge));
                    lineY += lyricSize * 1.3f;
                }
                if (!row.translated().isEmpty()) lineY += 6f;
                for (SkijaUi.PreparedText line : row.translated()) {
                    float edge = edgeAlpha(lineY, translationSize * 1.25f);
                    line.drawTop(canvas, lyricsX, lineY, MusicUi.alpha(MusicUi.TEXT, alpha * focus * edge * 0.66f));
                    lineY += translationSize * 1.35f;
                }
            }
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private float edgeAlpha(float y, float lineHeight) {
        float center = y + lineHeight * 0.5f;
        return clamp(Math.min((center - lyricsTop) / 32f, (lyricsBottom - center) / 40f));
    }

    private void rebuildRows(List<LyricLine> lyrics) {
        long fontRevision = SkijaUi.textFontRevision();
        if (lyrics.equals(cachedLyrics) && Math.abs(cachedWidth - lyricsWidth) < 0.1f
                && cachedTranslation == translationVisible && cachedFontRevision == fontRevision) return;
        cachedLyrics = lyrics;
        cachedWidth = lyricsWidth;
        cachedTranslation = translationVisible;
        cachedFontRevision = fontRevision;
        clearRows();
        float y = 0f;
        for (LyricLine line : lyrics) {
            String primary = line.text() == null || line.text().isBlank() ? "· · ·" : line.text().trim();
            List<SkijaUi.PreparedText> text = prepareLines(primary, lyricsWidth, lyricSize);
            List<SkijaUi.PreparedText> translated = !translationVisible || line.translation() == null || line.translation().isBlank()
                    ? List.of() : prepareLines(line.translation().trim(), lyricsWidth, translationSize);
            float h = text.size() * lyricSize * 1.3f
                    + (translated.isEmpty() ? 0f : 6f + translated.size() * translationSize * 1.35f);
            rows.add(new Row(line, text, translated, y, h));
            y += h + 20f;
        }
        contentHeight = Math.max(0f, y - 20f);
        positioned = false;
    }

    private void clearRows() {
        for (Row row : rows) {
            row.primary().forEach(SkijaUi.PreparedText::close);
            row.translated().forEach(SkijaUi.PreparedText::close);
        }
        rows.clear();
    }

    private static List<SkijaUi.PreparedText> prepareLines(String value, float maxWidth, float size) {
        return wrap(value, maxWidth, size).stream().map(line -> MusicUi.prepareBold(line, size)).toList();
    }

    private static List<String> wrap(String value, float maxWidth, float size) {
        List<String> result = new ArrayList<>();
        for (String paragraph : value.split("\\R", -1)) {
            String remaining = paragraph.trim();
            if (remaining.isEmpty()) {
                result.add("");
                continue;
            }
            while (!remaining.isEmpty()) {
                int end = 0;
                int lastSpace = -1;
                for (int at = 0; at < remaining.length();) {
                    int next = at + Character.charCount(remaining.codePointAt(at));
                    if (MusicUi.boldWidth(remaining.substring(0, next), size) > maxWidth && at > 0) break;
                    end = next;
                    if (Character.isWhitespace(remaining.codePointAt(at))) lastSpace = at;
                    at = next;
                }
                if (end == 0) end = Character.charCount(remaining.codePointAt(0));
                if (end < remaining.length() && lastSpace > 0) end = lastSpace;
                result.add(remaining.substring(0, end).stripTrailing());
                remaining = remaining.substring(end).stripLeading();
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

    private record Row(LyricLine line, List<SkijaUi.PreparedText> primary,
                       List<SkijaUi.PreparedText> translated, float top, float height) {
    }

    private record HitRow(LyricLine line, float y, float height) {
    }
}
