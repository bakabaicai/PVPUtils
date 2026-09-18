package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import com.pvp_utils.client.NeteaseMusic.MusicPlaybackService;
import com.pvp_utils.client.NeteaseMusic.NeteaseMusicCovers;
import com.pvp_utils.client.NeteaseMusic.Song;
import com.pvp_utils.client.gui.clickgui.theme.ClickGuiThemeColors;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaBlurRenderer;
import com.pvp_utils.client.render.skia.SkiaGlBackend;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.skija.impl.Library;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.Mth;

import java.util.List;

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

    private final SkiaGlBackend glBackend = new SkiaGlBackend();
    private final Paint bgPaint = new Paint().setAntiAlias(true);
    private final Paint coverBackPaint = new Paint().setAntiAlias(true);
    private final Paint coverImagePaint = new Paint().setAntiAlias(true);
    private final Paint trackPaint = new Paint().setAntiAlias(true);
    private final Paint fillPaint = new Paint().setAntiAlias(true);
    private boolean nativeLoaded;
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

        if (Config.musicInfoHudMode == Config.MusicInfoHudMode.NEW || Config.musicInfoHudMode == Config.MusicInfoHudMode.BLUR) {
            pendingFrame = true;
        } else {
            pendingFrame = false;
            renderLite(graphics, client, player, song);
        }
    }

    public void renderFrameEnd() {
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
        renderCardFrameEnd(client, player, song, Config.musicInfoHudMode == Config.MusicInfoHudMode.BLUR);
    }

    private void renderCardFrameEnd(Minecraft client, MusicPlaybackService player, Song song, boolean blurMode) {
        ensureNativeLoaded();
        int framebufferId = mainFramebufferId(client);
        if (framebufferId == 0) {
            return;
        }
        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        float userScale = getScale();
        float x = getRenderX(screenW);
        float y = getRenderY(screenH);
        float scaledW = CARD_W * userScale;
        float scaledH = CARD_H * userScale;

        boolean blurred = false;
        if (blurMode) {
            blurred = SkiaBlurRenderer.getInstance().render(client, x, y, scaledW, scaledH, RADIUS * userScale, Config.skiaBlurTintColor(), Config.skiaBlurStrength);
        }

        Canvas canvas = glBackend.begin(framebufferId);
        if (canvas == null) {
            return;
        }
        try {
            canvas.save();
            canvas.translate(x, y);
            canvas.scale(userScale, userScale);

            if (!blurMode) {
                bgPaint.setColor(cardColor());
                canvas.drawRRect(RRect.makeXYWH(0f, 0f, CARD_W, CARD_H, RADIUS), bgPaint);
            }
            coverBackPaint.setColor(coverBackplateColor(blurMode));
            canvas.drawRRect(RRect.makeXYWH(10f, 10f, NEW_COVER_SIZE, NEW_COVER_SIZE, 12f), coverBackPaint);

            Image cover = NeteaseMusicCovers.skiaImage(song.image());
            if (cover != null) {
                canvas.save();
                if (Config.musicInfoHudCoverRounded) {
                    canvas.clipRRect(RRect.makeXYWH(10f, 10f, NEW_COVER_SIZE, NEW_COVER_SIZE, 12f), true);
                } else {
                    canvas.clipRect(io.github.humbleui.types.Rect.makeXYWH(10f, 10f, NEW_COVER_SIZE, NEW_COVER_SIZE));
                }
                canvas.drawImageRect(cover,
                        Rect.makeXYWH(0f, 0f, cover.getWidth(), cover.getHeight()),
                        Rect.makeXYWH(10f, 10f, NEW_COVER_SIZE, NEW_COVER_SIZE),
                        SamplingMode.LINEAR, coverImagePaint, true);
                canvas.restore();
            } else {
                FontRenderer.drawText(canvas, "♪", 10f + NEW_COVER_SIZE / 2f - 4f, 10f + NEW_COVER_SIZE / 2f + 4f, 12f, 0x66FFFFFF);
            }

            FontRenderer.drawText(canvas, trimSkia(song.name(), 128f, 13f), 70f, 24f, 13f, primaryTextColor(blurMode));
            FontRenderer.drawText(canvas, trimSkia(song.displayArtist(), 128f, 11f), 70f, 40f, 11f, mutedTextColor(blurMode));

            long total = Math.max(0L, player.totalDurationMs());
            long position = Math.max(0L, Math.min(player.positionMs(), Math.max(total, 0L)));
            float progress = total <= 0L ? 0f : Mth.clamp(position / (float) total, 0f, 1f);
            float barX = 70f;
            float barY = 51f;
            float barW = 112f;
            float barH = 5f;
            trackPaint.setColor(trackColor(blurMode));
            canvas.drawRRect(RRect.makeXYWH(barX, barY, barW, barH, barH * 0.5f), trackPaint);
            fillPaint.setColor(ACCENT);
            canvas.drawRRect(RRect.makeXYWH(barX, barY, Math.max(barH, barW * progress), barH, barH * 0.5f), fillPaint);
            FontRenderer.drawText(canvas, MusicPlaybackService.formatTime(position) + " / " + MusicPlaybackService.formatTime(total), 70f, 64f, 9f, mutedTextColor(blurMode));
            String mode = player.playbackMode().label();
            FontRenderer.drawText(canvas, mode, CARD_W - 14f - FontRenderer.measureTextWidth(mode, 9f), 64f, 9f, mutedTextColor(blurMode));

            canvas.restore();
        } finally {
            glBackend.end();
        }
    }

    private void renderLite(GuiGraphics graphics, Minecraft client, MusicPlaybackService player, Song song) {
        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        float x = getRenderX(screenW);
        float y = getRenderY(screenH);
        float scale = getScale();

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        drawLiteCard(graphics, client, player, song);
        graphics.pose().popMatrix();
    }

    private void drawLiteCard(GuiGraphics graphics, Minecraft client, MusicPlaybackService player, Song song) {
        int bg = Config.hudTheme == Config.HudTheme.LIGHT ? 0xDDF8FAFC : 0xCC0C1018;
        int primary = Config.hudTheme == Config.HudTheme.LIGHT ? 0xFF111827 : 0xFFFFFFFF;
        int secondary = Config.hudTheme == Config.HudTheme.LIGHT ? 0xAA111827 : 0xCCFFFFFF;
        int muted = Config.hudTheme == Config.HudTheme.LIGHT ? 0x885C5870 : 0x88FFFFFF;

        graphics.fill(0, 0, Math.round(LITE_W), Math.round(LITE_H), bg);
        drawCover(graphics, song, 8, 8, COVER_SIZE);

        graphics.drawString(client.font, trimVanilla(client, song.name(), 118), 56, 9, primary, false);
        graphics.drawString(client.font, trimVanilla(client, song.displayArtist(), 118), 56, 22, secondary, false);
        drawVanillaProgress(graphics, client, player, 56, 39, 122, muted);
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

    private void drawVanillaProgress(GuiGraphics graphics, Minecraft client, MusicPlaybackService player, int x, int y, int w, int muted) {
        long total = Math.max(0L, player.totalDurationMs());
        long position = Math.max(0L, Math.min(player.positionMs(), Math.max(total, 0L)));
        float progress = total <= 0L ? 0f : Mth.clamp(position / (float) total, 0f, 1f);
        graphics.fill(x, y, x + w, y + 3, muted);
        graphics.fill(x, y, x + Math.round(w * progress), y + 3, ACCENT);
        String time = MusicPlaybackService.formatTime(position) + " / " + MusicPlaybackService.formatTime(total);
        graphics.drawString(client.font, time, x, y + 7, muted, false);
        String mode = player.playbackMode().label();
        graphics.drawString(client.font, mode, Math.round(LITE_W) - 8 - client.font.width(mode), y + 7, muted, false);
    }

    public float getEditWidth() {
        return baseWidth() * getScale();
    }

    public float getEditHeight() {
        return baseHeight() * getScale();
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

    private float baseWidth() {
        return Config.musicInfoHudMode == Config.MusicInfoHudMode.LITE ? LITE_W : CARD_W;
    }

    private float baseHeight() {
        return Config.musicInfoHudMode == Config.MusicInfoHudMode.LITE ? LITE_H : CARD_H;
    }

    private float getScale() {
        return Math.max(0.5f, Config.musicInfoHudScale);
    }

    private void ensureNativeLoaded() {
        if (nativeLoaded) return;
        io.github.humbleui.skija.impl.Library.load();
        nativeLoaded = true;
    }

    private int mainFramebufferId(Minecraft client) {
        if (client.getMainRenderTarget().getColorTexture() instanceof com.mojang.blaze3d.opengl.GlTexture texture
                && com.mojang.blaze3d.systems.RenderSystem.getDevice() instanceof com.mojang.blaze3d.opengl.GlDevice device) {
            return texture.getFbo(device.directStateAccess(), client.getMainRenderTarget().getDepthTexture());
        }
        return 0;
    }

    private int cardColor() {
        return Config.hudTheme == Config.HudTheme.LIGHT ? 0xF7F8FAFC : 0xE6111827;
    }

    private int coverBackplateColor(boolean blurMode) {
        if (blurMode) {
            return Config.hudTheme == Config.HudTheme.LIGHT ? 0x44FFFFFF : 0x332A3345;
        }
        return Config.hudTheme == Config.HudTheme.LIGHT ? 0x66FFFFFF : 0x332A3345;
    }

    private int primaryTextColor(boolean blurMode) {
        return blurMode ? Config.hudPrimaryTextColor() : (Config.hudTheme == Config.HudTheme.LIGHT ? 0xFF202027 : 0xFFF8FAFC);
    }

    private int mutedTextColor(boolean blurMode) {
        return blurMode ? Config.hudMutedTextColor() : (Config.hudTheme == Config.HudTheme.LIGHT ? 0xAA5C5870 : 0xB8CBD5E1);
    }

    private int trackColor(boolean blurMode) {
        if (blurMode) {
            return Config.hudTheme == Config.HudTheme.LIGHT ? 0x33111827 : 0x2D000000;
        }
        return Config.hudTheme == Config.HudTheme.LIGHT ? 0x22111827 : 0x33FFFFFF;
    }

    private String trimVanilla(Minecraft client, String text, int maxWidth) {
        if (text == null || text.isBlank()) return "";
        if (client.font.width(text) <= maxWidth) return text;
        String ellipsis = "...";
        while (text.length() > 1 && client.font.width(text + ellipsis) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + ellipsis;
    }

    private String trimSkia(String text, float maxWidth, float size) {
        if (text == null || text.isBlank()) return "";
        if (FontRenderer.measureTextWidth(text, size) <= maxWidth) return text;
        String ellipsis = "...";
        while (text.length() > 1 && FontRenderer.measureTextWidth(text + ellipsis, size) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + ellipsis;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
