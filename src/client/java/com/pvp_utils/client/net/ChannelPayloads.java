package com.pvp_utils.client.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class ChannelPayloads {
    private ChannelPayloads() {
    }

    public record ChannelC2S(String channel, String message) implements CustomPacketPayload {
        public static final Type<ChannelC2S> TYPE = new Type<>(Identifier.fromNamespaceAndPath("pvputils", "channel_c2s"));
        public static final StreamCodec<FriendlyByteBuf, ChannelC2S> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ChannelC2S::channel,
                ByteBufCodecs.STRING_UTF8, ChannelC2S::message,
                ChannelC2S::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ChannelS2C(String sender, String channel, String message) implements CustomPacketPayload {
        public static final Type<ChannelS2C> TYPE = new Type<>(Identifier.fromNamespaceAndPath("pvputils", "channel_s2c"));
        public static final StreamCodec<FriendlyByteBuf, ChannelS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ChannelS2C::sender,
                ByteBufCodecs.STRING_UTF8, ChannelS2C::channel,
                ByteBufCodecs.STRING_UTF8, ChannelS2C::message,
                ChannelS2C::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
