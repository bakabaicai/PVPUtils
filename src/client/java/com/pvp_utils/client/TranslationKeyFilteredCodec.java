package com.pvp_utils.client;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

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
