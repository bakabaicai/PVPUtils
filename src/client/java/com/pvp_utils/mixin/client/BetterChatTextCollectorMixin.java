package com.pvp_utils.mixin.client;

import com.pvp_utils.client.modules.impl.Render.BetterChat.BetterChatRenderState;
import net.minecraft.client.gui.ActiveTextCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = {
        "net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess",
        "net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess"
})
public abstract class BetterChatTextCollectorMixin {
    @ModifyArg(
            method = "handleMessage",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ActiveTextCollector;accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/client/gui/ActiveTextCollector$Parameters;Lnet/minecraft/util/FormattedCharSequence;)V"),
            index = 1
    )
    private int pvp_utils$shiftTextX(int x) {
        return BetterChatRenderState.isShiftingAvatarLine() ? x + 10 : x;
    }
}
