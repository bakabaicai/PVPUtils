package com.pvp_utils.mixin.client;

import com.mojang.authlib.GameProfile;
import com.pvp_utils.client.communication.EncryptedChatManager;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.PlayerChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.time.Instant;

@Mixin(ChatListener.class)
public abstract class EncryptedChatListenerMixin {
    @Redirect(method = "showMessageToPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/ChatComponent;addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V"))
    private void pvp_utils$showDecryptedPlayerChat(ChatComponent chat, Component component, MessageSignature signature,
            GuiMessageTag tag, ChatType.Bound bound, PlayerChatMessage message, Component decorated,
            GameProfile sender, boolean secureOnly, Instant receivedAt) {
        if (!message.filterMask().isEmpty()) {
            chat.addMessage(component, signature, tag);
            return;
        }
        EncryptedChatManager.DisplayResult display = EncryptedChatManager.receive(component, sender, message.signedContent(), bound);
        if (!display.hidden()) chat.addMessage(display.component(), signature, tag);
    }

    @ModifyArg(method = "narrateChatMessage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/GameNarrator;sayChatQueued(Lnet/minecraft/network/chat/Component;)V"), index = 0)
    private Component pvp_utils$narrateDecryptedChat(Component component) {
        Component result = EncryptedChatManager.narration(component);
        return result == null ? Component.empty() : result;
    }

    @ModifyArg(method = "handleSystemMessage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/GameNarrator;saySystemChatQueued(Lnet/minecraft/network/chat/Component;)V"), index = 0)
    private Component pvp_utils$narrateDecryptedSystemChat(Component component) {
        Component result = EncryptedChatManager.narration(component);
        return result == null ? Component.empty() : result;
    }
}
