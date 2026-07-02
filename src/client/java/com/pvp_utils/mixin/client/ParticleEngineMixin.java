package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void hideExplosionParticles(ParticleOptions particleOptions, double x, double y, double z, double vx, double vy, double vz, CallbackInfoReturnable<Particle> cir) {
        if (shouldHideParticle(particleOptions)) {
            cir.setReturnValue(null);
        }
    }

    private boolean shouldHideParticle(ParticleOptions particleOptions) {
        return (Config.hideExplosionParticles && isExplosionParticle(particleOptions))
                || (Config.hideRainParticles && isRainParticle(particleOptions));
    }

    private boolean isExplosionParticle(ParticleOptions particleOptions) {
        return particleOptions == ParticleTypes.EXPLOSION
                || particleOptions == ParticleTypes.EXPLOSION_EMITTER
                || particleOptions == ParticleTypes.POOF
                || particleOptions == ParticleTypes.SMOKE
                || particleOptions == ParticleTypes.LARGE_SMOKE;
    }

    private boolean isRainParticle(ParticleOptions particleOptions) {
        return particleOptions == ParticleTypes.RAIN
                || particleOptions == ParticleTypes.DRIPPING_WATER
                || particleOptions == ParticleTypes.FALLING_WATER;
    }
}
