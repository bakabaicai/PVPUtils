package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.pvp_utils.client.KillLightningManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityDeathMixin {
    @Inject(method = "die", at = @At("HEAD"))
    private void pvp_utils$killLightning(DamageSource source, CallbackInfo ci) {
        if (!Config.attackEffectsLightning) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity == client.player) return;
        Entity src = source.getEntity();
        if (src != client.player) {
            if (!(src instanceof Projectile projectile) || projectile.getOwner() != client.player) {
                return;
            }
        }
        KillLightningManager.spawnAt(entity);
    }
}
