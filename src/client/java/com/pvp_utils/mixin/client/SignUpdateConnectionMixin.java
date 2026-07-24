package com.pvp_utils.mixin.client;

import com.pvp_utils.client.ServerNetworkingCompatibility;
import com.pvp_utils.client.ServerTranslationContents;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class SignUpdateConnectionMixin {
    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void pvp_utils$blockRegistrationForNonFabricServers(Packet<?> packet, ChannelFutureListener listener, CallbackInfo ci) {
        if (ServerNetworkingCompatibility.shouldBlock((Connection) (Object) this, packet)) {
            ci.cancel();
        }
    }

    @ModifyVariable(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Packet<?> pvp_utils$replaceSignUpdate(Packet<?> packet) {
        if (packet instanceof ServerboundSignUpdatePacket signUpdate) {
            return ServerTranslationContents.replaceSignUpdate(signUpdate);
        }
        return ServerNetworkingCompatibility.replaceBrand((Connection) (Object) this, packet);
    }
}
