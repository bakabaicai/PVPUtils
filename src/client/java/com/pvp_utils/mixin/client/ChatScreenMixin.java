package com.pvp_utils.mixin.client;

import com.pvp_utils.client.gui.HudEditOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        HudEditOverlay.getInstance().render(guiGraphics);
    }
}