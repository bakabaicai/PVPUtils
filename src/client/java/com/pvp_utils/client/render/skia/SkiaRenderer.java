package com.pvp_utils.client.render.skia;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import io.github.humbleui.skija.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class SkiaRenderer {

    private static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath("pvp_utils", "skia_frame");
    private static final SurfaceProps PROPS = new SurfaceProps(false, PixelGeometry.RGB_H);

    private static Surface surface;
    private static DynamicTexture dynamicTexture;
    private static int lastPixelW = -1;
    private static int lastPixelH = -1;
    private static float currentScale = 1f;

    public static Canvas begin() {
        var window = Minecraft.getInstance().getWindow();
        int pw = window.getWidth();
        int ph = window.getHeight();
        currentScale = (float) window.getGuiScale();

        if (pw != lastPixelW || ph != lastPixelH || surface == null) {
            destroySurface();
            surface = Surface.makeRaster(
                    new ImageInfo(new ColorInfo(ColorType.RGBA_8888, ColorAlphaType.UNPREMUL, null), pw, ph), 0, PROPS);
            dynamicTexture = new DynamicTexture("pvp_utils:skia_frame", pw, ph, false);
            Minecraft.getInstance().getTextureManager().register(TEXTURE_ID, dynamicTexture);
            lastPixelW = pw;
            lastPixelH = ph;
        }

        Canvas canvas = surface.getCanvas();
        canvas.clear(0x00000000);
        canvas.save();
        canvas.scale(currentScale, currentScale);
        return canvas;
    }

    public static float getScale() { return currentScale; }

    public static void end(GuiGraphics graphics, int guiWidth, int guiHeight) {
        if (surface == null || dynamicTexture == null) return;

        surface.getCanvas().restore();
        surface.flush();

        Pixmap pixmap = new Pixmap();
        if (!surface.peekPixels(pixmap)) { pixmap.close(); return; }

        long addr = pixmap.getAddr();
        int byteSize = lastPixelH * pixmap.getRowBytes();
        ByteBuffer buf = MemoryUtil.memByteBuffer(addr, byteSize);

        GpuTexture gpuTexture = dynamicTexture.getTexture();
        RenderSystem.getDevice().createCommandEncoder()
                .writeToTexture(gpuTexture, buf, NativeImage.Format.RGBA, 0, 0, 0, 0, lastPixelW, lastPixelH);

        pixmap.close();
        graphics.blit(TEXTURE_ID, 0, 0, guiWidth, guiHeight, 0f, 1f, 0f, 1f);
    }

    private static void destroySurface() {
        if (surface != null) { surface.close(); surface = null; }
        if (dynamicTexture != null) {
            Minecraft.getInstance().getTextureManager().release(TEXTURE_ID);
            dynamicTexture = null;
        }
    }

    public static void destroy() {
        destroySurface();
        lastPixelW = -1;
        lastPixelH = -1;
    }
}