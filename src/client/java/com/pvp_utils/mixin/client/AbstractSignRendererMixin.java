package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignRenderer.class)
public abstract class AbstractSignRendererMixin {
    @Inject(method = "submitSignText", at = @At("HEAD"), cancellable = true)
    private void hideSignText(SignRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, boolean front, CallbackInfo ci) {
        if (Config.hideSignText) ci.cancel();
    }
}
