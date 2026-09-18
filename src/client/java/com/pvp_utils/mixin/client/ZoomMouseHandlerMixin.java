package com.pvp_utils.mixin.client;

import com.pvp_utils.client.modules.impl.Tool.Freelook.FreelookManager;
import com.pvp_utils.client.modules.impl.Tool.Zoom.ZoomManager;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(MouseHandler.class)
public abstract class ZoomMouseHandlerMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$handleZoomScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (ZoomManager.isZooming() && vertical != 0.0) {
            ZoomManager.scroll(vertical);
            ci.cancel();
        }
    }

    @ModifyArgs(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void pvp_utils$applyZoomSensitivity(Args args) {
        double multiplier = ZoomManager.getMouseSensitivityMultiplier();
        double yawDelta = ((Double) args.get(0)) * multiplier;
        double pitchDelta = ((Double) args.get(1)) * multiplier;

        if (FreelookManager.isActive()) {
            FreelookManager.turn(yawDelta, pitchDelta);
            args.set(0, 0.0D);
            args.set(1, 0.0D);
            return;
        }

        args.set(0, yawDelta);
        args.set(1, pitchDelta);
    }
}
