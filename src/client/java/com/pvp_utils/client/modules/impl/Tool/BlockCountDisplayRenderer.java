package com.pvp_utils.client.modules.impl.Tool;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Render.HudEditOverlay;
import com.pvp_utils.client.render.font.FontRenderer;
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
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

public class BlockCountDisplayRenderer {
    private static final BlockCountDisplayRenderer INSTANCE = new BlockCountDisplayRenderer();
    private static final long STAY_MS = 900L;
    private static final long SPEED_WINDOW_MS = 1000L;
    private static final float WIDTH = 190f;
    private static final float HEIGHT = 58f;
    private static final float PURPLE = 0xFF8F5CFF;
    private static final float MAX_BPS = 30f;
    private static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath("pvp_utils", "block_count_display");

    private final Deque<Long> clicks = new ArrayDeque<>();
    private Surface surface;
    private DynamicTexture dynamicTexture;
    private boolean wasUseDown = false;
    private boolean visible = false;
    private boolean nativeLoaded = false;
    private float scale = 0f;
    private float ringProgress = 0f;
    private String lastTextureName = "";
    private String lastTextureSpeed = "";
    private int lastTextureProgress = -1;
    private int textureW = -1;
    private int textureH = -1;
    private float textureScale = -1f;
    private double lastX = Double.NaN;
    private double lastZ = Double.NaN;
    private long lastMoveSampleMs = 0L;
    private float bps = 0f;
    private long lastInteractionMs = 0L;
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
        return WIDTH;
    }

    public float getEditHeight() {
        return HEIGHT;
    }

    public float getDefaultY(int screenH) {
        return screenH - 112f;
    }

    public float getRenderX(int screenW) {
        return clamp((screenW - WIDTH) * 0.5f + Config.blockCountDisplayX, 0f, Math.max(0f, screenW - WIDTH));
    }

    public float getRenderY(int screenH) {
        return clamp(getDefaultY(screenH) + Config.blockCountDisplayY, 0f, Math.max(0f, screenH - HEIGHT));
    }

    public void tick(Minecraft client) {
        if (!Config.blockCountDisplay) {
            reset();
            return;
        }
        if (HudEditOverlay.getInstance().isActive()) return;

        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.screen != null) {
            close();
            wasUseDown = false;
            return;
        }

        ItemStack stack = player.getMainHandItem();
        boolean block = stack.getItem() instanceof BlockItem;
        boolean useDown = client.options.keyUse.isDown() || GLFW.glfwGetMouseButton(client.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        long now = System.currentTimeMillis();
        updateBps(player, now);

        if (block && useDown && !wasUseDown) {
            clicks.addLast(now);
            visible = true;
            lastInteractionMs = now;
            if (scale <= 0.01f) ringProgress = 0f;
        } else if (block && useDown) {
            visible = true;
            lastInteractionMs = now;
        }
        wasUseDown = useDown;

        if (!block) {
            close();
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
        trim(clicks, now);
    }

    public void triggerUse(Minecraft client) {
        if (!Config.blockCountDisplay || HudEditOverlay.getInstance().isActive()) return;
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.screen != null) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof BlockItem)) return;

        long now = System.currentTimeMillis();
        clicks.addLast(now);
        visible = true;
        lastInteractionMs = now;
        if (scale <= 0.01f) ringProgress = 0f;
        lastSlot = player.getInventory().getSelectedSlot();
        lastCount = stack.getCount();
        displayStack = stack.copy();
        trim(clicks, now);
    }

    public void render(GuiGraphics graphics, Canvas canvas) {
        if (!Config.blockCountDisplay) {
            destroyTexture(Minecraft.getInstance());
            reset();
            return;
        }

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        boolean editActive = HudEditOverlay.getInstance().isActive();
        if (player == null || client.level == null || (client.screen != null && !editActive)) {
            close();
            updateScale();
            return;
        }

        ItemStack stack = player.getMainHandItem();
        boolean block = stack.getItem() instanceof BlockItem;
        long now = System.currentTimeMillis();

        if (editActive) {
            visible = true;
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

        trim(clicks, now);
        updateScale();
        if (scale <= 0.01f || displayStack.isEmpty()) return;

        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        float x = getRenderX(screenW);
        float y = getRenderY(screenH);
        float cx = x + WIDTH * 0.5f;
        float cy = y + HEIGHT * 0.5f;
        float drawScale = easeOutBack(scale);

        String name = displayStack.getHoverName().getString();
        if (FontRenderer.measureTextWidth(name, 13f) > 128f) {
            while (name.length() > 1 && FontRenderer.measureTextWidth(name + "...", 13f) > 128f) {
                name = name.substring(0, name.length() - 1);
            }
            name += "...";
        }
        String speed = String.format(Locale.ROOT, "%.1fBPS\\%dCPS", bps, clicks.size());

        float ringCx = x + WIDTH - 32f;
        float ringCy = y + HEIGHT * 0.5f;
        float radius = 19f;
        float ratio = Math.max(0f, Math.min(1f, displayStack.getCount() / (float) Math.max(1, displayStack.getMaxStackSize())));
        ringProgress += (ratio - ringProgress) * 0.18f;

        renderTexture(client, name, speed, ringProgress);

        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, cy);
        graphics.pose().scale(drawScale, drawScale);
        graphics.pose().translate(-cx, -cy);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_ID, Math.round(x), Math.round(y), 0f, 0f, Math.round(WIDTH), Math.round(HEIGHT), textureW, textureH, textureW, textureH);

        int itemX = Math.round(ringCx - 8f);
        int itemY = Math.round(ringCy - 8f);
        float iconScale = 0.35f + 0.65f * easeOutCubic(scale);
        graphics.pose().translate(ringCx, ringCy);
        graphics.pose().scale(iconScale, iconScale);
        graphics.pose().translate(-ringCx, -ringCy);
        graphics.renderFakeItem(displayStack, itemX, itemY);
        graphics.renderItemDecorations(client.font, displayStack, itemX, itemY);
        graphics.pose().popMatrix();
    }

    private void updateScale() {
        float target = visible ? 1f : 0f;
        scale += (target - scale) * 0.12f;
        if (!visible && scale < 0.01f) {
            scale = 0f;
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

    private void updateBps(LocalPlayer player, long now) {
        if (Double.isNaN(lastX) || Double.isNaN(lastZ) || lastMoveSampleMs == 0L) {
            lastX = player.getX();
            lastZ = player.getZ();
            lastMoveSampleMs = now;
            return;
        }
        long elapsedMs = now - lastMoveSampleMs;
        if (elapsedMs < 35L) return;
        double dx = player.getX() - lastX;
        double dz = player.getZ() - lastZ;
        float instant = (float) (Math.sqrt(dx * dx + dz * dz) / (elapsedMs / 1000.0));
        instant = Math.max(0f, Math.min(MAX_BPS, instant));
        bps = bps <= 0f ? instant : bps * 0.75f + instant * 0.25f;
        lastX = player.getX();
        lastZ = player.getZ();
        lastMoveSampleMs = now;
    }

    private void close() {
        visible = false;
        lastSlot = -1;
        lastCount = -1;
    }

    private void reset() {
        visible = false;
        scale = 0f;
        wasUseDown = false;
        lastInteractionMs = 0L;
        ringProgress = 0f;
        lastX = Double.NaN;
        lastZ = Double.NaN;
        lastMoveSampleMs = 0L;
        bps = 0f;
        lastSlot = -1;
        lastCount = -1;
        displayStack = ItemStack.EMPTY;
        clicks.clear();
    }

    private void trim(Deque<Long> queue, long now) {
        while (!queue.isEmpty() && now - queue.peekFirst() > SPEED_WINDOW_MS) {
            queue.removeFirst();
        }
    }

    private void renderTexture(Minecraft client, String name, String speed, float progress) {
        ensureNativeLoaded();
        float targetScale = Math.max(2f, (float) client.getWindow().getGuiScale());
        int targetW = Math.max(1, Math.round(WIDTH * targetScale));
        int targetH = Math.max(1, Math.round(HEIGHT * targetScale));
        int progressKey = Math.round(progress * 120f);
        if (dynamicTexture != null && targetW == textureW && targetH == textureH && name.equals(lastTextureName) && speed.equals(lastTextureSpeed) && progressKey == lastTextureProgress) return;

        if (surface == null || dynamicTexture == null || targetW != textureW || targetH != textureH) {
            destroyTexture(client);
            SurfaceProps props = new SurfaceProps(false, PixelGeometry.RGB_H);
            surface = Surface.makeRaster(new ImageInfo(new ColorInfo(ColorType.RGBA_8888, ColorAlphaType.UNPREMUL, null), targetW, targetH), 0, props);
            dynamicTexture = new DynamicTexture("pvp_utils:block_count_display", targetW, targetH, false);
            client.getTextureManager().register(TEXTURE_ID, dynamicTexture);
            textureW = targetW;
            textureH = targetH;
            textureScale = targetScale;
            lastTextureName = "";
            lastTextureSpeed = "";
            lastTextureProgress = -1;
        }

        Canvas c = surface.getCanvas();
        c.restoreToCount(1);
        c.resetMatrix();
        c.clear(0x00000000);
        c.save();
        c.scale(textureScale, textureScale);

        try (Paint bg = new Paint()) {
            bg.setColor(0xF2FFFFFF);
            bg.setAntiAlias(true);
            c.drawRRect(RRect.makeXYWH(0f, 0f, WIDTH, HEIGHT, 16f), bg);
        }

        FontRenderer.drawText(c, name, 16f, 22f, 12f, 0xFF202027);
        FontRenderer.drawText(c, speed, 16f, 40f, 11f, 0xFF5C5870);

        float ringCx = WIDTH - 32f;
        float ringCy = HEIGHT * 0.5f;
        float radius = 17f;
        try (Paint fill = new Paint()) {
            fill.setColor(0x1F8F5CFF);
            fill.setAntiAlias(true);
            c.drawCircle(ringCx, ringCy, radius + 5f, fill);
        }
        try (Paint track = new Paint()) {
            track.setColor(0x338F5CFF);
            track.setAntiAlias(true);
            track.setMode(PaintMode.STROKE);
            track.setStrokeWidth(4f);
            c.drawCircle(ringCx, ringCy, radius, track);
        }
        try (Paint arc = new Paint()) {
            arc.setColor((int) PURPLE);
            arc.setAntiAlias(true);
            arc.setMode(PaintMode.STROKE);
            arc.setStrokeWidth(4f);
            c.drawArc(ringCx - radius, ringCy - radius, ringCx + radius, ringCy + radius, -90f, -360f * progress, false, arc);
        }

        c.restore();
        Pixmap pixmap = new Pixmap();
        if (!surface.peekPixels(pixmap)) {
            pixmap.close();
            return;
        }
        long addr = pixmap.getAddr();
        int byteSize = textureH * pixmap.getRowBytes();
        ByteBuffer buf = MemoryUtil.memByteBuffer(addr, byteSize);
        GpuTexture gpuTexture = dynamicTexture.getTexture();
        RenderSystem.getDevice().createCommandEncoder()
                .writeToTexture(gpuTexture, buf, NativeImage.Format.RGBA, 0, 0, 0, 0, textureW, textureH);
        pixmap.close();
        lastTextureName = name;
        lastTextureSpeed = speed;
        lastTextureProgress = progressKey;
    }

    private void ensureNativeLoaded() {
        if (nativeLoaded) return;
        Library.load();
        nativeLoaded = true;
    }

    private void destroyTexture(Minecraft client) {
        if (surface != null) {
            surface.close();
            surface = null;
        }
        if (dynamicTexture != null) {
            client.getTextureManager().release(TEXTURE_ID);
            dynamicTexture = null;
        }
        lastTextureName = "";
        lastTextureSpeed = "";
        lastTextureProgress = -1;
        textureW = -1;
        textureH = -1;
        textureScale = -1f;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }
}
