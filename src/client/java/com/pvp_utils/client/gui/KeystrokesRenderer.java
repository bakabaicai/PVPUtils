package com.pvp_utils.client.gui;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

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
        if (!Config.keystrokes) return;

        Minecraft client = Minecraft.getInstance();
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

        x = Math.max(0, Math.min(x, screenW - scaledW));
        y = Math.max(0, Math.min(y, screenH - scaledH));

        boolean leftDown = client.options.keyAttack.isDown();
        boolean rightDown = client.options.keyUse.isDown();
        int leftCps = updateCps(leftClicks, leftDown, wasLeftDown);
        int rightCps = updateCps(rightClicks, rightDown, wasRightDown);
        wasLeftDown = leftDown;
        wasRightDown = rightDown;

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);

        drawKey(graphics, client, "W", KEY_SIZE + GAP, 0, KEY_SIZE, client.options.keyUp.isDown());
        drawKey(graphics, client, "A", 0, KEY_SIZE + GAP, KEY_SIZE, client.options.keyLeft.isDown());
        drawKey(graphics, client, "S", KEY_SIZE + GAP, KEY_SIZE + GAP, KEY_SIZE, client.options.keyDown.isDown());
        drawKey(graphics, client, "D", (KEY_SIZE + GAP) * 2, KEY_SIZE + GAP, KEY_SIZE, client.options.keyRight.isDown());

        int mouseY = (KEY_SIZE + GAP) * 2;
        int leftMouseW = (totalW - GAP) / 2;
        int rightMouseW = totalW - GAP - leftMouseW;
        drawMouseKey(graphics, client, "LMB", leftCps, 0, mouseY, leftMouseW, leftDown);
        drawMouseKey(graphics, client, "RMB", rightCps, leftMouseW + GAP, mouseY, rightMouseW, rightDown);

        int bottomY = (KEY_SIZE + GAP) * 3;
        drawKey(graphics, client, "SPACE", 0, bottomY, leftMouseW, client.options.keyJump.isDown());
        drawKey(graphics, client, "SHIFT", leftMouseW + GAP, bottomY, rightMouseW, client.options.keyShift.isDown());

        graphics.pose().popMatrix();
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

    private void drawKey(GuiGraphics graphics, Minecraft client, String label, int x, int y, int width, boolean active) {
        int height = KEY_SIZE;
        graphics.fill(x, y, x + width, y + height, active ? ACTIVE_COLOR : BG_COLOR);
        graphics.renderOutline(x, y, width, height, 0x99FFFFFF);

        int textColor = active ? ACTIVE_TEXT_COLOR : TEXT_COLOR;
        int textX = x + (width - client.font.width(label)) / 2;
        int textY = y + (height - 8) / 2;
        graphics.drawString(client.font, Component.literal(label), textX, textY, textColor, false);
    }

    private void drawMouseKey(GuiGraphics graphics, Minecraft client, String label, int cps, int x, int y, int width, boolean active) {
        int height = KEY_SIZE;
        graphics.fill(x, y, x + width, y + height, active ? ACTIVE_COLOR : BG_COLOR);
        graphics.renderOutline(x, y, width, height, 0x99FFFFFF);

        int textColor = active ? ACTIVE_TEXT_COLOR : TEXT_COLOR;
        String cpsText = cps + " CPS";
        int labelX = x + (width - client.font.width(label)) / 2;
        int cpsX = x + (width - client.font.width(cpsText)) / 2;
        graphics.drawString(client.font, Component.literal(label), labelX, y + 3, textColor, false);
        graphics.drawString(client.font, Component.literal(cpsText), cpsX, y + 13, textColor, false);
    }
}
