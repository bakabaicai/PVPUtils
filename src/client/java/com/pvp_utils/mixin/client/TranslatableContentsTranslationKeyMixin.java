package com.pvp_utils.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.pvp_utils.client.TranslationKeyGuard;
import com.pvp_utils.client.TranslationKeyMarker;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TranslatableContents.class)
public class TranslatableContentsTranslationKeyMixin implements TranslationKeyMarker {

    @Unique
    private boolean pvp_utils$fromNetwork = false;

    @WrapOperation(
            method = "decompose",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/locale/Language;getOrDefault(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;")
    )
    private String pvp_utils$decomposeWithFallback(Language language, String key, String vanillaFallback, Operation<String> original) {
        if (pvp_utils$fromNetwork) {
            return TranslationKeyGuard.getTranslation(key, vanillaFallback);
        }
        return original.call(language, key, vanillaFallback);
    }

    @WrapOperation(
            method = "decompose",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/locale/Language;getOrDefault(Ljava/lang/String;)Ljava/lang/String;")
    )
    private String pvp_utils$decompose(Language language, String key, Operation<String> original) {
        if (pvp_utils$fromNetwork) {
            return TranslationKeyGuard.getTranslation(key, key);
        }
        return original.call(language, key);
    }

    @Override
    public void pvp_utils$markTranslationKey() {
        pvp_utils$fromNetwork = true;
    }

    @Override
    public boolean pvp_utils$isTranslationKeyMarked() {
        return pvp_utils$fromNetwork;
    }
}
