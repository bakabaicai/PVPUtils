package com.pvp_utils.client.modules.impl.Tool;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Render.HudEditOverlay;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaBlurRenderer;
import com.pvp_utils.client.util.RateCounter;
import io.github.humbleui.skija.*;
import io.github.humbleui.skija.impl.Library;
import io.github.humbleui.types.RRect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Locale;

public class BlockCountDisplayRenderer {
    private static final BlockCountDisplayRenderer INSTANCE = new BlockCountDisplayRenderer();
    private static final long STAY_MS = 900L;
    private static final long ANIM_DURATION = 200L;
    private static final float WIDTH = 190f;
    private static final float HEIGHT = 58f;
    private static final float PURPLE = 0xFF8F5CFF;
    private static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath("pvp_utils", "block_count_display");
    private static final Identifier OVERLAY_TEXTURE_ID = Identifier.fromNamespaceAndPath("pvp_utils", "block_count_display_overlay");
    private static final SurfaceProps SURFACE_PROPS = new SurfaceProps(false, PixelGeometry.RGB_H);

    private final RateCounter rightClicks = new RateCounter();
    private final RateCounter placements = new RateCounter();
    private final Paint bgPaint = new Paint().setAntiAlias(true);
    private final Paint ringFillPaint = new Paint().setAntiAlias(true);
    private final Paint ringTrackPaint = new Paint().setAntiAlias(true).setMode(PaintMode.STROKE).setStrokeWidth(4f);
    private final Paint ringArcPaint = new Paint().setAntiAlias(true).setMode(PaintMode.STROKE).setStrokeWidth(4f);

    private Surface surface;
    private Surface overlaySurface;
    private DynamicTexture dynamicTexture;
    private DynamicTexture overlayTexture;
    private boolean visible = false;
    private boolean closing = false;
    private boolean nativeLoaded = false;
    private float scale = 0f;
    private float ringProgress = 0f;
    private float closingRingProgress = 0f;
    private String lastTextureName = "";
    private String lastTextureSpeed = "";
    private int lastTextureProgress = -1;
    private Config.HudTheme lastTextureTheme = null;
    private Config.HudTheme lastOverlayTextureTheme = null;
    private boolean lastTextureBlurMode = false;
    private boolean lastOverlayTextureBlurMode = false;
    private int textureW = -1;
    private int textureH = -1;
    private int overlayTextureW = -1;
    private int overlayTextureH = -1;
    private float textureScale = -1f;
    private long lastInteractionMs = 0L;
    private long appearanceTime = 0L;
    private long closeTime = 0L;
    private int lastSlot = -1;
    private int lastCount = -1;
    private ItemStack displayStack = ItemStack.EMPTY;

    public static BlockCountDisplayRenderer getInstance() {
        return INSTANCE;
    }

