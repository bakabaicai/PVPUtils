package com.pvp_utils.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.pvp_utils.client.TranslationKeyMarker;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.KeybindContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A keybind component received from a server would otherwise resolve client side
 * key mapping names (and therefore leak installed mods). Marked components render
 * the raw key name instead.
 */
@Mixin(KeybindContents.class)
public class KeybindContentsTranslationKeyMixin implements TranslationKeyMarker {

    @Unique
    private boolean pvp_utils$fromNetwork = false;

    @Shadow
    @Final
    private String name;

    @ModifyReturnValue(method = "getNestedComponent", at = @At("RETURN"))
    private Component pvp_utils$replaceNestedComponent(Component original) {
        return pvp_utils$fromNetwork ? Component.literal(name) : original;
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
