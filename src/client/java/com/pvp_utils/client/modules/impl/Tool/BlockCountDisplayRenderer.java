package com.pvp_utils.client.modules.impl.Tool;

import io.github.humbleui.types.RRect;

import com.pvp_utils.client.render.skia.SkijaUi;

import com.pvp_utils.client.render.skia.SkijaRenderer;
import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Render.HudEditOverlay;
import com.pvp_utils.client.util.RateCounter;
import io.github.humbleui.skija.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;

public class BlockCountDisplayRenderer {
    private static final BlockCountDisplayRenderer INSTANCE = new BlockCountDisplayRenderer();
    private static final long STAY_MS = 900L;
    private static final long ANIM_DURATION = 200L;
    private static final float WIDTH = 190f;
    private static final float HEIGHT = 58f;
    private static final float PURPLE = 0xFF8F5CFF;

    private final RateCounter rightClicks = new RateCounter();
    private final RateCounter placements = new RateCounter();
    private final Paint bgPaint = new Paint().setAntiAlias(true);
    private final Paint ringFillPaint = new Paint().setAntiAlias(true);
    private final Paint ringTrackPaint = new Paint().setAntiAlias(true).setMode(PaintMode.STROKE).setStrokeWidth(4f);
    private final Paint ringArcPaint = new Paint().setAntiAlias(true).setMode(PaintMode.STROKE).setStrokeWidth(4f);

    private boolean visible = false;
    private boolean closing = false;

    private float scale = 0f;
    private float ringProgress = 0f;
    private float closingRingProgress = 0f;
    private long lastInteractionMs = 0L;
    private long appearanceTime = 0L;
    private long closeTime = 0L;
    private int lastSlot = -1;
    private int lastCount = -1;
    private ItemStack displayStack = ItemStack.EMPTY;

    public static BlockCountDisplayRenderer getInstance() {
        return INSTANCE;
    }

    public float getEditWidth() {
        return WIDTH * Math.max(0.5f, Config.blockCountDisplayScale);
    }

    public float getEditHeight() {
        return HEIGHT * Math.max(0.5f, Config.blockCountDisplayScale);
    }

    public float getDefaultY(int screenH) {
        return screenH - 112f;
    }

    public float getRenderX(int screenW) {
        float scaledW = getEditWidth();
        return clamp((screenW - scaledW) * 0.5f + Config.blockCountDisplayX, 0f, Math.max(0f, screenW - scaledW));
    }

    public float getRenderY(int screenH) {
        float scaledH = getEditHeight();
        return clamp(getDefaultY(screenH) + Config.blockCountDisplayY, 0f, Math.max(0f, screenH - scaledH));
    }

    public void tick(Minecraft client) {
        if (!isFeatureActive()) {
            reset();
            return;
        }
        if (HudEditOverlay.getInstance().isActive()) return;

        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.screen != null) {
            close();
            rightClicks.resetPressed();
            return;
        }

        ItemStack stack = player.getMainHandItem();
        boolean block = stack.getItem() instanceof BlockItem;
        long now = System.currentTimeMillis();
        placements.count(now);

        if (!block) {
            close();
            rightClicks.resetPressed();
            return;
        }

        int slot = player.getInventory().getSelectedSlot();
        if (slot != lastSlot || !ItemStack.isSameItemSameComponents(stack, displayStack)) {
            if (visible && lastSlot != -1) close();
            ringProgress = 0f;
            lastSlot = slot;
        }
        lastCount = stack.getCount();
        displayStack = stack.copy();

