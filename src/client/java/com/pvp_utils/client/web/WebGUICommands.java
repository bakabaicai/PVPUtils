package com.pvp_utils.client.web;

import com.pvp_utils.Config;
import com.pvp_utils.client.Update;
import com.pvp_utils.client.modules.impl.Render.DynamicIsland.DynamicIslandNotifications;
import com.pvp_utils.client.util.ChatUtils;

import java.util.List;

final class WebGUICommands {
    private static final List<CommandItem> COMMANDS = List.of(
            new CommandItem("clientname", "Client Name", "客户端名称", "Change the text shown as the client name.", "修改灵动岛里显示的客户端名称。", "text", true, "PVPUtils"),
            new CommandItem("autogg", "Auto GG Text", "自动GG文本", "Change the text sent by Auto GG.", "修改自动GG获胜后发送的文字。", "text", true, "gg"),
            new CommandItem("update", "Check Update", "检查更新", "Run a manual update check.", "手动检查是否有可用更新。", "", false, ""),
            new CommandItem("qqgroup", "Copy QQ Group", "复制QQ群", "Copy the QQ group number through the update helper.", "通过更新工具复制QQ群号。", "", false, "")
    );

    private WebGUICommands() {
    }

    static String commandsJson() {
        StringBuilder json = new StringBuilder("{\"success\":true,\"result\":[");
        for (int i = 0; i < COMMANDS.size(); i++) {
            if (i > 0) json.append(',');
            CommandItem command = COMMANDS.get(i);
            json.append('{')
                    .append("\"id\":\"").append(escape(command.id)).append("\",")
                    .append("\"name\":\"").append(escape(command.name())).append("\",")
                    .append("\"desc\":\"").append(escape(command.desc())).append("\",")
                    .append("\"inputType\":\"").append(escape(command.inputType)).append("\",")
                    .append("\"requiresInput\":").append(command.requiresInput).append(',')
                    .append("\"placeholder\":\"").append(escape(command.placeholder)).append("\",")
                    .append("\"value\":\"").append(escape(command.currentValue())).append("\"")
                    .append('}');
        }
        return json.append("]}").toString();
    }

    static String executeJson(String id, String value) {
        String text = value == null ? "" : value.trim();
        switch (id) {
            case "clientname" -> {
                if (text.isEmpty()) {
                    Config.clientName = "PVPUtils";
                    Config.save();
                    String message = Config.isChinese ? "Clientname字段不能为空！" : "Clientname field cannot be empty!";
                    ChatUtils.error(message);
                    DynamicIslandNotifications.error(message);
                    return ok(message);
                }
                Config.clientName = text;
                Config.save();
                String message = Config.isChinese ? "客户端名称已被更改为：" + text : "Client name has been changed to: " + text;
                ChatUtils.success(message);
                DynamicIslandNotifications.success(message);
                return ok(message);
            }
            case "autogg" -> {
                if (text.isEmpty()) {
                    String message = Config.isChinese ? "Text字段不能为空！" : "Text field cannot be empty!";
                    ChatUtils.error(message);
                    DynamicIslandNotifications.error(message);
                    return ok(message);
                }
                Config.autoGGText = text;
                Config.save();
                String message = Config.isChinese ? "成功修改文本为：" + text : "Successfully changed text to: " + text;
                ChatUtils.success(message);
                DynamicIslandNotifications.success(message);
                return ok(message);
            }
            case "update" -> {
                Update.startManualCheck();
                String message = Config.isChinese ? "已开始手动检查更新。" : "Started manual update check.";
                ChatUtils.success(message);
                return ok(message);
            }
            case "qqgroup" -> {
                Update.copyQqGroupNumber();
                String message = Config.isChinese ? "已执行QQ群复制。" : "QQ group copy action executed.";
                ChatUtils.success(message);
                return ok(message);
            }
            default -> {
                return "{\"success\":false,\"reason\":\"Unknown command\"}";
            }
        }
    }

    private static String ok(String message) {
        return "{\"success\":true,\"message\":\"" + escape(message) + "\"}";
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> escaped.append(c);
            }
        }
        return escaped.toString();
    }

    private record CommandItem(String id, String en, String zh, String enDesc, String zhDesc, String inputType, boolean requiresInput, String placeholder) {
        String name() {
            return Config.isChinese ? zh : en;
        }

        String desc() {
            return Config.isChinese ? zhDesc : enDesc;
        }

        String currentValue() {
            return switch (id) {
                case "clientname" -> Config.clientName == null || Config.clientName.isBlank() ? "PVPUtils" : Config.clientName;
                case "autogg" -> Config.autoGGText == null || Config.autoGGText.isBlank() ? "gg" : Config.autoGGText;
                default -> "";
            };
        }
    }
}
