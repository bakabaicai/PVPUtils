package com.pvp_utils.client.command;

import com.pvp_utils.Config;
import com.pvp_utils.client.command.impl.AutoGGCommand;
import com.pvp_utils.client.command.impl.ClientNameCommand;
import com.pvp_utils.client.command.impl.DotCommand;
import com.pvp_utils.client.command.impl.HelpCommand;
import com.pvp_utils.client.command.impl.IrcChatCommand;
import com.pvp_utils.client.command.impl.IrcCommand;
import com.pvp_utils.client.command.impl.UpdateCommand;
import com.pvp_utils.client.util.ChatUtils;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CommandManager {
    private static final List<DotCommand> COMMANDS = List.of(
            new HelpCommand(),
            new UpdateCommand(),
            new ClientNameCommand(),
            new AutoGGCommand(),
            new IrcCommand(),
            new IrcChatCommand()
    );

    private CommandManager() {
    }

    public static void register() {
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> !execute(message));
    }

    public static boolean execute(String rawMessage) {
        if (rawMessage == null || !rawMessage.startsWith(".")) {
            return false;
        }
        String withoutPrefix = rawMessage.trim().substring(1);
        DotCommand command = find(firstToken(withoutPrefix));
        if (command == null) {
            ChatUtils.warning(Config.isChinese
                    ? "未知指令，输入 .help 查看可用指令。"
                    : "Unknown command. Type .help for available commands.");
            return true;
        }
        command.execute(rest(withoutPrefix));
        return true;
    }

    public static List<String> vanillaTabSuggestions(String input) {
        if (input == null || !input.startsWith(".")) {
            return List.of();
        }
        if (!input.contains(" ")) {
            return rootNames();
        }
        String withoutPrefix = input.substring(1);
        DotCommand command = find(firstToken(withoutPrefix));
        if (command == null || command.acceptsFreeText()) {
            return List.of();
        }
        return command.suggestions(restPreserveTrailing(withoutPrefix));
    }

    private static List<String> rootNames() {
        ArrayList<String> names = new ArrayList<>();
        for (DotCommand command : COMMANDS) {
            for (String name : command.names()) {
                names.add("." + name);
            }
        }
        return names;
    }

    private static DotCommand find(String name) {
        String normalized = name == null ? "" : name.toLowerCase(Locale.ROOT);
        for (DotCommand command : COMMANDS) {
            for (String candidate : command.names()) {
                if (candidate.equalsIgnoreCase(normalized)) {
                    return command;
                }
            }
        }
        return null;
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

    private static String restPreserveTrailing(String input) {
        String value = input == null ? "" : input;
        int space = value.indexOf(' ');
        return space < 0 ? "" : value.substring(space + 1);
    }
}
