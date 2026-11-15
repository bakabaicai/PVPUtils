package com.pvp_utils.client;

import com.pvp_utils.Config;

public class ResetManager {
    public static void resetAll() {
        Config.resetToDefaults();
        Config.applyGameLanguageDefault();
        ModuleKeybindManager.clearAll(false);
        Config.save();
    }

    public static void resetAnimPage() {
        Config.offsetX = 0.0f;
        Config.offsetY = 0.0f;
        Config.offsetZ = 0.0f;
        Config.animSpeed = 1.0f;
        Config.range = 3.0;
        Config.swordBlock = false;
        Config.autoMode = false;
        Config.noAttackCooldownAnimation = false;
        Config.animationMode = Config.AnimMode.MODE_1_7;
        Config.save();
    }
}
