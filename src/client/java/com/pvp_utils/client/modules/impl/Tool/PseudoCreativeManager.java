package com.pvp_utils.client.modules.impl.Tool;

import com.pvp_utils.Config;
import com.pvp_utils.client.util.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

public final class PseudoCreativeManager {
    private static boolean active;
    private static boolean warned;
    private static double originalX;
    private static double originalY;
    private static double originalZ;
    private static float originalYRot;
    private static float originalXRot;
    private static boolean originalMayBuild;
    private static boolean originalMayFly;
    private static boolean originalFlying;
    private static boolean originalInstabuild;
    private static GameType originalGameType;
    private static ItemStack[] inventorySnapshot;

    private PseudoCreativeManager() {
    }

    public static void setEnabled(boolean enabled) {
        Config.pseudoCreative = enabled;
        if (enabled) {
            warned = false;
            tryStart(Minecraft.getInstance());
        } else {
            stop(Minecraft.getInstance());
        }
    }

    public static void tick(Minecraft client) {
        if (!Config.pseudoCreative) {
            if (active) stop(client);
            return;
        }
        if (client.player == null || client.level == null || client.gameMode == null) {
            if (active) stop(client);
            Config.pseudoCreative = false;
            Config.save();
            return;
        }
        if (!active) {
            tryStart(client);
        }
        if (!active) return;
        if (client.gameMode.getPlayerMode() != GameType.CREATIVE) {
            client.gameMode.setLocalMode(GameType.CREATIVE);
        }
        Abilities abilities = client.player.getAbilities();
        abilities.mayBuild = true;
        abilities.mayfly = true;
        abilities.instabuild = true;
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean blocksMovement() {
        return active;
    }

    public static boolean blocksCreativePacket() {
        return active;
    }

    public static boolean blocksDrop() {
        return active;
    }

    private static void tryStart(Minecraft client) {
        if (active || client == null || client.player == null || client.level == null || client.gameMode == null) {
            return;
        }
        GameType mode = client.gameMode.getPlayerMode();
        if (mode == GameType.CREATIVE) {
            if (!warned) {
                ChatUtils.error("你创造开你妈呢？");
                warned = true;
            }
            return;
        }
        if (mode == GameType.SPECTATOR && !warned) {
            ChatUtils.error("你旁观开你妈呢？");
            warned = true;
        }
        LocalPlayer player = client.player;
        originalGameType = client.gameMode.getPlayerMode();
        originalX = player.getX();
        originalY = player.getY();
        originalZ = player.getZ();
        originalYRot = player.getYRot();
        originalXRot = player.getXRot();
        Abilities abilities = player.getAbilities();
        originalMayBuild = abilities.mayBuild;
        originalMayFly = abilities.mayfly;
        originalFlying = abilities.flying;
        originalInstabuild = abilities.instabuild;
        inventorySnapshot = new ItemStack[player.getInventory().getContainerSize()];
        for (int i = 0; i < inventorySnapshot.length; i++) {
            inventorySnapshot[i] = player.getInventory().getItem(i).copy();
        }
        abilities.mayBuild = true;
        abilities.mayfly = true;
        abilities.instabuild = true;
        abilities.flying = false;
        client.gameMode.setLocalMode(GameType.CREATIVE);
        active = true;
        client.setScreen(new CreativeModeInventoryScreen(player, player.connection.enabledFeatures(), player.canUseGameMasterBlocks()));
    }

    private static void stop(Minecraft client) {
        if (!active) return;
        LocalPlayer player = client == null ? null : client.player;
        if (player != null) {
            if (client.gameMode != null && originalGameType != null) {
                client.gameMode.setLocalMode(originalGameType);
            }
            if (inventorySnapshot != null) {
                for (int i = 0; i < inventorySnapshot.length && i < player.getInventory().getContainerSize(); i++) {
                    player.getInventory().setItem(i, inventorySnapshot[i].copy());
                }
                player.inventoryMenu.broadcastChanges();
            }
            player.setPos(originalX, originalY, originalZ);
            player.setYRot(originalYRot);
            player.setXRot(originalXRot);
            player.setDeltaMovement(0.0, 0.0, 0.0);
            Abilities abilities = player.getAbilities();
            abilities.mayBuild = originalMayBuild;
            abilities.mayfly = originalMayFly;
            abilities.flying = originalFlying;
            abilities.instabuild = originalInstabuild;
        }
        if (client != null && client.screen instanceof CreativeModeInventoryScreen) {
            client.setScreen(null);
        }
        inventorySnapshot = null;
        active = false;
    }
}
