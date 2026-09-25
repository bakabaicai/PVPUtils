package com.pvp_utils.client;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

public final class ReconnectionHelper {
    private static ServerAddress lastServerAddress;
    private static ServerData lastServerData;

    private ReconnectionHelper() {}

    public static void recordConnection(ServerAddress address, ServerData data) {
        lastServerAddress = address;
        lastServerData = data;
    }

    public static ServerAddress getLastServerAddress() {
        return lastServerAddress;
    }

    public static ServerData getLastServerData() {
        return lastServerData;
    }

    public static boolean hasLastServer() {
        return lastServerAddress != null;
    }

    public static void clear() {
        lastServerAddress = null;
        lastServerData = null;
    }
}
