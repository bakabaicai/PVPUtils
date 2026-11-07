package com.pvp_utils.client.command.impl;

import com.pvp_utils.client.plugin.PluginDirectory;
import com.pvp_utils.client.plugin.PluginFeedback;
import com.pvp_utils.client.plugin.PluginManager;
import com.pvp_utils.client.command.CommandManager;
import com.pvp_utils.client.gui.clickgui.UiText;
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
        String prefix = CommandManager.getPrefix();
        switch (words[0]) {
            case "screen" -> {
                String[] target = words.length > 1 ? words[1].split("\\s+", 2) : new String[0];
                if (target.length != 2) {
                    ChatUtils.error(UiText.t("用法：", "Usage: ") + prefix + "plugins screen "
                            + UiText.t("<插件ID> <界面ID>", "<plugin-id> <screen-id>"));
                    return;
                }
                try {
                    manager.openScreen(target[0], target[1]);
                } catch (IllegalArgumentException | IllegalStateException e) {
                    ChatUtils.error(PluginFeedback.message(e.getMessage()));
                }
            }
            case "check" -> {
                boolean started = manager.checkAsync(results -> {
                    if (results.isEmpty()) ChatUtils.send(UiText.t("未找到插件", "No plugins found"));
                    else results.forEach(ChatUtils::send);
                });
                ChatUtils.send(started ? UiText.t("插件安全审计已开始", "Plugin security audit started")
                        : UiText.t("插件安全审计正在进行", "Plugin security audit is busy"));
            }
            case "refresh" -> {
                manager.refresh();
                ChatUtils.send(UiText.t("插件数量：", "Plugins: ") + manager.plugins().size());
            }
            case "folder" -> PluginDirectory.open();
            case "enable", "disable", "reload" -> {
                if (words.length < 2 || manager.plugins().stream().noneMatch(plugin -> plugin.id().equals(words[1]))) {
                    ChatUtils.error(UiText.t("用法：", "Usage: ") + prefix + "plugins " + words[0] + " "
                            + UiText.t("<插件ID>", "<plugin-id>"));
                    return;
                }
                if (words[0].equals("reload")) manager.reload(words[1]);
                else manager.setEnabled(words[1], words[0].equals("enable"));
            }
            default -> {
                for (var plugin : manager.plugins()) {
                    ChatUtils.send(plugin.id() + " " + plugin.version() + " "
                            + (plugin.auditing() ? UiText.t("审计中", "AUDITING")
                            : plugin.pendingApproval() ? UiText.t("待审批", "REVIEW")
                            : plugin.enabled() ? UiText.t("已启用", "ON") : UiText.t("已禁用", "OFF"))
                            + (plugin.error().isBlank() ? "" : " | " + plugin.error()));
                }
                ChatUtils.send(prefix + "plugins <check|screen|refresh|folder|enable|disable|reload>");
            }
        }
    }

    @Override
    public List<String> suggestions(String args) {
        String[] words = args.split("\\s+", 2);
        if (words.length == 2) return PluginManager.INSTANCE.plugins().stream().map(PluginManager.PluginInfo::id)
                .filter(id -> id.startsWith(words[1])).toList();
        return List.of("check", "screen", "refresh", "folder", "enable", "disable", "reload").stream()
                .filter(value -> value.startsWith(args)).toList();
    }
}