        if (visible && now - lastInteractionMs > STAY_MS) close();
    }

    public void triggerUse(Minecraft client) {
        if (!isFeatureActive() || HudEditOverlay.getInstance().isActive()) return;
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.screen != null) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof BlockItem)) return;

        long now = System.currentTimeMillis();
        open(now);
        lastInteractionMs = now;
        if (scale <= 0.01f) ringProgress = 0f;
        lastSlot = player.getInventory().getSelectedSlot();
        lastCount = stack.getCount();
        displayStack = stack.copy();
        placements.count(now);
    }

    public void recordPlacement(Minecraft client) {
        if (!isFeatureActive() || HudEditOverlay.getInstance().isActive()) return;
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.screen != null) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof BlockItem)) return;

        long now = System.currentTimeMillis();
        placements.record();
        open(now);
        lastInteractionMs = now;
        if (scale <= 0.01f) ringProgress = 0f;
        lastSlot = player.getInventory().getSelectedSlot();
        lastCount = stack.getCount();
        displayStack = stack.copy();
    }

    public void render(GuiGraphics graphics, Canvas canvas) {
        if (!isFeatureActive()) {
            reset();
            return;
        }
        if (!Config.blockCountDisplay) {
            updateScale(System.currentTimeMillis());
            return;
        }

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        boolean editActive = HudEditOverlay.getInstance().isActive();
        if (player == null || client.level == null || (client.screen != null && !editActive)) {
            close();
            updateScale(System.currentTimeMillis());
            return;
        }

        ItemStack stack = player.getMainHandItem();
        boolean block = stack.getItem() instanceof BlockItem;
        long now = System.currentTimeMillis();

        if (editActive) {
            visible = true;
            closing = false;
            scale = Math.max(scale, 1f);
            lastInteractionMs = now;
            if (!block) {
                stack = new ItemStack(Items.STONE, 64);
                block = true;
            }
            displayStack = stack.copy();
            lastSlot = player.getInventory().getSelectedSlot();
            lastCount = stack.getCount();
            if (ringProgress <= 0.01f) ringProgress = 1f;
        }

        int rightCps = editActive ? rightClicks.count(now) : rightClicks.updatePressed(client.options.keyUse.isDown());
        placements.count(now);
        updateScale(now);
        if (scale <= 0.01f || displayStack.isEmpty()) return;

        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        float x = getRenderX(screenW);
        float y = getRenderY(screenH);
        float userScale = Math.max(0.5f, Config.blockCountDisplayScale);
        float scaledW = WIDTH * userScale;
        float scaledH = HEIGHT * userScale;
        float cx = x + scaledW * 0.5f;
        float cy = y + scaledH * 0.5f;
        float drawScale = easeOutBack(scale);
        float drawX = cx + (x - cx) * drawScale;
        float drawY = cy + (y - cy) * drawScale;
        float drawW = scaledW * drawScale;
        float drawH = scaledH * drawScale;
        float drawRadius = 16f * userScale * drawScale;

        String name = displayStack.getHoverName().getString();
        if (SkijaUi.textWidth(name, 13f) > 128f) {
            while (name.length() > 1 && SkijaUi.textWidth(name + "...", 13f) > 128f) {
                name = name.substring(0, name.length() - 1);
            }
            name += "...";
        }
        String speed = String.format(Locale.ROOT, "%.2fBPS\\%dCPS", RateCounter.horizontalBlocksPerSecond(client), rightCps);

        float ringCx = x + (WIDTH - 32f) * userScale;
        float ringCy = y + HEIGHT * 0.5f * userScale;
        float ratio = Math.max(0f, Math.min(1f, displayStack.getCount() / (float) Math.max(1, displayStack.getMaxStackSize())));
        ringProgress += (ratio - ringProgress) * 0.18f;

        boolean blurMode = Config.blockCountDisplayMode == Config.BlockCountDisplayMode.BLUR;
        if (blurMode) {
            SkijaRenderer.draw(blurCanvas -> {
                SkijaRenderer.drawBlurredBackdrop(blurCanvas, RRect.makeXYWH(drawX, drawY, drawW, drawH, drawRadius), drawX, drawY, drawW, drawH, Math.max(0f, Math.min(2f, Config.skiaBlurStrength)) * 10.5f);
                SkijaUi.rounded(blurCanvas, drawX, drawY, drawW, drawH, drawRadius, Config.skiaBlurTintColor());
            });
        }

        String frameName = name;
        float frameProgress = ringProgress;
        SkijaRenderer.draw(c -> {
            c.translate(drawX, drawY);
            c.scale(userScale * drawScale, userScale * drawScale);
            drawBase(c, frameName, blurMode);
            drawOverlay(c, speed, frameProgress, blurMode);
        });

        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, cy);
        graphics.pose().scale(drawScale, drawScale);
        graphics.pose().translate(-cx, -cy);

        int itemX = Math.round(ringCx - 8f);
        int itemY = Math.round(ringCy - 8f);
        float iconScale = 0.35f + 0.65f * easeOutCubic(scale);
        graphics.pose().translate(ringCx, ringCy);
        graphics.pose().scale(iconScale, iconScale);
        graphics.pose().translate(-ringCx, -ringCy);
        graphics.renderFakeItem(displayStack, itemX, itemY);
        graphics.renderItemDecorations(client.font, displayStack, itemX, itemY);
        graphics.renderDeferredElements();
        graphics.pose().popMatrix();
    }

    public Snapshot snapshot(Minecraft client) {
        if (!isFeatureActive() || client == null) return Snapshot.EMPTY;
        long now = System.currentTimeMillis();
        updateScale(now);
        if (scale <= 0.01f || displayStack.isEmpty()) return Snapshot.EMPTY;
        float blocksPerSecond = RateCounter.horizontalBlocksPerSecond(client);
        float ratio = Math.max(0f, Math.min(1f, displayStack.getCount() / (float) Math.max(1, displayStack.getMaxStackSize())));
        if (!closing) {
            ringProgress += (ratio - ringProgress) * 0.18f;
        }
        String name = displayStack.getHoverName().getString();
        float displayProgress = closing ? closingRingProgress : ringProgress;
        return new Snapshot(true, easeOutCubic(scale), name, displayStack.getCount(), blocksPerSecond, displayProgress);
    }

    private boolean isFeatureActive() {
        return Config.blockCountDisplay || (Config.dynamicIsland && Config.dynamicIslandBlockCount);
    }

    public record Snapshot(boolean visible, float alpha, String itemName, int blocksLeft, float blocksPerSecond, float progress) {
        public static final Snapshot EMPTY = new Snapshot(false, 0f, "", 0, 0, 0f);
    }

    private void updateScale(long now) {
        if (visible) {
            scale = clamp((now - appearanceTime) / (float) ANIM_DURATION, 0f, 1f);
            return;
        }

        if (closing) {
            scale = 1f - clamp((now - closeTime) / (float) ANIM_DURATION, 0f, 1f);
        } else {
            scale = 0f;
        }

        if (!visible && scale <= 0f) {
            closing = false;
            if (displayStack.isEmpty()) return;
            displayStack = ItemStack.EMPTY;
        }
    }

    private float easeOutBack(float value) {
        float t = Math.max(0f, Math.min(1f, value)) - 1f;
        return 1f + t * t * (1.55f * t + 0.55f);
    }

    private float easeOutCubic(float value) {
        float t = 1f - Math.max(0f, Math.min(1f, value));
        return 1f - t * t * t;
    }

    private void open(long now) {
        if (!visible) {
            appearanceTime = now - Math.round(scale * ANIM_DURATION);
        }
        visible = true;
        closing = false;
        closingRingProgress = 0f;
    }

    private void close() {
        if (visible) {
            closingRingProgress = ringProgress;
            closeTime = System.currentTimeMillis() - Math.round((1f - scale) * ANIM_DURATION);
        }
        visible = false;
        closing = true;
        lastSlot = -1;
        lastCount = -1;
    }

    private void reset() {
        visible = false;
        closing = false;
        scale = 0f;
        lastInteractionMs = 0L;
        appearanceTime = 0L;
        closeTime = 0L;
        ringProgress = 0f;
        closingRingProgress = 0f;
        lastSlot = -1;
        lastCount = -1;
        displayStack = ItemStack.EMPTY;
        rightClicks.clear();
        placements.clear();
    }

    private void drawBase(Canvas c, String name, boolean blurMode) {
        if (!blurMode) {
            bgPaint.setColor(newCardColor());
            c.drawRRect(RRect.makeXYWH(0f, 0f, WIDTH, HEIGHT, 16f), bgPaint);
        }
        SkijaUi.text(c, name, 16f, (22f) + SkijaUi.textMetrics(12f).getAscent(), SkijaUi.textMetrics(12f).getDescent() - SkijaUi.textMetrics(12f).getAscent(), primaryTextColor(blurMode), 12f);
    }

    private void drawOverlay(Canvas c, String speed, float progress, boolean blurMode) {
        SkijaUi.text(c, speed, 16f, (40f) + SkijaUi.textMetrics(11f).getAscent(), SkijaUi.textMetrics(11f).getDescent() - SkijaUi.textMetrics(11f).getAscent(), mutedTextColor(blurMode), 11f);
        float ringCx = WIDTH - 32f;
        float ringCy = HEIGHT * 0.5f;
        float radius = 17f;
        ringFillPaint.setColor(ringFillColor(blurMode));
        c.drawCircle(ringCx, ringCy, radius + 5f, ringFillPaint);
        ringTrackPaint.setColor(ringTrackColor(blurMode));
        c.drawCircle(ringCx, ringCy, radius, ringTrackPaint);
        ringArcPaint.setColor((int) PURPLE);
        c.drawArc(ringCx - radius, ringCy - radius, ringCx + radius, ringCy + radius, -90f, -360f * progress, false, ringArcPaint);
    }

    private int newCardColor() {
        return Config.hudTheme == Config.HudTheme.LIGHT ? 0xF7F8FAFC : 0xE6111827;
    }

    private int primaryTextColor(boolean blurMode) {
        return blurMode ? Config.hudPrimaryTextColor() : (Config.hudTheme == Config.HudTheme.LIGHT ? 0xFF202027 : 0xFFF8FAFC);
    }

    private int mutedTextColor(boolean blurMode) {
        return blurMode ? Config.hudMutedTextColor() : (Config.hudTheme == Config.HudTheme.LIGHT ? 0xAA5C5870 : 0xB8CBD5E1);
    }

    private int ringFillColor(boolean blurMode) {
        if (blurMode) {
            return Config.hudTheme == Config.HudTheme.LIGHT ? 0x228F5CFF : 0x1F8F5CFF;
        }
        return Config.hudTheme == Config.HudTheme.LIGHT ? 0x228F5CFF : 0x2A8F5CFF;
    }

    private int ringTrackColor(boolean blurMode) {
        if (blurMode) {
            return Config.hudTheme == Config.HudTheme.LIGHT ? 0x448F5CFF : 0x338F5CFF;
        }
        return Config.hudTheme == Config.HudTheme.LIGHT ? 0x448F5CFF : 0x668F5CFF;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }
}
