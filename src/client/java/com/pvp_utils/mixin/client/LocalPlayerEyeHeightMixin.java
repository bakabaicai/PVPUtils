package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class LocalPlayerEyeHeightMixin {
    @Inject(method = "getEyeHeight", at = @At("HEAD"), cancellable = true)
    private void modifyFastSneakEyeHeight(Pose pose, CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof LocalPlayer player)) {
            return;
        }

        if (!Config.noSneakAnimation || Minecraft.getInstance().player != player || pose != Pose.CROUCHING) {
            return;
        }

        if (!((PlayerPoseAccessMixin) player).pvpUtils$canPlayerFitWithinBlocksAndEntitiesWhen(Pose.STANDING)) {
            return;
        }

        float crouchingEyeHeight = player.getDimensions(Pose.CROUCHING).eyeHeight();
        float standingEyeHeight = player.getDimensions(Pose.STANDING).eyeHeight();
        float sneakDrop = standingEyeHeight - crouchingEyeHeight;
        cir.setReturnValue(standingEyeHeight - sneakDrop * Config.sneakDropScale);
    }

    @Inject(method = "getEyeHeight()F", at = @At("HEAD"), cancellable = true)
    private void modifyFastSneakCurrentEyeHeight(CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof LocalPlayer player)) {
            return;
        }

        if (!Config.noSneakAnimation || Minecraft.getInstance().player != player || player.getPose() != Pose.CROUCHING) {
            return;
        }

        if (!((PlayerPoseAccessMixin) player).pvpUtils$canPlayerFitWithinBlocksAndEntitiesWhen(Pose.STANDING)) {
            return;
        }

        float crouchingEyeHeight = player.getDimensions(Pose.CROUCHING).eyeHeight();
        float standingEyeHeight = player.getDimensions(Pose.STANDING).eyeHeight();
        float sneakDrop = standingEyeHeight - crouchingEyeHeight;
        cir.setReturnValue(standingEyeHeight - sneakDrop * Config.sneakDropScale);
    }
}
