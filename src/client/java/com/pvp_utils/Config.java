package com.pvp_utils;

import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.file.Path;
import java.util.Properties;

public class Config {
    public static boolean autoMode = false;
    public static boolean swordBlock = false;
    public static boolean useSwing = false;
    public static boolean noSneakAnimation = false;
    public static boolean isChinese = true;
    public static boolean autoScreenshot = false;
    public static boolean hitMarker = false;
    public static boolean hitSound = true;
    public static boolean damageRecord = false;
    public static boolean lowHealthNotify = false;
    public static boolean targetHud = false;
    public static boolean fallDamagePredict = false;
    public static boolean victorySound = false;
    public static boolean gammaOverride = false;
    public static boolean autoSprint = false;
    public static boolean autoChestDeposit = false;
    public static boolean autoChestDepositBlockMovement = true;
    public static boolean keystrokes = false;
    public static boolean disableImeInGame = false;
    public static HitSoundType hitSoundType = HitSoundType.NETHERITE;
    public static HitSoundCondition hitSoundCondition = HitSoundCondition.BOTH;
    public static double range = 3.0;
    public static float animSpeed = 1.0f;
    public static float sneakDropScale = 0.5f;
    public static float sneakAnimationSpeed = 1.0f;
    public static float targetHudX = -300f;
    public static float targetHudY = -100f;
    public static float targetHudZ = 0f;
    public static float keystrokesX = -170f;
    public static float keystrokesY = 70f;
    public static float keystrokesScale = 1.0f;
    public static float offsetX = 0f;
    public static float offsetY = 0f;
    public static float offsetZ = 0f;
    public static double gammaValue = 15.0;
    public static int autoChestDepositOpenDelay = 4;
    public static int autoChestDepositTransferDelay = 4;
    public static int autoChestDepositCloseDelay = 4;

    public enum AnimMode { MODE_1_7, MODE_PUSH, MODE_1_7_PLUS, MODE_NEW }
    public enum HitSoundType { NETHERITE, EXPERIENCE }
    public enum HitSoundCondition { BOTH, MELEE, RANGED }

    public static AnimMode animationMode = AnimMode.MODE_1_7;

    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("pvp_utils.properties");

