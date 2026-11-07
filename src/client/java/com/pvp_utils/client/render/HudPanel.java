package com.pvp_utils.client.render;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.skia.LiquidGlassRenderer;
import com.pvp_utils.client.render.skia.SkijaRenderer;
import com.pvp_utils.client.render.skia.SkijaUi;
import com.pvp_utils.client.render.skia.SoftShadow;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.types.RRect;
import net.minecraft.client.Minecraft;

public final class HudPanel {
    private HudPanel() {
    }

    public static void draw(Canvas canvas, float x, float y, float width, float height,
                            float radius, HudAppearance appearance, boolean liquid, float alpha) {
        if (width <= 0f || height <= 0f || alpha <= 0f) return;
        int color = opacity(appearance.background(), alpha);
        if (appearance.shadow) {
            SoftShadow.drawRounded(canvas, x, y, width, height, radius,
                    appearance.shadowStrength, opacity(0xB0000000, alpha));
        }
        if (liquid && LiquidGlassRenderer.getInstance().renderShapes(Minecraft.getInstance(),
                java.util.List.of(new LiquidGlassRenderer.GlassShape(x, y, width, height,
                        radius, radius, radius, radius, 0.3f, Math.clamp(alpha, 0f, 1f),
                        appearance.background())), appearance.glass)) return;
        SkijaRenderer.drawBlurredBackdrop(canvas, RRect.makeXYWH(x, y, width, height, radius),
                x, y, width, height, Math.clamp(Config.skiaBlurStrength, 0f, 2f) * 10.5f);
        SkijaUi.rounded(canvas, x, y, width, height, radius, color);
    }

    public static int opacity(int color, float opacity) {
        return (Math.round((color >>> 24) * Math.clamp(opacity, 0f, 1f)) << 24) | (color & 0xFFFFFF);
    }
}
