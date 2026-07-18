package com.pvp_utils.client.command.impl;

import com.pvp_utils.Config;
import com.pvp_utils.client.util.ChatUtils;

import java.util.List;
import java.util.Locale;

public final class VersionWarningCommand implements DotCommand {
    @Override
    public List<String> names() {
        return List.of("versionwarning");
    }

    @Override
    public void execute(String args) {
        String value = args.trim().toLowerCase(Locale.ROOT);
        if ("off".equals(value)) {
            Config.versionWarningDisabled = true;
            Config.save();
            ChatUtils.success(Config.isChinese ? "版本提示已关闭。" : "Version warning disabled.");
            return;
        }
        if ("on".equals(value)) {
            Config.versionWarningDisabled = false;
            Config.save();
            ChatUtils.success(Config.isChinese ? "版本提示已开启。" : "Version warning enabled.");
            return;
        }
        ChatUtils.warning(Config.isChinese ? "用法：.versionwarning <on/off>" : "Usage: .versionwarning <on/off>");
    }

    @Override
    public List<String> suggestions(String args) {
        String trimmed = args.trim().toLowerCase(Locale.ROOT);
        if (args.contains(" ") || trimmed.isBlank()) {
            return List.of();
        }
        return List.of("on", "off").stream()
                .filter(option -> option.startsWith(trimmed) && !option.equals(trimmed))
                .toList();
    }
}
