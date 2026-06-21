package com.pvp_utils.client.modules.impl.Render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.pvp_utils.Config;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaScreen;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorAlphaType;
import io.github.humbleui.skija.ColorInfo;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.PixelGeometry;
import io.github.humbleui.skija.Pixmap;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.SurfaceProps;
import io.github.humbleui.skija.impl.Library;
import io.github.humbleui.types.RRect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PotionStatusRenderer {
    private static final PotionStatusRenderer INSTANCE = new PotionStatusRenderer();
    private static final float WIDTH = 134f;
    private static final float ROW_H = 28f;
    private static final float GAP = 5f;
    private static final float PAD = 7f;
    private static final float V_PAD = 7f;
    private static final float ICON = 18f;
    private static final float BG_RADIUS = 11f;
    private static final float ITEM_RADIUS = 8f;
    private static final int MAX_EFFECTS = 64;
    private static final long HIDE_DELAY_MS = 260L;
    private static final long SIGNATURE_TIME_STEP_TICKS = 5L;
    private static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath("pvp_utils", "potion_status");
    private static final Identifier OVERLAY_TEXTURE_ID = Identifier.fromNamespaceAndPath("pvp_utils", "potion_status_overlay");
    private static final SurfaceProps SURFACE_PROPS = new SurfaceProps(false, PixelGeometry.RGB_H);

    private final Paint bgPaint = new Paint().setAntiAlias(true);
    private final Paint itemPaint = new Paint().setAntiAlias(true);
    private final Paint itemFillPaint = new Paint().setAntiAlias(true);
    private final Paint itemOverlayPaint = new Paint().setAntiAlias(true);
    private final Paint borderPaint = new Paint().setAntiAlias(true).setMode(PaintMode.STROKE).setStrokeWidth(1f);
    private final Map<String, EffectVisual> visuals = new HashMap<>();
    private Surface surface;
    private Surface overlaySurface;
    private DynamicTexture dynamicTexture;
    private DynamicTexture overlayTexture;
    private boolean nativeLoaded = false;
    private int textureW = -1;
    private int textureH = -1;
    private int overlayTextureW = -1;
    private int overlayTextureH = -1;
    private float textureScale = -1f;
    private String lastBaseSignature = "";
    private String lastOverlaySignature = "";
    private long lastFrameMs = 0L;

    public static PotionStatusRenderer getInstance() {
        return INSTANCE;
    }

    public void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        if (!Config.potionStatus) {
            destroyTexture(client);
            visuals.clear();
            lastFrameMs = 0L;
            return;
        }
        if (client.player == null || client.level == null || client.options.hideGui || client.screen instanceof SkiaScreen) return;

        List<MobEffectInstance> effects = HudEditOverlay.getInstance().isActive() ? previewEffects() : visibleEffects(client);
        long now = System.currentTimeMillis();
        float dt = lastFrameMs == 0L ? 0.016f : Math.min((now - lastFrameMs) / 1000f, 0.05f);
        lastFrameMs = now;
        List<EffectVisual> renderVisuals = updateVisuals(effects, dt, now);
        if (renderVisuals.isEmpty()) {
            destroyTexture(client);
            return;
        }

        float baseH = getAnimatedHeight(renderVisuals);
        float bgProgress = getBackgroundProgress(renderVisuals);
        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        float scale = getScale();
        float w = WIDTH * scale;
        float h = baseH * scale;
        float x = getRenderX(screenW, WIDTH * scale);
        float y = getRenderY(screenH, Math.max(h, ROW_H * scale));

        if (!renderTexture(client, renderVisuals, scale, bgProgress, baseH)) return;
        float easedBg = easeOutCubic(bgProgress);
        float drawW = WIDTH * scale * easedBg;
        float drawH = baseH * scale * easedBg;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(easedBg, easedBg);
        graphics.pose().translate(-x, -y);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_ID, Math.round(x), Math.round(y), 0f, 0f,
                Math.round(WIDTH * scale), Math.round(baseH * scale), textureW, textureH, textureW, textureH);
        graphics.blit(RenderPipelines.GUI_TEXTURED, OVERLAY_TEXTURE_ID, Math.round(x), Math.round(y), 0f, 0f,
                Math.round(WIDTH * scale), Math.round(baseH * scale), overlayTextureW, overlayTextureH, overlayTextureW, overlayTextureH);
        renderIcons(graphics, client, renderVisuals, x, y, scale, easedBg);
        graphics.pose().popMatrix();
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

    public boolean shouldHideVanillaEffects() {
        if (!Config.potionStatus || !Config.potionStatusHideVanilla) return false;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.options.hideGui || client.screen instanceof SkiaScreen) return false;
        return HudEditOverlay.getInstance().isActive() || !visibleEffects(client).isEmpty();
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
        return effects;
    }

    private List<MobEffectInstance> previewEffects() {
        ArrayList<MobEffectInstance> effects = new ArrayList<>(3);
        effects.add(new MobEffectInstance(MobEffects.SPEED, 3 * 60 * 20, 1));
        effects.add(new MobEffectInstance(MobEffects.STRENGTH, 90 * 20, 0));
        effects.add(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 8 * 60 * 20, 0));
        return effects;
    }

    private List<EffectVisual> updateVisuals(List<MobEffectInstance> effects, float dt, long now) {
        Map<String, MobEffectInstance> current = new HashMap<>();
        int visibleCount = effects.size();
        for (int i = 0; i < effects.size(); i++) {
            MobEffectInstance effect = effects.get(i);
            String key = keyOf(effect);
            current.put(key, effect);
            EffectVisual visual = visuals.computeIfAbsent(key, unused -> new EffectVisual());
            visual.key = key;
            visual.effect = effect;
            visual.targetIndex = i;
            visual.targetY = V_PAD + i * (ROW_H + GAP);
            visual.visible = true;
            visual.exiting = false;
            visual.progress = Math.min(1f, visual.progress + dt * 9f);
            visual.slide = Math.min(1f, visual.slide + dt * 10f);
            visual.rowAlpha = Math.min(1f, visual.rowAlpha + dt * 10f);
            visual.fillDelay = Math.min(1f, visual.fillDelay + dt * 8f);
            if (visual.displayDurationTicks < 0f) {
                visual.displayDurationTicks = effect.getDuration();
            }
            float remain = effect.isInfiniteDuration() ? 1f : Mth.clamp(effect.getDuration() / Math.max(1f, visual.maxDuration), 0f, 1f);
            float targetFill = remain * easeOutCubic(visual.fillDelay);
            float fillSpeed = targetFill < visual.fillProgress ? 6.5f : 10f;
            visual.fillProgress += (targetFill - visual.fillProgress) * Math.min(1f, dt * fillSpeed);
            if (effect.isInfiniteDuration()) {
                visual.displayDurationTicks = -1f;
            } else if (visual.displayDurationTicks >= 0f) {
                float durationTarget = effect.getDuration();
                float durationSpeed = durationTarget < visual.displayDurationTicks ? 6.5f : 10f;
                visual.displayDurationTicks += (durationTarget - visual.displayDurationTicks) * Math.min(1f, dt * durationSpeed);
            }
            visual.currentY += (visual.targetY - visual.currentY) * Math.min(1f, dt * 12f);
            if (visual.maxDuration <= 0 || effect.getDuration() > visual.maxDuration) {
                visual.maxDuration = Math.max(effect.getDuration(), 20);
            }
        }

        Iterator<Map.Entry<String, EffectVisual>> iterator = visuals.entrySet().iterator();
        while (iterator.hasNext()) {
            EffectVisual visual = iterator.next().getValue();
            if (current.containsKey(visual.key)) continue;
            visual.visible = false;
            visual.exiting = true;
            if (visual.hideUntilMs == 0L) visual.hideUntilMs = now + HIDE_DELAY_MS;
            visual.slide = Math.max(0f, visual.slide - dt * 10f);
            visual.rowAlpha = Math.max(0f, visual.rowAlpha - dt * 10f);
            visual.progress = Math.max(0f, visual.progress - dt * 7.5f);
            visual.fillDelay = Math.max(0f, visual.fillDelay - dt * 10f);
            if (now >= visual.hideUntilMs && visual.slide <= 0.01f && visual.rowAlpha <= 0.01f) {
                iterator.remove();
            }
        }

        ArrayList<EffectVisual> ordered = new ArrayList<>(visuals.values());
        ordered.sort(Comparator.comparingInt(v -> v.targetIndex));
        float cursorY = V_PAD;
        for (EffectVisual visual : ordered) {
            if (!visual.visible && visual.slide <= 0.01f) continue;
            if (visual.visible) {
                visual.targetY = cursorY;
                cursorY += ROW_H + GAP;
            }
            visual.currentY += (visual.targetY - visual.currentY) * Math.min(1f, dt * 12f);
        }

        if (visibleCount == 1) {
            float animatedHeight = estimateAnimatedHeight(ordered);
            float centeredY = Math.max(V_PAD, (animatedHeight - ROW_H) * 0.5f);
            for (EffectVisual visual : ordered) {
                if (!visual.visible) continue;
                visual.targetY = centeredY;
                visual.currentY += (visual.targetY - visual.currentY) * Math.min(1f, dt * 12f);
                break;
            }
        }
        return ordered;
    }

    private boolean renderTexture(Minecraft client, List<EffectVisual> visuals, float userScale, float bgProgress, float baseH) {
        ensureNativeLoaded();
        float targetScale = Math.max(1f, (float) client.getWindow().getGuiScale() * userScale);
        int targetW = Math.max(1, Math.round(WIDTH * targetScale));
        int targetH = Math.max(1, Math.round(baseH * targetScale));
        boolean animating = isAnimating(visuals, bgProgress);
        String baseSignature = targetW + "x" + targetH + "@" + Float.floatToIntBits(targetScale) + ":" + Math.round(bgProgress * 100f);
        String overlaySignature = makeSignature(visuals, targetW, targetH, targetScale, bgProgress);
        if (!animating && dynamicTexture != null && overlayTexture != null && targetW == textureW && targetH == textureH && overlaySignature.equals(lastOverlaySignature)) return true;

        if (surface == null || dynamicTexture == null || targetW != textureW || targetH != textureH) {
            destroyBaseTexture(client);
            surface = Surface.makeRaster(new ImageInfo(new ColorInfo(ColorType.RGBA_8888, ColorAlphaType.UNPREMUL, null), targetW, targetH), 0, SURFACE_PROPS);
            dynamicTexture = new DynamicTexture("pvp_utils:potion_status", targetW, targetH, false);
            client.getTextureManager().register(TEXTURE_ID, dynamicTexture);
            textureW = targetW;
            textureH = targetH;
            textureScale = targetScale;
            lastBaseSignature = "";
        }

        if (overlaySurface == null || overlayTexture == null || targetW != overlayTextureW || targetH != overlayTextureH) {
            destroyOverlayTexture(client);
            overlaySurface = Surface.makeRaster(new ImageInfo(new ColorInfo(ColorType.RGBA_8888, ColorAlphaType.UNPREMUL, null), targetW, targetH), 0, SURFACE_PROPS);
            overlayTexture = new DynamicTexture("pvp_utils:potion_status_overlay", targetW, targetH, false);
            client.getTextureManager().register(OVERLAY_TEXTURE_ID, overlayTexture);
            overlayTextureW = targetW;
            overlayTextureH = targetH;
            lastOverlaySignature = "";
        }

        if (!baseSignature.equals(lastBaseSignature)) {
            Canvas canvas = surface.getCanvas();
            canvas.restoreToCount(1);
            canvas.resetMatrix();
            canvas.clear(0x00000000);
            canvas.save();
            canvas.scale(textureScale, textureScale);
            drawBackground(canvas, baseH, bgProgress);
            canvas.restore();
            if (!uploadSurface(surface, dynamicTexture, textureW, textureH)) return false;
            lastBaseSignature = baseSignature;
        }

        Canvas overlayCanvas = overlaySurface.getCanvas();
        overlayCanvas.restoreToCount(1);
        overlayCanvas.resetMatrix();
        overlayCanvas.clear(0x00000000);
        overlayCanvas.save();
        overlayCanvas.scale(textureScale, textureScale);
        for (EffectVisual visual : visuals) {
            if (visual.slide <= 0.01f && visual.rowAlpha <= 0.01f) continue;
            drawEffect(overlayCanvas, visual);
        }
        overlayCanvas.restore();

        if (!uploadSurface(overlaySurface, overlayTexture, overlayTextureW, overlayTextureH)) return false;
        lastOverlaySignature = overlaySignature;
        return true;
    }

    private void drawBackground(Canvas canvas, float baseH, float bgProgress) {
        if (!Config.potionStatusBackground) return;
        float drawW = WIDTH * bgProgress;
        if (drawW <= 0.01f) return;
        bgPaint.setColor(0x7A6E737A);
        canvas.drawRRect(RRect.makeXYWH(0f, 0f, drawW, baseH, BG_RADIUS), bgPaint);
        borderPaint.setColor(0x30FFFFFF);
        canvas.drawRRect(RRect.makeXYWH(0.5f, 0.5f, Math.max(0f, drawW - 1f), Math.max(0f, baseH - 1f), BG_RADIUS), borderPaint);
    }

    private void drawEffect(Canvas canvas, EffectVisual visual) {
        MobEffectInstance effect = visual.effect;
        if (effect == null) return;
        float rowY = visual.currentY;
        float slide = easeOutCubic(visual.slide);
        float alpha = visual.rowAlpha;
        float drawX = PAD + (1f - slide) * -24f;
        float drawW = WIDTH - PAD * 2f;
        int effectColor = 0xFF000000 | (effect.getEffect().value().getColor() & 0xFFFFFF);
        int darkColor = darken(effectColor, 0.28f);
        float fillRatio = Mth.clamp(visual.fillProgress, 0f, 1f);
        boolean showCountdown = Config.potionStatusCountdown;

        itemPaint.setColor(withAlpha(darkColor, 0.92f * alpha));
        canvas.drawRRect(RRect.makeXYWH(drawX, rowY, drawW, ROW_H, ITEM_RADIUS), itemPaint);

        if (fillRatio > 0.01f) {
            float fillW = drawW * fillRatio;
            itemFillPaint.setColor(withAlpha(effectColor, 0.86f * alpha));
            canvas.drawRRect(RRect.makeXYWH(drawX, rowY, fillW, ROW_H, ITEM_RADIUS), itemFillPaint);
        }

        itemOverlayPaint.setColor(withAlpha(0x08FFFFFF, alpha));
        canvas.drawRRect(RRect.makeXYWH(drawX, rowY, drawW, ROW_H, ITEM_RADIUS), itemOverlayPaint);

        String name = truncate(Component.translatable(effect.getDescriptionId()).getString(), 70f, 10f);
        String time = formatDuration(visual);
        String amp = amplifier(effect.getAmplifier());
        if (showCountdown) {
            FontRenderer.drawText(canvas, name, drawX + ICON + 10f, rowY + 11f, 10f, withAlpha(0xF6FFFFFF, alpha));
            FontRenderer.drawText(canvas, amp.isEmpty() ? time : amp + "  " + time, drawX + ICON + 10f, rowY + 22f, 8f, withAlpha(0xCFFFFFFF, alpha));
        } else {
            FontRenderer.drawText(canvas, name, drawX + ICON + 10f, rowY + 17f, 10f, withAlpha(0xF6FFFFFF, alpha));
        }
    }

    private void renderIcons(GuiGraphics graphics, Minecraft client, List<EffectVisual> visuals, float baseX, float baseY, float scale, float bgProgress) {
        if (bgProgress <= 0.01f) return;
        for (EffectVisual visual : visuals) {
            if (visual.slide <= 0.01f || visual.rowAlpha <= 0.01f || visual.effect == null) continue;
            float slide = easeOutCubic(visual.slide);
            float drawX = baseX + (PAD + (1f - slide) * -24f + 5f) * scale;
            float drawY = baseY + (visual.currentY + 5f) * scale;
            try {
                Identifier texture = Identifier.fromNamespaceAndPath("minecraft", "textures/mob_effect/" + effectPath(visual.effect) + ".png");
                int size = Math.round(ICON * scale);
                graphics.blit(RenderPipelines.GUI_TEXTURED, texture, Math.round(drawX), Math.round(drawY), 0f, 0f, size, size, 18, 18, 18, 18);
            } catch (Exception ignored) {
            }
        }
    }

    private String makeSignature(List<EffectVisual> visuals, int targetW, int targetH, float targetScale, float bgProgress) {
        StringBuilder builder = new StringBuilder(160);
        builder.append(targetW).append('x').append(targetH).append('@').append(Float.floatToIntBits(targetScale)).append(':').append(Math.round(bgProgress * 100f));
        for (EffectVisual visual : visuals) {
            MobEffectInstance effect = visual.effect;
            if (effect == null) continue;
            builder.append('|').append(effect.getDescriptionId())
                    .append(':').append(effect.getAmplifier())
                    .append(':').append((effect.getDuration() / 20) / SIGNATURE_TIME_STEP_TICKS)
                    .append(':').append(effect.getEffect().value().getColor())
                    .append(':').append(Math.round(visual.currentY * 6f))
                    .append(':').append(Math.round(visual.slide * 60f))
                    .append(':').append(Math.round(visual.rowAlpha * 60f))
                    .append(':').append(Math.round(visual.fillProgress * 60f))
                    .append(':').append(Math.round(visual.fillDelay * 60f));
        }
        return builder.toString();
    }

    private String formatDuration(EffectVisual visual) {
        MobEffectInstance effect = visual.effect;
        if (effect == null) return "";
        if (effect.isInfiniteDuration() || visual.displayDurationTicks < 0f) return "inf";
        int totalSeconds = Math.max(0, Math.round(visual.displayDurationTicks / 20f));
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

    private float getAnimatedHeight(List<EffectVisual> visuals) {
        float max = ROW_H + V_PAD * 2f;
        for (EffectVisual visual : visuals) {
            if (visual.slide <= 0.01f && visual.rowAlpha <= 0.01f) continue;
            max = Math.max(max, visual.currentY + ROW_H);
        }
        return max + V_PAD;
    }

    private float estimateAnimatedHeight(List<EffectVisual> visuals) {
        float max = ROW_H + V_PAD * 2f;
        for (EffectVisual visual : visuals) {
            if (visual.slide <= 0.01f && visual.rowAlpha <= 0.01f) continue;
            float referenceY = Math.max(visual.currentY, visual.targetY);
            max = Math.max(max, referenceY + ROW_H);
        }
        return max + V_PAD;
    }

    private float getBackgroundProgress(List<EffectVisual> visuals) {
        float max = 0f;
        for (EffectVisual visual : visuals) {
            max = Math.max(max, visual.progress);
        }
        return max;
    }

    private float getBaseHeight(int count) {
        return count * ROW_H + Math.max(0, count - 1) * GAP + V_PAD * 2f;
    }

    private float getScale() {
        return Math.max(0.5f, Config.potionStatusScale);
    }

    private String keyOf(MobEffectInstance effect) {
        MobEffect value = effect.getEffect().value();
        return effect.getDescriptionId() + "#" + effect.getAmplifier() + "#" + Integer.toHexString(value.getColor()).toLowerCase(Locale.ROOT);
    }

    private String effectPath(MobEffectInstance effect) {
        String id = effect.getDescriptionId();
        int index = id.lastIndexOf('.');
        return index >= 0 && index + 1 < id.length() ? id.substring(index + 1) : "speed";
    }

    private int darken(int color, float factor) {
        int a = (color >>> 24) & 0xFF;
        int r = Math.round(((color >>> 16) & 0xFF) * factor);
        int g = Math.round(((color >>> 8) & 0xFF) * factor);
        int b = Math.round((color & 0xFF) * factor);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private int withAlpha(int color, float alpha) {
        int a = Math.round(((color >>> 24) & 0xFF) * clamp(alpha, 0f, 1f));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    private float easeOutCubic(float value) {
        float t = 1f - clamp(value, 0f, 1f);
        return 1f - t * t * t;
    }

    private boolean isAnimating(List<EffectVisual> visuals, float bgProgress) {
        if (bgProgress < 0.995f) return true;
        for (EffectVisual visual : visuals) {
            if (Math.abs(visual.currentY - visual.targetY) > 0.12f) return true;
            if (visual.slide < 0.995f || visual.rowAlpha < 0.995f || visual.progress < 0.995f) return true;
            if (visual.effect != null) {
                float remain = visual.effect.isInfiniteDuration() ? 1f : Mth.clamp(visual.effect.getDuration() / Math.max(1f, visual.maxDuration), 0f, 1f);
                float targetFill = remain * easeOutCubic(visual.fillDelay);
                if (visual.fillDelay < 0.995f || Math.abs(targetFill - visual.fillProgress) > 0.04f) return true;
            }
        }
        return false;
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
        if (overlaySurface != null) {
            overlaySurface.close();
            overlaySurface = null;
        }
        if (overlayTexture != null) {
            client.getTextureManager().release(OVERLAY_TEXTURE_ID);
            overlayTexture = null;
        }
        textureW = -1;
        textureH = -1;
        overlayTextureW = -1;
        overlayTextureH = -1;
        textureScale = -1f;
        lastBaseSignature = "";
        lastOverlaySignature = "";
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
        lastBaseSignature = "";
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
        lastOverlaySignature = "";
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }

    private static class EffectVisual {
        private String key = "";
        private MobEffectInstance effect;
        private float currentY = 0f;
        private float targetY = 0f;
        private float slide = 0f;
        private float rowAlpha = 0f;
        private float progress = 0f;
        private float fillProgress = 1f;
        private float fillDelay = 0f;
        private float displayDurationTicks = -1f;
        private int targetIndex = 0;
        private int maxDuration = 0;
        private boolean visible = true;
        private boolean exiting = false;
        private long hideUntilMs = 0L;
    }
}
