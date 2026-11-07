package com.pvp_utils.mixin.client;

import com.pvp_utils.client.render.skia.SkijaOverlay;
import com.pvp_utils.client.startup.StartupProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftStartupMixin {
    @Inject(method = "<init>", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;initRenderer(JIZLcom/mojang/blaze3d/shaders/ShaderSource;Z)V"))
    private void pvp_utils$prepareGraphics(CallbackInfo ci) {
        StartupProgress.advance(StartupProgress.Stage.GRAPHICS);
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Options;applyGraphicsPreset(Lnet/minecraft/client/GraphicsPreset;)V",
            shift = At.Shift.AFTER))
    private void pvp_utils$prepareSystems(CallbackInfo ci) {
        StartupProgress.advance(StartupProgress.Stage.SYSTEMS);
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/packs/repository/PackRepository;reload()V"))
    private void pvp_utils$scanPacks(CallbackInfo ci) {
        StartupProgress.advance(StartupProgress.Stage.PACKS);
    }

    @Inject(method = "onGameLoadFinished", at = @At("TAIL"))
    private void pvp_utils$startupReady(CallbackInfo ci) {
        StartupProgress.advance(StartupProgress.Stage.READY);
        Overlay overlay = ((Minecraft) (Object) this).getOverlay();
        if (!(overlay instanceof SkijaOverlay skijaOverlay) || !skijaOverlay.isSkijaOverlayActive()) {
            StartupProgress.complete();
        }
    }

    @Inject(method = "setOverlay", at = @At("TAIL"))
    private void pvp_utils$finishDismissedStartup(Overlay overlay, CallbackInfo ci) {
        if (overlay == null && StartupProgress.stage().ordinal() >= StartupProgress.Stage.ASSETS.ordinal()) {
            StartupProgress.complete();
        }
    }
}
