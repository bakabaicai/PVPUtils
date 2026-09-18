package com.pvp_utils.client.modules.impl.Tool.Zoom;

final class ZoomValueAnimator {
    private double previousValue = 1.0;
    private double currentValue = 1.0;
    private double startValue = 1.0;
    private double targetValue = 1.0;
    private double progress = 1.0;

    void tick(double target, double seconds) {
        previousValue = currentValue;

        if (Math.abs(target - targetValue) > 0.0001) {
            startValue = currentValue;
            targetValue = target;
            progress = 0.0;
        }

        if (seconds <= 0.0) {
            currentValue = targetValue;
            previousValue = currentValue;
            progress = 1.0;
            return;
        }

        if (progress < 1.0) {
            progress = Math.min(1.0, progress + 0.05 / seconds);
            currentValue = lerp(easeOutExpo(progress), startValue, targetValue);
        } else {
            currentValue = targetValue;
        }
    }

    double get(float tickDelta) {
        double delta = Math.max(0.0, Math.min(1.0, tickDelta));
        return lerp(delta, previousValue, currentValue);
    }

    boolean isAtRest() {
        return progress >= 1.0 && Math.abs(currentValue - 1.0) <= 0.0001;
    }

    void reset() {
        previousValue = 1.0;
        currentValue = 1.0;
        startValue = 1.0;
        targetValue = 1.0;
        progress = 1.0;
    }

    private static double easeOutExpo(double t) {
        if (t <= 0.0) return 0.0;
        if (t >= 1.0) return 1.0;
        return 1.0 - Math.pow(2.0, -10.0 * t);
    }

    private static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }
}
