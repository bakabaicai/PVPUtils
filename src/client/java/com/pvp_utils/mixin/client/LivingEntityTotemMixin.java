package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityTotemMixin {
    @Inject(method = "handleEntityEvent", at = @At("HEAD"), cancellable = true)
    private void hideTotemAnimation(byte flag, CallbackInfo ci) {
        if (Config.hideTotemAnimation && flag == 35) {
            ci.cancel();
        }
    }
}
