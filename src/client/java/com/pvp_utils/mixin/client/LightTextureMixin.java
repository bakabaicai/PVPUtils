package com.pvp_utils.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.pvp_utils.client.modules.impl.Render.GammaOverrideManager;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
    @ModifyExpressionValue(
            method = "updateLightTexture",
            at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 1)
    )
    private float overrideGamma(float gamma) {
        return GammaOverrideManager.apply(gamma);
    }
}
