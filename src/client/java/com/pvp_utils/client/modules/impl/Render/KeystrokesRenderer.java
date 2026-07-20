package com.pvp_utils.client.modules.impl.Render;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pvp_utils.Config;
import com.pvp_utils.client.NeteaseMusic.NeteaseMusicScreen;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaGlBackend;
import com.pvp_utils.client.render.skia.SkiaScreen;
import com.pvp_utils.client.util.RateCounter;
import io.github.humbleui.skija.*;
import io.github.humbleui.skija.impl.Library;
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
    private static final int GLOW_COLOR = 0x38FFFFFF;
    private static final int TEXT_COLOR = 0xEFFFFFFF;
    private static final int ACTIVE_TEXT_COLOR = 0xFF171724;
    private static final int ACCENT_COLOR = 0xFFFFFFFF;
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
    private final Paint glowPaint = new Paint().setAntiAlias(true);
    private final Paint bgPaint = new Paint().setAntiAlias(true);
    private final Paint ripplePaint = new Paint().setAntiAlias(true);
    private final SkiaGlBackend glBackend = new SkiaGlBackend();
    private boolean nativeLoaded = false;
    private long lastFrameMs = 0L;
    private boolean pendingFrame = false;
    private int pendingX = 0;
    private int pendingY = 0;
    private float pendingScale = 1f;
    private int pendingLeftCps = 0;
    private int pendingRightCps = 0;
    private final float keyLabelW = FontRenderer.measureTextWidth("W", 11f);
    private final float aLabelW = FontRenderer.measureTextWidth("A", 11f);
    private final float sLabelW = FontRenderer.measureTextWidth("S", 11f);
    private final float dLabelW = FontRenderer.measureTextWidth("D", 11f);
    private final float spaceLabelW = FontRenderer.measureTextWidth("SPACE", 11f);
    private final float shiftLabelW = FontRenderer.measureTextWidth("SHIFT", 11f);
    private final float lmbLabelW = FontRenderer.measureTextWidth("LMB", 8.5f);
    private final float rmbLabelW = FontRenderer.measureTextWidth("RMB", 8.5f);

    public static KeystrokesRenderer getInstance() {
        return INSTANCE;
    }

    public void render(GuiGraphics graphics) {
        if (!Config.keystrokes) return;

        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof SkiaScreen || client.screen instanceof NeteaseMusicScreen) return;
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

    public boolean needsCanvas() {
        return false;
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

    public void renderFrameEnd() {
        if (!pendingFrame) return;
        Minecraft client = Minecraft.getInstance();
        if (!Config.keystrokes || Config.keystrokesMode == Config.KeystrokesMode.LITE || client.options.hideGui || client.screen instanceof SkiaScreen || client.screen instanceof NeteaseMusicScreen) {
            clearPendingFrame();
            return;
        }
        renderGl(client, pendingX, pendingY, pendingScale, pendingLeftCps, pendingRightCps);
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

    private void renderGl(Minecraft client, int x, int y, float scale, int leftCps, int rightCps) {
        ensureNativeLoaded();
        Canvas canvas = glBackend.begin(mainFramebufferId(client));
        if (canvas == null) return;
        try {
            canvas.save();
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
            canvas.restore();
        } finally {
            glBackend.end();
        }
    }

    private int mainFramebufferId(Minecraft client) {
        if (client.getMainRenderTarget().getColorTexture() instanceof GlTexture texture
                && RenderSystem.getDevice() instanceof GlDevice device) {
            return texture.getFbo(device.directStateAccess(), client.getMainRenderTarget().getDepthTexture());
        }
        return 0;
    }

    private void drawFallback(GuiGraphics graphics, Minecraft client, int x, int y, float scale, int leftCps, int rightCps, boolean leftDown, boolean rightDown, boolean upDown, boolean keyLeftDown, boolean downDown, boolean keyRightDown, boolean jumpDown, boolean shiftDown) {
        int mouseY = (KEY_SIZE + GAP) * 2;
        int leftMouseW = (TOTAL_W - GAP) / 2;
        int rightMouseW = TOTAL_W - GAP - leftMouseW;
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
        float press = visual.press;
        float pulse = visual.pulse;
        float drawX = x;
        float drawY = y;
        float drawW = width;
        float drawH = height;
        float radius = Math.min(7f, drawH * 0.32f);

        if (pulse > 0.01f) {
            glowPaint.setColor(withAlpha(GLOW_COLOR, pulse * 0.28f));
            canvas.drawRRect(RRect.makeXYWH(drawX, drawY, drawW, drawH, radius), glowPaint);
        }

        bgPaint.setColor(BG_COLOR);
        canvas.drawRRect(RRect.makeXYWH(drawX, drawY, drawW, drawH, radius), bgPaint);

        if (press > 0.01f) {
            ripplePaint.setColor(withAlpha(BG_ACTIVE_COLOR, 0.92f * easeOutCubic(press)));
            canvas.drawRRect(RRect.makeXYWH(drawX, drawY, drawW, drawH, radius), ripplePaint);
        }

    }

    private void drawCenteredText(Canvas canvas, String text, float x, float y, float width, float height, float size, int color) {
        drawCenteredText(canvas, text, FontRenderer.measureTextWidth(text, size), x, y, width, height, size, color);
    }

    private void drawCenteredText(Canvas canvas, String text, float textW, float x, float y, float width, float height, float size, int color) {
        float textX = x + (width - textW) * 0.5f;
        float textY = y + (height + FontRenderer.getLineHeight(size)) * 0.5f - 2.2f;
        FontRenderer.drawText(canvas, text, textX, textY, size, color);
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

    private int animatedTextColor(KeyVisual visual) {
        return lerpColor(TEXT_COLOR, ACTIVE_TEXT_COLOR, easeOutCubic(visual.press));
    }

    private float easeOutCubic(float value) {
        float t = 1f - Math.max(0f, Math.min(1f, value));
        return 1f - t * t * t;
    }

    private void ensureNativeLoaded() {
        if (nativeLoaded) return;
        Library.load();
        nativeLoaded = true;
    }

    private void destroyTexture(Minecraft client) {
        glBackend.destroy();
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
