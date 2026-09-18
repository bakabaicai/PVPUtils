package com.pvp_utils.mixin.client;

import com.pvp_utils.client.ServerNetworkingCompatibility;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.network.Connection.class)
public class ClientConnectionMixin {
    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void pvp_utils$blockRegistrationChannels(Packet<?> packet, ChannelFutureListener listener, CallbackInfo ci) {
        if (ServerNetworkingCompatibility.shouldBlock(packet)) {
            ci.cancel();
        }
    }

    @ModifyVariable(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Packet<?> pvp_utils$replaceBrand(Packet<?> packet) {
        return ServerNetworkingCompatibility.replaceBrand(packet);
    }

}
