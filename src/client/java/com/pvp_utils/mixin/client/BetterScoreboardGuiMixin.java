package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Optimize.BetterScoreboardManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class BetterScoreboardGuiMixin {
    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$betterScoreboardPush(GuiGraphics graphics, Objective objective, CallbackInfo ci) {
        if (Config.betterScoreboard && Config.betterScoreboardVisualImprovement) {
            ci.cancel();
            return;
        }
        BetterScoreboardManager.pushTransform(graphics, objective);
    }

    @Inject(method = "displayScoreboardSidebar", at = @At("RETURN"))
    private void pvp_utils$betterScoreboardPop(GuiGraphics graphics, Objective objective, CallbackInfo ci) {
        BetterScoreboardManager.popTransform(graphics);
    }

    @Redirect(
            method = "displayScoreboardSidebar",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V", ordinal = 2)
    )
    private void pvp_utils$hideBetterScoreboardScores(GuiGraphics graphics, Font font, Component component, int x, int y, int color, boolean shadow) {
        if (Config.betterScoreboard && Config.betterScoreboardHideScores) {
            return;
        }
        graphics.drawString(font, component, x, y, color, shadow);
    }
}
