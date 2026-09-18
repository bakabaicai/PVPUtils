package com.pvp_utils.client.command.impl;

import com.pvp_utils.Config;
import com.pvp_utils.client.command.CommandManager;
import com.pvp_utils.client.util.ChatUtils;

import java.util.List;

public final class HelpCommand implements DotCommand {
    @Override
    public List<String> names() {
        return List.of("help");
    }

    @Override
    public void execute(String args) {
        String prefix = CommandManager.getPrefix();
        boolean chinese = Config.isChinese;
        ChatUtils.send(chinese ? "可用指令：" : "Available commands:");
        send(prefix + "autologin <密码>", "为当前服务器开启自动登录，密码加密保存", "Enable auto login for this server; the password is stored encrypted");
        send(prefix + "autologin on", "重新开启当前服务器的自动登录", "Re-enable auto login for this server");
        send(prefix + "autologin off", "关闭当前服务器的自动登录并清除密码", "Disable auto login for this server and clear the password");
        send(prefix + "autologin delay <秒>", "设置进入服务器后的登录延迟(0-30)", "Set the login delay after joining a server (0-30)");
        send(prefix + "autologin", "查看当前服务器的自动登录状态", "Show auto login status for this server");
        send(prefix + "autogg <文本>", "修改自动GG发送的文本", "Change the text sent by Auto GG");
        send(prefix + "clientname <名称>", "修改客户端显示名称", "Change the client display name");
        send(prefix + "clientcommand <符号>", "修改客户端指令前缀符号", "Change the client command prefix");
        send(prefix + "update", "检查客户端更新", "Check for client updates");
    }

    private static void send(String usage, String descriptionZh, String descriptionEn) {
        ChatUtils.send(usage + " - " + (Config.isChinese ? descriptionZh : descriptionEn));
    }
}
