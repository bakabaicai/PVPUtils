package com.pvp_utils.mixin.client;

import com.pvp_utils.client.modules.impl.Tool.ServerConnectionOverlay;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin {
    @Inject(method = "<init>(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;)V", at = @At("TAIL"))
    private void pvp_utils$logSimpleDisconnect(Screen parent, Component title, Component reason, CallbackInfo ci) {
        ServerConnectionOverlay.logFailure(title, reason);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;)V", at = @At("TAIL"))
    private void pvp_utils$logButtonDisconnect(Screen parent, Component title, Component reason, Component buttonText, CallbackInfo ci) {
        ServerConnectionOverlay.logFailure(title, reason);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/DisconnectionDetails;)V", at = @At("TAIL"))
    private void pvp_utils$logDetailedDisconnect(Screen parent, Component title, DisconnectionDetails details, CallbackInfo ci) {
        ServerConnectionOverlay.logFailure(title, details == null ? null : details.reason());
    }

    @Inject(method = "<init>(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/DisconnectionDetails;Lnet/minecraft/network/chat/Component;)V", at = @At("TAIL"))
    private void pvp_utils$logDetailedButtonDisconnect(Screen parent, Component title, DisconnectionDetails details, Component buttonText, CallbackInfo ci) {
        ServerConnectionOverlay.logFailure(title, details == null ? null : details.reason());
    }
}
