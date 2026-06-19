package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaScreen;
import com.pvp_utils.client.util.RateCounter;
import io.github.humbleui.skija.*;
import io.github.humbleui.types.RRect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

public class KeystrokesRenderer {
    private static final KeystrokesRenderer INSTANCE = new KeystrokesRenderer();
    private static final int KEY_SIZE = 24;
    private static final int GAP = 3;
    private static final int BG_COLOR = 0x7A0E1117;
    private static final int BG_ACTIVE_COLOR = 0xDDF2F4F8;
    private static final int BORDER_COLOR = 0x30FFFFFF;
    private static final int GLOW_COLOR = 0x38FFFFFF;
    private static final int TEXT_COLOR = 0xEFFFFFFF;
    private static final int ACTIVE_TEXT_COLOR = 0xFF171724;
    private static final int ACCENT_COLOR = 0xFFFFFFFF;
    private final RateCounter leftClicks = new RateCounter();
    private final RateCounter rightClicks = new RateCounter();
    private final KeyVisual wKey = new KeyVisual();
    private final KeyVisual aKey = new KeyVisual();
    private final KeyVisual sKey = new KeyVisual();
    private final KeyVisual dKey = new KeyVisual();
    private final KeyVisual leftMouse = new KeyVisual();
    private final KeyVisual rightMouse = new KeyVisual();
    private final KeyVisual spaceKey = new KeyVisual();
    private final KeyVisual shiftKey = new KeyVisual();
    private long lastFrameMs = 0L;

    public static KeystrokesRenderer getInstance() {
        return INSTANCE;
    }

    public void render(GuiGraphics graphics) {
        render(graphics, null);
    }

    public boolean needsCanvas() {
        return Config.keystrokes;
    }

    public void render(GuiGraphics graphics, Canvas canvas) {
        if (!Config.keystrokes) return;

        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof SkiaScreen) return;
        LocalPlayer player = client.player;
        if (player == null) return;

        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        int totalW = KEY_SIZE * 3 + GAP * 2;
        int totalH = KEY_SIZE * 4 + GAP * 3;
        float scale = Math.max(0.5f, Config.keystrokesScale);
        int scaledW = Math.round(totalW * scale);
        int scaledH = Math.round(totalH * scale);
        int x = (int) (screenW * 0.5f + Config.keystrokesX);
        int y = (int) (screenH * 0.5f + Config.keystrokesY);

        x = clampX(x, screenW);
        y = clampY(y, screenH);

        boolean leftDown = client.options.keyAttack.isDown();
        boolean rightDown = client.options.keyUse.isDown();
        int leftCps = leftClicks.updatePressed(leftDown);
        int rightCps = rightClicks.updatePressed(rightDown);

        long now = System.currentTimeMillis();
        float dt = lastFrameMs == 0L ? 0.016f : Math.min((now - lastFrameMs) / 1000f, 0.05f);
        lastFrameMs = now;

        boolean upDown = client.options.keyUp.isDown();
        boolean keyLeftDown = client.options.keyLeft.isDown();
        boolean downDown = client.options.keyDown.isDown();
        boolean keyRightDown = client.options.keyRight.isDown();
        boolean jumpDown = client.options.keyJump.isDown();
        boolean shiftDown = client.options.keyShift.isDown();

        wKey.update(upDown, dt);
        aKey.update(keyLeftDown, dt);
        sKey.update(downDown, dt);
        dKey.update(keyRightDown, dt);
        leftMouse.update(leftDown, dt);
        rightMouse.update(rightDown, dt);
        spaceKey.update(jumpDown, dt);
        shiftKey.update(shiftDown, dt);

        if (canvas == null) {
            drawFallback(graphics, client, x, y, scale, leftCps, rightCps, leftDown, rightDown, upDown, keyLeftDown, downDown, keyRightDown, jumpDown, shiftDown);
            return;
        }

        canvas.save();
        canvas.translate(x, y);
        canvas.scale(scale, scale);

        drawKey(canvas, "W", KEY_SIZE + GAP, 0, KEY_SIZE, KEY_SIZE, wKey);
        drawKey(canvas, "A", 0, KEY_SIZE + GAP, KEY_SIZE, KEY_SIZE, aKey);
        drawKey(canvas, "S", KEY_SIZE + GAP, KEY_SIZE + GAP, KEY_SIZE, KEY_SIZE, sKey);
        drawKey(canvas, "D", (KEY_SIZE + GAP) * 2, KEY_SIZE + GAP, KEY_SIZE, KEY_SIZE, dKey);

        int mouseY = (KEY_SIZE + GAP) * 2;
        int leftMouseW = (totalW - GAP) / 2;
        int rightMouseW = totalW - GAP - leftMouseW;
        drawMouseKey(canvas, "LMB", leftCps, 0, mouseY, leftMouseW, KEY_SIZE, leftMouse);
        drawMouseKey(canvas, "RMB", rightCps, leftMouseW + GAP, mouseY, rightMouseW, KEY_SIZE, rightMouse);

