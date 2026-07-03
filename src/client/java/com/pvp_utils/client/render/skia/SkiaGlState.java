package com.pvp_utils.client.render.skia;

import org.lwjgl.opengl.GL;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL45.*;

final class SkiaGlState {
    private final int glVersion;
    private final int[] lastActiveTexture = new int[1];
    private final int[] lastProgram = new int[1];
    private final int[] lastTexture = new int[1];
    private final int[] lastSampler = new int[1];
    private final int[] lastArrayBuffer = new int[1];
    private final int[] lastElementArrayBuffer = new int[1];
    private final int[] lastUniformBuffer = new int[1];
    private final int[] lastVertexArrayObject = new int[1];
    private final int[] lastReadFramebuffer = new int[1];
    private final int[] lastDrawFramebuffer = new int[1];
    private final int[] lastReadBuffer = new int[1];
    private final int[] lastDrawBuffer = new int[1];
    private final int[] lastPolygonMode = new int[2];
    private final int[] lastViewport = new int[4];
    private final int[] lastScissorBox = new int[4];
    private final int[] lastBlendSrcRgb = new int[1];
    private final int[] lastBlendDstRgb = new int[1];
    private final int[] lastBlendSrcAlpha = new int[1];
    private final int[] lastBlendDstAlpha = new int[1];
    private final int[] lastBlendEquationRgb = new int[1];
    private final int[] lastBlendEquationAlpha = new int[1];
    private final int[] lastDepthFunc = new int[1];
    private final int[] lastCullFaceMode = new int[1];
    private final int[] lastFrontFace = new int[1];
    private final ByteBuffer lastColorMask = BufferUtils.createByteBuffer(4);
    private final int[] lastPixelUnpackBufferBinding = new int[1];
    private final int[] lastPixelPackBufferBinding = new int[1];
    private final int[] lastUnpackAlignment = new int[1];
    private final int[] lastUnpackRowLength = new int[1];
    private final int[] lastUnpackSkipPixels = new int[1];
    private final int[] lastUnpackSkipRows = new int[1];
    private final int[] lastPackSwapBytes = new int[1];
    private final int[] lastPackLsbFirst = new int[1];
    private final int[] lastPackRowLength = new int[1];
    private final int[] lastPackImageHeight = new int[1];
    private final int[] lastPackSkipPixels = new int[1];
    private final int[] lastPackSkipRows = new int[1];
    private final int[] lastPackSkipImages = new int[1];
    private final int[] lastPackAlignment = new int[1];
    private final int[] lastUnpackSwapBytes = new int[1];
    private final int[] lastUnpackLsbFirst = new int[1];
    private final int[] lastUnpackImageHeight = new int[1];
    private final int[] lastUnpackSkipImages = new int[1];
    private boolean lastEnableBlend;
    private boolean lastEnableCullFace;
    private boolean lastEnableDepthTest;
    private boolean lastEnableStencilTest;
    private boolean lastEnableScissorTest;
    private boolean lastEnablePrimitiveRestart;
    private boolean lastDepthMask;

    SkiaGlState(int glVersion) {
        this.glVersion = glVersion;
    }

