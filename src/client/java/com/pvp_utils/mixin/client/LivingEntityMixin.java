package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Render.LowHealthHandler;
import com.pvp_utils.client.modules.impl.Tool.HeldItemPositionManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.entity.player.Player;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow public int swingTime;
    @Shadow public InteractionHand swingingArm;
    @Shadow public abstract float getHealth();

    @Inject(method = "tick", at = @At("HEAD"))
    private void checkLowHealth(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if ((Object) this == client.player) {
            LowHealthHandler.onHealthUpdate(client, this.getHealth());
        }
    }

    @Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true)
    private void modifySwingDuration(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof Player player) {
            int original = cir.getReturnValue();
            cir.setReturnValue(HeldItemPositionManager.swingDuration(original, player, this.swingingArm));
        }
    }

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"), cancellable = true)
    private void preventSwingReset(CallbackInfo ci) {
        if ((Object) this instanceof Player player && this.swingTime > 0 && HeldItemPositionManager.swingDuration(1, player, this.swingingArm) > 1) {
            ci.cancel();
        }
    }

    @WrapOperation(method = "lerpHeadRotationStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;rotLerp(DDD)D"))
    private double pvp_utils$legacy17HeadRotation(double delta, double start, double end, Operation<Double> original) {
        return Config.legacy17Animations && Config.legacy17HeadRotation ? end : original.call(delta, start, end);
    }
}
