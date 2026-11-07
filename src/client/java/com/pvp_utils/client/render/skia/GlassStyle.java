package com.pvp_utils.client.render.skia;

public final class GlassStyle {
    public float blur = 6f;
    public float refraction = 1.35f;
    public float dispersion = 1f;
    public float rim = 0.6f;
    public float alpha = 0.9f;
    public float glow = 0.4f;
    public float thickness = 10f;

    public void sanitize() {
        blur = finite(blur, 6f, 0f, 30f);
        refraction = finite(refraction, 1.35f, 1f, 5f);
        dispersion = finite(dispersion, 1f, 0f, 3f);
        rim = finite(rim, 0.6f, 0f, 1f);
        alpha = finite(alpha, 0.9f, 0f, 1f);
        glow = finite(glow, 0.4f, 0f, 1f);
        thickness = finite(thickness, 10f, 1f, 120f);
    }

    private static float finite(float value, float fallback, float min, float max) {
        return Float.isFinite(value) ? Math.clamp(value, min, max) : fallback;
    }
}
