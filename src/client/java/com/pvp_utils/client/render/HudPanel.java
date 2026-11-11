package com.pvp_utils.client.render;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.chroma.ChromaGraphics;
import com.pvp_utils.client.render.chroma.ChromaDraw;
import com.pvp_utils.client.render.chroma.effects.ChromaShadow;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.types.RRect;

public final class HudPanel {
    private HudPanel() {
    }

    public static void draw(Canvas canvas, float x, float y, float width, float height,
                            float radius, HudAppearance appearance, float alpha) {
        if (width <= 0f || height <= 0f || alpha <= 0f) return;
        draw(canvas, x, y, width, height, radius, appearance, appearance.background(), alpha);
    }

    public static void draw(Canvas canvas, float x, float y, float width, float height,
                            float radius, HudAppearance appearance, int backgroundColor, float alpha) {
        if (width <= 0f || height <= 0f || alpha <= 0f) return;
        int color = opacity(backgroundColor, alpha);
        if (appearance.shadow) {
            ChromaShadow.drawRounded(canvas, x, y, width, height, radius,
                    appearance.shadowStrength, opacity(0xB0000000, alpha));
        }
        ChromaGraphics.drawBlurredBackdrop(canvas, RRect.makeXYWH(x, y, width, height, radius),
                x, y, width, height, Math.clamp(Config.skiaBlurStrength, 0f, 2f) * 10.5f);
        ChromaDraw.rounded(canvas, x, y, width, height, radius, color);
    }

    public static int opacity(int color, float opacity) {
        return (Math.round((color >>> 24) * Math.clamp(opacity, 0f, 1f)) << 24) | (color & 0xFFFFFF);
    }
}
