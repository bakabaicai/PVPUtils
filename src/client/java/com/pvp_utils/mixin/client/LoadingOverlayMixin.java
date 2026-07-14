package com.pvp_utils.mixin.client;

import com.pvp_utils.client.render.MainUI.MainUISharedBackground;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingOverlay.class)
public class LoadingOverlayMixin {
    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V",
                    ordinal = 0
            )
    )
    private void pvp_utils$renderMainUiGlslBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (MainUISharedBackground.shouldReplaceLoadingOverlay()) {
            MainUISharedBackground.render(guiGraphics, mouseX, mouseY);
        }
    }
}
