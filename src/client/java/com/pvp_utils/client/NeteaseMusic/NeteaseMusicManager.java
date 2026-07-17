package com.pvp_utils.client.NeteaseMusic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class NeteaseMusicManager {
    private NeteaseMusicManager() {
    }

    public static void open() {
        NeteaseMusicLocalService.start();
        NeteaseMusicApi.restoreSession();
        Minecraft client = Minecraft.getInstance();
        Screen parent = client.screen;
        client.setScreen(new NeteaseMusicScreen(parent));
    }
}
