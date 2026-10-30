package com.pvp_utils.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.pvp_utils.client.TranslationKeyGuard;
import net.minecraft.network.PacketDecoder;
import net.minecraft.network.codec.StreamCodec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Flags the current thread while a packet is being decoded, so the component codec
 * only filters text that actually arrived from a server.
 */
@Mixin(PacketDecoder.class)
public class PacketDecoderMixin {

    @WrapOperation(
            method = "decode",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/codec/StreamCodec;decode(Ljava/lang/Object;)Ljava/lang/Object;")
    )
    private Object pvp_utils$markPacketDecoding(StreamCodec<?, ?> codec, Object buffer, Operation<Object> original) {
        TranslationKeyGuard.setFromPacket(true);
        try {
            return original.call(codec, buffer);
        } finally {
            TranslationKeyGuard.setFromPacket(false);
        }
    }
}
