package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Tool.BlockCountDisplayRenderer;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaGlBackend;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.PathEffect;
import io.github.humbleui.skija.impl.Library;
import io.github.humbleui.types.RRect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public class HudEditOverlay {

    private enum DragTarget { NONE, TARGET_HUD, KEYSTROKES, BLOCK_COUNT, NOTIFICATION, POTION_STATUS }

    private static final HudEditOverlay INSTANCE = new HudEditOverlay();
    private static final int TARGET_HUD_WIDTH = 164;
    private static final int TARGET_HUD_HEIGHT = 44;
    private static final int TARGET_HUD_NEW_WIDTH = 190;
    private static final int TARGET_HUD_NEW_HEIGHT = 58;
    private static final float DASH_SPEED = 20f;
    private static final float DASH_LEN = 12f;
    private static final float DASH_GAP = 6f;
    private static final float DASH_PERIOD = DASH_LEN + DASH_GAP;
    private static final float ANIM_DURATION = 0.2f;
    private static final float SNAP_THRESHOLD = 10f;
    private static final float HINT_TEXT_SIZE = 11f;

    private DragTarget dragTarget = DragTarget.NONE;
    private boolean wasMouseDown = false;
    private float dragOffsetX;
    private float dragOffsetY;
    private float targetHoverAlpha = 0f;
    private float keystrokesHoverAlpha = 0f;
    private float blockCountHoverAlpha = 0f;
    private float notificationHoverAlpha = 0f;
    private float potionStatusHoverAlpha = 0f;
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
    private final SkiaGlBackend glBackend = new SkiaGlBackend();
    private boolean nativeLoaded = false;
    private boolean pendingFrame = false;
    private int pendingGuiW = 0;
    private int pendingGuiH = 0;
    private float pendingProgress = 0f;
    private RectState pendingTargetHud = null;
    private RectState pendingKeystrokes = null;
    private RectState pendingBlockCount = null;
    private RectState pendingNotification = null;
    private RectState pendingPotionStatus = null;

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
        RectState blockCount = Config.blockCountDisplay ? getBlockCountRect(guiW, guiH) : null;
        RectState notification = getNotificationRect(guiW, guiH);
        RectState potionStatus = Config.potionStatus ? getPotionStatusRect(guiW, guiH) : null;

        if (targetHud != null && Float.isNaN(targetVisualX)) {
            targetVisualX = targetHud.x;
            targetVisualY = targetHud.y;
        }

        boolean targetHovered = targetHud != null && contains(targetHud, mx, my, 4f);
        boolean keystrokesHovered = keystrokes != null && contains(keystrokes, mx, my, 4f);
        boolean blockCountHovered = blockCount != null && contains(blockCount, mx, my, 4f);
        boolean notificationHovered = contains(notification, mx, my, 4f);
        boolean potionStatusHovered = potionStatus != null && contains(potionStatus, mx, my, 4f);

        if (mouseDown && !wasMouseDown) {
            if (potionStatusHovered) {
                dragTarget = DragTarget.POTION_STATUS;
                dragOffsetX = mx - potionStatus.x;
                dragOffsetY = my - potionStatus.y;
            } else if (notificationHovered) {
                dragTarget = DragTarget.NOTIFICATION;
                dragOffsetX = mx - notification.x;
                dragOffsetY = my - notification.y;
            } else if (blockCountHovered) {
                dragTarget = DragTarget.BLOCK_COUNT;
                dragOffsetX = mx - blockCount.x;
                dragOffsetY = my - blockCount.y;
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
        } else if (dragTarget == DragTarget.BLOCK_COUNT && blockCount != null) {
            BlockCountDisplayRenderer renderer = BlockCountDisplayRenderer.getInstance();
            RectState dragged = snapRect(clampRect(mx - dragOffsetX, my - dragOffsetY, blockCount.w, blockCount.h, guiW, guiH), guiW, guiH);
            Config.blockCountDisplayX = dragged.x - (guiW - renderer.getEditWidth()) * 0.5f;
            Config.blockCountDisplayY = dragged.y - renderer.getDefaultY(guiH);
            configDirty = true;
            blockCount = dragged;
        } else if (dragTarget == DragTarget.NOTIFICATION) {
            RectState dragged = snapRect(clampRect(mx - dragOffsetX, my - dragOffsetY, notification.w, notification.h, guiW, guiH), guiW, guiH);
            Config.notificationX = dragged.x + dragged.w - guiW * 0.5f;
            Config.notificationY = dragged.y - guiH * 0.5f;
            configDirty = true;
            notification = dragged;
        } else if (dragTarget == DragTarget.POTION_STATUS && potionStatus != null) {
            PotionStatusRenderer renderer = PotionStatusRenderer.getInstance();
            RectState dragged = snapRect(clampRect(mx - dragOffsetX, my - dragOffsetY, potionStatus.w, potionStatus.h, guiW, guiH), guiW, guiH);
            Config.potionStatusX = dragged.x - renderer.getDefaultX();
            Config.potionStatusY = dragged.y - (guiH - dragged.h) * 0.5f;
            configDirty = true;
            potionStatus = dragged;
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
        blockCountHoverAlpha += (((blockCountHovered || dragTarget == DragTarget.BLOCK_COUNT) ? 1f : 0f) - blockCountHoverAlpha) * Math.min(1f, dt * 14f);
        notificationHoverAlpha += (((notificationHovered || dragTarget == DragTarget.NOTIFICATION) ? 1f : 0f) - notificationHoverAlpha) * Math.min(1f, dt * 14f);
        potionStatusHoverAlpha += (((potionStatusHovered || dragTarget == DragTarget.POTION_STATUS) ? 1f : 0f) - potionStatusHoverAlpha) * Math.min(1f, dt * 14f);
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

        pendingGuiW = guiW;
        pendingGuiH = guiH;
        pendingProgress = progress;
        pendingTargetHud = targetHud == null ? null : new RectState(targetVisualX, targetVisualY, targetHud.w, targetHud.h);
        pendingKeystrokes = keystrokes;
        pendingBlockCount = blockCount;
        pendingNotification = notification;
        pendingPotionStatus = potionStatus;
        pendingFrame = true;

        if (canvas != null) {
            drawOverlay(canvas, guiW, guiH, progress, pendingTargetHud, keystrokes, blockCount, notification, potionStatus);
        }
    }

    public void renderFrameEnd() {
        if (!pendingFrame) return;
        Minecraft client = Minecraft.getInstance();
        if (!active || client.options.hideGui) {
            pendingFrame = false;
            return;
        }

        ensureNativeLoaded();
        Canvas canvas = glBackend.begin();
        if (canvas == null) return;
        try {
            drawOverlay(canvas, pendingGuiW, pendingGuiH, pendingProgress, pendingTargetHud, pendingKeystrokes, pendingBlockCount, pendingNotification, pendingPotionStatus);
        } finally {
            glBackend.end();
            pendingFrame = false;
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!active || Math.abs(amount) <= 0.001) return false;
        Minecraft mc = Minecraft.getInstance();
        int guiW = mc.getWindow().getGuiScaledWidth();
        int guiH = mc.getWindow().getGuiScaledHeight();

        RectState targetHud = Config.targetHud ? getTargetHudRect(guiW, guiH) : null;
        RectState keystrokes = Config.keystrokes ? getKeystrokesRect(guiW, guiH) : null;
        RectState blockCount = Config.blockCountDisplay ? getBlockCountRect(guiW, guiH) : null;
        RectState notification = getNotificationRect(guiW, guiH);
        RectState potionStatus = Config.potionStatus ? getPotionStatusRect(guiW, guiH) : null;

        float mx = (float) mouseX;
        float my = (float) mouseY;
        float delta = amount > 0 ? 0.05f : -0.05f;

        if (contains(notification, mx, my, 4f)) {
            Config.notificationScale = clampScale(Config.notificationScale + delta);
            Config.save();
            return true;
        }
        if (potionStatus != null && contains(potionStatus, mx, my, 4f)) {
            Config.potionStatusScale = clampScale(Config.potionStatusScale + delta);
            Config.save();
            return true;
        }
        if (blockCount != null && contains(blockCount, mx, my, 4f)) {
            Config.blockCountDisplayScale = clampScale(Config.blockCountDisplayScale + delta);
            Config.save();
            return true;
        }
        if (keystrokes != null && contains(keystrokes, mx, my, 4f)) {
            Config.keystrokesScale = clampScale(Config.keystrokesScale + delta);
            Config.save();
            return true;
        }
        if (targetHud != null && contains(targetHud, mx, my, 4f)) {
            Config.targetHudScale = clampScale(Config.targetHudScale + delta);
            Config.save();
            return true;
        }
        return false;
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

    private void drawOverlay(Canvas canvas, int guiW, int guiH, float progress, RectState targetHud, RectState keystrokes,
                             RectState blockCount, RectState notification, RectState potionStatus) {
        if (gridAlpha > 0.01f) {
            drawGrid(canvas, guiW, guiH, progress);
        }
        if (targetHud != null) {
            drawOutline(canvas, targetHud.x, targetHud.y, targetHud.w, targetHud.h, "Target HUD", targetHoverAlpha, progress);
        }
        if (keystrokes != null) {
            drawOutline(canvas, keystrokes.x, keystrokes.y, keystrokes.w, keystrokes.h, "Keystrokes", keystrokesHoverAlpha, progress);
        }
        if (blockCount != null) {
            drawOutline(canvas, blockCount.x, blockCount.y, blockCount.w, blockCount.h, "Block Count", blockCountHoverAlpha, progress);
        }
        if (potionStatus != null) {
            drawOutline(canvas, potionStatus.x, potionStatus.y, potionStatus.w, potionStatus.h, "Potion Status", potionStatusHoverAlpha, progress);
        }
        if (notification != null) {
            drawOutline(canvas, notification.x, notification.y, notification.w, notification.h, "Notification", notificationHoverAlpha, progress);
        }
        drawHint(canvas, guiW, guiH, progress);
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

    private void drawHint(Canvas canvas, int guiW, int guiH, float progress) {
        String text = Config.isChinese ? "鼠标悬浮在组件上滚轮即可调节组件大小" : "Hover over a widget and scroll to resize it";
        float textW = FontRenderer.measureTextWidth(text, HINT_TEXT_SIZE);
        float x = (guiW - textW) * 0.5f;
        float y = Math.max(48f, guiH - 92f);
        int alpha = Math.round(210f * Math.max(0f, Math.min(1f, progress)));
        FontRenderer.drawText(canvas, text, x, y, HINT_TEXT_SIZE, (alpha << 24) | 0xFFFFFF);
    }

    private RectState getTargetHudRect(int guiW, int guiH) {
        float scale = Math.max(0.5f, Config.targetHudScale);
        float baseW = Config.targetHudMode == Config.TargetHudMode.NEW ? TARGET_HUD_NEW_WIDTH : TARGET_HUD_WIDTH;
        float baseH = Config.targetHudMode == Config.TargetHudMode.NEW ? TARGET_HUD_NEW_HEIGHT : TARGET_HUD_HEIGHT;
        float w = baseW * scale;
        float h = baseH * scale;
        float x = guiW * 0.5f + Config.targetHudX;
        float y = guiH * 0.5f + Config.targetHudY;
        return clampRect(x, y, w, h, guiW, guiH);
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

    private RectState getBlockCountRect(int guiW, int guiH) {
        BlockCountDisplayRenderer renderer = BlockCountDisplayRenderer.getInstance();
        float w = renderer.getEditWidth();
        float h = renderer.getEditHeight();
        return clampRect(renderer.getRenderX(guiW), renderer.getRenderY(guiH), w, h, guiW, guiH);
    }

    private RectState getPotionStatusRect(int guiW, int guiH) {
        PotionStatusRenderer renderer = PotionStatusRenderer.getInstance();
        float w = renderer.getEditWidth();
        float h = renderer.getEditHeight();
        return clampRect(renderer.getRenderX(guiW), renderer.getRenderY(guiH), w, h, guiW, guiH);
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

    private float clampScale(float scale) {
        return Math.max(0.5f, Math.min(2.0f, scale));
    }

    private void ensureNativeLoaded() {
        if (nativeLoaded) return;
        Library.load();
        nativeLoaded = true;
    }

    private static final class RectState {
        final float x;
        final float y;
        final float w;
        final float h;

        RectState(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }
}
