package com.pvp_utils.client;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Wraps the vanilla component codec so that every component decoded from a server
 * packet is marked before it reaches the screen. Marked components refuse to resolve
 * client-side mod translation keys, which is what makes sign / anvil (and every other
 * server controlled text) unable to probe installed mods.
 */
public record TranslationKeyFilteredCodec(Codec<Component> delegate) implements Codec<Component> {

    @Override
    public <T> DataResult<Pair<Component, T>> decode(DynamicOps<T> ops, T input) {
        DataResult<Pair<Component, T>> result = delegate.decode(ops, input);
        if (!TranslationKeyGuard.isFromPacket() || !Config.modifyTranslationKeys) {
            return result;
        }
        if (Minecraft.getInstance().hasSingleplayerServer()) {
            return result;
        }
        return result.map(pair -> pair.mapFirst(TranslationKeyGuard::markFromNetwork));
    }

    @Override
    public <T> DataResult<T> encode(Component input, DynamicOps<T> ops, T prefix) {
        return delegate.encode(input, ops, prefix);
    }
}
