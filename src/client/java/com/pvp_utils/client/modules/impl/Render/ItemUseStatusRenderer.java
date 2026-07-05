package com.pvp_utils.client.modules.impl.Render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.pvp_utils.Config;
import com.pvp_utils.client.render.font.FontRenderer;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorAlphaType;
import io.github.humbleui.skija.ColorInfo;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PixelGeometry;
import io.github.humbleui.skija.Pixmap;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.SurfaceProps;
import io.github.humbleui.skija.impl.Library;
import io.github.humbleui.types.RRect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.KineticWeapon;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public final class ItemUseStatusRenderer {
    private static final ItemUseStatusRenderer INSTANCE = new ItemUseStatusRenderer();
    private static final long ANIM_DURATION_MS = 180L;
    private static final long HIDE_DELAY_MS = 120L;
    private static final int BAR_W = 140;
    private static final int BAR_H = 12;
    private static final int LITE_EDIT_H = 30;
    private static final int BORDER = 1;
    private static final int NEW_W = 174;
    private static final int NEW_H = 28;
    private static final float NEW_BAR_X = 6f;
    private static final float NEW_BAR_Y = 10f;
    private static final float NEW_BAR_W = 132f;
    private static final float NEW_BAR_H = 7f;
    private static final Identifier NEW_TEXTURE_ID = Identifier.fromNamespaceAndPath("pvp_utils", "item_use_status_new");
    private static final SurfaceProps SURFACE_PROPS = new SurfaceProps(false, PixelGeometry.RGB_H);
    private static final int VANILLA_CROSSBOW_CHARGE_TICKS = 25;
    private static final int VANILLA_SPEAR_DAMAGE_USE_TICKS = 10;

    private long appearTime;
    private long lastActiveTime;
    private long lastMixinSampleTime;
    private long lastLogTime;
    private long lastEntryLogTime;
    private long lastRenderLogTime;
    private boolean visible;
    private boolean mixinActive;
    private float lastProgress;
    private float mixinProgress;
    private float animatedProgress;
    private long lastFrameTime;
    private Surface newSurface;
    private DynamicTexture newTexture;
    private int newTextureW = -1;
    private int newTextureH = -1;
    private int lastNewProgressKey = -1;
    private int lastNewAlphaKey = -1;
    private Config.HudTheme lastNewTheme = null;
    private boolean nativeLoaded;
    private final Paint newTrackPaint = new Paint();
    private final Paint newFillPaint = new Paint();

    private ItemUseStatusRenderer() {
        newTrackPaint.setAntiAlias(true);
        newFillPaint.setAntiAlias(true);
    }

    public static ItemUseStatusRenderer getInstance() {
        return INSTANCE;
    }

    public float getEditWidth() {
        return getBaseWidth() * getScale();
    }

    public float getEditHeight() {
        return getBaseHeight() * getScale();
    }

    public float getDefaultY(int screenH) {
        return screenH * 0.5f + (Config.itemUseStatusMode == Config.ItemUseStatusMode.NEW ? 13f : 18f);
    }

    public float getRenderX(int screenW) {
        float w = getEditWidth();
        return clamp((screenW - w) * 0.5f + Config.itemUseStatusX, 0f, Math.max(0f, screenW - w));
    }

    public float getRenderY(int screenH) {
        float h = getEditHeight();
        return clamp(getDefaultY(screenH) + Config.itemUseStatusY, 0f, Math.max(0f, screenH - h));
    }

    public void captureFromMixin(LocalPlayer player) {
        Minecraft client = Minecraft.getInstance();
        if (player == null || client == null || client.player != player) return;

        boolean enabled = Config.itemUseStatus;
        boolean keyUseDown = client.options != null && client.options.keyUse.isDown();
        boolean using = player.isUsingItem();
        ItemStack stack = player.getUseItem();
        int remaining = using ? player.getUseItemRemainingTicks() : 0;
        int rawDuration = !stack.isEmpty() ? stack.getUseDuration(player) : 0;
        boolean eligible = !stack.isEmpty() && shouldDisplay(stack);
        int displayDuration = !stack.isEmpty() ? getDisplayDuration(stack, player) : 0;
        int kineticDuration = !stack.isEmpty() ? getKineticUseDuration(stack) : 0;

        if (!enabled || !using || stack.isEmpty() || !eligible || displayDuration <= 0 || remaining < 0) {
            mixinActive = false;
            logCapture(enabled, keyUseDown, using, stack, remaining, rawDuration, displayDuration, kineticDuration, 0, rejectReason(enabled, using, stack, eligible, displayDuration, remaining));
            return;
        }

        int usedTicks = getUsedTicks(stack, remaining, displayDuration, rawDuration);
        mixinProgress = calculateProgress(stack, usedTicks, displayDuration, rawDuration);
        mixinActive = true;
        lastMixinSampleTime = System.currentTimeMillis();
        logCapture(true, keyUseDown, true, stack, remaining, rawDuration, displayDuration, kineticDuration, usedTicks, "active progress=" + Math.round(mixinProgress * 100.0f) + "%");
    }

    public void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        logRenderEntry(client, now);
        UseState state = currentUseState(client, now);
        boolean activeNow = state != null;

        if (!Config.itemUseStatus || client.player == null || client.level == null || state == null) {
            if (!Config.itemUseStatus || client.player == null || client.level == null) {
                visible = false;
                lastProgress = 0.0f;
                return;
            }
        }

        if (state != null) {
            if (!visible) {
                appearTime = now;
                visible = true;
                animatedProgress = state.progress();
            }
            lastActiveTime = now;
            lastProgress = state.progress();
        }

        if (!visible) return;

        updateAnimatedProgress(now);

        float fadeIn = (now - appearTime) / (float) ANIM_DURATION_MS;
        float fadeOut = 1.0f - (now - (lastActiveTime + HIDE_DELAY_MS)) / (float) ANIM_DURATION_MS;
        float alpha = easeOutCubic(Mth.clamp(Math.min(fadeIn, fadeOut), 0.0f, 1.0f));
        if (alpha <= 0.0f) {
            if (!activeNow) {
                visible = false;
                lastProgress = 0.0f;
                animatedProgress = 0.0f;
                lastFrameTime = 0L;
            }
            return;
        }

        logRender(client, animatedProgress, alpha);
        if (Config.itemUseStatusMode == Config.ItemUseStatusMode.NEW) {
            renderNew(graphics, client, animatedProgress, alpha);
        } else {
            renderLite(graphics, client, animatedProgress, alpha);
        }
    }

    private UseState currentUseState(Minecraft client, long now) {
        if (client == null || client.player == null || client.level == null || client.gameMode == null) return null;
        Screen screen = client.screen;
        if (screen != null) return null;

        if (mixinActive && now - lastMixinSampleTime < 250L) {
            return new UseState(mixinProgress);
        }

        if (!client.player.isUsingItem()) return null;

        ItemStack stack = client.player.getUseItem();
        if (stack.isEmpty() || !shouldDisplay(stack)) return null;

        int duration = getDisplayDuration(stack, client.player);
        int remaining = client.player.getUseItemRemainingTicks();
        if (duration <= 0 || remaining < 0) return null;

        int rawDuration = stack.getUseDuration(client.player);
        int usedTicks = getUsedTicks(stack, remaining, duration, rawDuration);
        float progress = calculateProgress(stack, usedTicks, duration, rawDuration);
        return new UseState(progress);
    }

    private boolean shouldDisplay(ItemStack stack) {
        return stack.get(DataComponents.FOOD) != null
                || stack.get(DataComponents.CONSUMABLE) != null
                || stack.is(Items.BOW)
                || stack.is(Items.CROSSBOW)
                || stack.is(Items.TRIDENT)
                || stack.is(ItemTags.SPEARS)
                || stack.is(Items.POTION)
                || stack.is(Items.SPLASH_POTION)
                || stack.is(Items.LINGERING_POTION)
                || stack.is(Items.BRUSH);
    }

    private int getDisplayDuration(ItemStack stack, LocalPlayer player) {
        if (stack.is(Items.BOW)) {
            return 20;
        }
        if (stack.is(Items.CROSSBOW)) {
            return getCrossbowUseDuration(stack, player);
        }
        if (stack.is(Items.TRIDENT)) {
            return 10;
        }
        if (stack.is(ItemTags.SPEARS)) {
            return getSpearUseDuration(stack, player);
        }
        if (stack.is(Items.BRUSH)) {
            return 10;
        }
        if (player == null) {
            return 0;
        }
        int duration = stack.getUseDuration(player);
        return duration > 0 ? duration : 32;
    }

    private float calculateProgress(ItemStack stack, int usedTicks, int displayDuration, int rawDuration) {
        if (stack.is(ItemTags.SPEARS)) {
            return 1.0f - Mth.clamp(usedTicks / (float) Math.max(1, displayDuration), 0.0f, 1.0f);
        }
        return Mth.clamp(usedTicks / (float) displayDuration, 0.0f, 1.0f);
    }

    private int getUsedTicks(ItemStack stack, int remaining, int displayDuration, int rawDuration) {
        int baselineDuration = rawDuration > 0 ? rawDuration : displayDuration;
        if (stack.is(ItemTags.SPEARS)) {
            baselineDuration = rawDuration >= 72000 || rawDuration <= 0 ? 72000 : rawDuration;
        }
        return Math.max(0, baselineDuration - remaining);
    }

    private int getCrossbowUseDuration(ItemStack stack, LocalPlayer player) {
        try {
            return Math.max(1, CrossbowItem.getChargeDuration(stack, player));
        } catch (Throwable ignored) {
            return getVanillaCrossbowUseDuration(player);
        }
    }

    private int getVanillaCrossbowUseDuration(LocalPlayer player) {
        try {
            return Math.max(1, CrossbowItem.getChargeDuration(new ItemStack(Items.CROSSBOW), player));
        } catch (Throwable ignored) {
            return VANILLA_CROSSBOW_CHARGE_TICKS;
        }
    }

    private int getSpearUseDuration(ItemStack stack, LocalPlayer player) {
        int duration = getKineticUseDuration(stack);
        if (duration > 0) {
            return duration;
        }

        ItemStack vanillaStack = new ItemStack(stack.getItem());
        duration = getKineticUseDuration(vanillaStack);
        if (duration > 0) {
            return duration;
        }

        int rawDuration = player == null ? 0 : vanillaStack.getUseDuration(player);
        if (rawDuration > 0 && rawDuration < 72000) {
            return rawDuration;
        }

        return VANILLA_SPEAR_DAMAGE_USE_TICKS;
    }

    private int getKineticUseDuration(ItemStack stack) {
        KineticWeapon kineticWeapon = stack.get(DataComponents.KINETIC_WEAPON);
        return kineticWeapon == null ? 0 : kineticWeapon.computeDamageUseDuration();
    }

    private void renderLite(GuiGraphics graphics, Minecraft client, float progress, float alpha) {
        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        float scale = getScale();
        int x = Math.round(getRenderX(screenW));
        int y = Math.round(getRenderY(screenH));
        int alphaInt = Math.round(alpha * 255.0f);
        int alphaBits = alphaInt << 24;

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-x, -y);
        graphics.renderOutline(x, y, BAR_W, BAR_H, alphaBits | 0xFFFFFF);

        int innerW = BAR_W - BORDER * 2;
        int fillW = Math.max(0, Math.min(innerW, Math.round(innerW * Mth.clamp(progress, 0.0f, 1.0f))));
        if (fillW > 0) {
            graphics.fill(x + BORDER, y + BORDER, x + BORDER + fillW, y + BAR_H - BORDER, progressColor(progress, alphaInt));
        }

        int percent = Math.round(Mth.clamp(progress, 0.0f, 1.0f) * 100.0f);
        String text = percent + "%";
        int textW = client.font.width(text);
        int textX = x + BORDER + fillW - textW / 2;
        textX = Math.max(x, Math.min(textX, x + BAR_W - textW));
        int textY = y + BAR_H + 4;
        graphics.drawString(client.font, Component.literal(text), textX, textY, alphaBits | 0xFFFFFF, true);
        graphics.pose().popMatrix();
    }

    private void renderNew(GuiGraphics graphics, Minecraft client, float progress, float alpha) {
        ensureNativeLoaded();
        float targetScale = Math.max(1f, (float) client.getWindow().getGuiScale());
        int targetW = Math.max(1, Math.round(NEW_W * targetScale));
        int targetH = Math.max(1, Math.round(NEW_H * targetScale));
        int progressKey = Math.round(Mth.clamp(progress, 0.0f, 1.0f) * 220f);
        int alphaKey = Math.round(Mth.clamp(alpha, 0.0f, 1.0f) * 80f);

        if (newTexture == null || newSurface == null || newTextureW != targetW || newTextureH != targetH) {
            destroyNewTexture(client);
            newSurface = Surface.makeRaster(new ImageInfo(new ColorInfo(ColorType.RGBA_8888, ColorAlphaType.UNPREMUL, null), targetW, targetH), 0, SURFACE_PROPS);
            newTexture = new DynamicTexture("pvp_utils:item_use_status_new", targetW, targetH, false);
            client.getTextureManager().register(NEW_TEXTURE_ID, newTexture);
            newTextureW = targetW;
            newTextureH = targetH;
            lastNewProgressKey = -1;
        }

        if (progressKey != lastNewProgressKey || alphaKey != lastNewAlphaKey || lastNewTheme != Config.hudTheme) {
            Canvas c = newSurface.getCanvas();
            c.restoreToCount(1);
            c.resetMatrix();
            c.clear(0x00000000);
            c.save();
            c.scale(targetScale, targetScale);

            int alphaInt = Math.round(Mth.clamp(alpha, 0.0f, 1.0f) * 255f);
            newTrackPaint.setColor((Math.round(alphaInt * 0.22f) << 24) | (Config.hudTheme == Config.HudTheme.LIGHT ? 0x111827 : 0xFFFFFF));
            c.drawRRect(RRect.makeXYWH(NEW_BAR_X, NEW_BAR_Y, NEW_BAR_W, NEW_BAR_H, NEW_BAR_H * 0.5f), newTrackPaint);

            float fillW = Math.max(NEW_BAR_H, NEW_BAR_W * Mth.clamp(progress, 0.0f, 1.0f));
            newFillPaint.setColor((alphaInt << 24) | (progressColor(progress, 255) & 0xFFFFFF));
            c.drawRRect(RRect.makeXYWH(NEW_BAR_X, NEW_BAR_Y, fillW, NEW_BAR_H, NEW_BAR_H * 0.5f), newFillPaint);

            int textColor = (alphaInt << 24) | 0xFFFFFF;
            FontRenderer.drawText(c, Math.round(Mth.clamp(progress, 0.0f, 1.0f) * 100.0f) + "%", 146f, 17f, 10f, textColor);
            c.restore();

            uploadSurface(newSurface, newTexture, newTextureW, newTextureH);
            lastNewProgressKey = progressKey;
            lastNewAlphaKey = alphaKey;
            lastNewTheme = Config.hudTheme;
        }

        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        float scale = getScale();
        int x = Math.round(getRenderX(screenW));
        int y = Math.round(getRenderY(screenH));
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-x, -y);
        graphics.blit(RenderPipelines.GUI_TEXTURED, NEW_TEXTURE_ID, x, y, 0f, 0f, NEW_W, NEW_H, newTextureW, newTextureH, newTextureW, newTextureH);
        graphics.pose().popMatrix();
    }

    private void updateAnimatedProgress(long now) {
        if (lastFrameTime <= 0L) {
            lastFrameTime = now;
            animatedProgress = lastProgress;
            return;
        }
        float dt = Math.min(0.05f, Math.max(0.0f, (now - lastFrameTime) / 1000.0f));
        lastFrameTime = now;
        float follow = 1.0f - (float) Math.pow(0.0008f, dt);
        animatedProgress += (lastProgress - animatedProgress) * Mth.clamp(follow, 0.0f, 1.0f);
    }

    private float easeOutCubic(float value) {
        float t = 1.0f - Mth.clamp(value, 0.0f, 1.0f);
        return 1.0f - t * t * t;
    }

    private void ensureNativeLoaded() {
        if (nativeLoaded) return;
        Library.load();
        nativeLoaded = true;
    }

    private void uploadSurface(Surface sourceSurface, DynamicTexture targetTexture, int width, int height) {
        Pixmap pixmap = new Pixmap();
        try {
            if (!sourceSurface.peekPixels(pixmap)) {
                return;
            }
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

    private void destroyNewTexture(Minecraft client) {
        if (newSurface != null) {
            newSurface.close();
            newSurface = null;
        }
        if (newTexture != null) {
            client.getTextureManager().release(NEW_TEXTURE_ID);
            newTexture = null;
        }
        newTextureW = -1;
        newTextureH = -1;
        lastNewProgressKey = -1;
        lastNewAlphaKey = -1;
        lastNewTheme = null;
    }

    private int progressColor(float progress, int alpha) {
        progress = Mth.clamp(progress, 0.0f, 1.0f);
        int r;
        int g;
        if (progress < 0.5f) {
            float t = progress * 2.0f;
            r = 255;
            g = Math.round(255.0f * t);
        } else {
            float t = (progress - 0.5f) * 2.0f;
            r = Math.round(255.0f * (1.0f - t));
            g = 255;
        }
        return ((alpha & 0xFF) << 24) | (r << 16) | (g << 8);
    }

    private String rejectReason(boolean enabled, boolean using, ItemStack stack, boolean eligible, int displayDuration, int remaining) {
        if (!enabled) return "disabled";
        if (!using) return "not_using";
        if (stack.isEmpty()) return "empty_use_stack";
        if (!eligible) return "filtered_item";
        if (displayDuration <= 0) return "invalid_display_duration";
        if (remaining < 0) return "invalid_remaining";
        return "unknown";
    }

    private void logCapture(boolean enabled, boolean keyUseDown, boolean using, ItemStack stack, int remaining, int rawDuration, int displayDuration, int kineticDuration, int usedTicks, String reason) {
        long now = System.currentTimeMillis();
        if (now - lastLogTime < 250L) return;
        lastLogTime = now;
        String item = stack == null || stack.isEmpty() ? "empty" : stack.getItem().toString();
        System.out.println("[PVPUtils][ItemUseStatus] enabled=" + enabled
                + " keyUse=" + keyUseDown
                + " using=" + using
                + " item=" + item
                + " remaining=" + remaining
                + " rawDuration=" + rawDuration
                + " displayDuration=" + displayDuration
                + " kineticDuration=" + kineticDuration
                + " usedTicks=" + usedTicks
                + " reason=" + reason);
    }

    private void logRender(Minecraft client, float progress, float alpha) {
        long now = System.currentTimeMillis();
        if (now - lastRenderLogTime < 250L) return;
        lastRenderLogTime = now;
        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        int x = Math.round(getRenderX(screenW));
        int y = Math.round(getRenderY(screenH));
        System.out.println("[PVPUtils][ItemUseStatus][Render] mode=" + Config.itemUseStatusMode
                + " visible=" + visible
                + " mixinActive=" + mixinActive
                + " progress=" + Math.round(progress * 100.0f) + "%"
                + " alpha=" + Math.round(alpha * 100.0f) + "%"
                + " pos=" + x + "," + y
                + " size=" + BAR_W + "x" + BAR_H
                + " screen=" + screenW + "x" + screenH);
    }

    private float getBaseWidth() {
        return Config.itemUseStatusMode == Config.ItemUseStatusMode.NEW ? NEW_W : BAR_W;
    }

    private float getBaseHeight() {
        return Config.itemUseStatusMode == Config.ItemUseStatusMode.NEW ? NEW_H : LITE_EDIT_H;
    }

    private float getScale() {
        return Math.max(0.5f, Config.itemUseStatusScale);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private void logRenderEntry(Minecraft client, long now) {
        if (!Config.itemUseStatus || now - lastEntryLogTime < 500L) return;
        lastEntryLogTime = now;
        String screen = client == null || client.screen == null ? "none" : client.screen.getClass().getSimpleName();
        System.out.println("[PVPUtils][ItemUseStatus][RenderEntry] player=" + (client != null && client.player != null)
                + " level=" + (client != null && client.level != null)
                + " screen=" + screen
                + " visible=" + visible
                + " mixinActive=" + mixinActive
                + " sampleAgeMs=" + (lastMixinSampleTime <= 0 ? -1 : now - lastMixinSampleTime)
                + " lastProgress=" + Math.round(lastProgress * 100.0f) + "%"
                + " mixinProgress=" + Math.round(mixinProgress * 100.0f) + "%");
    }

    private record UseState(float progress) {}
}
