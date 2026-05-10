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
    public static boolean isChinese = false;
    public static boolean autoScreenshot = false;
    public static boolean hitMarker = false;
    public static boolean hitSound = true;
    public static boolean damageRecord = false;
    public static boolean lowHealthNotify = false;
    public static boolean targetHud = false;
    public static boolean fallDamagePredict = false;
    public static boolean victorySound = false;
    public static HitSoundType hitSoundType = HitSoundType.NETHERITE;
    public static HitSoundCondition hitSoundCondition = HitSoundCondition.BOTH;
    public static double range = 3.0;
    public static float animSpeed = 1.0f;
    public static float sneakDropScale = 0.5f;
    public static float targetHudX = -300f;
    public static float targetHudY = -100f;
    public static float targetHudZ = 0f;
    public static float offsetX = 0f;
    public static float offsetY = 0f;
    public static float offsetZ = 0f;

    public enum AnimMode { MODE_1_7, MODE_PUSH, MODE_1_7_PLUS }
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
            isChinese = Boolean.parseBoolean(prop.getProperty("isChinese", "false"));
            autoScreenshot = Boolean.parseBoolean(prop.getProperty("autoScreenshot", "false"));
            hitMarker = Boolean.parseBoolean(prop.getProperty("hitMarker", "false"));
            hitSound = Boolean.parseBoolean(prop.getProperty("hitSound", "true"));
            damageRecord = Boolean.parseBoolean(prop.getProperty("damageRecord", "false"));
            lowHealthNotify = Boolean.parseBoolean(prop.getProperty("lowHealthNotify", "false"));
            targetHud = Boolean.parseBoolean(prop.getProperty("targetHud", "false"));
            fallDamagePredict = Boolean.parseBoolean(prop.getProperty("fallDamagePredict", "false"));
            victorySound = Boolean.parseBoolean(prop.getProperty("victorySound", "false"));
            hitSoundType = HitSoundType.valueOf(prop.getProperty("hitSoundType", "NETHERITE"));
            hitSoundCondition = HitSoundCondition.valueOf(prop.getProperty("hitSoundCondition", "BOTH"));
            range = Double.parseDouble(prop.getProperty("range", "3.0"));
            animSpeed = Float.parseFloat(prop.getProperty("animSpeed", "1.0"));
            sneakDropScale = Float.parseFloat(prop.getProperty("sneakDropScale", "0.5"));
            animationMode = AnimMode.valueOf(prop.getProperty("animationMode", "MODE_1_7"));
            offsetX = Float.parseFloat(prop.getProperty("offsetX", "0"));
            offsetY = Float.parseFloat(prop.getProperty("offsetY", "0"));
            offsetZ = Float.parseFloat(prop.getProperty("offsetZ", "0"));
            targetHudX = Float.parseFloat(prop.getProperty("targetHudX", "-300"));
            targetHudY = Float.parseFloat(prop.getProperty("targetHudY", "-100"));
            targetHudZ = Float.parseFloat(prop.getProperty("targetHudZ", "0"));
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
            prop.setProperty("hitSoundType", hitSoundType.name());
            prop.setProperty("hitSoundCondition", hitSoundCondition.name());
            prop.setProperty("range", String.valueOf(range));
            prop.setProperty("animSpeed", String.valueOf(animSpeed));
            prop.setProperty("sneakDropScale", String.valueOf(sneakDropScale));
            prop.setProperty("animationMode", animationMode.name());
            prop.setProperty("offsetX", String.valueOf(offsetX));
            prop.setProperty("offsetY", String.valueOf(offsetY));
            prop.setProperty("offsetZ", String.valueOf(offsetZ));
            prop.setProperty("targetHudX", String.valueOf(targetHudX));
            prop.setProperty("targetHudY", String.valueOf(targetHudY));
            prop.setProperty("targetHudZ", String.valueOf(targetHudZ));
            prop.store(os, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
