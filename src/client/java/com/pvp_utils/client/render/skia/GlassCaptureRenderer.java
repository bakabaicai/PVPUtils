package com.pvp_utils.client.render.skia;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.DirectContext;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.SurfaceOrigin;
import io.github.humbleui.skija.impl.Library;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;

import static org.lwjgl.opengl.GL45.*;

public final class GlassCaptureRenderer {
    private static final GlassCaptureRenderer INSTANCE = new GlassCaptureRenderer();

    private final GlassGlBackend framebufferBackend = new GlassGlBackend();

    private boolean nativeLoaded = false;
    private final int[] oldTexture = new int[1];
    private final int[] oldActiveTexture = new int[1];
    private final int[] oldSampler = new int[1];
    private final int[] oldReadFramebuffer = new int[1];
    private final int[] oldDrawFramebuffer = new int[1];
    private final int[] oldReadBuffer = new int[1];
    private final int[] oldDrawBuffer = new int[1];
    private final int[] oldViewport = new int[4];
    private final int[] oldScissorBox = new int[4];
    private int captureFramebufferId = 0;

    private GlassCaptureRenderer() {}

    public static GlassCaptureRenderer getInstance() {
        return INSTANCE;
    }

    public static int currentDrawFramebufferId() {
        int[] framebuffer = new int[1];
        glGetIntegerv(GL_DRAW_FRAMEBUFFER_BINDING, framebuffer);
        return framebuffer[0];
    }

    public Canvas beginFrame(int framebufferId) {
        if (SkijaRenderer.isDrawing()) {
            SkijaRenderer.flush();
            framebufferId = SkijaRenderer.currentFramebuffer();
        }
        return framebufferBackend.begin(framebufferId);
    }

    public void endFrame() {
        framebufferBackend.end();
        SkijaRenderer.restoreContext();
    }

    public DirectContext context() {
        return framebufferBackend.ensureContext();
    }

    public Capture capture(Minecraft client, float x, float y, float width, float height, float margin) {
        if (client == null || client.getWindow() == null) return Capture.EMPTY;
        ensureNativeLoaded();
        DirectContext context = framebufferBackend.ensureContext();
        int framebufferId = mainFramebufferId(client);
        if (framebufferId == 0) return Capture.EMPTY;
        float scale = (float) client.getWindow().getGuiScale();
        return captureRegion(context, client, framebufferId, x, y, width, height, scale, margin);
    }

