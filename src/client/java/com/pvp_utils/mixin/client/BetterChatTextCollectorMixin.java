package com.pvp_utils.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Render.BetterChat.BetterChatNewStyle;
import com.pvp_utils.client.modules.impl.Render.BetterChat.BetterChatRenderState;
import com.pvp_utils.client.modules.impl.Render.BetterChat.BetterChatTextRenderer;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {
        "net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess",
        "net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess"
})
public abstract class BetterChatTextCollectorMixin {
    @Inject(method = "fill", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$hideLineBackground(int left, int top, int right, int bottom, int color, CallbackInfo ci) {
        if (BetterChatRenderState.isSuppressingBackground()) ci.cancel();
    }

    @ModifyArg(
            method = "handleMessage",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ActiveTextCollector;accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/client/gui/ActiveTextCollector$Parameters;Lnet/minecraft/util/FormattedCharSequence;)V"),
            index = 1
    )
    private int pvp_utils$shiftTextX(int x) {
        return BetterChatRenderState.isShiftingAvatarLine() ? x + 10 : x;
    }

    @WrapOperation(
            method = "handleMessage",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ActiveTextCollector;accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/client/gui/ActiveTextCollector$Parameters;Lnet/minecraft/util/FormattedCharSequence;)V")
    )
    private void pvp_utils$clientFont(ActiveTextCollector collector, TextAlignment alignment, int x, int y,
                                     ActiveTextCollector.Parameters parameters, FormattedCharSequence text,
                                     Operation<Void> original) {
        if (!BetterChatNewStyle.enabled()) {
            original.call(collector, alignment, x, y, parameters, text);
            return;
        }
        ActiveTextCollector.Parameters clipped = BetterChatNewStyle.clipText(parameters);
        if (!Config.betterChatBetterFont) {
            original.call(collector, alignment, x, y, clipped, text);
            return;
        }
        BetterChatTextRenderer.draw(x, y, clipped, text);
        original.call(collector, alignment, x, y, clipped.withOpacity(0f), text);
    }
}
