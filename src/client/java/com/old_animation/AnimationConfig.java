package com.old_animation;

import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.file.Path;
import java.util.Properties;

public class AnimationConfig {
    public static boolean autoMode = false;
    public static boolean swordBlock = false;
    public static boolean useSwing = false;
    public static boolean isChinese = false;
    public static boolean autoScreenshot = false;
    public static boolean hitMarker = false;
    public static boolean hitSound = true;
    public static boolean damageRecord = false;
    public static boolean lowHealthNotify = false;
    public static boolean targetHud = false;
    public static boolean fallDamagePredict = false;
    public static HitSoundType hitSoundType = HitSoundType.NETHERITE;
    public static HitSoundCondition hitSoundCondition = HitSoundCondition.BOTH;
    public static double range = 3.0;
    public static float animSpeed = 1.0f;
    public static float targetHudX = -300f;
    public static float targetHudY = -100f;
    public static float targetHudZ = 0f;

    public enum AnimMode { MODE_1_7, MODE_PUSH, MODE_1_7_PLUS }
    public enum HitSoundType { NETHERITE, EXPERIENCE }
    public enum HitSoundCondition { BOTH, MELEE, RANGED }

    public static AnimMode animationMode = AnimMode.MODE_1_7;
    public static float offsetX = 0.0f;
    public static float offsetY = 0.0f;
    public static float offsetZ = 0.0f;

    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("old_animation.properties");

    public static void load() {
        if (!CONFIG_FILE.toFile().exists()) return;
        try (InputStream is = new FileInputStream(CONFIG_FILE.toFile())) {
            Properties prop = new Properties();
            prop.load(is);
            autoMode = Boolean.parseBoolean(prop.getProperty("autoMode", "false"));
            swordBlock = Boolean.parseBoolean(prop.getProperty("swordBlock", "false"));
            useSwing = Boolean.parseBoolean(prop.getProperty("useSwing", "false"));
            isChinese = Boolean.parseBoolean(prop.getProperty("isChinese", "false"));
            autoScreenshot = Boolean.parseBoolean(prop.getProperty("autoScreenshot", "false"));
            hitMarker = Boolean.parseBoolean(prop.getProperty("hitMarker", "false"));
            hitSound = Boolean.parseBoolean(prop.getProperty("hitSound", "true"));
            damageRecord = Boolean.parseBoolean(prop.getProperty("damageRecord", "true"));
            lowHealthNotify = Boolean.parseBoolean(prop.getProperty("lowHealthNotify", "true"));
            targetHud = Boolean.parseBoolean(prop.getProperty("targetHud", "false"));
            fallDamagePredict = Boolean.parseBoolean(prop.getProperty("fallDamagePredict", "false"));
            hitSoundType = HitSoundType.valueOf(prop.getProperty("hitSoundType", "NETHERITE"));
            hitSoundCondition = HitSoundCondition.valueOf(prop.getProperty("hitSoundCondition", "BOTH"));
            range = Double.parseDouble(prop.getProperty("range", "3.0"));
            animSpeed = Float.parseFloat(prop.getProperty("animSpeed", "1.0"));
            animationMode = AnimMode.valueOf(prop.getProperty("animationMode", "MODE_1_7"));
            offsetX = Float.parseFloat(prop.getProperty("offsetX", "0.0"));
            offsetY = Float.parseFloat(prop.getProperty("offsetY", "0.0"));
            offsetZ = Float.parseFloat(prop.getProperty("offsetZ", "0.0"));
            targetHudX = Float.parseFloat(prop.getProperty("targetHudX", "-300.0"));
            targetHudY = Float.parseFloat(prop.getProperty("targetHudY", "-100.0"));
            targetHudZ = Float.parseFloat(prop.getProperty("targetHudZ", "0.0"));
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
            prop.setProperty("isChinese", String.valueOf(isChinese));
            prop.setProperty("autoScreenshot", String.valueOf(autoScreenshot));
            prop.setProperty("hitMarker", String.valueOf(hitMarker));
            prop.setProperty("hitSound", String.valueOf(hitSound));
            prop.setProperty("damageRecord", String.valueOf(damageRecord));
            prop.setProperty("lowHealthNotify", String.valueOf(lowHealthNotify));
            prop.setProperty("targetHud", String.valueOf(targetHud));
            prop.setProperty("fallDamagePredict", String.valueOf(fallDamagePredict));
            prop.setProperty("hitSoundType", hitSoundType.name());
            prop.setProperty("hitSoundCondition", hitSoundCondition.name());
            prop.setProperty("range", String.valueOf(range));
            prop.setProperty("animSpeed", String.valueOf(animSpeed));
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