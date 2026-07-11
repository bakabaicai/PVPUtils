package com.pvp_utils.client.command.impl;

import com.pvp_utils.Config;
import com.pvp_utils.client.irc.IrcBridge;
import com.pvp_utils.client.util.ChatUtils;

import java.util.List;
import java.util.Locale;

public final class IrcCommand implements DotCommand {
    private static final List<String> SUB_COMMANDS = List.of("status", "disconnect", "login", "chat", "autoconnect");

    @Override
    public List<String> names() {
        return List.of("irc");
    }

    @Override
    public void execute(String args) {
        if (!IrcBridge.available()) {
            IrcBridge.missingCore();
            return;
        }
        String subCommand = firstToken(args).toLowerCase(Locale.ROOT);
        String subArgs = rest(args);
        switch (subCommand) {
            case "", "status" -> ChatUtils.send("IRC: " + IrcBridge.status());
            case "disconnect" -> {
                Config.ircEnabled = false;
                Config.ircAutoConnect = false;
                Config.save();
                IrcBridge.disconnect();
                ChatUtils.success(Config.isChinese ? "IRC已断开。" : "IRC disconnected.");
            }
            case "login" -> {
                String username = firstToken(subArgs);
                String password = rest(subArgs);
                if (username.isBlank() || password.isBlank()) {
                    ChatUtils.error(Config.isChinese ? "用法：.irc login <用户名> <密码>" : "Usage: .irc login <username> <password>");
                    return;
                }
                IrcBridge.login(username, password);
            }
            case "chat" -> IrcBridge.sendChat(subArgs);
            case "autoconnect" -> setAutoConnect(subArgs);
            default -> ChatUtils.warning(Config.isChinese
                    ? "用法：.irc login <用户名> <密码> / .irc chat <文本>"
                    : "Usage: .irc login <username> <password> / .irc chat <text>");
        }
    }

    @Override
    public List<String> suggestions(String args) {
        String value = args == null ? "" : args;
        String trimmed = value.trim();
        if (trimmed.startsWith("autoconnect ")) {
            String option = rest(value).toLowerCase(Locale.ROOT);
            return List.of("on", "off").stream()
                    .filter(candidate -> candidate.startsWith(option))
                    .toList();
        }
        if (trimmed.contains(" ")) {
            return List.of();
        }
        String prefix = trimmed.toLowerCase(Locale.ROOT);
        return SUB_COMMANDS.stream()
                .filter(command -> command.startsWith(prefix))
                .toList();
    }

    private static void setAutoConnect(String args) {
        String value = firstToken(args).toLowerCase(Locale.ROOT);
        if (!value.equals("on") && !value.equals("off")) {
            ChatUtils.error(Config.isChinese ? "用法：.irc autoconnect <on/off>" : "Usage: .irc autoconnect <on/off>");
            return;
        }
        Config.ircAutoConnect = value.equals("on");
        if (Config.ircAutoConnect) {
            Config.ircEnabled = true;
        }
        Config.save();
        ChatUtils.success(Config.isChinese
                ? "IRC自动连接已" + (Config.ircAutoConnect ? "开启。" : "关闭。")
                : "IRC auto connect is " + (Config.ircAutoConnect ? "enabled." : "disabled."));
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


