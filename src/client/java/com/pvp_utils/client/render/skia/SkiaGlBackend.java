package com.pvp_utils.client.render.skia;

import io.github.humbleui.skija.BackendRenderTarget;
import io.github.humbleui.skija.BackendState;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorSpace;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.DirectContext;
import io.github.humbleui.skija.FramebufferFormat;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.SurfaceOrigin;
import net.minecraft.client.Minecraft;

import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL30.GL_MAJOR_VERSION;
import static org.lwjgl.opengl.GL30.GL_MINOR_VERSION;
import static org.lwjgl.opengl.GL30.glGetIntegerv;

public final class SkiaGlBackend {
    private static final BackendState[] RESET_STATES = {
            BackendState.GL_BLEND,
            BackendState.GL_VERTEX,
            BackendState.GL_PIXEL_STORE,
            BackendState.GL_TEXTURE_BINDING,
            BackendState.GL_MISC
    };

    private DirectContext context;
    private BackendRenderTarget renderTarget;
    private Surface surface;
    private Canvas canvas;
    private SkiaGlState state;
    private int width = -1;
    private int height = -1;
    private int framebufferId = -1;
    private boolean drawing = false;

    public Canvas begin() {
        return begin(0);
    }

    public Canvas begin(int targetFramebufferId) {
        if (drawing) return canvas;
        var window = Minecraft.getInstance().getWindow();
        int targetW = Math.max(1, window.getWidth());
        int targetH = Math.max(1, window.getHeight());
        ensureSurface(targetW, targetH, targetFramebufferId);
        if (surface == null || canvas == null) return null;

        state.push();
        glDisable(GL_CULL_FACE);
        glClearColor(0f, 0f, 0f, 0f);
        context.reset(RESET_STATES);

        canvas.restoreToCount(1);
        canvas.resetMatrix();
        canvas.save();
        canvas.scale((float) window.getGuiScale(), (float) window.getGuiScale());
        drawing = true;
        return canvas;
    }

    public void end() {
        if (!drawing || surface == null) return;
        try {
            canvas.restore();
            context.flushAndSubmit(surface);
        } finally {
            drawing = false;
            state.pop();
        }
    }

    public boolean isDrawing() {
        return drawing;
    }

    public boolean hasSurface() {
        return surface != null;
    }

    public void resetCanvasState() {
        drawing = false;
        if (canvas != null) {
            canvas.restoreToCount(1);
            canvas.resetMatrix();
        }
    }

    public void destroy() {
        resetCanvasState();
        if (surface != null) {
            surface.close();
            surface = null;
        }
        if (renderTarget != null) {
            renderTarget.close();
            renderTarget = null;
        }
        if (context != null) {
            context.close();
            context = null;
        }
        canvas = null;
        state = null;
        width = -1;
        height = -1;
        framebufferId = -1;
    }

    private void ensureSurface(int targetW, int targetH, int targetFramebufferId) {
        ensureContext();
        if (surface != null && targetW == width && targetH == height && targetFramebufferId == framebufferId) return;

        if (surface != null) {
            surface.close();
            surface = null;
        }
        if (renderTarget != null) {
            renderTarget.close();
            renderTarget = null;
        }

        renderTarget = BackendRenderTarget.makeGL(targetW, targetH, 0, 8, targetFramebufferId, FramebufferFormat.GR_GL_RGBA8);
        surface = Surface.wrapBackendRenderTarget(
                context,
                renderTarget,
                SurfaceOrigin.BOTTOM_LEFT,
                ColorType.RGBA_8888,
                ColorSpace.getSRGB()
        );
        canvas = surface.getCanvas();
        width = targetW;
        height = targetH;
        framebufferId = targetFramebufferId;
    }

    private void ensureContext() {
        if (context != null) return;
        context = DirectContext.makeGL();
        state = new SkiaGlState(readGlVersion());
    }

    private static int readGlVersion() {
        int[] major = new int[1];
        int[] minor = new int[1];
        glGetIntegerv(GL_MAJOR_VERSION, major);
        glGetIntegerv(GL_MINOR_VERSION, minor);
        return major[0] * 100 + minor[0] * 10;
    }
}
