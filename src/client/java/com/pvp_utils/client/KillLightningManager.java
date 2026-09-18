package com.pvp_utils.client;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.AABB;

public final class KillLightningManager {
    private KillLightningManager() {
    }

    public static void spawnAt(Entity target) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        int count = Math.max(1, Math.min(5, Config.attackEffectsLightningCount));
        AABB box = target.getBoundingBox();
        RandomSource random = target.getRandom();
        for (int i = 0; i < count; i++) {
            LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, client.level);
            lightning.setVisualOnly(true);
            double x = target.getX() + (random.nextDouble() - 0.5D) * Math.max(0.2D, box.getXsize());
            double z = target.getZ() + (random.nextDouble() - 0.5D) * Math.max(0.2D, box.getZsize());
            lightning.setPos(x, target.getY(), z);
            client.level.addEntity(lightning);
        }
    }
}
