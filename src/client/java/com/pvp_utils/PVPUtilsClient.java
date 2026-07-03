package com.pvp_utils;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
import com.pvp_utils.client.modules.impl.Optimize.InputMethodFix.InputMethodFix;
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
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

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
            InputMethodFix.tick(client);
            FishingRodAssistManager.tick(client);
            BlockCountDisplayRenderer.getInstance().tick(client);
            FakePlayerManager.tick(client);
            VersionWarningManager.tick(client);
            Update.tick(client);
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(createPvpUtilsCommand("PVPUtils"));
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> createPvpUtilsCommand(String name) {
        return ClientCommandManager.literal(name)
                .then(ClientCommandManager.literal("update")
                        .executes(context -> {
                            Update.startManualCheck();
                            return 1;
                        })
                        .then(ClientCommandManager.literal("qqgroup")
                                .executes(context -> {
                                    Update.copyQqGroupNumber();
                                    return 1;
                                })))
                .then(ClientCommandManager.literal("clientname")
                        .executes(context -> {
                            resetEmptyClientName(context.getSource());
                            return 1;
                        })
                        .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String clientName = StringArgumentType.getString(context, "name").trim();
                                    if (clientName.isEmpty()) {
                                        resetEmptyClientName(context.getSource());
                                        return 1;
                                    }
                                    Config.clientName = clientName;
                                    Config.save();
                                    String message = Config.isChinese
                                            ? "客户端名称已被更改为：" + clientName
                                            : "Client name has been changed to: " + clientName;
                                    context.getSource().sendFeedback(Component.literal(message).withStyle(ChatFormatting.GREEN));
                                    return 1;
                                })));
    }

    private static void resetEmptyClientName(FabricClientCommandSource source) {
        Config.clientName = "PVPUtils";
        Config.save();
        String message = Config.isChinese
                ? "Clientname字段不能为空！"
                : "Clientname field cannot be empty!";
        source.sendFeedback(Component.literal(message).withStyle(ChatFormatting.RED));
    }
}
