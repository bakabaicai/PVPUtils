package com.pvp_utils.client.modules.impl.Render.motionblur;

import com.mojang.blaze3d.buffers.GpuBuffer;
import net.minecraft.client.renderer.PostChain;

import java.util.Map;

public final class ManagedUniformBuffer {
    private final String debugName;
    private final int sizeBytes;
    private PostChain owner = null;
    private GpuBuffer buffer = null;

    public ManagedUniformBuffer(String debugName, int sizeBytes) {
        this.debugName = debugName;
        this.sizeBytes = sizeBytes;
    }

    public GpuBuffer put(PostChain chain, Map<String, GpuBuffer> uniformBuffers, String uniformName) {
        GpuBuffer ubo = get(chain);
        GpuBuffer old = uniformBuffers.get(uniformName);
        if (old == ubo) return ubo;

        old = uniformBuffers.put(uniformName, ubo);
        if (old != null && old != ubo) GpuBufferUtil.closeQuietly(old);
        return ubo;
    }

    public void reset() {
        GpuBufferUtil.closeQuietly(buffer);
        owner = null;
        buffer = null;
    }

    public boolean resetIfClosed(RuntimeException e) {
        if (!GpuBufferUtil.isClosedBufferException(e)) return false;
        reset();
        return true;
    }

    private GpuBuffer get(PostChain chain) {
        if (chain != owner) {
            reset();
            owner = chain;
        }
        if (buffer == null) buffer = GpuBufferUtil.createUBO(debugName, sizeBytes);
        return buffer;
    }

    public static final class Ring {
        private final String debugName;
        private final int sizeBytes;
        private final GpuBuffer[] buffers;
        private PostChain owner = null;
        private int index = 0;

        public Ring(String debugName, int sizeBytes, int count) {
            this.debugName = debugName;
            this.sizeBytes = sizeBytes;
            this.buffers = new GpuBuffer[count];
        }

        public GpuBuffer putNext(PostChain chain, Map<String, GpuBuffer> uniformBuffers, String uniformName) {
            GpuBuffer ubo = next(chain);
            GpuBuffer old = uniformBuffers.get(uniformName);
            if (old == ubo) return ubo;

            old = uniformBuffers.put(uniformName, ubo);
            if (old != null && old != ubo && !owns(old)) GpuBufferUtil.closeQuietly(old);
            return ubo;
        }

        public void reset() {
            for (int i = 0; i < buffers.length; i++) {
                GpuBufferUtil.closeQuietly(buffers[i]);
                buffers[i] = null;
            }
            owner = null;
            index = 0;
        }

        public boolean resetIfClosed(RuntimeException e) {
            if (!GpuBufferUtil.isClosedBufferException(e)) return false;
            reset();
            return true;
        }

        private GpuBuffer next(PostChain chain) {
            if (chain != owner) {
                reset();
                owner = chain;
            }

            GpuBuffer ubo = buffers[index];
            if (ubo == null) {
                ubo = GpuBufferUtil.createUBO(debugName, sizeBytes);
                buffers[index] = ubo;
            }
            index = (index + 1) % buffers.length;
            return ubo;
        }

        private boolean owns(GpuBuffer buffer) {
            for (GpuBuffer owned : buffers) {
                if (owned == buffer) return true;
            }
            return false;
        }
    }
}
