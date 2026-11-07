package com.pvp_utils.client.modules.impl.Render;

import io.github.humbleui.types.RRect;

import com.pvp_utils.client.render.skia.SkijaUi;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.HudAppearance;
import com.pvp_utils.client.render.HudPanel;
import com.pvp_utils.client.NeteaseMusic.NeteaseMusicScreen;
import com.pvp_utils.client.render.skia.SkijaRenderer;
import com.pvp_utils.client.render.skia.SkijaScreen;
import com.pvp_utils.client.util.RateCounter;
import io.github.humbleui.skija.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

import java.util.List;

public class KeystrokesRenderer {
    private static final KeystrokesRenderer INSTANCE = new KeystrokesRenderer();
    private static final int KEY_SIZE = 24;
    private static final int GAP = 3;
    private static final int LITE_BG_COLOR = 0x66000000;
    private static final int LITE_ACTIVE_COLOR = 0xCCFFFFFF;
    private static final int LITE_TEXT_COLOR = 0xFFFFFFFF;
    private static final int LITE_ACTIVE_TEXT_COLOR = 0xFF111111;
    private static final int TOTAL_W = KEY_SIZE * 3 + GAP * 2;
    private static final int TOTAL_H = KEY_SIZE * 4 + GAP * 3;
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
    private boolean pendingFrame = false;
    private int pendingX = 0;
    private int pendingY = 0;
    private float pendingScale = 1f;
    private int pendingLeftCps = 0;
    private int pendingRightCps = 0;
    private float keyLabelW;
    private float aLabelW;
    private float sLabelW;
    private float dLabelW;
    private float spaceLabelW;
    private float shiftLabelW;
    private float lmbLabelW;
    private float rmbLabelW;

    private long labelFontRevision = Long.MIN_VALUE;

    public static KeystrokesRenderer getInstance() {
        return INSTANCE;
    }

    public void render(GuiGraphics graphics) {
        if (!Config.keystrokes) return;

        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof SkijaScreen || client.screen instanceof NeteaseMusicScreen) return;
        LocalPlayer player = client.player;
        if (player == null) return;

        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        float scale = Math.max(0.5f, Config.keystrokesScale);
        int scaledW = Math.round(TOTAL_W * scale);
        int scaledH = Math.round(TOTAL_H * scale);
        int x = (int) (screenW * 0.5f + Config.keystrokesX);
        int y = (int) (screenH * 0.5f + Config.keystrokesY);

        x = clampX(x, screenW);
        y = clampY(y, screenH);

        boolean leftDown = client.options.keyAttack.isDown();
        boolean rightDown = client.options.keyUse.isDown();
        int leftCps = leftClicks.updatePressed(leftDown);
        int rightCps = rightClicks.updatePressed(rightDown);

        if (Config.keystrokesMode == Config.KeystrokesMode.LITE) {
            destroyTexture(client);
            lastFrameMs = 0L;
            renderLite(graphics, client, x, y, scale, leftCps, rightCps, leftDown, rightDown);
            return;
        }

