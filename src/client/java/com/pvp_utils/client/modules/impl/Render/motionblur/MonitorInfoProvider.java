package com.pvp_utils.client.modules.impl.Render.motionblur;

import net.minecraft.client.Minecraft;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

public final class MonitorInfoProvider {
    private static long lastMonitorHandle = 0;
    private static int lastRefreshRate = 60;
    private static long lastCheckTime = 0;
    private static final long CHECK_INTERVAL_NS = 1_000_000_000L;

    private MonitorInfoProvider() {}

    public static void updateDisplayInfo() {
        long now = System.nanoTime();
        if (now - lastCheckTime < CHECK_INTERVAL_NS) return;
        lastCheckTime = now;

        Minecraft client = Minecraft.getInstance();
        long window = client.getWindow().handle();
        long monitor = GLFW.glfwGetWindowMonitor(window);
        if (monitor == 0) {
            monitor = getMonitorFromWindowPosition(window, client.getWindow().getScreenWidth(), client.getWindow().getScreenHeight());
        }

        if (monitor != lastMonitorHandle) {
            lastRefreshRate = detectRefreshRate(monitor);
            lastMonitorHandle = monitor;
        }
    }

    public static int getRefreshRate() {
        return lastRefreshRate;
    }

    private static long getMonitorFromWindowPosition(long window, int windowWidth, int windowHeight) {
        int[] winX = new int[1];
        int[] winY = new int[1];
        GLFW.glfwGetWindowPos(window, winX, winY);

        int centerX = winX[0] + windowWidth / 2;
        int centerY = winY[0] + windowHeight / 2;
        long result = GLFW.glfwGetPrimaryMonitor();
        PointerBuffer monitors = GLFW.glfwGetMonitors();
        if (monitors != null) {
            for (int i = 0; i < monitors.limit(); i++) {
                long monitor = monitors.get(i);
                int[] mx = new int[1];
                int[] my = new int[1];
                GLFW.glfwGetMonitorPos(monitor, mx, my);
                GLFWVidMode mode = GLFW.glfwGetVideoMode(monitor);
                if (mode == null) continue;
                if (centerX >= mx[0] && centerX < mx[0] + mode.width() && centerY >= my[0] && centerY < my[0] + mode.height()) {
                    result = monitor;
                    break;
                }
            }
        }
        return result;
    }

    private static int detectRefreshRate(long monitor) {
        GLFWVidMode vidMode = GLFW.glfwGetVideoMode(monitor);
        return vidMode != null ? vidMode.refreshRate() : 60;
    }
}
