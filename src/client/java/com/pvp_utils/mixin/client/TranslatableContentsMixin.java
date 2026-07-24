package com.pvp_utils.mixin.client;

import com.pvp_utils.client.ServerTranslationContents;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TranslatableContents.class)
public class TranslatableContentsMixin {
    @Redirect(
            method = "decompose",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/locale/Language;getOrDefault(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;")
    )
    private String pvp_utils$useFallback(Language language, String key, String fallback) {
        return ServerTranslationContents.isServerContents((TranslatableContents) (Object) this)
                ? fallback
                : language.getOrDefault(key, fallback);
    }

    @Redirect(
            method = "decompose",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/locale/Language;getOrDefault(Ljava/lang/String;)Ljava/lang/String;")
    )
    private String pvp_utils$useTranslationKey(Language language, String key) {
        return ServerTranslationContents.isServerContents((TranslatableContents) (Object) this)
                ? key
                : language.getOrDefault(key);
    }
}
