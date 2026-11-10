package com.pvp_utils.mixin.compatibility;

import com.pvp_utils.client.compatibility.CompatibilityWarningController;
import com.pvp_utils.compatibility.CompatibilityGate;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public final class CompatibilityMinecraftMixin {
    @Inject(method = "runTick", at = @At("HEAD"), require = 0)
    private void pvp_utils$compatibilityWarning(boolean renderLevel, CallbackInfo ci) {
        if (CompatibilityGate.hasPendingWarning()) {
            CompatibilityWarningController.tick((Minecraft) (Object) this);
        }
    }
}
