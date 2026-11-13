package com.pvp_utils.mixin.client;

import com.pvp_utils.client.communication.EncryptedChatManager;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChatComponent.class, priority = 1100)
public abstract class EncryptedChatDisplayMixin {
    @Unique private boolean pvp_utils$replacingEncryptedMessage;

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"), cancellable = true)
    private void pvp_utils$showDecryptedSystemChat(Component component, MessageSignature signature,
            GuiMessageTag tag, CallbackInfo ci) {
        if (signature != null || this.pvp_utils$replacingEncryptedMessage) return;
        EncryptedChatManager.DisplayResult display = EncryptedChatManager.receive(component, null, null);
        if (display.hidden()) {
            ci.cancel();
        } else if (display.component() != component) {
            this.pvp_utils$replacingEncryptedMessage = true;
            try {
                ((ChatComponent) (Object) this).addMessage(display.component(), signature, tag);
            } finally {
                this.pvp_utils$replacingEncryptedMessage = false;
            }
            ci.cancel();
        }
    }
}
