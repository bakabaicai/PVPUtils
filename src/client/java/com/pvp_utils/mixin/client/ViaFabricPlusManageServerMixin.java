package com.pvp_utils.mixin.client;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ManageServerScreen.class, priority = 1100)
public abstract class ViaFabricPlusManageServerMixin extends Screen {
    protected ViaFabricPlusManageServerMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void pvp_utils$removeViaButton(CallbackInfo ci) {
        for (GuiEventListener child : new java.util.ArrayList<>(children())) {
            if (child instanceof Button button && isViaButton(button.getMessage())) {
                removeWidget(button);
            }
        }
    }

    private static boolean isViaButton(Component message) {
        String text = message == null ? "" : message.getString();
        return text.equalsIgnoreCase("ViaFabricPlus") || text.equalsIgnoreCase("Set version");
    }
}
