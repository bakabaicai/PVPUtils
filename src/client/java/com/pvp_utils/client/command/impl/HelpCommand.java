package com.pvp_utils.client.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.pvp_utils.Config;
import com.pvp_utils.client.util.ChatUtils;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class HelpCommand {
    private HelpCommand() {
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> build() {
        return ClientCommandManager.literal("help")
                .executes(context -> {
                    ChatUtils.send(Config.isChinese
                            ? "可用命令：/PVPUtils update，/PVPUtils clientname <名称>，/PVPUtils autogg <文本>"
                            : "Available commands: /PVPUtils update, /PVPUtils clientname <name>, /PVPUtils autogg <text>");
                    return 1;
                });
    }
}
