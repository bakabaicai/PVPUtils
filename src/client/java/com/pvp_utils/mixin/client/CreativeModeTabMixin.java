package com.pvp_utils.mixin.client;

import com.pvp_utils.client.creative.OpItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Set;

@Mixin(CreativeModeTab.class)
public abstract class CreativeModeTabMixin {
    @Shadow
    private Collection<ItemStack> displayItems;

    @Shadow
    private Set<ItemStack> displayItemsSearchTab;

    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$opItemsTitle(CallbackInfoReturnable<Component> cir) {
        if (pvp_utils$isOpItemsTab()) {
            cir.setReturnValue(OpItems.title());
        }
    }

    @Inject(method = "getIconItem", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$opItemsIcon(CallbackInfoReturnable<ItemStack> cir) {
        if (pvp_utils$isOpItemsTab()) {
            cir.setReturnValue(OpItems.icon());
        }
    }

    @Inject(method = "buildContents", at = @At("TAIL"))
    private void pvp_utils$appendOpItems(CreativeModeTab.ItemDisplayParameters parameters, CallbackInfo ci) {
        if (!pvp_utils$isOpItemsTab()) {
            return;
        }
        for (ItemStack stack : OpItems.stacks(parameters.holders())) {
            displayItems.add(stack);
            displayItemsSearchTab.add(stack);
        }
    }

    private boolean pvp_utils$isOpItemsTab() {
        return BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey((CreativeModeTab) (Object) this)
                .filter(CreativeModeTabs.OP_BLOCKS::equals)
                .isPresent();
    }
}
