package com.pvp_utils.client.command.impl;

import com.pvp_utils.Config;
import com.pvp_utils.client.command.CommandManager;
import com.pvp_utils.client.modules.impl.Render.DynamicIsland.DynamicIslandNotifications;
import com.pvp_utils.client.util.ChatUtils;

import java.util.List;

public final class ClientCommandPrefixCommand implements DotCommand {
    @Override
    public List<String> names() {
        return List.of("clientcommand");
    }

    @Override
    public void execute(String args) {
        String prefix = args == null ? "" : args.trim();
        if (!CommandManager.setPrefix(prefix)) {
            String message = Config.isChinese
                    ? "前缀必须为单个符号。"
                    : "The prefix must be a single symbol.";
            ChatUtils.error(message);
            DynamicIslandNotifications.error(message);
            return;
        }
        String message = Config.isChinese
                ? "客户端指令前缀已更改为：" + prefix
                : "Client command prefix changed to: " + prefix;
        ChatUtils.success(message);
        DynamicIslandNotifications.success(message);
    }
}
