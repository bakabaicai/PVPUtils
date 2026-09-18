package com.pvp_utils.mixin.client;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.context.StringRange;
import com.pvp_utils.client.command.CommandManager;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {
    @Shadow
    @Final
    EditBox input;

    @Shadow
    private CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow
    public abstract void showSuggestions(boolean narrate);

    @Inject(method = "updateCommandInfo", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$clientCommandSuggestions(CallbackInfo ci) {
        String value = this.input == null ? "" : this.input.getValue();
        if (!CommandManager.isClientCommandInput(value)) {
            return;
        }
        List<String> list = CommandManager.vanillaTabSuggestions(value);
        if (list.isEmpty()) {
            return;
        }
        ci.cancel();
        int cursor = Math.min(this.input.getCursorPosition(), value.length());
        String before = value.substring(0, cursor);
        int start = before.lastIndexOf(' ') + 1;
        StringRange range = new StringRange(start, cursor);
        List<Suggestion> converted = list.stream()
                .map(text -> new Suggestion(range, text))
                .toList();
        this.pendingSuggestions = CompletableFuture.completedFuture(new Suggestions(range, converted));
        this.showSuggestions(false);
    }
}
