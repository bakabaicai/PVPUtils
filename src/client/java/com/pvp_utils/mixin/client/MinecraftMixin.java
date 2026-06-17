package com.pvp_utils.mixin.client;

import com.pvp_utils.client.modules.impl.Tool.AutoChestDepositManager;
import com.pvp_utils.client.modules.impl.Tool.FakePlayerManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void hideAutoChestDepositScreen(Screen screen, CallbackInfo ci) {
        if (AutoChestDepositManager.shouldHideContainerScreen(screen)) {
            ci.cancel();
        }
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void attackFakePlayer(CallbackInfoReturnable<Boolean> cir) {
        if (FakePlayerManager.tryAttack((Minecraft) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
