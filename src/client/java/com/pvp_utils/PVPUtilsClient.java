package com.pvp_utils;

import com.pvp_utils.client.ModuleKeybindManager;
import com.pvp_utils.client.AntiCheat;
import com.pvp_utils.client.Update;
import com.pvp_utils.client.TermsManager;
import com.pvp_utils.client.VersionWarningManager;
import com.pvp_utils.client.command.CommandManager;
import com.pvp_utils.client.gui.clickgui.theme.ClickGuiThemeManager;
import com.pvp_utils.client.render.MainUI.MainUIBackgrounds;
import com.pvp_utils.client.render.MainUI.MainUIScreenManager;
import com.pvp_utils.client.modules.impl.Combat.ElytraAssistManager;
import com.pvp_utils.client.modules.impl.Combat.MainHandAssistManager;
import com.pvp_utils.client.modules.impl.Misc.VictorySound;
import com.pvp_utils.client.modules.impl.Optimize.InputMethodFix.InputMethodFix;
import com.pvp_utils.client.modules.impl.Render.CustomCapeManager;
import com.pvp_utils.client.modules.impl.Render.CustomEnchantmentGlint;
import com.pvp_utils.client.modules.impl.Tool.AutoChestDepositManager;
import com.pvp_utils.client.modules.impl.Tool.AutoGGManager;
import com.pvp_utils.client.modules.impl.Tool.BlockCountDisplayRenderer;
import com.pvp_utils.client.modules.impl.Tool.FakePlayerManager;
import com.pvp_utils.client.modules.impl.Tool.FishingRodAssistManager;
import com.pvp_utils.client.modules.impl.Tool.FoodInfo.FoodInfoHudOverlay;
import com.pvp_utils.client.modules.impl.Tool.Freelook.FreelookManager;
import com.pvp_utils.client.modules.impl.Tool.ServerAutoLoginManager;
import com.pvp_utils.client.modules.impl.Tool.Zoom.ZoomManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class PVPUtilsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Config.load();
        com.pvp_utils.client.plugin.PluginDirectory.ensureExists();
        com.pvp_utils.client.plugin.PluginManager.INSTANCE.initialize();
        ClickGuiThemeManager.applyConfig();
        AntiCheat.verifyEnvironment();
        VictorySound.init();
        MainUIBackgrounds.init();
        TermsManager.ensure();
        CustomCapeManager.init();
        ModuleKeybindManager.initialize();
        MainUIScreenManager.init();
        CommandManager.register();
        Update.startAutoCheck();
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(
                client -> {
                    com.pvp_utils.client.plugin.PluginManager.INSTANCE.close();
                    com.pvp_utils.client.render.skia.SkijaFrameRenderer.close();
                });

        ClientTickEvents.START_CLIENT_TICK.register(client ->
                com.pvp_utils.client.plugin.PluginManager.INSTANCE.prepareTick());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            com.pvp_utils.client.plugin.PluginManager.INSTANCE.tick(client);
            AutoChestDepositManager.tick(client);
            AutoGGManager.tick(client);
            ServerAutoLoginManager.tick(client);
            ElytraAssistManager.tick(client);
            MainHandAssistManager.tick(client);
            InputMethodFix.tick(client);
            FishingRodAssistManager.tick(client);
            FoodInfoHudOverlay.getInstance().tick(client);
            BlockCountDisplayRenderer.getInstance().tick(client);
            CustomEnchantmentGlint.tick(client);
            FakePlayerManager.tick(client);
            ZoomManager.tick(client);
            FreelookManager.tick(client);
            VersionWarningManager.tick(client);
            Update.tick(client);
            ModuleKeybindManager.tick(client);
        });

    }
}
