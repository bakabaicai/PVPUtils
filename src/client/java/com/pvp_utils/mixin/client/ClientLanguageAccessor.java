package com.pvp_utils.mixin.client;

import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(ClientLanguage.class)
public interface ClientLanguageAccessor {
    @Invoker("<init>")
    static ClientLanguage pvp_utils$create(Map<String, String> translations, boolean defaultRightToLeft) {
        throw new UnsupportedOperationException();
    }
}
