package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public class LowHealthHandler {
    private static int lastStage = 0;
    private static String lastTitle = "";
    private static String lastMessage = "";
    private static String lastIcon = "";

    public static void onHealthUpdate(Minecraft client, float health) {
        boolean islandEnabled = Config.dynamicIsland && Config.dynamicIslandLowHealthWarning;
        boolean notifyEnabled = Config.lowHealthNotify || islandEnabled;
        if (!notifyEnabled || client.player == null || !client.player.isAlive()) {
            reset(500);
            return;
        }

        int currentStage = health <= 10.0f ? 1 : 0;

        if (currentStage != lastStage) {
            if (currentStage == 0) {
                reset(1000);
            } else {
                showStage(Config.isChinese ? "低血量警告，请及时补充血量" : "Low health warning, please replenish health", "\uF22F", 0xFFFF55);
                playAnvil(client, 1);
            }
            lastStage = currentStage;
        }
    }

    public static Snapshot snapshot() {
        if (!Config.dynamicIsland || !Config.dynamicIslandLowHealthWarning || lastStage == 0 || lastTitle.isEmpty()) {
            return Snapshot.EMPTY;
        }
        return new Snapshot(true, lastStage, lastIcon, lastTitle, lastMessage);
    }

    private static void showStage(String message, String icon, int color) {
        lastTitle = "Low Health Warning";
        lastMessage = message;
        lastIcon = icon;
        if (Config.lowHealthNotify && (!Config.dynamicIsland || !Config.dynamicIslandLowHealthWarning)) {
            NotificationOverlay.getInstance().showPersistentSymbol(message, color, icon, color);
        }
    }

    private static void reset(long delay) {
        if (lastStage != 0) {
            NotificationOverlay.getInstance().stopPersistent(delay);
            lastStage = 0;
            lastTitle = "";
            lastMessage = "";
            lastIcon = "";
        }
    }

    private static void playAnvil(Minecraft client, int count) {
        new Thread(() -> {
            try {
                for (int i = 0; i < count; i++) {
                    client.execute(() -> client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_LAND, 1.0f)));
                    if (count > 1) Thread.sleep(160);
                }
            } catch (InterruptedException ignored) {
            }
        }, "pvp-utils-low-health-sound").start();
    }

    public static void tick(Minecraft client) {
    }

    public record Snapshot(boolean visible, int stage, String icon, String title, String message) {
        public static final Snapshot EMPTY = new Snapshot(false, 0, "", "", "");
    }
}
