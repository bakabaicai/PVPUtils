package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.skia.SkiaScreen;
import io.github.humbleui.skija.Canvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

import java.util.ArrayDeque;
import java.util.Deque;

public class KeystrokesRenderer {
    private static final KeystrokesRenderer INSTANCE = new KeystrokesRenderer();
    private static final int KEY_SIZE = 24;
    private static final int GAP = 3;
    private static final int BG_COLOR = 0x66000000;
    private static final int ACTIVE_COLOR = 0xCCFFFFFF;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int ACTIVE_TEXT_COLOR = 0xFF111111;
    private static final long CPS_WINDOW_MS = 1000;

    private final Deque<Long> leftClicks = new ArrayDeque<>();
    private final Deque<Long> rightClicks = new ArrayDeque<>();
    private boolean wasLeftDown = false;
    private boolean wasRightDown = false;

    public static KeystrokesRenderer getInstance() {
        return INSTANCE;
    }

    public void render(GuiGraphics graphics) {
        render(graphics, null);
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
        int leftCps = updateCps(leftClicks, leftDown, wasLeftDown);
        int rightCps = updateCps(rightClicks, rightDown, wasRightDown);
        wasLeftDown = leftDown;
        wasRightDown = rightDown;

        drawKey(graphics, "W", x + (KEY_SIZE + GAP) * scale, y, KEY_SIZE * scale, KEY_SIZE * scale, client.options.keyUp.isDown());
        drawKey(graphics, "A", x, y + (KEY_SIZE + GAP) * scale, KEY_SIZE * scale, KEY_SIZE * scale, client.options.keyLeft.isDown());
        drawKey(graphics, "S", x + (KEY_SIZE + GAP) * scale, y + (KEY_SIZE + GAP) * scale, KEY_SIZE * scale, KEY_SIZE * scale, client.options.keyDown.isDown());
        drawKey(graphics, "D", x + (KEY_SIZE + GAP) * 2 * scale, y + (KEY_SIZE + GAP) * scale, KEY_SIZE * scale, KEY_SIZE * scale, client.options.keyRight.isDown());

        int mouseY = (KEY_SIZE + GAP) * 2;
        int leftMouseW = (totalW - GAP) / 2;
        int rightMouseW = totalW - GAP - leftMouseW;
        drawMouseKey(graphics, "LMB", leftCps, x, y + mouseY * scale, leftMouseW * scale, KEY_SIZE * scale, leftDown);
        drawMouseKey(graphics, "RMB", rightCps, x + (leftMouseW + GAP) * scale, y + mouseY * scale, rightMouseW * scale, KEY_SIZE * scale, rightDown);

        int bottomY = (KEY_SIZE + GAP) * 3;
        drawKey(graphics, "SPACE", x, y + bottomY * scale, leftMouseW * scale, KEY_SIZE * scale, client.options.keyJump.isDown());
        drawKey(graphics, "SHIFT", x + (leftMouseW + GAP) * scale, y + bottomY * scale, rightMouseW * scale, KEY_SIZE * scale, client.options.keyShift.isDown());
    }

    private int updateCps(Deque<Long> clicks, boolean isDown, boolean wasDown) {
        long now = System.currentTimeMillis();
        if (isDown && !wasDown) {
            clicks.addLast(now);
        }
        while (!clicks.isEmpty() && now - clicks.peekFirst() > CPS_WINDOW_MS) {
            clicks.removeFirst();
        }
        return clicks.size();
    }

    private void drawKey(GuiGraphics graphics, String label, float x, float y, float width, float height, boolean active) {
        int ix = Math.round(x);
        int iy = Math.round(y);
        int iw = Math.round(width);
        int ih = Math.round(height);
        graphics.fill(ix, iy, ix + iw, iy + ih, active ? ACTIVE_COLOR : BG_COLOR);
        graphics.renderOutline(ix, iy, iw, ih, 0x99FFFFFF);

        int textColor = active ? ACTIVE_TEXT_COLOR : TEXT_COLOR;
        Minecraft client = Minecraft.getInstance();
        int textW = client.font.width(label);
        int textX = Math.round(x + (width - textW) * 0.5f);
        int textY = Math.round(y + (height - 8f) * 0.5f);
        graphics.drawString(client.font, label, textX, textY, textColor, false);
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

    private void drawMouseKey(GuiGraphics graphics, String label, int cps, float x, float y, float width, float height, boolean active) {
        int ix = Math.round(x);
        int iy = Math.round(y);
        int iw = Math.round(width);
        int ih = Math.round(height);
        graphics.fill(ix, iy, ix + iw, iy + ih, active ? ACTIVE_COLOR : BG_COLOR);
        graphics.renderOutline(ix, iy, iw, ih, 0x99FFFFFF);

        int textColor = active ? ACTIVE_TEXT_COLOR : TEXT_COLOR;
        String cpsText = cps + " CPS";
        Minecraft client = Minecraft.getInstance();
        int labelX = Math.round(x + (width - client.font.width(label)) * 0.5f);
        int cpsX = Math.round(x + (width - client.font.width(cpsText)) * 0.5f);
        graphics.drawString(client.font, label, labelX, Math.round(y + 4f), textColor, false);
        graphics.drawString(client.font, cpsText, cpsX, Math.round(y + 14f), textColor, false);
    }
}
