package com.pvp_utils.client.render.chroma.effects;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.ImageFilter;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.Rect;

public final class ChromaTransition {
    private ChromaTransition() {
    }

    public static void draw(Canvas canvas, Rect bounds, float visibility, float offsetY, Runnable drawing) {
        if (canvas == null || bounds == null || drawing == null || !Float.isFinite(visibility)
                || !Float.isFinite(offsetY) || bounds.getWidth() <= 0f || bounds.getHeight() <= 0f) return;
        float visible = Math.clamp(visibility, 0f, 1f);
        if (visible <= 0f) return;
        if (visible >= 0.999f && Math.abs(offsetY) < 0.01f) {
            drawing.run();
            return;
        }
        float spread = 1f - visible;
        float sigma = spread * 2.6f;
        int alpha = Math.round(255f * visible);
        int save = canvas.save();
        try (ImageFilter filter = ImageFilter.makeBlur(sigma, sigma, FilterTileMode.DECAL);
             Paint paint = new Paint().setImageFilter(filter).setAlpha(alpha)) {
            canvas.clipRect(bounds);
            int layer = canvas.saveLayer(bounds, paint);
            try {
                canvas.translate(0f, offsetY);
                drawing.run();
            } finally {
                canvas.restoreToCount(layer);
            }
        } finally {
            canvas.restoreToCount(save);
        }
    }
}
