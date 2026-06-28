package com.pvp_utils.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Render.BetterChat.BetterChatState;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class BetterChatChatMixin {
    private static final int CHAT_HEAD_SIZE = 8;
    private static final int CHAT_HEAD_GAP = 2;
    private static final int CHAT_HEAD_SHIFT = CHAT_HEAD_SIZE + CHAT_HEAD_GAP;
    @Shadow private int chatScrollbarPos;
    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;
    @Shadow private int getLineHeight() { return 0; }

    @ModifyReturnValue(method = "getWidth", at = @At("RETURN"))
    private int pvp_utils$extendWidthForAvatars(int original) {
        return Config.betterChat && Config.betterChatAvatar ? original + CHAT_HEAD_SHIFT : original;
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void pvp_utils$chatRenderStart(GuiGraphics context, Font font, int currentTick, int mouseX, int mouseY, boolean focused, boolean open, CallbackInfo ci) {
        if (!Config.betterChat || !Config.betterChatMessageAnimation) return;
        int offset = BetterChatState.getInstance().calculateChatDisplacementY(this.getLineHeight(), this.chatScrollbarPos);
        context.pose().translate(0, offset);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void pvp_utils$chatRenderEnd(GuiGraphics context, Font font, int currentTick, int mouseX, int mouseY, boolean focused, boolean open, CallbackInfo ci) {
        if (!Config.betterChat || !Config.betterChatMessageAnimation) return;
        int offset = BetterChatState.getInstance().calculateChatDisplacementY(this.getLineHeight(), this.chatScrollbarPos);
        context.pose().translate(0, -offset);
    }

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V", at = @At("TAIL"))
    private void pvp_utils$onAddMessage(Component message, MessageSignature signatureData, GuiMessageTag indicator, CallbackInfo ci) {
        if (!Config.betterChat || !Config.betterChatMessageAnimation) return;
        BetterChatState.getInstance().recordMessage();
        BetterChatState.getInstance().trimMessageCount(this.trimmedMessages.size());
    }
}
