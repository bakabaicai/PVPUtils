package com.pvp_utils.client.modules.impl.Tool.Zoom;

import com.pvp_utils.Config;
import com.pvp_utils.client.KeyBindings;
import net.minecraft.client.Minecraft;

public final class ZoomManager {
    private static final ZoomValueAnimator ZOOM_ANIMATOR = new ZoomValueAnimator();
    private static int scrollSteps = 0;
    private static boolean zooming = false;
    private static boolean zoomingLastTick = false;
    private static double previousZoomDivisor = 1.0;

    private ZoomManager() {
    }

    public static void tick(Minecraft client) {
        boolean active = Config.zoom && KeyBindings.zoom != null && KeyBindings.zoom.isDown() && client.screen == null;
        zooming = active;

        double target = active ? targetDivisor() : 1.0;
        ZOOM_ANIMATOR.tick(target, active ? Config.zoomInTime : Config.zoomOutTime);

        if (!active && zoomingLastTick && ZOOM_ANIMATOR.isAtRest()) {
            scrollSteps = 0;
        }

        if (!active && ZOOM_ANIMATOR.isAtRest()) {
            scrollSteps = 0;
        }

        zoomingLastTick = active;
    }

    public static boolean isZooming() {
        return zooming;
    }

    public static void scroll(double amount) {
        if (!Config.zoomScroll || !zooming || amount == 0.0) return;
        if (amount > 0.0) {
            scrollSteps++;
        } else {
            scrollSteps--;
        }
        scrollSteps = Math.max(0, Math.min(maxScrollSteps(), scrollSteps));
    }

    public static float getZoomDivisor(float tickDelta) {
        double divisor = ZOOM_ANIMATOR.get(tickDelta);
        previousZoomDivisor = Math.max(1.0, divisor);
        return (float) previousZoomDivisor;
    }

    public static double getMouseSensitivityMultiplier() {
        if (previousZoomDivisor <= 1.0) return 1.0;
        double sensitivity = Math.max(0.0, Math.min(1.0, Config.zoomRelativeSensitivity / 100.0));
        return lerp(sensitivity, 1.0 / previousZoomDivisor, 1.0);
    }

    public static void reset() {
        zooming = false;
        scrollSteps = 0;
        previousZoomDivisor = 1.0;
        zoomingLastTick = false;
        ZOOM_ANIMATOR.reset();
    }

    private static double targetDivisor() {
        double base = Math.max(1, Config.zoomAmount);
        if (!Config.zoomScroll || scrollSteps <= 0) {
            return base;
        }

        double stepMultiplier = Math.max(1.01, Config.zoomPerStep / 100.0);
        return Math.max(1.0, Math.min(500.0, base * Math.pow(stepMultiplier, scrollSteps)));
    }

    private static int maxScrollSteps() {
        return Math.max(1, Config.zoomScrollSteps);
    }

    private static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }
}
