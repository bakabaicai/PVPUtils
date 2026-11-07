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
    public static String themePreset = "BLEND";
    public static boolean themeAnimated = true;
    public static float themeSpeed = 1f;
    public static int themePrimaryColor = 0xFF4794FD;
    public static int themeSecondaryColor = 0xFF47FDA0;
    public static boolean themeCustomColors;
    public static boolean mainUIFrameLimitEnabled = true;
    public static int mainUIFrameLimit = 120;
    public static boolean hitSoundMeleeEnabled = true;
    public static boolean hitSoundRangedEnabled = true;
    public static HitSoundType hitSoundMeleeType = HitSoundType.NETHERITE;
    public static HitSoundType hitSoundRangedType = HitSoundType.NETHERITE;
    public static String hitSoundMeleeFile = "";
    public static String hitSoundRangedFile = "";
    public static HudStyle potionStatusStyle = HudStyle.LITE;
    public static com.pvp_utils.client.render.HudAppearance musicInfoAppearance = new com.pvp_utils.client.render.HudAppearance();
    public static com.pvp_utils.client.render.HudAppearance targetAppearance = new com.pvp_utils.client.render.HudAppearance();
    public static com.pvp_utils.client.render.HudAppearance armorAppearance = new com.pvp_utils.client.render.HudAppearance();
    public static com.pvp_utils.client.render.HudAppearance pingAppearance = new com.pvp_utils.client.render.HudAppearance();
    public static com.pvp_utils.client.render.HudAppearance tpsAppearance = new com.pvp_utils.client.render.HudAppearance();
    public static com.pvp_utils.client.render.HudAppearance clockAppearance = new com.pvp_utils.client.render.HudAppearance();
    public static com.pvp_utils.client.render.HudAppearance scoreboardAppearance = new com.pvp_utils.client.render.HudAppearance();
    public static com.pvp_utils.client.render.HudAppearance clickGuiAppearance = new com.pvp_utils.client.render.HudAppearance();
    public static com.pvp_utils.client.render.HudAppearance hotbarAppearance = new com.pvp_utils.client.render.HudAppearance();
    public static com.pvp_utils.client.render.HudAppearance potionAppearance = new com.pvp_utils.client.render.HudAppearance();
    public static com.pvp_utils.client.render.HudAppearance keystrokesAppearance = new com.pvp_utils.client.render.HudAppearance();
    public static com.pvp_utils.client.render.HudAppearance blockCountAppearance = new com.pvp_utils.client.render.HudAppearance();
    public static com.pvp_utils.client.render.HudAppearance itemUseAppearance = new com.pvp_utils.client.render.HudAppearance();
    public static com.pvp_utils.client.render.HudAppearance dynamicIslandAppearance = new com.pvp_utils.client.render.HudAppearance();
    public static com.pvp_utils.client.render.HudAppearance notificationAppearance = new com.pvp_utils.client.render.HudAppearance();
    public static com.pvp_utils.client.render.HudAppearance lyricsAppearance = new com.pvp_utils.client.render.HudAppearance();
    public static com.pvp_utils.client.render.HudAppearance arraylistAppearance = new com.pvp_utils.client.render.HudAppearance();

    private static final boolean PROFILE_GATE = false;

    public static boolean autoMode = false;
    public static boolean swordBlock = false;
    public static boolean legacy17Animations = false;
    public static boolean legacy17UseSwing = true;
    public static boolean legacy17FishingRod = true;
    public static boolean legacy17HeadRotation = true;
    public static boolean legacy17HurtTilt = true;
    public static boolean legacy17ItemPickup = true;
    public static boolean noSneakAnimation = false;
    public static boolean noDoubleSneak = true;
    public static boolean isChinese = defaultChinese();
    public static String moduleKeybinds = "";
    public static String clientCommandPrefix = ".";
    public static boolean autoScreenshot = false;
    public static boolean autoGG = false;
    public static boolean serverAutoLogin = false;
    public static int serverAutoLoginMode = 0;
    public static String serverAutoLoginRules = "";
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
    public static boolean lyricsDisplay = true;
    public static boolean lyricsDisplayTranslation = true;
    public static boolean musicInfoHud = false;
    public static boolean musicInfoHudCoverRounded = true;
    public static boolean fallDamagePredict = false;
    public static boolean fireballLandingPredict = false;
    public static boolean projectileTrajectoryPredict = false;
    public static boolean projectileTrajectoryPredictBow = true;
    public static boolean projectileTrajectoryPredictSnowball = true;
    public static boolean projectileTrajectoryPredictEnderPearl = true;
    public static boolean projectileTrajectoryPredictBlock = true;
    public static boolean projectileTrajectoryPredictEntity = true;
    public static boolean projectileTrajectoryPredictEntityMovement = true;
    public static boolean projectileTrajectoryPredictOtherPlayers = false;
    public static boolean victorySound = false;
    public static boolean fakeFps = false;
    public static int fakeFpsMin = 300;
    public static int fakeFpsMax = 600;
    public static boolean fakeFpsRandom = false;
    public static float fakeFpsJumpMinSeconds = 0.5f;
    public static float fakeFpsJumpMaxSeconds = 1.5f;
    public static boolean gammaOverride = false;
    public static boolean autoSprint = false;
    public static boolean noSwimming = false;
    public static boolean nickHider = false;
    public static boolean nickHiderChat = true;
    public static boolean nickHiderTab = true;
    public static boolean nickHiderNametag = true;
    public static boolean sunWorldRankSpoof = false;
    public static boolean sunWorldRankVip1 = true;
    public static boolean sunWorldRankVip2 = false;
    public static boolean sunWorldRankVip3 = false;
    public static boolean sunWorldRankVip4 = false;
    public static boolean sunWorldRankSvip = false;
    public static boolean sunWorldRankCreator = false;
    public static boolean sunWorldRankGuide = false;
    public static boolean sunWorldRankAdmin = false;
    public static boolean heldItemPosition = false;
    public static boolean fishingRodAssist = false;
    public static boolean blockCountDisplay = false;
    public static BlockCountDisplayMode blockCountDisplayMode = BlockCountDisplayMode.BLUR;
    public static boolean dynamicIsland = false;
    public static boolean dynamicIslandLyrics = false;
    public static boolean dynamicIslandDoubleRow = false;
    public static DynamicIslandShape dynamicIslandShape = DynamicIslandShape.CAPSULE;
    public static DynamicIslandMusicMode dynamicIslandMusicMode = DynamicIslandMusicMode.LYRICS;
    public static DynamicIslandBackground dynamicIslandBackground = DynamicIslandBackground.BLUR;
    public static int dynamicIslandColor = 0xED15191F;
    public static boolean dynamicIslandShadow = true;
    public static float dynamicIslandShadowStrength = 0.35f;
    public static boolean dynamicIslandBlockCount = false;
    public static boolean dynamicIslandBlockCountRestoresBlockCount = false;
    public static boolean dynamicIslandBlockCountAltIcon = false;
    public static boolean dynamicIslandItemUseStatus = false;
    public static boolean dynamicIslandItemUseStatusRestoresItemUseStatus = false;
    public static boolean dynamicIslandLowHealthWarning = false;
    public static boolean itemUseStatus = false;
    public static boolean itemPhysics = false;
    public static boolean item2DRender = false;
    public static boolean mcbiSkinDisplay = false;
    public static String mcbiSkinModes = "";
    public static float itemPhysicsRotationSpeed = 1.0f;
    public static HudTheme hudTheme = HudTheme.LIGHT;
    public static float skiaBlurStrength = 1.0f;
    public static boolean clickGuiPanelBlur = false;
    public static String clickGuiTheme = "white";
    public static int clickGuiScale = 0;
    public static float clickGuiScrollSpeed = 1.0f;
    public static float clickGuiCardW = 740f;
    public static float clickGuiCardH = 500f;
    public static float clickGuiFlyoutW = 288f;
    public static boolean timeChange = false;
    public static boolean weatherChange = false;
    public static boolean zoom = false;
    public static boolean zoomScroll = true;
    public static boolean freelook = false;
    public static boolean armorHud = false;
    public static boolean armorHudShowPercentage = true;
    public static boolean armorHudShowBar = true;
    public static boolean armorTransparency = false;
    public static boolean armorTransparencyShowInCombat = true;
    public static ArmorHudDisplayMode armorHudDisplayMode = ArmorHudDisplayMode.BOTH;
    public static boolean potionStatus = false;
    public static boolean potionStatusBackground = true;
    public static boolean potionStatusCountdown = true;
    public static boolean potionStatusHideVanilla = true;
    public static boolean autoChestDeposit = false;
    public static boolean autoChestDepositResourcesOnly = true;
    public static boolean removeContainerBackground = false;
    public static boolean pseudoCreative = false;
    public static boolean keystrokes = false;
    public static boolean disableImeInGame = false;
    public static boolean hideSignText = false;
    public static boolean multiplayerAdvancedMode = false;
    public static boolean modifyBrand = true;
    public static boolean modifyChannels = true;
    public static boolean modifyTranslationKeys = true;
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
    public static boolean betterItemSelector = false;
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
    public static boolean versionWarningDisabled = false;
    public static boolean fullMode = false;
    public static String clientName = "PVPUtils";
    public static String updateMirror = "https://ghfast.top/";
    public static String autoGGText = "gg";
    public static String nickHiderNickname = "You";
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
    public static MusicInfoHudMode musicInfoHudMode = MusicInfoHudMode.LITE;
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
    public static boolean pingHud = false;
    public static HudStyle pingHudStyle = HudStyle.LITE;
    public static float pingHudX = 130f;
    public static float pingHudY = 40f;
    public static float pingHudScale = 1.0f;
    public static boolean tpsHud = false;
    public static HudStyle tpsHudStyle = HudStyle.LITE;
    public static float tpsHudX = 130f;
    public static float tpsHudY = 68f;
    public static float tpsHudScale = 1.0f;
    public static boolean clockHud = false;
    public static HudStyle clockHudStyle = HudStyle.LITE;
    public static float clockHudX = 130f;
    public static float clockHudY = 140f;
    public static float clockHudScale = 1.0f;
    public static boolean pingHudBackground = true;
    public static boolean tpsHudBackground = true;
    public static boolean clockHudBackground = true;
    public static boolean clockHudOpaqueBackground = true;
    public static float clockHudBackgroundOpacity = 1.0f;
    public static boolean clockHudBlurBackground = false;
    public static boolean nameTag = false;
    public static float nameTagScale = 1.0f;
    public static boolean nameTagDynamicScale = false;
    public static boolean nameTagOnlyPlayer = false;
    public static boolean nameTagShowSelf = false;
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
    public static int armorTransparencyHead = 50;
    public static int armorTransparencyChest = 50;
    public static int armorTransparencyLegs = 50;
    public static int armorTransparencyFeet = 50;
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
    public static float musicInfoHudX = 0f;
    public static float musicInfoHudY = 0f;
    public static float musicInfoHudScale = 1.0f;
    public static float neteaseMusicVolume = 1.0f;
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
    public static float heldItemMainX = 0f;
    public static float heldItemMainY = 0f;
    public static float heldItemMainZ = 0f;
    public static float heldItemMainRotX = 0f;
    public static float heldItemMainRotY = 0f;
    public static float heldItemMainSwingSpeed = 1f;
    public static int heldItemMainAlpha = 100;
    public static float heldItemOffX = 0f;
    public static float heldItemOffY = 0f;
    public static float heldItemOffZ = 0f;
    public static float heldItemOffRotX = 0f;
    public static float heldItemOffRotY = 0f;
    public static float heldItemOffSwingSpeed = 1f;
    public static int heldItemOffAlpha = 100;
    public static double gammaValue = 15.0;
    public static int fishingRodAssistUseDelay = 4;
    public static int autoGGDelayTicks = 20;
    public static int mainHandAssistSwitchDelayTicks = 2;
    public static int autoChestDepositDepositDelay = 4;
    public static int autoChestDepositCloseDelay = 4;
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
    public enum HitSoundType { NETHERITE, EXPERIENCE, CUSTOM }
    public enum HitSoundCondition { BOTH, MELEE, RANGED }
    public enum TargetHudMode { LITE, BLUR }
    public enum BlockCountDisplayMode { BLUR }
    public enum KeystrokesMode { LITE, BLUR }
    public enum ArmorHudMode { LITE, BLUR }
    public enum ArmorHudLayout { SEPARATED, VERTICAL, HORIZONTAL }
    public enum ArmorHudDisplayMode { PERCENTAGE, BAR, BOTH }
    public enum MusicInfoHudMode { LITE, BLUR }

    public enum MotionBlurAlgorithm { VELOCITY_BASED, FRAME_BLENDING, HYBRID_BLENDING, ACCUMULATION_MAX, ACCUMULATION_MIX }
    public enum HudTheme { DARK, LIGHT }
    public enum WeatherMode { CLEAR, RAIN, SNOW, THUNDER }
    public enum ItemUseStatusMode { LITE, BLUR }
    public enum FreelookTriggerMode { HOLD, TOGGLE }
    public enum HudStyle { LITE, BLUR, MATERIAL }
    public enum MainUIBackgroundMode { GLSL, IMAGE, VIDEO }
    public enum MainUIGlslMode { RANDOM, FIXED }
    public enum DynamicIslandBackground { BLUR, SOLID }
    public enum DynamicIslandMusicMode { LYRICS, MINIMIZED }
    public enum DynamicIslandShape { CAPSULE, SPLIT }

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
    private static boolean configSaveAllowed;
    private static boolean hudSaveAllowed;

    public static synchronized void load() {
        ensureConfigDirectory();
        boolean newConfig = Files.notExists(CONFIG_FILE);
        configSaveAllowed = newConfig || loadJsonConfig(CONFIG_FILE);
        if (newConfig) {
            applyGameLanguageDefault();
        }
        hudSaveAllowed = Files.notExists(HUD_CONFIG_FILE) || loadHudConfig(HUD_CONFIG_FILE);
        normalizeDynamicIslandBlockCountState();
        enforceRestrictedProfile();
        save();
    }

    public static boolean restrictedFeaturesUnlocked() {
        return PROFILE_GATE;
    }

    private static void enforceRestrictedProfile() {
        if (restrictedFeaturesUnlocked()) {
            return;
        }
        fullMode = false;
        mainHandAssist = false;
        mainHandAssistMeleeWeapon = false;
        mainHandAssistShield = false;
        mainHandAssistQuickUse = false;
        elytraAssist = false;
        fireballLandingPredict = false;
        projectileTrajectoryPredict = false;
        fishingRodAssist = false;
    }

    public static synchronized void save() {
        normalizeFakeFps();
        normalizeDynamicIslandAppearance();
        normalizeAppearance();
        ensureConfigDirectory();
        try {
            if (configSaveAllowed) {
                Files.writeString(CONFIG_FILE, GSON.toJson(toJsonConfig()), StandardCharsets.UTF_8);
            }
            if (hudSaveAllowed) {
                Files.writeString(HUD_CONFIG_FILE, GSON.toJson(toHudJsonConfig()), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void normalizeAppearance() {
        mainUIFrameLimit = Math.clamp(mainUIFrameLimit, 30, 240);
        themeSpeed = Float.isFinite(themeSpeed) ? Math.clamp(themeSpeed, 0.1f, 5f) : 1f;
        for (Field field : configFields().values()) {
            if (field.getType() != com.pvp_utils.client.render.HudAppearance.class) continue;
            try {
                com.pvp_utils.client.render.HudAppearance appearance =
                        (com.pvp_utils.client.render.HudAppearance) field.get(null);
                if (appearance == null) {
                    appearance = new com.pvp_utils.client.render.HudAppearance();
                    field.set(null, appearance);
                }
                appearance.normalize();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public static void resetAppearance() {
        themePreset = "BLEND";
        themeAnimated = true;
        themeSpeed = 1f;
        themePrimaryColor = 0xFF4794FD;
        themeSecondaryColor = 0xFF47FDA0;
        themeCustomColors = false;
        mainUIFrameLimitEnabled = true;
        mainUIFrameLimit = 120;
        hitSoundMeleeEnabled = true;
        hitSoundRangedEnabled = true;
        hitSoundMeleeType = HitSoundType.NETHERITE;
        hitSoundRangedType = HitSoundType.NETHERITE;
        hitSoundMeleeFile = "";
        hitSoundRangedFile = "";
        potionStatusStyle = HudStyle.LITE;
        clickGuiCardW = 740f;
        clickGuiCardH = 500f;
        clickGuiFlyoutW = 288f;
        for (Field field : configFields().values()) {
            if (field.getType() != com.pvp_utils.client.render.HudAppearance.class) continue;
            try {
                field.set(null, new com.pvp_utils.client.render.HudAppearance());
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public static int clampFakeFps(int value) {
        return Math.clamp(value, 1, 99999);
    }

    private static void normalizeDynamicIslandAppearance() {
        if (dynamicIslandShape == null) dynamicIslandShape = DynamicIslandShape.CAPSULE;
        if (dynamicIslandBackground == null) {
            dynamicIslandBackground = DynamicIslandBackground.BLUR;
        }
        if (dynamicIslandMusicMode == null) dynamicIslandMusicMode = DynamicIslandMusicMode.LYRICS;
        dynamicIslandShadowStrength = Float.isFinite(dynamicIslandShadowStrength)
                ? Math.clamp(dynamicIslandShadowStrength, 0f, 1f) : 0.35f;
    }

    public static float clampFakeFpsJumpSeconds(float value) {
        return Float.isFinite(value) ? Math.round(Math.clamp(value, 0.1f, 3f) * 10f) / 10f : 0.1f;
    }

    private static void normalizeFakeFps() {
        fakeFpsMin = clampFakeFps(fakeFpsMin);
        fakeFpsMax = Math.max(fakeFpsMin, clampFakeFps(fakeFpsMax));
        fakeFpsJumpMinSeconds = clampFakeFpsJumpSeconds(fakeFpsJumpMinSeconds);
        fakeFpsJumpMaxSeconds = Math.max(fakeFpsJumpMinSeconds, clampFakeFpsJumpSeconds(fakeFpsJumpMaxSeconds));
    }

    private static boolean loadJsonConfig(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonObject()) {
                throw new IOException("Expected a JSON object");
            }
            applyJsonConfig(element.getAsJsonObject());
            return true;
        } catch (Exception e) {
            PVPUtils.LOGGER.error("Failed to load {}; preserving the original file and disabling saves until it is loaded successfully", path, e);
            return false;
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
        JsonObject sound = root.has("HitSound") && root.get("HitSound").isJsonObject()
                ? root.getAsJsonObject("HitSound") : root;
        if (!sound.has("melee-type") && !root.has("hitSoundMeleeType")) hitSoundMeleeType = hitSoundType;
        if (!sound.has("ranged-type") && !root.has("hitSoundRangedType")) hitSoundRangedType = hitSoundType;
        if (!sound.has("melee-enabled") && !root.has("hitSoundMeleeEnabled")) {
            hitSoundMeleeEnabled = hitSoundCondition != HitSoundCondition.RANGED;
        }
        if (!sound.has("ranged-enabled") && !root.has("hitSoundRangedEnabled")) {
            hitSoundRangedEnabled = hitSoundCondition != HitSoundCondition.MELEE;
        }
    }

    private static boolean loadHudConfig(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonObject()) {
                throw new IOException("Expected a JSON object");
            }
            applyHudJsonConfig(element.getAsJsonObject());
            return true;
        } catch (Exception e) {
            PVPUtils.LOGGER.error("Failed to load {}; preserving the original file and disabling saves until it is loaded successfully", path, e);
            return false;
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
            } else if (type == com.pvp_utils.client.render.HudAppearance.class) {
                object.add(key, GSON.toJsonTree(field.get(null)));
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
            } else if (type == com.pvp_utils.client.render.HudAppearance.class) {
                if (!value.isJsonObject()) throw new IllegalArgumentException("Expected an appearance object");
                com.pvp_utils.client.render.HudAppearance appearance =
                        GSON.fromJson(value, com.pvp_utils.client.render.HudAppearance.class);
                appearance.normalize();
                field.set(null, appearance);
            } else if (type.isEnum()) {
                String name = value.getAsString();
                if (("NEW".equals(name) || "LIQUID_GLASS".equals(name) || "LIQUID".equals(name))
                        && (type == TargetHudMode.class || type == KeystrokesMode.class
                        || type == ArmorHudMode.class || type == MusicInfoHudMode.class
                        || type == BlockCountDisplayMode.class || type == ItemUseStatusMode.class
                        || type == HudStyle.class || type == DynamicIslandBackground.class)) name = "BLUR";
                field.set(null, Enum.valueOf((Class<? extends Enum>) type, name));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid value for configuration field " + field.getName(), e);
        }
    }

    private static ConfigPath pathForField(String fieldName) {
        String appearanceModule = switch (fieldName) {
            case "musicInfoAppearance" -> "MusicInfoHUD";
            case "targetAppearance" -> "TargetHUD";
            case "armorAppearance" -> "ArmorHUD";
            case "pingAppearance" -> "PingHUD";
            case "tpsAppearance" -> "TpsHUD";
            case "clockAppearance" -> "ClockHUD";
            case "scoreboardAppearance" -> "BetterScoreboard";
            case "clickGuiAppearance" -> "ClickGUI";
            case "hotbarAppearance" -> "BetterItemSelector";
            case "potionAppearance" -> "PotionStatus";
            case "keystrokesAppearance" -> "Keystrokes";
            case "blockCountAppearance" -> "BlockCountDisplay";
            case "itemUseAppearance" -> "ItemUseStatus";
            case "dynamicIslandAppearance" -> "DynamicIsland";
            case "notificationAppearance" -> "Notification";
            case "lyricsAppearance" -> "LyricsDisplay";
            case "arraylistAppearance" -> "Arraylist";
            default -> null;
        };
        if (appearanceModule != null) return new ConfigPath(appearanceModule, "appearance");
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
            new ModuleRule("Legacy17Animations", "legacy17", "legacy17Animations", "legacy17UseSwing", "legacy17FishingRod", "legacy17HeadRotation", "legacy17HurtTilt", "legacy17ItemPickup"),
            new ModuleRule("SwordBlockingAnimation", "swordBlock", "swordBlock", "offsetX", "offsetY", "offsetZ", "animSpeed", "animationMode"),
            new ModuleRule("AutoBlock", "autoMode", "autoMode", "range"),
            new ModuleRule("RemoveAttackCooldownAnimation", "noAttackCooldownAnimation", "noAttackCooldownAnimation"),
            new ModuleRule("PseudoCreative", "pseudoCreative", "pseudoCreative"),
            new ModuleRule("SneakAnimationAdjustment", "noSneakAnimation", "noSneakAnimation", "sneakDropScale", "sneakAnimationSpeed"),
            new ModuleRule("AutoScreenshot", "autoScreenshot", "autoScreenshot"),
            new ModuleRule("AutoGG", "autoGG", "autoGG"),
            new ModuleRule("ServerAutoLogin", "serverAutoLogin", "serverAutoLogin", "serverAutoLoginRules", "serverAutoLoginMode"),
            new ModuleRule("FoodInfo", "foodInfo", "foodInfo"),
            new ModuleRule("LowHealthWarning", "lowHealthNotify", "lowHealthNotify"),
            new ModuleRule("DamageNumbers", "damageNumbers", "damageNumbers"),
            new ModuleRule("AttackReachDisplay", "attackReachDisplay", "attackReachDisplay"),
            new ModuleRule("FallDamagePrediction", "fallDamagePredict", "fallDamagePredict"),
            new ModuleRule("FireballLandingPrediction", "fireballLandingPredict", "fireballLandingPredict"),
            new ModuleRule("ProjectileTrajectoryPrediction", "projectileTrajectoryPredict", "projectileTrajectoryPredict", "projectileTrajectoryPredictBow", "projectileTrajectoryPredictSnowball", "projectileTrajectoryPredictEnderPearl", "projectileTrajectoryPredictBlock", "projectileTrajectoryPredictEntity", "projectileTrajectoryPredictEntityMovement", "projectileTrajectoryPredictOtherPlayers"),
            new ModuleRule("VictorySound", "victorySound", "victorySound"),
            new ModuleRule("FakeFPS", "fakeFps", "fakeFps"),
            new ModuleRule("GammaOverride", "gammaOverride", "gammaOverride", "gammaValue"),
            new ModuleRule("AutoSprint", "autoSprint", "autoSprint"),
            new ModuleRule("NoSwimming", "noSwimming", "noSwimming"),
            new ModuleRule("NickHider", "nickHider", "nickHider"),
            new ModuleRule("SunWorldRankSpoof", "sunWorldRankSpoof", "sunWorldRankSpoof", "sunWorldRankVip1", "sunWorldRankVip2", "sunWorldRankVip3", "sunWorldRankVip4", "sunWorldRankSvip", "sunWorldRankCreator", "sunWorldRankGuide", "sunWorldRankAdmin"),
            new ModuleRule("HeldItemPosition", "heldItem", "heldItemPosition"),
            new ModuleRule("FishingRodAssist", "fishingRodAssist", "fishingRodAssist"),
            new ModuleRule("BlockCountDisplay", "blockCountDisplay", "blockCountDisplay"),
            new ModuleRule("BetterPingDisplay", "betterPingDisplay", "betterPingDisplay"),
            new ModuleRule("LyricsDisplay", "lyricsDisplay", "lyricsDisplay"),
            new ModuleRule("MusicInfoHUD", "musicInfoHud", "musicInfoHud", "musicInfoHudCoverRounded"),
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
            new ModuleRule("ArmorTransparency", "armorTransparency", "armorTransparency", "armorTransparencyHead", "armorTransparencyChest", "armorTransparencyLegs", "armorTransparencyFeet", "armorTransparencyShowInCombat"),
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
            new ModuleRule("BetterItemSelector", "betterItemSelector", "betterItemSelector"),
            new ModuleRule("BetterMouseLogic", "betterMouseLogic", "betterMouseLogic"),
            new ModuleRule("SmoothHotbarScrolling", "smoothHotbar", "smoothHotbarScrolling", "hotbarRollover"),
            new ModuleRule("MainUI", "mainUI", "useMainUI", "mainUIBackgroundImage", "mainUIGlslShader"),
            new ModuleRule("TargetHUD", "targetHud", "targetHud"),
            new ModuleRule("NameTags", "nameTag", "nameTag", "nameTagShowSelf"),
            new ModuleRule("DynamicMotionBlur", "dynamicMotionBlur", "dynamicMotionBlur", "motionBlurAlgorithm"),
            new ModuleRule("HUDTheme", "hud", "", "skiaBlurStrength"),
            new ModuleRule("ClickGuiScale", "clickGui", "", "clickGuiScale", "clickGuiScrollSpeed", "clickGuiCardW", "clickGuiCardH", "clickGuiFlyoutW"),
            new ModuleRule("Notification", "notification", ""),
    };

    private record ModuleRule(String module, String prefix, String toggleField, String... extraFields) {}
    private record ConfigPath(String module, String key) {}
    private record HudComponent(String name, String prefix, String... fields) {}

    private static final HudComponent[] HUD_COMPONENTS = new HudComponent[] {
            new HudComponent("TargetHUD", "targetHud", "targetHudX", "targetHudY", "targetHudZ", "targetHudScale"),
            new HudComponent("Keystrokes", "keystrokes", "keystrokesX", "keystrokesY", "keystrokesScale"),
            new HudComponent("PingHUD", "pingHud", "pingHudStyle", "pingHudBackground", "pingHudX", "pingHudY", "pingHudScale"),
            new HudComponent("TpsHUD", "tpsHud", "tpsHudStyle", "tpsHudBackground", "tpsHudX", "tpsHudY", "tpsHudScale"),
            new HudComponent("ClockHUD", "clockHud", "clockHudStyle", "clockHudBackground", "clockHudOpaqueBackground", "clockHudBackgroundOpacity", "clockHudBlurBackground", "clockHudX", "clockHudY", "clockHudScale"),
            new HudComponent("BlockCountDisplay", "blockCountDisplay", "blockCountDisplayX", "blockCountDisplayY", "blockCountDisplayScale"),
            new HudComponent("ArmorHUD", "armorHud", "armorHudX", "armorHudY", "armorHudScale"),
            new HudComponent("ItemUseStatus", "itemUseStatus", "itemUseStatusX", "itemUseStatusY", "itemUseStatusScale"),
            new HudComponent("DynamicIsland", "dynamicIsland", "dynamicIslandX", "dynamicIslandY", "dynamicIslandScale"),
            new HudComponent("Notification", "notification", "notificationX", "notificationY", "notificationScale"),
            new HudComponent("PotionStatus", "potionStatus", "potionStatusX", "potionStatusY", "potionStatusScale"),
            new HudComponent("LyricsDisplay", "lyricsDisplay", "lyricsDisplayX", "lyricsDisplayY", "lyricsDisplayScale"),
            new HudComponent("MusicInfoHUD", "musicInfoHud", "musicInfoHudX", "musicInfoHudY", "musicInfoHudScale"),
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
