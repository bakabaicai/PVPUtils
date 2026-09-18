package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.particle.ItemPickupParticle;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemPickupParticle.class)
public abstract class ItemPickupParticleMixin {
    @Shadow @Final private Entity target;

    @ModifyExpressionValue(method = "updatePosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getEyeY()D"))
    private double pvp_utils$legacy17PickupPosition(double original) {
        return Config.legacy17Animations && Config.legacy17ItemPickup ? this.target.position().y : original;
    }
}
