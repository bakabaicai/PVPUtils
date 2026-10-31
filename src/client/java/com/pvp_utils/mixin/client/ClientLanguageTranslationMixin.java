package com.pvp_utils.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.pvp_utils.client.TranslationKeyGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;
import java.util.List;
import java.util.function.BiConsumer;

@Mixin(ClientLanguage.class)
public class ClientLanguageTranslationMixin {
    @Inject(method = "loadFrom", at = @At("HEAD"))
    private static void pvp_utils$beginLanguageLoad(ResourceManager resourceManager, List<String> filenames, boolean defaultRightToLeft, CallbackInfoReturnable<ClientLanguage> cir) {
        TranslationKeyGuard.beginLanguageLoad();
    }

    @WrapOperation(method = "appendFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/locale/Language;loadFromJson(Ljava/io/InputStream;Ljava/util/function/BiConsumer;)V"))
    private static void pvp_utils$recordTranslations(InputStream stream, BiConsumer<String, String> output, Operation<Void> original, @Local Resource resource) {
        PackResources pack = resource.source();
        original.call(stream, (BiConsumer<String, String>) (key, value) -> {
            TranslationKeyGuard.recordTranslation(key, value, pack == Minecraft.getInstance().getVanillaPackResources(), pack.location().source());
            output.accept(key, value);
        });
    }
}