    void push() {
        glGetIntegerv(GL_ACTIVE_TEXTURE, lastActiveTexture);
        glActiveTexture(GL_TEXTURE0);
        glGetIntegerv(GL_CURRENT_PROGRAM, lastProgram);
        glGetIntegerv(GL_TEXTURE_BINDING_2D, lastTexture);
        if (glVersion >= 330 || GL.getCapabilities().GL_ARB_sampler_objects) {
            glGetIntegerv(GL_SAMPLER_BINDING, lastSampler);
        }
        glGetIntegerv(GL_ARRAY_BUFFER_BINDING, lastArrayBuffer);
        glGetIntegerv(GL_ELEMENT_ARRAY_BUFFER_BINDING, lastElementArrayBuffer);
        glGetIntegerv(GL_UNIFORM_BUFFER_BINDING, lastUniformBuffer);
        glGetIntegerv(GL_VERTEX_ARRAY_BINDING, lastVertexArrayObject);
        glGetIntegerv(GL_READ_FRAMEBUFFER_BINDING, lastReadFramebuffer);
        glGetIntegerv(GL_DRAW_FRAMEBUFFER_BINDING, lastDrawFramebuffer);
        glGetIntegerv(GL_READ_BUFFER, lastReadBuffer);
        glGetIntegerv(GL_DRAW_BUFFER, lastDrawBuffer);
        if (glVersion >= 200) {
            glGetIntegerv(GL_POLYGON_MODE, lastPolygonMode);
        }
        glGetIntegerv(GL_VIEWPORT, lastViewport);
        glGetIntegerv(GL_SCISSOR_BOX, lastScissorBox);
        glGetIntegerv(GL_BLEND_SRC_RGB, lastBlendSrcRgb);
        glGetIntegerv(GL_BLEND_DST_RGB, lastBlendDstRgb);
        glGetIntegerv(GL_BLEND_SRC_ALPHA, lastBlendSrcAlpha);
        glGetIntegerv(GL_BLEND_DST_ALPHA, lastBlendDstAlpha);
        glGetIntegerv(GL_BLEND_EQUATION_RGB, lastBlendEquationRgb);
        glGetIntegerv(GL_BLEND_EQUATION_ALPHA, lastBlendEquationAlpha);
        glGetIntegerv(GL_DEPTH_FUNC, lastDepthFunc);
        glGetIntegerv(GL_CULL_FACE_MODE, lastCullFaceMode);
        glGetIntegerv(GL_FRONT_FACE, lastFrontFace);
        lastColorMask.clear();
        glGetBooleanv(GL_COLOR_WRITEMASK, lastColorMask);
        lastEnableBlend = glIsEnabled(GL_BLEND);
        lastEnableCullFace = glIsEnabled(GL_CULL_FACE);
        lastEnableDepthTest = glIsEnabled(GL_DEPTH_TEST);
        lastEnableStencilTest = glIsEnabled(GL_STENCIL_TEST);
        lastEnableScissorTest = glIsEnabled(GL_SCISSOR_TEST);
        if (glVersion >= 310) {
            lastEnablePrimitiveRestart = glIsEnabled(GL_PRIMITIVE_RESTART);
        }
        lastDepthMask = glGetBoolean(GL_DEPTH_WRITEMASK);

        glGetIntegerv(GL_PIXEL_UNPACK_BUFFER_BINDING, lastPixelUnpackBufferBinding);
        glGetIntegerv(GL_PIXEL_PACK_BUFFER_BINDING, lastPixelPackBufferBinding);
        glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
        glGetIntegerv(GL_PACK_SWAP_BYTES, lastPackSwapBytes);
        glGetIntegerv(GL_PACK_LSB_FIRST, lastPackLsbFirst);
        glGetIntegerv(GL_PACK_ROW_LENGTH, lastPackRowLength);
        glGetIntegerv(GL_PACK_SKIP_PIXELS, lastPackSkipPixels);
        glGetIntegerv(GL_PACK_SKIP_ROWS, lastPackSkipRows);
        glGetIntegerv(GL_PACK_ALIGNMENT, lastPackAlignment);
        glGetIntegerv(GL_UNPACK_SWAP_BYTES, lastUnpackSwapBytes);
        glGetIntegerv(GL_UNPACK_LSB_FIRST, lastUnpackLsbFirst);
        glGetIntegerv(GL_UNPACK_ALIGNMENT, lastUnpackAlignment);
        glGetIntegerv(GL_UNPACK_ROW_LENGTH, lastUnpackRowLength);
        glGetIntegerv(GL_UNPACK_SKIP_PIXELS, lastUnpackSkipPixels);
        glGetIntegerv(GL_UNPACK_SKIP_ROWS, lastUnpackSkipRows);
        if (glVersion >= 120) {
            glGetIntegerv(GL_PACK_IMAGE_HEIGHT, lastPackImageHeight);
            glGetIntegerv(GL_PACK_SKIP_IMAGES, lastPackSkipImages);
            glGetIntegerv(GL_UNPACK_IMAGE_HEIGHT, lastUnpackImageHeight);
            glGetIntegerv(GL_UNPACK_SKIP_IMAGES, lastUnpackSkipImages);
        }

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
        glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
    }

