package com.pvp_utils;

import com.pvp_utils.client.KeyBindings;
import com.pvp_utils.client.KeyInputHandler;
import com.pvp_utils.client.AntiCheat;
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
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            AutoChestDepositManager.tick(client);
            ElytraAssistManager.tick(client);
            InputMethodManager.tick(client);
            FishingRodAssistManager.tick(client);
            BlockCountDisplayRenderer.getInstance().tick(client);
            FakePlayerManager.tick(client);
            VersionWarningManager.tick(client);
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("pvputils")
                    .then(ClientCommandManager.literal("auto")
                            .then(ClientCommandManager.argument("enabled", BoolArgumentType.bool())
                                    .executes(context -> {
                                        Config.autoMode = BoolArgumentType.getBool(context, "enabled");
                                        Config.save();
                                        context.getSource().sendFeedback(Component.literal("Auto mode: " + Config.autoMode));
                                        return 1;
                                    })))
                    .then(ClientCommandManager.literal("range")
                            .then(ClientCommandManager.argument("value", DoubleArgumentType.doubleArg(0, 10))
                                    .executes(context -> {
                                        Config.range = DoubleArgumentType.getDouble(context, "value");
                                        Config.save();
                                        context.getSource().sendFeedback(Component.literal("Range set to: " + Config.range));
                                        return 1;
                                    })))
            );

            dispatcher.register(ClientCommandManager.literal("test1")
                    .then(ClientCommandManager.argument("text", StringArgumentType.greedyString())
                            .executes(context -> {
                                String text = StringArgumentType.getString(context, "text");
                                NotificationOverlay.getInstance().show(text, 0xFFFFFF, new ItemStack(Items.GOLDEN_APPLE));
                                return 1;
                            }))
                    .executes(context -> {
                        NotificationOverlay.getInstance().show("Icon Test Message", 0xFFFFFF, new ItemStack(Items.COMPASS));
                        return 1;
                    })
            );

            dispatcher.register(ClientCommandManager.literal("test2")
                    .then(ClientCommandManager.literal("start")
                            .executes(context -> {
                                NotificationOverlay.getInstance().showPersistent("Text Message", 0xFF5555, new ItemStack(Items.NETHERITE_SWORD));
                                return 1;
                            }))
                    .then(ClientCommandManager.literal("stop")
                            .executes(context -> {
                                NotificationOverlay.getInstance().stopPersistent(1000);
                                return 1;
                            }))
            );

            dispatcher.register(ClientCommandManager.literal("test3")
                    .then(ClientCommandManager.argument("text", StringArgumentType.greedyString())
                            .executes(context -> {
                                String text = StringArgumentType.getString(context, "text");
                                NotificationOverlay.getInstance().show(text, 0xFFFFFF);
                                return 1;
                            }))
                    .executes(context -> {
                        NotificationOverlay.getInstance().show("Text Message", 0xFFFFFF);
                        return 1;
                    })
            );
        });
    }
}
