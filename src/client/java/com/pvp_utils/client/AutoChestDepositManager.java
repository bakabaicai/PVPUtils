package com.pvp_utils.client;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class AutoChestDepositManager {
    private enum Phase { IDLE, WAIT_OPEN, WAIT_TRANSFER, WAIT_CLOSE }

    private static Phase phase = Phase.IDLE;
    private static BlockHitResult targetHit;
    private static int ticksRemaining;
    private static int transferWaitTimeout;
    private static boolean wasAttackDown;

    private AutoChestDepositManager() {}

    public static void onChestRendered(ChestBlockEntity chest) {
    }

    public static void tick(Minecraft client) {
        boolean attackDown = client.options.keyAttack.isDown();
        if (!attackDown && wasAttackDown) {
            tryStart(client);
        }
        wasAttackDown = attackDown;

        if (phase == Phase.IDLE) return;
        if (!Config.autoChestDeposit || client.player == null || client.gameMode == null) {
            reset();
            return;
        }

        if (ticksRemaining > 0) {
            ticksRemaining--;
            return;
        }

        switch (phase) {
            case WAIT_OPEN -> openChest(client);
            case WAIT_TRANSFER -> transferHeldItem(client);
            case WAIT_CLOSE -> closeChest(client.player);
            default -> {}
        }
    }

    public static boolean shouldBlockMovementInput() {
        return Config.autoChestDepositBlockMovement && phase != Phase.IDLE;
    }

    public static boolean shouldHideContainerScreen(Screen screen) {
        return phase != Phase.IDLE && screen instanceof AbstractContainerScreen<?>;
    }

    private static void tryStart(Minecraft client) {
        if (!Config.autoChestDeposit || phase != Phase.IDLE || client.player == null || client.level == null || client.screen != null) return;
        if (client.player.getMainHandItem().isEmpty()) return;

        BlockHitResult hit = getCurrentContainerHit(client);
        if (hit == null) return;

        targetHit = hit;
        ticksRemaining = Math.max(0, Config.autoChestDepositOpenDelay);
        phase = Phase.WAIT_OPEN;
    }

    private static BlockHitResult getCurrentContainerHit(Minecraft client) {
        if (client.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            if (client.level != null && isSupportedContainer(client, pos)) {
                return hit;
            }
        }
        return null;
    }

    private static boolean isSupportedContainer(Minecraft client, BlockPos pos) {
        BlockState state = client.level.getBlockState(pos);
        return getMenuProvider(client, pos) != null || state.getBlock() instanceof EnderChestBlock;
    }

    private static MenuProvider getMenuProvider(Minecraft client, BlockPos pos) {
        if (client.level == null) return null;
        return client.level.getBlockState(pos).getMenuProvider(client.level, pos);
    }

    private static void openChest(Minecraft client) {
        if (targetHit == null) {
            reset();
            return;
        }

        MultiPlayerGameMode gameMode = client.gameMode;
        LocalPlayer player = client.player;
        if (gameMode == null || player == null) {
            reset();
            return;
        }

        gameMode.useItemOn(player, InteractionHand.MAIN_HAND, targetHit);
        ticksRemaining = Math.max(0, Config.autoChestDepositTransferDelay);
        transferWaitTimeout = Math.max(40, ticksRemaining + 40);
        phase = Phase.WAIT_TRANSFER;
    }

    private static void transferHeldItem(Minecraft client) {
        AbstractContainerMenu menu = client.player.containerMenu;
        if (menu == client.player.inventoryMenu) {
            if (transferWaitTimeout-- <= 0) reset();
            else ticksRemaining = 1;
            return;
        }

        LocalPlayer player = client.player;
        MultiPlayerGameMode gameMode = client.gameMode;
        if (gameMode == null || player.getMainHandItem().isEmpty()) {
            reset();
            return;
        }

        int menuSlot = findSelectedHotbarSlot(menu, player);
        if (menuSlot >= 0) {
            gameMode.handleInventoryMouseClick(menu.containerId, menuSlot, 0, ClickType.QUICK_MOVE, player);
        }

        ticksRemaining = Math.max(0, Config.autoChestDepositCloseDelay);
        phase = Phase.WAIT_CLOSE;
    }

    private static int findSelectedHotbarSlot(AbstractContainerMenu menu, LocalPlayer player) {
        Inventory inventory = player.getInventory();
        int selectedSlot = inventory.getSelectedSlot();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (slot.container == inventory && slot.getContainerSlot() == selectedSlot) {
                return i;
            }
        }
        return -1;
    }

    private static void closeChest(LocalPlayer player) {
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
        reset();
    }

    private static void reset() {
        phase = Phase.IDLE;
        targetHit = null;
        ticksRemaining = 0;
        transferWaitTimeout = 0;
    }
}
