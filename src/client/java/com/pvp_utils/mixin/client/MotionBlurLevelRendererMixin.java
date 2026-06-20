package com.pvp_utils.mixin.client;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import com.pvp_utils.client.modules.impl.Render.motionblur.MotionBlurManager;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MotionBlurLevelRendererMixin {
    @Unique private final Matrix4f pvp_utils$prevModelView = new Matrix4f();
    @Unique private final Matrix4f pvp_utils$prevProjection = new Matrix4f();
    @Unique private final Matrix4f pvp_utils$scratchModelView = new Matrix4f();
    @Unique private final Matrix4f pvp_utils$scratchProjection = new Matrix4f();
    @Unique private double pvp_utils$prevCamX;
    @Unique private double pvp_utils$prevCamY;
    @Unique private double pvp_utils$prevCamZ;
    @Unique private boolean pvp_utils$previousFrameReady = false;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void pvp_utils$onRenderLevelHead(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, Camera camera, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, Matrix4f frustumMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
        boolean blurActive = MotionBlurManager.shouldRun();
        var camPos = camera.position();
        double cx = camPos.x();
        double cy = camPos.y();
        double cz = camPos.z();

        if (!blurActive) {
            MotionBlurManager.clearFrameAllocator();
            pvp_utils$rememberCurrentFrameState(modelViewMatrix, projectionMatrix, cx, cy, cz);
            return;
        }

        MotionBlurManager.captureAllocator(resourceAllocator);
        MotionBlurManager.beginFrame();
        pvp_utils$scratchModelView.set(modelViewMatrix);
        pvp_utils$scratchProjection.set(projectionMatrix);

        if (!pvp_utils$previousFrameReady) {
            MotionBlurManager.setFrameMotionBlur(pvp_utils$scratchModelView, pvp_utils$scratchModelView, pvp_utils$scratchProjection, pvp_utils$scratchProjection, 0.0f, 0.0f, 0.0f);
            pvp_utils$rememberCurrentFrameState(pvp_utils$scratchModelView, pvp_utils$scratchProjection, cx, cy, cz);
            return;
        }

        MotionBlurManager.setFrameMotionBlur(
                pvp_utils$scratchModelView,
                pvp_utils$prevModelView,
                pvp_utils$scratchProjection,
                pvp_utils$prevProjection,
                (float) (cx - pvp_utils$prevCamX),
                (float) (cy - pvp_utils$prevCamY),
                (float) (cz - pvp_utils$prevCamZ));
        pvp_utils$rememberCurrentFrameState(pvp_utils$scratchModelView, pvp_utils$scratchProjection, cx, cy, cz);
    }

    @Inject(method = "submitEntities", at = @At("HEAD"))
    private void pvp_utils$beforeSubmitEntities(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector output, CallbackInfo ci) {
        if (!MotionBlurManager.shouldRun()) return;
        if (pvp_utils$shouldUseSpecialSingleBlur()) {
            MotionBlurManager.applyF5EntityRideBlur();
            return;
        }
        MotionBlurManager.applyPreEntityBlur();
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void pvp_utils$onRenderLevelTail(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, Camera camera, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, Matrix4f frustumMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
        if (MotionBlurManager.shouldRun() && !pvp_utils$shouldUseSpecialSingleBlur()) {
            MotionBlurManager.applyPostRenderVelocityOnly();
        }
    }

    @Unique
    private void pvp_utils$rememberCurrentFrameState(Matrix4fc modelViewMatrix, Matrix4fc projectionMatrix, double cx, double cy, double cz) {
        pvp_utils$prevModelView.set(modelViewMatrix);
        pvp_utils$prevProjection.set(projectionMatrix);
        pvp_utils$prevCamX = cx;
        pvp_utils$prevCamY = cy;
        pvp_utils$prevCamZ = cz;
        pvp_utils$previousFrameReady = true;
    }

    @Unique
    private boolean pvp_utils$shouldUseSpecialSingleBlur() {
        Minecraft client = Minecraft.getInstance();
        if (client.options.getCameraType() != CameraType.FIRST_PERSON) return true;
        return client.player != null && client.player.isPassenger();
    }
}
