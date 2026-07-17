package com.pvp_utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class Config {
    public static boolean autoMode = false;
    public static boolean swordBlock = false;
    public static boolean useSwing = false;
    public static boolean noSneakAnimation = false;
    public static boolean noDoubleSneak = true;
    public static boolean isChinese = defaultChinese();
    public static boolean autoScreenshot = false;
    public static boolean autoGG = false;
    public static boolean foodInfo = false;
    public static boolean hitMarker = false;
    public static boolean hitSound = false;
    public static boolean mainHandAssist = false;
    public static boolean mainHandAssistMeleeWeapon = false;
    public static boolean mainHandAssistShield = false;
    public static boolean mainHandAssistQuickUse = false;
    public static boolean mainHandAssistSwitchBack = true;
    public static boolean elytraAssist = false;
    public static boolean elytraAutoDeploy = true;
    public static boolean elytraAutoFirework = true;
    public static boolean lowHealthNotify = false;
    public static boolean damageNumbers = false;
    public static boolean attackReachDisplay = false;
    public static boolean arraylist = false;
    public static boolean targetHud = false;
    public static boolean diggingStatus = false;
    public static boolean betterPingDisplay = false;
    public static boolean lyricsDisplay = false;
    public static boolean fallDamagePredict = false;
    public static boolean fireballLandingPredict = false;
    public static boolean victorySound = false;
    public static boolean gammaOverride = false;
    public static boolean autoSprint = false;
    public static boolean noSwimming = false;
    public static boolean fishingRodAssist = false;
    public static boolean blockCountDisplay = false;
    public static BlockCountDisplayMode blockCountDisplayMode = BlockCountDisplayMode.NEW;
    public static boolean dynamicIsland = false;
    public static boolean dynamicIslandBlockCount = false;
    public static boolean dynamicIslandBlockCountRestoresBlockCount = false;
    public static boolean dynamicIslandBlockCountAltIcon = false;
    public static boolean dynamicIslandItemUseStatus = false;
    public static boolean dynamicIslandItemUseStatusRestoresItemUseStatus = false;
    public static boolean dynamicIslandLowHealthWarning = false;
    public static boolean itemUseStatus = false;
    public static boolean itemPhysics = false;
    public static boolean item2DRender = false;
    public static float itemPhysicsRotationSpeed = 1.0f;
    public static HudTheme hudTheme = HudTheme.LIGHT;
    public static float skiaBlurStrength = 1.0f;
    public static boolean timeChange = false;
    public static boolean weatherChange = false;
    public static boolean zoom = false;
    public static boolean zoomScroll = true;
    public static boolean freelook = false;
    public static boolean armorHud = false;
    public static boolean armorHudShowPercentage = true;
    public static boolean armorHudShowBar = true;
    public static ArmorHudDisplayMode armorHudDisplayMode = ArmorHudDisplayMode.BOTH;
    public static boolean potionStatus = false;
    public static boolean potionStatusBackground = true;
    public static boolean potionStatusCountdown = true;
    public static boolean potionStatusHideVanilla = true;
    public static boolean autoChestDeposit = false;
    public static boolean autoChestDepositResourcesOnly = true;
    public static boolean removeContainerBackground = false;
    public static boolean keystrokes = false;
    public static boolean disableImeInGame = false;
    public static boolean hideSignText = false;
    public static boolean hideEnchantTableBook = false;
    public static boolean hideFireOverlay = false;
    public static boolean hideHurtShake = false;
    public static boolean hideTotemAnimation = false;
    public static boolean hideExplosionParticles = false;
    public static boolean hideRainParticles = false;
    public static boolean hideBossBar = false;
    public static boolean hideVignette = false;
    public static boolean hideFog = false;
    public static boolean attackEffectsCritParticles = false;
    public static boolean attackEffectsSharpnessParticles = false;
    public static boolean attackEffectsFlameParticles = false;
    public static boolean attackEffectsBloodParticles = false;
    public static boolean attackEffectsLightning = false;
    public static boolean hitColor = false;
    public static boolean customBlockOutline = false;
    public static boolean customBlockOutlineFill = false;
    public static boolean customBlockOutlineAnimation = false;
    public static boolean customEnchantmentGlint = false;
    public static boolean motionCamera = false;
    public static boolean noAttackCooldownAnimation = false;
    public static boolean customCape = false;
    public static boolean chatHudEditQuickEnable = true;
    public static boolean betterChat = false;
    public static boolean betterChatMessageAnimation = true;
    public static boolean betterChatInputAnimation = true;
    public static boolean betterChatAvatar = true;
    public static boolean betterScoreboard = false;
    public static boolean betterScoreboardHideScores = false;
    public static boolean betterScoreboardVisualImprovement = false;
    public static boolean betterMouseLogic = false;
    public static boolean smoothHotbarScrolling = false;
    public static float smoothHotbarAnimationSpeed = 0.55f;
    public static int betterChatMessageFadeTime = 170;
    public static int betterChatInputFadeTime = 170;
    public static int hotbarRollover = 0;
    public static boolean useMainUI = false;
    public static MainUIBackgroundMode mainUIBackgroundMode = MainUIBackgroundMode.GLSL;
    public static MainUIGlslMode mainUIGlslMode = MainUIGlslMode.RANDOM;
    public static boolean mainUICustomBackground = false;
    public static boolean mainUIMouseEffect = false;
    public static boolean termsRead = false;
    public static boolean fullMode = false;
    public static boolean ircEnabled = false;
    public static boolean ircAutoConnect = false;
    public static String clientName = "PVPUtils";
    public static String autoGGText = "gg";
    public static String ircUsername = "";
    public static String ircToken = "";
    public static String ircPasswordHash = "";
    public static String mainUIBackgroundImage = "1.png";
    public static String mainUIGlslShader = "Galaxy.frag.glsl";
    public static String mainUIVideoBackground = "background.mp4";
    public static String customCapeImage = "default.png";
    public static HitSoundType hitSoundType = HitSoundType.NETHERITE;
    public static HitSoundCondition hitSoundCondition = HitSoundCondition.BOTH;
    public static TargetHudMode targetHudMode = TargetHudMode.LITE;
    public static KeystrokesMode keystrokesMode = KeystrokesMode.LITE;
    public static ArmorHudMode armorHudMode = ArmorHudMode.LITE;
    public static ArmorHudLayout armorHudLayout = ArmorHudLayout.SEPARATED;
    public static ItemUseStatusMode itemUseStatusMode = ItemUseStatusMode.LITE;
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
    public static boolean nameTag = false;
    public static float nameTagScale = 1.0f;
    public static boolean nameTagDynamicScale = false;
    public static boolean nameTagOnlyPlayer = false;
    public static boolean dynamicMotionBlur = false;
    public static float dynamicMotionBlurStrength = 1.0f;
    public static boolean dynamicMotionBlurRefreshRateScaling = true;
    public static float motionCameraFollowSpeed = 0.12f;
    public static float motionCameraDistance = 4.0f;
    public static float attackEffectsCritMultiplier = 1.0f;
    public static float attackEffectsSharpnessMultiplier = 1.0f;
    public static float attackEffectsFlameMultiplier = 1.0f;
    public static float attackEffectsBloodMultiplier = 1.0f;
    public static int attackEffectsLightningCount = 1;
    public static int hitColorRed = 255;
    public static int hitColorGreen = 0;
    public static int hitColorBlue = 0;
    public static int hitColorAlpha = 179;
    public static int customBlockOutlineRed = 255;
    public static int customBlockOutlineGreen = 255;
    public static int customBlockOutlineBlue = 255;
    public static int customBlockOutlineAlpha = 255;
    public static int customBlockOutlineFillRed = 255;
    public static int customBlockOutlineFillGreen = 255;
    public static int customBlockOutlineFillBlue = 255;
    public static int customBlockOutlineFillAlpha = 64;
    public static float blockCountDisplayX = 0f;
    public static float blockCountDisplayY = 0f;
    public static float blockCountDisplayScale = 1.0f;
    public static float armorHudX = 0f;
    public static float armorHudY = 0f;
    public static float armorHudScale = 1.0f;
    public static float itemUseStatusX = 0f;
    public static float itemUseStatusY = 0f;
    public static float itemUseStatusScale = 1.0f;
    public static float dynamicIslandX = 0f;
    public static float dynamicIslandY = 0f;
    public static float dynamicIslandScale = 1.0f;
    public static float notificationX = Float.NaN;
    public static float notificationY = Float.NaN;
    public static float notificationScale = 1.0f;
    public static float potionStatusX = 0f;
    public static float potionStatusY = 0f;
    public static float potionStatusScale = 1.0f;
    public static float lyricsDisplayX = 0f;
    public static float lyricsDisplayY = 0f;
    public static float lyricsDisplayScale = 1.0f;
    public static float betterScoreboardX = 0f;
    public static float betterScoreboardY = 0f;
    public static float betterScoreboardScale = 1.0f;
    public static float arraylistX = 0f;
    public static float arraylistY = 0f;
    public static float arraylistScale = 1.0f;
    public static int arraylistColorRed = 80;
    public static int arraylistColorGreen = 255;
    public static int arraylistColorBlue = 255;
    public static boolean arraylistGradient = false;
    public static int arraylistGradientRed = 80;
    public static int arraylistGradientGreen = 150;
    public static int arraylistGradientBlue = 255;
    public static float arraylistGradientSpeed = 1.0f;
    public static boolean arraylistBorder = false;
    public static float arraylistBorderWidth = 1.0f;
    public static float customBlockOutlineWidth = 1.0f;
    public static float customBlockOutlineAnimationSpeed = 8.0f;
    public static float customBlockOutlineMoveSpeed = 12.0f;
    public static float offsetX = 0f;
    public static float offsetY = 0f;
    public static float offsetZ = 0f;
    public static double gammaValue = 15.0;
    public static int fishingRodAssistUseDelay = 4;
    public static int autoGGDelayTicks = 20;
    public static int mainHandAssistSwitchDelayTicks = 2;
    public static int autoChestDepositDepositDelay = 4;
    public static int autoChestDepositCloseDelay = 4;
    public static int ircProtocolVersion = 1;
    public static int clientTime = 6000;
    public static int zoomAmount = 4;
    public static int zoomScrollSteps = 10;
    public static int zoomPerStep = 150;
    public static int zoomRelativeSensitivity = 100;
    public static int freelookSensitivity = 100;
    public static float zoomInTime = 0.25f;
    public static float zoomOutTime = 0.18f;
    public static WeatherMode weatherMode = WeatherMode.CLEAR;
    public static FreelookTriggerMode freelookTriggerMode = FreelookTriggerMode.HOLD;

    public enum AnimMode { MODE_1_7, MODE_PUSH, MODE_1_7_PLUS, MODE_NEW }
    public enum HitSoundType { NETHERITE, EXPERIENCE }
    public enum HitSoundCondition { BOTH, MELEE, RANGED }
    public enum TargetHudMode { LITE, NEW, BLUR }
    public enum BlockCountDisplayMode { NEW, BLUR }
    public enum KeystrokesMode { LITE, NEW }
    public enum ArmorHudMode { LITE, NEW }
    public enum ArmorHudLayout { SEPARATED, VERTICAL, HORIZONTAL }
    public enum ArmorHudDisplayMode { PERCENTAGE, BAR, BOTH }
    public enum MotionBlurAlgorithm { VELOCITY_BASED, FRAME_BLENDING, HYBRID_BLENDING, ACCUMULATION_MAX, ACCUMULATION_MIX }
    public enum HudTheme { DARK, LIGHT }
    public enum WeatherMode { CLEAR, RAIN, SNOW, THUNDER }
    public enum ItemUseStatusMode { LITE, NEW }
    public enum FreelookTriggerMode { HOLD, TOGGLE }
    public enum MainUIBackgroundMode { GLSL, IMAGE, VIDEO }
    public enum MainUIGlslMode { RANDOM, FIXED }

    public static void setMotionCamera(boolean value) {
        motionCamera = value;
    }

    public static int skiaBlurTintColor() {
        return hudTheme == HudTheme.LIGHT ? 0x66F8FAFC : 0x66111827;
    }

    public static int hudPrimaryTextColor() {
        return hudTheme == HudTheme.LIGHT ? 0xFF111827 : 0xFFFFFFFF;
    }

    public static int hudSecondaryTextColor() {
        return hudTheme == HudTheme.LIGHT ? 0xAA111827 : 0xCCFFFFFF;
    }

    public static int hudMutedTextColor() {
        return hudTheme == HudTheme.LIGHT ? 0xAA5C5870 : 0xBFFFFFFF;
    }

    public static int hudBorderColor() {
        return hudTheme == HudTheme.LIGHT ? 0x55111827 : 0x55FFFFFF;
    }

    public static void setDynamicIsland(boolean value) {
        dynamicIsland = value;
        if (value) {
            if (dynamicIslandBlockCount) {
                applyDynamicIslandBlockCountOverride();
            }
            if (dynamicIslandItemUseStatus) {
                applyDynamicIslandItemUseStatusOverride();
            }
        } else {
            restoreBlockCountDisplayFromDynamicIsland();
            restoreItemUseStatusFromDynamicIsland();
        }
    }

    public static void setDynamicIslandBlockCount(boolean value) {
        dynamicIslandBlockCount = value;
        if (value) {
            if (dynamicIsland) {
                applyDynamicIslandBlockCountOverride();
            }
        } else {
            restoreBlockCountDisplayFromDynamicIsland();
        }
    }

    public static void setBlockCountDisplay(boolean value) {
        blockCountDisplay = value;
        if (value) {
            dynamicIslandBlockCount = false;
            dynamicIslandBlockCountRestoresBlockCount = false;
        }
    }

    public static void setDynamicIslandItemUseStatus(boolean value) {
        dynamicIslandItemUseStatus = value;
        if (value) {
            if (dynamicIsland) {
                applyDynamicIslandItemUseStatusOverride();
            }
        } else {
            restoreItemUseStatusFromDynamicIsland();
        }
    }

    public static void setItemUseStatus(boolean value) {
        itemUseStatus = value;
        if (value) {
            dynamicIslandItemUseStatus = false;
            dynamicIslandItemUseStatusRestoresItemUseStatus = false;
        }
    }

    private static void normalizeDynamicIslandBlockCountState() {
        if (dynamicIsland && dynamicIslandBlockCount) {
            applyDynamicIslandBlockCountOverride();
        } else {
            restoreBlockCountDisplayFromDynamicIsland();
        }
        if (dynamicIsland && dynamicIslandItemUseStatus) {
            applyDynamicIslandItemUseStatusOverride();
        } else {
            restoreItemUseStatusFromDynamicIsland();
        }
    }

    private static void applyDynamicIslandBlockCountOverride() {
        if (blockCountDisplay) {
            blockCountDisplay = false;
            dynamicIslandBlockCountRestoresBlockCount = true;
        }
    }

    private static void restoreBlockCountDisplayFromDynamicIsland() {
        if (dynamicIslandBlockCountRestoresBlockCount) {
            blockCountDisplay = true;
            dynamicIslandBlockCountRestoresBlockCount = false;
        }
    }

    private static void applyDynamicIslandItemUseStatusOverride() {
        if (itemUseStatus) {
            itemUseStatus = false;
            dynamicIslandItemUseStatusRestoresItemUseStatus = true;
        }
    }

    private static void restoreItemUseStatusFromDynamicIsland() {
        if (dynamicIslandItemUseStatusRestoresItemUseStatus) {
            itemUseStatus = true;
            dynamicIslandItemUseStatusRestoresItemUseStatus = false;
        }
    }

    public static AnimMode animationMode = AnimMode.MODE_1_7;
    public static MotionBlurAlgorithm motionBlurAlgorithm = MotionBlurAlgorithm.VELOCITY_BASED;

    private static final Path CONFIG_DIRECTORY = FabricLoader.getInstance().getGameDir().resolve("PVPUtils");
    private static final Path CONFIG_FILE = CONFIG_DIRECTORY.resolve("Config.cfg");
    private static final Path HUD_CONFIG_FILE = CONFIG_DIRECTORY.resolve("Hud.cfg");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load() {
        ensureConfigDirectory();
        if (!Files.exists(CONFIG_FILE)) {
            applyGameLanguageDefault();
            if (Files.exists(HUD_CONFIG_FILE)) {
                loadHudConfig(HUD_CONFIG_FILE);
            }
            save();
            return;
        }
        loadJsonConfig(CONFIG_FILE);
        if (Files.exists(HUD_CONFIG_FILE)) {
            loadHudConfig(HUD_CONFIG_FILE);
        }
        normalizeDynamicIslandBlockCountState();
        save();
    }

    public static void save() {
        ensureConfigDirectory();
        try {
            Files.writeString(CONFIG_FILE, GSON.toJson(toJsonConfig()), StandardCharsets.UTF_8);
            Files.writeString(HUD_CONFIG_FILE, GSON.toJson(toHudJsonConfig()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void clearIrcSession() {
        ircEnabled = false;
        ircAutoConnect = false;
        ircToken = "";
    }

    private static void loadJsonConfig(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonObject()) {
                return;
            }
            applyJsonConfig(element.getAsJsonObject());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static JsonObject toJsonConfig() {
        JsonObject root = new JsonObject();
        for (Field field : configFields().values()) {
            if (isHudField(field.getName())) {
                continue;
            }
            ConfigPath path = pathForField(field.getName());
            JsonObject module = root.has(path.module()) && root.get(path.module()).isJsonObject()
                    ? root.getAsJsonObject(path.module())
                    : new JsonObject();
            writeJsonValue(module, path.key(), field);
            root.add(path.module(), module);
        }
        return root;
    }

    private static void applyJsonConfig(JsonObject root) {
        Map<String, Field> fields = configFields();
        for (Field field : fields.values()) {
            if (isHudField(field.getName())) {
                continue;
            }
            ConfigPath path = pathForField(field.getName());
            JsonObject module = root.has(path.module()) && root.get(path.module()).isJsonObject()
                    ? root.getAsJsonObject(path.module())
                    : null;
            JsonElement value = module != null ? module.get(path.key()) : root.get(field.getName());
            if (value == null || value.isJsonNull()) {
                continue;
            }
            readJsonValue(field, value);
        }
    }

    private static void loadHudConfig(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonObject()) {
                return;
            }
            applyHudJsonConfig(element.getAsJsonObject());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static JsonObject toHudJsonConfig() {
        JsonObject root = new JsonObject();
        Map<String, Field> fields = configFields();
        for (HudComponent component : HUD_COMPONENTS) {
            JsonObject object = new JsonObject();
            for (String fieldName : component.fields()) {
                Field field = fields.get(fieldName);
                if (field != null) {
                    writeJsonValue(object, hudKeyForField(component, fieldName), field);
                }
            }
            root.add(component.name(), object);
        }
        return root;
    }

    private static void applyHudJsonConfig(JsonObject root) {
        Map<String, Field> fields = configFields();
        for (HudComponent component : HUD_COMPONENTS) {
            JsonObject object = root.has(component.name()) && root.get(component.name()).isJsonObject()
                    ? root.getAsJsonObject(component.name())
                    : null;
            if (object == null) {
                continue;
            }
            for (String fieldName : component.fields()) {
                Field field = fields.get(fieldName);
                JsonElement value = field != null ? object.get(hudKeyForField(component, fieldName)) : null;
                if (field != null && value != null && !value.isJsonNull()) {
                    readJsonValue(field, value);
                }
            }
        }
    }

    private static Map<String, Field> configFields() {
        Map<String, Field> fields = new LinkedHashMap<>();
        for (Field field : Config.class.getFields()) {
            int modifiers = field.getModifiers();
            if (!Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
                continue;
            }
            fields.put(field.getName(), field);
        }
        return fields;
    }

    private static void writeJsonValue(JsonObject object, String key, Field field) {
        try {
            Class<?> type = field.getType();
            if (type == boolean.class) {
                object.addProperty(key, field.getBoolean(null));
            } else if (type == int.class) {
                object.addProperty(key, field.getInt(null));
            } else if (type == float.class) {
                object.addProperty(key, field.getFloat(null));
            } else if (type == double.class) {
                object.addProperty(key, field.getDouble(null));
            } else if (type == String.class) {
                object.addProperty(key, (String) field.get(null));
            } else if (type.isEnum()) {
                Object value = field.get(null);
                object.addProperty(key, value == null ? "" : ((Enum<?>) value).name());
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void readJsonValue(Field field, JsonElement value) {
        try {
            Class<?> type = field.getType();
            if (type == boolean.class) {
                field.setBoolean(null, value.getAsBoolean());
            } else if (type == int.class) {
                field.setInt(null, value.getAsInt());
            } else if (type == float.class) {
                field.setFloat(null, value.getAsFloat());
            } else if (type == double.class) {
                field.setDouble(null, value.getAsDouble());
            } else if (type == String.class) {
                field.set(null, value.getAsString());
            } else if (type.isEnum()) {
                field.set(null, Enum.valueOf((Class<? extends Enum>) type, value.getAsString()));
            }
        } catch (Exception ignored) {
        }
    }

    private static ConfigPath pathForField(String fieldName) {
        ModuleRule rule = moduleRuleFor(fieldName);
        if (rule == null) {
            return new ConfigPath("Global", toKebabCase(fieldName));
        }
        if (fieldName.equals(rule.toggleField())) {
            return new ConfigPath(rule.module(), "toggled");
        }
        String keySource = fieldName;
        if (!rule.prefix().isEmpty() && fieldName.startsWith(rule.prefix())) {
            keySource = fieldName.substring(rule.prefix().length());
            if (keySource.isEmpty()) {
                keySource = fieldName;
            } else {
                keySource = Character.toLowerCase(keySource.charAt(0)) + keySource.substring(1);
            }
        }
        return new ConfigPath(rule.module(), toKebabCase(keySource));
    }

    private static ModuleRule moduleRuleFor(String fieldName) {
        for (ModuleRule rule : MODULE_RULES) {
            if (fieldName.equals(rule.toggleField()) || (!rule.prefix().isEmpty() && fieldName.startsWith(rule.prefix()))) {
                return rule;
            }
            for (String extra : rule.extraFields()) {
                if (fieldName.equals(extra)) {
                    return rule;
                }
            }
        }
        return null;
    }

    private static boolean isHudField(String fieldName) {
        for (HudComponent component : HUD_COMPONENTS) {
            for (String hudField : component.fields()) {
                if (hudField.equals(fieldName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String hudKeyForField(HudComponent component, String fieldName) {
        String key = fieldName;
        if (!component.prefix().isEmpty() && fieldName.startsWith(component.prefix())) {
            key = fieldName.substring(component.prefix().length());
            if (!key.isEmpty()) {
                key = Character.toLowerCase(key.charAt(0)) + key.substring(1);
            }
        }
        return toKebabCase(key);
    }

    private static String toKebabCase(String value) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    out.append('-');
                }
                out.append(Character.toLowerCase(c));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static final ModuleRule[] MODULE_RULES = new ModuleRule[] {
            new ModuleRule("AutoMainHand", "mainHandAssist", "mainHandAssist"),
            new ModuleRule("ElytraImprovements", "elytra", "elytraAssist"),
            new ModuleRule("HitMarker", "hitMarker", "hitMarker"),
            new ModuleRule("HitSound", "hitSound", "hitSound"),
            new ModuleRule("SwordBlockingAnimation", "swordBlock", "swordBlock", "offsetX", "offsetY", "offsetZ", "animSpeed", "animationMode"),
            new ModuleRule("AutoBlock", "autoMode", "autoMode", "range"),
            new ModuleRule("UseAnimation", "useSwing", "useSwing"),
            new ModuleRule("RemoveAttackCooldownAnimation", "noAttackCooldownAnimation", "noAttackCooldownAnimation"),
            new ModuleRule("SneakAnimationAdjustment", "noSneakAnimation", "noSneakAnimation", "sneakDropScale", "sneakAnimationSpeed"),
            new ModuleRule("AutoScreenshot", "autoScreenshot", "autoScreenshot"),
            new ModuleRule("AutoGG", "autoGG", "autoGG"),
            new ModuleRule("FoodInfo", "foodInfo", "foodInfo"),
            new ModuleRule("LowHealthWarning", "lowHealthNotify", "lowHealthNotify"),
            new ModuleRule("DamageNumbers", "damageNumbers", "damageNumbers"),
            new ModuleRule("AttackReachDisplay", "attackReachDisplay", "attackReachDisplay"),
            new ModuleRule("FallDamagePrediction", "fallDamagePredict", "fallDamagePredict"),
            new ModuleRule("FireballLandingPrediction", "fireballLandingPredict", "fireballLandingPredict"),
            new ModuleRule("VictorySound", "victorySound", "victorySound"),
            new ModuleRule("GammaOverride", "gammaOverride", "gammaOverride", "gammaValue"),
            new ModuleRule("AutoSprint", "autoSprint", "autoSprint"),
            new ModuleRule("NoSwimming", "noSwimming", "noSwimming"),
            new ModuleRule("FishingRodAssist", "fishingRodAssist", "fishingRodAssist"),
            new ModuleRule("BlockCountDisplay", "blockCountDisplay", "blockCountDisplay"),
            new ModuleRule("BetterPingDisplay", "betterPingDisplay", "betterPingDisplay"),
            new ModuleRule("LyricsDisplay", "lyricsDisplay", "lyricsDisplay"),
            new ModuleRule("DynamicIsland", "dynamicIsland", "dynamicIsland"),
            new ModuleRule("Arraylist", "arraylist", "arraylist"),
            new ModuleRule("ItemUseStatus", "itemUseStatus", "itemUseStatus"),
            new ModuleRule("ItemPhysics", "itemPhysics", "itemPhysics"),
            new ModuleRule("DroppedItem2DRender", "item2DRender", "item2DRender"),
            new ModuleRule("TimeChange", "timeChange", "timeChange", "clientTime"),
            new ModuleRule("WeatherChange", "weatherChange", "weatherChange", "weatherMode"),
            new ModuleRule("Zoom", "zoom", "zoom"),
            new ModuleRule("Freelook", "freelook", "freelook"),
            new ModuleRule("ArmorHUD", "armorHud", "armorHud"),
            new ModuleRule("PotionStatus", "potionStatus", "potionStatus"),
            new ModuleRule("QuickDeposit", "autoChestDeposit", "autoChestDeposit"),
            new ModuleRule("RemoveContainerBackground", "removeContainerBackground", "removeContainerBackground"),
            new ModuleRule("Keystrokes", "keystrokes", "keystrokes"),
            new ModuleRule("InputMethodFix", "disableImeInGame", "disableImeInGame"),
            new ModuleRule("RenderControl", "hide", ""),
            new ModuleRule("AttackEffects", "attackEffects", ""),
            new ModuleRule("HitColor", "hitColor", "hitColor"),
            new ModuleRule("CustomBlockOutline", "customBlockOutline", "customBlockOutline", "customBlockOutlineFill", "customBlockOutlineAnimation", "customBlockOutlineRed", "customBlockOutlineGreen", "customBlockOutlineBlue", "customBlockOutlineAlpha", "customBlockOutlineFillRed", "customBlockOutlineFillGreen", "customBlockOutlineFillBlue", "customBlockOutlineFillAlpha", "customBlockOutlineWidth", "customBlockOutlineAnimationSpeed", "customBlockOutlineMoveSpeed"),
            new ModuleRule("RainbowEnchantmentGlint", "customEnchantmentGlint", "customEnchantmentGlint"),
            new ModuleRule("MotionCamera", "motionCamera", "motionCamera"),
            new ModuleRule("CustomCape", "customCape", "customCape", "customCapeImage"),
            new ModuleRule("BetterChat", "betterChat", "betterChat"),
            new ModuleRule("BetterScoreboard", "betterScoreboard", "betterScoreboard"),
            new ModuleRule("BetterMouseLogic", "betterMouseLogic", "betterMouseLogic"),
            new ModuleRule("SmoothHotbarScrolling", "smoothHotbar", "smoothHotbarScrolling", "hotbarRollover"),
            new ModuleRule("MainUI", "mainUI", "useMainUI", "mainUIBackgroundImage", "mainUIGlslShader"),
            new ModuleRule("TargetHUD", "targetHud", "targetHud"),
            new ModuleRule("NameTags", "nameTag", "nameTag"),
            new ModuleRule("DynamicMotionBlur", "dynamicMotionBlur", "dynamicMotionBlur", "motionBlurAlgorithm"),
            new ModuleRule("HUDTheme", "hud", "", "skiaBlurStrength"),
            new ModuleRule("Notification", "notification", ""),
    };

    private record ModuleRule(String module, String prefix, String toggleField, String... extraFields) {}
    private record ConfigPath(String module, String key) {}
    private record HudComponent(String name, String prefix, String... fields) {}

    private static final HudComponent[] HUD_COMPONENTS = new HudComponent[] {
            new HudComponent("TargetHUD", "targetHud", "targetHudX", "targetHudY", "targetHudZ", "targetHudScale"),
            new HudComponent("Keystrokes", "keystrokes", "keystrokesX", "keystrokesY", "keystrokesScale"),
            new HudComponent("BlockCountDisplay", "blockCountDisplay", "blockCountDisplayX", "blockCountDisplayY", "blockCountDisplayScale"),
            new HudComponent("ArmorHUD", "armorHud", "armorHudX", "armorHudY", "armorHudScale"),
            new HudComponent("ItemUseStatus", "itemUseStatus", "itemUseStatusX", "itemUseStatusY", "itemUseStatusScale"),
            new HudComponent("DynamicIsland", "dynamicIsland", "dynamicIslandX", "dynamicIslandY", "dynamicIslandScale"),
            new HudComponent("Notification", "notification", "notificationX", "notificationY", "notificationScale"),
            new HudComponent("PotionStatus", "potionStatus", "potionStatusX", "potionStatusY", "potionStatusScale"),
            new HudComponent("LyricsDisplay", "lyricsDisplay", "lyricsDisplayX", "lyricsDisplayY", "lyricsDisplayScale"),
            new HudComponent("BetterScoreboard", "betterScoreboard", "betterScoreboardX", "betterScoreboardY", "betterScoreboardScale"),
    };

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

    private static HudTheme parseHudTheme(String value) {
        if ("LIGHT".equals(value)) {
            return HudTheme.LIGHT;
        }
        return HudTheme.DARK;
    }

    public static void applyGameLanguageDefault() {
        isChinese = defaultChinese();
    }

    public static void applyFirstUseLanguageDefault() {
        if (!termsRead) {
            applyGameLanguageDefault();
        }
    }

    private static void ensureConfigDirectory() {
        try {
            Files.createDirectories(CONFIG_DIRECTORY);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create config directory: " + CONFIG_DIRECTORY, e);
        }
    }

}

