package com.pvp_utils;

import com.pvp_utils.client.ModuleKeybindManager;
import com.pvp_utils.client.communication.EncryptedChatManager;
import com.pvp_utils.client.modules.impl.Tool.tactical.TacticalPingManager;
import com.pvp_utils.client.modules.impl.Tool.tactical.TacticalPingRenderer;
import com.pvp_utils.client.AntiCheat;
import com.pvp_utils.client.Update;
import com.pvp_utils.client.TermsManager;
import com.pvp_utils.client.VersionWarningManager;
import com.pvp_utils.client.command.CommandManager;
import com.pvp_utils.client.combat.KillConfirmManager;
import com.pvp_utils.client.modules.impl.Combat.combo.ComboEffectsManager;
import com.pvp_utils.client.render.MainUI.MainUIBackgrounds;
import com.pvp_utils.client.render.MainUI.MainUISharedBackground;
import com.pvp_utils.client.render.MainUI.MainUIScreenManager;
import com.pvp_utils.client.startup.StartupProgress;
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
import com.pvp_utils.client.modules.impl.Tool.PseudoCreativeManager;
import com.pvp_utils.client.modules.impl.Tool.Zoom.ZoomManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

final class PVPUtilsClientRuntime {
    private static final KillConfirmManager.KillListener COMBO_EFFECTS_LISTENER =
            (target, name, distance, combo) -> ComboEffectsManager.onKill(target);

    private PVPUtilsClientRuntime() {
    }
    static void initialize() {
        StartupProgress.advance(StartupProgress.Stage.MODULES);
        AntiCheat.verifyEnvironment();
        Config.load();
        TacticalPingManager.initialize();
        ComboEffectsManager.initialize();
        KillConfirmManager.INSTANCE.subscribe(COMBO_EFFECTS_LISTENER);
        com.pvp_utils.client.plugin.PluginDirectory.ensureExists();
        com.pvp_utils.client.plugin.PluginManager.INSTANCE.initialize();
        VictorySound.init();
        MainUIBackgrounds.initAsync();
        if (Config.useMainUI && Config.mainUIBackgroundMode == Config.MainUIBackgroundMode.VIDEO) {
            MainUISharedBackground.preloadVideo();
        }
        TermsManager.ensure();
        CustomCapeManager.init();
        ModuleKeybindManager.initialize();
        MainUIScreenManager.init();
        CommandManager.register();
        Update.startAutoCheck();
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(
                client -> {
                    KillConfirmManager.INSTANCE.unsubscribe(COMBO_EFFECTS_LISTENER);
                    KillConfirmManager.INSTANCE.clear();
                    ComboEffectsManager.close();
                    TacticalPingManager.close();
                    TacticalPingRenderer.close();
                    EncryptedChatManager.close();
                    com.pvp_utils.client.modules.impl.Combat.HitSoundPlayer.close();
                    MainUISharedBackground.close();
                    PseudoCreativeManager.setEnabled(false);
                    com.pvp_utils.client.plugin.PluginManager.INSTANCE.close();
                    com.pvp_utils.client.render.chroma.ChromaFrameRenderer.close();
                });

        ClientTickEvents.START_CLIENT_TICK.register(client ->
                com.pvp_utils.client.plugin.PluginManager.INSTANCE.prepareTick());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            EncryptedChatManager.tick();
            TacticalPingManager.tick(client);
            ComboEffectsManager.tick(client);
            KillConfirmManager.INSTANCE.tick(client);
            com.pvp_utils.client.skin.McbiServerGate.tick(client);
            com.pvp_utils.client.plugin.PluginManager.INSTANCE.tick(client);
            AutoChestDepositManager.tick(client);
            AutoGGManager.tick(client);
            ServerAutoLoginManager.tick(client);
            PseudoCreativeManager.tick(client);
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