        int bottomY = (KEY_SIZE + GAP) * 3;
        drawKey(canvas, "SPACE", 0, bottomY, leftMouseW, KEY_SIZE, spaceKey);
        drawKey(canvas, "SHIFT", leftMouseW + GAP, bottomY, rightMouseW, KEY_SIZE, shiftKey);
        canvas.restore();
    }

    private void drawFallback(GuiGraphics graphics, Minecraft client, int x, int y, float scale, int leftCps, int rightCps, boolean leftDown, boolean rightDown, boolean upDown, boolean keyLeftDown, boolean downDown, boolean keyRightDown, boolean jumpDown, boolean shiftDown) {
        int totalW = KEY_SIZE * 3 + GAP * 2;
        int mouseY = (KEY_SIZE + GAP) * 2;
        int leftMouseW = (totalW - GAP) / 2;
        int rightMouseW = totalW - GAP - leftMouseW;
        drawFallbackKey(graphics, "W", x + (KEY_SIZE + GAP) * scale, y, KEY_SIZE * scale, KEY_SIZE * scale, upDown);
        drawFallbackKey(graphics, "A", x, y + (KEY_SIZE + GAP) * scale, KEY_SIZE * scale, KEY_SIZE * scale, keyLeftDown);
        drawFallbackKey(graphics, "S", x + (KEY_SIZE + GAP) * scale, y + (KEY_SIZE + GAP) * scale, KEY_SIZE * scale, KEY_SIZE * scale, downDown);
        drawFallbackKey(graphics, "D", x + (KEY_SIZE + GAP) * 2 * scale, y + (KEY_SIZE + GAP) * scale, KEY_SIZE * scale, KEY_SIZE * scale, keyRightDown);
        drawFallbackMouseKey(graphics, client, "LMB", leftCps, x, y + mouseY * scale, leftMouseW * scale, KEY_SIZE * scale, leftDown);
        drawFallbackMouseKey(graphics, client, "RMB", rightCps, x + (leftMouseW + GAP) * scale, y + mouseY * scale, rightMouseW * scale, KEY_SIZE * scale, rightDown);
        int bottomY = (KEY_SIZE + GAP) * 3;
        drawFallbackKey(graphics, "SPACE", x, y + bottomY * scale, leftMouseW * scale, KEY_SIZE * scale, jumpDown);
        drawFallbackKey(graphics, "SHIFT", x + (leftMouseW + GAP) * scale, y + bottomY * scale, rightMouseW * scale, KEY_SIZE * scale, shiftDown);
    }

    private void drawFallbackKey(GuiGraphics graphics, String label, float x, float y, float width, float height, boolean active) {
        int ix = Math.round(x);
        int iy = Math.round(y);
        int iw = Math.round(width);
        int ih = Math.round(height);
        graphics.fill(ix, iy, ix + iw, iy + ih, active ? BG_ACTIVE_COLOR : BG_COLOR);
        graphics.renderOutline(ix, iy, iw, ih, 0x99FFFFFF);

        int textColor = active ? ACTIVE_TEXT_COLOR : TEXT_COLOR;
        Minecraft client = Minecraft.getInstance();
        int textW = client.font.width(label);
        int textX = Math.round(x + (width - textW) * 0.5f);
        int textY = Math.round(y + (height - 8f) * 0.5f);
        graphics.drawString(client.font, label, textX, textY, textColor, false);
    }

    private void drawKey(Canvas canvas, String label, float x, float y, float width, float height, KeyVisual visual) {
        drawKeyShell(canvas, x, y, width, height, visual);
        int textColor = visual.press > 0.55f ? ACTIVE_TEXT_COLOR : TEXT_COLOR;
        drawCenteredText(canvas, label, x, y, width, height, 11f, textColor);
    }

    private void drawMouseKey(Canvas canvas, String label, int cps, float x, float y, float width, float height, KeyVisual visual) {
        drawKeyShell(canvas, x, y, width, height, visual);
        int textColor = visual.press > 0.55f ? ACTIVE_TEXT_COLOR : TEXT_COLOR;
        String cpsText = cps + " CPS";
        drawCenteredText(canvas, label, x, y + 3f, width, 9f, 8.5f, textColor);
        drawCenteredText(canvas, cpsText, x, y + 13f, width, 8f, 7.5f, withAlpha(textColor, 0.82f));
    }

    private void drawKeyShell(Canvas canvas, float x, float y, float width, float height, KeyVisual visual) {
        float press = visual.press;
        float pulse = visual.pulse;
        float drawX = x;
        float drawY = y;
        float drawW = width;
        float drawH = height;
        float radius = Math.min(7f, drawH * 0.32f);

        if (pulse > 0.01f) {
            try (Paint glow = new Paint()) {
                glow.setColor(withAlpha(GLOW_COLOR, pulse * 0.28f));
                glow.setAntiAlias(true);
                canvas.drawRRect(RRect.makeXYWH(drawX, drawY, drawW, drawH, radius), glow);
            }
        }

        try (Paint bg = new Paint()) {
            bg.setColor(BG_COLOR);
            bg.setAntiAlias(true);
            canvas.drawRRect(RRect.makeXYWH(drawX, drawY, drawW, drawH, radius), bg);
        }

        if (press > 0.01f) {
            canvas.save();
            canvas.clipRRect(RRect.makeXYWH(drawX, drawY, drawW, drawH, radius), ClipMode.INTERSECT, true);
            try (Paint ripple = new Paint()) {
                float maxRadius = (float) Math.sqrt(drawW * drawW + drawH * drawH) * 0.56f;
                float radiusNow = maxRadius * easeOutCubic(press);
                ripple.setColor(withAlpha(BG_ACTIVE_COLOR, 0.90f));
                ripple.setAntiAlias(true);
                canvas.drawCircle(drawX + drawW * 0.5f, drawY + drawH * 0.5f, radiusNow, ripple);
            }
            canvas.restore();
        }

        try (Paint border = new Paint()) {
            border.setColor(lerpColor(BORDER_COLOR, 0x99FFFFFF, press));
            border.setAntiAlias(true);
            border.setMode(PaintMode.STROKE);
            border.setStrokeWidth(1f);
            canvas.drawRRect(RRect.makeXYWH(drawX + 0.5f, drawY + 0.5f, drawW - 1f, drawH - 1f, radius), border);
        }
    }

    private void drawCenteredText(Canvas canvas, String text, float x, float y, float width, float height, float size, int color) {
        float textW = FontRenderer.measureTextWidth(text, size);
        float textX = x + (width - textW) * 0.5f;
        float textY = y + (height + FontRenderer.getLineHeight(size)) * 0.5f - 2.2f;
        FontRenderer.drawText(canvas, text, textX, textY, size, color);
    }

    public int getScaledWidth() {
        return Math.round((KEY_SIZE * 3 + GAP * 2) * Math.max(0.5f, Config.keystrokesScale));
    }

    public int getScaledHeight() {
        return Math.round((KEY_SIZE * 4 + GAP * 3) * Math.max(0.5f, Config.keystrokesScale));
    }

    public int getRenderX(int screenW) {
        return clampX((int) (screenW * 0.5f + Config.keystrokesX), screenW);
    }

    public int getRenderY(int screenH) {
        return clampY((int) (screenH * 0.5f + Config.keystrokesY), screenH);
    }

    private int clampX(int x, int screenW) {
        return Math.max(0, Math.min(x, screenW - getScaledWidth()));
    }

    private int clampY(int y, int screenH) {
        return Math.max(0, Math.min(y, screenH - getScaledHeight()));
    }

    private void drawFallbackMouseKey(GuiGraphics graphics, Minecraft client, String label, int cps, float x, float y, float width, float height, boolean active) {
        int ix = Math.round(x);
        int iy = Math.round(y);
        int iw = Math.round(width);
        int ih = Math.round(height);
        graphics.fill(ix, iy, ix + iw, iy + ih, active ? BG_ACTIVE_COLOR : BG_COLOR);
        graphics.renderOutline(ix, iy, iw, ih, 0x99FFFFFF);

        int textColor = active ? ACTIVE_TEXT_COLOR : TEXT_COLOR;
        String cpsText = cps + " CPS";
        int labelX = Math.round(x + (width - client.font.width(label)) * 0.5f);
        int cpsX = Math.round(x + (width - client.font.width(cpsText)) * 0.5f);
        graphics.drawString(client.font, label, labelX, Math.round(y + 4f), textColor, false);
        graphics.drawString(client.font, cpsText, cpsX, Math.round(y + 14f), textColor, false);
    }

    private int lerpColor(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = Math.round(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
        int r = Math.round(((from >>> 16) & 0xFF) + (((to >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)) * t);
        int g = Math.round(((from >>> 8) & 0xFF) + (((to >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private int withAlpha(int color, float alpha) {
        int a = Math.round(((color >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, alpha)));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    private float easeOutCubic(float value) {
        float t = 1f - Math.max(0f, Math.min(1f, value));
        return 1f - t * t * t;
    }

    private static class KeyVisual {
        private float press = 0f;
        private float pulse = 0f;
        private boolean wasActive = false;

        private void update(boolean active, float dt) {
            if (active && !wasActive) {
                pulse = 1f;
            }
            float target = active ? 1f : 0f;
            press += (target - press) * Math.min(1f, dt * 18f);
            pulse = Math.max(0f, pulse - dt * 3.8f);
            wasActive = active;
        }
    }
}
