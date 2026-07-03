package com.pvp_utils.mixin.client;

import com.pvp_utils.client.modules.impl.Render.motionblur.MotionBlurManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class, priority = 500)
public class MotionBlurGameRendererMixin {
    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void pvp_utils$afterRenderLevel(DeltaTracker deltaTracker, CallbackInfo ci) {
        MotionBlurManager.applyTemporalBlur();
        MotionBlurManager.clearFrameAllocator();
    }
}