    void pop() {
        glUseProgram(lastProgram[0]);
        glBindTexture(GL_TEXTURE_2D, lastTexture[0]);
        if (glVersion >= 330 || GL.getCapabilities().GL_ARB_sampler_objects) {
            glBindSampler(0, lastSampler[0]);
        }
        glActiveTexture(lastActiveTexture[0]);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, lastReadFramebuffer[0]);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, lastDrawFramebuffer[0]);
        restoreReadBuffer(lastReadFramebuffer[0], lastReadBuffer[0]);
        restoreDrawBuffer(lastDrawFramebuffer[0], lastDrawBuffer[0]);
        glBindVertexArray(lastVertexArrayObject[0]);
        glBindBuffer(GL_ARRAY_BUFFER, lastArrayBuffer[0]);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, lastElementArrayBuffer[0]);
        glBindBuffer(GL_UNIFORM_BUFFER, lastUniformBuffer[0]);
        glBlendEquationSeparate(lastBlendEquationRgb[0], lastBlendEquationAlpha[0]);
        glBlendFuncSeparate(lastBlendSrcRgb[0], lastBlendDstRgb[0], lastBlendSrcAlpha[0], lastBlendDstAlpha[0]);
        glDepthFunc(lastDepthFunc[0]);
        glCullFace(lastCullFaceMode[0]);
        glFrontFace(lastFrontFace[0]);
        glColorMask(lastColorMask.get(0) != 0, lastColorMask.get(1) != 0, lastColorMask.get(2) != 0, lastColorMask.get(3) != 0);
        setEnabled(GL_BLEND, lastEnableBlend);
        setEnabled(GL_CULL_FACE, lastEnableCullFace);
        setEnabled(GL_DEPTH_TEST, lastEnableDepthTest);
        setEnabled(GL_STENCIL_TEST, lastEnableStencilTest);
        setEnabled(GL_SCISSOR_TEST, lastEnableScissorTest);
        if (glVersion >= 310) {
            setEnabled(GL_PRIMITIVE_RESTART, lastEnablePrimitiveRestart);
        }
        if (glVersion >= 200) {
            glPolygonMode(GL_FRONT_AND_BACK, lastPolygonMode[0]);
        }
        glViewport(lastViewport[0], lastViewport[1], lastViewport[2], lastViewport[3]);
        glScissor(lastScissorBox[0], lastScissorBox[1], lastScissorBox[2], lastScissorBox[3]);
        glPixelStorei(GL_PACK_SWAP_BYTES, lastPackSwapBytes[0]);
        glPixelStorei(GL_PACK_LSB_FIRST, lastPackLsbFirst[0]);
        glPixelStorei(GL_PACK_ROW_LENGTH, lastPackRowLength[0]);
        glPixelStorei(GL_PACK_SKIP_PIXELS, lastPackSkipPixels[0]);
        glPixelStorei(GL_PACK_SKIP_ROWS, lastPackSkipRows[0]);
        glPixelStorei(GL_PACK_ALIGNMENT, lastPackAlignment[0]);
        glBindBuffer(GL_PIXEL_PACK_BUFFER, lastPixelPackBufferBinding[0]);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, lastPixelUnpackBufferBinding[0]);
        glPixelStorei(GL_UNPACK_SWAP_BYTES, lastUnpackSwapBytes[0]);
        glPixelStorei(GL_UNPACK_LSB_FIRST, lastUnpackLsbFirst[0]);
        glPixelStorei(GL_UNPACK_ALIGNMENT, lastUnpackAlignment[0]);
        glPixelStorei(GL_UNPACK_ROW_LENGTH, lastUnpackRowLength[0]);
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, lastUnpackSkipPixels[0]);
        glPixelStorei(GL_UNPACK_SKIP_ROWS, lastUnpackSkipRows[0]);
        if (glVersion >= 120) {
            glPixelStorei(GL_PACK_IMAGE_HEIGHT, lastPackImageHeight[0]);
            glPixelStorei(GL_PACK_SKIP_IMAGES, lastPackSkipImages[0]);
            glPixelStorei(GL_UNPACK_IMAGE_HEIGHT, lastUnpackImageHeight[0]);
            glPixelStorei(GL_UNPACK_SKIP_IMAGES, lastUnpackSkipImages[0]);
        }
        glDepthMask(lastDepthMask);
    }

    private static void setEnabled(int target, boolean enabled) {
        if (enabled) glEnable(target);
        else glDisable(target);
    }

    private static void restoreReadBuffer(int framebufferId, int readBuffer) {
        if (readBuffer == GL_NONE) {
            glReadBuffer(GL_NONE);
            return;
        }
        if (framebufferId == 0) {
            glReadBuffer(isDefaultFramebufferBuffer(readBuffer) ? readBuffer : GL_BACK);
            return;
        }
        glReadBuffer(isColorAttachmentBuffer(readBuffer) ? readBuffer : GL_COLOR_ATTACHMENT0);
    }

    private static void restoreDrawBuffer(int framebufferId, int drawBuffer) {
        if (drawBuffer == GL_NONE) {
            glDrawBuffer(GL_NONE);
            return;
        }
        if (framebufferId == 0) {
            glDrawBuffer(isDefaultFramebufferBuffer(drawBuffer) ? drawBuffer : GL_BACK);
            return;
        }
        glDrawBuffer(isColorAttachmentBuffer(drawBuffer) ? drawBuffer : GL_COLOR_ATTACHMENT0);
    }

    private static boolean isDefaultFramebufferBuffer(int buffer) {
        return buffer == GL_FRONT
                || buffer == GL_BACK
                || buffer == GL_LEFT
                || buffer == GL_RIGHT
                || buffer == GL_FRONT_LEFT
                || buffer == GL_FRONT_RIGHT
                || buffer == GL_BACK_LEFT
                || buffer == GL_BACK_RIGHT;
    }

    private static boolean isColorAttachmentBuffer(int buffer) {
        return buffer >= GL_COLOR_ATTACHMENT0 && buffer <= GL_COLOR_ATTACHMENT0 + 31;
    }
}
