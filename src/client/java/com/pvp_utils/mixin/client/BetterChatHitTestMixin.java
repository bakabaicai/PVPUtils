package com.pvp_utils.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.pvp_utils.client.modules.impl.Render.BetterChat.BetterChatRenderState;
import com.pvp_utils.client.modules.impl.Render.BetterChat.BetterChatTextRenderer;
import net.minecraft.client.gui.render.state.GuiTextRenderState;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

@Mixin(targets = {
        "net.minecraft.client.gui.GuiGraphics$RenderingTextCollector",
        "net.minecraft.client.gui.ActiveTextCollector$ClickableStyleFinder"
})
public abstract class BetterChatHitTestMixin {
    @WrapOperation(method = "accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/client/gui/ActiveTextCollector$Parameters;Lnet/minecraft/util/FormattedCharSequence;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ActiveTextCollector;findElementUnderCursor(Lnet/minecraft/client/gui/render/state/GuiTextRenderState;FFLjava/util/function/Consumer;)V"))
    private void pvp_utils$findClientFontStyle(GuiTextRenderState state, float mouseX, float mouseY,
                                               Consumer<Style> consumer, Operation<Void> original) {
        if (!BetterChatRenderState.isUsingClientFontMetrics()) {
            original.call(state, mouseX, mouseY, consumer);
            return;
        }
        Style style = BetterChatTextRenderer.styleAt(state, mouseX, mouseY);
        if (style != null) consumer.accept(style);
    }
}
