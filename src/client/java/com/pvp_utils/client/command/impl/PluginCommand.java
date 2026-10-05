package com.pvp_utils.client.command.impl;

import com.pvp_utils.client.plugin.PluginDirectory;
import com.pvp_utils.client.plugin.PluginManager;
import com.pvp_utils.client.util.ChatUtils;

import java.util.List;

public final class PluginCommand implements DotCommand {
    @Override
    public List<String> names() {
        return List.of("plugins");
    }

    @Override
    public void execute(String args) {
        String[] words = args.strip().split("\\s+", 2);
        PluginManager manager = PluginManager.INSTANCE;
        switch (words[0]) {
            case "refresh" -> {
                manager.refresh();
                ChatUtils.send("Plugins: " + manager.plugins().size());
            }
            case "folder" -> PluginDirectory.open();
            case "enable", "disable", "reload" -> {
                if (words.length < 2 || manager.plugins().stream().noneMatch(plugin -> plugin.id().equals(words[1]))) {
                    ChatUtils.error("Usage: .plugins " + words[0] + " <plugin-id>");
                    return;
                }
                if (words[0].equals("reload")) manager.reload(words[1]);
                else manager.setEnabled(words[1], words[0].equals("enable"));
            }
            default -> {
                for (var plugin : manager.plugins()) {
                    ChatUtils.send(plugin.id() + " " + plugin.version() + " "
                            + (plugin.enabled() ? "ON" : "OFF") + (plugin.error().isBlank() ? "" : " | " + plugin.error()));
                }
                ChatUtils.send(".plugins <refresh|folder|enable|disable|reload>");
            }
        }
    }

    @Override
    public List<String> suggestions(String args) {
        String[] words = args.split("\\s+", 2);
        if (words.length == 2) return PluginManager.INSTANCE.plugins().stream().map(PluginManager.PluginInfo::id)
                .filter(id -> id.startsWith(words[1])).toList();
        return List.of("refresh", "folder", "enable", "disable", "reload").stream()
                .filter(value -> value.startsWith(args)).toList();
    }
}
