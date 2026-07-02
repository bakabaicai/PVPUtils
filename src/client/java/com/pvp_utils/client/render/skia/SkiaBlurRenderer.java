package com.pvp_utils.client.render.skia;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorFilter;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.DirectContext;
import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.ImageFilter;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.skija.SurfaceOrigin;
import io.github.humbleui.skija.impl.Library;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;

import static org.lwjgl.opengl.GL45.*;

public final class SkiaBlurRenderer {
    private static final SkiaBlurRenderer INSTANCE = new SkiaBlurRenderer();
    private static final float MIN_CAPTURE_MARGIN = 18f;

    private final Paint blurPaint = new Paint().setAntiAlias(true);
    private final Paint tintPaint = new Paint().setAntiAlias(true);
    private final SkiaGlBackend framebufferBackend = new SkiaGlBackend();
    private boolean nativeLoaded = false;

    private SkiaBlurRenderer() {}

    public static SkiaBlurRenderer getInstance() {
        return INSTANCE;
    }

    public static int currentDrawFramebufferId() {
        int[] framebuffer = new int[1];
        glGetIntegerv(GL_DRAW_FRAMEBUFFER_BINDING, framebuffer);
        return framebuffer[0];
    }

    public boolean render(Minecraft client, float x, float y, float width, float height, float radius, int tintColor, float strength) {
        if (client == null || client.getWindow() == null || client.getMainRenderTarget() == null) return false;
        int framebufferId = mainFramebufferId(client);
        Canvas canvas = framebufferBackend.begin(framebufferId);
        DirectContext context = framebufferBackend.getContext();
        if (canvas == null || context == null) {
            framebufferBackend.end();
            return false;
        }
        try {
            return render(canvas, context, client, framebufferId, x, y, width, height, radius, tintColor, strength);
        } finally {
            framebufferBackend.end();
        }
    }

    public boolean render(Canvas canvas, DirectContext context, Minecraft client, int sourceFramebufferId,
                          float x, float y, float width, float height, float radius, int tintColor, float strength) {
        if (canvas == null || context == null || client == null || client.getWindow() == null) return false;
        ensureNativeLoaded();

        float scale = (float) client.getWindow().getGuiScale();
        float blurSigma = blurSigma(strength);
        Capture capture = captureRegion(client, sourceFramebufferId, x, y, width, height, scale, Math.max(MIN_CAPTURE_MARGIN, blurSigma * 2f));
        if (capture.textureId == 0) return false;

        Image image = null;
        ImageFilter linearize = null;
        ImageFilter blur = null;
        ImageFilter encode = null;
        try {
            image = Image.adoptGLTextureFrom(context, capture.textureId, GL_TEXTURE_2D, capture.width, capture.height,
                    GL_RGBA8, SurfaceOrigin.BOTTOM_LEFT, ColorType.RGB_888X);
            capture.textureId = 0;

            canvas.save();
            canvas.clipRRect(RRect.makeXYWH(x, y, width, height, radius), true);

            linearize = ImageFilter.makeColorFilter(ColorFilter.getSRGBToLinearGamma(), null);
            blur = ImageFilter.makeBlur(blurSigma, blurSigma, FilterTileMode.CLAMP, linearize, (Rect) null);
            encode = ImageFilter.makeColorFilter(ColorFilter.getLinearToSRGBGamma(), blur);
            blurPaint.setImageFilter(encode);
            canvas.drawImageRect(image,
                    Rect.makeXYWH(0f, 0f, capture.width, capture.height),
                    Rect.makeXYWH(capture.dstX, capture.dstY, capture.dstW, capture.dstH),
                    SamplingMode.LINEAR,
                    blurPaint,
                    true);
            blurPaint.setImageFilter(null);

            tintPaint.setColor(tintColor);
            canvas.drawRRect(RRect.makeXYWH(x, y, width, height, radius), tintPaint);
            canvas.restore();
            return true;
        } finally {
            if (encode != null) encode.close();
            if (blur != null) blur.close();
            if (linearize != null) linearize.close();
            if (image != null) image.close();
            if (capture.textureId != 0) glDeleteTextures(capture.textureId);
        }
    }

