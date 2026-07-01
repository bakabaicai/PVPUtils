package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.BlurBackgroundRenderer;
import com.pvp_utils.client.render.skia.SkiaBlurBackgroundRenderer;
import com.pvp_utils.client.render.skia.SkiaGlBackend;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.impl.Library;
import io.github.humbleui.types.RRect;
import net.minecraft.client.Minecraft;

import static org.lwjgl.opengl.GL45.*;

public class SkiaBlurCardRenderer {
    private static final SkiaBlurCardRenderer INSTANCE = new SkiaBlurCardRenderer();
    private static final float CARD_W = 190f;
    private static final float CARD_H = 74f;
    private static final float RADIUS = 16f;

    private final SkiaGlBackend glBackend = new SkiaGlBackend();
    private final Paint borderPaint = new Paint().setAntiAlias(true).setMode(PaintMode.STROKE).setStrokeWidth(1.1f);
    private final BlurBackgroundRenderer blurBackgroundRenderer = SkiaBlurBackgroundRenderer.getInstance();
    private boolean nativeLoaded = false;

    public static SkiaBlurCardRenderer getInstance() {
        return INSTANCE;
    }

    public void renderFrameEnd() {
        Minecraft client = Minecraft.getInstance();
        if (!Config.skiaBlurCardTest || client.options.hideGui) return;
        if (client.getWindow() == null || client.getMainRenderTarget() == null) return;

        ensureNativeLoaded();

        float scale = (float) client.getWindow().getGuiScale();
        int screenW = client.getWindow().getGuiScaledWidth();
        float x = screenW - CARD_W - 18f;
        float y = 18f;
        int targetFramebufferId = currentDrawFramebufferId();
        Canvas canvas = glBackend.begin(targetFramebufferId);
        if (canvas == null || glBackend.getContext() == null) {
            return;
        }

        try {
            blurBackgroundRenderer.render(
                    glBackend.getContext(),
                    canvas,
                    targetFramebufferId,
                    x,
                    y,
                    CARD_W,
                    CARD_H,
                    RADIUS,
                    BlurBackgroundRenderer.BlurStyle.STANDARD,
                    0x8A111827,
                    1.0f
            );

            borderPaint.setColor(0x55FFFFFF);
            canvas.drawRRect(RRect.makeXYWH(x + 0.5f, y + 0.5f, CARD_W - 1f, CARD_H - 1f, RADIUS), borderPaint);

            FontRenderer.drawText(canvas, "Skija GPU Blur", x + 16f, y + 27f, 15f, 0xFFFFFFFF);
            FontRenderer.drawText(canvas, "Framebuffer -> GL texture -> Skia", x + 16f, y + 49f, 9.5f, 0xCCFFFFFF);
        } finally {
            glBackend.end();
        }
    }

    private int currentDrawFramebufferId() {
        int[] framebuffer = new int[1];
        glGetIntegerv(GL_DRAW_FRAMEBUFFER_BINDING, framebuffer);
        return framebuffer[0];
    }

    private void ensureNativeLoaded() {
        if (nativeLoaded) return;
        Library.load();
        nativeLoaded = true;
    }

}
