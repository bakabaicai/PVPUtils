package com.pvp_utils.client.command.impl;

import com.pvp_utils.Config;
import com.pvp_utils.client.irc.IrcBridge;
import com.pvp_utils.client.util.ChatUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

public final class IrcCommand implements DotCommand {
    private static final List<String> SUB_COMMANDS = List.of("status", "chat", "reconnect");
    private static final List<String> MODERATOR_SUB_COMMANDS = List.of("ban", "kick", "mute", "unmute", "unban");

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
            case "chat" -> IrcBridge.sendChat(subArgs);
            case "reconnect" -> {
                if ("CONNECTED".equalsIgnoreCase(IrcBridge.status())) {
                    ChatUtils.error(Config.isChinese ? "您已经连接到IRC服务器了！" : "You are already connected to the IRC server!");
                    return;
                }
                if (Config.ircToken == null || Config.ircToken.isBlank()) {
                    ChatUtils.warning(Config.isChinese
                            ? "当前未连接至IRC服务器，IRC相关功能将无法使用，您可以稍后重新启动游戏并尝试登录IRC服务器。"
                            : "You are not connected to the IRC server. IRC features are unavailable. You can restart the game later and try logging in to the IRC server.");
                    return;
                }
                Config.ircEnabled = true;
                Config.save();
                IrcBridge.reconnect();
                ChatUtils.success(Config.isChinese ? "正在尝试重新连接IRC服务器。" : "Trying to reconnect to the IRC server.");
            }
            case "kick" -> executeModeratorCommand("KICK", subArgs, false);
            case "ban" -> executeModeratorCommand("BAN", subArgs, true);
            case "mute" -> executeModeratorCommand("MUTE", subArgs, true);
            case "unmute" -> executeModeratorCommand("UNMUTE", subArgs, false);
            case "unban" -> executeModeratorCommand("UNBAN", subArgs, false);
            case "cape", "skin" -> executeCosmeticCommand(subCommand, subArgs);
            default -> ChatUtils.warning(Config.isChinese
                    ? "用法：.irc chat <文本>"
                    : "Usage: .irc chat <text>");
        }
    }

    @Override
    public List<String> suggestions(String args) {
        String value = args == null ? "" : args;
        String trimmed = value.trim();
        if (value.contains(" ")) {
            return List.of();
        }
        String prefix = trimmed.toLowerCase(Locale.ROOT);
        return visibleSubCommands().stream()
                .filter(command -> command.startsWith(prefix) && !command.equals(prefix))
                .toList();
    }

    private static List<String> visibleSubCommands() {
        java.util.stream.Stream<String> commands = SUB_COMMANDS.stream();
        if (IrcBridge.hasPrivilege("cape")) {
            commands = java.util.stream.Stream.concat(commands, java.util.stream.Stream.of("cape"));
        }
        if (IrcBridge.hasPrivilege("skin")) {
            commands = java.util.stream.Stream.concat(commands, java.util.stream.Stream.of("skin"));
        }
        if (canModerate()) {
            commands = java.util.stream.Stream.concat(commands, MODERATOR_SUB_COMMANDS.stream());
        }
        return commands.toList();
    }

    private static void executeCosmeticCommand(String kind, String path) {
        if (!IrcBridge.hasPrivilege(kind)) {
            ChatUtils.error(Config.isChinese
                    ? "当前账户没有对应的材质特权。"
                    : "Your account does not have the required cosmetic privilege.");
            return;
        }
        if (path == null || path.isBlank()) {
            ChatUtils.error(Config.isChinese
                    ? "用法：.irc " + kind + " <PNG文件完整路径>"
                    : "Usage: .irc " + kind + " <full PNG path>");
            return;
        }
        IrcBridge.uploadCosmetic(kind, path);
    }

    private static void executeModeratorCommand(String commandType, String args, boolean requiresDuration) {
        if (!canModerate()) {
            ChatUtils.warning(Config.isChinese
                    ? "用法：.irc chat <文本>"
                    : "Usage: .irc chat <text>");
            return;
        }
        String target = firstToken(args);
        String duration = firstToken(rest(args));
        String reason = rest(rest(args));
        if (target.isBlank() || (requiresDuration && duration.isBlank())) {
            ChatUtils.error(requiresDuration
                    ? (Config.isChinese ? "用法：.irc " + commandType.toLowerCase(Locale.ROOT) + " <用户> <时长> [理由]" : "Usage: .irc " + commandType.toLowerCase(Locale.ROOT) + " <user> <duration> [reason]")
                    : (Config.isChinese ? "用法：.irc " + commandType.toLowerCase(Locale.ROOT) + " <用户>" : "Usage: .irc " + commandType.toLowerCase(Locale.ROOT) + " <user>"));
            return;
        }
        sendModerationCommand(commandType, target, duration, reason);
    }

    private static boolean canModerate() {
        try {
            Class<?> userManager = Class.forName("com.pvp_utils.client.irc.user.IrcUserManager");
            Object currentUser = userManager.getMethod("currentUser").invoke(null);
            if (currentUser == null) {
                return false;
            }
            Method hasGroup = currentUser.getClass().getMethod("hasGroup", String.class);
            Object adminGroup = hasGroup.invoke(currentUser, "ADMIN");
            Object developerGroup = hasGroup.invoke(currentUser, "DEVELOPER");
            if (Boolean.TRUE.equals(adminGroup) || Boolean.TRUE.equals(developerGroup)) {
                return true;
            }
            Object role = currentUser.getClass().getMethod("role").invoke(currentUser);
            String value = role == null ? "" : role.toString();
            return "DEVELOPER".equalsIgnoreCase(value) || "ADMIN".equalsIgnoreCase(value);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static void sendModerationCommand(String commandType, String target, String duration, String reason) {
        try {
            Class<?> clientClass = Class.forName("com.pvp_utils.client.irc.network.PVPUtilsIrcClient");
            Object instance = clientClass.getMethod("getInstance").invoke(null);
            Method method = clientClass.getMethod("sendModerationCommand", String.class, String.class, String.class, String.class);
            method.invoke(instance, commandType, target, duration, reason);
        } catch (ReflectiveOperationException e) {
            IrcBridge.missingCore();
        }
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


