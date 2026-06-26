package com.pvp_utils;

import com.pvp_utils.client.KeyBindings;
import com.pvp_utils.client.KeyInputHandler;
import com.pvp_utils.client.AntiCheat;
import com.pvp_utils.client.Update;
import com.pvp_utils.client.TermsManager;
import com.pvp_utils.client.VersionWarningManager;
import com.pvp_utils.client.render.MainUI.MainUIBackgrounds;
import com.pvp_utils.client.render.MainUI.MainUIScreenManager;
import com.pvp_utils.client.modules.impl.Combat.ElytraAssistManager;
import com.pvp_utils.client.modules.impl.Misc.VictorySound;
import com.pvp_utils.client.modules.impl.Optimize.InputMethodManager;
import com.pvp_utils.client.modules.impl.Render.CustomCapeManager;
import com.pvp_utils.client.modules.impl.Render.NotificationOverlay;
import com.pvp_utils.client.modules.impl.Tool.AutoChestDepositManager;
import com.pvp_utils.client.modules.impl.Tool.BlockCountDisplayRenderer;
import com.pvp_utils.client.modules.impl.Tool.FakePlayerManager;
import com.pvp_utils.client.modules.impl.Tool.FishingRodAssistManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;

public class PVPUtilsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Config.load();
        AntiCheat.verifyEnvironment();
        VictorySound.init();
        MainUIBackgrounds.init();
        TermsManager.ensure();
        CustomCapeManager.init();
        KeyBindings.register();
        KeyInputHandler.register();
        MainUIScreenManager.init();
        Update.startAutoCheck();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            AutoChestDepositManager.tick(client);
            ElytraAssistManager.tick(client);
            InputMethodManager.tick(client);
            FishingRodAssistManager.tick(client);
            BlockCountDisplayRenderer.getInstance().tick(client);
            FakePlayerManager.tick(client);
            VersionWarningManager.tick(client);
            Update.tick(client);
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("PVPUtils")
                    .then(ClientCommandManager.literal("update")
                            .executes(context -> {
                                context.getSource().sendFeedback(Update.checkingMessage());
                                Update.startManualCheck();
                                return 1;
                            })
                            .then(ClientCommandManager.literal("qqgroup")
                                    .executes(context -> {
                                        Update.copyQqGroupNumber();
                                        return 1;
                                    })))
            );

        });
    }
}
