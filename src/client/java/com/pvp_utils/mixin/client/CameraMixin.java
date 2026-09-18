package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Render.MotionCamera.MotionCameraManager;
import com.pvp_utils.client.modules.impl.Tool.Freelook.FreelookManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private Entity entity;
    @Shadow private boolean detached;
    @Shadow private float eyeHeight;
    @Shadow protected abstract void setPosition(Vec3 position);
    @Shadow protected abstract void move(float zoom, float y, float x);
    @Shadow protected abstract float getMaxZoom(float desiredCameraDistance);
    @Unique private final Pose[] pvpUtils$lastPoses = new Pose[2];

    @Inject(method = "tick", at = @At("HEAD"))
    private void storeSneakPose(CallbackInfo ci) {
        if (this.entity == null) return;

        Pose pose = this.entity.getPose();
        if (pose != this.pvpUtils$lastPoses[0]) {
            this.pvpUtils$lastPoses[1] = this.pvpUtils$lastPoses[0];
            this.pvpUtils$lastPoses[0] = pose;
        }
    }

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Camera;eyeHeight:F", ordinal = 0))
    private void updateInstantSneakEyeHeight(CallbackInfo ci) {
        if (Config.sneakAnimationSpeed < 1.0f || !isValidSneakTransition() || !(this.entity instanceof LocalPlayer player)) {
            return;
        }

        this.eyeHeight = getCustomEyeHeight(player, player.getPose());
    }

    @ModifyConstant(method = "tick", constant = @Constant(floatValue = 0.5f))
    private float updateSneakEyeHeightSpeed(float modifier) {
        if (!isValidSneakTransition()) {
            return modifier;
        }

        return Config.sneakAnimationSpeed >= 1.0f ? 0.0f : getSneakAnimationModifier();
    }

    @ModifyArgs(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"))
    private void applyFreelookRotation(Args args) {
        Minecraft client = Minecraft.getInstance();
        if (FreelookManager.isActive() && this.entity == client.player) {
            args.set(0, FreelookManager.getYaw());
            args.set(1, FreelookManager.getPitch());
        }
    }

    @Inject(method = "setup", at = @At("RETURN"))
    private void pvp_utils$applyMotionCamera(Level level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (entity != client.player) {
            return;
        }

        if (MotionCameraManager.active(detached, entity)) {
            this.setPosition(MotionCameraManager.updateAnchor(entity, partialTick));
            float smoothDistance = MotionCameraManager.smoothDistance(partialTick, true);
            this.move(-this.getMaxZoom(smoothDistance), 0.0f, 0.0f);
            return;
        }
        if (Config.motionCamera && MotionCameraManager.transitioning()) {
            float smoothDistance = MotionCameraManager.smoothDistance(partialTick, false);
            if (smoothDistance > 0.001f) {
                this.move(-this.getMaxZoom(smoothDistance), 0.0f, 0.0f);
            }
            return;
        }
        if (!Config.motionCamera) {
            MotionCameraManager.reset();
        }

    }

    private float getCustomEyeHeight(LocalPlayer player, Pose pose) {
        float standingEyeHeight = player.getDimensions(Pose.STANDING).eyeHeight();
        if (pose != Pose.CROUCHING) {
            return standingEyeHeight;
        }

        float crouchingEyeHeight = player.getDimensions(Pose.CROUCHING).eyeHeight();
        float sneakDrop = standingEyeHeight - crouchingEyeHeight;
        return standingEyeHeight - sneakDrop * Config.sneakDropScale;
    }

    private float getSneakAnimationModifier() {
        return 0.5f + Config.sneakAnimationSpeed * 0.5f;
    }

    @Unique
    private boolean isValidSneakTransition() {
        if (!Config.noSneakAnimation || this.detached || !(this.entity instanceof LocalPlayer player)) {
            return false;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player != player) {
            return false;
        }

        return this.pvpUtils$lastPoses[1] == Pose.STANDING && this.pvpUtils$lastPoses[0] == Pose.CROUCHING
                || this.pvpUtils$lastPoses[1] == Pose.CROUCHING && this.pvpUtils$lastPoses[0] == Pose.STANDING;
    }
}
