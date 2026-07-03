package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class, priority = 500)
public class GameRendererTotemMixin {
    @Inject(method = "displayItemActivation", at = @At("HEAD"), cancellable = true)
    private void hideTotemAnimation(ItemStack itemStack, CallbackInfo ci) {
        if (Config.hideTotemAnimation && itemStack.is(Items.TOTEM_OF_UNDYING)) {
            ci.cancel();
        }
    }
}
