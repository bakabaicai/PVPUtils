package com.pvp_utils.client.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Render.DynamicIsland.DynamicIslandNotifications;
import com.pvp_utils.client.util.ChatUtils;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class ClientNameCommand {
    private ClientNameCommand() {
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> build() {
        return ClientCommandManager.literal("clientname")
                .executes(context -> {
                    resetEmptyClientName();
                    return 1;
                })
                .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                        .executes(context -> {
                            String clientName = StringArgumentType.getString(context, "name").trim();
                            if (clientName.isEmpty()) {
                                resetEmptyClientName();
                                return 1;
                            }
                            Config.clientName = clientName;
                            Config.save();
                            String message = Config.isChinese
                                    ? "客户端名称已被更改为：" + clientName
                                    : "Client name has been changed to: " + clientName;
                            ChatUtils.success(message);
                            DynamicIslandNotifications.success(message);
                            return 1;
                        }));
    }

    private static void resetEmptyClientName() {
        Config.clientName = "PVPUtils";
        Config.save();
        String message = Config.isChinese
                ? "Clientname字段不能为空！"
                : "Clientname field cannot be empty!";
        ChatUtils.error(message);
        DynamicIslandNotifications.error(message);
    }
}
