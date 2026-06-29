package com.pvp_utils.mixin.client;

import com.pvp_utils.client.modules.impl.Optimize.InputMethodFix.InputMethodFix;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EditBox.class)
public abstract class InputMethodFixEditBoxMixin {
    @Inject(method = "setFocused", at = @At("TAIL"))
    private void pvp_utils$onFocusedChanged(boolean focused, CallbackInfo ci) {
        InputMethodFix.refreshForFocusedTextField(net.minecraft.client.Minecraft.getInstance());
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$onCharTyped(CharacterEvent characterEvent, CallbackInfoReturnable<Boolean> cir) {
        InputMethodFix.refreshForFocusedTextField(net.minecraft.client.Minecraft.getInstance());
    }
}
