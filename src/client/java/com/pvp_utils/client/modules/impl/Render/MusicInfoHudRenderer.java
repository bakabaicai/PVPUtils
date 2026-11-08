package com.pvp_utils.client.modules.impl.Render;

import io.github.humbleui.types.RRect;

import com.pvp_utils.client.render.skia.SkijaUi;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.HudPanel;
import com.pvp_utils.client.render.skia.SoftShadow;
import com.pvp_utils.client.NeteaseMusic.MusicPlaybackService;
import com.pvp_utils.client.NeteaseMusic.NeteaseMusicCovers;
import com.pvp_utils.client.NeteaseMusic.Song;
import com.pvp_utils.client.render.skia.SkijaRenderer;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.Mth;

public class MusicInfoHudRenderer {
    private static final MusicInfoHudRenderer INSTANCE = new MusicInfoHudRenderer();
    private static final float LITE_W = 190f;
    private static final float LITE_H = 58f;
    private static final float CARD_W = 216f;
    private static final float CARD_H = 68f;
    private static final float RADIUS = 16f;
    private static final int COVER_SIZE = 42;
    private static final int NEW_COVER_SIZE = 48;
    private static final int ACCENT = 0xFFE5484D;

    private final Paint coverImagePaint = new Paint().setAntiAlias(true);

    private boolean pendingFrame;

    public static MusicInfoHudRenderer getInstance() {
        return INSTANCE;
    }

    public void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        if (!Config.musicInfoHud) {
            return;
        }
        if (client.player == null || client.options.hideGui) {
            return;
        }
        if (client.screen != null && !(client.screen instanceof ChatScreen) && !HudEditOverlay.getInstance().isActive()) {
            return;
        }

        MusicPlaybackService player = MusicPlaybackService.INSTANCE;
        Song song = player.currentSong();
        if (song == null) {
            return;
        }

