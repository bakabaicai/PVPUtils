package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.WeatherRenderState;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherEffectRenderer.class)
public class WeatherEffectRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void pvp_utils$hideRainColumns(Level level, int ticks, float partialTick, Vec3 cameraPosition, WeatherRenderState state, CallbackInfo ci) {
        if (Config.hideRainParticles) {
            state.rainColumns.clear();
        }
    }

    @Inject(method = "tickRainParticles", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$hideRainDrops(ClientLevel level, Camera camera, int ticks, ParticleStatus particleStatus, int particleCount, CallbackInfo ci) {
        if (Config.hideRainParticles) {
            ci.cancel();
        }
    }
}
