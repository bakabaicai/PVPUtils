package com.pvp_utils.mixin.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin extends Screen {
    protected ConnectScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void pvp_utils$fixCancelButtonOverlap(CallbackInfo ci) {
        int x = this.width / 2 - 100;
        int y = Math.min(this.height - 32, this.height / 2 - 18);
        for (GuiEventListener child : this.children()) {
            if (child instanceof Button button && CommonComponents.GUI_CANCEL.equals(button.getMessage())) {
                button.setX(x);
                button.setY(y);
            }
        }
    }
}
