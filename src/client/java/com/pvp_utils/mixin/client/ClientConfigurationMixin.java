package com.pvp_utils.mixin.client;

import net.fabricmc.fabric.impl.networking.AbstractChanneledNetworkAddon;
import net.fabricmc.fabric.impl.networking.CommonRegisterPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;

@Mixin(AbstractChanneledNetworkAddon.class)
public class ClientConfigurationMixin {
    @Inject(method = "sendInitialChannelRegistrationPacket", at = @At("HEAD"), cancellable = true)
    private void onSendInitialChannelRegistrationPacket(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "createRegisterPayload", at = @At("RETURN"), cancellable = true)
    private void onCreateRegisterPayload(CallbackInfoReturnable<CommonRegisterPayload> ci) {
        CommonRegisterPayload original = ci.getReturnValue();
        if (original == null) {
            return;
        }
        ci.setReturnValue(new CommonRegisterPayload(original.version(), original.phase(), Collections.emptySet()));
    }
}
