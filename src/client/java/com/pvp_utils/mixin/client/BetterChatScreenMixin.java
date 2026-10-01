package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Render.BetterChat.BetterChatState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class BetterChatScreenMixin {
    @Unique private final Minecraft client = Minecraft.getInstance();
    @Unique private float pvp_utils$screenOffset;

    @Inject(method = "render", at = @At("HEAD"))
    private void pvp_utils$renderStart(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!Config.betterChat || !Config.betterChatInputAnimation) {
            this.pvp_utils$screenOffset = 0f;
            return;
        }
        BetterChatState.getInstance().beginChatScreenIfNeeded(client);
        this.pvp_utils$screenOffset = BetterChatState.getInstance().calculateChatScreenOffsetY(client);
        if (this.pvp_utils$screenOffset != 0f) {
            context.pose().translate(0, this.pvp_utils$screenOffset);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void pvp_utils$renderEnd(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.pvp_utils$screenOffset != 0f) {
            context.pose().translate(0, -this.pvp_utils$screenOffset);
            this.pvp_utils$screenOffset = 0f;
        }
        if (!Config.betterChat || !Config.betterChatInputAnimation) return;
        if (BetterChatState.getInstance().shouldCloseChatScreen() && client != null) {
            client.setScreen(null);
        }
    }

    @Inject(method = "onClose", at = @At("HEAD"))
    private void pvp_utils$onClose(CallbackInfo ci) {
        if (!Config.betterChat || !Config.betterChatInputAnimation) return;
        if (BetterChatState.getInstance().hasActiveChatMessages(client)) {
            return;
        }
        BetterChatState.getInstance().startClosing();
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void pvp_utils$removed(CallbackInfo ci) {
        BetterChatState.getInstance().resetChatScreenState();
    }
}
