package com.pvp_utils.client.gui;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaRenderer;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PathEffect;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.types.RRect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public class HudEditOverlay {

    private static final HudEditOverlay INSTANCE = new HudEditOverlay();
    public static HudEditOverlay getInstance() { return INSTANCE; }

    private boolean dragging = false;
    private boolean wasMouseDown = false;
    private float dragOffsetX, dragOffsetY;

    private float dashOffset = 0f;
    private float hoverAlpha = 0f;

    private float visualX = Float.NaN;
    private float visualY = Float.NaN;

    private long lastFrameMs = 0;

    private static final int HUD_WIDTH  = 164;
    private static final int HUD_HEIGHT = 44;
    private static final float DASH_SPEED = 20f;
    private static final float DASH_LEN = 12f;
    private static final float DASH_GAP = 6f;

    public void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        int guiW = mc.getWindow().getGuiScaledWidth();
        int guiH = mc.getWindow().getGuiScaledHeight();
        long windowHandle = mc.getWindow().handle();

        long now = System.currentTimeMillis();
        float dt = lastFrameMs == 0 ? 0.016f : Math.min((now - lastFrameMs) / 1000f, 0.05f);
        lastFrameMs = now;

        dashOffset = (dashOffset + DASH_SPEED * dt) % (DASH_LEN + DASH_GAP);

        float guiScale = (float) mc.getWindow().getGuiScale();
        double[] rawX = new double[1], rawY = new double[1];
        GLFW.glfwGetCursorPos(windowHandle, rawX, rawY);
        float mx = (float)(rawX[0] / guiScale);
        float my = (float)(rawY[0] / guiScale);

        boolean mouseDown = GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        float targetX = guiW * 0.5f + Config.targetHudX;
        float targetY = guiH * 0.5f + Config.targetHudY;
        targetX = Math.max(0, Math.min(targetX, guiW - HUD_WIDTH));
        targetY = Math.max(0, Math.min(targetY, guiH - HUD_HEIGHT));

        if (Float.isNaN(visualX)) { visualX = targetX; visualY = targetY; }

        float pad = 4f;
        boolean hovered = !dragging
                && mx >= targetX - pad && mx <= targetX + HUD_WIDTH + pad
                && my >= targetY - pad && my <= targetY + HUD_HEIGHT + pad;

        hoverAlpha += ((hovered || dragging ? 1f : 0f) - hoverAlpha) * Math.min(1f, dt * 14f);

        if (mouseDown && !wasMouseDown && hovered) {
            dragging = true;
            dragOffsetX = mx - targetX;
            dragOffsetY = my - targetY;
        }
        if (!mouseDown) dragging = false;
        wasMouseDown = mouseDown;

        if (dragging) {
            float newX = mx - dragOffsetX;
            float newY = my - dragOffsetY;
            newX = Math.max(0, Math.min(newX, guiW - HUD_WIDTH));
            newY = Math.max(0, Math.min(newY, guiH - HUD_HEIGHT));
            Config.targetHudX = newX - guiW * 0.5f;
            Config.targetHudY = newY - guiH * 0.5f;
            Config.save();
            targetX = newX;
            targetY = newY;
        }

        float lerpSpeed = dragging ? 22f : 16f;
        visualX += (targetX - visualX) * Math.min(1f, dt * lerpSpeed);
        visualY += (targetY - visualY) * Math.min(1f, dt * lerpSpeed);

        Canvas canvas = SkiaRenderer.begin();
        if (canvas == null) return;

        drawGrid(canvas, guiW, guiH);
        drawHudOutline(canvas, visualX, visualY);

        SkiaRenderer.end(graphics, guiW, guiH);
    }

    private void drawGrid(Canvas canvas, int guiW, int guiH) {
        try (Paint p = new Paint()) {
            p.setColor(0x44FFFFFF);
            p.setAntiAlias(true);
            p.setPathEffect(PathEffect.makeDash(new float[]{8f, 8f}, 0f));
            p.setStrokeWidth(1f);
            p.setMode(PaintMode.STROKE);

            canvas.drawLine(guiW / 3f, 0, guiW / 3f, guiH, p);
            canvas.drawLine(guiW * 2f / 3f, 0, guiW * 2f / 3f, guiH, p);
            canvas.drawLine(0, guiH / 3f, guiW, guiH / 3f, p);
            canvas.drawLine(0, guiH * 2f / 3f, guiW, guiH * 2f / 3f, p);
        }
    }

    private void drawHudOutline(Canvas canvas, float x, float y) {
        float pad = 4f;
        float rx = x - pad, ry = y - pad;
        float rw = HUD_WIDTH + pad * 2f, rh = HUD_HEIGHT + pad * 2f;

        float a = 0.55f + hoverAlpha * 0.45f;
        int alpha = (int)(a * 255);

        if (hoverAlpha > 0.01f) {
            try (Paint fill = new Paint()) {
                fill.setColor((int)(hoverAlpha * 30) << 24 | 0xFFFFFF);
                fill.setAntiAlias(true);
                canvas.drawRRect(RRect.makeXYWH(rx, ry, rw, rh, 6f), fill);
            }
        }

        try (Paint p = new Paint()) {
            p.setColor((alpha << 24) | 0xFFFFFF);
            p.setAntiAlias(true);
            p.setPathEffect(PathEffect.makeDash(new float[]{DASH_LEN, DASH_GAP}, dashOffset));
            p.setStrokeWidth(1.5f);
            p.setMode(PaintMode.STROKE);
            canvas.drawRRect(RRect.makeXYWH(rx, ry, rw, rh, 6f), p);
        }

        FontRenderer.drawText(canvas, "Target HUD", rx, ry - 4f, 10f, (alpha << 24) | 0xFFFFFF);
    }
}
