package com.pvp_utils.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pvp_utils.Config;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class CustomBlockOutlineLevelRendererMixin {
    @Inject(method = "renderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$hideVanillaBlockOutline(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, boolean renderBlockOutline, LevelRenderState levelRenderState, CallbackInfo ci) {
        if (Config.customBlockOutline) {
            ci.cancel();
        }
    }
}
