package com.pvp_utils.client.render.MainUI;

import com.pvp_utils.Config;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.network.chat.Component;

public final class MainUIScreenManager {
    private MainUIScreenManager() {}

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof TitleScreen titleScreen)) return;
            if (Config.useMainUI) {
                client.setScreen(new PVPUtilsMainUI(titleScreen));
                return;
            }
            Button button = Button.builder(Component.literal("P"), b -> {
                Config.useMainUI = true;
                Config.save();
                client.setScreen(new PVPUtilsMainUI(titleScreen, true));
            }).bounds(scaledWidth / 2 + 104, scaledHeight / 4 + 48 + 36 - 24, 20, 20).build();
            Screens.getButtons(titleScreen).add(button);
        });
    }
}