        renderNew(client, x, y, scale, leftCps, rightCps, leftDown, rightDown);
    }

    private void renderLite(GuiGraphics graphics, Minecraft client, int x, int y, float scale, int leftCps, int rightCps, boolean leftDown, boolean rightDown) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-x, -y);

        drawLiteKey(graphics, "W", x + KEY_SIZE + GAP, y, KEY_SIZE, KEY_SIZE, client.options.keyUp.isDown());
        drawLiteKey(graphics, "A", x, y + KEY_SIZE + GAP, KEY_SIZE, KEY_SIZE, client.options.keyLeft.isDown());
        drawLiteKey(graphics, "S", x + KEY_SIZE + GAP, y + KEY_SIZE + GAP, KEY_SIZE, KEY_SIZE, client.options.keyDown.isDown());
        drawLiteKey(graphics, "D", x + (KEY_SIZE + GAP) * 2, y + KEY_SIZE + GAP, KEY_SIZE, KEY_SIZE, client.options.keyRight.isDown());

        int mouseY = (KEY_SIZE + GAP) * 2;
        int leftMouseW = (TOTAL_W - GAP) / 2;
        int rightMouseW = TOTAL_W - GAP - leftMouseW;
        drawLiteMouseKey(graphics, client, "LMB", leftCps, x, y + mouseY, leftMouseW, KEY_SIZE, leftDown);
        drawLiteMouseKey(graphics, client, "RMB", rightCps, x + leftMouseW + GAP, y + mouseY, rightMouseW, KEY_SIZE, rightDown);

        int bottomY = (KEY_SIZE + GAP) * 3;
        drawLiteKey(graphics, "SPACE", x, y + bottomY, leftMouseW, KEY_SIZE, client.options.keyJump.isDown());
        drawLiteKey(graphics, "SHIFT", x + leftMouseW + GAP, y + bottomY, rightMouseW, KEY_SIZE, client.options.keyShift.isDown());

        graphics.pose().popMatrix();
    }

    private void renderNew(Minecraft client, int x, int y, float scale, int leftCps, int rightCps, boolean leftDown, boolean rightDown) {
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

        pendingFrame = true;
        pendingX = x;
        pendingY = y;
        pendingScale = scale;
        pendingLeftCps = leftCps;
        pendingRightCps = rightCps;
    }

    public void renderSkija(Canvas canvas) {
        if (!pendingFrame) return;
        Minecraft client = Minecraft.getInstance();
        if (!Config.keystrokes || Config.keystrokesMode == Config.KeystrokesMode.LITE || client.options.hideGui || client.screen instanceof SkijaScreen || client.screen instanceof NeteaseMusicScreen) {
            clearPendingFrame();
            return;
        }
        updateLabelMetrics();
        renderGl(canvas, client, pendingX, pendingY, pendingScale, pendingLeftCps, pendingRightCps);
        clearPendingFrame();
    }

    private void drawLiteKey(GuiGraphics graphics, String label, float x, float y, float width, float height, boolean active) {
        int ix = Math.round(x);
        int iy = Math.round(y);
        int iw = Math.round(width);
        int ih = Math.round(height);
        graphics.fill(ix, iy, ix + iw, iy + ih, active ? LITE_ACTIVE_COLOR : LITE_BG_COLOR);
        graphics.renderOutline(ix, iy, iw, ih, 0x99FFFFFF);

        int textColor = active ? LITE_ACTIVE_TEXT_COLOR : LITE_TEXT_COLOR;
        Minecraft client = Minecraft.getInstance();
        int textW = client.font.width(label);
        int textX = Math.round(x + (width - textW) * 0.5f);
        int textY = Math.round(y + (height - 8f) * 0.5f);
        graphics.drawString(client.font, label, textX, textY, textColor, false);
    }

    private void drawLiteMouseKey(GuiGraphics graphics, Minecraft client, String label, int cps, float x, float y, float width, float height, boolean active) {
        int ix = Math.round(x);
        int iy = Math.round(y);
        int iw = Math.round(width);
        int ih = Math.round(height);
        graphics.fill(ix, iy, ix + iw, iy + ih, active ? LITE_ACTIVE_COLOR : LITE_BG_COLOR);
        graphics.renderOutline(ix, iy, iw, ih, 0x99FFFFFF);

        int textColor = active ? LITE_ACTIVE_TEXT_COLOR : LITE_TEXT_COLOR;
        String cpsText = cps + " CPS";
        int labelX = Math.round(x + (width - client.font.width(label)) * 0.5f);
        int cpsX = Math.round(x + (width - client.font.width(cpsText)) * 0.5f);
        graphics.drawString(client.font, label, labelX, Math.round(y + 4f), textColor, false);
        graphics.drawString(client.font, cpsText, cpsX, Math.round(y + 14f), textColor, false);
    }

    private void renderGl(Canvas canvas, Minecraft client, int x, int y, float scale, int leftCps, int rightCps) {
        renderKeyBackgrounds(canvas, client, x, y, scale);
        int save = canvas.save();
        try {
            canvas.translate(x, y);
            canvas.scale(scale, scale);
            int mouseY = (KEY_SIZE + GAP) * 2;
            int leftMouseW = (TOTAL_W - GAP) / 2;
            int rightMouseW = TOTAL_W - GAP - leftMouseW;

            int bottomY = (KEY_SIZE + GAP) * 3;
            drawDynamicKey(canvas, KEY_SIZE + GAP, 0, KEY_SIZE, KEY_SIZE, wKey);
            drawDynamicKey(canvas, 0, KEY_SIZE + GAP, KEY_SIZE, KEY_SIZE, aKey);
            drawDynamicKey(canvas, KEY_SIZE + GAP, KEY_SIZE + GAP, KEY_SIZE, KEY_SIZE, sKey);
            drawDynamicKey(canvas, (KEY_SIZE + GAP) * 2, KEY_SIZE + GAP, KEY_SIZE, KEY_SIZE, dKey);

            drawDynamicMouseKey(canvas, leftCps, 0, mouseY, leftMouseW, KEY_SIZE, leftMouse);
            drawDynamicMouseKey(canvas, rightCps, leftMouseW + GAP, mouseY, rightMouseW, KEY_SIZE, rightMouse);

            drawDynamicKey(canvas, 0, bottomY, leftMouseW, KEY_SIZE, spaceKey);
            drawDynamicKey(canvas, leftMouseW + GAP, bottomY, rightMouseW, KEY_SIZE, shiftKey);
            drawKeyLabel(canvas, "W", keyLabelW, KEY_SIZE + GAP, 0, KEY_SIZE, KEY_SIZE, wKey);
            drawKeyLabel(canvas, "A", aLabelW, 0, KEY_SIZE + GAP, KEY_SIZE, KEY_SIZE, aKey);
            drawKeyLabel(canvas, "S", sLabelW, KEY_SIZE + GAP, KEY_SIZE + GAP, KEY_SIZE, KEY_SIZE, sKey);
            drawKeyLabel(canvas, "D", dLabelW, (KEY_SIZE + GAP) * 2, KEY_SIZE + GAP, KEY_SIZE, KEY_SIZE, dKey);
            drawMouseLabel(canvas, "LMB", lmbLabelW, 0, mouseY, leftMouseW, leftMouse);
            drawMouseLabel(canvas, "RMB", rmbLabelW, leftMouseW + GAP, mouseY, rightMouseW, rightMouse);
            drawKeyLabel(canvas, "SPACE", spaceLabelW, 0, bottomY, leftMouseW, KEY_SIZE, spaceKey);
            drawKeyLabel(canvas, "SHIFT", shiftLabelW, leftMouseW + GAP, bottomY, rightMouseW, KEY_SIZE, shiftKey);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void renderKeyBackgrounds(Canvas canvas, Minecraft client, int x, int y, float scale) {
        int mouseY = (KEY_SIZE + GAP) * 2;
        int leftMouseW = (TOTAL_W - GAP) / 2;
        int rightMouseW = TOTAL_W - GAP - leftMouseW;
        int bottomY = (KEY_SIZE + GAP) * 3;
        List<RRect> regions = List.of(
                blurRegion(x, y, scale, KEY_SIZE + GAP, 0, KEY_SIZE, KEY_SIZE),
                blurRegion(x, y, scale, 0, KEY_SIZE + GAP, KEY_SIZE, KEY_SIZE),
                blurRegion(x, y, scale, KEY_SIZE + GAP, KEY_SIZE + GAP, KEY_SIZE, KEY_SIZE),
                blurRegion(x, y, scale, (KEY_SIZE + GAP) * 2, KEY_SIZE + GAP, KEY_SIZE, KEY_SIZE),
                blurRegion(x, y, scale, 0, mouseY, leftMouseW, KEY_SIZE),
                blurRegion(x, y, scale, leftMouseW + GAP, mouseY, rightMouseW, KEY_SIZE),
                blurRegion(x, y, scale, 0, bottomY, leftMouseW, KEY_SIZE),
                blurRegion(x, y, scale, leftMouseW + GAP, bottomY, rightMouseW, KEY_SIZE));
        HudAppearance appearance = Config.keystrokesAppearance;
        float radius = 7f * scale;
        for (RRect region : regions) {
            HudPanel.draw(canvas, region.getLeft(), region.getTop(), region.getWidth(), region.getHeight(), radius, appearance, 1f);
        }
    }

    private RRect blurRegion(int baseX, int baseY, float scale, float x, float y, float width, float height) {
        return RRect.makeXYWH(baseX + x * scale, baseY + y * scale, width * scale, height * scale, 7f * scale);
    }

    private void updateLabelMetrics() {
        long revision = SkijaUi.textFontRevision();
        if (labelFontRevision == revision) return;
        keyLabelW = SkijaUi.textWidth("W", 11f);
        aLabelW = SkijaUi.textWidth("A", 11f);
        sLabelW = SkijaUi.textWidth("S", 11f);
        dLabelW = SkijaUi.textWidth("D", 11f);
        spaceLabelW = SkijaUi.textWidth("SPACE", 11f);
        shiftLabelW = SkijaUi.textWidth("SHIFT", 11f);
        lmbLabelW = SkijaUi.textWidth("LMB", 8.5f);
        rmbLabelW = SkijaUi.textWidth("RMB", 8.5f);
        labelFontRevision = revision;
    }

    private void drawDynamicKey(Canvas canvas, float x, float y, float width, float height, KeyVisual visual) {
        drawKeyShell(canvas, x, y, width, height, visual);
    }

    private void drawDynamicMouseKey(Canvas canvas, int cps, float x, float y, float width, float height, KeyVisual visual) {
        drawKeyShell(canvas, x, y, width, height, visual);
        int textColor = animatedTextColor(visual);
        String cpsText = cps + " CPS";
        drawCenteredText(canvas, cpsText, x, y + 13f, width, 8f, 7.5f, withAlpha(textColor, 0.82f));
    }

    private void drawKeyLabel(Canvas canvas, String label, float labelWidth, float x, float y, float width, float height, KeyVisual visual) {
        int textColor = animatedTextColor(visual);
        drawCenteredText(canvas, label, labelWidth, x, y, width, height, 11f, textColor);
    }

    private void drawMouseLabel(Canvas canvas, String label, float labelWidth, float x, float y, float width, KeyVisual visual) {
        int textColor = animatedTextColor(visual);
        drawCenteredText(canvas, label, labelWidth, x, y + 3f, width, 9f, 8.5f, textColor);
    }

    private void drawKeyShell(Canvas canvas, float x, float y, float width, float height, KeyVisual visual) {
        int accent = Config.keystrokesAppearance.accent(x, y);
        if (visual.pulse > 0.01f) {
            SkijaUi.rounded(canvas, x, y, width, height, 7f, withAlpha(accent, visual.pulse * 0.12f));
        }
        if (visual.press > 0.01f) {
            SkijaUi.rounded(canvas, x, y, width, height, 7f, withAlpha(accent, 0.9f * easeOutCubic(visual.press)));
        }
    }

    private void drawCenteredText(Canvas canvas, String text, float x, float y, float width, float height, float size, int color) {
        drawCenteredText(canvas, text, SkijaUi.textWidth(text, size), x, y, width, height, size, color);
    }

    private void drawCenteredText(Canvas canvas, String text, float textW, float x, float y, float width, float height, float size, int color) {
        float textX = x + (width - textW) * 0.5f;
        SkijaUi.text(canvas, text, textX, y, height, color, size);
    }

    public int getScaledWidth() {
        return Math.round(TOTAL_W * Math.max(0.5f, Config.keystrokesScale));
    }

    public int getScaledHeight() {
        return Math.round(TOTAL_H * Math.max(0.5f, Config.keystrokesScale));
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

    private int animatedTextColor(KeyVisual visual) {
        int accent = Config.keystrokesAppearance.accent();
        float luminance = ((accent >> 16) & 255) * 0.2126f + ((accent >> 8) & 255) * 0.7152f + (accent & 255) * 0.0722f;
        int activeText = luminance > 145f ? 0xFF111827 : 0xFFFFFFFF;
        return lerpColor(Config.keystrokesAppearance.text(), activeText, easeOutCubic(visual.press));
    }

    private float easeOutCubic(float value) {
        float t = 1f - Math.max(0f, Math.min(1f, value));
        return 1f - t * t * t;
    }

    private void destroyTexture(Minecraft client) {
        clearPendingFrame();
    }

    private void clearPendingFrame() {
        pendingFrame = false;
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
            float speed = active ? 16f : 13f;
            press += (target - press) * Math.min(1f, dt * speed);
            if (active && press > 0.985f) press = 1f;
            if (!active && press < 0.015f) press = 0f;
            pulse = Math.max(0f, pulse - dt * 3.8f);
            wasActive = active;
        }
    }
}
