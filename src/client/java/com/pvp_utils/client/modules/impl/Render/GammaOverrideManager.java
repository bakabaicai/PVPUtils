package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;

public final class GammaOverrideManager {
    private static final float SNAP_EPSILON = 0.01f;
    private static float currentGamma = Float.NaN;
    private static long lastUpdateNanos = 0L;

    private GammaOverrideManager() {
    }

    public static float apply(float vanillaGamma) {
        float targetGamma = Config.gammaOverride ? (float) Config.gammaValue : vanillaGamma;
        if (Float.isNaN(currentGamma)) {
            currentGamma = vanillaGamma;
            lastUpdateNanos = System.nanoTime();
        }

        long now = System.nanoTime();
        float deltaSeconds = lastUpdateNanos == 0L ? 0.0f : (now - lastUpdateNanos) / 1_000_000_000.0f;
        lastUpdateNanos = now;

        float alpha = 1.0f - (float) Math.exp(-7.5f * Math.max(0.0f, deltaSeconds));
        alpha = clamp(alpha, 0.02f, 0.35f);
        currentGamma += (targetGamma - currentGamma) * alpha;

        if (Math.abs(targetGamma - currentGamma) <= SNAP_EPSILON) {
            currentGamma = targetGamma;
        }

        if (!Config.gammaOverride && currentGamma == vanillaGamma) {
            return vanillaGamma;
        }
        return currentGamma;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
