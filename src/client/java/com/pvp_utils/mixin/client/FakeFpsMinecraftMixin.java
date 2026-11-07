package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Misc.FakeFpsManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class FakeFpsMinecraftMixin {
    @Inject(method = "runTick", at = @At("HEAD"))
    private void pvp_utils$updateFakeFps(boolean advanceGameTime, CallbackInfo ci) {
        FakeFpsManager.beginFrame();
    }

    @Inject(method = "getFps", at = @At("RETURN"), cancellable = true)
    private void pvp_utils$overrideFps(CallbackInfoReturnable<Integer> cir) {
        if (Config.fakeFps) cir.setReturnValue(FakeFpsManager.getFps(cir.getReturnValue()));
    }
}
