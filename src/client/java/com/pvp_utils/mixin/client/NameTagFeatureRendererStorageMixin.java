package com.pvp_utils.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pvp_utils.Config;
import com.pvp_utils.client.util.NameTagPlayerFilterContext;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(targets = "net.minecraft.client.renderer.feature.NameTagFeatureRenderer$Storage")
public class NameTagFeatureRendererStorageMixin {
    @Unique private double pvp_utils$nameTagDistanceSq = 64.0;

    @Inject(method = "add", at = @At("HEAD"))
    private void pvp_utils$captureNameTagDistance(PoseStack poseStack, Vec3 attachment, int yOffset, Component component, boolean seeThrough, int light, double distanceToCameraSq, CameraRenderState cameraRenderState, CallbackInfo ci) {
        pvp_utils$nameTagDistanceSq = distanceToCameraSq;
    }

    @ModifyArgs(
            method = "add",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V")
    )
    private void pvp_utils$modifyNameTagScale(Args args) {
        if (!Config.nameTag) return;
        if (Config.nameTagOnlyPlayer && !NameTagPlayerFilterContext.isRealPlayer()) return;
        float scale = clamp(Config.nameTagScale, 0.5f, 3.0f) * dynamicScale();
        args.set(0, (float) args.get(0) * scale);
        args.set(1, (float) args.get(1) * scale);
        args.set(2, (float) args.get(2) * scale);
    }

    private float dynamicScale() {
        if (!Config.nameTagDynamicScale) return 1.0f;
        return clamp((float) (Math.sqrt(Math.max(0.0, pvp_utils$nameTagDistanceSq)) / 8.0), 0.5f, 8.0f);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
