package com.pvp_utils.client;

import com.pvp_utils.Config;

public class ResetManager {
    public static void resetAll() {
        Config.offsetX = 0.0f;
        Config.offsetY = 0.0f;
        Config.offsetZ = 0.0f;
        Config.animSpeed = 1.0f;
        Config.range = 3.0;
        Config.swordBlock = false;
        Config.useSwing = false;
        Config.autoMode = false;
        Config.noSneakAnimation = false;
        Config.sneakDropScale = 0.5f;
        Config.sneakAnimationSpeed = 1.0f;
        Config.autoScreenshot = false;
        Config.hitMarker = false;
        Config.hitSound = false;
        Config.lowHealthNotify = false;
        Config.targetHud = false;
        Config.diggingStatus = false;
        Config.fallDamagePredict = false;
        Config.victorySound = false;
        Config.gammaOverride = false;
        Config.gammaValue = 15.0;
        Config.autoSprint = false;
        Config.autoChestDeposit = false;
        Config.autoChestDepositResourcesOnly = true;
        Config.autoChestDepositDepositDelay = 4;
        Config.autoChestDepositCloseDelay = 4;
        Config.targetHudX = -300f;
        Config.targetHudY = -100f;
        Config.targetHudZ = 0f;
        Config.keystrokes = false;
        Config.keystrokesX = -170f;
        Config.keystrokesY = 70f;
        Config.keystrokesScale = 1.0f;
        Config.notificationX = Float.NaN;
        Config.notificationY = Float.NaN;
        Config.disableImeInGame = false;
        Config.hideSignText = false;
        Config.hideEnchantTableBook = false;
        Config.hideFireOverlay = false;
        Config.hideHurtShake = false;
        Config.hitSoundType = Config.HitSoundType.NETHERITE;
        Config.hitSoundCondition = Config.HitSoundCondition.BOTH;
        Config.animationMode = Config.AnimMode.MODE_1_7;
        Config.save();
    }

    public static void resetAnimPage() {
        Config.offsetX = 0.0f;
        Config.offsetY = 0.0f;
        Config.offsetZ = 0.0f;
        Config.animSpeed = 1.0f;
        Config.range = 3.0;
        Config.swordBlock = false;
        Config.useSwing = false;
        Config.autoMode = false;
        Config.animationMode = Config.AnimMode.MODE_1_7;
        Config.save();
    }
}
