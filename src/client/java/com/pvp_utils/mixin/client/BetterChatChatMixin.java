package com.pvp_utils.mixin.client;

import com.mojang.authlib.GameProfile;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Render.BetterChat.BetterChatState;
import com.pvp_utils.client.modules.impl.Render.BetterChat.BetterChatHeadsState;
import com.pvp_utils.client.modules.impl.Render.BetterChat.BetterChatLineProfileAccessor;
import com.pvp_utils.client.modules.impl.Render.BetterChat.BetterChatNewStyle;
import com.pvp_utils.client.modules.impl.Render.BetterChat.BetterChatRenderState;
import com.pvp_utils.client.modules.impl.Render.BetterChat.BetterChatTextRenderer;
import com.pvp_utils.client.modules.impl.Tool.NickHiderManager;
import com.pvp_utils.client.render.chroma.ChromaDraw;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(ChatComponent.class)
public abstract class BetterChatChatMixin {
    private static final int CHAT_HEAD_SIZE = 8;
    private static final int CHAT_HEAD_GAP = 2;
    private static final int CHAT_HEAD_SHIFT = CHAT_HEAD_SIZE + CHAT_HEAD_GAP;
    private static final AtomicBoolean REPLACING = new AtomicBoolean(false);
    @Unique private int pvp_utils$chatOffset;
    @Unique private boolean pvp_utils$panelClip;
    @Unique private boolean pvp_utils$clientFontLayout;
    @Unique private boolean pvp_utils$layoutAvatars;
    @Unique private long pvp_utils$layoutFontRevision = -1L;
    @Unique private final Map<GuiMessage, GameProfile> pvp_utils$messageOwners = new WeakHashMap<>();
    @Shadow private int chatScrollbarPos;
    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;
    @Shadow private int getLineHeight() { return 0; }

    @ModifyReturnValue(method = "getWidth", at = @At("RETURN"))
    private int pvp_utils$extendWidthForAvatars(int original) {
        return Config.betterChat && Config.betterChatAvatar ? original + CHAT_HEAD_SHIFT : original;
    }

