package com.pvp_utils.mixin.client;

import com.pvp_utils.client.modules.impl.Render.BetterChat.BetterChatRenderState;
import com.pvp_utils.client.modules.impl.Render.BetterChat.BetterChatTextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$ClickableTextOnlyGraphicsAccess")
public abstract class BetterChatClickableTextMixin {
    @ModifyArg(method = "handleMessage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/ActiveTextCollector;accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/util/FormattedCharSequence;)V"), index = 1)
    private int pvp_utils$shiftClickableText(int x) {
        return BetterChatTextRenderer.usesClientFont() && BetterChatRenderState.isShiftingAvatarLine() ? x + 10 : x;
    }
}