    private Capture captureRegion(DirectContext context, Minecraft client, int sourceFramebufferId,
                                  float x, float y, float width, float height, float scale, float margin) {
        int framebufferW = client.getWindow().getWidth();
        int framebufferH = client.getWindow().getHeight();
        int left = Math.max(0, (int) Math.floor((x - margin) * scale));
        int top = Math.max(0, (int) Math.floor((y - margin) * scale));
        int right = Math.min(framebufferW, (int) Math.ceil((x + width + margin) * scale));
        int bottom = Math.min(framebufferH, (int) Math.ceil((y + height + margin) * scale));
        int copyW = Math.max(1, right - left);
        int copyH = Math.max(1, bottom - top);
        int sourceY = Math.max(0, framebufferH - bottom);

        boolean framebufferSrgb = glIsEnabled(GL_FRAMEBUFFER_SRGB);
        glGetIntegerv(GL_ACTIVE_TEXTURE, oldActiveTexture);
        glActiveTexture(GL_TEXTURE0);
        glGetIntegerv(GL_TEXTURE_BINDING_2D, oldTexture);
        glGetIntegerv(GL_SAMPLER_BINDING, oldSampler);
        glGetIntegerv(GL_READ_FRAMEBUFFER_BINDING, oldReadFramebuffer);
        glGetIntegerv(GL_DRAW_FRAMEBUFFER_BINDING, oldDrawFramebuffer);
        glGetIntegerv(GL_READ_BUFFER, oldReadBuffer);
        glGetIntegerv(GL_DRAW_BUFFER, oldDrawBuffer);
        glGetIntegerv(GL_VIEWPORT, oldViewport);
        glGetIntegerv(GL_SCISSOR_BOX, oldScissorBox);
        CaptureTarget target = null;
        boolean handedOff = false;
        try {
            target = ensureCaptureTarget(context, copyW, copyH);
            if (target == null) return Capture.EMPTY;

            glDisable(GL_FRAMEBUFFER_SRGB);
            glBindTexture(GL_TEXTURE_2D, target.textureId);
            glBindSampler(0, 0);

            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, target.framebufferId);
            glDrawBuffer(GL_COLOR_ATTACHMENT0);

            int readBuffer = prepareReadFramebuffer(sourceFramebufferId);
            if (readBuffer == 0) {
                return Capture.EMPTY;
            }
            glReadBuffer(readBuffer);
            glBlitFramebuffer(
                    left, sourceY, left + copyW, sourceY + copyH,
                    0, 0, copyW, copyH,
                    GL_COLOR_BUFFER_BIT,
                    GL_NEAREST
            );
            handedOff = true;
            return new Capture(target.image, copyW, copyH,
                    left / scale, top / scale, copyW / scale, copyH / scale);
        } finally {
            if (target != null && !handedOff) {
                target.image.close();
            }
            glBindFramebuffer(GL_READ_FRAMEBUFFER, oldReadFramebuffer[0]);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, oldDrawFramebuffer[0]);
            restoreReadBuffer(oldReadFramebuffer[0], oldReadBuffer[0]);
            restoreDrawBuffer(oldDrawFramebuffer[0], oldDrawBuffer[0]);
            glViewport(oldViewport[0], oldViewport[1], oldViewport[2], oldViewport[3]);
            glScissor(oldScissorBox[0], oldScissorBox[1], oldScissorBox[2], oldScissorBox[3]);
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, oldTexture[0]);
            glBindSampler(0, oldSampler[0]);
            glActiveTexture(oldActiveTexture[0]);
            if (framebufferSrgb) {
                glEnable(GL_FRAMEBUFFER_SRGB);
            } else {
                glDisable(GL_FRAMEBUFFER_SRGB);
            }
        }
    }

    private CaptureTarget ensureCaptureTarget(DirectContext context, int requiredW, int requiredH) {
        if (captureFramebufferId == 0) {
            captureFramebufferId = glGenFramebuffers();
        }
        int framebufferId = captureFramebufferId;
        int textureId = glGenTextures();
        Image image = null;
        CaptureTarget created = null;
        try {
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, requiredW, requiredH, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0L);

            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, framebufferId);
            glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureId, 0);
            glDrawBuffer(GL_COLOR_ATTACHMENT0);
            if (glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                return null;
            }

            try (io.github.humbleui.skija.BackendTexture backend = io.github.humbleui.skija.BackendTexture.makeGL(
                    requiredW, requiredH, false,
                    new io.github.humbleui.skija.GLTextureInfo(GL_TEXTURE_2D, textureId, GL_RGBA8))) {
                image = Image.adoptTextureFrom(context, backend, SurfaceOrigin.BOTTOM_LEFT, ColorType.RGB_888X);
            }
            created = new CaptureTarget(textureId, framebufferId, requiredW, requiredH, image);
            return created;
        } finally {
            if (created == null) {
                if (image != null) {
                    image.close();
                } else if (textureId != 0) {
                    glDeleteTextures(textureId);
                }
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, framebufferId);
                glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, 0, 0);
            }
        }
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

    private void restoreDrawBuffer(int framebufferId, int drawBuffer) {
        if (drawBuffer == GL_NONE) {
            glDrawBuffer(GL_NONE);
            return;
        }
        if (framebufferId == 0) {
            glDrawBuffer(isDefaultFramebufferReadBuffer(drawBuffer) ? drawBuffer : GL_BACK);
            return;
        }
        glDrawBuffer(isColorAttachmentReadBuffer(drawBuffer) ? drawBuffer : GL_COLOR_ATTACHMENT0);
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

    private void ensureNativeLoaded() {
        if (nativeLoaded) return;
        Library.load();
        nativeLoaded = true;
    }

    public void destroy() {
        framebufferBackend.destroy();
        if (captureFramebufferId != 0) glDeleteFramebuffers(captureFramebufferId);
        captureFramebufferId = 0;
    }

    public static final class Capture {
        public static final Capture EMPTY = new Capture(null, 0, 0, 0f, 0f, 0f, 0f);

        public final Image image;
        public final int width;
        public final int height;
        public final float dstX;
        public final float dstY;
        public final float dstW;
        public final float dstH;

        private Capture(Image image, int width, int height, float dstX, float dstY, float dstW, float dstH) {
            this.image = image;
            this.width = width;
            this.height = height;
            this.dstX = dstX;
            this.dstY = dstY;
            this.dstW = dstW;
            this.dstH = dstH;
        }
    }

    private record CaptureTarget(int textureId, int framebufferId, int width, int height, Image image) {}
}
