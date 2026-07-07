package com.pvp_utils.client.modules.impl.Tool;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Misc.VictorySound;
import com.pvp_utils.client.modules.impl.Render.DynamicIsland.DynamicIslandNotifications;

public class TitleDetector {
    private static final String[] KEYWORDS = {"victory", "胜利", "获胜", "恭喜"};
    private static final long DYNAMIC_ISLAND_COOLDOWN_MS = 1500L;
    private static long lastDynamicIslandVictoryMs;

    public static void check(String title, String subtitle) {
        if (matches(title) || matches(subtitle)) {
            showDynamicIslandVictory();
            VictoryScreenshot.tryCapture();
            VictorySound.play();
            AutoGGManager.onVictoryDetected();
        }
    }

    private static void showDynamicIslandVictory() {
        if (!Config.dynamicIsland) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastDynamicIslandVictoryMs < DYNAMIC_ISLAND_COOLDOWN_MS) {
            return;
        }
        lastDynamicIslandVictoryMs = now;
        DynamicIslandNotifications.victory();
    }

    private static boolean matches(String text) {
        if (text == null || text.isEmpty()) return false;
        String cleaned = text.replaceAll("§[0-9a-fk-orA-FK-OR]", "").toLowerCase();
        for (String keyword : KEYWORDS) {
            if (cleaned.contains(keyword)) return true;
        }
        return false;
    }
}
