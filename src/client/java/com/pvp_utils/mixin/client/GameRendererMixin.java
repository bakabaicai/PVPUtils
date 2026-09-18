package com.pvp_utils.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pvp_utils.Config;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void hideHurtShake(PoseStack poseStack, float tickProgress, CallbackInfo ci) {
        if (Config.hideHurtShake) ci.cancel();
    }

    @WrapOperation(method = "bobHurt", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;hurtTime:I", opcode = Opcodes.GETFIELD))
    private int pvp_utils$legacy17HurtTiltTime(LivingEntity entity, Operation<Integer> original) {
        int hurtTime = original.call(entity);
        return Config.legacy17Animations && Config.legacy17HurtTilt ? Math.max(hurtTime - 1, 0) : hurtTime;
    }
}
