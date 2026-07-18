package com.pvp_utils.client.render.skia;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorAlphaType;
import io.github.humbleui.skija.ColorInfo;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.PixelGeometry;
import io.github.humbleui.skija.Pixmap;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.SurfaceProps;
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
    private static final SurfaceProps SURFACE_PROPS = new SurfaceProps(false, PixelGeometry.RGB_H);
    private static final long FRAME_IDLE_TIMEOUT_MS = 10000L;
    private static final long REGION_IDLE_TIMEOUT_MS = 5000L;
    private static final SkiaGlBackend GL_BACKEND = new SkiaGlBackend();
    private static final boolean USE_GL_BACKEND_FOR_FRAME = false;

    private static Surface surface;
    private static Surface regionSurface;
    private static DynamicTexture dynamicTexture;
    private static DynamicTexture regionTexture;
    private static int lastPixelW = -1;
    private static int lastPixelH = -1;
    private static int regionPixelW = -1;
    private static int regionPixelH = -1;
    private static int regionCapacityPixelW = -1;
    private static int regionCapacityPixelH = -1;
    private static float currentScale = 1f;
    private static int regionX = 0;
    private static int regionY = 0;
    private static int regionW = 0;
    private static int regionH = 0;
    private static boolean nativeLoaded = false;
    private static boolean drawing = false;
    private static boolean regionDrawing = false;
    private static boolean frameDirty = true;
    private static boolean regionDirty = true;
    private static long lastFrameUseMs = 0L;
    private static long lastRegionUseMs = 0L;
    // Content-hash cache for the region layer: if the accumulated draw commands and the
    // region geometry are byte-for-byte identical to the last uploaded frame, we skip the
    // expensive peekPixels + writeToTexture and just re-blit the cached texture.
    private static long regionContentHash = 1L;
    private static long lastUploadedRegionHash = 0L;
    private static boolean regionCacheValid = false;
    // Whether the current region pass opted into content-hash caching (text-only callers).
    private static boolean regionCacheAllowed = false;

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
        if (USE_GL_BACKEND_FOR_FRAME) {
            ensureNativeLoaded();
            drawing = true;
            return GL_BACKEND.begin();
        }
        if (drawing) return surface != null ? surface.getCanvas() : null;
        ensureNativeLoaded();
        pruneIdleSurfaces();

        var window = Minecraft.getInstance().getWindow();
        int pw = window.getWidth();
        int ph = window.getHeight();
        currentScale = (float) window.getGuiScale();

        if (pw != lastPixelW || ph != lastPixelH || surface == null || dynamicTexture == null) {
            destroySurface();
            surface = Surface.makeRaster(
                    new ImageInfo(new ColorInfo(ColorType.RGBA_8888, ColorAlphaType.UNPREMUL, null), pw, ph), 0, SURFACE_PROPS);
            dynamicTexture = new DynamicTexture("pvp_utils:skia_frame", pw, ph, false);
            Minecraft.getInstance().getTextureManager().register(TEXTURE_ID, dynamicTexture);
            lastPixelW = pw;
            lastPixelH = ph;
            frameDirty = true;
        }

        Canvas canvas = surface.getCanvas();
        canvas.restoreToCount(1);
        canvas.resetMatrix();
        canvas.clear(0x00000000);
        canvas.save();
        canvas.scale(currentScale, currentScale);
        drawing = true;
        frameDirty = true;
        lastFrameUseMs = System.currentTimeMillis();
        return canvas;
    }

    public static Canvas beginRegion(int x, int y, int w, int h) {
        return beginRegion(x, y, w, h, false);
    }

    // allowContentCache: only pass true when the region draws *nothing but* text through
    // FontRenderer. Callers that draw shapes directly on the canvas (drawRect/drawRRect/
    // drawLine/drawCircle) must leave it false, otherwise the content hash misses those
    // draws and a static hash would freeze animated shapes.
    public static Canvas beginRegion(int x, int y, int w, int h, boolean allowContentCache) {
        if (regionDrawing) return regionSurface != null ? regionSurface.getCanvas() : null;
        ensureNativeLoaded();
        pruneIdleSurfaces();
        regionCacheAllowed = allowContentCache;

        var window = Minecraft.getInstance().getWindow();
        currentScale = (float) window.getGuiScale();
        regionX = x;
        regionY = y;
        regionW = Math.max(1, w);
        regionH = Math.max(1, h);
        regionPixelW = Math.max(1, (int) Math.ceil(regionW * currentScale));
        regionPixelH = Math.max(1, (int) Math.ceil(regionH * currentScale));

        if (regionSurface == null || regionTexture == null || regionPixelW > regionCapacityPixelW || regionPixelH > regionCapacityPixelH) {
            int newPixelW = Math.max(regionPixelW, regionCapacityPixelW);
            int newPixelH = Math.max(regionPixelH, regionCapacityPixelH);
            destroyRegionSurface();
            regionSurface = Surface.makeRaster(
                    new ImageInfo(new ColorInfo(ColorType.RGBA_8888, ColorAlphaType.UNPREMUL, null), newPixelW, newPixelH), 0, SURFACE_PROPS);
            regionTexture = new DynamicTexture("pvp_utils:skia_region", newPixelW, newPixelH, false);
            Minecraft.getInstance().getTextureManager().register(REGION_TEXTURE_ID, regionTexture);
            regionCapacityPixelW = newPixelW;
            regionCapacityPixelH = newPixelH;
            regionDirty = true;
            regionCacheValid = false;
        }

        Canvas canvas = regionSurface.getCanvas();
        canvas.restoreToCount(1);
        canvas.resetMatrix();
        canvas.clear(0x00000000);
        canvas.save();
        canvas.scale(currentScale, currentScale);
        canvas.translate(-regionX, -regionY);
        regionDrawing = true;
        regionDirty = true;
        // Seed the content hash with the region geometry so any layout/scale change forces
        // a re-upload; each draw command mixes its parameters in via feedRegionContent.
        regionContentHash = 1L;
        mixRegionHash(Float.floatToIntBits(currentScale));
        mixRegionHash(regionX);
        mixRegionHash(regionY);
        mixRegionHash(regionW);
        mixRegionHash(regionH);
        lastRegionUseMs = System.currentTimeMillis();
        return canvas;
    }

    private static void mixRegionHash(long value) {
        regionContentHash = regionContentHash * 31L + value;
    }

    // Called by FontRenderer for every text draw while a region pass is active, so the hash
    // reflects the exact rendered content without the screen having to describe its state.
    public static void feedRegionContent(String text, float x, float y, float size, int argb, String fontName) {
        if (!regionDrawing) {
            return;
        }
        mixRegionHash(text == null ? 0 : text.hashCode());
        mixRegionHash(Float.floatToIntBits(x));
        mixRegionHash(Float.floatToIntBits(y));
        mixRegionHash(Float.floatToIntBits(size));
        mixRegionHash(argb);
        mixRegionHash(fontName == null ? 0 : fontName.hashCode());
    }

    public static boolean isRegionDrawing() {
        return regionDrawing;
    }

    public static float getScale() {
        return currentScale;
    }

    public static void end(GuiGraphics graphics, int guiWidth, int guiHeight) {
        if (USE_GL_BACKEND_FOR_FRAME) {
            try {
                GL_BACKEND.end();
            } finally {
                drawing = false;
            }
            return;
        }
        if (!drawing || surface == null || dynamicTexture == null) return;
        drawing = false;
        try {
            surface.getCanvas().restore();
            if (frameDirty && !uploadSurface(surface, dynamicTexture, lastPixelW, lastPixelH)) {
                return;
            }
            graphics.blit(TEXTURE_ID, 0, 0, guiWidth, guiHeight, 0f, 1f, 0f, 1f);
            frameDirty = false;
            lastFrameUseMs = System.currentTimeMillis();
        } finally {
            drawing = false;
        }
    }

    public static void endRegion(GuiGraphics graphics) {
        if (!regionDrawing || regionSurface == null || regionTexture == null) return;
        regionDrawing = false;
        try {
            regionSurface.getCanvas().restore();
            // Skip the expensive peekPixels copy + full GPU upload when the drawn content and
            // geometry are byte-for-byte identical to the last uploaded frame; the cached
            // region texture is still valid so we only need to blit it again.
            boolean contentUnchanged = regionCacheAllowed && regionCacheValid && regionContentHash == lastUploadedRegionHash;
            if (!contentUnchanged) {
                if (!uploadSurface(regionSurface, regionTexture, regionCapacityPixelW, regionCapacityPixelH)) {
                    return;
                }
                lastUploadedRegionHash = regionContentHash;
                regionCacheValid = true;
            }
            graphics.blit(RenderPipelines.GUI_TEXTURED, REGION_TEXTURE_ID, regionX, regionY, 0f, 0f, regionW, regionH, regionPixelW, regionPixelH, regionCapacityPixelW, regionCapacityPixelH);
            regionDirty = false;
            lastRegionUseMs = System.currentTimeMillis();
        } finally {
            regionDrawing = false;
        }
    }

    public static void drawCached(GuiGraphics graphics, int guiWidth, int guiHeight) {
        if (USE_GL_BACKEND_FOR_FRAME) return;
        pruneIdleSurfaces();
        if (dynamicTexture == null) return;
        graphics.blit(TEXTURE_ID, 0, 0, guiWidth, guiHeight, 0f, 1f, 0f, 1f);
        lastFrameUseMs = System.currentTimeMillis();
    }

    public static void drawCachedRegion(GuiGraphics graphics) {
        pruneIdleSurfaces();
        if (regionTexture == null || regionCapacityPixelW <= 0 || regionCapacityPixelH <= 0) return;
        graphics.blit(RenderPipelines.GUI_TEXTURED, REGION_TEXTURE_ID, regionX, regionY, 0f, 0f, regionW, regionH, regionPixelW, regionPixelH, regionCapacityPixelW, regionCapacityPixelH);
        lastRegionUseMs = System.currentTimeMillis();
    }

    public static boolean isDrawing() {
        return drawing || GL_BACKEND.isDrawing();
    }

    public static boolean hasFrameCache() {
        if (USE_GL_BACKEND_FOR_FRAME) return GL_BACKEND.hasSurface();
        return surface != null && dynamicTexture != null;
    }

    public static boolean supportsFrameCache() {
        return !USE_GL_BACKEND_FOR_FRAME;
    }

    public static boolean hasRegionCache() {
        return regionSurface != null && regionTexture != null;
    }

    public static void markFrameDirty() {
        frameDirty = true;
    }

    public static void markRegionDirty() {
        regionDirty = true;
        regionCacheValid = false;
    }

    private static boolean uploadSurface(Surface sourceSurface, DynamicTexture targetTexture, int uploadW, int uploadH) {
        Pixmap pixmap = new Pixmap();
        try {
            if (!sourceSurface.peekPixels(pixmap)) {
                return false;
            }
            long addr = pixmap.getAddr();
            int byteSize = uploadH * pixmap.getRowBytes();
            ByteBuffer buf = MemoryUtil.memByteBuffer(addr, byteSize);
            GpuTexture gpuTexture = targetTexture.getTexture();
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToTexture(gpuTexture, buf, NativeImage.Format.RGBA, 0, 0, 0, 0, uploadW, uploadH);
            return true;
        } finally {
            pixmap.close();
        }
    }

    private static void pruneIdleSurfaces() {
        long now = System.currentTimeMillis();
        if (!drawing && surface != null && lastFrameUseMs > 0L && now - lastFrameUseMs > FRAME_IDLE_TIMEOUT_MS) {
            destroySurface();
            lastPixelW = -1;
            lastPixelH = -1;
            frameDirty = true;
            lastFrameUseMs = 0L;
        }
        if (!regionDrawing && regionSurface != null && lastRegionUseMs > 0L && now - lastRegionUseMs > REGION_IDLE_TIMEOUT_MS) {
            destroyRegionSurface();
            regionPixelW = -1;
            regionPixelH = -1;
            regionCapacityPixelW = -1;
            regionCapacityPixelH = -1;
            regionDirty = true;
            lastRegionUseMs = 0L;
        }
    }

    private static void destroySurface() {
        if (surface != null) {
            surface.close();
            surface = null;
        }
        if (dynamicTexture != null) {
            Minecraft.getInstance().getTextureManager().release(TEXTURE_ID);
            dynamicTexture = null;
        }
    }

    private static void destroyRegionSurface() {
        if (regionSurface != null) {
            regionSurface.close();
            regionSurface = null;
        }
        if (regionTexture != null) {
            Minecraft.getInstance().getTextureManager().release(REGION_TEXTURE_ID);
            regionTexture = null;
        }
        // The cached texture is gone, so the next pass must re-upload.
        regionCacheValid = false;
    }

    public static void destroy() {
        GL_BACKEND.destroy();
        destroySurface();
        destroyRegionSurface();
        lastPixelW = -1;
        lastPixelH = -1;
        regionPixelW = -1;
        regionPixelH = -1;
        regionCapacityPixelW = -1;
        regionCapacityPixelH = -1;
        drawing = false;
        regionDrawing = false;
        frameDirty = true;
        regionDirty = true;
        lastFrameUseMs = 0L;
        lastRegionUseMs = 0L;
    }

    public static void resetFrameState() {
        drawing = false;
        regionDrawing = false;
        GL_BACKEND.resetCanvasState();
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
