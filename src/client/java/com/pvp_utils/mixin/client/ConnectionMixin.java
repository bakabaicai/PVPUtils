package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {
    @Shadow
    public void send(Packet<?> packet) {
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"))
    private void beforeSend(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (!Config.criticalAssist || !Config.fullMode || client.player == null) return;
        if (!(packet instanceof ServerboundInteractPacket interactPacket) || !isAttackPacket(interactPacket)) return;
        if (!client.player.isSprinting() || !canCritical(client)) return;

        this.send(new ServerboundPlayerCommandPacket(client.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
        client.player.setSprinting(false);
    }

    private boolean isAttackPacket(ServerboundInteractPacket packet) {
        final boolean[] attack = {false};
        packet.dispatch(new ServerboundInteractPacket.Handler() {
            @Override
            public void onInteraction(InteractionHand interactionHand) {
            }

            @Override
            public void onInteraction(InteractionHand interactionHand, Vec3 vec3) {
            }

            @Override
            public void onAttack() {
                attack[0] = true;
            }
        });
        return attack[0];
    }

    private boolean canCritical(Minecraft client) {
        return client.player.fallDistance > 0.0f
                && !client.player.onGround()
                && !client.player.onClimbable()
                && !client.player.isInWater()
                && !client.player.isInLava()
                && !client.player.isPassenger()
                && !client.player.isUsingItem();
    }
}
