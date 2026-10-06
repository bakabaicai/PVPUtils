package com.pvp_utils.client.render.skia;

import io.github.humbleui.skija.BackendRenderTarget;
import io.github.humbleui.skija.BackendTexture;
import io.github.humbleui.skija.ColorAlphaType;
import io.github.humbleui.skija.GLTextureInfo;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorSpace;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.DirectContext;
import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.ImageFilter;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.SurfaceOrigin;
import io.github.humbleui.types.Rect;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.util.function.Consumer;

final class SkijaBackdrop implements AutoCloseable {
    private int texture;
    private int framebuffer;
    private int width;
    private int height;
    private BackendTexture backend;

    static Surface wrap(DirectContext context, BackendRenderTarget target) {
        return Surface.wrapBackendRenderTarget(context, target, SurfaceOrigin.BOTTOM_LEFT,
                ColorType.RGBA_8888, ColorSpace.getSRGB());
    }

    Image capture(DirectContext context, int source, int w, int h) {
        int previousRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int previousUnpack = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        boolean srgb = GL11.glIsEnabled(GL30.GL_FRAMEBUFFER_SRGB);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var colorMask = stack.malloc(4);
            GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, colorMask);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source);
            int previousReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);
            try {
                ensureSize(w, h);
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, framebuffer);
                GL11.glReadBuffer(source == 0 ? GL11.GL_BACK : GL30.GL_COLOR_ATTACHMENT0);
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
                GL11.glDisable(GL30.GL_FRAMEBUFFER_SRGB);
                GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
                GL11.glColorMask(false, false, false, true);
                GL30.glClearBufferfv(GL11.GL_COLOR, 0, stack.floats(0f, 0f, 0f, 1f));
            } finally {
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source);
                GL11.glReadBuffer(previousReadBuffer);
                GL11.glColorMask(colorMask.get(0) != 0, colorMask.get(1) != 0,
                        colorMask.get(2) != 0, colorMask.get(3) != 0);
            }
        } finally {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, previousUnpack);
            if (scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST);
            if (srgb) GL11.glEnable(GL30.GL_FRAMEBUFFER_SRGB);
            context.resetGLAll();
        }
        Image image = Image.borrowTextureFrom(context, backend, SurfaceOrigin.BOTTOM_LEFT,
                ColorType.RGBA_8888, ColorAlphaType.OPAQUE, ColorSpace.getSRGB(), () -> {});
        if (image == null) throw new IllegalStateException("Failed to capture opaque backdrop");
        return image;
    }

    private void ensureSize(int w, int h) {
        if (backend != null && width == w && height == h) return;
        close();
        texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0L);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        framebuffer = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, framebuffer);
        GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, texture, 0);
        if (GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            close();
            throw new IllegalStateException("Incomplete backdrop framebuffer");
        }
        backend = BackendTexture.makeGL(w, h, false,
                new GLTextureInfo(GL11.GL_TEXTURE_2D, texture, GL11.GL_RGBA8));
        width = w;
        height = h;
    }

    @Override
    public void close() {
        if (backend != null) backend.close();
        if (framebuffer != 0) GL30.glDeleteFramebuffers(framebuffer);
        if (texture != 0) GL11.glDeleteTextures(texture);
        backend = null;
        framebuffer = 0;
        texture = 0;
        width = 0;
        height = 0;
    }

    static void draw(Canvas canvas, Image image, Consumer<Canvas> clipper,
                     float x, float y, float width, float height,
                     float guiWidth, float guiHeight, float strength) {
        float scaleX = image.getWidth() / guiWidth;
        float scaleY = image.getHeight() / guiHeight;
        float padding = Math.max(2f, strength * 3f + 1f);
        int left = Math.max(0, (int) Math.floor((x - padding) * scaleX));
        int top = Math.max(0, (int) Math.floor((y - padding) * scaleY));
        int right = Math.min(image.getWidth(), (int) Math.ceil((x + width + padding) * scaleX));
        int bottom = Math.min(image.getHeight(), (int) Math.ceil((y + height + padding) * scaleY));
        if (right <= left || bottom <= top) return;
        try (ImageFilter filter = ImageFilter.makeBlur(strength, strength, FilterTileMode.CLAMP);
             Paint paint = new Paint().setAntiAlias(true).setImageFilter(filter)) {
            int save = canvas.save();
            try {
                clipper.accept(canvas);
                canvas.drawImageRect(image, Rect.makeLTRB(left, top, right, bottom),
                        Rect.makeLTRB(left / scaleX, top / scaleY, right / scaleX, bottom / scaleY),
                        SamplingMode.LINEAR, paint, true);
            } finally {
                canvas.restoreToCount(save);
            }
        }
    }
}
