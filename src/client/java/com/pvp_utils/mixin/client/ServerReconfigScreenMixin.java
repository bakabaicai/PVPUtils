package com.pvp_utils.mixin.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.multiplayer.ServerReconfigScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerReconfigScreen.class)
public abstract class ServerReconfigScreenMixin {
    @Shadow private Button disconnectButton;
    @Shadow private int delayTicker;

    @Inject(method = "init", at = @At("TAIL"))
    private void pvp_utils$removeDisconnectDelay(CallbackInfo ci) {
        delayTicker = 600;
        if (disconnectButton != null) {
            disconnectButton.active = true;
        }
    }
}
