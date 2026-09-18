package com.pvp_utils.client.modules.impl.Render.motionblur;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;

import java.lang.reflect.Method;
import java.util.function.Supplier;

public final class GpuBufferUtil {
    private static final int UBO_USAGE = 130;
    private static Method createBufferMethod = null;

    private GpuBufferUtil() {}

    public static GpuBuffer createUBO(String debugName, int sizeBytes) {
        Object device = RenderSystem.getDevice();
        Supplier<String> label = () -> "pvp_utils:" + debugName;
        try {
            if (createBufferMethod == null) {
                createBufferMethod = device.getClass().getMethod("createBuffer", Supplier.class, int.class, long.class);
            }
            return (GpuBuffer) createBufferMethod.invoke(device, label, UBO_USAGE, (long) sizeBytes);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("[PVPUtils] No compatible createBuffer found on " + device.getClass(), e);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[PVPUtils] GpuBufferUtil.createUBO failed", e);
        }
    }

    public static void closeQuietly(GpuBuffer buffer) {
        if (buffer == null) return;
        try {
            buffer.close();
        } catch (RuntimeException ignored) {
        }
    }

    public static boolean isClosedBufferException(RuntimeException e) {
        String message = e.getMessage();
        return message != null && message.toLowerCase().contains("closed");
    }
}
