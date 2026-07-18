package com.pvp_utils.client.web;

import com.pvp_utils.Config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

final class WebGUIModules {
    private static final List<Category> CATEGORIES = List.of(
            new Category("Combat", "战斗", List.of(
                    module("AutoMainHand", "主手辅助", "Automatically switch the main-hand item at suitable moments.", "在合适的时候自动切换主手手持的物品", "mainHandAssist",
                            check("mainHandAssistMeleeWeapon", "Melee Weapon Assist", "近战武器辅助"),
                            check("mainHandAssistShield", "Shield", "盾牌"),
                            check("mainHandAssistQuickUse", "Quick Use", "快速使用"),
                            check("mainHandAssistSwitchBack", "Switch Back After Use", "执行完毕后切回原先槽位"),
                            slider("mainHandAssistSwitchDelayTicks", "Switch Delay (tick)", "切换延迟(tick)", 2, 20, 1)),
                    module("Elytra Improvements", "鞘翅改进", "Make elytra usage more convenient.", "让鞘翅的使用更加便捷", "elytraAssist",
                            check("elytraAutoDeploy", "Auto Deploy Elytra", "自动展开鞘翅"),
                            check("elytraAutoFirework", "Auto Firework Boost", "自动烟花推进")),
                    module("Hit Marker", "命中标记", "Show a marker when you hit a target.", "在命中时显示标记", "hitMarker"),
                    module("Hit Sound", "命中提示音", "Play a sound when you hit a target.", "命中目标时播放音效", "hitSound",
                            select("hitSoundType", "Sound Type", "提示音类型"),
                            select("hitSoundCondition", "Trigger Timing", "播放时机"))
            )),
            new Category("Render", "视觉", List.of(
                    module("Sword Blocking Animation", "防砍动画", "Simulate the old blocking animation.", "模拟旧版本格挡动画效果", "swordBlock",
                            select("animationMode", "Animation Mode", "动画模式"),
                            slider("offsetX", "Offset X", "偏移 X", -1, 1, 0.01),
                            slider("offsetY", "Offset Y", "偏移 Y", -1, 1, 0.01),
                            slider("offsetZ", "Offset Z", "偏移 Z", -1, 1, 0.01),
                            slider("animSpeed", "Animation Speed", "动画速度", 0, 4, 0.01)),
                    module("Remove Attack Cooldown Animation", "去除攻击冷却动画", "Remove the extra hand raise after weapon swings.", "去除武器挥动后的额外抬手动画", "noAttackCooldownAnimation"),
                    module("Motion camera", "运动相机", "Better third-person view.", "更好的第三人称视角", "motionCamera",
                            slider("motionCameraFollowSpeed", "Follow Speed", "跟随速度", 0, 1.0, 0.01),
                            slider("motionCameraDistance", "Camera Distance", "相机距离", 1, 8, 0.1)),
                    module("Attack Effects", "攻击特效", "Control particles shown when attacking.", "控制攻击时显示的粒子效果", "attackEffectsCritParticles",
                            check("attackEffectsCritParticles", "Crit Particles", "暴击粒子"),
                            slider("attackEffectsCritMultiplier", "Crit Multiplier", "暴击粒子倍数", 1, 10, 0.1, "attackEffectsCritParticles"),
                            check("attackEffectsSharpnessParticles", "Sharpness Particles", "锋利粒子"),
                            slider("attackEffectsSharpnessMultiplier", "Sharpness Multiplier", "锋利粒子倍数", 1, 10, 0.1, "attackEffectsSharpnessParticles"),
                            check("attackEffectsFlameParticles", "Flame Particles", "火焰粒子"),
                            slider("attackEffectsFlameMultiplier", "Flame Multiplier", "火焰粒子倍数", 1, 10, 0.1, "attackEffectsFlameParticles"),
                            check("attackEffectsBloodParticles", "Blood Particles", "血液粒子"),
                            slider("attackEffectsBloodMultiplier", "Blood Multiplier", "血液粒子倍数", 1, 10, 0.1, "attackEffectsBloodParticles"),
                            check("attackEffectsLightning", "Lightning", "闪电"),
                            slider("attackEffectsLightningCount", "Lightning Count", "闪电数量", 1, 5, 1, "attackEffectsLightning")),
                    module("Hit Color", "更改受击颜色", "Change the color shown when entities are hit.", "更改实体受击时的颜色", "hitColor",
                            slider("hitColorRed", "R", "R", 0, 255, 1),
                            slider("hitColorGreen", "G", "G", 0, 255, 1),
                            slider("hitColorBlue", "B", "B", 0, 255, 1),
                            slider("hitColorAlpha", "Alpha", "透明度", 0, 255, 1)),
                    module("Custom Block Outline", "自定义方块轮廓", "Customize the outline of the block under the crosshair.", "自定义准星指向方块的轮廓样式", "customBlockOutline",
                            slider("customBlockOutlineWidth", "Border Width", "边框粗细", 1, 4, 0.1),
                            slider("customBlockOutlineRed", "Border R", "边框 R", 0, 255, 1),
                            slider("customBlockOutlineGreen", "Border G", "边框 G", 0, 255, 1),
                            slider("customBlockOutlineBlue", "Border B", "边框 B", 0, 255, 1),
                            slider("customBlockOutlineAlpha", "Border Alpha", "边框透明度", 0, 255, 1),
                            check("customBlockOutlineFill", "Fill", "填充"),
                            slider("customBlockOutlineFillRed", "Fill R", "填充 R", 0, 255, 1, "customBlockOutlineFill"),
                            slider("customBlockOutlineFillGreen", "Fill G", "填充 G", 0, 255, 1, "customBlockOutlineFill"),
                            slider("customBlockOutlineFillBlue", "Fill B", "填充 B", 0, 255, 1, "customBlockOutlineFill"),
                            slider("customBlockOutlineFillAlpha", "Fill Alpha", "填充透明度", 0, 255, 1, "customBlockOutlineFill"),
                            check("customBlockOutlineAnimation", "Animation Improvements", "动画改进"),
                            slider("customBlockOutlineAnimationSpeed", "Enter/Exit Speed", "进入/退出速度", 1, 20, 0.1, "customBlockOutlineAnimation"),
                            slider("customBlockOutlineMoveSpeed", "Move Speed", "移动速度", 1, 20, 0.1, "customBlockOutlineAnimation")),
                    module("Rainbow Enchantment Glint", "彩虹附魔光效", "Change the enchantment glint to rainbow colors.", "将附魔光效更改为彩虹色", "customEnchantmentGlint"),
                    module("Auto Block", "自动格挡", "Automatically trigger blocking.", "自动触发格挡动作", "autoMode",
                            slider("range", "Trigger Range", "触发距离", 2, 6, 0.01)),
                    module("Use Animation", "使用动画", "Enable item use animation.", "启用物品使用动画", "useSwing"),
                    module("Digging Status", "挖掘状态显示", "Show current digging progress under the crosshair.", "在准星下方显示当前挖掘进度", "diggingStatus"),
                    module("Better Ping Display", "更好的延迟显示", "Show latency as numbers in the player list.", "在玩家列表中用数字显示延迟", "betterPingDisplay"),
                    module("Lyrics Display", "歌词显示", "Show lyrics for the currently playing music.", "显示当前播放音乐的歌词", "lyricsDisplay"),
                    module("Music Info HUD", "音乐信息显示", "Show current Netease Music playback information.", "显示当前播放的网易云音乐信息", "musicInfoHud"),
                    module("Item Use Status", "物品使用状态显示", "Show current item use progress or status on the screen.", "在屏幕上显示当前物品使用进度或状态", "itemUseStatus",
                            select("itemUseStatusMode", "Mode", "模式")),
                    module("Arraylist", "功能列表", "Show currently enabled modules on the HUD.", "在HUD上显示当前启用的功能。", "arraylist",
                            slider("arraylistColorRed", "R", "R", 0, 255, 1),
                            slider("arraylistColorGreen", "G", "G", 0, 255, 1),
                            slider("arraylistColorBlue", "B", "B", 0, 255, 1),
                            check("arraylistGradient", "Gradient", "渐变"),
                            slider("arraylistGradientRed", "R2", "R2", 0, 255, 1, "arraylistGradient"),
                            slider("arraylistGradientGreen", "G2", "G2", 0, 255, 1, "arraylistGradient"),
                            slider("arraylistGradientBlue", "B2", "B2", 0, 255, 1, "arraylistGradient"),
                            slider("arraylistGradientSpeed", "Gradient Speed", "渐变速度", 0, 5, 0.1, "arraylistGradient"),
                            check("arraylistBorder", "Border", "边框"),
                            slider("arraylistBorderWidth", "Border Width", "边框粗细", 1, 4, 0.1, "arraylistBorder")),
                    module("Dynamic Island", "灵动岛", "Add a Dynamic Island component to the HUD.", "在界面上添加灵动岛组件", "dynamicIsland",
                            check("dynamicIslandBlockCount", "Block Count Display", "方块数量显示"),
                            check("dynamicIslandBlockCountAltIcon", "???", "???", "dynamicIslandBlockCount"),
                            check("dynamicIslandItemUseStatus", "Item Use Status", "物品使用状态"),
                            check("dynamicIslandLowHealthWarning", "Low Health Warning", "低血量提示")),
                    module("Item Physics", "物品物理掉落", "Make dropped items fall in a more physical way.", "让掉落物以更加物理的方式掉落", "itemPhysics",
                            slider("itemPhysicsRotationSpeed", "Rotation Speed", "旋转速度", 0, 3, 0.1)),
                    module("Dropped Item 2D Render", "掉落物2D渲染", "Change dropped item rendering to old 2D style.", "将掉落物的渲染方式更改为2D渲染", "item2DRender"),
                    module("Armor HUD", "盔甲 HUD", "Show equipped armor and durability beside the hotbar.", "在快捷栏两侧显示当前装备和耐久", "armorHud",
                            select("armorHudMode", "Mode", "模式"),
                            select("armorHudLayout", "Layout", "布局"),
                            select("armorHudDisplayMode", "Display Mode", "显示模式"),
                            check("armorHudShowPercentage", "Show Percentage", "显示百分比"),
                            check("armorHudShowBar", "Show Bar", "显示耐久条")),
                    module("Potion Status", "药水状态", "Show active potion effects and remaining time.", "显示当前药水效果和剩余时间", "potionStatus",
                            check("potionStatusBackground", "Gray Background", "灰色遮罩"),
                            check("potionStatusCountdown", "Countdown Text", "倒计时数字"),
                            check("potionStatusHideVanilla", "Hide Vanilla Effects", "屏蔽原版显示")),
                    module("Sneak Animation Adjustment", "潜行动画调整", "Adjust sneak camera drop effect.", "调整潜行视角下降效果", "noSneakAnimation",
                            slider("sneakDropScale", "Drop Amount", "下降幅度", 0, 1, 0.01),
                            slider("sneakAnimationSpeed", "Transition Speed", "过渡速度", 0, 1, 0.01)),
                    module("Gamma Override", "伽马覆写", "Force a custom brightness value.", "强制使用自定义亮度值", "gammaOverride",
                            slider("gammaValue", "Gamma Value", "伽马值", 0, 15, 0.1)),
                    module("Dynamic Motion Blur", "动态模糊", "Apply velocity blur based on camera motion.", "根据相机运动生成速度模糊效果", "dynamicMotionBlur",
                            select("motionBlurAlgorithm", "Algorithm", "算法"),
                            slider("dynamicMotionBlurStrength", "Strength", "强度", 0, 3, 0.01),
                            check("dynamicMotionBlurRefreshRateScaling", "Refresh Rate Scaling", "刷新率缩放")),
                    module("Render Control", "渲染控制", "Selectively disable in-game rendering effects.", "选择性关闭游戏内渲染效果", "hideFireOverlay",
                            check("hideSignText", "Sign Text", "告示牌文本"),
                            check("hideEnchantTableBook", "Enchanting Table Book", "附魔台悬浮书"),
                            check("hideFireOverlay", "Fire Overlay", "火焰遮挡"),
                            check("hideVignette", "Vignette", "屏幕暗角"),
                            check("hideFog", "View Fog", "视角迷雾"),
                            check("hideTotemAnimation", "Totem Animation", "图腾动画"),
                            check("hideExplosionParticles", "Explosion Particles", "爆炸粒子"),
                            check("hideRainParticles", "Rain Particles", "雨滴粒子"),
                            check("hideBossBar", "Boss Bar", "Boss 血条"),
                            check("hideHurtShake", "Hurt Shake", "受伤抖动")),
                    module("Low Health Warning", "低血量提示", "Show a warning when health is low.", "血量过低时显示警告", "lowHealthNotify"),
                    module("Damage Numbers", "伤害数值显示", "Show target health changes.", "显示目标血量变化", "damageNumbers"),
                    module("Target HUD", "目标 HUD", "Show target information panel.", "显示目标信息面板", "targetHud",
                            check("attackReachDisplay", "Attack Reach", "攻击距离"),
                            select("targetHudMode", "Mode", "模式")),
                    module("Keystrokes", "按键显示", "Show WASD and mouse button states.", "显示 WASD 和鼠标按键状态", "keystrokes",
                            select("keystrokesMode", "Mode", "模式")),
                    module("Name Tags", "名称标签", "Adjust vanilla entity name tag rendering.", "调整原版实体名称标签显示效果", "nameTag",
                            slider("nameTagScale", "Scale", "缩放", 0.5, 3, 0.01),
                            check("nameTagDynamicScale", "Dynamic Scale", "动态缩放"),
                            check("nameTagOnlyPlayer", "Only Player", "仅玩家"))
            )),
            new Category("Tool", "工具", List.of(
                    module("Auto Screenshot", "自动截图", "Automatically take a screenshot when you win.", "在你胜利时自动截图", "autoScreenshot"),
                    module("Auto GG", "自动GG", "Automatically send text after winning.", "在获胜后自动发送文字", "autoGG",
                            slider("autoGGDelayTicks", "Send Delay (tick)", "发送延迟(tick)", 0, 100, 1)),
                    module("Food Info", "食物信息显示", "Show food-related information.", "显示食物相关信息", "foodInfo"),
                    module("Fall Damage Prediction", "摔落伤害预测", "Predict fall damage value.", "预测摔落伤害数值", "fallDamagePredict"),
                    module("Fireball Landing Prediction", "火焰弹落点预测", "Show the trajectory and predicted impact point of large fireballs.", "显示大型火焰弹的轨迹和预测落点", "fireballLandingPredict"),
                    module("Projectile Trajectory Prediction", "投掷物与弓箭轨迹预测", "Predict the flight trajectory and impact point of arrows, snowballs and ender pearls.", "预测弓箭、雪球和末影珍珠的飞行轨迹与落点", "projectileTrajectoryPredict",
                            check("projectileTrajectoryPredictBow", "Bow Trajectory Prediction", "弓箭轨迹预测"),
                            check("projectileTrajectoryPredictSnowball", "Snowball Trajectory Prediction", "雪球轨迹预测"),
                            check("projectileTrajectoryPredictEnderPearl", "Ender Pearl Trajectory Prediction", "末影珍珠轨迹预测"),
                            check("projectileTrajectoryPredictBlock", "Block Impact Prediction", "落点方块预测"),
                            check("projectileTrajectoryPredictEntity", "Entity Impact Prediction", "落点实体预测"),
                            check("projectileTrajectoryPredictEntityMovement", "Entity Movement Prediction", "实体运动预测", "projectileTrajectoryPredictEntity"),
                            check("projectileTrajectoryPredictOtherPlayers", "Other Player Arrow Warning", "其他玩家箭矢预警")),
                    module("Auto Sprint", "自动疾跑", "Automatically sprint while moving forward.", "前进时自动进入疾跑状态", "autoSprint"),
                    module("No Swimming", "禁止游泳", "Prevent entering the swimming state in water, which can avoid setbacks on some older servers.", "在水里时禁止进入游泳状态，这在某些低版本服务器上可避免回弹", "noSwimming"),
                    module("Remove Container Background", "去除容器半透明背景", "Remove the translucent background from inventory and container screens.", "去除背包和容器界面的半透明背景", "removeContainerBackground"),
                    module("Fishing Rod Assist", "钓鱼竿辅助", "Automatically right-click when switching to a fishing rod.", "切换到钓鱼竿时自动右键使用", "fishingRodAssist",
                            slider("fishingRodAssistUseDelay", "Use Delay (tick)", "使用间隔(tick)", 0, 20, 1)),
                    module("Block Count Display", "方块数量显示", "Show block count, placement speed, and click speed while right-clicking blocks.", "右键放置方块时显示方块数量、放置速度和点击速度", "blockCountDisplay",
                            select("blockCountDisplayMode", "Mode", "模式")),
                    module("Time Change", "改变客户端时间", "Force the client-side world time.", "强制修改客户端显示的世界时间", "timeChange",
                            slider("clientTime", "Time", "时间", 0, 23999, 1)),
                    module("Weather Change", "改变客户端天气", "Force the client-side weather.", "强制修改客户端显示的天气", "weatherChange",
                            select("weatherMode", "Weather Mode", "天气模式")),
                    module("Freelook", "自由视角", "Look around freely without changing your original view direction.", "自由观看四周而不影响原本视角朝向", "freelook",
                            select("freelookTriggerMode", "Trigger Mode", "触发模式"),
                            slider("freelookSensitivity", "Sensitivity", "灵敏度", 1, 100, 1)),
                    module("Zoom", "缩放", "Use a keybind to zoom. The key can be changed in controls.", "使用快捷键进行缩放，在设置中可以调整键位", "zoom",
                            slider("zoomAmount", "Zoom Amount", "缩放倍率", 2, 20, 1),
                            check("zoomScroll", "Scroll Zoom", "滚轮缩放"),
                            slider("zoomScrollSteps", "Scroll Steps", "滚轮档位", 1, 20, 1, "zoomScroll"),
                            slider("zoomPerStep", "Step Multiplier", "每档倍率", 110, 250, 1, "zoomScroll"),
                            slider("zoomInTime", "Zoom In Time", "进入时间", 0, 1.5, 0.01),
                            slider("zoomOutTime", "Zoom Out Time", "退出时间", 0, 1.5, 0.01),
                            slider("zoomRelativeSensitivity", "Zoom Camera Sensitivity", "缩放相机灵敏度", 0, 100, 1)),
                    module("Custom Cape", "自定义披风", "Load a local custom cape file.", "加载本地的自定义披风文件", "customCape"),
                    module("Quick Deposit", "快捷存入", "Quickly deposit the held item into a container.", "快捷存入手中的物品", "autoChestDeposit",
                            check("autoChestDepositResourcesOnly", "Resources Only", "仅限资源"),
                            slider("autoChestDepositDepositDelay", "Deposit Delay", "存入延迟", 0, 40, 1),
                            slider("autoChestDepositCloseDelay", "Close Container Delay", "关闭容器延迟", 0, 40, 1))
            )),
            new Category("Optimize", "优化", List.of(
                    module("IME Fix In Game", "游戏内输入法修复", "Fix CJK input methods causing controls to stop working in game.", "修复中文、日文、韩文输入法在游戏中导致无法操作的问题", "disableImeInGame"),
                    module("Better Chat", "更好的聊天栏", "Optimizations and improvements for the chat bar.", "对于聊天栏的优化与改进", "betterChat",
                            check("betterChatMessageAnimation", "Message Animation", "消息入场动画"),
                            check("betterChatInputAnimation", "Input Bar Animation", "输入栏动画"),
                            check("betterChatAvatar", "Chat Heads", "聊天头像"),
                            slider("betterChatMessageFadeTime", "Message Fade Time", "消息动画时间", 100, 900, 1),
                            slider("betterChatInputFadeTime", "Input Fade Time", "输入栏动画时间", 100, 900, 1)),
                    module("Better Scoreboard", "更好的计分板", "Allows moving and scaling the scoreboard in the HUD editor.", "开启后可在 HUD 编辑器中拖动和缩放计分板", "betterScoreboard",
                            check("betterScoreboardHideScores", "Hide Red Numbers", "隐藏红色数字"),
                            check("betterScoreboardVisualImprovement", "Visual Improvement", "视觉改进")),
                    module("Better Mouse Logic", "更好的鼠标逻辑", "Improve inventory mouse behavior.", "改进物品栏内鼠标指针的工作逻辑", "betterMouseLogic"),
                    module("Smooth Hotbar", "平滑快捷栏", "Make hotbar scrolling smooth.", "让快捷栏滚轮切换时带有平滑过渡", "smoothHotbarScrolling",
                            slider("smoothHotbarAnimationSpeed", "Animation Speed", "动画速度", 0.05, 0.99, 0.01))
            )),
            new Category("Misc", "其他", List.of(
                    module("Main UI", "主界面", "Use the custom PVPUtils main menu.", "使用 PVPUtils 自定义主界面", "useMainUI",
                            select("mainUIBackgroundMode", "Background Mode", "背景模式"),
                            select("mainUIGlslMode", "GLSL Mode", "GLSL 模式"),
                            check("mainUICustomBackground", "Custom Background", "自定义背景"),
                            check("mainUIMouseEffect", "Mouse Effect", "鼠标效果")),
                    settingsModule("HUD Theme", "HUD 主题", "Control the shared HUD theme and blur style.", "控制 HUD 通用主题和模糊风格",
                            select("hudTheme", "Theme", "主题"),
                            slider("skiaBlurStrength", "Blur Strength", "模糊强度", 0, 5, 0.05),
                            select("musicInfoHudMode", "Music Info HUD", "音乐信息显示")),
                    module("Victory Sound", "胜利音效", "Play a custom sound when you win.", "在你胜利时播放自定义音效", "victorySound")
            ))
    );

