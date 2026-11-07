package com.pvp_utils.client.render.skia;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.PaintStrokeCap;
import io.github.humbleui.skija.PaintStrokeJoin;
import io.github.humbleui.skija.Path;
import io.github.humbleui.skija.PathBuilder;

public final class AnimatedStatusIcon {
    private AnimatedStatusIcon() {
    }

    public static void draw(Canvas canvas, boolean success, float x, float y, float size, float progress, int color) {
        if (canvas == null || !Float.isFinite(size) || size <= 0f || !Float.isFinite(progress)
                || progress <= 0f || (color >>> 24) == 0) return;
        float fraction = Math.clamp(progress, 0f, 1f);
        try (PathBuilder builder = new PathBuilder()) {
            if (success) {
                float firstLength = (float) Math.hypot(0.24, 0.22);
                float secondLength = (float) Math.hypot(0.43, 0.5);
                float distance = fraction * (firstLength + secondLength);
                float first = Math.min(1f, distance / firstLength);
                builder.moveTo(x + size * 0.17f, y + size * 0.52f)
                        .lineTo(x + size * (0.17f + 0.24f * first), y + size * (0.52f + 0.22f * first));
                if (distance > firstLength) {
                    float second = (distance - firstLength) / secondLength;
                    builder.lineTo(x + size * (0.41f + 0.43f * second), y + size * (0.74f - 0.5f * second));
                }
            } else {
                float first = Math.min(1f, fraction / 0.62f);
                builder.moveTo(x + size * 0.25f, y + size * 0.25f)
                        .lineTo(x + size * (0.25f + 0.5f * first), y + size * (0.25f + 0.5f * first));
                if (fraction > 0.34f) {
                    float second = Math.min(1f, (fraction - 0.34f) / 0.66f);
                    builder.moveTo(x + size * 0.75f, y + size * 0.25f)
                            .lineTo(x + size * (0.75f - 0.5f * second), y + size * (0.25f + 0.5f * second));
                }
            }
            try (Path path = builder.detach()) {
                SkijaUi.strokePath(canvas, path, Math.max(1.4f, size * 0.085f), color,
                        PaintStrokeCap.ROUND, PaintStrokeJoin.ROUND);
            }
        }
    }
}
