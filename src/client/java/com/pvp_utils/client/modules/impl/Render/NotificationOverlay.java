package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.font.FontRenderer;
import io.github.humbleui.skija.Canvas;
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
    private static final int PREVIEW_WIDTH = 192;
    private static final float TEXT_SIZE = 12f;
    private static final float SYMBOL_SIZE = 17f;

    private boolean active = false;
    private boolean persistent = false;
    private long startTime = 0;
    private long stopTime = -1;
    private String message = "";
    private int textColor = 0xFFFFFF;
    private ItemStack iconStack = ItemStack.EMPTY;
    private String symbolIcon = "";
    private int symbolColor = 0xFFFFFF;
    private boolean previewActive = false;
    private long previewStartTime = 0;
    private long previewStopTime = -1;

    public static NotificationOverlay getInstance() {
        return INSTANCE;
    }

    public void show(String text, int color) {
        this.message = text;
        this.textColor = color;
        this.iconStack = ItemStack.EMPTY;
        this.symbolIcon = "";
        this.active = true;
        this.persistent = false;
        this.startTime = System.currentTimeMillis();
        this.stopTime = -1;
        this.previewActive = false;
    }

    public void show(String text) {
        show(text, 0xFFFFFF);
    }

    public void show(String text, int color, ItemStack stack) {
        this.message = text;
        this.textColor = color;
        this.iconStack = stack == null ? ItemStack.EMPTY: stack;
        this.symbolIcon = "";
        this.active = true;
        this.persistent = false;
        this.startTime = System.currentTimeMillis();
        this.stopTime = -1;
    }

    public void showPersistent(String text, int color, ItemStack stack) {
        show(text, color, stack);
        this.persistent = true;
    }

    public void showPersistentSymbol(String text, int color, String icon, int iconColor) {
        this.message = text;
        this.textColor = color;
        this.iconStack = ItemStack.EMPTY;
        this.symbolIcon = icon == null ? "" : icon;
        this.symbolColor = iconColor;
        this.active = true;
        this.persistent = true;
        this.startTime = System.currentTimeMillis();
        this.stopTime = -1;
        this.previewActive = false;
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

    public void startEditPreview() {
        if (active) return;
        if (previewActive && previewStopTime == -1) return;
        previewActive = true;
        previewStartTime = System.currentTimeMillis();
        previewStopTime = -1;
    }

    public void stopEditPreview() {
        if (active) return;
        if (previewActive && previewStopTime == -1) {
            previewStopTime = System.currentTimeMillis();
        }
    }

    public void render(GuiGraphics graphics) {
        render(graphics, null);
    }

    public void render(GuiGraphics graphics, Canvas canvas) {
        boolean editPreview = previewActive && !active;
        if (!active && !editPreview) return;

        long now = System.currentTimeMillis();
        long elapsed = now - (active ? startTime : previewStartTime);

        Minecraft client = Minecraft.getInstance();
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        String renderMessage = active ? message : "Notification";
        ItemStack renderIcon = active ? iconStack : ItemStack.EMPTY;
        String renderSymbol = active ? symbolIcon : "";
        int textWidth = Math.round(FontRenderer.measureTextWidth(renderMessage, TEXT_SIZE));
        boolean hasSymbol = !renderSymbol.isEmpty();
        boolean hasIcon = !renderIcon.isEmpty() || hasSymbol;
        int iconSize = 16;
        int iconMargin = 8;

        int dynamicWidth = active ? textWidth + PADDING + GREEN_BAR_WIDTH : PREVIEW_WIDTH;
        float scale = Math.max(0.5f, Config.notificationScale);
        int scaledWidth = Math.round(dynamicWidth * scale);
        int scaledHeight = Math.round(HEIGHT * scale);
        int finalX = getRenderX(screenWidth, scaledWidth);
        int y = getRenderY(screenHeight);
        finalX = Math.max(0, Math.min(finalX, screenWidth - scaledWidth));
        y = Math.max(0, Math.min(y, screenHeight - scaledHeight));

        graphics.pose().pushMatrix();
        graphics.pose().translate(finalX, y);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-finalX, -y);
        if (canvas != null) {
            canvas.save();
            canvas.translate(finalX, y);
            canvas.scale(scale, scale);
            canvas.translate(-finalX, -y);
        }

        float greenProgress = 0f;
        float blackProgress = 0f;
        boolean contentVisible = false;

        long totalAnimIn = GREEN_ANIM_DURATION + BLACK_ANIM_DURATION;

        if (editPreview) {
            if (previewStopTime != -1) {
                long outElapsed = now - previewStopTime;
                if (outElapsed < BLACK_ANIM_DURATION) {
                    greenProgress = 1f;
                    blackProgress = 1f - easeInPow3((float) outElapsed / BLACK_ANIM_DURATION);
                } else if (outElapsed < BLACK_ANIM_DURATION + GREEN_ANIM_DURATION) {
                    blackProgress = 0f;
                    greenProgress = 1f - easeInPow3((float) (outElapsed - BLACK_ANIM_DURATION) / GREEN_ANIM_DURATION);
                } else {
                    previewActive = false;
                    previewStopTime = -1;
                    return;
                }
            } else if (elapsed < GREEN_ANIM_DURATION) {
                greenProgress = easeOutPow3((float) elapsed / GREEN_ANIM_DURATION);
            } else if (elapsed < totalAnimIn) {
                greenProgress = 1f;
                blackProgress = easeOutPow3((float) (elapsed - GREEN_ANIM_DURATION) / BLACK_ANIM_DURATION);
            } else {
                greenProgress = 1f;
                blackProgress = 1f;
                contentVisible = true;
            }
        } else if (elapsed < GREEN_ANIM_DURATION) {
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

        int greenX = (int) (screenWidth + (finalX - screenWidth) * greenProgress);
        int greenAlpha = (int) (greenProgress * 255) << 24;
        graphics.fill(greenX, y, finalX + dynamicWidth, y + HEIGHT, greenAlpha | GREEN_COLOR);

        int blackX = (int) (finalX + GREEN_BAR_WIDTH + (finalX + dynamicWidth - GREEN_BAR_WIDTH - finalX) * (1f - blackProgress));
        int blackAlpha = (int) (blackProgress * 255) << 24;
        graphics.fill(blackX, y, finalX + dynamicWidth, y + HEIGHT, blackAlpha | BLACK_COLOR);

        if (contentVisible) {
            int totalContentWidth = textWidth + (hasIcon ? (iconSize + iconMargin) : 0);
            int currentX = blackX + (dynamicWidth - GREEN_BAR_WIDTH - totalContentWidth) / 2;
            float textY = y + HEIGHT / 2f + 5f;

            if (hasSymbol) {
                if (canvas != null) {
                    FontRenderer.drawText(canvas, renderSymbol, currentX, y + HEIGHT / 2f + 7f, SYMBOL_SIZE, symbolColor | 0xFF000000, FontRenderer.MATERIAL_SYMBOLS);
                }
                currentX += iconSize + iconMargin;
            } else if (hasIcon) {
                graphics.renderFakeItem(renderIcon, currentX, y + (HEIGHT - iconSize) / 2);
                currentX += iconSize + iconMargin;
            }
            if (canvas != null) {
                FontRenderer.drawText(canvas, renderMessage, currentX, textY, TEXT_SIZE, textColor | 0xFF000000);
            }
        }
        if (canvas != null) {
            canvas.restore();
        }
        graphics.pose().popMatrix();
    }

    public int getEditWidth() {
        return Math.round(PREVIEW_WIDTH * Math.max(0.5f, Config.notificationScale));
    }

    public int getEditHeight() {
        return Math.round(HEIGHT * Math.max(0.5f, Config.notificationScale));
    }

    public int getRenderX(int screenWidth, int width) {
        return getRenderRight(screenWidth) - width;
    }

    public int getRenderY(int screenHeight) {
        if (Float.isNaN(Config.notificationY)) return (int) (screenHeight * 0.12);
        return Math.round(screenHeight * 0.5f + Config.notificationY);
    }

    public boolean needsCanvas() {
        return active || previewActive || HudEditOverlay.getInstance().isActive();
    }

    public int getRenderRight(int screenWidth) {
        if (Float.isNaN(Config.notificationX)) return screenWidth;
        return Math.round(screenWidth * 0.5f + Config.notificationX);
    }

    private float easeOutPow3(float value) {
        return 1.0f - (float) Math.pow(1.0 - value, 3);
    }

    private float easeInPow3(float value) {
        return (float) Math.pow(value, 3);
    }
}
