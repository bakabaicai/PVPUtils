package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.pvp_utils.LowHealthHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow public int swingTime;
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
        if ((Object) this instanceof Player) {
            int original = cir.getReturnValue();
            cir.setReturnValue((int) (original / Config.animSpeed));
        }
    }

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"), cancellable = true)
    private void preventSwingReset(CallbackInfo ci) {
        if ((Object) this instanceof Player && this.swingTime > 0 && Config.animSpeed < 1.0f) {
            ci.cancel();
        }
    }

    @Inject(method = "isUsingItem", at = @At("HEAD"), cancellable = true)
    private void trickIsUsingItemForSwing(CallbackInfoReturnable<Boolean> cir) {
        if (Config.useSwing && (Object) this instanceof Player) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stackTrace) {
                String methodName = element.getMethodName();
                if (methodName.equals("swing") || methodName.equals("m_6674_") ||
                        methodName.contains("attack") || methodName.contains("handleAttack")) {
                    cir.setReturnValue(false);
                    return;
                }
            }
        }
    }
}