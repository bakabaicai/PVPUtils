package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @Inject(method = "shouldShowName", at = @At("RETURN"), cancellable = true)
    private void pvp_utils$showOwnNameTag(LivingEntity entity, double distance, CallbackInfoReturnable<Boolean> cir) {
        Minecraft client = Minecraft.getInstance();
        if (Config.nameTagShowSelf
                && client.player != null
                && client.options.getCameraType() != CameraType.FIRST_PERSON
                && entity == client.player) {
            cir.setReturnValue(true);
        }
    }
}