        if (Config.musicInfoHudMode != Config.MusicInfoHudMode.LITE) {
            pendingFrame = true;
        } else {
            pendingFrame = false;
            renderLite(graphics, client, player, song);
        }
    }

    public void renderSkija(Canvas canvas) {
        if (!Config.musicInfoHud || Config.musicInfoHudMode == Config.MusicInfoHudMode.LITE) {
            pendingFrame = false;
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (!pendingFrame || client.player == null || client.options.hideGui) {
            return;
        }
        if (client.screen != null && !(client.screen instanceof ChatScreen) && !HudEditOverlay.getInstance().isActive()) {
            return;
        }
        pendingFrame = false;

        MusicPlaybackService player = MusicPlaybackService.INSTANCE;
        Song song = player.currentSong();
        if (song == null) {
            return;
        }
        renderCard(canvas, client, player, song, true);
    }

    private void renderCard(Canvas canvas, Minecraft client, MusicPlaybackService player, Song song, boolean blurMode) {
        int save = canvas.save();
        try {
            int screenW = client.getWindow().getGuiScaledWidth();
            int screenH = client.getWindow().getGuiScaledHeight();
            float userScale = getScale();
            float x = getRenderX(screenW);
            float y = getRenderY(screenH);
            float scaledW = CARD_W * userScale;
            float scaledH = CARD_H * userScale;

            HudPanel.draw(canvas, x, y, scaledW, scaledH, RADIUS * userScale,
                    Config.musicInfoAppearance, 1f);
            canvas.translate(x, y);
            canvas.scale(userScale, userScale);

            drawCardContent(canvas, song, player.positionMs(), player.totalDurationMs(), player.playbackMode().label(), blurMode);

        } finally {
            canvas.restoreToCount(save);
        }
    }

    public void drawPreview(Canvas canvas, Song song, long positionMs, long totalMs, String playbackMode, float alpha) {
        if (song == null || alpha <= 0f) return;
        int save = canvas.save();
        try (Paint opacity = alpha < 1f ? new Paint().setAlphaf(Math.clamp(alpha, 0f, 1f)) : null) {
            if (opacity != null) canvas.saveLayer(Rect.makeWH(getBaseWidth(), getBaseHeight()), opacity);
            if (Config.musicInfoHudMode == Config.MusicInfoHudMode.LITE) {
                drawLiteCard(new SkiaLitePainter(canvas), song, positionMs, totalMs, playbackMode);
            } else {
                SkijaUi.rounded(canvas, 0f, 0f, CARD_W, CARD_H, RADIUS, Config.musicInfoAppearance.background());
                drawCardContent(canvas, song, positionMs, totalMs, playbackMode, true);
            }
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void drawCardContent(Canvas canvas, Song song, long positionMs, long totalMs, String playbackMode, boolean blurMode) {
        SkijaUi.rounded(canvas, 10f, 10f, NEW_COVER_SIZE, NEW_COVER_SIZE, 12f, coverBackplateColor(blurMode));

        Image cover = NeteaseMusicCovers.skiaImage(song.image());
        if (cover != null) {
            int coverSave = canvas.save();
            try {
                if (Config.musicInfoHudCoverRounded) {
                    canvas.clipRRect(RRect.makeXYWH(10f, 10f, NEW_COVER_SIZE, NEW_COVER_SIZE, 12f), true);
                } else {
                    canvas.clipRect(Rect.makeXYWH(10f, 10f, NEW_COVER_SIZE, NEW_COVER_SIZE));
                }
                canvas.drawImageRect(cover,
                Rect.makeXYWH(0f, 0f, cover.getWidth(), cover.getHeight()),
                Rect.makeXYWH(10f, 10f, NEW_COVER_SIZE, NEW_COVER_SIZE),
                SamplingMode.LINEAR, coverImagePaint, true);
            } finally {
                canvas.restoreToCount(coverSave);
            }
        } else {
            SkijaUi.text(canvas, "♪", 10f + NEW_COVER_SIZE / 2f - 4f, (10f + NEW_COVER_SIZE / 2f + 4f) + SkijaUi.textMetrics(12f).getAscent(), SkijaUi.textMetrics(12f).getDescent() - SkijaUi.textMetrics(12f).getAscent(), 0x66FFFFFF, 12f);
        }

        SkijaUi.text(canvas, trimSkia(song.name(), 128f, 13f), 70f, (24f) + SkijaUi.textMetrics(13f).getAscent(), SkijaUi.textMetrics(13f).getDescent() - SkijaUi.textMetrics(13f).getAscent(), primaryTextColor(blurMode), 13f);
        SkijaUi.text(canvas, trimSkia(song.displayArtist(), 128f, 11f), 70f, (40f) + SkijaUi.textMetrics(11f).getAscent(), SkijaUi.textMetrics(11f).getDescent() - SkijaUi.textMetrics(11f).getAscent(), mutedTextColor(blurMode), 11f);

        long total = Math.max(0L, totalMs);
        long position = Math.max(0L, Math.min(positionMs, Math.max(total, 0L)));
        float progress = total <= 0L ? 0f : Mth.clamp(position / (float) total, 0f, 1f);
        float barX = 70f;
        float barY = 51f;
        float barW = 112f;
        float barH = 5f;
        SkijaUi.rounded(canvas, barX, barY, barW, barH, barH * 0.5f, trackColor(blurMode));
        SkijaUi.rounded(canvas, barX, barY, Math.max(barH, barW * progress), barH, barH * 0.5f, Config.musicInfoAppearance.accent());
        SkijaUi.text(canvas, MusicPlaybackService.formatTime(position) + " / " + MusicPlaybackService.formatTime(total), 70f, (64f) + SkijaUi.textMetrics(9f).getAscent(), SkijaUi.textMetrics(9f).getDescent() - SkijaUi.textMetrics(9f).getAscent(), mutedTextColor(blurMode), 9f);
        String mode = playbackMode;
        SkijaUi.text(canvas, mode, CARD_W - 14f - SkijaUi.textWidth(mode, 9f), (64f) + SkijaUi.textMetrics(9f).getAscent(), SkijaUi.textMetrics(9f).getDescent() - SkijaUi.textMetrics(9f).getAscent(), mutedTextColor(blurMode), 9f);
    }

    private void renderLite(GuiGraphics graphics, Minecraft client, MusicPlaybackService player, Song song) {
        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        float x = getRenderX(screenW);
        float y = getRenderY(screenH);
        float scale = getScale();

        if (Config.musicInfoAppearance.shadow) {
            SkijaRenderer.draw(c -> SoftShadow.drawRounded(c, x, y, LITE_W * scale, LITE_H * scale,
                    0f, Config.musicInfoAppearance.shadowStrength, 0xB0000000));
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        drawLiteCard(graphics, client, player, song);
        graphics.pose().popMatrix();
    }

    private void drawLiteCard(GuiGraphics graphics, Minecraft client, MusicPlaybackService player, Song song) {
        drawLiteCard(new VanillaLitePainter(graphics, client), song, player.positionMs(), player.totalDurationMs(), player.playbackMode().label());
    }

    private void drawLiteCard(LitePainter painter, Song song, long positionMs, long totalMs, String playbackMode) {
        int bg = Config.musicInfoAppearance.background();
        int primary = Config.musicInfoAppearance.text();
        int secondary = Config.musicInfoAppearance.secondaryText();
        int muted = 0x88000000 | Config.musicInfoAppearance.secondaryText() & 0xFFFFFF;
        painter.fill(0, 0, Math.round(LITE_W), Math.round(LITE_H), bg);
        painter.cover(song, 8, 8, COVER_SIZE);
        painter.text(trimLite(painter, song.name(), 118), 56, 9, primary);
        painter.text(trimLite(painter, song.displayArtist(), 118), 56, 22, secondary);
        long total = Math.max(0L, totalMs);
        long position = Math.max(0L, Math.min(positionMs, total));
        float progress = total <= 0L ? 0f : Mth.clamp(position / (float) total, 0f, 1f);
        painter.fill(56, 39, 178, 42, muted);
        painter.fill(56, 39, 56 + Math.round(122 * progress), 42, Config.musicInfoAppearance.accent());
        String time = MusicPlaybackService.formatTime(position) + " / " + MusicPlaybackService.formatTime(total);
        painter.text(time, 56, 46, muted);
        painter.text(playbackMode, Math.round(LITE_W) - 8 - Math.round(painter.width(playbackMode)), 46, muted);
    }

    private void drawCover(GuiGraphics graphics, Song song, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, 0xFF273244);
        net.minecraft.resources.Identifier texture = NeteaseMusicCovers.texture(song.image());
        if (texture != null) {
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, size, size,
            NeteaseMusicCovers.TEXTURE_SIZE, NeteaseMusicCovers.TEXTURE_SIZE,
            NeteaseMusicCovers.TEXTURE_SIZE, NeteaseMusicCovers.TEXTURE_SIZE);
            return;
        }
        graphics.fill(x + size / 5, y + size / 5, x + size - size / 5, y + size - size / 5, 0x55FFFFFF);
        graphics.drawString(Minecraft.getInstance().font, "♪", x + size / 2 - 4, y + size / 2 - 5, 0xFFFFFFFF, false);
    }

    public float getEditWidth() {
        return getBaseWidth() * getScale();
    }

    public float getEditHeight() {
        return getBaseHeight() * getScale();
    }

    public float getRenderX(int screenW) {
        return clamp(getDefaultX(screenW) + Config.musicInfoHudX, 0f, Math.max(0f, screenW - getEditWidth()));
    }

    public float getRenderY(int screenH) {
        return clamp(getDefaultY(screenH) + Config.musicInfoHudY, 0f, Math.max(0f, screenH - getEditHeight()));
    }

    public float getDefaultX(int screenW) {
        return 12f;
    }

    public float getDefaultY(int screenH) {
        return Math.max(52f, screenH - getEditHeight() - 74f);
    }

    public float getBaseWidth() {
        return Config.musicInfoHudMode == Config.MusicInfoHudMode.LITE ? LITE_W : CARD_W;
    }

    public float getBaseHeight() {
        return Config.musicInfoHudMode == Config.MusicInfoHudMode.LITE ? LITE_H : CARD_H;
    }

    private float getScale() {
        return Math.max(0.5f, Config.musicInfoHudScale);
    }

    private int cardColor() {
        return 0xEE000000 | Config.skiaBlurTintColor() & 0xFFFFFF;
    }

    private int coverBackplateColor(boolean blurMode) {
        return 0x22000000 | Config.musicInfoAppearance.text() & 0xFFFFFF;
    }

    private int primaryTextColor(boolean blurMode) {
        return Config.musicInfoAppearance.text();
    }

    private int mutedTextColor(boolean blurMode) {
        return Config.musicInfoAppearance.secondaryText();
    }

    private int trackColor(boolean blurMode) {
        return 0x22000000 | Config.musicInfoAppearance.text() & 0xFFFFFF;
    }

    private String trimLite(LitePainter painter, String text, int maxWidth) {
        if (text == null || text.isBlank()) return "";
        if (painter.width(text) <= maxWidth) return text;
        String ellipsis = "...";
        while (text.length() > 1 && painter.width(text + ellipsis) > maxWidth) {
            text = text.substring(0, text.offsetByCodePoints(text.length(), -1));
        }
        return text + ellipsis;
    }

    private String trimSkia(String text, float maxWidth, float size) {
        if (text == null || text.isBlank()) return "";
        if (SkijaUi.textWidth(text, size) <= maxWidth) return text;
        String ellipsis = "...";
        while (text.length() > 1 && SkijaUi.textWidth(text + ellipsis, size) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + ellipsis;
    }

    private interface LitePainter {
        void fill(int left, int top, int right, int bottom, int color);
        void text(String value, int x, int y, int color);
        float width(String value);
        void cover(Song song, int x, int y, int size);
    }

    private final class VanillaLitePainter implements LitePainter {
        private final GuiGraphics graphics;
        private final Minecraft client;

        private VanillaLitePainter(GuiGraphics graphics, Minecraft client) {
            this.graphics = graphics;
            this.client = client;
        }

        public void fill(int left, int top, int right, int bottom, int color) {
            graphics.fill(left, top, right, bottom, color);
        }

        public void text(String value, int x, int y, int color) {
            graphics.drawString(client.font, value, x, y, color, false);
        }

        public float width(String value) {
            return client.font.width(value);
        }

        public void cover(Song song, int x, int y, int size) {
            drawCover(graphics, song, x, y, size);
        }
    }

    private final class SkiaLitePainter implements LitePainter {
        private final Canvas canvas;

        private SkiaLitePainter(Canvas canvas) {
            this.canvas = canvas;
        }

        public void fill(int left, int top, int right, int bottom, int color) {
            SkijaUi.fill(canvas, left, top, right - left, bottom - top, color);
        }

        public void text(String value, int x, int y, int color) {
            SkijaUi.textAtTop(canvas, value, x, y, color, 9f, false);
        }

        public float width(String value) {
            return SkijaUi.textWidth(value, 9f);
        }

        public void cover(Song song, int x, int y, int size) {
            fill(x, y, x + size, y + size, 0xFF273244);
            Image cover = NeteaseMusicCovers.skiaImage(song.image());
            if (cover != null) {
                canvas.drawImageRect(cover, Rect.makeWH(cover.getWidth(), cover.getHeight()),
                        Rect.makeXYWH(x, y, size, size), SamplingMode.LINEAR, coverImagePaint, true);
            } else {
                fill(x + size / 5, y + size / 5, x + size - size / 5, y + size - size / 5, 0x55FFFFFF);
                text("♪", x + size / 2 - 4, y + size / 2 - 5, 0xFFFFFFFF);
            }
        }
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
