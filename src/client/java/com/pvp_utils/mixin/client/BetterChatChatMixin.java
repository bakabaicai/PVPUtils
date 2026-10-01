package com.pvp_utils.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Render.BetterChat.BetterChatState;
import com.pvp_utils.client.modules.impl.Tool.NickHiderManager;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(ChatComponent.class)
public abstract class BetterChatChatMixin {
    private static final int CHAT_HEAD_SIZE = 8;
    private static final int CHAT_HEAD_GAP = 2;
    private static final int CHAT_HEAD_SHIFT = CHAT_HEAD_SIZE + CHAT_HEAD_GAP;
    private static final AtomicBoolean REPLACING = new AtomicBoolean(false);
    @Unique private int pvp_utils$chatOffset;
    @Shadow private int chatScrollbarPos;
    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;
    @Shadow private int getLineHeight() { return 0; }

    @ModifyReturnValue(method = "getWidth", at = @At("RETURN"))
    private int pvp_utils$extendWidthForAvatars(int original) {
        return Config.betterChat && Config.betterChatAvatar ? original + CHAT_HEAD_SHIFT : original;
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void pvp_utils$chatRenderStart(GuiGraphics context, Font font, int currentTick, int mouseX, int mouseY, boolean focused, boolean open, CallbackInfo ci) {
        this.pvp_utils$chatOffset = (!Config.betterChat || !Config.betterChatMessageAnimation) ? 0
                : BetterChatState.getInstance().calculateChatDisplacementY(this.getLineHeight(), this.chatScrollbarPos);
        if (this.pvp_utils$chatOffset != 0) {
            context.pose().translate(0, this.pvp_utils$chatOffset);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void pvp_utils$chatRenderEnd(GuiGraphics context, Font font, int currentTick, int mouseX, int mouseY, boolean focused, boolean open, CallbackInfo ci) {
        if (this.pvp_utils$chatOffset != 0) {
            context.pose().translate(0, -this.pvp_utils$chatOffset);
            this.pvp_utils$chatOffset = 0;
        }
    }

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$nickHiderChat(Component message, MessageSignature signatureData, GuiMessageTag indicator, CallbackInfo ci) {
        if (!REPLACING.compareAndSet(false, true)) {
            return;
        }
        try {
            Component replaced = NickHiderManager.replaceChat(message);
            if (replaced != message) {
                ((ChatComponent) (Object) this).addMessage(replaced, signatureData, indicator);
                ci.cancel();
            }
        } finally {
            REPLACING.set(false);
        }
    }

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V", at = @At("TAIL"))
    private void pvp_utils$onAddMessage(Component message, MessageSignature signatureData, GuiMessageTag indicator, CallbackInfo ci) {
        if (!Config.betterChat || !Config.betterChatMessageAnimation) return;
        BetterChatState.getInstance().recordMessage();
        BetterChatState.getInstance().trimMessageCount(this.trimmedMessages.size());
    }
}