    public boolean needsCanvas() {
        return false;
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
            destroyTexture(Minecraft.getInstance());
            reset();
            return;
        }
        if (!Config.blockCountDisplay) {
            destroyTexture(Minecraft.getInstance());
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
        if (FontRenderer.measureTextWidth(name, 13f) > 128f) {
            while (name.length() > 1 && FontRenderer.measureTextWidth(name + "...", 13f) > 128f) {
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
        renderBaseTexture(client, name, blurMode);
        renderOverlayTexture(client, speed, ringProgress, blurMode);
        if (blurMode) {
            SkiaBlurRenderer.getInstance().render(client, drawX, drawY, drawW, drawH, drawRadius, Config.skiaBlurTintColor(), Config.skiaBlurStrength);
        }

        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, cy);
        graphics.pose().scale(drawScale, drawScale);
        graphics.pose().translate(-cx, -cy);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_ID, Math.round(x), Math.round(y), 0f, 0f, Math.round(scaledW), Math.round(scaledH), textureW, textureH, textureW, textureH);
        graphics.blit(RenderPipelines.GUI_TEXTURED, OVERLAY_TEXTURE_ID, Math.round(x), Math.round(y), 0f, 0f, Math.round(scaledW), Math.round(scaledH), overlayTextureW, overlayTextureH, overlayTextureW, overlayTextureH);

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

    public void renderFrameEnd() {
        // Kept for the shared frame-end hook. BlockCount renders through GuiGraphics to keep item layering correct.
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

    private void renderBaseTexture(Minecraft client, String name, boolean blurMode) {
        ensureNativeLoaded();
        float userScale = Math.max(0.5f, Config.blockCountDisplayScale);
        float targetScale = Math.max(1f, (float) client.getWindow().getGuiScale() * userScale);
        int targetW = Math.max(1, Math.round(WIDTH * targetScale));
        int targetH = Math.max(1, Math.round(HEIGHT * targetScale));
        if (dynamicTexture != null && targetW == textureW && targetH == textureH && name.equals(lastTextureName) && lastTextureTheme == Config.hudTheme && lastTextureBlurMode == blurMode) return;

        if (surface == null || dynamicTexture == null || targetW != textureW || targetH != textureH) {
            destroyBaseTexture(client);
            surface = Surface.makeRaster(new ImageInfo(new ColorInfo(ColorType.RGBA_8888, ColorAlphaType.UNPREMUL, null), targetW, targetH), 0, SURFACE_PROPS);
            dynamicTexture = new DynamicTexture("pvp_utils:block_count_display", targetW, targetH, false);
            client.getTextureManager().register(TEXTURE_ID, dynamicTexture);
            textureW = targetW;
            textureH = targetH;
            textureScale = targetScale;
            lastTextureName = "";
        }

        Canvas c = surface.getCanvas();
        c.restoreToCount(1);
        c.resetMatrix();
        c.clear(0x00000000);
        c.save();
        c.scale(textureScale, textureScale);
        if (!blurMode) {
            bgPaint.setColor(newCardColor());
            c.drawRRect(RRect.makeXYWH(0f, 0f, WIDTH, HEIGHT, 16f), bgPaint);
        }
        FontRenderer.drawText(c, name, 16f, 22f, 12f, primaryTextColor(blurMode));
        c.restore();
        uploadSurface(surface, dynamicTexture, textureW, textureH);
        lastTextureName = name;
        lastTextureTheme = Config.hudTheme;
        lastTextureBlurMode = blurMode;
    }

    private void renderOverlayTexture(Minecraft client, String speed, float progress, boolean blurMode) {
        ensureNativeLoaded();
        float userScale = Math.max(0.5f, Config.blockCountDisplayScale);
        float targetScale = Math.max(1f, (float) client.getWindow().getGuiScale() * userScale);
        int targetW = Math.max(1, Math.round(WIDTH * targetScale));
        int targetH = Math.max(1, Math.round(HEIGHT * targetScale));
        int progressKey = Math.round(progress * 48f);
        if (overlayTexture != null && targetW == overlayTextureW && targetH == overlayTextureH && speed.equals(lastTextureSpeed) && progressKey == lastTextureProgress && lastOverlayTextureTheme == Config.hudTheme && lastOverlayTextureBlurMode == blurMode) return;

        if (overlaySurface == null || overlayTexture == null || targetW != overlayTextureW || targetH != overlayTextureH) {
            destroyOverlayTexture(client);
            overlaySurface = Surface.makeRaster(new ImageInfo(new ColorInfo(ColorType.RGBA_8888, ColorAlphaType.UNPREMUL, null), targetW, targetH), 0, SURFACE_PROPS);
            overlayTexture = new DynamicTexture("pvp_utils:block_count_display_overlay", targetW, targetH, false);
            client.getTextureManager().register(OVERLAY_TEXTURE_ID, overlayTexture);
            overlayTextureW = targetW;
            overlayTextureH = targetH;
        }

        Canvas c = overlaySurface.getCanvas();
        c.restoreToCount(1);
        c.resetMatrix();
        c.clear(0x00000000);
        c.save();
        c.scale(targetScale, targetScale);
        FontRenderer.drawText(c, speed, 16f, 40f, 11f, mutedTextColor(blurMode));
        float ringCx = WIDTH - 32f;
        float ringCy = HEIGHT * 0.5f;
        float radius = 17f;
        ringFillPaint.setColor(ringFillColor(blurMode));
        c.drawCircle(ringCx, ringCy, radius + 5f, ringFillPaint);
        ringTrackPaint.setColor(ringTrackColor(blurMode));
        c.drawCircle(ringCx, ringCy, radius, ringTrackPaint);
        ringArcPaint.setColor((int) PURPLE);
        c.drawArc(ringCx - radius, ringCy - radius, ringCx + radius, ringCy + radius, -90f, -360f * progress, false, ringArcPaint);
        c.restore();
        uploadSurface(overlaySurface, overlayTexture, overlayTextureW, overlayTextureH);
        lastTextureSpeed = speed;
        lastTextureProgress = progressKey;
        lastOverlayTextureTheme = Config.hudTheme;
        lastOverlayTextureBlurMode = blurMode;
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

    private void ensureNativeLoaded() {
        if (nativeLoaded) return;
        Library.load();
        nativeLoaded = true;
    }

    private void uploadSurface(Surface sourceSurface, DynamicTexture targetTexture, int width, int height) {
        Pixmap pixmap = new Pixmap();
        try {
            if (!sourceSurface.peekPixels(pixmap)) return;
            long addr = pixmap.getAddr();
            int byteSize = height * pixmap.getRowBytes();
            ByteBuffer buf = MemoryUtil.memByteBuffer(addr, byteSize);
            GpuTexture gpuTexture = targetTexture.getTexture();
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToTexture(gpuTexture, buf, NativeImage.Format.RGBA, 0, 0, 0, 0, width, height);
        } finally {
            pixmap.close();
        }
    }

    private void destroyBaseTexture(Minecraft client) {
        if (surface != null) {
            surface.close();
            surface = null;
        }
        if (dynamicTexture != null) {
            client.getTextureManager().release(TEXTURE_ID);
            dynamicTexture = null;
        }
        textureW = -1;
        textureH = -1;
        textureScale = -1f;
        lastTextureName = "";
        lastTextureBlurMode = false;
        lastTextureTheme = null;
    }

    private void destroyOverlayTexture(Minecraft client) {
        if (overlaySurface != null) {
            overlaySurface.close();
            overlaySurface = null;
        }
        if (overlayTexture != null) {
            client.getTextureManager().release(OVERLAY_TEXTURE_ID);
            overlayTexture = null;
        }
        overlayTextureW = -1;
        overlayTextureH = -1;
        lastTextureSpeed = "";
        lastTextureProgress = -1;
        lastOverlayTextureBlurMode = false;
        lastOverlayTextureTheme = null;
    }

    private void destroyTexture(Minecraft client) {
        destroyBaseTexture(client);
        destroyOverlayTexture(client);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }
}
