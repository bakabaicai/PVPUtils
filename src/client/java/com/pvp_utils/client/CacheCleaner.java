package com.pvp_utils.client;

import com.pvp_utils.Config;
import com.pvp_utils.client.NeteaseMusic.NeteaseMusicCovers;
import net.minecraft.client.Minecraft;

public final class CacheCleaner {
    private static long lastClean = 0L;

    private CacheCleaner() {
    }

    public static void tick(Minecraft client) {
        if (!Config.cacheCleaner) {
            lastClean = 0L;
            return;
        }
        long now = System.currentTimeMillis();
        if (lastClean == 0L) {
            lastClean = now;
            return;
        }
        long interval = Math.max(1, Config.cacheCleanerInterval) * 60_000L;
        if (now - lastClean < interval) {
            return;
        }
        lastClean = now;
        try {
            NeteaseMusicCovers.clear();
        } catch (Throwable ignored) {
        }
    }
}
