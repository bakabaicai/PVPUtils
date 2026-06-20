package com.pvp_utils;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import java.io.*;
import java.nio.file.Path;
import java.util.Properties;

public class Config {
    public static boolean autoMode = false;
    public static boolean swordBlock = false;
    public static boolean useSwing = false;
    public static boolean noSneakAnimation = false;
    public static boolean isChinese = defaultChinese();
    public static boolean autoScreenshot = false;
    public static boolean hitMarker = false;
    public static boolean hitSound = true;
    public static boolean elytraAssist = false;
    public static boolean elytraAutoDeploy = true;
    public static boolean elytraAutoFirework = true;
    public static boolean lowHealthNotify = false;
    public static boolean targetHud = false;
    public static boolean diggingStatus = false;
    public static boolean fallDamagePredict = false;
    public static boolean victorySound = false;
    public static boolean gammaOverride = false;
    public static boolean autoSprint = false;
    public static boolean fishingRodAssist = false;
    public static boolean blockCountDisplay = false;
    public static boolean autoChestDeposit = false;
    public static boolean autoChestDepositResourcesOnly = true;
    public static boolean keystrokes = false;
    public static boolean disableImeInGame = false;
    public static boolean hideSignText = false;
    public static boolean hideEnchantTableBook = false;
    public static boolean hideFireOverlay = false;
    public static boolean hideHurtShake = false;
    public static boolean hideTotemAnimation = false;
    public static boolean hideExplosionParticles = false;
    public static boolean noAttackCooldownAnimation = false;
    public static boolean customCape = false;
    public static boolean chatHudEditQuickEnable = true;
    public static boolean useMainUI = false;
    public static boolean mainUICustomBackground = false;
    public static boolean mainUIMouseEffect = false;
    public static boolean termsRead = false;
    public static boolean fullMode = false;
    public static String mainUIBackgroundImage = "1.png";
    public static String customCapeImage = "default.png";
    public static HitSoundType hitSoundType = HitSoundType.NETHERITE;
    public static HitSoundCondition hitSoundCondition = HitSoundCondition.BOTH;
    public static TargetHudMode targetHudMode = TargetHudMode.NEW;
    public static KeystrokesMode keystrokesMode = KeystrokesMode.NEW;
    public static double range = 3.0;
    public static float animSpeed = 1.0f;
    public static float sneakDropScale = 0.5f;
    public static float sneakAnimationSpeed = 1.0f;
    public static float targetHudX = -300f;
    public static float targetHudY = -100f;
    public static float targetHudZ = 0f;
    public static float targetHudScale = 1.0f;
    public static float keystrokesX = -170f;
    public static float keystrokesY = 70f;
    public static float keystrokesScale = 1.0f;
    public static float blockCountDisplayX = 0f;
    public static float blockCountDisplayY = 0f;
    public static float blockCountDisplayScale = 1.0f;
    public static float notificationX = Float.NaN;
    public static float notificationY = Float.NaN;
    public static float notificationScale = 1.0f;
    public static float offsetX = 0f;
    public static float offsetY = 0f;
    public static float offsetZ = 0f;
    public static double gammaValue = 15.0;
    public static int fishingRodAssistUseDelay = 0;
    public static int autoChestDepositDepositDelay = 4;
    public static int autoChestDepositCloseDelay = 4;

    public enum AnimMode { MODE_1_7, MODE_PUSH, MODE_1_7_PLUS, MODE_NEW }
    public enum HitSoundType { NETHERITE, EXPERIENCE }
    public enum HitSoundCondition { BOTH, MELEE, RANGED }
    public enum TargetHudMode { LITE, NEW }
    public enum KeystrokesMode { LITE, NEW }

    public static AnimMode animationMode = AnimMode.MODE_1_7;

    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("pvp_utils.properties");

