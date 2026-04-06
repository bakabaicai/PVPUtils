package com.old_animation.client.gui;

import com.old_animation.AnimationConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

public class TargetHudRenderer {
    private static final TargetHudRenderer INSTANCE = new TargetHudRenderer();

    private LivingEntity target = null;
    private long lastHitTime = 0;
    private long appearanceTime = 0;
    private boolean isFullyHidden = true;

    private long lastDamageTime = 0;
    private static final long DAMAGE_FLASH_DURATION = 300;

    private static final long HIDE_DELAY = 3000;
    private static final long ANIM_DURATION = 200;

    private static final int HUD_WIDTH = 160;
    private static final int HUD_HEIGHT = 40;
    private static final int AVATAR_SIZE = 28;
    private static final int PADDING = 6;
    private static final int BORDER = 1;

    public static TargetHudRenderer getInstance() {
        return INSTANCE;
    }

    public void onHit(LivingEntity entity) {
        if (!AnimationConfig.targetHud) return;

        long now = System.currentTimeMillis();

        if (this.target == null || isFullyHidden) {
            this.appearanceTime = now;
            this.isFullyHidden = false;
        }
        this.lastDamageTime = now;
        this.target = entity;
        this.lastHitTime = now;
    }

    public void render(GuiGraphics graphics) {
        if (!AnimationConfig.targetHud || target == null) return;

        long now = System.currentTimeMillis();

        float fadeIn = (float) (now - appearanceTime) / ANIM_DURATION;
        float fadeOut = 1.0f - (float) (now - (lastHitTime + HIDE_DELAY)) / ANIM_DURATION;

        float alpha = Mth.clamp(Math.min(fadeIn, fadeOut), 0.0f, 1.0f);

        if (alpha <= 0.0f) {
            if (now - lastHitTime > HIDE_DELAY) {
                isFullyHidden = true;
                target = null;
            }
            return;
        }

        Minecraft client = Minecraft.getInstance();
        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();

        int x = (int) (screenW * 0.5f + AnimationConfig.targetHudX);
        int y = (int) (screenH * 0.5f + AnimationConfig.targetHudY);
        x = Math.max(0, Math.min(x, screenW - HUD_WIDTH));
        y = Math.max(0, Math.min(y, screenH - HUD_HEIGHT));

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

        long damageElapsed = now - lastDamageTime;
        if (damageElapsed < DAMAGE_FLASH_DURATION) {
            float damageFactor = 1.0f - (float) damageElapsed / DAMAGE_FLASH_DURATION;
            int damageAlphaInt = (int) (alphaInt * damageFactor * 0.6f);
            int flashColor = (damageAlphaInt << 24) | 0xFF0000;
            graphics.fill(avatarX, avatarY, avatarX2, avatarY2, flashColor);
        }

        int infoX = avatarX + AVATAR_SIZE + PADDING;
        int infoW = HUD_WIDTH - BORDER - PADDING - AVATAR_SIZE - PADDING * 2 - BORDER;

        String name = target.getDisplayName().getString();
        if (name.length() > 16) name = name.substring(0, 16) + "..";
        graphics.drawString(client.font, Component.literal(name), infoX, y + PADDING + 2, whiteWithAlpha, false);

        float maxHealth = target.getMaxHealth();
        float currentHealth = target.getHealth();
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