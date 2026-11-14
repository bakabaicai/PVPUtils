package com.pvp_utils.mixin.client;

import com.pvp_utils.client.ModuleKeybindManager;
import com.pvp_utils.client.modules.impl.Tool.tactical.TacticalPingManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class TacticalPingMouseMixin {
    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$tacticalPingButton(long window, MouseButtonInfo button, int action, CallbackInfo ci) {
        if (action != GLFW.GLFW_PRESS) return;
        if (!ModuleKeybindManager.isBoundToMouseButton(ModuleKeybindManager.ACTION_TACTICAL_PING, button.button())) return;
        Minecraft client = Minecraft.getInstance();
        if (!TacticalPingManager.ping(client)) return;
        ci.cancel();
    }
}
