package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.widget.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

import java.util.List;

public class RenderPage extends BasePage {
    public RenderPage() {
        modules.add(new SettingModule(UiText.t("UI 编辑", "UI Editor"), UiText.t("打开 HUD 位置编辑器，悬浮控件后可使用滚轮缩放大小", "Open the HUD editor. Hover an element and use the mouse wheel to resize it"),
                new SettingToggle(() -> false, v -> Minecraft.getInstance().setScreen(new ChatScreen("", false))))
                .addSub(UiText.t("在聊天框中快速启用", "Quick Enable in Chat"), UiText.t("打开聊天框时自动启用 HUD 拖动编辑", "Automatically enable HUD drag editing when opening chat"),
                        new SettingToggle(() -> Config.chatHudEditQuickEnable, v -> { Config.chatHudEditQuickEnable = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("防砍动画", "Sword Blocking Animation"), UiText.t("模拟旧版格挡动画效果", "Simulate the old blocking animation"),
                new SettingToggle(() -> Config.swordBlock, v -> { Config.swordBlock = v; Config.save(); }))
                .addSub(UiText.t("动画模式", "Animation Mode"), UiText.t("选择动画风格", "Choose the animation style"),
                        new SettingCycle(List.of("1.7", "Push", "1.7+", "New"),
                                () -> switch (Config.animationMode) { case MODE_1_7 -> 0; case MODE_PUSH -> 1; case MODE_1_7_PLUS -> 2; case MODE_NEW -> 3; },
                                i -> { Config.animationMode = switch (i) { case 1 -> Config.AnimMode.MODE_PUSH; case 2 -> Config.AnimMode.MODE_1_7_PLUS; case 3 -> Config.AnimMode.MODE_NEW; default -> Config.AnimMode.MODE_1_7; }; Config.save(); }))
                .addSub(UiText.t("偏移 X", "Offset X"), UiText.t("水平偏移量", "Horizontal offset"),
                        new SettingSlider(-1.0, 1.0, "%.2f", () -> (double) Config.offsetX, v -> { Config.offsetX = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("偏移 Y", "Offset Y"), UiText.t("垂直偏移量", "Vertical offset"),
                        new SettingSlider(-1.0, 1.0, "%.2f", () -> (double) Config.offsetY, v -> { Config.offsetY = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("偏移 Z", "Offset Z"), UiText.t("深度偏移量", "Depth offset"),
                        new SettingSlider(-1.0, 1.0, "%.2f", () -> (double) Config.offsetZ, v -> { Config.offsetZ = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("动画速度", "Animation Speed"), UiText.t("调整动画播放速度", "Adjust animation playback speed"),
                        new SettingSlider(0.0, 4.0, "%.2f", () -> (double) Config.animSpeed, v -> { Config.animSpeed = v.floatValue(); Config.save(); })));

        modules.add(new SettingModule(UiText.t("去除攻击冷却动画", "Remove Attack Cooldown Animation"), UiText.t("去除武器挥动后高版本额外的抬手动画", "Remove the extra hand raise after weapon swings"),
                new SettingToggle(() -> Config.noAttackCooldownAnimation, v -> { Config.noAttackCooldownAnimation = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("攻击特效", "Attack Effects"), UiText.t("控制攻击时显示的粒子效果", "Control particles shown when attacking"), null)
                .addSub(UiText.t("暴击粒子", "Crit Particles"), UiText.t("攻击时常驻显示暴击粒子", "Always show crit particles when attacking"),
                        new SettingToggle(() -> Config.attackEffectsCritParticles, v -> { Config.attackEffectsCritParticles = v; Config.save(); }))
                .addSubWhen(() -> Config.attackEffectsCritParticles, UiText.t("暴击粒子倍数", "Crit Multiplier"), UiText.t("调整额外暴击粒子的显示倍数", "Adjust the extra crit particle multiplier"),
                        new SettingSlider(1.0, 10.0, "%.1fx", () -> (double) Config.attackEffectsCritMultiplier, v -> { Config.attackEffectsCritMultiplier = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("锋利粒子", "Sharpness Particles"), UiText.t("攻击时常驻显示锋利粒子", "Always show sharpness particles when attacking"),
                        new SettingToggle(() -> Config.attackEffectsSharpnessParticles, v -> { Config.attackEffectsSharpnessParticles = v; Config.save(); }))
                .addSubWhen(() -> Config.attackEffectsSharpnessParticles, UiText.t("锋利粒子倍数", "Sharpness Multiplier"), UiText.t("调整额外锋利粒子的显示倍数", "Adjust the extra sharpness particle multiplier"),
                        new SettingSlider(1.0, 10.0, "%.1fx", () -> (double) Config.attackEffectsSharpnessMultiplier, v -> { Config.attackEffectsSharpnessMultiplier = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("火焰粒子", "Flame Particles"), UiText.t("攻击时常驻显示火焰粒子", "Always show flame particles when attacking"),
                        new SettingToggle(() -> Config.attackEffectsFlameParticles, v -> { Config.attackEffectsFlameParticles = v; Config.save(); }))
                .addSubWhen(() -> Config.attackEffectsFlameParticles, UiText.t("火焰粒子倍数", "Flame Multiplier"), UiText.t("调整火焰粒子的显示倍数", "Adjust the flame particle multiplier"),
                        new SettingSlider(1.0, 10.0, "%.1fx", () -> (double) Config.attackEffectsFlameMultiplier, v -> { Config.attackEffectsFlameMultiplier = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("血液粒子", "Blood Particles"), UiText.t("攻击时常驻显示血液粒子", "Always show blood particles when attacking"),
                        new SettingToggle(() -> Config.attackEffectsBloodParticles, v -> { Config.attackEffectsBloodParticles = v; Config.save(); }))
                .addSubWhen(() -> Config.attackEffectsBloodParticles, UiText.t("血液粒子倍数", "Blood Multiplier"), UiText.t("调整血液粒子的显示倍数", "Adjust the blood particle multiplier"),
                        new SettingSlider(1.0, 10.0, "%.1fx", () -> (double) Config.attackEffectsBloodMultiplier, v -> { Config.attackEffectsBloodMultiplier = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("闪电", "Lightning"), UiText.t("攻击时渲染闪电效果", "Render lightning effects when attacking"),
                        new SettingToggle(() -> Config.attackEffectsLightning, v -> { Config.attackEffectsLightning = v; Config.save(); }))
                .addSubWhen(() -> Config.attackEffectsLightning, UiText.t("闪电数量", "Lightning Count"), UiText.t("攻击时渲染的闪电数量", "Number of lightning effects rendered per attack"),
                        new SettingSlider(1.0, 5.0, "%.0f", () -> (double) Config.attackEffectsLightningCount, v -> { Config.attackEffectsLightningCount = v.intValue(); Config.save(); })));

        modules.add(new SettingModule(UiText.t("更改受击颜色", "Hit Color"), UiText.t("更改实体受击时的颜色", "Change the color shown when entities are hit"),
                new SettingToggle(() -> Config.hitColor, v -> { Config.hitColor = v; Config.save(); }))
                .addSub("R", "",
                        new SettingSlider(0.0, 255.0, "%.0f", () -> (double) Config.hitColorRed, v -> { Config.hitColorRed = clampColor(v); Config.save(); }))
                .addSub("G", "",
                        new SettingSlider(0.0, 255.0, "%.0f", () -> (double) Config.hitColorGreen, v -> { Config.hitColorGreen = clampColor(v); Config.save(); }))
                .addSub("B", "",
                        new SettingSlider(0.0, 255.0, "%.0f", () -> (double) Config.hitColorBlue, v -> { Config.hitColorBlue = clampColor(v); Config.save(); }))
                .addSub(UiText.t("透明度", "Transparency"), "",
                        new SettingSlider(0.0, 100.0, "%.0f%%", () -> alphaToTransparencyPercent(Config.hitColorAlpha), v -> { Config.hitColorAlpha = transparencyPercentToAlpha(v); Config.save(); }))
                .addSub(UiText.t("当前颜色", "Current Color"), UiText.t("显示当前受击覆盖颜色", "Preview the current hit overlay color"),
                        new SettingColorPreview(() -> hitColorArgb())));

        modules.add(new SettingModule(UiText.t("彩虹附魔光效", "Rainbow Enchantment Glint"), UiText.t("将附魔光效更改为彩虹色", "Change the enchantment glint to rainbow colors"),
                new SettingToggle(() -> Config.customEnchantmentGlint, v -> { Config.customEnchantmentGlint = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("自动格挡", "Auto Block"), UiText.t("自动触发格挡动作", "Automatically trigger blocking"),
                new SettingToggle(() -> Config.autoMode, v -> { Config.autoMode = v; Config.save(); }))
                .addSub(UiText.t("触发距离", "Trigger Range"), UiText.t("自定义近战触发距离", "Customize melee trigger range"),
                        new SettingSlider(2.0, 6.0, "%.2f", () -> Config.range, v -> { Config.range = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("使用动画", "Use Animation"), UiText.t("启用物品使用动画", "Enable item use animation"),
                new SettingToggle(() -> Config.useSwing, v -> { Config.useSwing = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("挖掘状态显示", "Digging Status"), UiText.t("在准星下方显示当前挖掘进度和预计剩余时间", "Show current digging progress and estimated remaining time under the crosshair"),
                new SettingToggle(() -> Config.diggingStatus, v -> { Config.diggingStatus = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("物品使用状态显示", "Item Use Status"), UiText.t("在屏幕上显示当前物品使用进度或状态", "Show current item use progress or status on the screen"),
                new SettingToggle(() -> Config.itemUseStatus, v -> { Config.setItemUseStatus(v); Config.save(); }))
                .addSub(UiText.t("模式", "Mode"), UiText.t("选择物品使用状态显示样式", "Choose the item use status style"),
                        new SettingCycle(List.of("Lite", "New"),
                                () -> Config.itemUseStatusMode == Config.ItemUseStatusMode.NEW ? 1 : 0,
                                i -> {
                                    Config.itemUseStatusMode = switch (i) {
                                        case 1 -> Config.ItemUseStatusMode.NEW;
                                        default -> Config.ItemUseStatusMode.LITE;
                                    };
                                    Config.save();
                                })));

        modules.add(new SettingModule(UiText.t("灵动岛", "Dynamic Island"), UiText.t("在界面上添加灵动岛组件", "Add a Dynamic Island component to the HUD"),
                new SettingToggle(() -> Config.dynamicIsland, v -> { Config.setDynamicIsland(v); Config.save(); }))
                .addSub(UiText.t("方块数量显示", "Block Count Display"), "",
                        new SettingToggle(() -> Config.dynamicIslandBlockCount, v -> {
                            Config.setDynamicIslandBlockCount(v);
                            Config.save();
                        }))
                .addSub("???", "",
                        new SettingToggle(() -> Config.dynamicIslandBlockCountAltIcon, v -> {
                            Config.dynamicIslandBlockCountAltIcon = v;
                            Config.save();
                        }),
                        () -> Config.dynamicIslandBlockCount)
                .addSub(UiText.t("物品使用状态", "Item Use Status"), "",
                        new SettingToggle(() -> Config.dynamicIslandItemUseStatus, v -> {
                            Config.setDynamicIslandItemUseStatus(v);
                            Config.save();
                        }))
                .addSub(UiText.t("低血量提示", "Low Health Warning"), "",
                        new SettingToggle(() -> Config.dynamicIslandLowHealthWarning, v -> {
                            Config.dynamicIslandLowHealthWarning = v;
                            if (v) Config.lowHealthNotify = false;
                            Config.save();
                        })));

        modules.add(new SettingModule(UiText.t("物品物理掉落", "Item Physics"), UiText.t("让掉落物以更加物理的方式掉落", "Make dropped items fall in a more physical way"),
                new SettingToggle(() -> Config.itemPhysics, v -> {
                    Config.itemPhysics = v;
                    if (v) Config.item2DRender = false;
                    Config.save();
                }))
                .addSub(UiText.t("旋转速度", "Rotation Speed"), UiText.t("调整掉落物腾空时的旋转速度", "Adjust how fast dropped items rotate in the air"),
                        new SettingSlider(0.0, 3.0, "%.1fx", () -> (double) Config.itemPhysicsRotationSpeed,
                                v -> { Config.itemPhysicsRotationSpeed = v.floatValue(); Config.save(); })));

        modules.add(new SettingModule(UiText.t("掉落物 2D 渲染", "Dropped Item 2D Render"), UiText.t("将掉落物的渲染方式更改为2D渲染（老版本渲染方式）", "Change dropped item rendering to 2D rendering (old version style)"),
                new SettingToggle(() -> Config.item2DRender, v -> {
                    Config.item2DRender = v;
                    if (v) Config.itemPhysics = false;
                    Config.save();
                })));

        modules.add(new SettingModule(
                UiText.t("盔甲 HUD", "Armor HUD"),
                UiText.t("在快捷栏两侧显示当前装备和耐久", "Show equipped armor and durability beside the hotbar"),
                new SettingToggle(() -> Config.armorHud, v -> { Config.armorHud = v; Config.save(); }))
                .addSub(UiText.t("模式", "Mode"), UiText.t("在 new 和 lite 之间切换", "Switch between new and lite"),
                        new SettingCycle(List.of("New", "Lite"),
                                () -> Config.armorHudMode == Config.ArmorHudMode.NEW ? 0 : 1,
                                i -> {
                                    Config.armorHudMode = i == 0 ? Config.ArmorHudMode.NEW : Config.ArmorHudMode.LITE;
                                    if (Config.armorHudMode == Config.ArmorHudMode.LITE && Config.armorHudLayout == Config.ArmorHudLayout.SEPARATED) {
                                        Config.armorHudLayout = Config.ArmorHudLayout.HORIZONTAL;
                                    }
                                    Config.save();
                                }))
                .addSub(UiText.t("布局", "Layout"), UiText.t("选择 Armor HUD 的排列方式", "Choose the Armor HUD layout"),
                        new SettingCycle(List.of(
                                        UiText.t("分离式", "Separated"),
                                        UiText.t("竖向", "Vertical"),
                                        UiText.t("横向", "Horizontal")),
                                () -> switch (Config.armorHudLayout) {
                                    case SEPARATED -> 0;
                                    case VERTICAL -> 1;
                                    case HORIZONTAL -> 2;
                                },
                                i -> {
                                    if (Config.armorHudMode == Config.ArmorHudMode.NEW) {
                                        Config.armorHudLayout = switch (i) {
                                            case 1 -> Config.ArmorHudLayout.VERTICAL;
                                            case 2 -> Config.ArmorHudLayout.HORIZONTAL;
                                            default -> Config.ArmorHudLayout.SEPARATED;
                                        };
                                    } else {
                                        Config.armorHudLayout = switch (i) {
                                            case 0 -> Config.ArmorHudLayout.VERTICAL;
                                            default -> Config.ArmorHudLayout.HORIZONTAL;
                                        };
                                    }
                                    Config.save();
                                })));

        modules.add(new SettingModule(UiText.t("药水状态", "Potion Status"), UiText.t("显示当前药水效果和剩余时间。", "Show active potion effects and remaining time."),
                new SettingToggle(() -> Config.potionStatus, v -> { Config.potionStatus = v; Config.save(); }))
                .addSub(UiText.t("灰色遮罩", "Gray Background"), UiText.t("控制外层灰色背景是否显示", "Control whether the outer gray background is visible"),
                        new SettingToggle(() -> Config.potionStatusBackground, v -> { Config.potionStatusBackground = v; Config.save(); }))
                .addSub(UiText.t("倒计时数字", "Countdown Text"), UiText.t("关闭后只显示效果名称", "When disabled, only the effect name is shown"),
                        new SettingToggle(() -> Config.potionStatusCountdown, v -> { Config.potionStatusCountdown = v; Config.save(); }))
                .addSub(UiText.t("屏蔽原版显示", "Hide Vanilla Effects"), UiText.t("启用后会屏蔽右上角原版药水效果显示", "Hide the vanilla potion effect UI in the top-right while this widget is active"),
                        new SettingToggle(() -> Config.potionStatusHideVanilla, v -> { Config.potionStatusHideVanilla = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("潜行动画调整", "Sneak Animation Adjustment"), UiText.t("调整潜行视角下降效果", "Adjust sneak camera drop effect"),
                new SettingToggle(() -> Config.noSneakAnimation, v -> { Config.noSneakAnimation = v; Config.save(); }))
                .addSub(UiText.t("下降幅度", "Drop Amount"), UiText.t("潜行时的下降幅度", "Sneak camera drop amount"),
                        new SettingSlider(0.0, 100.0, "%.0f%%", () -> (double) Config.sneakDropScale * 100.0, v -> { Config.sneakDropScale = (v.floatValue() / 100.0f); Config.save(); }))
                .addSub(UiText.t("过渡速度", "Transition Speed"), UiText.t("潜行动画的过渡速度", "Sneak animation transition speed"),
                        new SettingSlider(0.0, 100.0, "%.0f%%", () -> (double) Config.sneakAnimationSpeed * 100.0, v -> { Config.sneakAnimationSpeed = (v.floatValue() / 100.0f); Config.save(); })));

        modules.add(new SettingModule(UiText.t("伽马覆写", "Gamma Override"), UiText.t("强制使用自定义亮度值", "Force a custom brightness value"),
                new SettingToggle(() -> Config.gammaOverride, v -> { Config.gammaOverride = v; Config.save(); }))
                .addSub(UiText.t("伽马值", "Gamma Value"), UiText.t("调整游戏亮度上限", "Adjust the brightness limit"),
                        new SettingSlider(0.0, 15.0, "%.1f", () -> Config.gammaValue, v -> { Config.gammaValue = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("动态模糊", "Dynamic Motion Blur"), UiText.t("根据相机运动生成速度模糊效果", "Apply velocity blur based on camera motion"),
                new SettingToggle(() -> Config.dynamicMotionBlur, v -> { Config.dynamicMotionBlur = v; Config.save(); }))
                .addSub(UiText.t("算法", "Algorithm"), UiText.t("选择动态模糊算法", "Choose the motion blur algorithm"),
                        new SettingCycle(List.of("Velocity", "Frame", "Hybrid", "Max", "Mix"),
                                () -> Config.motionBlurAlgorithm.ordinal(),
                                i -> { Config.motionBlurAlgorithm = Config.MotionBlurAlgorithm.values()[i % Config.MotionBlurAlgorithm.values().length]; Config.save(); }))
                .addSub(UiText.t("强度", "Strength"), UiText.t("调整动态模糊强度", "Adjust motion blur strength"),
                        new SettingSlider(0.0, 300.0, "%.0f%%", () -> (double) Config.dynamicMotionBlurStrength * 100.0, v -> { Config.dynamicMotionBlurStrength = v.floatValue() / 100.0f; Config.save(); }))
                .addSub(UiText.t("刷新率缩放", "Refresh Rate Scaling"), UiText.t("高 FPS 时按显示器刷新率自动增强采样", "Scale blur samples against display refresh rate at high FPS"),
                        new SettingToggle(() -> Config.dynamicMotionBlurRefreshRateScaling, v -> { Config.dynamicMotionBlurRefreshRateScaling = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("渲染控制", "Render Control"), UiText.t("选择性关闭游戏内渲染效果", "Selectively disable in-game rendering effects"), null)
                .addSub(UiText.t("告示牌文本", "Sign Text"), UiText.t("隐藏告示牌和悬挂告示牌文字", "Hide text on signs and hanging signs"),
                        new SettingToggle(() -> Config.hideSignText, v -> { Config.hideSignText = v; Config.save(); }))
                .addSub(UiText.t("附魔台悬浮书", "Enchanting Table Book"), UiText.t("隐藏附魔台上方悬浮的书", "Hide the floating book above enchanting tables"),
                        new SettingToggle(() -> Config.hideEnchantTableBook, v -> { Config.hideEnchantTableBook = v; Config.save(); }))
                .addSub(UiText.t("火焰效果", "Fire Overlay"), UiText.t("隐藏第一人称着火遮挡效果", "Hide the first-person fire overlay"),
                        new SettingToggle(() -> Config.hideFireOverlay, v -> { Config.hideFireOverlay = v; Config.save(); }))
                .addSub(UiText.t("屏幕暗角", "Vignette"), UiText.t("隐藏屏幕边缘暗角效果", "Hide the screen edge vignette"),
                        new SettingToggle(() -> Config.hideVignette, v -> { Config.hideVignette = v; Config.save(); }))
                .addSub(UiText.t("视角场迷雾", "View Fog"), UiText.t("隐藏视角中的世界迷雾效果", "Hide world fog in the view"),
                        new SettingToggle(() -> Config.hideFog, v -> { Config.hideFog = v; Config.save(); }))
                .addSub(UiText.t("图腾动画", "Totem Animation"), UiText.t("隐藏图腾触发时的全屏动画效果", "Hide the full-screen animation when a totem triggers"),
                        new SettingToggle(() -> Config.hideTotemAnimation, v -> { Config.hideTotemAnimation = v; Config.save(); }))
                .addSub(UiText.t("爆炸粒子", "Explosion Particles"), UiText.t("隐藏爆炸产生的粒子效果", "Hide particles produced by explosions"),
                        new SettingToggle(() -> Config.hideExplosionParticles, v -> { Config.hideExplosionParticles = v; Config.save(); }))
                .addSub(UiText.t("雨滴粒子", "Rain Particles"), UiText.t("隐藏雨天时的雨滴效果", "Hide raindrop effects during rainy weather"),
                        new SettingToggle(() -> Config.hideRainParticles, v -> { Config.hideRainParticles = v; Config.save(); }))
                .addSub(UiText.t("受伤抖动", "Hurt Shake"), UiText.t("隐藏受到伤害时的视角抖动", "Disable camera shake when hurt"),
                        new SettingToggle(() -> Config.hideHurtShake, v -> { Config.hideHurtShake = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("低血量提示", "Low Health Warning"), UiText.t("血量过低时显示警告", "Show a warning when health is low"),
                new SettingToggle(() -> Config.lowHealthNotify, v -> {
                    Config.lowHealthNotify = v;
                    if (v) Config.dynamicIslandLowHealthWarning = false;
                    Config.save();
                })));

        modules.add(new SettingModule(UiText.t("目标 HUD", "Target HUD"), UiText.t("显示目标信息面板", "Show target information panel"),
                new SettingToggle(() -> Config.targetHud, v -> { Config.targetHud = v; Config.save(); }))
                .addSub(UiText.t("选择目标 HUD 样式", "Mode"), UiText.t("选择目标 HUD 样式", "Choose the Target HUD style"),
                        new SettingCycle(List.of("New", "Blur", "Lite"),
                                () -> Config.targetHudMode == Config.TargetHudMode.NEW ? 0 : Config.targetHudMode == Config.TargetHudMode.BLUR ? 1 : 2,
                                i -> { Config.targetHudMode = i == 0 ? Config.TargetHudMode.NEW : i == 1 ? Config.TargetHudMode.BLUR : Config.TargetHudMode.LITE; Config.save(); }))
                .visibleWhen(() -> Config.fullMode));

        modules.add(new SettingModule(UiText.t("按键显示", "Keystrokes"), UiText.t("显示 WASD 和鼠标按键状态", "Show WASD and mouse button states"),
                new SettingToggle(() -> Config.keystrokes, v -> { Config.keystrokes = v; Config.save(); }))
                .addSub(UiText.t("选择按键显示样式", "Mode"), UiText.t("选择按键显示样式", "Choose the Keystrokes style"),
                        new SettingCycle(List.of("New", "Lite"),
                                () -> Config.keystrokesMode == Config.KeystrokesMode.NEW ? 0 : 1,
                                i -> { Config.keystrokesMode = i == 0 ? Config.KeystrokesMode.NEW : Config.KeystrokesMode.LITE; Config.save(); })));

        modules.add(new SettingModule(UiText.t("名称标签", "Name Tags"), UiText.t("调整原版实体名称标签显示效果", "Adjust vanilla entity name tag rendering"),
                new SettingToggle(() -> Config.nameTag, v -> { Config.nameTag = v; Config.save(); }))
                .addSub(UiText.t("缩放", "Scale"), UiText.t("调整名称标签整体大小", "Adjust name tag size"),
                        new SettingSlider(50.0, 300.0, "%.0f%%", () -> (double) Config.nameTagScale * 100.0, v -> { Config.nameTagScale = v.floatValue() / 100.0f; Config.save(); }))
                .addSub(UiText.t("动态缩放", "Dynamic Scale"), UiText.t("根据距离自动缩放名称标签，让远近大小更接近", "Scale name tags by distance so their screen size stays closer"),
                        new SettingToggle(() -> Config.nameTagDynamicScale, v -> { Config.nameTagDynamicScale = v; Config.save(); }))
                .addSub(UiText.t("仅玩家", "Only Player"), UiText.t("只放大真实玩家的名称标签，过滤多数 NPC", "Only scale real player name tags and filter most NPCs"),
                        new SettingToggle(() -> Config.nameTagOnlyPlayer, v -> { Config.nameTagOnlyPlayer = v; Config.save(); })));
    }

    @Override public String getTitle() { return UiText.t("视觉设置", "Render Settings"); }
    @Override public String getSubtitle() { return UiText.t("调整视觉与动画效果", "Adjust visuals and animations"); }

    private static int clampColor(Double value) {
        return Math.max(0, Math.min(255, value.intValue()));
    }

    private static double alphaToTransparencyPercent(int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return (255.0 - clamped) / 255.0 * 100.0;
    }

    private static int transparencyPercentToAlpha(Double value) {
        double percent = Math.max(0.0, Math.min(100.0, value));
        return Math.max(0, Math.min(255, (int) Math.round((100.0 - percent) / 100.0 * 255.0)));
    }

    private static int hitColorArgb() {
        return ((Config.hitColorAlpha & 0xFF) << 24)
                | ((Config.hitColorRed & 0xFF) << 16)
                | ((Config.hitColorGreen & 0xFF) << 8)
                | (Config.hitColorBlue & 0xFF);
    }
}
