package com.pvp_utils.client;

import com.pvp_utils.Config;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;

public final class ServerNetworkingCompatibility {
    private ServerNetworkingCompatibility() {
    }

    public static boolean shouldBlock(Packet<?> packet) {
        if (!Config.modifyChannels || !(packet instanceof ServerboundCustomPayloadPacket customPayload)) {
            return false;
        }
        String channel = customPayload.payload().type().id().toString();
        return channel.equals("minecraft:register") || channel.equals("minecraft:unregister");
    }

    public static Packet<?> replaceBrand(Packet<?> packet) {
        if (Config.modifyBrand
                && packet instanceof ServerboundCustomPayloadPacket customPayload
                && customPayload.payload() instanceof BrandPayload) {
            return new ServerboundCustomPayloadPacket(new BrandPayload("vanilla"));
        }
        return packet;
    }
}
