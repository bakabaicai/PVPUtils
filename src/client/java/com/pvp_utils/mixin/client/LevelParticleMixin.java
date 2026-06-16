package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public class LevelParticleMixin {
    @Inject(method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V", at = @At("HEAD"), cancellable = true)
    private void hideExplosionParticles(ParticleOptions particleOptions, double x, double y, double z, double vx, double vy, double vz, CallbackInfo ci) {
        if (Config.hideExplosionParticles && isExplosionParticle(particleOptions)) {
            ci.cancel();
        }
    }

    @Inject(method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)V", at = @At("HEAD"), cancellable = true)
    private void hideExplosionParticlesForced(ParticleOptions particleOptions, boolean force, boolean important, double x, double y, double z, double vx, double vy, double vz, CallbackInfo ci) {
        if (Config.hideExplosionParticles && isExplosionParticle(particleOptions)) {
            ci.cancel();
        }
    }

    @Inject(method = "addAlwaysVisibleParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V", at = @At("HEAD"), cancellable = true)
    private void hideAlwaysVisibleExplosionParticles(ParticleOptions particleOptions, double x, double y, double z, double vx, double vy, double vz, CallbackInfo ci) {
        if (Config.hideExplosionParticles && isExplosionParticle(particleOptions)) {
            ci.cancel();
        }
    }

    @Inject(method = "addAlwaysVisibleParticle(Lnet/minecraft/core/particles/ParticleOptions;ZDDDDDD)V", at = @At("HEAD"), cancellable = true)
    private void hideAlwaysVisibleExplosionParticlesForced(ParticleOptions particleOptions, boolean ignoreRange, double x, double y, double z, double vx, double vy, double vz, CallbackInfo ci) {
        if (Config.hideExplosionParticles && isExplosionParticle(particleOptions)) {
            ci.cancel();
        }
    }

    private boolean isExplosionParticle(ParticleOptions particleOptions) {
        return particleOptions == ParticleTypes.EXPLOSION || particleOptions == ParticleTypes.EXPLOSION_EMITTER;
    }
}
