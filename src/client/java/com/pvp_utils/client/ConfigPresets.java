package com.pvp_utils.client;

import com.pvp_utils.Config;

public final class ConfigPresets {
    private ConfigPresets() {
    }

    public static void applyDefault() {
        Config.entityOptimize = false;
        Config.hideExplosionParticles = false;
        Config.hideRainParticles = false;
        Config.hideFireParticles = false;
        Config.hideAmbientParticles = false;
        Config.cacheCleaner = true;
        Config.cacheCleanerInterval = 10;
        Config.attackEffectsLightning = false;
        Config.attackEffectsCritParticles = true;
        Config.attackEffectsBloodParticles = false;
        Config.attackEffectsSharpnessParticles = true;
        Config.droppedItemRadar = false;
        Config.smoothHotbarScrolling = false;
        Config.save();
    }

    public static void applyPvp() {
        Config.entityOptimize = false;
        Config.hideExplosionParticles = false;
        Config.hideRainParticles = false;
        Config.hideFireParticles = false;
        Config.hideAmbientParticles = false;
        Config.cacheCleaner = true;
        Config.cacheCleanerInterval = 10;
        Config.attackEffectsLightning = true;
        Config.attackEffectsLightningCount = 1;
        Config.attackEffectsCritParticles = true;
        Config.attackEffectsBloodParticles = false;
        Config.attackEffectsSharpnessParticles = true;
        Config.droppedItemRadar = true;
        Config.smoothHotbarScrolling = true;
        Config.save();
    }

    public static void applyLowEnd() {
        Config.entityOptimize = true;
        Config.entityRenderDistance = 64;
        Config.itemRenderDistance = 48;
        Config.xpOrbRenderDistance = 32;
        Config.arrowRenderDistance = 64;
        Config.hideExplosionParticles = true;
        Config.hideRainParticles = true;
        Config.hideFireParticles = true;
        Config.hideAmbientParticles = true;
        Config.cacheCleaner = true;
        Config.cacheCleanerInterval = 10;
        Config.attackEffectsLightning = false;
        Config.attackEffectsCritParticles = false;
        Config.attackEffectsBloodParticles = false;
        Config.attackEffectsSharpnessParticles = false;
        Config.droppedItemRadar = false;
        Config.smoothHotbarScrolling = false;
        Config.save();
    }
}
