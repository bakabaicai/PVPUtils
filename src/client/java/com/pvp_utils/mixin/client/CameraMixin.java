package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private Level level;
    @Shadow private Entity entity;
    @Shadow private boolean detached;
    @Shadow private float eyeHeight;
    @Shadow private float eyeHeightOld;
    @Shadow private Vec3 position;
    @Shadow private EnvironmentAttributeProbe attributeProbe;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void removeFirstPersonSneakSmoothing(CallbackInfo ci) {
        if (!Config.noSneakAnimation || this.detached || !(this.entity instanceof LocalPlayer player)) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player != player) {
            return;
        }

        Pose pose = player.getPose();
        if (pose == Pose.SLEEPING || pose == Pose.SWIMMING) {
            return;
        }

        this.eyeHeightOld = this.eyeHeight;
        this.eyeHeight = getCustomEyeHeight(player, pose);
        this.attributeProbe.tick(this.level, this.position);
        ci.cancel();
    }

    private float getCustomEyeHeight(LocalPlayer player, Pose pose) {
        float standingEyeHeight = player.getEyeHeight(Pose.STANDING);
        if (pose != Pose.CROUCHING) {
            return standingEyeHeight;
        }

        float crouchingEyeHeight = player.getEyeHeight(Pose.CROUCHING);
        float sneakDrop = standingEyeHeight - crouchingEyeHeight;
        return standingEyeHeight - sneakDrop * Config.sneakDropScale;
    }
}
