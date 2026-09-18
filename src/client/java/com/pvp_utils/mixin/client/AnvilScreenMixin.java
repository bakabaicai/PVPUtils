package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.pvp_utils.client.TranslationKeyGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AnvilScreen.class)
public class AnvilScreenMixin {
    @Redirect(
            method = "slotChanged",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;getString()Ljava/lang/String;")
    )
    private String pvp_utils$useSafeItemName(Component component) {
        return Config.modifyTranslationKeys && Minecraft.getInstance().getCurrentServer() != null
                ? TranslationKeyGuard.getSafeText(component)
                : component.getString();
    }
}
