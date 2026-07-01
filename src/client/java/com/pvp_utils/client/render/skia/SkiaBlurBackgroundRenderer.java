package com.pvp_utils.client.render.skia;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.DirectContext;
import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.ImageFilter;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.SurfaceOrigin;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;

import static org.lwjgl.opengl.GL45.*;

public final class SkiaBlurBackgroundRenderer implements BlurBackgroundRenderer {
    private static final SkiaBlurBackgroundRenderer INSTANCE = new SkiaBlurBackgroundRenderer();
    private static final int MAX_COLOR_ATTACHMENTS = 32;

    private final Paint blurPaint = new Paint().setAntiAlias(true);
    private final Paint tintPaint = new Paint().setAntiAlias(true);

    public static SkiaBlurBackgroundRenderer getInstance() {
        return INSTANCE;
    }

    @Override
    public void render(DirectContext context, Canvas canvas, int sourceFramebufferId, float x, float y, float width, float height, float cornerRadius, BlurStyle style, int tintColor, float blurStrength) {
        if (context == null || canvas == null || width <= 0f || height <= 0f) return;

        Minecraft client = Minecraft.getInstance();
        if (client.getWindow() == null) return;

        float safeStrength = Math.max(0f, blurStrength);
        float sigma = Math.max(0f, style.sigma() * safeStrength);
        float pad = Math.max(12f, cornerRadius + sigma * 2.2f);
        float scale = (float) client.getWindow().getGuiScale();
        int framebufferW = client.getWindow().getWidth();
        int framebufferH = client.getWindow().getHeight();

        int left = Math.max(0, (int) Math.floor((x - pad) * scale));
        int top = Math.max(0, (int) Math.floor((y - pad) * scale));
        int right = Math.min(framebufferW, (int) Math.ceil((x + width + pad) * scale));
        int bottom = Math.min(framebufferH, (int) Math.ceil((y + height + pad) * scale));
        int copyW = Math.max(1, right - left);
        int copyH = Math.max(1, bottom - top);
        int sourceY = Math.max(0, framebufferH - bottom);

        int textureId = glGenTextures();
        int[] oldTexture = new int[1];
        int[] oldReadFramebuffer = new int[1];
        int[] oldReadBuffer = new int[1];
        glGetIntegerv(GL_TEXTURE_BINDING_2D, oldTexture);
        glGetIntegerv(GL_READ_FRAMEBUFFER_BINDING, oldReadFramebuffer);
        glGetIntegerv(GL_READ_BUFFER, oldReadBuffer);

        Image image = null;
        ImageFilter blur = null;
        try {
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, copyW, copyH, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0L);

            int readBuffer = prepareReadFramebuffer(sourceFramebufferId);
            if (readBuffer == GL_NONE) return;
            glReadBuffer(readBuffer);
            glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, left, sourceY, copyW, copyH);

            image = Image.adoptGLTextureFrom(context, textureId, GL_TEXTURE_2D, copyW, copyH, GL_RGBA8, SurfaceOrigin.BOTTOM_LEFT, ColorType.RGBA_8888);
            textureId = 0;

            canvas.save();
            canvas.clipRRect(RRect.makeXYWH(x, y, width, height, cornerRadius), true);

            if (sigma > 0.01f) {
                blur = ImageFilter.makeBlur(sigma, sigma, FilterTileMode.CLAMP);
                blurPaint.setImageFilter(blur);
                canvas.drawImageRect(
                        image,
                        Rect.makeXYWH(0f, 0f, copyW, copyH),
                        Rect.makeXYWH(left / scale, top / scale, copyW / scale, copyH / scale),
                        blurPaint,
                        true
                );
                blurPaint.setImageFilter(null);
            }

            tintPaint.setColor(tintColor);
            canvas.drawRRect(RRect.makeXYWH(x, y, width, height, cornerRadius), tintPaint);
            canvas.restore();
        } finally {
            if (blur != null) blur.close();
            if (image != null) image.close();
            if (textureId != 0) glDeleteTextures(textureId);
            glBindFramebuffer(GL_READ_FRAMEBUFFER, oldReadFramebuffer[0]);
            restoreReadBuffer(oldReadFramebuffer[0], oldReadBuffer[0]);
            glBindTexture(GL_TEXTURE_2D, oldTexture[0]);
        }
    }

    private int prepareReadFramebuffer(int framebufferId) {
        glBindFramebuffer(GL_READ_FRAMEBUFFER, framebufferId);
        if (glCheckFramebufferStatus(GL_READ_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            return GL_NONE;
        }

        if (framebufferId == 0) {
            return GL_BACK;
        }

        int attachmentType = glGetFramebufferAttachmentParameteri(
                GL_READ_FRAMEBUFFER,
                GL_COLOR_ATTACHMENT0,
                GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE
        );
        return attachmentType == GL_NONE ? GL_NONE : GL_COLOR_ATTACHMENT0;
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
        return readBuffer >= GL_COLOR_ATTACHMENT0 && readBuffer < GL_COLOR_ATTACHMENT0 + MAX_COLOR_ATTACHMENTS;
    }
}
