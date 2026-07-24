package com.pvp_utils.mixin.client;

import com.pvp_utils.client.ServerTranslationContents;
import com.pvp_utils.client.modules.impl.Combat.HitMarkerRenderer;
import com.pvp_utils.client.modules.impl.Render.ArmorTransparency.ArmorTransparencyManager;
import com.pvp_utils.client.modules.impl.Render.DamageNumberRenderer;
import com.pvp_utils.client.modules.impl.Render.TargetHudRenderer;
import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "handleSetTime", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$overrideServerTime(ClientboundSetTimePacket packet, CallbackInfo ci) {
        if (Config.timeChange) {
            Minecraft client = Minecraft.getInstance();
            ClientLevel level = client.level;
            if (level != null) {
                long time = Math.floorMod(Config.clientTime, 24000);
                level.setTimeFromServer(packet.gameTime(), time, false);
                ci.cancel();
            }
        }
    }

    @Redirect(method = "handleSetEntityData", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundSetEntityDataPacket;packedItems()Ljava/util/List;", ordinal = 0))
    private List<SynchedEntityData.DataValue<?>> fixLocalPlayerPoseTracker(ClientboundSetEntityDataPacket packet) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null) {
            Entity entity = client.level.getEntity(packet.id());
            if (entity instanceof LivingEntity living) {
                float oldHealth = living.getHealth();
                float maxHealth = living.getMaxHealth();
                for (SynchedEntityData.DataValue<?> value : packet.packedItems()) {
                    if (value.serializer().equals(EntityDataSerializers.FLOAT) && value.value() instanceof Float newHealth) {
                        if (newHealth >= 0.0f && newHealth <= maxHealth + 0.01f && Math.abs(newHealth - oldHealth) >= 0.05f) {
                            DamageNumberRenderer.getInstance().syncHealth(living, newHealth);
                            break;
                        }
                    }
                }
            }
        }
        if (Config.noDoubleSneak && client.level != null && client.player != null) {
            Entity entity = client.level.getEntity(packet.id());
            if (entity == client.player) {
                packet.packedItems().removeIf(value -> value.serializer().equals(EntityDataSerializers.POSE));
            }
        }

        return packet.packedItems();
    }

    @Inject(method = "handleDamageEvent", at = @At("HEAD"))
    private void onDamageEvent(ClientboundDamageEventPacket packet, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && client.player != null) {
            Entity target = client.level.getEntity(packet.entityId());
            if (target instanceof Player targetPlayer) {
                ArmorTransparencyManager.markCombat(targetPlayer);
            }
            Entity sourceCause = client.level.getEntity(packet.sourceCauseId());
            if (sourceCause instanceof Player sourcePlayer) {
                ArmorTransparencyManager.markCombat(sourcePlayer);
            }
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
                client.execute(() -> {
                    HitMarkerRenderer.getInstance().onHit(isRanged, finalColor);
                    TargetHudRenderer.getInstance().onHit(livingTarget);
                });
            }
        }
    }

    @Inject(method = "handleOpenSignEditor", at = @At("HEAD"))
    private void pvp_utils$trackSignTranslations(ClientboundOpenSignEditorPacket packet, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && client.level.getBlockEntity(packet.getPos()) instanceof SignBlockEntity sign) {
            ServerTranslationContents.trackSignText(packet.getPos(), packet.isFrontText(), sign.getText(packet.isFrontText()));
        }
    }

}
