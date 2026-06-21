package com.pvp_utils.client.modules.impl.Render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.pvp_utils.Config;
import com.pvp_utils.client.render.font.FontRenderer;
import io.github.humbleui.skija.*;
import io.github.humbleui.skija.impl.Library;
import io.github.humbleui.types.RRect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class ArmorHudRenderer {
    private static final ArmorHudRenderer INSTANCE = new ArmorHudRenderer();
    private static final float CARD_W = 48f;
    private static final float CARD_H = 20f;
    private static final float GAP = 3f;
    private static final float HOTBAR_W = 182f;
    private static final float SIDE_GAP = 4f;
    private static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath("pvp_utils", "armor_hud");
    private static final SurfaceProps SURFACE_PROPS = new SurfaceProps(false, PixelGeometry.RGB_H);

    private final Paint bgPaint = new Paint().setAntiAlias(true);
    private final Paint borderPaint = new Paint().setAntiAlias(true).setMode(PaintMode.STROKE).setStrokeWidth(1f);
    private final Paint barTrackPaint = new Paint().setAntiAlias(true);
    private final Paint barFillPaint = new Paint().setAntiAlias(true);
    private Surface surface;
    private DynamicTexture dynamicTexture;
    private boolean nativeLoaded = false;
    private int textureW = -1;
    private int textureH = -1;
    private float textureScale = -1f;
    private String lastSignature = "";

    public static ArmorHudRenderer getInstance() {
        return INSTANCE;
    }

    public void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        if (!Config.armorHud) {
            destroyTexture(client);
            return;
        }
        if (client.player == null || client.level == null || client.options.hideGui || client.screen instanceof com.pvp_utils.client.render.skia.SkiaScreen) return;

        ItemStack head = client.player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = client.player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = client.player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack feet = client.player.getItemBySlot(EquipmentSlot.FEET);
        if (head.isEmpty() && chest.isEmpty() && legs.isEmpty() && feet.isEmpty()) return;

        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        Layout layout = makeLayout(screenW, screenH);
        if (!renderTexture(client, layout, head, chest, legs, feet)) return;

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_ID, Math.round(layout.x), Math.round(layout.y), 0f, 0f,
                Math.round(layout.w), Math.round(layout.h), textureW, textureH, textureW, textureH);
        renderItem(graphics, head, layout.leftX, layout.topY);
        renderItem(graphics, chest, layout.leftX, layout.bottomY);
        renderItem(graphics, legs, layout.rightX, layout.topY);
        renderItem(graphics, feet, layout.rightX, layout.bottomY);
    }

    private Layout makeLayout(int screenW, int screenH) {
        float hotbarX = (screenW - HOTBAR_W) * 0.5f;
        float hotbarY = screenH - 22f;
        float topY = Math.max(0f, hotbarY - CARD_H * 2f - GAP - 3f);
        float bottomY = topY + CARD_H + GAP;
        float leftX = clamp(hotbarX - CARD_W - SIDE_GAP, 0f, Math.max(0f, screenW - CARD_W));
        float rightX = clamp(hotbarX + HOTBAR_W + SIDE_GAP, 0f, Math.max(0f, screenW - CARD_W));
        float x = Math.min(leftX, rightX);
        float y = topY;
        float w = Math.max(leftX, rightX) + CARD_W - x;
        float h = CARD_H * 2f + GAP;
        return new Layout(x, y, w, h, leftX, rightX, topY, bottomY);
    }

    private boolean renderTexture(Minecraft client, Layout layout, ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet) {
        ensureNativeLoaded();
        float targetScale = Math.max(1f, (float) client.getWindow().getGuiScale());
        int targetW = Math.max(1, Math.round(layout.w * targetScale));
        int targetH = Math.max(1, Math.round(layout.h * targetScale));
        String signature = targetW + "x" + targetH + '@' + Float.floatToIntBits(targetScale) + ':'
                + Math.round((layout.leftX - layout.x) * 10f) + ':' + Math.round((layout.rightX - layout.x) * 10f) + ':'
                + stackSignature(head) + '|' + stackSignature(chest) + '|' + stackSignature(legs) + '|' + stackSignature(feet);
        if (dynamicTexture != null && targetW == textureW && targetH == textureH && signature.equals(lastSignature)) return true;

        if (surface == null || dynamicTexture == null || targetW != textureW || targetH != textureH) {
            destroyTexture(client);
            surface = Surface.makeRaster(new ImageInfo(new ColorInfo(ColorType.RGBA_8888, ColorAlphaType.UNPREMUL, null), targetW, targetH), 0, SURFACE_PROPS);
            dynamicTexture = new DynamicTexture("pvp_utils:armor_hud", targetW, targetH, false);
            client.getTextureManager().register(TEXTURE_ID, dynamicTexture);
            textureW = targetW;
            textureH = targetH;
            textureScale = targetScale;
            lastSignature = "";
        }

        Canvas canvas = surface.getCanvas();
        canvas.restoreToCount(1);
        canvas.resetMatrix();
        canvas.clear(0x00000000);
        canvas.save();
        canvas.scale(textureScale, textureScale);
        drawCard(canvas, head, layout.leftX - layout.x, layout.topY - layout.y);
        drawCard(canvas, chest, layout.leftX - layout.x, layout.bottomY - layout.y);
        drawCard(canvas, legs, layout.rightX - layout.x, layout.topY - layout.y);
        drawCard(canvas, feet, layout.rightX - layout.x, layout.bottomY - layout.y);
        canvas.restore();

        if (!uploadSurface(surface, dynamicTexture, textureW, textureH)) return false;
        lastSignature = signature;
        return true;
    }

    private void drawCard(Canvas canvas, ItemStack stack, float x, float y) {
        if (stack.isEmpty()) return;
        bgPaint.setColor(0x7A0E1117);
        canvas.drawRRect(RRect.makeXYWH(x, y, CARD_W, CARD_H, 6f), bgPaint);
        borderPaint.setColor(0x35FFFFFF);
        canvas.drawRRect(RRect.makeXYWH(x + 0.5f, y + 0.5f, CARD_W - 1f, CARD_H - 1f, 6f), borderPaint);

        int maxDamage = stack.getMaxDamage();
        if (!stack.isDamageableItem() || maxDamage <= 0) return;

        float ratio = clamp((maxDamage - stack.getDamageValue()) / (float) maxDamage, 0f, 1f);
        int percent = Math.round(ratio * 100f);
        String text = percent + "%";
        int color = durabilityColor(ratio);
        FontRenderer.drawText(canvas, text, x + 22f, y + 13f, 8.5f, 0xEFFFFFFF);

        float barX = x + 22f;
        float barY = y + 15.5f;
        float barW = 20f;
        barTrackPaint.setColor(0x33FFFFFF);
        canvas.drawRRect(RRect.makeXYWH(barX, barY, barW, 2f, 1f), barTrackPaint);
        barFillPaint.setColor(color);
        canvas.drawRRect(RRect.makeXYWH(barX, barY, Math.max(1f, barW * ratio), 2f, 1f), barFillPaint);
    }

    private void renderItem(GuiGraphics graphics, ItemStack stack, float cardX, float cardY) {
        if (stack.isEmpty()) return;
        graphics.renderFakeItem(stack, Math.round(cardX + 2f), Math.round(cardY + 2f));
    }

    private String stackSignature(ItemStack stack) {
        if (stack.isEmpty()) return "empty";
        return stack.getItem() + ":" + stack.getDamageValue() + ":" + stack.getMaxDamage();
    }

    private int durabilityColor(float ratio) {
        if (ratio > 0.55f) return 0xFF63E37D;
        if (ratio > 0.25f) return 0xFFFFD166;
        return 0xFFFF5C5C;
    }

    private void ensureNativeLoaded() {
        if (nativeLoaded) return;
        Library.load();
        nativeLoaded = true;
    }

    private boolean uploadSurface(Surface sourceSurface, DynamicTexture targetTexture, int width, int height) {
        Pixmap pixmap = new Pixmap();
        try {
            if (!sourceSurface.peekPixels(pixmap)) return false;
            long addr = pixmap.getAddr();
            int byteSize = height * pixmap.getRowBytes();
            ByteBuffer buf = MemoryUtil.memByteBuffer(addr, byteSize);
            GpuTexture gpuTexture = targetTexture.getTexture();
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToTexture(gpuTexture, buf, NativeImage.Format.RGBA, 0, 0, 0, 0, width, height);
            return true;
        } finally {
            pixmap.close();
        }
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
        textureW = -1;
        textureH = -1;
        textureScale = -1f;
        lastSignature = "";
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }

    private record Layout(float x, float y, float w, float h, float leftX, float rightX, float topY, float bottomY) {}
}
