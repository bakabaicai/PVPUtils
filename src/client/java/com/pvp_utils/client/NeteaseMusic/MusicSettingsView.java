package com.pvp_utils.client.NeteaseMusic;

import com.pvp_utils.Config;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.types.Rect;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MusicSettingsView {
    private static final float INSET = 24f;
    private static final float CONTENT_TOP = 86f;
    private static final float ROW_HEIGHT = 24f;
    private final List<Control> controls = new ArrayList<>();
    private int tab;
    private float width;
    private float height;
    private float contentWidth;
    private float scroll;
    private float contentHeight;
    private Control dragging;
    private boolean dirty;
    private boolean lyricPreview;

    public void render(Canvas canvas, float w, float h, float mouseX, float mouseY, float alpha) {
        width = w;
        height = h;
        float previewWidth = w >= 660f ? Math.min(300f, w - 460f) : 0f;
        contentWidth = Math.max(1f, w - INSET * 2f - (previewWidth > 0f ? previewWidth + 12f : 0f));
        contentHeight = tab == 0 ? 146f : tab == 1 ? 70f : 198f;
        scroll = Math.max(0f, Math.min(maxScroll(), scroll));
        controls.clear();
        lyricPreview = tab == 1;
        MusicUi.bold(canvas, text("设置", "Settings"), INSET, 18f, 17f, color(MusicUi.TEXT, alpha));
        String[] labels = {text("通用", "General"), text("歌词", "Lyrics"), text("布局", "Layout")};
        for (int i = 0; i < labels.length; i++) {
            float x = INSET + i * 84f;
            boolean hovered = hit(mouseX, mouseY, x, 52f, 78f, 24f);
            MusicUi.rounded(canvas, x, 52f, 78f, 24f, 5f,
                    color(tab == i ? MusicUi.ACCENT : hovered ? 0xFF34363D : MusicUi.PANEL, alpha));
            MusicUi.bold(canvas, labels[i], x + (78f - MusicUi.boldWidth(labels[i], 8f)) * 0.5f,
                    60f, 8f, color(MusicUi.TEXT, alpha));
        }
        float resetX = Math.max(INSET + 252f, width - INSET - 84f);
        boolean resetHovered = hit(mouseX, mouseY, resetX, 52f, 84f, 24f);
        MusicUi.rounded(canvas, resetX, 52f, 84f, 24f, 5f,
                color(resetHovered ? 0xFF34363D : MusicUi.PANEL, alpha));
        String reset = text("重置此页", "Reset page");
        MusicUi.bold(canvas, reset, resetX + (84f - MusicUi.boldWidth(reset, 8f)) * 0.5f,
                60f, 8f, color(MusicUi.TEXT, alpha));
        int save = canvas.save();
        try {
            canvas.clipRect(Rect.makeXYWH(INSET, CONTENT_TOP, contentWidth, Math.max(1f, height - CONTENT_TOP - 24f)));
            float y = CONTENT_TOP - scroll;
            if (tab == 0) {
                y = section(canvas, text("音乐信息", "Music information"), y, alpha);
                y = toggle(canvas, "info", text("显示音乐信息", "Show music information"), Config.musicInfoHud, y, mouseX, mouseY, alpha);
                y = mode(canvas, y, mouseX, mouseY, alpha);
                y = toggle(canvas, "rounded", text("圆角封面", "Rounded cover"), Config.musicInfoHudCoverRounded, y, mouseX, mouseY, alpha);
                y = section(canvas, text("播放", "Playback"), y + 8f, alpha);
                slider(canvas, Slider.VOLUME, text("音量", "Volume"), y, mouseX, mouseY, alpha);
            } else if (tab == 1) {
                y = section(canvas, text("歌词显示", "Lyrics display"), y, alpha);
                y = toggle(canvas, "lyrics", text("显示歌词", "In-game lyrics"), Config.lyricsDisplay, y, mouseX, mouseY, alpha);
                toggle(canvas, "island", text("灵动岛歌词", "Dynamic Island lyrics"), Config.dynamicIslandLyrics, y, mouseX, mouseY, alpha);
            } else {
                y = section(canvas, text("音乐信息", "Music information"), y, alpha);
                y = slider(canvas, Slider.INFO_SCALE, text("缩放", "Scale"), y, mouseX, mouseY, alpha);
                y = slider(canvas, Slider.INFO_X, text("水平位置", "Horizontal position"), y, mouseX, mouseY, alpha);
                y = slider(canvas, Slider.INFO_Y, text("垂直位置", "Vertical position"), y, mouseX, mouseY, alpha);
                y = section(canvas, text("歌词", "Lyrics"), y + 8f, alpha);
                y = slider(canvas, Slider.LYRICS_SCALE, text("缩放", "Scale"), y, mouseX, mouseY, alpha);
                y = slider(canvas, Slider.LYRICS_X, text("水平位置", "Horizontal position"), y, mouseX, mouseY, alpha);
                slider(canvas, Slider.LYRICS_Y, text("垂直位置", "Vertical position"), y, mouseX, mouseY, alpha);
            }
        } finally {
            canvas.restoreToCount(save);
        }
        if (maxScroll() > 0f) {
            float viewHeight = Math.max(1f, height - CONTENT_TOP - 24f);
            float handleHeight = Math.max(24f, viewHeight * viewHeight / contentHeight);
            float handleY = CONTENT_TOP + scroll / maxScroll() * Math.max(0f, viewHeight - handleHeight);
            MusicUi.rounded(canvas, INSET + contentWidth - 3f, handleY, 3f, handleHeight, 1.5f,
                    color(MusicUi.MUTED, alpha * 0.65f));
        }
        if (previewWidth > 0f) renderPreview(canvas, w - INSET - previewWidth, previewWidth, alpha);
    }

    private float section(Canvas canvas, String title, float y, float alpha) {
        MusicUi.bold(canvas, title, INSET + 4f, y + 3f, 7.5f, color(MusicUi.MUTED, alpha));
        float start = INSET + MusicUi.boldWidth(title, 7.5f) + 14f;
        MusicUi.line(canvas, start, y + 8f, INSET + contentWidth, y + 8f, 0.5f, color(0x16FFFFFF, alpha));
        return y + 16f;
    }

    private void row(Canvas canvas, String title, float y, float controlWidth, float mouseX, float mouseY, float alpha) {
        boolean hovered = mouseY >= CONTENT_TOP && mouseY <= height - 24f
                && hit(mouseX, mouseY, INSET, y, contentWidth, ROW_HEIGHT);
        MusicUi.rounded(canvas, INSET, y, contentWidth, ROW_HEIGHT, 6f,
                color(hovered ? 0xFF303238 : MusicUi.PANEL, alpha));
        MusicUi.bold(canvas, MusicUi.fit(title, Math.max(1f, contentWidth - controlWidth - 36f), 8f),
                INSET + 12f, y + 7f, 8f, color(MusicUi.TEXT, alpha));
    }

    private float toggle(Canvas canvas, String id, String title, boolean value, float y, float mouseX, float mouseY, float alpha) {
        row(canvas, title, y, 24f, mouseX, mouseY, alpha);
        float x = INSET + contentWidth - 36f;
        MusicUi.rounded(canvas, x, y + 6f, 24f, 12f, 6f,
                color(value ? MusicUi.ACCENT : 0xFF55575E, alpha));
        MusicUi.rounded(canvas, x + (value ? 14f : 2f), y + 8f, 8f, 8f, 4f,
                color(MusicUi.TEXT, alpha));
        controls.add(new Control(id, x - 4f, y, 32f, ROW_HEIGHT, null));
        return y + ROW_HEIGHT + 2f;
    }

    private float mode(Canvas canvas, float y, float mouseX, float mouseY, float alpha) {
        row(canvas, text("显示样式", "Display style"), y, 104f, mouseX, mouseY, alpha);
        float x = INSET + contentWidth - 116f;
        MusicUi.rounded(canvas, x, y + 3f, 104f, 18f, 4f, color(0xFF383A42, alpha));
        MusicUi.text(canvas, modeName(), x + 8f, y + 7.5f, 7.5f, color(MusicUi.TEXT, alpha));
        MusicUi.icon(canvas, "chevron_right", x + 87f, y + 7f, 10f, color(MusicUi.MUTED, alpha));
        controls.add(new Control("mode", x, y, 104f, ROW_HEIGHT, null));
        return y + ROW_HEIGHT + 2f;
    }

    private float slider(Canvas canvas, Slider slider, String title, float y, float mouseX, float mouseY, float alpha) {
        float controlWidth = Math.max(112f, Math.min(160f, contentWidth * 0.4f));
        row(canvas, title, y, controlWidth, mouseX, mouseY, alpha);
        float x = INSET + contentWidth - controlWidth - 12f;
        float trackWidth = controlWidth - 36f;
        float fraction = Math.max(0f, Math.min(1f, (slider.value() - slider.min()) / (slider.max() - slider.min())));
        MusicUi.rounded(canvas, x, y + 11f, trackWidth, 2f, 1f, color(0xFF484B52, alpha));
        MusicUi.rounded(canvas, x, y + 11f, trackWidth * fraction, 2f, 1f, color(MusicUi.ACCENT, alpha));
        MusicUi.rounded(canvas, x + trackWidth * fraction - 3f, y + 9f, 6f, 6f, 3f, color(MusicUi.TEXT, alpha));
        String value = slider.label();
        MusicUi.text(canvas, value, x + controlWidth - MusicUi.width(value, 7.5f), y + 7f, 7.5f,
                color(MusicUi.MUTED, alpha));
        Control control = new Control(slider.name(), x, y, trackWidth, ROW_HEIGHT, slider);
        controls.add(control);
        if (dragging != null && dragging.slider == slider) dragging = control;
        if (hit(mouseX, mouseY, INSET, y, contentWidth, ROW_HEIGHT) && mouseY >= CONTENT_TOP
                && mouseY <= height - 24f) lyricPreview = slider.name().startsWith("LYRICS");
        return y + ROW_HEIGHT + 2f;
    }

    private void renderPreview(Canvas canvas, float x, float w, float alpha) {
        float h = Math.min(236f, height - CONTENT_TOP - 24f);
        if (h < 70f) return;
        MusicUi.rounded(canvas, x - 1f, CONTENT_TOP - 1f, w + 2f, h + 2f, 10f, color(0x22FFFFFF, alpha));
        MusicUi.rounded(canvas, x, CONTENT_TOP, w, h, 9f, color(MusicUi.SIDEBAR, alpha));
        String title = lyricPreview ? text("歌词", "Lyrics") : text("音乐信息", "Music information");
        MusicUi.bold(canvas, title, x + 12f, CONTENT_TOP + 9f, 8f, color(MusicUi.TEXT, alpha));
        String label = text("实时预览", "Preview");
        MusicUi.text(canvas, label, x + w - 12f - MusicUi.width(label, 7.5f), CONTENT_TOP + 9f, 7.5f,
                color(MusicUi.MUTED, alpha));
        float innerX = x + 8f;
        float innerY = CONTENT_TOP + 28f;
        float innerW = w - 16f;
        float innerH = h - 36f;
        MusicUi.rounded(canvas, innerX, innerY, innerW, innerH, 6f, color(0xFF090A0D, alpha));
        MusicUi.line(canvas, innerX, innerY + innerH * 0.5f, innerX + innerW, innerY + innerH * 0.5f,
                1f, color(0x0FFFFFFF, alpha));
        MusicUi.line(canvas, innerX + innerW * 0.5f, innerY, innerX + innerW * 0.5f, innerY + innerH,
                1f, color(0x0FFFFFFF, alpha));
        int save = canvas.save();
        try {
            canvas.clipRect(Rect.makeXYWH(innerX, innerY, innerW, innerH));
            float userScale = lyricPreview ? Config.lyricsDisplayScale : Config.musicInfoHudScale;
            float scale = Math.min(innerW / 320f, innerH / 160f) * Math.max(0.5f, userScale);
            float offsetX = tab == 2 ? (lyricPreview ? Config.lyricsDisplayX : Config.musicInfoHudX) * 0.08f : 0f;
            float offsetY = tab == 2 ? (lyricPreview ? Config.lyricsDisplayY : Config.musicInfoHudY) * 0.08f : 0f;
            canvas.translate(innerX + innerW * 0.5f + offsetX, innerY + innerH * 0.5f + offsetY);
            canvas.scale(scale, scale);
            if (lyricPreview) drawLyricsPreview(canvas, alpha);
            else drawMusicPreview(canvas, alpha);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void drawMusicPreview(Canvas canvas, float alpha) {
        boolean lite = Config.musicInfoHudMode == Config.MusicInfoHudMode.LITE;
        float w = lite ? 190f : 216f;
        float h = lite ? 58f : 68f;
        MusicUi.rounded(canvas, -w * 0.5f, -h * 0.5f, w, h, lite ? 8f : 16f,
                color(Config.musicInfoHudMode == Config.MusicInfoHudMode.LIQUID_GLASS ? 0xA5444750 : 0xEC20232B, alpha));
        float coverSize = lite ? 42f : 48f;
        float left = -w * 0.5f + 10f;
        Song song = MusicPlaybackService.INSTANCE.currentSong();
        MusicUi.cover(canvas, song == null ? null : NeteaseMusicCovers.skiaImage(song.image()), left,
                -coverSize * 0.5f, coverSize, Config.musicInfoHudCoverRounded ? 10f : 0f, Math.round(alpha * 255f));
        String title = song == null ? "PVPUtils Music" : song.name();
        String artist = song == null ? text("正在播放", "Now playing") : song.displayArtist();
        float textX = left + coverSize + 10f;
        float textW = w - coverSize - 30f;
        MusicUi.bold(canvas, MusicUi.fit(title, textW, 12f), textX, -19f, 12f, color(MusicUi.TEXT, alpha));
        MusicUi.text(canvas, MusicUi.fit(artist, textW, 10f), textX, -2f, 10f, color(MusicUi.MUTED, alpha));
        MusicUi.rounded(canvas, textX, 17f, textW, 3f, 1.5f, color(0xFF464A54, alpha));
        MusicUi.rounded(canvas, textX, 17f, textW * 0.42f, 3f, 1.5f, color(MusicUi.ACCENT, alpha));
    }

    private void drawLyricsPreview(Canvas canvas, float alpha) {
        List<LyricLine> lyrics = MusicPlaybackService.INSTANCE.lyricsSnapshot();
        int active = Math.max(0, LyricLineProcessor.currentIndex(lyrics, MusicPlaybackService.INSTANCE.positionMs()));
        String[] fallback = {text("让音乐陪伴每一次旅程", "Music for every journey"), "PVPUtils Music", text("此刻，听见你的热爱", "Find your rhythm")};
        for (int i = -1; i <= 1; i++) {
            int index = active + i;
            String value = index >= 0 && index < lyrics.size() ? lyrics.get(index).text() : fallback[i + 1];
            float size = i == 0 ? 20f : 13f;
            value = MusicUi.fit(value, 286f, size);
            MusicUi.bold(canvas, value, -MusicUi.boldWidth(value, size) * 0.5f, i * 33f - size * 0.5f, size,
                    color(i == 0 ? MusicUi.TEXT : MusicUi.MUTED, alpha * (i == 0 ? 1f : 0.5f)));
        }
    }

    public boolean mouseClicked(float x, float y, int button) {
        if (button != 0) return false;
        for (int i = 0; i < 3; i++) {
            if (hit(x, y, INSET + i * 84f, 52f, 78f, 24f)) {
                mouseReleased();
                tab = i;
                scroll = 0f;
                controls.clear();
                return true;
            }
        }
        if (hit(x, y, Math.max(INSET + 252f, width - INSET - 84f), 52f, 84f, 24f)) {
            mouseReleased();
            resetPage();
            return true;
        }
        if (y < CONTENT_TOP || y > height - 24f) return false;
        for (Control control : controls) {
            if (!hit(x, y, control.x - (control.slider == null ? 0f : 5f), control.y,
                    control.width + (control.slider == null ? 0f : 10f), control.height)) continue;
            if (control.slider != null) {
                dragging = control;
                mouseDragged(x, y);
            } else {
                switch (control.id) {
                    case "info" -> Config.musicInfoHud = !Config.musicInfoHud;
                    case "rounded" -> Config.musicInfoHudCoverRounded = !Config.musicInfoHudCoverRounded;
                    case "lyrics" -> Config.lyricsDisplay = !Config.lyricsDisplay;
                    case "island" -> Config.dynamicIslandLyrics = !Config.dynamicIslandLyrics;
                    case "mode" -> {
                        Config.MusicInfoHudMode[] modes = Config.MusicInfoHudMode.values();
                        Config.musicInfoHudMode = modes[(Config.musicInfoHudMode.ordinal() + 1) % modes.length];
                    }
                    default -> { return false; }
                }
                Config.save();
            }
            return true;
        }
        return false;
    }

    public boolean mouseDragged(float x, float y) {
        if (dragging == null) return false;
        float fraction = Math.max(0f, Math.min(1f, (x - dragging.x) / dragging.width));
        Slider slider = dragging.slider;
        float value = slider.min() + fraction * (slider.max() - slider.min());
        slider.set(slider.position() ? Math.round(value) : Math.round(value * 100f) / 100f);
        dirty = true;
        return true;
    }

    public void mouseReleased() {
        dragging = null;
        if (!dirty) return;
        dirty = false;
        Config.save();
    }

    public boolean mouseScrolled(float x, float y, float amount) {
        if (!hit(x, y, INSET, CONTENT_TOP, contentWidth, Math.max(1f, height - CONTENT_TOP - 24f))) return false;
        scroll = Math.max(0f, Math.min(maxScroll(), scroll - amount * 28f));
        return true;
    }

    private void resetPage() {
        if (tab == 0) {
            Config.musicInfoHud = false;
            Config.musicInfoHudCoverRounded = true;
            Config.musicInfoHudMode = Config.MusicInfoHudMode.LITE;
            Config.neteaseMusicVolume = 1f;
            MusicPlaybackService.INSTANCE.setVolume(1f, false);
        } else if (tab == 1) {
            Config.lyricsDisplay = false;
            Config.dynamicIslandLyrics = false;
        } else {
            Config.musicInfoHudScale = 1f;
            Config.musicInfoHudX = 0f;
            Config.musicInfoHudY = 0f;
            Config.lyricsDisplayScale = 1f;
            Config.lyricsDisplayX = 0f;
            Config.lyricsDisplayY = 0f;
        }
        Config.save();
    }

    private float maxScroll() {
        return Math.max(0f, contentHeight - Math.max(1f, height - CONTENT_TOP - 24f));
    }

    private static String modeName() {
        return switch (Config.musicInfoHudMode) {
            case LITE -> text("简洁", "Compact");
            case NEW -> text("卡片", "Card");
            case BLUR -> text("毛玻璃", "Blur");
            case LIQUID_GLASS -> text("液态玻璃", "Liquid glass");
        };
    }

    private static int color(int value, float alpha) {
        return MusicUi.alpha(value, alpha);
    }

    private static String text(String chinese, String english) {
        return Config.isChinese ? chinese : english;
    }

    private static boolean hit(float x, float y, float left, float top, float w, float h) {
        return x >= left && x <= left + w && y >= top && y <= top + h;
    }

    private record Control(String id, float x, float y, float width, float height, Slider slider) {
    }

    private enum Slider {
        VOLUME, INFO_SCALE, INFO_X, INFO_Y, LYRICS_SCALE, LYRICS_X, LYRICS_Y;

        private float value() {
            return switch (this) {
                case VOLUME -> Config.neteaseMusicVolume;
                case INFO_SCALE -> Config.musicInfoHudScale;
                case INFO_X -> Config.musicInfoHudX;
                case INFO_Y -> Config.musicInfoHudY;
                case LYRICS_SCALE -> Config.lyricsDisplayScale;
                case LYRICS_X -> Config.lyricsDisplayX;
                case LYRICS_Y -> Config.lyricsDisplayY;
            };
        }

        private void set(float value) {
            switch (this) {
                case VOLUME -> {
                    Config.neteaseMusicVolume = value;
                    MusicPlaybackService.INSTANCE.setVolume(value, false);
                }
                case INFO_SCALE -> Config.musicInfoHudScale = value;
                case INFO_X -> Config.musicInfoHudX = value;
                case INFO_Y -> Config.musicInfoHudY = value;
                case LYRICS_SCALE -> Config.lyricsDisplayScale = value;
                case LYRICS_X -> Config.lyricsDisplayX = value;
                case LYRICS_Y -> Config.lyricsDisplayY = value;
            }
        }

        private boolean position() {
            return this == INFO_X || this == INFO_Y || this == LYRICS_X || this == LYRICS_Y;
        }

        private float min() {
            return position() ? -1000f : this == VOLUME ? 0f : 0.5f;
        }

        private float max() {
            return position() ? 1000f : this == VOLUME ? 1f : 2f;
        }

        private String label() {
            return position() ? String.format(Locale.ROOT, "%.0f", value()) : Math.round(value() * 100f) + "%";
        }
    }
}
