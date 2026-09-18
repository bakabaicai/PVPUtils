package com.pvp_utils.client.command.impl;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Render.DynamicIsland.DynamicIslandNotifications;
import com.pvp_utils.client.util.ChatUtils;

import java.util.List;

public final class ClientNameCommand implements DotCommand {
    @Override
    public List<String> names() {
        return List.of("clientname");
    }

    @Override
    public boolean acceptsFreeText() {
        return true;
    }

    @Override
    public void execute(String args) {
        String clientName = args.trim();
        if (clientName.isEmpty()) {
            Config.clientName = "PVPUtils";
            Config.save();
            String message = Config.isChinese
                    ? "Clientname字段不能为空！"
                    : "Clientname field cannot be empty!";
            ChatUtils.error(message);
            DynamicIslandNotifications.error(message);
            return;
        }
        Config.clientName = clientName;
        Config.save();
        String message = Config.isChinese
                ? "客户端名称已被更改为：" + clientName
                : "Client name has been changed to: " + clientName;
        ChatUtils.success(message);
        DynamicIslandNotifications.success(message);
    }
}
