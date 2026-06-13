package com.pvp_utils.client.render.skia;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import io.github.humbleui.skija.*;
import io.github.humbleui.skija.impl.Library;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class SkiaRenderer {

    private static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath("pvp_utils", "skia_frame");
    private static final Identifier REGION_TEXTURE_ID = Identifier.fromNamespaceAndPath("pvp_utils", "skia_region");

    private static Surface surface;
    private static Surface regionSurface;
    private static DynamicTexture dynamicTexture;
    private static DynamicTexture regionTexture;
    private static int lastPixelW = -1;
    private static int lastPixelH = -1;
    private static int lastRegionPixelW = -1;
    private static int lastRegionPixelH = -1;
    private static float currentScale = 1f;
    private static int regionX = 0;
    private static int regionY = 0;
    private static int regionW = 0;
    private static int regionH = 0;
    private static boolean nativeLoaded = false;
    private static boolean drawing = false;
    private static boolean regionDrawing = false;

    private static void ensureNativeLoaded() {
        if (nativeLoaded) return;
        try {
            Library.load();
            nativeLoaded = true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static Canvas begin() {
        if (drawing) return surface != null ? surface.getCanvas() : null;
        ensureNativeLoaded();
        SurfaceProps props = new SurfaceProps(false, PixelGeometry.RGB_H);

        var window = Minecraft.getInstance().getWindow();
        int pw = window.getWidth();
        int ph = window.getHeight();
        currentScale = (float) window.getGuiScale();

        if (pw != lastPixelW || ph != lastPixelH || surface == null) {
            destroySurface();
            surface = Surface.makeRaster(
                    new ImageInfo(new ColorInfo(ColorType.RGBA_8888, ColorAlphaType.UNPREMUL, null), pw, ph), 0, props);
            dynamicTexture = new DynamicTexture("pvp_utils:skia_frame", pw, ph, false);
            Minecraft.getInstance().getTextureManager().register(TEXTURE_ID, dynamicTexture);
            lastPixelW = pw;
            lastPixelH = ph;
        }

        Canvas canvas = surface.getCanvas();
        canvas.restoreToCount(1);
        canvas.resetMatrix();
        canvas.clear(0x00000000);
        canvas.save();
        canvas.scale(currentScale, currentScale);
        drawing = true;
        return canvas;
    }

    public static Canvas beginRegion(int x, int y, int w, int h) {
        if (regionDrawing) return regionSurface != null ? regionSurface.getCanvas() : null;
        ensureNativeLoaded();
        SurfaceProps props = new SurfaceProps(false, PixelGeometry.RGB_H);

        var window = Minecraft.getInstance().getWindow();
        currentScale = (float) window.getGuiScale();
        regionX = x;
        regionY = y;
        regionW = Math.max(1, w);
        regionH = Math.max(1, h);
        int pixelW = Math.max(1, (int) Math.ceil(regionW * currentScale));
        int pixelH = Math.max(1, (int) Math.ceil(regionH * currentScale));

        if (pixelW != lastRegionPixelW || pixelH != lastRegionPixelH || regionSurface == null) {
            destroyRegionSurface();
            regionSurface = Surface.makeRaster(
                    new ImageInfo(new ColorInfo(ColorType.RGBA_8888, ColorAlphaType.UNPREMUL, null), pixelW, pixelH), 0, props);
            regionTexture = new DynamicTexture("pvp_utils:skia_region", pixelW, pixelH, false);
            Minecraft.getInstance().getTextureManager().register(REGION_TEXTURE_ID, regionTexture);
            lastRegionPixelW = pixelW;
            lastRegionPixelH = pixelH;
        }

        Canvas canvas = regionSurface.getCanvas();
        canvas.restoreToCount(1);
        canvas.resetMatrix();
        canvas.clear(0x00000000);
        canvas.save();
        canvas.scale(currentScale, currentScale);
        canvas.translate(-regionX, -regionY);
        regionDrawing = true;
        return canvas;
    }

    public static float getScale() { return currentScale; }

    public static void end(GuiGraphics graphics, int guiWidth, int guiHeight) {
        if (!drawing || surface == null || dynamicTexture == null) return;

        surface.getCanvas().restore();
        Pixmap pixmap = new Pixmap();
        if (!surface.peekPixels(pixmap)) {
            pixmap.close();
            drawing = false;
            return;
        }

        long addr = pixmap.getAddr();
        int byteSize = lastPixelH * pixmap.getRowBytes();
        ByteBuffer buf = MemoryUtil.memByteBuffer(addr, byteSize);

        GpuTexture gpuTexture = dynamicTexture.getTexture();
        RenderSystem.getDevice().createCommandEncoder()
                .writeToTexture(gpuTexture, buf, NativeImage.Format.RGBA, 0, 0, 0, 0, lastPixelW, lastPixelH);

        pixmap.close();
        graphics.blit(TEXTURE_ID, 0, 0, guiWidth, guiHeight, 0f, 1f, 0f, 1f);
        drawing = false;
    }

    public static void endRegion(GuiGraphics graphics) {
        if (!regionDrawing || regionSurface == null || regionTexture == null) return;

        regionSurface.getCanvas().restore();
        Pixmap pixmap = new Pixmap();
        if (!regionSurface.peekPixels(pixmap)) {
            pixmap.close();
            regionDrawing = false;
            return;
        }

        long addr = pixmap.getAddr();
        int byteSize = lastRegionPixelH * pixmap.getRowBytes();
        ByteBuffer buf = MemoryUtil.memByteBuffer(addr, byteSize);

        GpuTexture gpuTexture = regionTexture.getTexture();
        RenderSystem.getDevice().createCommandEncoder()
                .writeToTexture(gpuTexture, buf, NativeImage.Format.RGBA, 0, 0, 0, 0, lastRegionPixelW, lastRegionPixelH);

        pixmap.close();
        graphics.blit(RenderPipelines.GUI_TEXTURED, REGION_TEXTURE_ID, regionX, regionY, 0f, 0f, regionW, regionH, lastRegionPixelW, lastRegionPixelH, lastRegionPixelW, lastRegionPixelH);
        regionDrawing = false;
    }

    public static boolean isDrawing() {
        return drawing;
    }

    private static void destroySurface() {
        if (surface != null) { surface.close(); surface = null; }
        if (dynamicTexture != null) {
            Minecraft.getInstance().getTextureManager().release(TEXTURE_ID);
            dynamicTexture = null;
        }
    }

    private static void destroyRegionSurface() {
        if (regionSurface != null) { regionSurface.close(); regionSurface = null; }
        if (regionTexture != null) {
            Minecraft.getInstance().getTextureManager().release(REGION_TEXTURE_ID);
            regionTexture = null;
        }
    }

    public static void destroy() {
        destroySurface();
        destroyRegionSurface();
        lastPixelW = -1;
        lastPixelH = -1;
        lastRegionPixelW = -1;
        lastRegionPixelH = -1;
        drawing = false;
        regionDrawing = false;
    }

    public static void resetFrameState() {
        drawing = false;
        regionDrawing = false;
        if (surface != null) {
            Canvas canvas = surface.getCanvas();
            canvas.restoreToCount(1);
            canvas.resetMatrix();
        }
        if (regionSurface != null) {
            Canvas canvas = regionSurface.getCanvas();
            canvas.restoreToCount(1);
            canvas.resetMatrix();
        }
    }
}
