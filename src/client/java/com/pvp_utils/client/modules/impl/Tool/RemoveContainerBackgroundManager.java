package com.pvp_utils.client.modules.impl.Tool;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;

public final class RemoveContainerBackgroundManager {
    private RemoveContainerBackgroundManager() {
    }

    public static boolean shouldRemove(Screen screen) {
        if (!Config.removeContainerBackground || screen == null || Minecraft.getInstance().player == null) {
            return false;
        }
        if (screen.isPauseScreen() || isExcluded(screen)) {
            return false;
        }
        return screen instanceof InventoryScreen
                || screen instanceof HorseInventoryScreen
                || screen instanceof CreativeModeInventoryScreen
                || screen instanceof CraftingScreen
                || screen instanceof ContainerScreen
                || screen instanceof ShulkerBoxScreen
                || screen instanceof DispenserScreen
                || screen instanceof HopperScreen
                || screen instanceof EnchantmentScreen
                || screen instanceof AnvilScreen
                || screen instanceof BeaconScreen
                || screen instanceof BrewingStandScreen
                || screen instanceof FurnaceScreen
                || screen instanceof BlastFurnaceScreen
                || screen instanceof SmokerScreen
                || screen instanceof LoomScreen
                || screen instanceof CartographyTableScreen
                || screen instanceof GrindstoneScreen
                || screen instanceof StonecutterScreen
                || screen instanceof SmithingScreen
                || screen instanceof MerchantScreen;
    }

    private static boolean isExcluded(Screen screen) {
        return screen instanceof DeathScreen
                || screen instanceof StatsScreen
                || screen instanceof WinScreen
                || screen instanceof ProgressScreen
                || screen instanceof GenericMessageScreen
                || screen instanceof ChatScreen;
    }
}