    private Capture captureRegion(Minecraft client, int sourceFramebufferId, float x, float y, float width, float height, float scale, float margin) {
        int framebufferW = client.getWindow().getWidth();
        int framebufferH = client.getWindow().getHeight();
        int left = Math.max(0, (int) Math.floor((x - margin) * scale));
        int top = Math.max(0, (int) Math.floor((y - margin) * scale));
        int right = Math.min(framebufferW, (int) Math.ceil((x + width + margin) * scale));
        int bottom = Math.min(framebufferH, (int) Math.ceil((y + height + margin) * scale));
        int copyW = Math.max(1, right - left);
        int copyH = Math.max(1, bottom - top);
        int sourceY = Math.max(0, framebufferH - bottom);

        int textureId = glGenTextures();
        int resolveFramebufferId = glGenFramebuffers();
        int[] oldTexture = new int[1];
        int[] oldReadFramebuffer = new int[1];
        int[] oldDrawFramebuffer = new int[1];
        int[] oldReadBuffer = new int[1];
        boolean framebufferSrgb = glIsEnabled(GL_FRAMEBUFFER_SRGB);
        glGetIntegerv(GL_TEXTURE_BINDING_2D, oldTexture);
        glGetIntegerv(GL_READ_FRAMEBUFFER_BINDING, oldReadFramebuffer);
        glGetIntegerv(GL_DRAW_FRAMEBUFFER_BINDING, oldDrawFramebuffer);
        glGetIntegerv(GL_READ_BUFFER, oldReadBuffer);
        try {
            glDisable(GL_FRAMEBUFFER_SRGB);
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, copyW, copyH, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0L);

            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, resolveFramebufferId);
            glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureId, 0);
            if (glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                glDeleteFramebuffers(resolveFramebufferId);
                glDeleteTextures(textureId);
                textureId = 0;
                return Capture.EMPTY;
            }

            int readBuffer = prepareReadFramebuffer(sourceFramebufferId);
            if (readBuffer == 0) {
                glDeleteFramebuffers(resolveFramebufferId);
                glDeleteTextures(textureId);
                textureId = 0;
                return Capture.EMPTY;
            }
            glReadBuffer(readBuffer);
            glBlitFramebuffer(
                    left, sourceY, left + copyW, sourceY + copyH,
                    0, 0, copyW, copyH,
                    GL_COLOR_BUFFER_BIT,
                    GL_NEAREST
            );
            glFlush();
        } finally {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, oldReadFramebuffer[0]);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, oldDrawFramebuffer[0]);
            restoreReadBuffer(oldReadFramebuffer[0], oldReadBuffer[0]);
            glBindTexture(GL_TEXTURE_2D, oldTexture[0]);
            if (framebufferSrgb) {
                glEnable(GL_FRAMEBUFFER_SRGB);
            } else {
                glDisable(GL_FRAMEBUFFER_SRGB);
            }
            if (resolveFramebufferId != 0) glDeleteFramebuffers(resolveFramebufferId);
        }

        return new Capture(textureId, copyW, copyH, left / scale, top / scale, copyW / scale, copyH / scale);
    }

    private int prepareReadFramebuffer(int framebufferId) {
        glBindFramebuffer(GL_READ_FRAMEBUFFER, framebufferId);
        if (glCheckFramebufferStatus(GL_READ_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            return 0;
        }
        if (framebufferId == 0) {
            return GL_BACK;
        }
        int attachmentType = glGetFramebufferAttachmentParameteri(
                GL_READ_FRAMEBUFFER,
                GL_COLOR_ATTACHMENT0,
                GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE
        );
        return attachmentType == GL_NONE ? 0 : GL_COLOR_ATTACHMENT0;
    }

    private void restoreReadBuffer(int framebufferId, int readBuffer) {
        if (readBuffer == GL_NONE) {
            glReadBuffer(GL_NONE);
            return;
        }
        if (framebufferId == 0) {
            glReadBuffer(isDefaultFramebufferReadBuffer(readBuffer) ? readBuffer : GL_BACK);
            return;
        }
        glReadBuffer(isColorAttachmentReadBuffer(readBuffer) ? readBuffer : GL_COLOR_ATTACHMENT0);
    }

    private boolean isDefaultFramebufferReadBuffer(int readBuffer) {
        return readBuffer == GL_FRONT
                || readBuffer == GL_BACK
                || readBuffer == GL_LEFT
                || readBuffer == GL_RIGHT
                || readBuffer == GL_FRONT_LEFT
                || readBuffer == GL_FRONT_RIGHT
                || readBuffer == GL_BACK_LEFT
                || readBuffer == GL_BACK_RIGHT;
    }

    private boolean isColorAttachmentReadBuffer(int readBuffer) {
        return readBuffer >= GL_COLOR_ATTACHMENT0 && readBuffer <= GL_COLOR_ATTACHMENT0 + 31;
    }

    private int mainFramebufferId(Minecraft client) {
        if (client.getMainRenderTarget().getColorTexture() instanceof GlTexture texture
                && RenderSystem.getDevice() instanceof GlDevice device) {
            return texture.getFbo(device.directStateAccess(), client.getMainRenderTarget().getDepthTexture());
        }
        return currentDrawFramebufferId();
    }

    private float blurSigma(float strength) {
        float clamped = Math.max(0f, Math.min(2f, strength));
        return Math.max(0.1f, 3f + clamped * 9f);
    }

    private void ensureNativeLoaded() {
        if (nativeLoaded) return;
        Library.load();
        nativeLoaded = true;
    }

    private static class Capture {
        private static final Capture EMPTY = new Capture(0, 0, 0, 0f, 0f, 0f, 0f);

        private int textureId;
        private final int width;
        private final int height;
        private final float dstX;
        private final float dstY;
        private final float dstW;
        private final float dstH;

        private Capture(int textureId, int width, int height, float dstX, float dstY, float dstW, float dstH) {
            this.textureId = textureId;
            this.width = width;
            this.height = height;
            this.dstX = dstX;
            this.dstY = dstY;
            this.dstW = dstW;
            this.dstH = dstH;
        }
    }
}
