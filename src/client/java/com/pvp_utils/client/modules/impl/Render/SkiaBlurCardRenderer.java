package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaBlurRenderer;
import com.pvp_utils.client.render.skia.SkiaGlBackend;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.DirectContext;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.types.RRect;
import net.minecraft.client.Minecraft;

public class SkiaBlurCardRenderer {
    private static final SkiaBlurCardRenderer INSTANCE = new SkiaBlurCardRenderer();
    private static final float CARD_W = 190f;
    private static final float CARD_H = 74f;
    private static final float RADIUS = 16f;

    private final SkiaGlBackend glBackend = new SkiaGlBackend();
    private final Paint borderPaint = new Paint().setAntiAlias(true).setMode(PaintMode.STROKE).setStrokeWidth(1.1f);

    public static SkiaBlurCardRenderer getInstance() {
        return INSTANCE;
    }

    public void renderFrameEnd() {
        Minecraft client = Minecraft.getInstance();
        if (!Config.skiaBlurCardTest || client.options.hideGui) return;
        if (client.getWindow() == null || client.getMainRenderTarget() == null) return;

        int screenW = client.getWindow().getGuiScaledWidth();
        float x = screenW - CARD_W - 18f;
        float y = 18f;
        int framebufferId = SkiaBlurRenderer.currentDrawFramebufferId();

        Canvas canvas = glBackend.begin(framebufferId);
        DirectContext context = glBackend.getContext();
        if (canvas == null || context == null) {
            glBackend.end();
            return;
        }

        try {
            int tintColor = scrimColorForConfig();
            SkiaBlurRenderer.getInstance().render(canvas, context, client, framebufferId,
                    x, y, CARD_W, CARD_H, RADIUS, tintColor, Config.skiaBlurStrength);

            borderPaint.setColor(borderColorForConfig());
            canvas.drawRRect(RRect.makeXYWH(x + 0.5f, y + 0.5f, CARD_W - 1f, CARD_H - 1f, RADIUS), borderPaint);

            FontRenderer.drawText(canvas, "Skija GPU Blur", x + 16f, y + 27f, 15f, textColorForConfig(0xFF, 0x11));
            FontRenderer.drawText(canvas, "Reusable Gaussian API", x + 16f, y + 49f, 9.5f, textColorForConfig(0xCC, 0x88));
        } finally {
            glBackend.end();
        }
    }

    private int scrimColorForConfig() {
        return argb(0x82, colorRgbForConfig());
    }

    private int borderColorForConfig() {
        return Config.skiaBlurColor == Config.SkiaBlurColor.LIGHT ? argb(0x55, 0x111827) : argb(0x55, 0xFFFFFF);
    }

    private int textColorForConfig(int darkAlpha, int lightAlpha) {
        return Config.skiaBlurColor == Config.SkiaBlurColor.LIGHT ? argb(lightAlpha, 0x111827) : argb(darkAlpha, 0xFFFFFF);
    }

    private int colorRgbForConfig() {
        return switch (Config.skiaBlurColor) {
            case LIGHT -> 0xF8FAFC;
            case BLUE -> 0x1D4ED8;
            case PURPLE -> 0x7C3AED;
            case GREEN -> 0x047857;
            default -> 0x111827;
        };
    }

    private int argb(int alpha, int rgb) {
        return ((alpha & 0xFF) << 24) | (rgb & 0x00FFFFFF);
    }
}
