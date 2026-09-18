package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.client.renderer.entity.FishingHookRenderer")
public abstract class FishingHookRendererMixin {
    @ModifyArgs(
            method = "getPlayerHandPos",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera$NearPlane;getPointOnPlane(FF)Lnet/minecraft/world/phys/Vec3;"
            )
    )
    private void pvp_utils$legacy17LineStart(Args args) {
        if (Config.legacy17Animations && Config.legacy17FishingRod) {
            args.set(1, ((float) args.get(1)) + 0.15F);
        }
    }

    @ModifyExpressionValue(
            method = "getPlayerHandPos",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;isCrouching()Z"
            )
    )
    private boolean pvp_utils$legacy17LineCrouch(boolean original) {
        return Config.legacy17Animations && Config.legacy17FishingRod ? false : original;
    }

    @ModifyExpressionValue(
            method = "getPlayerHandPos",
            at = @At(value = "CONSTANT", args = "doubleValue=0.8")
    )
    private double pvp_utils$legacy17LinePosition(double original) {
        return Config.legacy17Animations && Config.legacy17FishingRod ? original + 0.05D : original;
    }

    @ModifyArg(
            method = "method_72983",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/FishingHookRenderer;stringVertex(FFFLcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;FFF)V"
            ),
            index = 7
    )
    private static float pvp_utils$legacy17LineThickness(float original) {
        return Config.legacy17Animations && Config.legacy17FishingRod ? 2.0F : original;
    }
}
