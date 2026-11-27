package com.pvp_utils.mixin.client;

import com.pvp_utils.client.modules.impl.Tool.Freelook.FreelookManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class FreelookEntityMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$turnFreelookCamera(double yawDelta, double pitchDelta, CallbackInfo ci) {
        if ((Object) this != Minecraft.getInstance().player || !FreelookManager.isActive()) {
            return;
        }

        FreelookManager.turn(yawDelta, pitchDelta);
        ci.cancel();
    }
}
