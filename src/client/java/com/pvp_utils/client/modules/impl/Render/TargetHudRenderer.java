package com.pvp_utils.client.modules.impl.Render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.pvp_utils.client.gui.TargetScoreboardUtil;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.font.FontRenderer;
import io.github.humbleui.skija.*;
import io.github.humbleui.skija.impl.Library;
import io.github.humbleui.types.Rect;
import io.github.humbleui.types.RRect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class TargetHudRenderer {
    private static final TargetHudRenderer INSTANCE = new TargetHudRenderer();

    private LivingEntity target = null;
    private long lastHitTime = 0;
    private long appearanceTime = 0;
    private boolean isFullyHidden = true;
    private boolean editPreview = false;
    private boolean wasEditActive = false;
    private Surface surface;
    private Surface avatarMaskSurface;
    private DynamicTexture dynamicTexture;
    private DynamicTexture avatarMaskTexture;
    private boolean nativeLoaded = false;
    private int textureW = -1;
    private int textureH = -1;
    private int avatarMaskW = -1;
    private int avatarMaskH = -1;
    private String lastTextureName = "";
    private String lastTextureHealthText = "";
    private int lastTextureHealth = -1;
    private int lastTextureAbsorption = -1;
    private int lastTextureHealthTextAnim = -1;
    private String lastRawName = "";
    private String lastTruncatedName = "";
    private float animatedHealthRatio = 1f;
    private float animatedAbsorptionRatio = 0f;
    private long lastRenderTime = 0L;
    private String currentHealthText = "";
    private String previousHealthText = "";
    private long healthTextAnimStart = 0L;
    private int healthTextDirection = 0;
    private float lastHealthTextValue = -1f;

    private long lastDamageTime = 0;
    private long lastHealTime = 0;
    private float lastObservedHealth = -1f;
    private static final long DAMAGE_FLASH_DURATION = 300;

    private static final long HIDE_DELAY = 3000;
    private static final long ANIM_DURATION = 200;
    private static final long HEALTH_TEXT_ANIM_DURATION = 220;

    private static final int HUD_WIDTH = 160;
    private static final int HUD_HEIGHT = 40;
    private static final int NEW_HUD_WIDTH = 190;
    private static final int NEW_HUD_HEIGHT = 58;
    private static final int NEW_AVATAR_SIZE = 38;
    private static final int AVATAR_SIZE = 28;
    private static final int PADDING = 6;
    private static final int BORDER = 1;
    private static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath("pvp_utils", "target_hud_new");
    private static final Identifier AVATAR_MASK_TEXTURE_ID = Identifier.fromNamespaceAndPath("pvp_utils", "target_hud_avatar_mask");

    public static TargetHudRenderer getInstance() {
        return INSTANCE;
    }

    public void onHit(LivingEntity entity) {
        if (!Config.targetHud || entity == null) return;

        long now = System.currentTimeMillis();

        if (this.target == null || isFullyHidden) {
            this.appearanceTime = now;
            this.isFullyHidden = false;
        }
        float currentHealth = entity.getHealth();
        if (this.target != entity || lastObservedHealth < 0f || currentHealth < lastObservedHealth - 0.001f) {
            this.lastDamageTime = now;
        } else if (currentHealth > lastObservedHealth + 0.001f) {
            this.lastHealTime = now;
        }
        this.target = entity;
        this.lastHitTime = now;
    }

    public void render(GuiGraphics graphics) {
        long now = System.currentTimeMillis();
        Minecraft client = Minecraft.getInstance();
        boolean editActive = HudEditOverlay.getInstance().isActive() && Config.targetHud;

        if (editActive && client.player != null) {
            if (!editPreview || target != client.player || isFullyHidden) {
                appearanceTime = now;
                isFullyHidden = false;
            }
            target = client.player;
            lastHitTime = now;
            lastDamageTime = 0;
            lastHealTime = 0;
            lastObservedHealth = -1f;
            editPreview = true;
        } else if (editPreview && wasEditActive) {
            lastHitTime = now - HIDE_DELAY;
            editPreview = false;
        }
        wasEditActive = editActive;

        if ((!Config.targetHud && !editPreview) || target == null) return;

        if (!target.isAlive() && now - lastHitTime < HIDE_DELAY) {
            lastHitTime = now - HIDE_DELAY;
        }

        float fadeIn = (float) (now - appearanceTime) / ANIM_DURATION;
        float fadeOut = 1.0f - (float) (now - (lastHitTime + HIDE_DELAY)) / ANIM_DURATION;

        float alpha = Mth.clamp(Math.min(fadeIn, fadeOut), 0.0f, 1.0f);

        if (alpha <= 0.0f) {
            if (now - lastHitTime > HIDE_DELAY || !target.isAlive()) {
                isFullyHidden = true;
                target = null;
                resetHealthTextAnimation();
                lastObservedHealth = -1f;
            }
            return;
        }

        if (Config.targetHudMode == Config.TargetHudMode.NEW) {
            renderNew(graphics, client, alpha, now);
            return;
        }

        renderLite(graphics, client, alpha, now);
    }

    private void renderLite(GuiGraphics graphics, Minecraft client, float alpha, long now) {
        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        float hudScale = Math.max(0.5f, Config.targetHudScale);
        int scaledW = Math.round(HUD_WIDTH * hudScale);
        int scaledH = Math.round(HUD_HEIGHT * hudScale);

        int x = (int) (screenW * 0.5f + Config.targetHudX);
        int y = (int) (screenH * 0.5f + Config.targetHudY);
        x = Math.max(0, Math.min(x, screenW - scaledW));
        y = Math.max(0, Math.min(y, screenH - scaledH));

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(hudScale, hudScale);
        graphics.pose().translate(-x, -y);

        int alphaInt = Math.round(alpha * 255);
        int alphaBits = alphaInt << 24;
        int whiteWithAlpha = alphaBits | 0xFFFFFF;
        int grayWithAlpha = alphaBits | 0x444444;

        graphics.renderOutline(x, y, HUD_WIDTH, HUD_HEIGHT, whiteWithAlpha);

        int avatarX = x + BORDER + PADDING;
        int avatarY = y + (HUD_HEIGHT - AVATAR_SIZE) / 2;
        int avatarX2 = avatarX + AVATAR_SIZE;
        int avatarY2 = avatarY + AVATAR_SIZE;
        int iconX = avatarX + (AVATAR_SIZE - 16) / 2;
        int iconY = avatarY + (AVATAR_SIZE - 16) / 2;

        float scale = 1.0f;
        float flashAlphaFactor = 0.0f;
        long damageElapsed = now - lastDamageTime;
        if (damageElapsed < DAMAGE_FLASH_DURATION) {
            float damageFactor = (float) damageElapsed / DAMAGE_FLASH_DURATION;
            float scaleProgress = (float) Math.sin(damageFactor * Math.PI);
            scale = 1.0f - scaleProgress * 0.2f;
            flashAlphaFactor = 1.0f - damageFactor;
        }

        graphics.pose().pushMatrix();
        float centerX = avatarX + AVATAR_SIZE * 0.5f;
        float centerY = avatarY + AVATAR_SIZE * 0.5f;

        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-centerX, -centerY);

        if (target instanceof Player player) {
            try {
                PlayerSkin skin = client.getSkinManager().createLookup(player.getGameProfile(), false).get();
                PlayerFaceRenderer.draw(graphics, skin, avatarX, avatarY, AVATAR_SIZE);
            } catch (Exception e) {
                graphics.fill(avatarX, avatarY, avatarX2, avatarY2, alphaBits | 0x000000);
            }
        } else {
            SpawnEggItem eggItem = SpawnEggItem.byId(target.getType());
            if (eggItem != null) {
                graphics.renderFakeItem(new ItemStack(eggItem), iconX, iconY);
            } else {
                graphics.fill(avatarX, avatarY, avatarX2, avatarY2, alphaBits | 0x000000);
            }
        }

        if (flashAlphaFactor > 0.0f) {
            int damageAlphaInt = (int) (alphaInt * flashAlphaFactor * 0.6f);
            int flashColor = (damageAlphaInt << 24) | 0xFF0000;
            graphics.fill(avatarX, avatarY, avatarX2, avatarY2, flashColor);
        }
        graphics.pose().popMatrix();

        int infoX = avatarX + AVATAR_SIZE + PADDING;
        int infoW = HUD_WIDTH - BORDER - PADDING - AVATAR_SIZE - PADDING * 2 - BORDER;

        String name = target.getDisplayName().getString();
        if (name.length() > 16) name = name.substring(0, 16) + "..";
        graphics.drawString(client.font, Component.literal(name), infoX, y + PADDING + 2, whiteWithAlpha, false);

        float maxHealth = target.getMaxHealth();
        float currentHealth = target.getHealth();

        int scoreboardHealth = TargetScoreboardUtil.getBelowNameHealth(target);
        if (scoreboardHealth != -1) {
            currentHealth = (float) scoreboardHealth;
            if (currentHealth > maxHealth) maxHealth = currentHealth;
        }

        float ratio = maxHealth > 0 ? Math.max(0, Math.min(1, currentHealth / maxHealth)) : 0;

        int barY = y + HUD_HEIGHT - PADDING - 6;
        int barW = infoW;

        graphics.fill(infoX, barY, infoX + barW, barY + 5, grayWithAlpha);

        int filledW = (int) (barW * ratio);
        if (filledW > 0) {
            int hColor = getHealthColor(ratio);
            int hColorWithAlpha = alphaBits | (hColor & 0xFFFFFF);
            graphics.fill(infoX, barY, infoX + filledW, barY + 5, hColorWithAlpha);
        }

        if (client.player != null) {
            float selfHealth = client.player.getHealth();
            String statusText = selfHealth > currentHealth ? "W" : "L";
            int statusColor = selfHealth > currentHealth ? (alphaBits | 0x55FF55) : (alphaBits | 0xFF5555);

            int textWidth = client.font.width(statusText);
            graphics.drawString(client.font, Component.literal(statusText), x + HUD_WIDTH - PADDING - textWidth, y + PADDING + 2, statusColor, false);
        }
        graphics.pose().popMatrix();
    }

    private void renderNew(GuiGraphics graphics, Minecraft client, float alpha, long now) {
        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        float hudScale = Math.max(0.5f, Config.targetHudScale);
        int scaledW = Math.round(NEW_HUD_WIDTH * hudScale);
        int scaledH = Math.round(NEW_HUD_HEIGHT * hudScale);

        int x = (int) (screenW * 0.5f + Config.targetHudX);
        int y = (int) (screenH * 0.5f + Config.targetHudY);
        x = Math.max(0, Math.min(x, screenW - scaledW));
        y = Math.max(0, Math.min(y, screenH - scaledH));

        float maxHealth = target.getMaxHealth();
        float currentHealth = target.getHealth();
        int scoreboardHealth = TargetScoreboardUtil.getBelowNameHealth(target);
        if (scoreboardHealth != -1) {
            currentHealth = (float) scoreboardHealth;
            if (currentHealth > maxHealth) maxHealth = currentHealth;
        }
        float absorption = Math.max(0f, target.getAbsorptionAmount());
        updateHealthTransition(currentHealth, now);
        float ratio = maxHealth > 0 ? Mth.clamp(currentHealth / maxHealth, 0f, 1f) : 0f;
        float absorptionRatio = maxHealth > 0 ? Mth.clamp(absorption / maxHealth, 0f, 1f) : 0f;
        updateHealthTextAnimation(currentHealth, now);
        float dt = lastRenderTime == 0L ? 0.016f : Math.min((now - lastRenderTime) / 1000f, 0.05f);
        lastRenderTime = now;
        animatedHealthRatio += (ratio - animatedHealthRatio) * Math.min(1f, dt * 10f);
        animatedAbsorptionRatio += (absorptionRatio - animatedAbsorptionRatio) * Math.min(1f, dt * 10f);

        String name = truncateName(target.getDisplayName().getString());
        float cx = x + scaledW * 0.5f;
        float cy = y + scaledH * 0.5f;
        float drawScale = easeOutBack(alpha);

        renderNewTexture(client, name, currentHealthText, animatedHealthRatio, animatedAbsorptionRatio, now);
        renderAvatarMaskTexture(client);

        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, cy);
        graphics.pose().scale(drawScale, drawScale);
        graphics.pose().translate(-cx, -cy);
        graphics.pose().translate(x, y);
        graphics.pose().scale(hudScale, hudScale);
        graphics.pose().translate(-x, -y);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_ID, x, y, 0f, 0f, NEW_HUD_WIDTH, NEW_HUD_HEIGHT, textureW, textureH, textureW, textureH);

        int avatarX = x + 12;
        int avatarY = y + 10;
        int alphaInt = Math.round(alpha * 255);
        int alphaBits = alphaInt << 24;
        float avatarScale = 1.0f;
        float hurtFlashFactor = getFlashFactor(now, lastDamageTime);
        float healFlashFactor = getFlashFactor(now, lastHealTime);
        if (hurtFlashFactor > 0.0f) {
            float damageFactor = 1.0f - hurtFlashFactor;
            float scaleProgress = (float) Math.sin(damageFactor * Math.PI);
            avatarScale = 1.0f - scaleProgress * 0.2f;
        }

        graphics.pose().pushMatrix();
        float avatarCenterX = avatarX + NEW_AVATAR_SIZE * 0.5f;
        float avatarCenterY = avatarY + NEW_AVATAR_SIZE * 0.5f;
        graphics.pose().translate(avatarCenterX, avatarCenterY);
        graphics.pose().scale(avatarScale, avatarScale);
        graphics.pose().translate(-avatarCenterX, -avatarCenterY);
        if (target instanceof Player player) {
            try {
                PlayerSkin skin = client.getSkinManager().createLookup(player.getGameProfile(), false).get();
                PlayerFaceRenderer.draw(graphics, skin, avatarX, avatarY, NEW_AVATAR_SIZE);
            } catch (Exception e) {
                graphics.fill(avatarX, avatarY, avatarX + NEW_AVATAR_SIZE, avatarY + NEW_AVATAR_SIZE, alphaBits | 0x111111);
            }
        } else {
            SpawnEggItem eggItem = SpawnEggItem.byId(target.getType());
            if (eggItem != null) {
                graphics.renderFakeItem(new ItemStack(eggItem), avatarX + 11, avatarY + 11);
            } else {
                graphics.fill(avatarX, avatarY, avatarX + NEW_AVATAR_SIZE, avatarY + NEW_AVATAR_SIZE, alphaBits | 0x111111);
            }
        }
        if (hurtFlashFactor > 0.0f) {
            int damageAlphaInt = (int) (alphaInt * hurtFlashFactor * 0.6f);
            graphics.fill(avatarX, avatarY, avatarX + NEW_AVATAR_SIZE, avatarY + NEW_AVATAR_SIZE, (damageAlphaInt << 24) | 0xFF0000);
        }
        if (healFlashFactor > 0.0f) {
            int healAlphaInt = (int) (alphaInt * healFlashFactor * 0.62f);
            graphics.fill(avatarX, avatarY, avatarX + NEW_AVATAR_SIZE, avatarY + NEW_AVATAR_SIZE, (healAlphaInt << 24) | 0x55FF55);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, AVATAR_MASK_TEXTURE_ID, avatarX, avatarY, 0f, 0f, NEW_AVATAR_SIZE, NEW_AVATAR_SIZE, avatarMaskW, avatarMaskH, avatarMaskW, avatarMaskH);
        graphics.pose().popMatrix();
        graphics.pose().popMatrix();
    }

    private void renderNewTexture(Minecraft client, String name, String healthText, float healthRatio, float absorptionRatio, long now) {
        ensureNativeLoaded();
        float targetScale = Math.max(1f, (float) client.getWindow().getGuiScale() * Math.max(0.5f, Config.targetHudScale));
        int targetW = Math.max(1, Math.round(NEW_HUD_WIDTH * targetScale));
        int targetH = Math.max(1, Math.round(NEW_HUD_HEIGHT * targetScale));
        int healthKey = Math.round(healthRatio * 120f);
        int absorptionKey = Math.round(absorptionRatio * 80f);
        int healthTextAnimKey = getHealthTextAnimKey(now);
        if (dynamicTexture != null && targetW == textureW && targetH == textureH && name.equals(lastTextureName) && healthText.equals(lastTextureHealthText) && healthTextAnimKey == lastTextureHealthTextAnim && healthKey == lastTextureHealth && absorptionKey == lastTextureAbsorption) return;

        if (surface == null || dynamicTexture == null || targetW != textureW || targetH != textureH) {
            destroyTexture(client);
            SurfaceProps props = new SurfaceProps(false, PixelGeometry.RGB_H);
            surface = Surface.makeRaster(new ImageInfo(new ColorInfo(ColorType.RGBA_8888, ColorAlphaType.UNPREMUL, null), targetW, targetH), 0, props);
            dynamicTexture = new DynamicTexture("pvp_utils:target_hud_new", targetW, targetH, false);
            client.getTextureManager().register(TEXTURE_ID, dynamicTexture);
            textureW = targetW;
            textureH = targetH;
            lastTextureName = "";
            lastTextureHealthText = "";
        }

        Canvas c = surface.getCanvas();
        c.restoreToCount(1);
        c.resetMatrix();
        c.clear(0x00000000);
        c.save();
        c.scale(targetScale, targetScale);
        try (Paint bg = new Paint()) {
            bg.setColor(0xFFFFFFFF);
            bg.setAntiAlias(true);
            c.drawRRect(RRect.makeXYWH(0f, 0f, NEW_HUD_WIDTH, NEW_HUD_HEIGHT, 16f), bg);
        }
        try (Paint avatar = new Paint()) {
            avatar.setColor(0xFFFFFFFF);
            avatar.setAntiAlias(true);
            c.drawRRect(RRect.makeXYWH(12f, 10f, NEW_AVATAR_SIZE, NEW_AVATAR_SIZE, 9f), avatar);
        }

        FontRenderer.drawText(c, name, 60f, 24f, 13f, 0xFF202027);
        drawAnimatedHealthText(c, healthText, now);

        float barX = 60f;
        float barY = 45f;
        float barW = 112f;
        float barH = 7f;
        try (Paint track = new Paint()) {
            track.setColor(0x2D000000);
            track.setAntiAlias(true);
            c.drawRRect(RRect.makeXYWH(barX, barY, barW, barH, barH * 0.5f), track);
        }
        float fillW = Math.max(barH, barW * Mth.clamp(healthRatio, 0f, 1f));
        try (Paint fill = new Paint()) {
            fill.setColor(0xFF000000 | (getHealthColor(Mth.clamp(healthRatio, 0f, 1f)) & 0xFFFFFF));
            fill.setAntiAlias(true);
            c.drawRRect(RRect.makeXYWH(barX, barY, fillW, barH, barH * 0.5f), fill);
        }
        if (absorptionRatio > 0.01f) {
            float absorbW = Math.min(barW, barW * Mth.clamp(absorptionRatio, 0f, 1f));
            try (Paint absorb = new Paint()) {
                absorb.setColor(0xFFF5B83D);
                absorb.setAntiAlias(true);
                c.drawRRect(RRect.makeXYWH(barX + barW - absorbW, barY, absorbW, barH, barH * 0.5f), absorb);
            }
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
        lastTextureHealthText = healthText;
        lastTextureHealth = healthKey;
        lastTextureAbsorption = absorptionKey;
        lastTextureHealthTextAnim = healthTextAnimKey;
    }

    private void updateHealthTextAnimation(float value, long now) {
        String text = String.format(java.util.Locale.ROOT, "%.1f HP", value);
        if (currentHealthText.isEmpty()) {
            currentHealthText = text;
            previousHealthText = text;
            lastHealthTextValue = value;
            return;
        }
        if (!text.equals(currentHealthText)) {
            previousHealthText = currentHealthText;
            healthTextDirection = value > lastHealthTextValue ? 1 : -1;
            healthTextAnimStart = now;
            currentHealthText = text;
            lastHealthTextValue = value;
        }
    }

    private void drawAnimatedHealthText(Canvas c, String healthText, long now) {
        float progress = healthTextAnimStart == 0L ? 1f : Mth.clamp((now - healthTextAnimStart) / (float) HEALTH_TEXT_ANIM_DURATION, 0f, 1f);
        float eased = 1f - (1f - progress) * (1f - progress) * (1f - progress);
        float baseY = 40f;
        float height = 14f;
        float x = 60f;
        c.save();
        c.clipRect(Rect.makeXYWH(58f, 28f, 72f, 16f));
        for (int i = 0; i < healthText.length(); i++) {
            String ch = healthText.substring(i, i + 1);
            String oldCh = i < previousHealthText.length() ? previousHealthText.substring(i, i + 1) : ch;
            boolean digit = Character.isDigit(ch.charAt(0));
            boolean changed = digit && progress < 1f && healthTextDirection != 0 && !ch.equals(oldCh);
            float w = FontRenderer.measureTextWidth(ch, 10f);
            if (changed) {
                float oldY = baseY + (healthTextDirection > 0 ? -height * eased : height * eased);
                float newY = baseY + (healthTextDirection > 0 ? height * (1f - eased) : -height * (1f - eased));
                FontRenderer.drawText(c, oldCh, x, oldY, 10f, 0xAA5C5870);
                FontRenderer.drawText(c, ch, x, newY, 10f, 0xAA5C5870);
            } else {
                FontRenderer.drawText(c, ch, x, baseY, 10f, 0xAA5C5870);
            }
            x += w;
        }
        c.restore();
    }

    private int getHealthTextAnimKey(long now) {
        if (healthTextAnimStart == 0L) return 100;
        float progress = Mth.clamp((now - healthTextAnimStart) / (float) HEALTH_TEXT_ANIM_DURATION, 0f, 1f);
        if (progress >= 1f) return 100;
        return Math.round(progress * 24f);
    }

    private void resetHealthTextAnimation() {
        currentHealthText = "";
        previousHealthText = "";
        healthTextAnimStart = 0L;
        healthTextDirection = 0;
        lastHealthTextValue = -1f;
    }

    private void updateHealthTransition(float currentHealth, long now) {
        if (lastObservedHealth < 0f) {
            lastObservedHealth = currentHealth;
            return;
        }
        if (currentHealth > lastObservedHealth + 0.001f) {
            lastHealTime = now;
        } else if (currentHealth < lastObservedHealth - 0.001f) {
            lastDamageTime = now;
        }
        lastObservedHealth = currentHealth;
    }

    private float getFlashFactor(long now, long startTime) {
        if (startTime <= 0L) return 0.0f;
        long elapsed = now - startTime;
        if (elapsed < 0L || elapsed >= DAMAGE_FLASH_DURATION) return 0.0f;
        return 1.0f - (float) elapsed / DAMAGE_FLASH_DURATION;
    }

    private String truncateName(String rawName) {
        if (rawName.equals(lastRawName)) return lastTruncatedName;

        lastRawName = rawName;
        if (FontRenderer.measureTextWidth(rawName, 13f) <= 95f) {
            lastTruncatedName = rawName;
            return lastTruncatedName;
        }

        int low = 1;
        int high = rawName.length();
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (FontRenderer.measureTextWidth(rawName.substring(0, mid) + "...", 13f) <= 95f) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        lastTruncatedName = rawName.substring(0, low) + "...";
        return lastTruncatedName;
    }

    private void renderAvatarMaskTexture(Minecraft client) {
        ensureNativeLoaded();
        float targetScale = Math.max(1f, (float) client.getWindow().getGuiScale() * Math.max(0.5f, Config.targetHudScale));
        int targetW = Math.max(1, Math.round(NEW_AVATAR_SIZE * targetScale));
        int targetH = Math.max(1, Math.round(NEW_AVATAR_SIZE * targetScale));
        if (avatarMaskTexture != null && targetW == avatarMaskW && targetH == avatarMaskH) return;

        if (avatarMaskSurface != null) {
            avatarMaskSurface.close();
            avatarMaskSurface = null;
        }
        if (avatarMaskTexture != null) {
            client.getTextureManager().release(AVATAR_MASK_TEXTURE_ID);
            avatarMaskTexture = null;
        }

        SurfaceProps props = new SurfaceProps(false, PixelGeometry.RGB_H);
        avatarMaskSurface = Surface.makeRaster(new ImageInfo(new ColorInfo(ColorType.RGBA_8888, ColorAlphaType.UNPREMUL, null), targetW, targetH), 0, props);
        avatarMaskTexture = new DynamicTexture("pvp_utils:target_hud_avatar_mask", targetW, targetH, false);
        client.getTextureManager().register(AVATAR_MASK_TEXTURE_ID, avatarMaskTexture);
        avatarMaskW = targetW;
        avatarMaskH = targetH;

        Canvas c = avatarMaskSurface.getCanvas();
        c.restoreToCount(1);
        c.resetMatrix();
        c.clear(0x00000000);
        c.save();
        c.scale(targetScale, targetScale);
        try (Paint cover = new Paint()) {
            cover.setColor(0xFFFFFFFF);
            cover.setAntiAlias(true);
            c.drawRect(io.github.humbleui.types.Rect.makeXYWH(0f, 0f, NEW_AVATAR_SIZE, NEW_AVATAR_SIZE), cover);
        }
        try (Paint hole = new Paint()) {
            hole.setBlendMode(BlendMode.CLEAR);
            hole.setAntiAlias(true);
            c.drawRRect(RRect.makeXYWH(-0.75f, -0.75f, NEW_AVATAR_SIZE + 1.5f, NEW_AVATAR_SIZE + 1.5f, 9.75f), hole);
        }
        c.restore();

        Pixmap pixmap = new Pixmap();
        if (!avatarMaskSurface.peekPixels(pixmap)) {
            pixmap.close();
            return;
        }
        long addr = pixmap.getAddr();
        int byteSize = avatarMaskH * pixmap.getRowBytes();
        ByteBuffer buf = MemoryUtil.memByteBuffer(addr, byteSize);
        GpuTexture gpuTexture = avatarMaskTexture.getTexture();
        RenderSystem.getDevice().createCommandEncoder()
                .writeToTexture(gpuTexture, buf, NativeImage.Format.RGBA, 0, 0, 0, 0, avatarMaskW, avatarMaskH);
        pixmap.close();
    }

    private float easeOutBack(float value) {
        float t = Mth.clamp(value, 0f, 1f) - 1f;
        return 1f + t * t * (1.55f * t + 0.55f);
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
        if (avatarMaskSurface != null) {
            avatarMaskSurface.close();
            avatarMaskSurface = null;
        }
        if (dynamicTexture != null) {
            client.getTextureManager().release(TEXTURE_ID);
            dynamicTexture = null;
        }
        if (avatarMaskTexture != null) {
            client.getTextureManager().release(AVATAR_MASK_TEXTURE_ID);
            avatarMaskTexture = null;
        }
        textureW = -1;
        textureH = -1;
        avatarMaskW = -1;
        avatarMaskH = -1;
        lastTextureName = "";
        lastTextureHealthText = "";
        lastTextureHealth = -1;
        lastTextureAbsorption = -1;
        lastTextureHealthTextAnim = -1;
    }

    private int getHealthColor(float ratio) {
        int r, g;
        if (ratio > 0.5f) {
            float t = (ratio - 0.5f) * 2f;
            r = (int) (255 * (1f - t));
            g = 255;
        } else {
            float t = ratio * 2f;
            r = 255;
            g = (int) (255 * t);
        }
        return 0xFF000000 | (r << 16) | (g << 8);
    }
}
