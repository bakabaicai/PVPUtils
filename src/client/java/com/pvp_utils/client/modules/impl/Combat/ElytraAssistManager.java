package com.pvp_utils.client.modules.impl.Combat;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

public final class ElytraAssistManager {
    private static int fireworkCooldown;
    private static boolean wasJumpDown;

    private ElytraAssistManager() {}

    public static void tick(Minecraft client) {
        if (!Config.elytraAssist || client.player == null || client.level == null || client.gameMode == null || client.screen != null) {
            reset(client);
            return;
        }

        LocalPlayer player = client.player;
        boolean jumpDown = client.options.keyJump.isDown();
        if (Config.elytraAutoDeploy && jumpDown && !wasJumpDown && canStartFlying(player)) {
            player.connection.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            player.tryToStartFallFlying();
        }
        wasJumpDown = jumpDown;

        if (fireworkCooldown > 0) fireworkCooldown--;
        if (Config.elytraAutoFirework && player.isFallFlying() && fireworkCooldown <= 0 && isHoldingFirework(player) && !hasFireworkBoost(client, player)) {
            InteractionHand hand = player.getMainHandItem().is(Items.FIREWORK_ROCKET) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            client.gameMode.useItem(player, hand);
            player.swing(hand);
            fireworkCooldown = 8;
        }
    }

    private static boolean canStartFlying(LocalPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return chest.is(Items.ELYTRA)
                && !player.onGround()
                && !player.isFallFlying()
                && !player.isInWater()
                && !player.isInLava()
                && !player.onClimbable()
                && !player.getAbilities().flying;
    }

    private static boolean isHoldingFirework(LocalPlayer player) {
        return player.getMainHandItem().is(Items.FIREWORK_ROCKET) || player.getOffhandItem().is(Items.FIREWORK_ROCKET);
    }

    private static boolean hasFireworkBoost(Minecraft client, LocalPlayer player) {
        AABB area = player.getBoundingBox().inflate(2.5);
        return !client.level.getEntities(player, area, entity -> entity instanceof FireworkRocketEntity).isEmpty();
    }

    private static void reset(Minecraft client) {
        fireworkCooldown = 0;
        wasJumpDown = client != null && client.options.keyJump.isDown();
    }
}
