package com.pvp_utils.client.modules.impl.Render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.pvp_utils.Config;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaScreen;
import io.github.humbleui.skija.*;
import io.github.humbleui.skija.impl.Library;
import io.github.humbleui.types.RRect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PotionStatusRenderer {
    private static final PotionStatusRenderer INSTANCE = new PotionStatusRenderer();
    private static final float WIDTH = 112f;
    private static final float ROW_H = 27f;
    private static final float GAP = 4f;
    private static final int MAX_EFFECTS = 8;
    private static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath("pvp_utils", "potion_status");
    private static final SurfaceProps SURFACE_PROPS = new SurfaceProps(false, PixelGeometry.RGB_H);

    private final Paint bgPaint = new Paint().setAntiAlias(true);
    private final Paint borderPaint = new Paint().setAntiAlias(true).setMode(PaintMode.STROKE).setStrokeWidth(1f);
    private final Paint dotPaint = new Paint().setAntiAlias(true);
    private Surface surface;
    private DynamicTexture dynamicTexture;
    private boolean nativeLoaded = false;
    private int textureW = -1;
    private int textureH = -1;
    private float textureScale = -1f;
    private String lastSignature = "";

    public static PotionStatusRenderer getInstance() {
        return INSTANCE;
    }

    public void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        if (!Config.potionStatus) {
            destroyTexture(client);
            return;
        }
        if (client.player == null || client.level == null || client.options.hideGui || client.screen instanceof SkiaScreen) return;

        List<MobEffectInstance> effects = HudEditOverlay.getInstance().isActive() ? previewEffects() : visibleEffects(client);
        if (effects.isEmpty()) return;

        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        float scale = getScale();
        float w = WIDTH * scale;
        float h = getBaseHeight(effects.size()) * scale;
        float x = getRenderX(screenW, w);
        float y = getRenderY(screenH, h);

        if (!renderTexture(client, effects, scale)) return;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_ID, Math.round(x), Math.round(y), 0f, 0f,
                Math.round(w), Math.round(h), textureW, textureH, textureW, textureH);
    }

    public float getEditWidth() {
        return WIDTH * getScale();
    }

    public float getEditHeight() {
        Minecraft client = Minecraft.getInstance();
        int count = client.player == null ? 3 : Math.max(3, Math.min(MAX_EFFECTS, visibleEffects(client).size()));
        return getBaseHeight(count) * getScale();
    }

    public float getDefaultX() {
        return 8f;
    }

    public float getDefaultY(int screenH) {
        return (screenH - getEditHeight()) * 0.5f;
    }

    public float getRenderX(int screenW) {
        return getRenderX(screenW, getEditWidth());
    }

    public float getRenderY(int screenH) {
        return getRenderY(screenH, getEditHeight());
    }

    private float getRenderX(int screenW, float w) {
        return clamp(getDefaultX() + Config.potionStatusX, 0f, Math.max(0f, screenW - w));
    }

    private float getRenderY(int screenH, float h) {
        return clamp((screenH - h) * 0.5f + Config.potionStatusY, 0f, Math.max(0f, screenH - h));
    }

    private List<MobEffectInstance> visibleEffects(Minecraft client) {
        ArrayList<MobEffectInstance> effects = new ArrayList<>();
        if (client.player == null) return effects;
        for (MobEffectInstance effect : client.player.getActiveEffects()) {
            if (!effect.isVisible()) continue;
            effects.add(effect);
        }
        effects.sort(Comparator.comparing(MobEffectInstance::getDescriptionId));
        if (effects.size() > MAX_EFFECTS) return new ArrayList<>(effects.subList(0, MAX_EFFECTS));
        return effects;
    }

    private List<MobEffectInstance> previewEffects() {
        ArrayList<MobEffectInstance> effects = new ArrayList<>(3);
        effects.add(new MobEffectInstance(MobEffects.SPEED, 3 * 60 * 20, 1));
        effects.add(new MobEffectInstance(MobEffects.STRENGTH, 90 * 20, 0));
        effects.add(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 8 * 60 * 20, 0));
        return effects;
    }

    private boolean renderTexture(Minecraft client, List<MobEffectInstance> effects, float userScale) {
        ensureNativeLoaded();
        float targetScale = Math.max(1f, (float) client.getWindow().getGuiScale() * userScale);
        float baseH = getBaseHeight(effects.size());
        int targetW = Math.max(1, Math.round(WIDTH * targetScale));
        int targetH = Math.max(1, Math.round(baseH * targetScale));
        String signature = makeSignature(effects, targetW, targetH, targetScale);
        if (dynamicTexture != null && targetW == textureW && targetH == textureH && signature.equals(lastSignature)) return true;

        if (surface == null || dynamicTexture == null || targetW != textureW || targetH != textureH) {
            destroyTexture(client);
            surface = Surface.makeRaster(new ImageInfo(new ColorInfo(ColorType.RGBA_8888, ColorAlphaType.UNPREMUL, null), targetW, targetH), 0, SURFACE_PROPS);
            dynamicTexture = new DynamicTexture("pvp_utils:potion_status", targetW, targetH, false);
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
        for (int i = 0; i < effects.size(); i++) {
            drawEffect(canvas, effects.get(i), i * (ROW_H + GAP));
        }
        canvas.restore();

        if (!uploadSurface(surface, dynamicTexture, textureW, textureH)) return false;
        lastSignature = signature;
        return true;
    }

    private void drawEffect(Canvas canvas, MobEffectInstance effect, float y) {
        int effectColor = 0xFF000000 | (effect.getEffect().value().getColor() & 0xFFFFFF);
        bgPaint.setColor(0x7A0E1117);
        canvas.drawRRect(RRect.makeXYWH(0f, y, WIDTH, ROW_H, 7f), bgPaint);
        borderPaint.setColor(0x30FFFFFF);
        canvas.drawRRect(RRect.makeXYWH(0.5f, y + 0.5f, WIDTH - 1f, ROW_H - 1f, 7f), borderPaint);

        dotPaint.setColor(withAlpha(effectColor, 0.26f));
        canvas.drawCircle(13f, y + ROW_H * 0.5f, 8f, dotPaint);
        dotPaint.setColor(effectColor);
        canvas.drawCircle(13f, y + ROW_H * 0.5f, 4f, dotPaint);

        String name = truncate(Component.translatable(effect.getDescriptionId()).getString(), 56f, 10f);
        String time = formatDuration(effect);
        String amp = amplifier(effect.getAmplifier());
        FontRenderer.drawText(canvas, name, 26f, y + 11f, 10f, 0xEFFFFFFF);
        FontRenderer.drawText(canvas, amp.isEmpty() ? time : amp + "  " + time, 26f, y + 22f, 8f, 0xB8FFFFFF);
    }

    private String makeSignature(List<MobEffectInstance> effects, int targetW, int targetH, float targetScale) {
        StringBuilder builder = new StringBuilder(96);
        builder.append(targetW).append('x').append(targetH).append('@').append(Float.floatToIntBits(targetScale));
        for (MobEffectInstance effect : effects) {
            builder.append('|').append(effect.getDescriptionId())
                    .append(':').append(effect.getAmplifier())
                    .append(':').append(effect.getDuration() / 20)
                    .append(':').append(effect.getEffect().value().getColor());
        }
        return builder.toString();
    }

    private String formatDuration(MobEffectInstance effect) {
        if (effect.isInfiniteDuration()) return "inf";
        int totalSeconds = Math.max(0, effect.getDuration() / 20);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        if (minutes >= 60) return (minutes / 60) + "h";
        return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
    }

    private String amplifier(int amplifier) {
        return switch (amplifier) {
            case 0 -> "";
            case 1 -> "II";
            case 2 -> "III";
            case 3 -> "IV";
            case 4 -> "V";
            default -> String.valueOf(amplifier + 1);
        };
    }

    private String truncate(String text, float maxWidth, float size) {
        if (FontRenderer.measureTextWidth(text, size) <= maxWidth) return text;
        while (text.length() > 1 && FontRenderer.measureTextWidth(text + "...", size) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "...";
    }

    private float getBaseHeight(int count) {
        return count * ROW_H + Math.max(0, count - 1) * GAP;
    }

    private float getScale() {
        return Math.max(0.5f, Config.potionStatusScale);
    }

    private int withAlpha(int color, float alpha) {
        int a = Math.round(((color >>> 24) & 0xFF) * clamp(alpha, 0f, 1f));
        return (color & 0x00FFFFFF) | (a << 24);
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
}
