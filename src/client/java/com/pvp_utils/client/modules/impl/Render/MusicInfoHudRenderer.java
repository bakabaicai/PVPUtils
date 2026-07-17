package com.pvp_utils.client.modules.impl.Render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.pvp_utils.Config;
import com.pvp_utils.client.NeteaseMusic.MusicPlaybackService;
import com.pvp_utils.client.NeteaseMusic.NeteaseMusicCovers;
import com.pvp_utils.client.NeteaseMusic.Song;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaBlurRenderer;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorAlphaType;
import io.github.humbleui.skija.ColorInfo;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PixelGeometry;
import io.github.humbleui.skija.Pixmap;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.SurfaceProps;
import io.github.humbleui.skija.impl.Library;
import io.github.humbleui.types.RRect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class MusicInfoHudRenderer {
    private static final MusicInfoHudRenderer INSTANCE = new MusicInfoHudRenderer();
    private static final Identifier BASE_TEXTURE_ID = Identifier.fromNamespaceAndPath("pvp_utils", "music_info_hud_base");
    private static final Identifier OVERLAY_TEXTURE_ID = Identifier.fromNamespaceAndPath("pvp_utils", "music_info_hud_overlay");
    private static final SurfaceProps SURFACE_PROPS = new SurfaceProps(false, PixelGeometry.RGB_H);
    private static final float LITE_W = 190f;
    private static final float LITE_H = 58f;
    private static final float CARD_W = 216f;
    private static final float CARD_H = 68f;
    private static final float RADIUS = 16f;
    private static final int COVER_SIZE = 42;
    private static final int NEW_COVER_SIZE = 48;
    private static final int ACCENT = 0xFFE5484D;

    private final Paint bgPaint = new Paint().setAntiAlias(true);
    private final Paint coverBackPaint = new Paint().setAntiAlias(true);
    private final Paint trackPaint = new Paint().setAntiAlias(true);
    private final Paint fillPaint = new Paint().setAntiAlias(true);

    private Surface baseSurface;
    private Surface overlaySurface;
    private DynamicTexture baseTexture;
    private DynamicTexture overlayTexture;
    private boolean nativeLoaded;
    private int baseTextureW = -1;
    private int baseTextureH = -1;
    private int overlayTextureW = -1;
    private int overlayTextureH = -1;
    private float textureScale = -1f;
    private String lastBaseKey = "";
    private String lastOverlayKey = "";
    private Config.HudTheme lastBaseTheme = null;
    private Config.HudTheme lastOverlayTheme = null;
    private boolean lastBaseBlurMode;
    private boolean lastOverlayBlurMode;

    public static MusicInfoHudRenderer getInstance() {
        return INSTANCE;
    }

    public void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        boolean editActive = HudEditOverlay.getInstance().isActive();
        if (!Config.musicInfoHud || editActive) {
            return;
        }
        if (client.player == null || client.options.hideGui) {
            return;
        }
        if (client.screen != null && !(client.screen instanceof ChatScreen)) {
            return;
        }

        MusicPlaybackService player = MusicPlaybackService.INSTANCE;
        Song song = player.currentSong();
        if (song == null) {
            destroyTextures(client);
            return;
        }

        if (Config.musicInfoHudMode == Config.MusicInfoHudMode.NEW || Config.musicInfoHudMode == Config.MusicInfoHudMode.BLUR) {
            renderCard(graphics, client, player, song, Config.musicInfoHudMode == Config.MusicInfoHudMode.BLUR);
        } else {
            destroyTextures(client);
            renderLite(graphics, client, player, song);
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

    private void renderCard(GuiGraphics graphics, Minecraft client, MusicPlaybackService player, Song song, boolean blurMode) {
        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        float userScale = getScale();
        float x = getRenderX(screenW);
        float y = getRenderY(screenH);
        float scaledW = CARD_W * userScale;
        float scaledH = CARD_H * userScale;

        renderBaseTexture(client, song, blurMode);
        renderOverlayTexture(client, player, blurMode);
        if (blurMode) {
            SkiaBlurRenderer.getInstance().render(client, x, y, scaledW, scaledH, RADIUS * userScale, Config.skiaBlurTintColor(), Config.skiaBlurStrength);
        }

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(userScale, userScale);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BASE_TEXTURE_ID, 0, 0, 0f, 0f, Math.round(CARD_W), Math.round(CARD_H),
                baseTextureW, baseTextureH, baseTextureW, baseTextureH);
        drawCover(graphics, song, 10, 10, NEW_COVER_SIZE);
        graphics.blit(RenderPipelines.GUI_TEXTURED, OVERLAY_TEXTURE_ID, 0, 0, 0f, 0f, Math.round(CARD_W), Math.round(CARD_H),
                overlayTextureW, overlayTextureH, overlayTextureW, overlayTextureH);
        graphics.pose().popMatrix();
    }

    private void renderBaseTexture(Minecraft client, Song song, boolean blurMode) {
        ensureNativeLoaded();
        float targetScale = targetScale(client);
        int targetW = Math.max(1, Math.round(CARD_W * targetScale));
        int targetH = Math.max(1, Math.round(CARD_H * targetScale));
        String key = trimSkia(song.name(), 128f, 13f) + "|" + trimSkia(song.displayArtist(), 128f, 11f);
        if (baseTexture != null && baseTextureW == targetW && baseTextureH == targetH && key.equals(lastBaseKey)
                && lastBaseTheme == Config.hudTheme && lastBaseBlurMode == blurMode) {
            return;
        }
        if (baseSurface == null || baseTexture == null || baseTextureW != targetW || baseTextureH != targetH) {
            destroyBaseTexture(client);
            baseSurface = Surface.makeRaster(new ImageInfo(new ColorInfo(ColorType.RGBA_8888, ColorAlphaType.UNPREMUL, null), targetW, targetH), 0, SURFACE_PROPS);
            baseTexture = new DynamicTexture("pvp_utils:music_info_hud_base", targetW, targetH, false);
            client.getTextureManager().register(BASE_TEXTURE_ID, baseTexture);
            baseTextureW = targetW;
            baseTextureH = targetH;
            textureScale = targetScale;
        }

        Canvas canvas = baseSurface.getCanvas();
        canvas.restoreToCount(1);
        canvas.resetMatrix();
        canvas.clear(0x00000000);
        canvas.save();
        canvas.scale(textureScale, textureScale);
        if (!blurMode) {
            bgPaint.setColor(cardColor());
            canvas.drawRRect(RRect.makeXYWH(0f, 0f, CARD_W, CARD_H, RADIUS), bgPaint);
        }
        coverBackPaint.setColor(coverBackplateColor(blurMode));
        canvas.drawRRect(RRect.makeXYWH(10f, 10f, NEW_COVER_SIZE, NEW_COVER_SIZE, 12f), coverBackPaint);
        FontRenderer.drawText(canvas, trimSkia(song.name(), 128f, 13f), 70f, 24f, 13f, primaryTextColor(blurMode));
        FontRenderer.drawText(canvas, trimSkia(song.displayArtist(), 128f, 11f), 70f, 40f, 11f, mutedTextColor(blurMode));
        canvas.restore();
        uploadSurface(baseSurface, baseTexture, baseTextureW, baseTextureH);

        lastBaseKey = key;
        lastBaseTheme = Config.hudTheme;
        lastBaseBlurMode = blurMode;
    }

    private void renderOverlayTexture(Minecraft client, MusicPlaybackService player, boolean blurMode) {
        ensureNativeLoaded();
        float targetScale = targetScale(client);
        int targetW = Math.max(1, Math.round(CARD_W * targetScale));
        int targetH = Math.max(1, Math.round(CARD_H * targetScale));
        long total = Math.max(0L, player.totalDurationMs());
        long position = Math.max(0L, Math.min(player.positionMs(), Math.max(total, 0L)));
        float progress = total <= 0L ? 0f : Mth.clamp(position / (float) total, 0f, 1f);
        String time = MusicPlaybackService.formatTime(position) + " / " + MusicPlaybackService.formatTime(total);
        String mode = player.playbackMode().label();
        String key = Math.round(progress * 120f) + "|" + time + "|" + mode;
        if (overlayTexture != null && overlayTextureW == targetW && overlayTextureH == targetH && key.equals(lastOverlayKey)
                && lastOverlayTheme == Config.hudTheme && lastOverlayBlurMode == blurMode) {
            return;
        }
        if (overlaySurface == null || overlayTexture == null || overlayTextureW != targetW || overlayTextureH != targetH) {
            destroyOverlayTexture(client);
            overlaySurface = Surface.makeRaster(new ImageInfo(new ColorInfo(ColorType.RGBA_8888, ColorAlphaType.UNPREMUL, null), targetW, targetH), 0, SURFACE_PROPS);
            overlayTexture = new DynamicTexture("pvp_utils:music_info_hud_overlay", targetW, targetH, false);
            client.getTextureManager().register(OVERLAY_TEXTURE_ID, overlayTexture);
            overlayTextureW = targetW;
            overlayTextureH = targetH;
        }

        Canvas canvas = overlaySurface.getCanvas();
        canvas.restoreToCount(1);
        canvas.resetMatrix();
        canvas.clear(0x00000000);
        canvas.save();
        canvas.scale(targetScale, targetScale);
        float barX = 70f;
        float barY = 51f;
        float barW = 112f;
        float barH = 5f;
        trackPaint.setColor(trackColor(blurMode));
        canvas.drawRRect(RRect.makeXYWH(barX, barY, barW, barH, barH * 0.5f), trackPaint);
        fillPaint.setColor(ACCENT);
        canvas.drawRRect(RRect.makeXYWH(barX, barY, Math.max(barH, barW * progress), barH, barH * 0.5f), fillPaint);
        FontRenderer.drawText(canvas, time, 70f, 64f, 9f, mutedTextColor(blurMode));
        FontRenderer.drawText(canvas, mode, CARD_W - 14f - FontRenderer.measureTextWidth(mode, 9f), 64f, 9f, mutedTextColor(blurMode));
        canvas.restore();
        uploadSurface(overlaySurface, overlayTexture, overlayTextureW, overlayTextureH);

        lastOverlayKey = key;
        lastOverlayTheme = Config.hudTheme;
        lastOverlayBlurMode = blurMode;
    }

    private void drawCover(GuiGraphics graphics, Song song, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, 0xFF273244);
        Identifier texture = NeteaseMusicCovers.texture(song.image());
        if (texture != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, size, size,
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

    private float targetScale(Minecraft client) {
        return Math.max(1f, (float) client.getWindow().getGuiScale() * getScale());
    }

    private void ensureNativeLoaded() {
        if (nativeLoaded) return;
        Library.load();
        nativeLoaded = true;
    }

    private void uploadSurface(Surface sourceSurface, DynamicTexture targetTexture, int width, int height) {
        Pixmap pixmap = new Pixmap();
        try {
            if (!sourceSurface.peekPixels(pixmap)) return;
            long addr = pixmap.getAddr();
            int byteSize = height * pixmap.getRowBytes();
            ByteBuffer buf = MemoryUtil.memByteBuffer(addr, byteSize);
            GpuTexture gpuTexture = targetTexture.getTexture();
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToTexture(gpuTexture, buf, NativeImage.Format.RGBA, 0, 0, 0, 0, width, height);
        } finally {
            pixmap.close();
        }
    }

    private void destroyBaseTexture(Minecraft client) {
        if (baseSurface != null) {
            baseSurface.close();
            baseSurface = null;
        }
        if (baseTexture != null) {
            client.getTextureManager().release(BASE_TEXTURE_ID);
            baseTexture = null;
        }
        baseTextureW = -1;
        baseTextureH = -1;
        textureScale = -1f;
        lastBaseKey = "";
        lastBaseTheme = null;
        lastBaseBlurMode = false;
    }

    private void destroyOverlayTexture(Minecraft client) {
        if (overlaySurface != null) {
            overlaySurface.close();
            overlaySurface = null;
        }
        if (overlayTexture != null) {
            client.getTextureManager().release(OVERLAY_TEXTURE_ID);
            overlayTexture = null;
        }
        overlayTextureW = -1;
        overlayTextureH = -1;
        lastOverlayKey = "";
        lastOverlayTheme = null;
        lastOverlayBlurMode = false;
    }

    private void destroyTextures(Minecraft client) {
        destroyBaseTexture(client);
        destroyOverlayTexture(client);
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
