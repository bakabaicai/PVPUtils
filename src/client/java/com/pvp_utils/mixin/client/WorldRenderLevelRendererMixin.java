package com.pvp_utils.mixin.client;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.pvp_utils.client.modules.impl.Tool.FireballLandingPredictor;
import com.pvp_utils.client.render.world.CustomBlockOutlineRenderer;
import com.pvp_utils.client.render.world.CustomBlockOutlineRenderer;
import com.pvp_utils.client.render.world.WorldRender;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.gizmos.Gizmos;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class WorldRenderLevelRendererMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void pvp_utils$renderWorld(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, Camera camera, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, Matrix4f frustumMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
        WorldRender.capture(camera, modelViewMatrix, projectionMatrix);
        try (Gizmos.TemporaryCollection ignored = ((LevelRenderer) (Object) this).collectPerFrameGizmos()) {
            FireballLandingPredictor.render();
            CustomBlockOutlineRenderer.render();
            CustomBlockOutlineRenderer.render();
        }
    }
}
