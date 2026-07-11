package com.pvp_utils.client.command.impl;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Render.DynamicIsland.DynamicIslandNotifications;
import com.pvp_utils.client.util.ChatUtils;

import java.util.List;

public final class AutoGGCommand implements DotCommand {
    @Override
    public List<String> names() {
        return List.of("autogg");
    }

    @Override
    public boolean acceptsFreeText() {
        return true;
    }

    @Override
    public void execute(String args) {
        String text = args.trim();
        if (text.isEmpty()) {
            String message = Config.isChinese
                    ? "Text字段不能为空！"
                    : "Text field cannot be empty!";
            ChatUtils.error(message);
            DynamicIslandNotifications.error(message);
            return;
        }
        Config.autoGGText = text;
        Config.save();
        String message = Config.isChinese
                ? "成功修改文本为：" + text
                : "Successfully changed text to: " + text;
        ChatUtils.success(message);
        DynamicIslandNotifications.success(message);
    }
}
