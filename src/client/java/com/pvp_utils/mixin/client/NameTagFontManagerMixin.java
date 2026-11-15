package com.pvp_utils.mixin.client;

import com.pvp_utils.client.util.NameTagTextCache;
import net.minecraft.client.gui.font.FontManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FontManager.class)
public class NameTagFontManagerMixin {
    @Inject(method = {"apply", "updateOptions", "close"}, at = @At("HEAD"))
    private void pvp_utils$clearNameTagTextCache(CallbackInfo ci) {
        NameTagTextCache.clear();
    }
}
