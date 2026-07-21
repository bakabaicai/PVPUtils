package com.pvp_utils.client.command.impl;

import com.pvp_utils.Config;
import com.pvp_utils.client.command.CommandManager;
import com.pvp_utils.client.util.ChatUtils;

import java.util.List;

public final class HelpCommand implements DotCommand {
    @Override
    public List<String> names() {
        return List.of("help");
    }

    @Override
    public void execute(String args) {
        String prefix = CommandManager.getPrefix();
        ChatUtils.send(Config.isChinese
                ? "可用指令：" + prefix + "update，" + prefix + "clientcommand <符号>，" + prefix + "clientname <名称>，" + prefix + "autogg <文本>，" + prefix + "irc chat <文本>，" + prefix + "c <文本>，" + prefix + "chat <文本>，" + prefix + "msg <用户> <文本>，" + prefix + "w <用户> <文本>"
                : "Available commands: " + prefix + "update, " + prefix + "clientcommand <symbol>, " + prefix + "clientname <name>, " + prefix + "autogg <text>, " + prefix + "irc chat <text>, " + prefix + "c <text>, " + prefix + "chat <text>, " + prefix + "msg <user> <text>, " + prefix + "w <user> <text>");
    }
}
