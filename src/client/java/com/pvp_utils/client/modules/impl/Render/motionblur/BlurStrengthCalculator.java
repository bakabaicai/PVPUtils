package com.pvp_utils.client.modules.impl.Render.motionblur;

public final class BlurStrengthCalculator {
    private static final int BASE_SAMPLE_AMOUNT = 240;
    private static final int MAX_SAMPLE_AMOUNT = 4096;

    public record Result(float strength, int sampleAmount) {}

    public Result calculate(float baseStrength, float fps, int refreshRate, boolean scalingEnabled) {
        if (!scalingEnabled) return new Result(baseStrength, BASE_SAMPLE_AMOUNT);

        float fpsOverRefresh = refreshRate > 0 ? fps / refreshRate : 1.0f;
        if (fpsOverRefresh < 1.0f) fpsOverRefresh = 1.0f;
        int sampleAmount = Math.min(MAX_SAMPLE_AMOUNT, Math.max(BASE_SAMPLE_AMOUNT, (int) (BASE_SAMPLE_AMOUNT * fpsOverRefresh)));
        return new Result(baseStrength * fpsOverRefresh, sampleAmount);
    }
}
