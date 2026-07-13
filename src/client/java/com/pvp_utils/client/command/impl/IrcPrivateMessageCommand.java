package com.pvp_utils.client.command.impl;

import com.pvp_utils.Config;
import com.pvp_utils.client.irc.IrcBridge;
import com.pvp_utils.client.irc.user.IrcUserManager;
import com.pvp_utils.client.util.ChatUtils;

import java.util.List;

public final class IrcPrivateMessageCommand implements DotCommand {
    @Override
    public List<String> names() {
        return List.of("msg", "w");
    }

    @Override
    public void execute(String args) {
        String target = firstToken(args);
        String message = rest(args);
        if (target.isBlank() || message.isBlank()) {
            ChatUtils.warning(Config.isChinese ? "用法：.msg <用户> <文本>" : "Usage: .msg <user> <text>");
            return;
        }
        IrcBridge.sendPrivateMessage(target, message);
    }

    @Override
    public List<String> suggestions(String args) {
        String value = args == null ? "" : args;
        if (value.contains(" ")) {
            return List.of();
        }
        return IrcUserManager.usernamesStartingWith(value).stream()
                .filter(name -> !name.equalsIgnoreCase(value))
                .toList();
    }

    private static String firstToken(String input) {
        String trimmed = input == null ? "" : input.trim();
        int space = trimmed.indexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(0, space);
    }

    private static String rest(String input) {
        String trimmed = input == null ? "" : input.trim();
        int space = trimmed.indexOf(' ');
        return space < 0 ? "" : trimmed.substring(space + 1).trim();
    }
}