    @ModifyExpressionValue(method = "forEachLine", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/ChatComponent$AlphaCalculator;calculate(Lnet/minecraft/client/GuiMessage$Line;)F"))
    private float pvp_utils$neverFadeLines(float original) {
        return BetterChatNewStyle.enabled() ? 1f : original;
    }

    @ModifyExpressionValue(method = "forEachLine", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/ChatComponent;getLinesPerPage()I"))
    private int pvp_utils$keepExitingLines(int original) {
        return BetterChatNewStyle.enabled()
                ? BetterChatNewStyle.renderLineLimit(original, this.trimmedMessages, this.chatScrollbarPos, this.getLineHeight())
                : original;
    }

    @ModifyReturnValue(method = "forEachLine", at = @At("RETURN"))
    private int pvp_utils$keepScrollbarPageSize(int original) {
        return BetterChatNewStyle.enabled()
                ? Math.min(original, ((ChatComponent) (Object) this).getLinesPerPage()) : original;
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;IIIZZ)V", at = @At("HEAD"))
    private void pvp_utils$chatRenderStart(GuiGraphics context, Font font, int currentTick, int mouseX, int mouseY, boolean focused, boolean open, CallbackInfo ci) {
        boolean clientFont = BetterChatTextRenderer.usesClientFont();
        long revision = clientFont ? ChromaDraw.textFontRevision() : -1L;
        if (clientFont != this.pvp_utils$clientFontLayout || clientFont
                && (revision != this.pvp_utils$layoutFontRevision || Config.betterChatAvatar != this.pvp_utils$layoutAvatars)) {
            this.pvp_utils$clientFontLayout = clientFont;
            this.pvp_utils$layoutFontRevision = revision;
            this.pvp_utils$layoutAvatars = Config.betterChatAvatar;
            ((ChatComponent) (Object) this).rescaleChat();
        }
        BetterChatNewStyle.beginFrame(this.chatScrollbarPos != 0);
        this.pvp_utils$panelClip = BetterChatNewStyle.drawPanel(context, (ChatComponent) (Object) this, this.trimmedMessages,
                this.chatScrollbarPos, currentTick, focused, this.getLineHeight());
        this.pvp_utils$chatOffset = (!Config.betterChat || !Config.betterChatMessageAnimation || BetterChatNewStyle.enabled()) ? 0
                : BetterChatState.getInstance().calculateChatDisplacementY(this.getLineHeight(), this.chatScrollbarPos);
        if (this.pvp_utils$chatOffset != 0) {
            context.pose().translate(0, this.pvp_utils$chatOffset);
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;IIIZZ)V", at = @At("TAIL"))
    private void pvp_utils$chatRenderEnd(GuiGraphics context, Font font, int currentTick, int mouseX, int mouseY, boolean focused, boolean open, CallbackInfo ci) {
        if (this.pvp_utils$panelClip) {
            context.disableScissor();
            this.pvp_utils$panelClip = false;
        }
        if (this.pvp_utils$chatOffset != 0) {
            context.pose().translate(0, -this.pvp_utils$chatOffset);
            this.pvp_utils$chatOffset = 0;
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IIZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;forEachLine(Lnet/minecraft/client/gui/components/ChatComponent$AlphaCalculator;Lnet/minecraft/client/gui/components/ChatComponent$LineConsumer;)I", ordinal = 0))
    private void pvp_utils$beginPanelBackground(ChatComponent.ChatGraphicsAccess graphics, int height, int tick, boolean focused, CallbackInfo ci) {
        BetterChatRenderState.setSuppressingBackground(BetterChatNewStyle.enabled());
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IIZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;forEachLine(Lnet/minecraft/client/gui/components/ChatComponent$AlphaCalculator;Lnet/minecraft/client/gui/components/ChatComponent$LineConsumer;)I", ordinal = 0, shift = At.Shift.AFTER))
    private void pvp_utils$endPanelBackground(ChatComponent.ChatGraphicsAccess graphics, int height, int tick, boolean focused, CallbackInfo ci) {
        BetterChatRenderState.setSuppressingBackground(false);
    }

    @WrapMethod(method = "render(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IIZ)V")
    private void pvp_utils$useClientFontMetrics(ChatComponent.ChatGraphicsAccess graphics, int height, int tick,
                                               boolean focused, Operation<Void> original) {
        boolean previous = BetterChatRenderState.isUsingClientFontMetrics();
        BetterChatRenderState.setUsingClientFontMetrics(BetterChatTextRenderer.usesClientFont());
        try {
            original.call(graphics, height, tick, focused);
        } finally {
            BetterChatRenderState.setUsingClientFontMetrics(previous);
        }
    }

    @WrapOperation(method = "addMessageToDisplayQueue", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/GuiMessage;splitLines(Lnet/minecraft/client/gui/Font;I)Ljava/util/List;"))
    private List<FormattedCharSequence> pvp_utils$wrapClientFont(GuiMessage message, Font font, int width,
                                                               Operation<List<FormattedCharSequence>> original) {
        if (!BetterChatTextRenderer.usesClientFont()) return original.call(message, font, width);
        int available = width - (Config.betterChatAvatar ? CHAT_HEAD_SHIFT : 0);
        if (message.tag() != null && message.tag().icon() != null) {
            available -= message.tag().icon().width + 6;
        }
        return BetterChatTextRenderer.splitLines(message.content(), available);
    }

    @Inject(method = "addMessageToDisplayQueue", at = @At("HEAD"))
    private void pvp_utils$beforeDisplayMessage(GuiMessage message, CallbackInfo ci) {
        GameProfile owner = this.pvp_utils$messageOwners.get(message);
        if (owner != null) BetterChatHeadsState.getInstance().setPendingSender(owner);
        if (BetterChatNewStyle.enabled()) BetterChatNewStyle.track(this.trimmedMessages);
    }

    @Inject(method = "addMessageToDisplayQueue", at = @At("TAIL"))
    private void pvp_utils$afterDisplayMessage(GuiMessage message, CallbackInfo ci) {
        if (!this.trimmedMessages.isEmpty()) {
            GameProfile owner = ((BetterChatLineProfileAccessor) (Object) this.trimmedMessages.getFirst()).pvp_utils$getOwnerProfile();
            if (owner != null) this.pvp_utils$messageOwners.put(message, owner);
        }
        BetterChatNewStyle.added(this.trimmedMessages, this.chatScrollbarPos != 0);
    }

    @Inject(method = "clearMessages", at = @At("TAIL"))
    private void pvp_utils$clearMessageOwners(boolean clearRecentChat, CallbackInfo ci) {
        this.pvp_utils$messageOwners.clear();
    }

    @Inject(method = {"clearMessages", "refreshTrimmedMessages"}, at = @At("TAIL"))
    private void pvp_utils$resetNewAnimations(CallbackInfo ci) {
        BetterChatNewStyle.reset();
    }

    @Inject(method = "scrollChat", at = @At("HEAD"))
    private void pvp_utils$stopAnimationOnScroll(int amount, CallbackInfo ci) {
        if (BetterChatNewStyle.enabled() && amount != 0) BetterChatNewStyle.reset();
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
