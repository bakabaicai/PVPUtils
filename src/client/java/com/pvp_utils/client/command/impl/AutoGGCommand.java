package com.pvp_utils.client.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Render.DynamicIsland.DynamicIslandNotifications;
import com.pvp_utils.client.util.ChatUtils;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class AutoGGCommand {
    private AutoGGCommand() {
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> build() {
        return ClientCommandManager.literal("autogg")
                .executes(context -> {
                    sendEmptyError();
                    return 1;
                })
                .then(ClientCommandManager.argument("text", StringArgumentType.greedyString())
                        .executes(context -> {
                            String text = StringArgumentType.getString(context, "text").trim();
                            if (text.isEmpty()) {
                                sendEmptyError();
                                return 1;
                            }
                            Config.autoGGText = text;
                            Config.save();
                            String message = Config.isChinese
                                    ? "成功修改文本为：" + text
                                    : "Successfully changed text to: " + text;
                            ChatUtils.success(message);
                            DynamicIslandNotifications.success(message);
                            return 1;
                        }));
    }

    private static void sendEmptyError() {
        String message = Config.isChinese
                ? "Text字段不能为空！"
                : "Text field cannot be empty!";
        ChatUtils.error(message);
        DynamicIslandNotifications.error(message);
    }
}
