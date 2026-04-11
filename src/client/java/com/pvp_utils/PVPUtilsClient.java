package com.pvp_utils;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.pvp_utils.client.gui.NotificationOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PVPUtilsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Config.load();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("oldanimation")
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