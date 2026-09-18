package com.pvp_utils.mixin.client;

import com.pvp_utils.client.modules.impl.Tool.RemoveContainerBackgroundManager;
import com.pvp_utils.client.modules.impl.Tool.ServerConnectionOverlay;
import com.pvp_utils.client.render.MainUI.MainUISharedBackground;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$mainUiBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (MainUISharedBackground.shouldReplace((Screen) (Object) this)) {
            MainUISharedBackground.render(guiGraphics, mouseX, mouseY);
            ci.cancel();
        }
    }

    @Inject(method = "renderMenuBackground", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$mainUiMenuBackground(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (MainUISharedBackground.shouldReplace((Screen) (Object) this)) {
            MainUISharedBackground.render(guiGraphics, 0, 0);
            ci.cancel();
        }
    }

    @Inject(method = "renderTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$removeContainerTransparentBackground(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (RemoveContainerBackgroundManager.shouldRemove((Screen) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void pvp_utils$renderDisconnectedConnectionLog(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (screen instanceof DisconnectedScreen) {
            ServerConnectionOverlay.renderFailure(guiGraphics, screen.width, screen.height);
        }
    }
}
