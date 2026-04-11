package com.pvp_utils.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public class NotificationOverlay {
    private static final NotificationOverlay INSTANCE = new NotificationOverlay();
    private static final int HEIGHT = 55;
    private static final int GREEN_BAR_WIDTH = 6;
    private static final int PADDING = 80;
    private static final long GREEN_ANIM_DURATION = 350;
    private static final long BLACK_ANIM_DURATION = 350;
    private static final long STAY_DURATION = 2000;
    private static final int GREEN_COLOR = 0x76B900;
    private static final int BLACK_COLOR = 0x1A1A1A;

    private boolean active = false;
    private boolean persistent = false;
    private long startTime = 0;
    private long stopTime = -1;
    private String message = "";
    private int textColor = 0xFFFFFF;
    private ItemStack iconStack = ItemStack.EMPTY;

    public static NotificationOverlay getInstance() {
        return INSTANCE;
    }

    public void show(String text, int color) {
        this.message = text;
        this.textColor = color;
        this.iconStack = ItemStack.EMPTY;
        this.active = true;
        this.persistent = false;
        this.startTime = System.currentTimeMillis();
        this.stopTime = -1;
    }

    public void show(String text) {
        show(text, 0xFFFFFF);
    }

    public void show(String text, int color, ItemStack stack) {
        this.message = text;
        this.textColor = color;
        this.iconStack = stack == null ? ItemStack.EMPTY: stack;
        this.active = true;
        this.persistent = false;
        this.startTime = System.currentTimeMillis();
        this.stopTime = -1;
    }

    public void showPersistent(String text, int color, ItemStack stack) {
        show(text, color, stack);
        this.persistent = true;
    }

    public void stopPersistent() {
        stopPersistent(0);
    }

    public void stopPersistent(long delay) {
        if (this.persistent) {
            this.persistent = false;
            this.stopTime = System.currentTimeMillis() + delay;
        }
    }

    public void render(GuiGraphics graphics) {
        if (!active) return;

        long now = System.currentTimeMillis();
        long elapsed = now - startTime;

        Minecraft client = Minecraft.getInstance();
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        int textWidth = client.font.width(message);
        boolean hasIcon = !iconStack.isEmpty();
        int iconSize = 16;
        int iconMargin = 8;

        int dynamicWidth = textWidth + PADDING + GREEN_BAR_WIDTH;
        int y = (int) (screenHeight * 0.12);

        float greenProgress = 0f;
        float blackProgress = 0f;
        boolean contentVisible = false;

        long totalAnimIn = GREEN_ANIM_DURATION + BLACK_ANIM_DURATION;

        if (elapsed < GREEN_ANIM_DURATION) {
            greenProgress = easeOutPow3((float) elapsed / GREEN_ANIM_DURATION);
        } else if (elapsed < totalAnimIn) {
            greenProgress = 1f;
            blackProgress = easeOutPow3((float) (elapsed - GREEN_ANIM_DURATION) / BLACK_ANIM_DURATION);
        } else {
            boolean shouldStay;
            if (persistent) {
                shouldStay = true;
            } else if (stopTime != -1) {
                shouldStay = now < stopTime;
            } else {
                shouldStay = elapsed < totalAnimIn + STAY_DURATION;
            }

            if (shouldStay) {
                greenProgress = 1f;
                blackProgress = 1f;
                contentVisible = true;
            } else {
                long outStart = (stopTime != -1) ? stopTime : (startTime + totalAnimIn + STAY_DURATION);
                long outElapsed = now - outStart;

                if (outElapsed < BLACK_ANIM_DURATION) {
                    greenProgress = 1f;
                    blackProgress = 1f - easeInPow3((float) outElapsed / BLACK_ANIM_DURATION);
                } else if (outElapsed < BLACK_ANIM_DURATION + GREEN_ANIM_DURATION) {
                    blackProgress = 0f;
                    greenProgress = 1f - easeInPow3((float) (outElapsed - BLACK_ANIM_DURATION) / GREEN_ANIM_DURATION);
                } else {
                    active = false;
                    return;
                }
            }
        }

        int greenX = (int) (screenWidth - (dynamicWidth * greenProgress));
        int greenAlpha = (int) (greenProgress * 255) << 24;
        graphics.fill(greenX, y, screenWidth, y + HEIGHT, greenAlpha | GREEN_COLOR);

        int blackX = (int) (screenWidth - ((dynamicWidth - GREEN_BAR_WIDTH) * blackProgress));
        int blackAlpha = (int) (blackProgress * 255) << 24;
        graphics.fill(blackX, y, screenWidth, y + HEIGHT, blackAlpha | BLACK_COLOR);

        if (contentVisible) {
            int totalContentWidth = textWidth + (hasIcon ? (iconSize + iconMargin) : 0);
            int currentX = blackX + (dynamicWidth - GREEN_BAR_WIDTH - totalContentWidth) / 2;
            int centerY = y + (HEIGHT - 8) / 2;

            if (hasIcon) {
                graphics.renderFakeItem(iconStack, currentX, y + (HEIGHT - iconSize) / 2);
                currentX += iconSize + iconMargin;
            }
            graphics.drawString(client.font, message, currentX, centerY, textColor | 0xFF000000, false);
        }
    }

    private float easeOutPow3(float value) {
        return 1.0f - (float) Math.pow(1.0 - value, 3);
    }

    private float easeInPow3(float value) {
        return (float) Math.pow(value, 3);
    }
}