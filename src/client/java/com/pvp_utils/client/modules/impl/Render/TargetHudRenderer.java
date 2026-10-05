package com.pvp_utils.client.modules.impl.Render;

import io.github.humbleui.types.RRect;

import com.pvp_utils.client.render.skia.SkijaUi;

import com.pvp_utils.client.render.skia.SkijaRenderer;
import com.pvp_utils.client.gui.TargetScoreboardUtil;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.skia.LiquidGlassRenderer;
import io.github.humbleui.skija.*;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.phys.Vec3;

public class TargetHudRenderer {
    private static final TargetHudRenderer INSTANCE = new TargetHudRenderer();

    private LivingEntity target = null;
    private long lastHitTime = 0;
    private long appearanceTime = 0;
    private boolean isFullyHidden = true;
    private boolean editPreview = false;
    private boolean wasEditActive = false;

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
    private final Paint newHudBgPaint = new Paint();
    private final Paint newHudAvatarPaint = new Paint();
    private final Paint newHudTrackPaint = new Paint();
    private final Paint newHudFillPaint = new Paint();
    private final Paint newHudAbsorbPaint = new Paint();
    private PlayerSkin cachedPlayerSkin = null;
    private int cachedPlayerSkinEntityId = Integer.MIN_VALUE;
    private final float[] healthCharWidthCache = new float[128];
    private final boolean[] healthCharWidthCached = new boolean[128];

    private long lastDamageTime = 0;
    private long lastHealTime = 0;
    private float lastObservedHealth = -1f;
    private float lastAttackDistance = -1f;
    private long lastAttackDistanceTime = 0;
    private String currentDistText = "";
    private String previousDistText = "";
    private long distTextAnimStart = 0L;
    private int distTextDirection = 0;
    private float lastDistTextValue = -1f;
    private static final long DAMAGE_FLASH_DURATION = 300;

    private static final long HIDE_DELAY = 3000;
    private static final long ANIM_DURATION = 200;
    private static final long HEALTH_TEXT_ANIM_DURATION = 220;
    private static final long ATTACK_DISTANCE_DISPLAY_DURATION = 2000;

    private static final int HUD_WIDTH = 160;
    private static final int HUD_HEIGHT = 40;
    private static final int NEW_HUD_WIDTH = 190;
    private static final int NEW_HUD_HEIGHT = 58;
    private static final int NEW_AVATAR_SIZE = 38;
    private static final float NEW_AVATAR_RADIUS = 12f;
    private static final int AVATAR_SIZE = 28;
    private static final int PADDING = 6;
    private static final int BORDER = 1;

    public static TargetHudRenderer getInstance() {
        return INSTANCE;
    }

    private TargetHudRenderer() {
        newHudBgPaint.setAntiAlias(true);
        newHudAvatarPaint.setAntiAlias(true);
        newHudTrackPaint.setAntiAlias(true);
        newHudFillPaint.setAntiAlias(true);
        newHudAbsorbPaint.setAntiAlias(true);
    }

    private PlayerSkin resolvePlayerSkin(Minecraft client, Player player) {
        int entityId = player.getId();
        if (cachedPlayerSkin != null && cachedPlayerSkinEntityId == entityId) {
            return cachedPlayerSkin;
        }
        cachedPlayerSkin = client.getSkinManager().createLookup(player.getGameProfile(), false).get();
        cachedPlayerSkinEntityId = entityId;
        return cachedPlayerSkin;
    }

    private void invalidatePlayerSkinCache(Entity entity) {
        if (entity == null || entity.getId() == cachedPlayerSkinEntityId) {
            cachedPlayerSkin = null;
            cachedPlayerSkinEntityId = Integer.MIN_VALUE;
        }
    }

    private float getHealthCharWidth(String ch) {
        if (ch.length() == 1) {
            char c = ch.charAt(0);
            if (c < healthCharWidthCache.length) {
                if (!healthCharWidthCached[c]) {
                    healthCharWidthCache[c] = SkijaUi.textWidth(ch, 10f);
                    healthCharWidthCached[c] = true;
                }
                return healthCharWidthCache[c];
            }
        }
        return SkijaUi.textWidth(ch, 10f);
    }

