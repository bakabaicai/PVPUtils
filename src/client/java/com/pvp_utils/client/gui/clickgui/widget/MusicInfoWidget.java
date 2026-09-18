package com.pvp_utils.client.gui.clickgui.widget;

import com.pvp_utils.Config;
import com.pvp_utils.client.NeteaseMusic.MusicPlaybackService;
import com.pvp_utils.client.NeteaseMusic.Song;
import com.pvp_utils.client.gui.clickgui.theme.ClickGuiThemeColors;
import com.pvp_utils.client.render.font.FontRenderer;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;

public class MusicInfoWidget extends SettingWidget {
    private static final float W = 176f;
    private static final float H = 44f;
    private final Paint bgPaint = new Paint().setAntiAlias(true);
    private final Paint trackPaint = new Paint().setAntiAlias(true);
    private final Paint progressPaint = new Paint().setAntiAlias(true);

    @Override public float getWidth() { return W; }
    @Override public float getHeight() { return H; }

    @Override
    public void draw(Canvas canvas, float x, float y, float alpha) {
        ClickGuiThemeColors tc = ClickGuiThemeColors.current();
        bgPaint.setColor(withAlpha(tc.subModule, ClickGuiThemeColors.panelBackgroundAlpha(alpha)));
        canvas.drawRRect(RRect.makeXYWH(x, y, W, H, 8f), bgPaint);

        Song song = MusicPlaybackService.INSTANCE.currentSong();
        if (song == null) {
            FontRenderer.drawText(canvas, Config.isChinese ? "未在播放" : "Idle", x + 10f, y + H / 2f + 4f, 11f, withAlpha(tc.secondaryText, alpha));
            return;
        }

        FontRenderer.drawText(canvas, fit(song.name(), 11f, W - 20f), x + 10f, y + 15f, 11f, withAlpha(tc.primaryText, alpha));
        FontRenderer.drawText(canvas, fit(song.displayArtist(), 9f, W - 20f), x + 10f, y + 27f, 9f, withAlpha(tc.secondaryText, alpha));

        float total = MusicPlaybackService.INSTANCE.totalDurationMs();
        float pos = MusicPlaybackService.INSTANCE.positionMs();
        float progress = total > 0f ? Math.min(1f, pos / total) : 0f;
        float barX = x + 10f;
        float barY = y + 35f;
        float barW = W - 20f;
        trackPaint.setColor(withAlpha(tc.border, alpha));
        canvas.drawRRect(RRect.makeXYWH(barX, barY, barW, 3f, 1.5f), trackPaint);
        if (progress > 0.005f) {
            progressPaint.setColor(withAlpha(tc.accent, alpha));
            canvas.drawRRect(RRect.makeXYWH(barX, barY, Math.max(4f, barW * progress), 3f, 1.5f), progressPaint);
        }
    }

    @Override
    public boolean onClick(float mx, float my, float x, float y, int button) {
        if (button != 0) return false;
        Song song = MusicPlaybackService.INSTANCE.currentSong();
        if (song == null) return false;
        float total = MusicPlaybackService.INSTANCE.totalDurationMs();
        if (total <= 0f) return false;
        float barX = x + 10f;
        float barW = W - 20f;
        float barY = y + 33f;
        if (mx >= barX && mx <= barX + barW && my >= barY && my <= barY + 7f) {
            MusicPlaybackService.INSTANCE.seekToProgress(Math.max(0f, Math.min(1f, (mx - barX) / barW)));
            return true;
        }
        return false;
    }

    private static String fit(String text, float size, float maxWidth) {
        if (text == null || text.isBlank()) return "";
        if (FontRenderer.measureTextWidth(text, size) <= maxWidth) return text;
        String clipped = text;
        while (!clipped.isEmpty() && FontRenderer.measureTextWidth(clipped + "...", size) > maxWidth) {
            clipped = clipped.substring(0, clipped.length() - 1);
        }
        return clipped + "...";
    }
}
