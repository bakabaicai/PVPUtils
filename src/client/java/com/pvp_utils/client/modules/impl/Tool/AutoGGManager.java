package com.pvp_utils.client.modules.impl.Tool;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;

public final class AutoGGManager {
    private static final long COOLDOWN_MS = 5000L;
    private static long lastTriggerMs;
    private static int ticksRemaining = -1;

    private AutoGGManager() {
    }

    public static void onVictoryDetected() {
        if (!Config.autoGG) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastTriggerMs < COOLDOWN_MS) {
            return;
        }
        lastTriggerMs = now;
        ticksRemaining = Math.max(0, Config.autoGGDelayTicks);
    }

    public static void tick(Minecraft client) {
        if (ticksRemaining < 0) {
            return;
        }
        if (!Config.autoGG || client == null || client.player == null || client.level == null) {
            ticksRemaining = -1;
            return;
        }
        if (ticksRemaining-- > 0) {
            return;
        }

        String text = Config.autoGGText == null ? "" : Config.autoGGText.trim();
        if (!text.isEmpty()) {
            client.player.connection.sendChat(text);
        }
        ticksRemaining = -1;
    }
}
