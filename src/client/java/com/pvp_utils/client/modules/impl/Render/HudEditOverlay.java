package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.font.FontRenderer;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.PathEffect;
import io.github.humbleui.types.RRect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public class HudEditOverlay {

    private enum DragTarget { NONE, TARGET_HUD, KEYSTROKES, NOTIFICATION }

    private static final HudEditOverlay INSTANCE = new HudEditOverlay();
    private static final int TARGET_HUD_WIDTH = 164;
    private static final int TARGET_HUD_HEIGHT = 44;
    private static final float DASH_SPEED = 20f;
    private static final float DASH_LEN = 12f;
    private static final float DASH_GAP = 6f;
    private static final float DASH_PERIOD = DASH_LEN + DASH_GAP;
    private static final float ANIM_DURATION = 0.2f;
    private static final float SNAP_THRESHOLD = 10f;

    private DragTarget dragTarget = DragTarget.NONE;
    private boolean wasMouseDown = false;
    private float dragOffsetX;
    private float dragOffsetY;
    private float targetHoverAlpha = 0f;
    private float keystrokesHoverAlpha = 0f;
    private float notificationHoverAlpha = 0f;
    private float dashOffset = 0f;
    private float targetVisualX = Float.NaN;
    private float targetVisualY = Float.NaN;
    private long lastFrameMs = 0;
    private long animStartTime = 0;
    private long animCloseTime = 0;
    private boolean closing = false;
    private boolean active = false;
    private float snapXLine = -1f;
    private float snapYLine = -1f;
    private float gridAlpha = 0f;
    private float snapXAlpha = 0f;
    private float snapYAlpha = 0f;
    private boolean configDirty = false;

    public static HudEditOverlay getInstance() {
        return INSTANCE;
    }

    public boolean isActive() {
        return active;
    }

    public void startOpen() {
        animStartTime = System.currentTimeMillis();
        closing = false;
        active = true;
        NotificationOverlay.getInstance().startEditPreview();
    }

    public void startClose() {
        animCloseTime = System.currentTimeMillis();
        closing = true;
        active = true;
        NotificationOverlay.getInstance().stopEditPreview();
    }

    public void render(GuiGraphics graphics) {
        render(graphics, null);
    }

    public void render(GuiGraphics graphics, Canvas canvas) {
        if (!active) return;

        Minecraft mc = Minecraft.getInstance();
        int guiW = mc.getWindow().getGuiScaledWidth();
        int guiH = mc.getWindow().getGuiScaledHeight();
        long windowHandle = mc.getWindow().handle();

        long now = System.currentTimeMillis();
        float dt = lastFrameMs == 0 ? 0.016f : Math.min((now - lastFrameMs) / 1000f, 0.05f);
        lastFrameMs = now;

        dashOffset -= DASH_SPEED * dt;
        if (dashOffset < -DASH_PERIOD) dashOffset += DASH_PERIOD;

        float guiScale = (float) mc.getWindow().getGuiScale();
        double[] rawX = new double[1];
        double[] rawY = new double[1];
        GLFW.glfwGetCursorPos(windowHandle, rawX, rawY);
        float mx = (float) (rawX[0] / guiScale);
        float my = (float) (rawY[0] / guiScale);
        boolean mouseDown = GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        RectState targetHud = Config.targetHud ? getTargetHudRect(guiW, guiH) : null;
        RectState keystrokes = Config.keystrokes ? getKeystrokesRect(guiW, guiH) : null;
        RectState notification = getNotificationRect(guiW, guiH);

        if (targetHud != null && Float.isNaN(targetVisualX)) {
            targetVisualX = targetHud.x;
            targetVisualY = targetHud.y;
        }

        boolean targetHovered = targetHud != null && contains(targetHud, mx, my, 4f);
        boolean keystrokesHovered = keystrokes != null && contains(keystrokes, mx, my, 4f);
        boolean notificationHovered = contains(notification, mx, my, 4f);

        if (mouseDown && !wasMouseDown) {
            if (notificationHovered) {
                dragTarget = DragTarget.NOTIFICATION;
                dragOffsetX = mx - notification.x;
                dragOffsetY = my - notification.y;
            } else if (keystrokesHovered) {
                dragTarget = DragTarget.KEYSTROKES;
                dragOffsetX = mx - keystrokes.x;
                dragOffsetY = my - keystrokes.y;
            } else if (targetHovered && targetHud != null) {
                dragTarget = DragTarget.TARGET_HUD;
                dragOffsetX = mx - targetHud.x;
                dragOffsetY = my - targetHud.y;
            }
        }
        if (!mouseDown) {
            if (configDirty) {
                Config.save();
                configDirty = false;
            }
            dragTarget = DragTarget.NONE;
        }
        wasMouseDown = mouseDown;

        if (dragTarget == DragTarget.TARGET_HUD && targetHud != null) {
            RectState dragged = snapRect(clampRect(mx - dragOffsetX, my - dragOffsetY, targetHud.w, targetHud.h, guiW, guiH), guiW, guiH);
            Config.targetHudX = dragged.x - guiW * 0.5f;
            Config.targetHudY = dragged.y - guiH * 0.5f;
            configDirty = true;
            targetHud = dragged;
        } else if (dragTarget == DragTarget.KEYSTROKES && keystrokes != null) {
            RectState dragged = snapRect(clampRect(mx - dragOffsetX, my - dragOffsetY, keystrokes.w, keystrokes.h, guiW, guiH), guiW, guiH);
            Config.keystrokesX = dragged.x - guiW * 0.5f;
            Config.keystrokesY = dragged.y - guiH * 0.5f;
            configDirty = true;
            keystrokes = dragged;
        } else if (dragTarget == DragTarget.NOTIFICATION) {
            RectState dragged = snapRect(clampRect(mx - dragOffsetX, my - dragOffsetY, notification.w, notification.h, guiW, guiH), guiW, guiH);
            Config.notificationX = dragged.x + dragged.w - guiW * 0.5f;
            Config.notificationY = dragged.y - guiH * 0.5f;
            configDirty = true;
            notification = dragged;
        }

        if (targetHud != null) {
            float lerpSpeed = dragTarget == DragTarget.TARGET_HUD ? 22f : 16f;
            targetVisualX += (targetHud.x - targetVisualX) * Math.min(1f, dt * lerpSpeed);
            targetVisualY += (targetHud.y - targetVisualY) * Math.min(1f, dt * lerpSpeed);
        } else {
            targetVisualX = Float.NaN;
            targetVisualY = Float.NaN;
        }

        targetHoverAlpha += (((targetHovered || dragTarget == DragTarget.TARGET_HUD) ? 1f : 0f) - targetHoverAlpha) * Math.min(1f, dt * 14f);
        keystrokesHoverAlpha += (((keystrokesHovered || dragTarget == DragTarget.KEYSTROKES) ? 1f : 0f) - keystrokesHoverAlpha) * Math.min(1f, dt * 14f);
        notificationHoverAlpha += (((notificationHovered || dragTarget == DragTarget.NOTIFICATION) ? 1f : 0f) - notificationHoverAlpha) * Math.min(1f, dt * 14f);
        gridAlpha += (((dragTarget != DragTarget.NONE) ? 1f : 0f) - gridAlpha) * Math.min(1f, dt * 12f);
        snapXAlpha += (((snapXLine >= 0f && dragTarget != DragTarget.NONE) ? 1f : 0f) - snapXAlpha) * Math.min(1f, dt * 18f);
        snapYAlpha += (((snapYLine >= 0f && dragTarget != DragTarget.NONE) ? 1f : 0f) - snapYAlpha) * Math.min(1f, dt * 18f);

        float progress;
        if (closing) {
            progress = 1f - Math.min(1f, (now - animCloseTime) / (ANIM_DURATION * 1000f));
            if (progress <= 0f) {
                active = false;
                return;
            }
        } else {
            progress = Math.min(1f, (now - animStartTime) / (ANIM_DURATION * 1000f));
        }

        if (canvas == null) return;

        if (gridAlpha > 0.01f) {
            drawGrid(canvas, guiW, guiH, progress);
        }
        if (targetHud != null) {
            drawOutline(canvas, targetVisualX, targetVisualY, TARGET_HUD_WIDTH, TARGET_HUD_HEIGHT, "Target HUD", targetHoverAlpha, progress);
        }
        if (keystrokes != null) {
            drawOutline(canvas, keystrokes.x, keystrokes.y, keystrokes.w, keystrokes.h, "Keystrokes", keystrokesHoverAlpha, progress);
        }
        drawOutline(canvas, notification.x, notification.y, notification.w, notification.h, "Notification", notificationHoverAlpha, progress);
    }

    private void drawGrid(Canvas canvas, int guiW, int guiH, float alpha) {
        float[] xs = {guiW / 3f, guiW * 0.5f, guiW * 2f / 3f};
        float[] ys = {guiH / 3f, guiH * 0.5f, guiH * 2f / 3f};

        try (Paint p = new Paint()) {
            int a = (int) (0x44 * alpha * gridAlpha);
            p.setColor((a << 24) | 0xFFFFFF);
            p.setAntiAlias(true);
            p.setPathEffect(PathEffect.makeDash(new float[]{10f, 10f}, 0f));
            p.setStrokeWidth(1f);
            p.setMode(PaintMode.STROKE);
            for (float x : xs) canvas.drawLine(x, 0, x, guiH, p);
            for (float y : ys) canvas.drawLine(0, y, guiW, y, p);
        }

        try (Paint p = new Paint()) {
            p.setColor((int) (alpha * snapXAlpha * 255) << 24 | 0x7AA2FF);
            p.setAntiAlias(true);
            p.setStrokeWidth(1.5f);
            p.setMode(PaintMode.STROKE);
            if (snapXLine >= 0f) canvas.drawLine(snapXLine, 0, snapXLine, guiH, p);
        }

        try (Paint p = new Paint()) {
            p.setColor((int) (alpha * snapYAlpha * 255) << 24 | 0x7AA2FF);
            p.setAntiAlias(true);
            p.setStrokeWidth(1.5f);
            p.setMode(PaintMode.STROKE);
            if (snapYLine >= 0f) canvas.drawLine(0, snapYLine, guiW, snapYLine, p);
        }
    }

    private void drawOutline(Canvas canvas, float x, float y, float w, float h, String label, float hoverAlpha, float alpha) {
        float pad = 4f;
        float rw = w + pad * 2f;
        float rh = h + pad * 2f;
        float a = (0.55f + hoverAlpha * 0.45f) * alpha;
        int alphaInt = (int) (a * 255);
        float drawX = Math.round(x) + 0.5f;
        float drawY = Math.round(y) + 0.5f;

        if (alpha > 0.01f) {
            try (Paint fill = new Paint()) {
                fill.setColor((int) (alpha * 30) << 24 | 0xFFFFFF);
                fill.setAntiAlias(true);
                canvas.drawRRect(RRect.makeXYWH(drawX - pad, drawY - pad, rw, rh, 6f), fill);
            }
        }

        try (Paint p = new Paint()) {
            p.setColor((alphaInt << 24) | 0xFFFFFF);
            p.setAntiAlias(true);
            p.setPathEffect(PathEffect.makeDash(new float[]{DASH_LEN, DASH_GAP}, dashOffset));
            p.setStrokeWidth(1.5f);
            p.setMode(PaintMode.STROKE);
            canvas.drawRRect(RRect.makeXYWH(drawX - pad, drawY - pad, rw, rh, 6f), p);
        }

        FontRenderer.drawText(canvas, label, drawX - pad, drawY - pad - 4f, 10f, (alphaInt << 24) | 0xFFFFFF);
    }

    private RectState getTargetHudRect(int guiW, int guiH) {
        float x = guiW * 0.5f + Config.targetHudX;
        float y = guiH * 0.5f + Config.targetHudY;
        return clampRect(x, y, TARGET_HUD_WIDTH, TARGET_HUD_HEIGHT, guiW, guiH);
    }

    private RectState getKeystrokesRect(int guiW, int guiH) {
        KeystrokesRenderer renderer = KeystrokesRenderer.getInstance();
        return new RectState(renderer.getRenderX(guiW), renderer.getRenderY(guiH), renderer.getScaledWidth(), renderer.getScaledHeight());
    }

    private RectState getNotificationRect(int guiW, int guiH) {
        NotificationOverlay renderer = NotificationOverlay.getInstance();
        int w = renderer.getEditWidth();
        int h = renderer.getEditHeight();
        return clampRect(renderer.getRenderX(guiW, w), renderer.getRenderY(guiH), w, h, guiW, guiH);
    }

    private RectState clampRect(float x, float y, float w, float h, int guiW, int guiH) {
        float clampedX = Math.max(0, Math.min(x, guiW - w));
        float clampedY = Math.max(0, Math.min(y, guiH - h));
        return new RectState(clampedX, clampedY, w, h);
    }

    private RectState snapRect(RectState rect, int guiW, int guiH) {
        snapXLine = -1f;
        snapYLine = -1f;
        float x = rect.x;
        float y = rect.y;

        float[] xLines = {guiW / 3f, guiW * 0.5f, guiW * 2f / 3f};
        float[] yLines = {guiH / 3f, guiH * 0.5f, guiH * 2f / 3f};

        for (float line : xLines) {
            if (Math.abs(x - line) <= SNAP_THRESHOLD) {
                x = line;
                snapXLine = line;
                break;
            }
            if (Math.abs(x + rect.w - line) <= SNAP_THRESHOLD) {
                x = line - rect.w;
                snapXLine = line;
                break;
            }
        }
        for (float line : yLines) {
            if (Math.abs(y - line) <= SNAP_THRESHOLD) {
                y = line;
                snapYLine = line;
                break;
            }
            if (Math.abs(y + rect.h - line) <= SNAP_THRESHOLD) {
                y = line - rect.h;
                snapYLine = line;
                break;
            }
        }

        return clampRect(x, y, rect.w, rect.h, guiW, guiH);
    }

    private boolean contains(RectState rect, float mx, float my, float pad) {
        return mx >= rect.x - pad && mx <= rect.x + rect.w + pad && my >= rect.y - pad && my <= rect.y + rect.h + pad;
    }

    private record RectState(float x, float y, float w, float h) {}
}
