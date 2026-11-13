package com.pvp_utils.mixin.client;

import com.pvp_utils.client.communication.EncryptedChatManager;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class EncryptedChatOutgoingMixin {
    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$encryptChat(String message, CallbackInfo ci) {
        if (EncryptedChatManager.interceptChat((ClientPacketListener) (Object) this, message)) ci.cancel();
    }

    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$encryptPrivateChat(String command, CallbackInfo ci) {
        if (EncryptedChatManager.interceptCommand((ClientPacketListener) (Object) this, command)) ci.cancel();
    }

    @Inject(method = "sendChat", at = @At("TAIL"))
    private void pvp_utils$recordChatSend(String message, CallbackInfo ci) {
        EncryptedChatManager.noteChatSent();
    }

    @Inject(method = "sendCommand", at = @At("TAIL"))
    private void pvp_utils$recordPrivateChatSend(String command, CallbackInfo ci) {
        EncryptedChatManager.noteCommandSent(command);
    }
}
