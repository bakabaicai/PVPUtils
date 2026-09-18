package com.pvp_utils.client.command.impl;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Tool.ServerAutoLoginManager;
import com.pvp_utils.client.util.ChatUtils;
import com.pvp_utils.client.util.PasswordCipher;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AutoLoginCommand implements DotCommand {
    @Override
    public List<String> names() {
        return List.of("autologin");
    }

    @Override
    public void execute(String args) {
        String address = ServerAutoLoginManager.currentAddress(net.minecraft.client.Minecraft.getInstance());
        if (address == null) {
            ChatUtils.error(Config.isChinese
                    ? "请先进入一个服务器后再使用该指令。"
                    : "Join a server before using this command.");
            return;
        }
        String value = args == null ? "" : args.trim();
        ServerAutoLoginManager.Rule rule = ServerAutoLoginManager.ruleFor(address);
        if (value.isEmpty()) {
            sendStatus(address, rule);
            return;
        }
        String keyword = value.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        switch (keyword) {
            case "off" -> {
                boolean removed = ServerAutoLoginManager.removeRule(address);
                ChatUtils.success(Config.isChinese
                        ? removed ? "已关闭当前服务器的自动登录并清除已存密码。" : "当前服务器没有自动登录配置。"
                        : removed ? "Auto login disabled for this server and the saved password was cleared."
                        : "No auto login config for this server.");
            }
            case "delay" -> {
                if (rule == null || !rule.enabled) {
                    ChatUtils.warning(Config.isChinese
                            ? "当前服务器尚未启用自动登录，请先使用 .autologin <密码> 设置。"
                            : "Auto login is not enabled for this server. Use .autologin <password> first.");
                    return;
                }
                String delayArg = value.length() > keyword.length() ? value.substring(keyword.length()).trim() : "";
                int delaySeconds;
                try {
                    delaySeconds = Integer.parseInt(delayArg);
                } catch (NumberFormatException e) {
                    ChatUtils.error(Config.isChinese ? "延迟必须是 0-30 的整数秒。" : "Delay must be an integer between 0 and 30 seconds.");
                    return;
                }
                if (delaySeconds < 0 || delaySeconds > 30) {
                    ChatUtils.error(Config.isChinese ? "延迟必须是 0-30 的整数秒。" : "Delay must be an integer between 0 and 30 seconds.");
                    return;
                }
                Map<String, ServerAutoLoginManager.Rule> values = ServerAutoLoginManager.rules();
                values.get(address).delay = delaySeconds;
                ServerAutoLoginManager.saveRules(values);
                ChatUtils.success(Config.isChinese
                        ? "登录延迟已设为 " + delaySeconds + " 秒。"
                        : "Login delay set to " + delaySeconds + " second(s).");
            }
            case "on" -> {
                if (rule != null && !rule.password.isBlank()) {
                    Map<String, ServerAutoLoginManager.Rule> values = ServerAutoLoginManager.rules();
                    values.get(address).enabled = true;
                    ServerAutoLoginManager.saveRules(values);
                    ChatUtils.success(Config.isChinese
                            ? "已开启当前服务器的自动登录。"
                            : "Auto login enabled for this server.");
                } else {
                    ChatUtils.warning(Config.isChinese
                            ? "当前服务器还没有保存密码，请使用 .autologin <密码> 设置。"
                            : "No saved password for this server. Use .autologin <password> first.");
                }
            }
            default -> {
                String password = value;
                if (PasswordCipher.isEncrypted(password) || password.startsWith("********")) {
                    ChatUtils.warning(Config.isChinese
                            ? "请输入真实的登录密码，而不是占位内容。"
                            : "Please enter the real login password, not a placeholder.");
                    return;
                }
                int delay = rule == null ? 1 : Math.max(0, rule.delay);
                ServerAutoLoginManager.setRule(address, password, delay);
                ChatUtils.success(Config.isChinese
                        ? "已为当前服务器开启自动登录，密码已加密保存（延迟 " + delay + " 秒）。"
                        : "Auto login enabled for this server; the password is stored encrypted (delay " + delay + "s).");
            }
        }
    }

    @Override
    public List<String> suggestions(String args) {
        String value = args == null ? "" : args;
        String trimmed = value.trim();
        if (trimmed.contains(" ")) {
            return List.of();
        }
        String prefix = trimmed.toLowerCase(Locale.ROOT);
        return List.of("off", "delay", "on").stream()
                .filter(option -> option.startsWith(prefix) && !option.equals(prefix))
                .toList();
    }

    private static void sendStatus(String address, ServerAutoLoginManager.Rule rule) {
        String status;
        if (rule == null) {
            status = Config.isChinese ? "未配置" : "not configured";
        } else {
            String state = rule.enabled
                    ? (Config.isChinese ? "已开启" : "enabled")
                    : (Config.isChinese ? "已关闭" : "disabled");
            status = state + (rule.password.isBlank()
                    ? (Config.isChinese ? "，未存密码" : ", no saved password")
                    : (Config.isChinese ? "，密码已加密保存" : ", password stored encrypted"))
                    + (Config.isChinese ? "，延迟 " + rule.delay + " 秒" : ", delay " + rule.delay + "s");
        }
        ChatUtils.send(Config.isChinese
                ? "服务器 " + address + " 自动登录状态：" + status
                : "Auto login for " + address + ": " + status);
    }
}