    private WebGUIModules() {
    }

    static String categoriesJson() {
        StringBuilder json = new StringBuilder("{\"success\":true,\"result\":[");
        for (int i = 0; i < CATEGORIES.size(); i++) {
            if (i > 0) json.append(',');
            Category category = CATEGORIES.get(i);
            json.append('{')
                    .append("\"id\":\"").append(escape(category.id)).append("\",")
                    .append("\"name\":\"").append(escape(Config.isChinese ? category.zh : category.id)).append("\"")
                    .append('}');
        }
        if (!CATEGORIES.isEmpty()) json.append(',');
        json.append("{\"id\":\"Command\",\"name\":\"")
                .append(escape(Config.isChinese ? "命令" : "Command"))
                .append("\"}");
        return json.append("]}").toString();
    }

    static String modulesJson(String categoryId) {
        Category category = findCategory(categoryId);
        if (category == null) {
            return "{\"success\":false,\"reason\":\"Unknown category\"}";
        }
        StringBuilder json = new StringBuilder("{\"success\":true,\"result\":[");
        for (int i = 0; i < category.modules.size(); i++) {
            if (i > 0) json.append(',');
            Module module = category.modules.get(i);
            json.append('{')
                    .append("\"id\":\"").append(escape(module.id)).append("\",")
                    .append("\"name\":\"").append(escape(module.name())).append("\",")
                    .append("\"desc\":\"").append(escape(module.desc())).append("\",")
                    .append("\"state\":").append(readState(module)).append(',')
                    .append("\"toggleable\":").append(module.toggleField != null).append(',')
                    .append("\"settings\":").append(hasVisibleSettings(module))
                    .append('}');
        }
        return json.append("]}").toString();
    }

