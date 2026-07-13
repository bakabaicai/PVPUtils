package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.pvp_utils.client.command.CommandManager;
import com.pvp_utils.client.modules.impl.Render.HudEditOverlay;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Shadow
    protected EditBox input;

    @Shadow
    CommandSuggestions commandSuggestions;

    @Inject(method = "<init>(Ljava/lang/String;Z)V", at = @At("RETURN"))
    private void constructorHook(String string, boolean bl, CallbackInfo ci) {
        if (!Config.chatHudEditQuickEnable) return;
        HudEditOverlay.getInstance().startOpen();
    }

    @Inject(method = "onClose", at = @At("HEAD"))
    private void onCloseHook(CallbackInfo ci) {
        if (!Config.chatHudEditQuickEnable) return;
        HudEditOverlay.getInstance().startClose();
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void removedHook(CallbackInfo ci) {
        if (!Config.chatHudEditQuickEnable) return;
        HudEditOverlay.getInstance().startClose();
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.chatHudEditQuickEnable) return;
        if (HudEditOverlay.getInstance().mouseScrolled(mouseX, mouseY, verticalAmount)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "onEdited", at = @At("TAIL"))
    private void showDotCommandSuggestions(String value, CallbackInfo ci) {
        if (input == null || commandSuggestions == null || value == null || !value.startsWith(".")) {
            return;
        }
        if (CommandManager.vanillaTabSuggestions(value).isEmpty()) {
            commandSuggestions.hide();
            commandSuggestions.setAllowSuggestions(false);
            return;
        }
        commandSuggestions.setAllowSuggestions(true);
        commandSuggestions.updateCommandInfo();
        commandSuggestions.showSuggestions(false);
    }
}
