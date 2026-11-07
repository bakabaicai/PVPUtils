package com.pvp_utils.client.render.skia;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ClipMode;
import io.github.humbleui.skija.Path;
import io.github.humbleui.types.RRect;

public final class SoftShadow {
    private SoftShadow() {
    }

    public static void drawRounded(Canvas canvas, float x, float y, float width, float height,
                                   float radius, float strength, int color) {
        float amount = amount(strength);
        int shadowColor = shadowColor(color, amount);
        if (canvas == null || width <= 0f || height <= 0f || (shadowColor >>> 24) == 0) return;
        float corner = Math.max(0f, Math.min(radius, Math.min(width, height) * 0.5f));
        int save = canvas.save();
        try {
            canvas.clipRRect(RRect.makeXYWH(x, y, width, height, corner), ClipMode.DIFFERENCE, true);
            SkijaUi.dropShadowRounded(canvas, x, y, width, height, corner, 0f, blur(amount), shadowColor);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    public static void drawPath(Canvas canvas, Path path, float strength, int color) {
        float amount = amount(strength);
        int shadowColor = shadowColor(color, amount);
        if (canvas == null || path == null || path.isEmpty() || (shadowColor >>> 24) == 0) return;
        int save = canvas.save();
        try {
            canvas.clipPath(path, ClipMode.DIFFERENCE, true);
            SkijaUi.dropShadowPath(canvas, path, blur(amount), shadowColor);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private static float amount(float strength) {
        return Float.isFinite(strength) ? Math.clamp(strength, 0f, 1f) : 0f;
    }

    private static float blur(float strength) {
        return 8f + 12f * strength;
    }

    private static int shadowColor(int color, float strength) {
        int alpha = Math.round((color >>> 24) * strength);
        return alpha << 24 | color & 0x00FFFFFF;
    }
}
