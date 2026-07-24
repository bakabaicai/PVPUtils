package com.pvp_utils.client;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class ServerNetworkingCompatibility {
    private static final Set<Connection> FABRIC_SERVERS = Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>()));

    private ServerNetworkingCompatibility() {
    }

    public static void observeInboundPayload(Connection connection, CustomPacketPayload payload) {
        String channel = payload.type().id().toString();
        if (channel.equals("c:version") || channel.equals("c:register")) {
            FABRIC_SERVERS.add(connection);
        }
    }

    public static boolean shouldBlock(Connection connection, Packet<?> packet) {
        if (!(packet instanceof ServerboundCustomPayloadPacket customPayload) || FABRIC_SERVERS.contains(connection)) {
            return false;
        }
        String channel = customPayload.payload().type().id().toString();
        return channel.equals("minecraft:register") || channel.equals("minecraft:unregister");
    }

    public static Packet<?> replaceBrand(Connection connection, Packet<?> packet) {
        if (packet instanceof ServerboundCustomPayloadPacket customPayload
                && customPayload.payload() instanceof BrandPayload
                && !FABRIC_SERVERS.contains(connection)) {
            return new ServerboundCustomPayloadPacket(new BrandPayload("vanilla"));
        }
        return packet;
    }
}
