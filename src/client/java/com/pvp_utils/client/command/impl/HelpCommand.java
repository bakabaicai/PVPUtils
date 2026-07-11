package com.pvp_utils.client.command.impl;

import com.pvp_utils.Config;
import com.pvp_utils.client.util.ChatUtils;

import java.util.List;

public final class HelpCommand implements DotCommand {
    @Override
    public List<String> names() {
        return List.of("help");
    }

    @Override
    public void execute(String args) {
        ChatUtils.send(Config.isChinese
                ? "可用指令：.update，.clientname <名称>，.autogg <文本>，.irc chat <文本>，.c <文本>，.chat <文本>"
                : "Available commands: .update, .clientname <name>, .autogg <text>, .irc chat <text>, .c <text>, .chat <text>");
    }
}

