package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;

public final class GammaOverrideManager {
    private static final float SNAP_EPSILON = 0.001f;
    private static final float GAMMA_PER_SECOND = 2f;
    private static float currentGamma = Float.NaN;
    private static long lastUpdateNanos = 0L;

    private GammaOverrideManager() {
    }

    public static float apply(float vanillaGamma) {
        float targetGamma = Config.gammaOverride && Double.isFinite(Config.gammaValue)
                ? Math.max(0f, (float) Config.gammaValue) : vanillaGamma;
        if (Float.isNaN(currentGamma)) {
            currentGamma = vanillaGamma;
            lastUpdateNanos = System.nanoTime();
        }

        long now = System.nanoTime();
        float deltaSeconds = lastUpdateNanos == 0L ? 0.0f : (now - lastUpdateNanos) / 1_000_000_000.0f;
        lastUpdateNanos = now;

        float maximumStep = GAMMA_PER_SECOND * clamp(deltaSeconds, 0f, 0.1f);
        float difference = targetGamma - currentGamma;
        currentGamma += Math.copySign(Math.min(Math.abs(difference), maximumStep), difference);

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
