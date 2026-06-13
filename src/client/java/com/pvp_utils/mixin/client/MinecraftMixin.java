package com.pvp_utils.mixin.client;

import com.pvp_utils.client.modules.impl.Tool.AutoChestDepositManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void hideAutoChestDepositScreen(Screen screen, CallbackInfo ci) {
        if (AutoChestDepositManager.shouldHideContainerScreen(screen)) {
            ci.cancel();
        }
    }
}
