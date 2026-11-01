package com.pvp_utils.mixin.client;

import com.pvp_utils.client.modules.impl.Tool.PseudoCreativeManager;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @Inject(method = "handleCreativeModeItemAdd", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$blockFakeCreativeAdd(ItemStack stack, int slot, CallbackInfo ci) {
        if (PseudoCreativeManager.blocksCreativePacket()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleCreativeModeItemDrop", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$blockFakeCreativeDrop(ItemStack stack, CallbackInfo ci) {
        if (PseudoCreativeManager.blocksCreativePacket()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleInventoryMouseClick", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$blockFakeInventoryClick(int containerId, int slot, int button, ClickType clickType, Player player, CallbackInfo ci) {
        if (PseudoCreativeManager.blocksCreativePacket()) {
            ci.cancel();
        }
    }
}
