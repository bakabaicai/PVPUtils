package com.pvp_utils.mixin.client;

import com.pvp_utils.client.modules.impl.Tool.ServerConnectionOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin extends Screen {
    @Shadow private Component status;

    protected ConnectScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void pvp_utils$fixCancelButtonOverlap(CallbackInfo ci) {
        ServerConnectionOverlay.begin();
        int x = this.width / 2 - 100;
        int y = Math.min(this.height - 32, this.height / 2 + 36);
        for (GuiEventListener child : this.children()) {
            if (child instanceof Button button && CommonComponents.GUI_CANCEL.equals(button.getMessage())) {
                button.setX(x);
                button.setY(y);
            }
        }
    }

    @Inject(method = "updateStatus", at = @At("TAIL"))
    private void pvp_utils$logConnectionStatus(Component status, CallbackInfo ci) {
        ServerConnectionOverlay.logStatus(status);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void pvp_utils$renderConnectionOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ServerConnectionOverlay.render(graphics, this.width, this.height, this.status);
    }
}
