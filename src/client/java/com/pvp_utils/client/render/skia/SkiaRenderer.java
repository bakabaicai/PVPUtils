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
    private static int lastGuiWidth = -1;
    private static int lastGuiHeight = -1;
    private static float currentScale = 1f;

    public static Canvas begin() {
        var window = Minecraft.getInstance().getWindow();
        int gw = window.getGuiScaledWidth();
        int gh = window.getGuiScaledHeight();
        currentScale = (float) window.getGuiScale();

        if (gw != lastGuiWidth || gh != lastGuiHeight || surface == null) {
            destroySurface();
            surface = Surface.makeRaster(
                    new ImageInfo(new ColorInfo(ColorType.RGBA_8888, ColorAlphaType.UNPREMUL, null), gw, gh), 0, PROPS);
            dynamicTexture = new DynamicTexture("pvp_utils:skia_frame", gw, gh, false);
            Minecraft.getInstance().getTextureManager().register(TEXTURE_ID, dynamicTexture);
            lastGuiWidth = gw;
            lastGuiHeight = gh;
        }

        Canvas canvas = surface.getCanvas();
        canvas.clear(0x00000000);
        return canvas;
    }

    public static float getScale() {
        return currentScale;
    }

    public static void end(GuiGraphics graphics, int guiWidth, int guiHeight) {
        if (surface == null || dynamicTexture == null) return;

        surface.flush();

        Pixmap pixmap = new Pixmap();
        if (!surface.peekPixels(pixmap)) {
            pixmap.close();
            return;
        }

        long addr = pixmap.getAddr();
        int byteSize = lastGuiHeight * pixmap.getRowBytes();
        ByteBuffer buf = MemoryUtil.memByteBuffer(addr, byteSize);

        GpuTexture gpuTexture = dynamicTexture.getTexture();
        RenderSystem.getDevice().createCommandEncoder()
                .writeToTexture(gpuTexture, buf, NativeImage.Format.RGBA, 0, 0, 0, 0, lastGuiWidth, lastGuiHeight);

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
        lastGuiWidth = -1;
        lastGuiHeight = -1;
    }
}