    public static void load() {
        if (!CONFIG_FILE.toFile().exists()) {
            save();
            return;
        }
        try (InputStream is = new FileInputStream(CONFIG_FILE.toFile())) {
            Properties prop = new Properties();
            prop.load(is);
            autoMode = Boolean.parseBoolean(prop.getProperty("autoMode", "false"));
            swordBlock = Boolean.parseBoolean(prop.getProperty("swordBlock", "false"));
            useSwing = Boolean.parseBoolean(prop.getProperty("useSwing", "false"));
            noSneakAnimation = Boolean.parseBoolean(prop.getProperty("noSneakAnimation", "false"));
            isChinese = Boolean.parseBoolean(prop.getProperty("isChinese", "true"));
            autoScreenshot = Boolean.parseBoolean(prop.getProperty("autoScreenshot", "false"));
            hitMarker = Boolean.parseBoolean(prop.getProperty("hitMarker", "false"));
            hitSound = Boolean.parseBoolean(prop.getProperty("hitSound", "true"));
            damageRecord = Boolean.parseBoolean(prop.getProperty("damageRecord", "false"));
            lowHealthNotify = Boolean.parseBoolean(prop.getProperty("lowHealthNotify", "false"));
            targetHud = Boolean.parseBoolean(prop.getProperty("targetHud", "false"));
            fallDamagePredict = Boolean.parseBoolean(prop.getProperty("fallDamagePredict", "false"));
            victorySound = Boolean.parseBoolean(prop.getProperty("victorySound", "false"));
            gammaOverride = Boolean.parseBoolean(prop.getProperty("gammaOverride", "false"));
            autoSprint = Boolean.parseBoolean(prop.getProperty("autoSprint", "false"));
            autoChestDeposit = Boolean.parseBoolean(prop.getProperty("autoChestDeposit", "false"));
            autoChestDepositBlockMovement = Boolean.parseBoolean(prop.getProperty("autoChestDepositBlockMovement", "true"));
            keystrokes = Boolean.parseBoolean(prop.getProperty("keystrokes", "false"));
            disableImeInGame = Boolean.parseBoolean(prop.getProperty("disableImeInGame", "false"));
            hitSoundType = HitSoundType.valueOf(prop.getProperty("hitSoundType", "NETHERITE"));
            hitSoundCondition = HitSoundCondition.valueOf(prop.getProperty("hitSoundCondition", "BOTH"));
            range = Double.parseDouble(prop.getProperty("range", "3.0"));
            animSpeed = Float.parseFloat(prop.getProperty("animSpeed", "1.0"));
            sneakDropScale = Float.parseFloat(prop.getProperty("sneakDropScale", "0.5"));
            sneakAnimationSpeed = Float.parseFloat(prop.getProperty("sneakAnimationSpeed", "1.0"));
            animationMode = AnimMode.valueOf(prop.getProperty("animationMode", "MODE_1_7"));
            offsetX = Float.parseFloat(prop.getProperty("offsetX", "0"));
            offsetY = Float.parseFloat(prop.getProperty("offsetY", "0"));
            offsetZ = Float.parseFloat(prop.getProperty("offsetZ", "0"));
            targetHudX = Float.parseFloat(prop.getProperty("targetHudX", "-300"));
            targetHudY = Float.parseFloat(prop.getProperty("targetHudY", "-100"));
            targetHudZ = Float.parseFloat(prop.getProperty("targetHudZ", "0"));
            keystrokesX = Float.parseFloat(prop.getProperty("keystrokesX", "-170"));
            keystrokesY = Float.parseFloat(prop.getProperty("keystrokesY", "70"));
            keystrokesScale = Float.parseFloat(prop.getProperty("keystrokesScale", "1.0"));
            gammaValue = Double.parseDouble(prop.getProperty("gammaValue", "15.0"));
            autoChestDepositOpenDelay = Integer.parseInt(prop.getProperty("autoChestDepositOpenDelay", "4"));
            autoChestDepositTransferDelay = Integer.parseInt(prop.getProperty("autoChestDepositTransferDelay", "4"));
            autoChestDepositCloseDelay = Integer.parseInt(prop.getProperty("autoChestDepositCloseDelay", "4"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try (OutputStream os = new FileOutputStream(CONFIG_FILE.toFile())) {
            Properties prop = new Properties();
            prop.setProperty("autoMode", String.valueOf(autoMode));
            prop.setProperty("swordBlock", String.valueOf(swordBlock));
            prop.setProperty("useSwing", String.valueOf(useSwing));
            prop.setProperty("noSneakAnimation", String.valueOf(noSneakAnimation));
            prop.setProperty("isChinese", String.valueOf(isChinese));
            prop.setProperty("autoScreenshot", String.valueOf(autoScreenshot));
            prop.setProperty("hitMarker", String.valueOf(hitMarker));
            prop.setProperty("hitSound", String.valueOf(hitSound));
            prop.setProperty("damageRecord", String.valueOf(damageRecord));
            prop.setProperty("lowHealthNotify", String.valueOf(lowHealthNotify));
            prop.setProperty("targetHud", String.valueOf(targetHud));
            prop.setProperty("fallDamagePredict", String.valueOf(fallDamagePredict));
            prop.setProperty("victorySound", String.valueOf(victorySound));
            prop.setProperty("gammaOverride", String.valueOf(gammaOverride));
            prop.setProperty("autoSprint", String.valueOf(autoSprint));
            prop.setProperty("autoChestDeposit", String.valueOf(autoChestDeposit));
            prop.setProperty("autoChestDepositBlockMovement", String.valueOf(autoChestDepositBlockMovement));
            prop.setProperty("keystrokes", String.valueOf(keystrokes));
            prop.setProperty("disableImeInGame", String.valueOf(disableImeInGame));
            prop.setProperty("hitSoundType", hitSoundType.name());
            prop.setProperty("hitSoundCondition", hitSoundCondition.name());
            prop.setProperty("range", String.valueOf(range));
            prop.setProperty("animSpeed", String.valueOf(animSpeed));
            prop.setProperty("sneakDropScale", String.valueOf(sneakDropScale));
            prop.setProperty("sneakAnimationSpeed", String.valueOf(sneakAnimationSpeed));
            prop.setProperty("animationMode", animationMode.name());
            prop.setProperty("offsetX", String.valueOf(offsetX));
            prop.setProperty("offsetY", String.valueOf(offsetY));
            prop.setProperty("offsetZ", String.valueOf(offsetZ));
            prop.setProperty("targetHudX", String.valueOf(targetHudX));
            prop.setProperty("targetHudY", String.valueOf(targetHudY));
            prop.setProperty("targetHudZ", String.valueOf(targetHudZ));
            prop.setProperty("keystrokesX", String.valueOf(keystrokesX));
            prop.setProperty("keystrokesY", String.valueOf(keystrokesY));
            prop.setProperty("keystrokesScale", String.valueOf(keystrokesScale));
            prop.setProperty("gammaValue", String.valueOf(gammaValue));
            prop.setProperty("autoChestDepositOpenDelay", String.valueOf(autoChestDepositOpenDelay));
            prop.setProperty("autoChestDepositTransferDelay", String.valueOf(autoChestDepositTransferDelay));
            prop.setProperty("autoChestDepositCloseDelay", String.valueOf(autoChestDepositCloseDelay));
            prop.store(os, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
