package com.pvp_utils.client.modules.impl.Render.motionblur;

public final class BlurStrengthCalculator {
    public record Result(float strength, int sampleAmount) {}

    public Result calculate(float baseStrength, float fps, int refreshRate, boolean scalingEnabled) {
        if (!scalingEnabled) return new Result(baseStrength, 100);

        float fpsOverRefresh = refreshRate > 0 ? fps / refreshRate : 1.0f;
        if (fpsOverRefresh < 1.0f) fpsOverRefresh = 1.0f;
        return new Result(baseStrength * fpsOverRefresh, fpsOverRefresh > 1.0f ? (int) (100 * fpsOverRefresh) : 100);
    }
}
