package com.pvp_utils.client.NeteaseMusic;

import com.pvp_utils.Config;
import com.pvp_utils.client.util.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class NeteaseMusicManager {
    private static final String PLATFORM_FIX_URL = "https://www.curseforge.com/minecraft/mc-mods/pvputils-fix";

    private NeteaseMusicManager() {
    }

    public static void open() {
        if (!NeteaseMusicLocalService.isSupportedPlatformReady()) {
            ChatUtils.error(Config.isChinese
                    ? "当前系统需要安装 PVPUtils-fix 补丁 Mod 后才能使用网易云播放器：" + PLATFORM_FIX_URL
                    : "This platform requires the PVPUtils-fix patch mod to use Netease Music: " + PLATFORM_FIX_URL);
            return;
        }
        NeteaseMusicLocalService.start();
        NeteaseMusicApi.restoreSession();
        Minecraft client = Minecraft.getInstance();
        Screen parent = client.screen;
        client.setScreen(new NeteaseMusicScreen(parent));
    }
}
