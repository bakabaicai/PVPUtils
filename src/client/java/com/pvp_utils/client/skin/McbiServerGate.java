package com.pvp_utils.client.skin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.util.Locale;

public final class McbiServerGate {
    private static boolean connectedToMcbi;

    private McbiServerGate() {
    }

    public static void tick(Minecraft client) {
        connectedToMcbi = isMcbi(client);
    }

    public static boolean isAvailable() {
        Minecraft client = Minecraft.getInstance();
        connectedToMcbi = isMcbi(client);
        return connectedToMcbi;
    }

    private static boolean isMcbi(Minecraft client) {
        ServerData server = client == null ? null : client.getCurrentServer();
        if (server == null || server.ip == null) {
            return false;
        }
        String address = server.ip.strip().toLowerCase(Locale.ROOT);
        int colon = address.indexOf(':');
        if (colon >= 0) {
            address = address.substring(0, colon);
        }
        return address.equals("mcbi.top");
    }
}
