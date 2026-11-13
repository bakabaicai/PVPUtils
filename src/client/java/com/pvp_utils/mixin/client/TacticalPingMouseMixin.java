package com.pvp_utils.mixin.client;

import com.pvp_utils.client.modules.impl.Tool.tactical.TacticalPingManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class TacticalPingMouseMixin {
    @Inject(method = "onButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;click(Lcom/mojang/blaze3d/platform/InputConstants$Key;)V", shift = At.Shift.AFTER))
    private void pvp_utils$tacticalPing(long window, MouseButtonInfo button, int action, CallbackInfo ci) {
        if (button.button() != 2 || action != 1) return;
        Minecraft client = Minecraft.getInstance();
        if (!TacticalPingManager.onMiddleClick(client)) return;
        if (client.options.keyPickItem.matchesMouse(new MouseButtonEvent(0.0, 0.0, button))) {
            while (client.options.keyPickItem.consumeClick()) {
            }
            client.options.keyPickItem.setDown(false);
        }
    }
}
