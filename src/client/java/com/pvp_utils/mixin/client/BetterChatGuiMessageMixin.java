package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiMessage.Line.class)
public class BetterChatGuiMessageMixin {
    @Inject(method = "tag", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$removeIndicator(CallbackInfoReturnable<GuiMessageTag> cir) {
        if (Config.betterChat) {
            cir.setReturnValue(null);
        }
    }
}