    static String setStatusJson(String moduleId, boolean state) {
        Module module = findModule(moduleId);
        if (module == null) {
            return "{\"success\":false,\"reason\":\"Unknown module\"}";
        }
        try {
            if (module.toggleField == null) {
                return "{\"success\":false,\"reason\":\"This module has no toggle\"}";
            }
            Field field = Config.class.getField(module.toggleField);
            if (field.getType() != boolean.class) {
                return "{\"success\":false,\"reason\":\"Unsupported field\"}";
            }
            setBooleanConfig(module.toggleField, state);
            Config.save();
            return "{\"success\":true,\"result\":" + field.getBoolean(null) + "}";
        } catch (ReflectiveOperationException e) {
            return "{\"success\":false,\"reason\":\"Failed to update config\"}";
        }
    }

    static String settingsJson(String moduleId) {
        Module module = findModule(moduleId);
        if (module == null) {
            return "{\"success\":false,\"reason\":\"Unknown module\"}";
        }
        StringBuilder json = new StringBuilder("{\"success\":true,\"result\":[");
        List<Setting> visible = visibleSettings(module);
        for (int i = 0; i < visible.size(); i++) {
            if (i > 0) json.append(',');
            Setting setting = visible.get(i);
            json.append('{')
                    .append("\"type\":\"").append(setting.type).append("\",")
                    .append("\"name\":\"").append(escape(setting.field)).append("\",")
                    .append("\"displayName\":\"").append(escape(setting.name())).append("\",")
                    .append("\"value\":").append(settingValueJson(setting));
            if (setting.type == SettingType.SLIDER) {
                json.append(',').append("\"min\":").append(setting.min)
                        .append(',').append("\"max\":").append(setting.max)
                        .append(',').append("\"step\":").append(setting.step);
            } else if (setting.type == SettingType.SELECTION) {
                json.append(',').append("\"values\":").append(enumValuesJson(setting.field));
            }
            json.append('}');
        }
        return json.append("]}").toString();
    }