    public static void load() {
        if (!CONFIG_FILE.toFile().exists()) {
            isChinese = defaultChinese();
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
            isChinese = Boolean.parseBoolean(prop.getProperty("isChinese", String.valueOf(defaultChinese())));
            autoScreenshot = Boolean.parseBoolean(prop.getProperty("autoScreenshot", "false"));
            hitMarker = Boolean.parseBoolean(prop.getProperty("hitMarker", "false"));
            hitSound = Boolean.parseBoolean(prop.getProperty("hitSound", "true"));
            elytraAssist = Boolean.parseBoolean(prop.getProperty("elytraAssist", "false"));
            elytraAutoDeploy = Boolean.parseBoolean(prop.getProperty("elytraAutoDeploy", "true"));
            elytraAutoFirework = Boolean.parseBoolean(prop.getProperty("elytraAutoFirework", "true"));
            lowHealthNotify = Boolean.parseBoolean(prop.getProperty("lowHealthNotify", "false"));
            targetHud = Boolean.parseBoolean(prop.getProperty("targetHud", "false"));
            diggingStatus = Boolean.parseBoolean(prop.getProperty("diggingStatus", "false"));
            fallDamagePredict = Boolean.parseBoolean(prop.getProperty("fallDamagePredict", "false"));
            victorySound = Boolean.parseBoolean(prop.getProperty("victorySound", "false"));
            gammaOverride = Boolean.parseBoolean(prop.getProperty("gammaOverride", "false"));
            autoSprint = Boolean.parseBoolean(prop.getProperty("autoSprint", "false"));
            fishingRodAssist = Boolean.parseBoolean(prop.getProperty("fishingRodAssist", "false"));
            blockCountDisplay = Boolean.parseBoolean(prop.getProperty("blockCountDisplay", "false"));
            autoChestDeposit = Boolean.parseBoolean(prop.getProperty("autoChestDeposit", "false"));
            autoChestDepositResourcesOnly = Boolean.parseBoolean(prop.getProperty("autoChestDepositResourcesOnly", "true"));
            keystrokes = Boolean.parseBoolean(prop.getProperty("keystrokes", "false"));
            disableImeInGame = Boolean.parseBoolean(prop.getProperty("disableImeInGame", "false"));
            hideSignText = Boolean.parseBoolean(prop.getProperty("hideSignText", "false"));
            hideEnchantTableBook = Boolean.parseBoolean(prop.getProperty("hideEnchantTableBook", "false"));
            hideFireOverlay = Boolean.parseBoolean(prop.getProperty("hideFireOverlay", "false"));
            hideHurtShake = Boolean.parseBoolean(prop.getProperty("hideHurtShake", "false"));
            hideTotemAnimation = Boolean.parseBoolean(prop.getProperty("hideTotemAnimation", "false"));
            hideExplosionParticles = Boolean.parseBoolean(prop.getProperty("hideExplosionParticles", "false"));
            noAttackCooldownAnimation = Boolean.parseBoolean(prop.getProperty("noAttackCooldownAnimation", "false"));
            customCape = Boolean.parseBoolean(prop.getProperty("customCape", "false"));
            chatHudEditQuickEnable = Boolean.parseBoolean(prop.getProperty("chatHudEditQuickEnable", "true"));
            useMainUI = Boolean.parseBoolean(prop.getProperty("useMainUI", "false"));
            mainUICustomBackground = Boolean.parseBoolean(prop.getProperty("mainUICustomBackground", "false"));
            mainUIMouseEffect = Boolean.parseBoolean(prop.getProperty("mainUIMouseEffect", "false"));
            termsRead = Boolean.parseBoolean(prop.getProperty("termsRead", "false"));
            fullMode = Boolean.parseBoolean(prop.getProperty("fullMode", "false"));
            mainUIBackgroundImage = prop.getProperty("mainUIBackgroundImage", "1.png");
            customCapeImage = prop.getProperty("customCapeImage", "default.png");
            hitSoundType = HitSoundType.valueOf(prop.getProperty("hitSoundType", "NETHERITE"));
            hitSoundCondition = HitSoundCondition.valueOf(prop.getProperty("hitSoundCondition", "BOTH"));
            targetHudMode = TargetHudMode.valueOf(prop.getProperty("targetHudMode", "NEW"));
            keystrokesMode = KeystrokesMode.valueOf(prop.getProperty("keystrokesMode", "NEW"));
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
            targetHudScale = Float.parseFloat(prop.getProperty("targetHudScale", "1.0"));
            keystrokesX = Float.parseFloat(prop.getProperty("keystrokesX", "-170"));
            keystrokesY = Float.parseFloat(prop.getProperty("keystrokesY", "70"));
            keystrokesScale = Float.parseFloat(prop.getProperty("keystrokesScale", "1.0"));
            blockCountDisplayX = Float.parseFloat(prop.getProperty("blockCountDisplayX", "0"));
            blockCountDisplayY = Float.parseFloat(prop.getProperty("blockCountDisplayY", "0"));
            blockCountDisplayScale = Float.parseFloat(prop.getProperty("blockCountDisplayScale", "1.0"));
            notificationX = Float.parseFloat(prop.getProperty("notificationX", "NaN"));
            notificationY = Float.parseFloat(prop.getProperty("notificationY", "NaN"));
            notificationScale = Float.parseFloat(prop.getProperty("notificationScale", "1.0"));
            gammaValue = Double.parseDouble(prop.getProperty("gammaValue", "15.0"));
            fishingRodAssistUseDelay = Integer.parseInt(prop.getProperty("fishingRodAssistUseDelay", "0"));
            autoChestDepositDepositDelay = Integer.parseInt(prop.getProperty("autoChestDepositDepositDelay", "4"));
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
            prop.setProperty("elytraAssist", String.valueOf(elytraAssist));
            prop.setProperty("elytraAutoDeploy", String.valueOf(elytraAutoDeploy));
            prop.setProperty("elytraAutoFirework", String.valueOf(elytraAutoFirework));
            prop.setProperty("lowHealthNotify", String.valueOf(lowHealthNotify));
            prop.setProperty("targetHud", String.valueOf(targetHud));
            prop.setProperty("diggingStatus", String.valueOf(diggingStatus));
            prop.setProperty("fallDamagePredict", String.valueOf(fallDamagePredict));
            prop.setProperty("victorySound", String.valueOf(victorySound));
            prop.setProperty("gammaOverride", String.valueOf(gammaOverride));
            prop.setProperty("autoSprint", String.valueOf(autoSprint));
            prop.setProperty("fishingRodAssist", String.valueOf(fishingRodAssist));
            prop.setProperty("blockCountDisplay", String.valueOf(blockCountDisplay));
            prop.setProperty("autoChestDeposit", String.valueOf(autoChestDeposit));
            prop.setProperty("autoChestDepositResourcesOnly", String.valueOf(autoChestDepositResourcesOnly));
            prop.setProperty("keystrokes", String.valueOf(keystrokes));
            prop.setProperty("disableImeInGame", String.valueOf(disableImeInGame));
            prop.setProperty("hideSignText", String.valueOf(hideSignText));
            prop.setProperty("hideEnchantTableBook", String.valueOf(hideEnchantTableBook));
            prop.setProperty("hideFireOverlay", String.valueOf(hideFireOverlay));
            prop.setProperty("hideHurtShake", String.valueOf(hideHurtShake));
            prop.setProperty("hideTotemAnimation", String.valueOf(hideTotemAnimation));
            prop.setProperty("hideExplosionParticles", String.valueOf(hideExplosionParticles));
            prop.setProperty("noAttackCooldownAnimation", String.valueOf(noAttackCooldownAnimation));
            prop.setProperty("customCape", String.valueOf(customCape));
            prop.setProperty("chatHudEditQuickEnable", String.valueOf(chatHudEditQuickEnable));
            prop.setProperty("useMainUI", String.valueOf(useMainUI));
            prop.setProperty("mainUICustomBackground", String.valueOf(mainUICustomBackground));
            prop.setProperty("mainUIMouseEffect", String.valueOf(mainUIMouseEffect));
            prop.setProperty("termsRead", String.valueOf(termsRead));
            prop.setProperty("fullMode", String.valueOf(fullMode));
            prop.setProperty("mainUIBackgroundImage", mainUIBackgroundImage);
            prop.setProperty("customCapeImage", customCapeImage);
            prop.setProperty("hitSoundType", hitSoundType.name());
            prop.setProperty("hitSoundCondition", hitSoundCondition.name());
            prop.setProperty("targetHudMode", targetHudMode.name());
            prop.setProperty("keystrokesMode", keystrokesMode.name());
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
            prop.setProperty("targetHudScale", String.valueOf(targetHudScale));
            prop.setProperty("keystrokesX", String.valueOf(keystrokesX));
            prop.setProperty("keystrokesY", String.valueOf(keystrokesY));
            prop.setProperty("keystrokesScale", String.valueOf(keystrokesScale));
            prop.setProperty("blockCountDisplayX", String.valueOf(blockCountDisplayX));
            prop.setProperty("blockCountDisplayY", String.valueOf(blockCountDisplayY));
            prop.setProperty("blockCountDisplayScale", String.valueOf(blockCountDisplayScale));
            prop.setProperty("notificationX", String.valueOf(notificationX));
            prop.setProperty("notificationY", String.valueOf(notificationY));
            prop.setProperty("notificationScale", String.valueOf(notificationScale));
            prop.setProperty("gammaValue", String.valueOf(gammaValue));
            prop.setProperty("fishingRodAssistUseDelay", String.valueOf(fishingRodAssistUseDelay));
            prop.setProperty("autoChestDepositDepositDelay", String.valueOf(autoChestDepositDepositDelay));
            prop.setProperty("autoChestDepositCloseDelay", String.valueOf(autoChestDepositCloseDelay));
            prop.store(os, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean defaultChinese() {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.getLanguageManager() != null) {
                String selected = client.getLanguageManager().getSelected();
                return selected != null && selected.toLowerCase().startsWith("zh_");
            }
        } catch (Throwable ignored) {}
        return false;
    }
}
