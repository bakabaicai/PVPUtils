package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public class LowHealthHandler {
    private static int lastStage = 0;

    public static void onHealthUpdate(Minecraft client, float health) {
        if (!Config.lowHealthNotify || client.player == null || !client.player.isAlive()) {
            reset(500);
            return;
        }

        int currentStage = 0;
        if (health <= 6.0f) {
            currentStage = 2;
        } else if (health <= 10.0f) {
            currentStage = 1;
        }

        if (currentStage != lastStage) {
            if (currentStage == 0) {
                reset(1000);
            } else if (currentStage == 1) {
                String msg = Config.isChinese ? "低血量警告，请及时补充血量" : "Low health warning, please replenish health";
                NotificationOverlay.getInstance().showPersistentSymbol(msg, 0xFFFF55, "\uE001", 0xFFFF55);
                playAnvil(client, 1);
            } else if (currentStage == 2) {
                String msg = Config.isChinese ? "极低生命值警告，请及时补充血量！！！" : "CRITICAL health warning, replenish health NOW!!!";
                NotificationOverlay.getInstance().showPersistentSymbol(msg, 0xFF5555, "\uE002", 0xFF5555);
                playAnvil(client, 3);
            }
            lastStage = currentStage;
        }
    }

    private static void reset(long delay) {
        if (lastStage != 0) {
            NotificationOverlay.getInstance().stopPersistent(delay);
            lastStage = 0;
        }
    }

    private static void playAnvil(Minecraft client, int count) {
        new Thread(() -> {
            try {
                for (int i = 0; i < count; i++) {
                    client.execute(() -> {
                        client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_LAND, 1.0f));
                    });
                    if (count > 1) Thread.sleep(160);
                }
            } catch (InterruptedException ignored) {}
        }).start();
    }

    public static void tick(Minecraft client) {}
}