    static String setSettingJson(String moduleId, String fieldName, String value) {
        Module module = findModule(moduleId);
        if (module == null) {
            return "{\"success\":false,\"reason\":\"Unknown module\"}";
        }
        Setting setting = null;
        for (Setting candidate : module.settings) {
            if (candidate.field.equals(fieldName)) {
                setting = candidate;
                break;
            }
        }
        if (setting == null || !isVisible(setting)) {
            return "{\"success\":false,\"reason\":\"Unknown setting\"}";
        }
        try {
            Field field = Config.class.getField(setting.field);
            if (setting.type == SettingType.CHECKBOX && field.getType() == boolean.class) {
                setBooleanConfig(setting.field, Boolean.parseBoolean(value));
            } else if (setting.type == SettingType.SLIDER) {
                double parsed = clamp(Double.parseDouble(value), setting.min, setting.max);
                if (field.getType() == int.class) {
                    field.setInt(null, (int) Math.round(parsed));
                } else if (field.getType() == float.class) {
                    field.setFloat(null, (float) parsed);
                } else if (field.getType() == double.class) {
                    field.setDouble(null, parsed);
                } else {
                    return "{\"success\":false,\"reason\":\"Unsupported slider field\"}";
                }
            } else if (setting.type == SettingType.SELECTION && field.getType().isEnum()) {
                Object[] constants = field.getType().getEnumConstants();
                int index = parseSelectionIndex(value, constants);
                field.set(null, constants[Math.max(0, Math.min(constants.length - 1, index))]);
            } else {
                return "{\"success\":false,\"reason\":\"Unsupported setting\"}";
            }
            Config.save();
            return "{\"success\":true,\"result\":" + settingValueJson(setting) + "}";
        } catch (Exception e) {
            return "{\"success\":false,\"reason\":\"Failed to update setting\"}";
        }
    }

