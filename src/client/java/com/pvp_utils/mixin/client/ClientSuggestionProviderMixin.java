package com.pvp_utils.mixin.client;

import com.pvp_utils.client.command.CommandManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;

@Mixin(ClientSuggestionProvider.class)
public class ClientSuggestionProviderMixin {
    @Inject(method = "getCustomTabSugggestions", at = @At("RETURN"), cancellable = true)
    private void addPvpUtilsDotCommands(CallbackInfoReturnable<Collection<String>> cir) {
        String input = currentChatInput();
        if (CommandManager.isClientCommandInput(input)) {
            cir.setReturnValue(CommandManager.vanillaTabSuggestions(input));
            return;
        }
        ArrayList<String> suggestions = new ArrayList<>(cir.getReturnValue());
        cir.setReturnValue(suggestions);
    }

    private static String currentChatInput() {
        if (!(Minecraft.getInstance().screen instanceof ChatScreen chatScreen)) {
            return null;
        }
        EditBox input = ((ChatScreenAccessor) chatScreen).pvp_utils$getInput();
        return input == null ? null : input.getValue();
    }
}