    public void onHit(LivingEntity entity) {
        if (!Config.targetHud || entity == null) return;

        long now = System.currentTimeMillis();

        if (this.target == null || isFullyHidden) {
            this.appearanceTime = now;
            this.isFullyHidden = false;
        }
        if (this.target != entity) {
            invalidatePlayerSkinCache(this.target);
        }
        float currentHealth = entity.getHealth();
        if (this.target != entity || lastObservedHealth < 0f || currentHealth < lastObservedHealth - 0.001f) {
            this.lastDamageTime = now;
        } else if (currentHealth > lastObservedHealth + 0.001f) {
            this.lastHealTime = now;
        }
        this.target = entity;
        this.lastHitTime = now;

        Minecraft client = Minecraft.getInstance();
        if (client.player != null && Config.attackReachDisplay) {
            Vec3 playerPos = client.player.position().add(0, client.player.getEyeHeight(), 0);
            Vec3 targetPos = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
            this.lastAttackDistance = (float) playerPos.distanceTo(targetPos);
            this.lastAttackDistanceTime = now;
        }
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

        if ((!Config.targetHud && !editPreview) || target == null) {
            if (!Config.targetHud) {
                resetNewHudRuntimeState();
            }
            return;
        }

        if (!target.isAlive() && now - lastHitTime < HIDE_DELAY) {
            lastHitTime = now - HIDE_DELAY;
        }

        float fadeIn = (float) (now - appearanceTime) / ANIM_DURATION;
        float fadeOut = 1.0f - (float) (now - (lastHitTime + HIDE_DELAY)) / ANIM_DURATION;

        float alpha = Mth.clamp(Math.min(fadeIn, fadeOut), 0.0f, 1.0f);

        if (alpha <= 0.0f) {
            if (now - lastHitTime > HIDE_DELAY || !target.isAlive()) {
                isFullyHidden = true;
                invalidatePlayerSkinCache(target);
                target = null;
                resetHealthTextAnimation();
                resetNewHudRuntimeState();
                lastObservedHealth = -1f;
            }
            return;
        }

        updateAttackDistance(client, now);

        if (Config.targetHudMode == Config.TargetHudMode.NEW || Config.targetHudMode == Config.TargetHudMode.BLUR || Config.targetHudMode == Config.TargetHudMode.LIQUID_GLASS) {
            renderNew(graphics, client, alpha, now, Config.targetHudMode == Config.TargetHudMode.BLUR);
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
                PlayerSkin skin = resolvePlayerSkin(client, player);
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

        if (attackReachActive(now)) {
            long elapsed = now - lastAttackDistanceTime;
            float distAlpha = elapsed < 150f ? elapsed / 150f : (elapsed > ATTACK_DISTANCE_DISPLAY_DURATION - 400f ? (float)(ATTACK_DISTANCE_DISPLAY_DURATION - elapsed) / 400f : 1.0f);
            distAlpha = Math.max(0f, Math.min(1f, distAlpha));
            int distAlphaInt = Math.round(distAlpha * alpha * 255);
            int distColor = (distAlphaInt << 24) | 0xFFAA00;
            String distText = String.format(java.util.Locale.ROOT, "%.2fm", lastAttackDistance);
            int nameWidth = client.font.width(name);
            graphics.drawString(client.font, Component.literal(distText), infoX + nameWidth + 4, y + PADDING + 2, distColor, false);
        }

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

    private void renderNew(GuiGraphics graphics, Minecraft client, float alpha, long now, boolean blurMode) {
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
        float drawX = cx + (x - cx) * drawScale;
        float drawY = cy + (y - cy) * drawScale;
        float drawW = scaledW * drawScale;
        float drawH = scaledH * drawScale;
        float drawRadius = 16f * hudScale * drawScale;

        boolean transparentMode = blurMode || Config.targetHudMode == Config.TargetHudMode.LIQUID_GLASS;
        if (blurMode) {
            SkijaRenderer.draw(blurCanvas -> {
                SkijaRenderer.drawBlurredBackdrop(blurCanvas, RRect.makeXYWH(drawX, drawY, drawW, drawH, drawRadius), drawX, drawY, drawW, drawH, Math.max(0f, Math.min(2f, Config.skiaBlurStrength)) * 10.5f);
                SkijaUi.rounded(blurCanvas, drawX, drawY, drawW, drawH, drawRadius, Config.skiaBlurTintColor());
            });
        } else if (Config.targetHudMode == Config.TargetHudMode.LIQUID_GLASS) {
            LiquidGlassRenderer.getInstance().renderPanel(client, drawX, drawY, drawW, drawH, drawRadius,
            LiquidGlassRenderer.panelTint(), Config.liquidGlassShadow, Config.liquidGlassHighlight, 0f, 1);
        }

        SkijaRenderer.draw(c -> {
            c.translate(drawX, drawY);
            c.scale(hudScale * drawScale, hudScale * drawScale);
            drawNewBase(c, name, transparentMode);
            drawNewOverlay(c, currentHealthText, animatedHealthRatio, animatedAbsorptionRatio, now, transparentMode);
        });

        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, cy);
        graphics.pose().scale(drawScale, drawScale);
        graphics.pose().translate(-cx, -cy);
        graphics.pose().translate(x, y);
        graphics.pose().scale(hudScale, hudScale);
        graphics.pose().translate(-x, -y);

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
            renderNewAvatar(client, player, drawX, drawY, hudScale * drawScale, avatarScale,
            hurtFlashFactor, healFlashFactor, alphaInt);
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
        graphics.pose().popMatrix();
        graphics.pose().popMatrix();
    }

    private void drawNewBase(Canvas c, String name, boolean blurMode) {
        if (!blurMode) {
            newHudBgPaint.setColor(newHudCardColor());
            c.drawRRect(RRect.makeXYWH(0f, 0f, NEW_HUD_WIDTH, NEW_HUD_HEIGHT, 16f), newHudBgPaint);
        }
        newHudAvatarPaint.setColor(newHudAvatarBackplateColor());
        c.drawRRect(RRect.makeXYWH(12f, 10f, NEW_AVATAR_SIZE, NEW_AVATAR_SIZE, NEW_AVATAR_RADIUS), newHudAvatarPaint);

        SkijaUi.text(c, name, 60f, (24f) + SkijaUi.textMetrics(13f).getAscent(), SkijaUi.textMetrics(13f).getDescent() - SkijaUi.textMetrics(13f).getAscent(), newHudPrimaryTextColor(blurMode), 13f);
    }

    private void drawNewOverlay(Canvas c, String healthText, float healthRatio, float absorptionRatio, long now, boolean blurMode) {
        boolean showDist = attackReachActive(now);
        if (showDist) {
            updateDistTextAnimation(lastAttackDistance, now);
        } else {
            currentDistText = "";
            previousDistText = "";
            distTextAnimStart = 0L;
            distTextDirection = 0;
            lastDistTextValue = -1f;
        }
        drawAnimatedHealthText(c, healthText, now, blurMode);
        if (showDist) {
            float healthTextEndX = 60f + SkijaUi.textWidth(healthText, 10f) + 4f;
            drawAnimatedDistText(c, currentDistText, healthTextEndX, now, blurMode);
        }

        float barX = 60f;
        float barY = 45f;
        float barW = 112f;
        float barH = 7f;
        newHudTrackPaint.setColor(newHudTrackColor(blurMode));
        c.drawRRect(RRect.makeXYWH(barX, barY, barW, barH, barH * 0.5f), newHudTrackPaint);
        float fillW = Math.max(barH, barW * Mth.clamp(healthRatio, 0f, 1f));
        newHudFillPaint.setColor(0xFF000000 | (getHealthColor(Mth.clamp(healthRatio, 0f, 1f)) & 0xFFFFFF));
        c.drawRRect(RRect.makeXYWH(barX, barY, fillW, barH, barH * 0.5f), newHudFillPaint);
        if (absorptionRatio > 0.01f) {
            float absorbW = Math.min(barW, barW * Mth.clamp(absorptionRatio, 0f, 1f));
            newHudAbsorbPaint.setColor(0xFFF5B83D);
            c.drawRRect(RRect.makeXYWH(barX + barW - absorbW, barY, absorbW, barH, barH * 0.5f), newHudAbsorbPaint);
        }
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

    private void updateDistTextAnimation(float distance, long now) {
        String text = String.format(java.util.Locale.ROOT, "%.2fm", distance);
        if (currentDistText.isEmpty()) {
            currentDistText = text;
            previousDistText = text;
            lastDistTextValue = distance;
            return;
        }
        if (!text.equals(currentDistText)) {
            previousDistText = currentDistText;
            distTextDirection = distance > lastDistTextValue ? 1 : -1;
            distTextAnimStart = now;
            currentDistText = text;
            lastDistTextValue = distance;
        }
    }

    private void drawAnimatedHealthText(Canvas c, String healthText, long now, boolean blurMode) {
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
            float w = getHealthCharWidth(ch);
            if (changed) {
                float oldY = baseY + (healthTextDirection > 0 ? -height * eased : height * eased);
                float newY = baseY + (healthTextDirection > 0 ? height * (1f - eased) : -height * (1f - eased));
                SkijaUi.text(c, oldCh, x, (oldY) + SkijaUi.textMetrics(10f).getAscent(), SkijaUi.textMetrics(10f).getDescent() - SkijaUi.textMetrics(10f).getAscent(), newHudMutedTextColor(blurMode), 10f);
                SkijaUi.text(c, ch, x, (newY) + SkijaUi.textMetrics(10f).getAscent(), SkijaUi.textMetrics(10f).getDescent() - SkijaUi.textMetrics(10f).getAscent(), newHudMutedTextColor(blurMode), 10f);
            } else {
                SkijaUi.text(c, ch, x, (baseY) + SkijaUi.textMetrics(10f).getAscent(), SkijaUi.textMetrics(10f).getDescent() - SkijaUi.textMetrics(10f).getAscent(), newHudMutedTextColor(blurMode), 10f);
            }
            x += w;
        }
        c.restore();
    }

    private void drawAnimatedDistText(Canvas c, String distText, float startX, long now, boolean blurMode) {
        if (distText.isEmpty()) return;
        float progress = distTextAnimStart == 0L ? 1f : Mth.clamp((now - distTextAnimStart) / (float) HEALTH_TEXT_ANIM_DURATION, 0f, 1f);
        float eased = 1f - (1f - progress) * (1f - progress) * (1f - progress);
        float baseY = 40f;
        float height = 14f;
        float x = startX;
        c.save();
        c.clipRect(Rect.makeXYWH(startX - 2f, 28f, 80f, 16f));
        for (int i = 0; i < distText.length(); i++) {
            String ch = distText.substring(i, i + 1);
            String oldCh = i < previousDistText.length() ? previousDistText.substring(i, i + 1) : ch;
            boolean digit = Character.isDigit(ch.charAt(0));
            boolean changed = digit && progress < 1f && distTextDirection != 0 && !ch.equals(oldCh);
            float w = getHealthCharWidth(ch);
            if (changed) {
                float oldY = baseY + (distTextDirection > 0 ? -height * eased : height * eased);
                float newY = baseY + (distTextDirection > 0 ? height * (1f - eased) : -height * (1f - eased));
                SkijaUi.text(c, oldCh, x, (oldY) + SkijaUi.textMetrics(10f).getAscent(), SkijaUi.textMetrics(10f).getDescent() - SkijaUi.textMetrics(10f).getAscent(), 0xFFFFAA00, 10f);
                SkijaUi.text(c, ch, x, (newY) + SkijaUi.textMetrics(10f).getAscent(), SkijaUi.textMetrics(10f).getDescent() - SkijaUi.textMetrics(10f).getAscent(), 0xFFFFAA00, 10f);
            } else {
                SkijaUi.text(c, ch, x, (baseY) + SkijaUi.textMetrics(10f).getAscent(), SkijaUi.textMetrics(10f).getDescent() - SkijaUi.textMetrics(10f).getAscent(), 0xFFFFAA00, 10f);
            }
            x += w;
        }
        c.restore();
    }

    private int newHudCardColor() {
        return Config.hudTheme == Config.HudTheme.LIGHT ? 0xF7F8FAFC : 0xE6111827;
    }

    private int newHudAvatarBackplateColor() {
        return Config.hudTheme == Config.HudTheme.LIGHT ? 0x66FFFFFF : 0x332A3345;
    }

    private int newHudPrimaryTextColor(boolean blurMode) {
        return blurMode ? Config.hudPrimaryTextColor() : (Config.hudTheme == Config.HudTheme.LIGHT ? 0xFF202027 : 0xFFF8FAFC);
    }

    private int newHudMutedTextColor(boolean blurMode) {
        return blurMode ? Config.hudMutedTextColor() : (Config.hudTheme == Config.HudTheme.LIGHT ? 0xAA5C5870 : 0xB8CBD5E1);
    }

    private int newHudTrackColor(boolean blurMode) {
        if (blurMode) {
            return Config.hudTheme == Config.HudTheme.LIGHT ? 0x33111827 : 0x2D000000;
        }
        return Config.hudTheme == Config.HudTheme.LIGHT ? 0x22111827 : 0x33FFFFFF;
    }

    private int getHealthTextAnimKey(long now) {
        if (healthTextAnimStart == 0L) return 100;
        float progress = Mth.clamp((now - healthTextAnimStart) / (float) HEALTH_TEXT_ANIM_DURATION, 0f, 1f);
        if (progress >= 1f) return 100;
        return Math.round(progress * 24f);
    }

    private int getDistTextAnimKey(long now) {
        if (distTextAnimStart == 0L) return 100;
        float progress = Mth.clamp((now - distTextAnimStart) / (float) HEALTH_TEXT_ANIM_DURATION, 0f, 1f);
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

    private void resetNewHudRuntimeState() {
        lastRenderTime = 0L;
        animatedHealthRatio = 1f;
        animatedAbsorptionRatio = 0f;
        lastAttackDistance = -1f;
        lastAttackDistanceTime = 0;
        currentDistText = "";
        previousDistText = "";
        distTextAnimStart = 0L;
        distTextDirection = 0;
        lastDistTextValue = -1f;
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

    private boolean attackReachActive(long now) {
        return Config.attackReachDisplay && lastAttackDistance >= 0f
        && now - lastAttackDistanceTime < ATTACK_DISTANCE_DISPLAY_DURATION;
    }

    private void updateAttackDistance(Minecraft client, long now) {
        if (client.player == null || target == null || !attackReachActive(now)) {
            return;
        }
        Vec3 playerPos = client.player.position().add(0, client.player.getEyeHeight(), 0);
        Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
        lastAttackDistance = (float) playerPos.distanceTo(targetPos);
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
        if (SkijaUi.textWidth(rawName, 13f) <= 95f) {
            lastTruncatedName = rawName;
            return lastTruncatedName;
        }

        int low = 1;
        int high = rawName.length();
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (SkijaUi.textWidth(rawName.substring(0, mid) + "...", 13f) <= 95f) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        lastTruncatedName = rawName.substring(0, low) + "...";
        return lastTruncatedName;
    }

    private void renderNewAvatar(Minecraft client, Player player, float x, float y, float scale,
    float avatarScale, float hurt, float heal, int alpha) {
        PlayerSkin skin = resolvePlayerSkin(client, player);
        net.minecraft.resources.Identifier texture = skin.body().texturePath();
        SkijaRenderer.submit(canvas -> {
            canvas.translate(x, y);
            canvas.scale(scale, scale);
            canvas.translate(12f + NEW_AVATAR_SIZE * 0.5f, 10f + NEW_AVATAR_SIZE * 0.5f);
            canvas.scale(avatarScale, avatarScale);
            canvas.translate(-NEW_AVATAR_SIZE * 0.5f, -NEW_AVATAR_SIZE * 0.5f);
            canvas.clipRRect(RRect.makeXYWH(0, 0, NEW_AVATAR_SIZE, NEW_AVATAR_SIZE, NEW_AVATAR_RADIUS), true);
            try (SkijaRenderer.BorrowedImage borrowed = SkijaRenderer.borrowTexture(texture);
            io.github.humbleui.skija.Paint paint = new io.github.humbleui.skija.Paint()) {
                if (borrowed == null) return;
                paint.setAlphaf(alpha / 255f);
                io.github.humbleui.types.Rect destination = io.github.humbleui.types.Rect.makeXYWH(0, 0, NEW_AVATAR_SIZE, NEW_AVATAR_SIZE);
                canvas.drawImageRect(borrowed.image(), io.github.humbleui.types.Rect.makeXYWH(8, 8, 8, 8),
                destination, io.github.humbleui.skija.SamplingMode.DEFAULT, paint, true);
                canvas.drawImageRect(borrowed.image(), io.github.humbleui.types.Rect.makeXYWH(40, 8, 8, 8),
                destination, io.github.humbleui.skija.SamplingMode.DEFAULT, paint, true);
                if (hurt > 0f) {
                    paint.setColor((Math.round(alpha * hurt * 0.6f) << 24) | 0xFF0000);
                    canvas.drawRect(destination, paint);
                }
                if (heal > 0f) {
                    paint.setColor((Math.round(alpha * heal * 0.62f) << 24) | 0x55FF55);
                    canvas.drawRect(destination, paint);
                }
            }
        });
    }

    private float easeOutBack(float value) {
        float t = Mth.clamp(value, 0f, 1f) - 1f;
        return 1f + t * t * (1.55f * t + 0.55f);
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