    private static boolean readState(Module module) {
        try {
            if (module.toggleField == null) return false;
            Field field = Config.class.getField(module.toggleField);
            return field.getType() == boolean.class && field.getBoolean(null);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static void setBooleanConfig(String fieldName, boolean value) throws ReflectiveOperationException {
        switch (fieldName) {
            case "dynamicIsland" -> Config.setDynamicIsland(value);
            case "dynamicIslandBlockCount" -> Config.setDynamicIslandBlockCount(value);
            case "blockCountDisplay" -> Config.setBlockCountDisplay(value);
            case "dynamicIslandItemUseStatus" -> Config.setDynamicIslandItemUseStatus(value);
            case "itemUseStatus" -> Config.setItemUseStatus(value);
            case "motionCamera" -> Config.setMotionCamera(value);
            case "itemPhysics" -> {
                Config.itemPhysics = value;
                if (value) Config.item2DRender = false;
            }
            case "item2DRender" -> {
                Config.item2DRender = value;
                if (value) Config.itemPhysics = false;
            }
            case "lowHealthNotify" -> {
                Config.lowHealthNotify = value;
                if (value) Config.dynamicIslandLowHealthWarning = false;
            }
            case "dynamicIslandLowHealthWarning" -> {
                Config.dynamicIslandLowHealthWarning = value;
                if (value) Config.lowHealthNotify = false;
            }
            default -> Config.class.getField(fieldName).setBoolean(null, value);
        }
    }

    private static boolean hasVisibleSettings(Module module) {
        return !visibleSettings(module).isEmpty();
    }

    private static List<Setting> visibleSettings(Module module) {
        List<Setting> visible = new ArrayList<>();
        for (Setting setting : module.settings) {
            if (isVisible(setting)) {
                visible.add(setting);
            }
        }
        return visible;
    }

    private static boolean isVisible(Setting setting) {
        if (setting.visibleField == null || setting.visibleField.isBlank()) {
            return true;
        }
        try {
            Field field = Config.class.getField(setting.visibleField);
            return field.getType() == boolean.class && field.getBoolean(null);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static String settingValueJson(Setting setting) {
        try {
            Field field = Config.class.getField(setting.field);
            if (field.getType() == boolean.class) {
                return Boolean.toString(field.getBoolean(null));
            }
            if (field.getType() == int.class) {
                return Integer.toString(field.getInt(null));
            }
            if (field.getType() == float.class) {
                return Float.toString(field.getFloat(null));
            }
            if (field.getType() == double.class) {
                return Double.toString(field.getDouble(null));
            }
            if (field.getType().isEnum()) {
                return "\"" + escape(((Enum<?>) field.get(null)).name()) + "\"";
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return "null";
    }

    private static String enumValuesJson(String fieldName) {
        try {
            Field field = Config.class.getField(fieldName);
            if (!field.getType().isEnum()) return "[]";
            StringBuilder json = new StringBuilder("[");
            Object[] constants = field.getType().getEnumConstants();
            for (int i = 0; i < constants.length; i++) {
                if (i > 0) json.append(',');
                json.append('"').append(escape(((Enum<?>) constants[i]).name())).append('"');
            }
            return json.append(']').toString();
        } catch (ReflectiveOperationException e) {
            return "[]";
        }
    }

    private static int parseSelectionIndex(String value, Object[] constants) {
        for (int i = 0; i < constants.length; i++) {
            if (((Enum<?>) constants[i]).name().equals(value)) return i;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Category findCategory(String id) {
        for (Category category : CATEGORIES) {
            if (category.id.equals(id)) return category;
        }
        return null;
    }

    private static Module findModule(String id) {
        for (Category category : CATEGORIES) {
            for (Module module : category.modules) {
                if (module.id.equals(id)) return module;
            }
        }
        return null;
    }

    private static Module module(String en, String zh, String enDesc, String zhDesc, String toggleField, Setting... settings) {
        return new Module(toId(en), en, zh, enDesc, zhDesc, toggleField, List.of(settings));
    }

    private static Module settingsModule(String en, String zh, String enDesc, String zhDesc, Setting... settings) {
        return new Module(toId(en), en, zh, enDesc, zhDesc, null, List.of(settings));
    }

    private static Setting check(String field, String en, String zh) {
        return check(field, en, zh, null);
    }

    private static Setting check(String field, String en, String zh, String visibleField) {
        return new Setting(SettingType.CHECKBOX, field, en, zh, 0, 0, 0, visibleField);
    }

    private static Setting slider(String field, String en, String zh, double min, double max, double step) {
        return slider(field, en, zh, min, max, step, null);
    }

    private static Setting slider(String field, String en, String zh, double min, double max, double step, String visibleField) {
        return new Setting(SettingType.SLIDER, field, en, zh, min, max, step, visibleField);
    }

    private static Setting select(String field, String en, String zh) {
        return new Setting(SettingType.SELECTION, field, en, zh, 0, 0, 0, null);
    }

    private static String toId(String value) {
        return value.replace(" ", "").replace("-", "");
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> escaped.append(c);
            }
        }
        return escaped.toString();
    }

    private record Category(String id, String zh, List<Module> modules) {
    }

    private record Module(String id, String en, String zh, String enDesc, String zhDesc, String toggleField, List<Setting> settings) {
        String name() {
            return Config.isChinese ? zh : en;
        }

        String desc() {
            return Config.isChinese ? zhDesc : enDesc;
        }
    }

    private enum SettingType {
        CHECKBOX("checkbox"),
        SLIDER("slider"),
        SELECTION("selection");

        private final String id;

        SettingType(String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    private record Setting(SettingType type, String field, String en, String zh, double min, double max, double step, String visibleField) {
        String name() {
            return Config.isChinese ? zh : en;
        }
    }
}
