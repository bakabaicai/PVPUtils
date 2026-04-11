package com.pvp_utils.mixin.client;

import com.pvp_utils.DamageRecordHandler;
import com.pvp_utils.client.gui.HitMarkerRenderer;
import com.pvp_utils.client.gui.TargetHudRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "handleDamageEvent", at = @At("HEAD"))
    private void onDamageEvent(ClientboundDamageEventPacket packet, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && client.player != null) {
            Entity target = client.level.getEntity(packet.entityId());
            if (target instanceof LivingEntity livingTarget && packet.sourceCauseId() == client.player.getId()) {
                Entity source = client.level.getEntity(packet.sourceDirectId());
                boolean isRanged = source instanceof Projectile;

                boolean isCrit = !isRanged &&
                        client.player.fallDistance > 0.0f &&
                        !client.player.onGround();

                int colorValue = 0xFFFFFF;
                if (isCrit) {
                    colorValue = 0xFF0000;
                } else if (isRanged) {
                    colorValue = 0x00FF00;
                }

                final int finalColor = colorValue;
                float cooldown = client.player.getAttackStrengthScale(0.5f);
                final float baseDamage = 2.0f + (cooldown * 7.0f);
                final float finalDamage = isCrit ? baseDamage * 1.5f : baseDamage;

                client.execute(() -> {
                    HitMarkerRenderer.getInstance().onHit(isRanged, finalColor);
                    DamageRecordHandler.showDamage(livingTarget, finalDamage, isRanged);
                    TargetHudRenderer.getInstance().onHit(livingTarget);
                });
            }
        }
    }
}