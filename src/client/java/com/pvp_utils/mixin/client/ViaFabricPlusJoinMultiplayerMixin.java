package com.pvp_utils.mixin.client;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = JoinMultiplayerScreen.class, priority = 1100)
public abstract class ViaFabricPlusJoinMultiplayerMixin extends Screen {
    protected ViaFabricPlusJoinMultiplayerMixin(Component title) {
        super(title);
    }

    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void pvp_utils$removeViaButton(CallbackInfo ci) {
        for (GuiEventListener child : new java.util.ArrayList<>(children())) {
            if (child instanceof Button button && isViaButton(button.getMessage())) {
                removeWidget(button);
            }
        }
    }

    @Unique
    private static boolean isViaButton(Component message) {
        String text = message == null ? "" : message.getString();
        return text.equalsIgnoreCase("ViaFabricPlus");
    }
}
