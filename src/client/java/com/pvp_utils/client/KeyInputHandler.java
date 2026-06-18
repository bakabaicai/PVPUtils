package com.pvp_utils.client;

import com.pvp_utils.client.gui.clickgui.NewSettingsScreen;
import com.pvp_utils.client.gui.clickgui.TermsScreen;
import com.pvp_utils.Config;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public class KeyInputHandler {

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (KeyBindings.openSettings.consumeClick()) {
                if (client.screen == null) {
                    Minecraft.getInstance().setScreen(Config.termsRead ? new NewSettingsScreen(null) : new TermsScreen(null));
                }
            }
        });
    }
}
