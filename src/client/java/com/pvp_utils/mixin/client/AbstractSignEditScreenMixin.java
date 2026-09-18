package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.pvp_utils.client.TranslationKeyGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.level.block.entity.SignBlockEntity;

@Mixin(AbstractSignEditScreen.class)
public class AbstractSignEditScreenMixin {
    @Shadow @Final @Mutable private String[] messages;

    @Inject(
            method = "<init>(Lnet/minecraft/world/level/block/entity/SignBlockEntity;ZZLnet/minecraft/network/chat/Component;)V",
            at = @At("RETURN")
    )
    private void pvp_utils$replaceInitialSignText(SignBlockEntity sign, boolean frontText, boolean filtered, Component title, CallbackInfo ci) {
        if (!Config.modifyTranslationKeys || Minecraft.getInstance().getCurrentServer() == null) {
            return;
        }
        for (int line = 0; line < messages.length; line++) {
            messages[line] = TranslationKeyGuard.getSafeText(sign.getText(frontText).getMessage(line, filtered));
        }
    }
}
