package com.pvp_utils.mixin.client;

import com.pvp_utils.client.modules.impl.Tool.AutoChestDepositManager;
import com.pvp_utils.client.modules.impl.Tool.BlockCountDisplayRenderer;
import com.pvp_utils.client.modules.impl.Tool.FakePlayerManager;
import com.pvp_utils.client.modules.impl.Tool.TimeWeatherChanger;
import com.pvp_utils.client.modules.impl.Render.HudEditOverlay;
import com.pvp_utils.client.modules.impl.Render.KeystrokesRenderer;
import com.pvp_utils.client.modules.impl.Render.PotionStatusRenderer;
import com.pvp_utils.client.modules.impl.Render.SkiaBlurCardRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void pvp_utils$tickToolOverrides(CallbackInfo ci) {
        TimeWeatherChanger.tick((Minecraft) (Object) this);
    }

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

    @Inject(
            method = "runTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/Window;updateDisplay(Lcom/mojang/blaze3d/TracyFrameCapture;)V"
            )
    )
    private void pvp_utils$renderSkiaFrameEnd(boolean advanceGameTime, CallbackInfo ci) {
        PotionStatusRenderer.getInstance().renderFrameEnd();
        KeystrokesRenderer.getInstance().renderFrameEnd();
        SkiaBlurCardRenderer.getInstance().renderFrameEnd();
        BlockCountDisplayRenderer.getInstance().renderFrameEnd();
        HudEditOverlay.getInstance().renderFrameEnd();
    }
}
