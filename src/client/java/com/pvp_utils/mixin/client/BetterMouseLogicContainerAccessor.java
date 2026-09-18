package com.pvp_utils.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerScreen.class)
public interface BetterMouseLogicContainerAccessor {
    @Invoker("getHoveredSlot")
    Slot pvp_utils$getHoveredSlot(double x, double y);

    @Invoker("slotClicked")
    void pvp_utils$slotClicked(Slot slot, int slotId, int mouseButton, ClickType clickType);

    @Accessor("isQuickCrafting")
    boolean pvp_utils$isQuickCrafting();

    @Accessor("isQuickCrafting")
    void pvp_utils$setQuickCrafting(boolean value);

    @Accessor("quickCraftingButton")
    int pvp_utils$getQuickCraftingButton();

    @Accessor("skipNextRelease")
    void pvp_utils$setSkipNextRelease(boolean value);
}
