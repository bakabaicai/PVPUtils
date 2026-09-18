package com.pvp_utils.client.net;

import com.pvp_utils.client.net.ChannelPayloads.ChannelC2S;
import com.pvp_utils.client.net.ChannelPayloads.ChannelS2C;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ChannelChatManager {
    private static final int MAX_CHANNEL = 32;
    private static final int MAX_MESSAGE = 256;

    private ChannelChatManager() {
    }

    public static void init() {
        PayloadTypeRegistry.playC2S().register(ChannelC2S.TYPE, ChannelC2S.CODEC);
        PayloadTypeRegistry.playS2C().register(ChannelS2C.TYPE, ChannelS2C.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ChannelS2C.TYPE, (payload, context) -> {
            Minecraft client = context.client();
            client.execute(() -> {
                if (client.player == null) return;
                client.player.displayClientMessage(Component.literal(
                        "§7[§bPVP§7/§f" + payload.channel() + "§7] §e" + payload.sender() + "§7 » §r" + payload.message()), false);
            });
        });
    }

    public static boolean send(String channel, String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) return false;
        if (channel == null || channel.trim().isEmpty()) return false;
        if (message == null || message.trim().isEmpty()) return false;
        String ch = channel.trim();
        String msg = message.trim();
        if (ch.length() > MAX_CHANNEL) ch = ch.substring(0, MAX_CHANNEL);
        if (msg.length() > MAX_MESSAGE) msg = msg.substring(0, MAX_MESSAGE);
        ClientPlayNetworking.send(new ChannelC2S(ch, msg));
        return true;
    }
}
