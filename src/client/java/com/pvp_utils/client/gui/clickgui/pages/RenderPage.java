package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.widget.*;
import com.pvp_utils.client.modules.impl.Render.CustomCapeManager;
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

        modules.add(new SettingModule(UiText.t("自动格挡", "Auto Block"), UiText.t("自动触发格挡动作", "Automatically trigger blocking"),
                new SettingToggle(() -> Config.autoMode, v -> { Config.autoMode = v; Config.save(); }))
                .addSub(UiText.t("触发距离", "Trigger Range"), UiText.t("自定义近战触发距离", "Customize melee trigger range"),
                        new SettingSlider(2.0, 6.0, "%.2f", () -> Config.range, v -> { Config.range = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("使用动画", "Use Animation"), UiText.t("启用物品使用动画", "Enable item use animation"),
                new SettingToggle(() -> Config.useSwing, v -> { Config.useSwing = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("挖掘状态显示", "Digging Status"), UiText.t("在准星下方显示当前挖掘进度和预计剩余时间", "Show current digging progress and estimated remaining time under the crosshair"),
                new SettingToggle(() -> Config.diggingStatus, v -> { Config.diggingStatus = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("自定义披风", "Custom Cape"), UiText.t("加载本地的自定义皮肤文件", "Load a local custom skin file"),
                new SettingToggle(() -> Config.customCape, v -> { Config.customCape = v; Config.save(); }))
                .addSub(UiText.t("打开目录", "Open Folder"), UiText.t("打开自定义披风文件夹", "Open the custom cape folder"),
                        new SettingButton(UiText.t("打开", "Open"), CustomCapeManager::openFolder))
                .addSub(UiText.t("切换披风", "Switch Cape"), UiText.t("切换当前使用的披风文件", "Switch the selected cape file"),
                        new SettingButton(() -> Config.customCapeImage, CustomCapeManager::cycleCape)));

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
                .addSub(UiText.t("图腾动画", "Totem Animation"), UiText.t("隐藏图腾触发时的全屏动画效果", "Hide the full-screen animation when a totem triggers"),
                        new SettingToggle(() -> Config.hideTotemAnimation, v -> { Config.hideTotemAnimation = v; Config.save(); }))
                .addSub(UiText.t("爆炸粒子", "Explosion Particles"), UiText.t("隐藏爆炸产生的粒子效果", "Hide particles produced by explosions"),
                        new SettingToggle(() -> Config.hideExplosionParticles, v -> { Config.hideExplosionParticles = v; Config.save(); }))
                .addSub(UiText.t("受伤抖动", "Hurt Shake"), UiText.t("隐藏受到伤害时的视角抖动", "Disable camera shake when hurt"),
                        new SettingToggle(() -> Config.hideHurtShake, v -> { Config.hideHurtShake = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("低血量提示", "Low Health Warning"), UiText.t("血量过低时显示警告", "Show a warning when health is low"),
                new SettingToggle(() -> Config.lowHealthNotify, v -> { Config.lowHealthNotify = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("目标 HUD", "Target HUD"), UiText.t("显示目标信息面板", "Show target information panel"),
                new SettingToggle(() -> Config.targetHud, v -> { Config.targetHud = v; Config.save(); }))
                .addSub(UiText.t("模式", "Mode"), UiText.t("选择目标 HUD 样式", "Choose the Target HUD style"),
                        new SettingCycle(List.of("New", "Lite"),
                                () -> Config.targetHudMode == Config.TargetHudMode.NEW ? 0 : 1,
                                i -> { Config.targetHudMode = i == 0 ? Config.TargetHudMode.NEW : Config.TargetHudMode.LITE; Config.save(); }))
                .visibleWhen(() -> Config.fullMode));

        modules.add(new SettingModule(UiText.t("按键显示", "Keystrokes"), UiText.t("显示 WASD 和鼠标按键状态", "Show WASD and mouse button states"),
                new SettingToggle(() -> Config.keystrokes, v -> { Config.keystrokes = v; Config.save(); }))
                .addSub(UiText.t("模式", "Mode"), UiText.t("选择按键显示样式", "Choose the Keystrokes style"),
                        new SettingCycle(List.of("New", "Lite"),
                                () -> Config.keystrokesMode == Config.KeystrokesMode.NEW ? 0 : 1,
                                i -> { Config.keystrokesMode = i == 0 ? Config.KeystrokesMode.NEW : Config.KeystrokesMode.LITE; Config.save(); })));

        modules.add(new SettingModule(UiText.t("名称标签", "Name Tags"), UiText.t("调整原版实体名称标签显示效果", "Adjust vanilla entity name tag rendering"),
                new SettingToggle(() -> Config.nameTag, v -> { Config.nameTag = v; Config.save(); }))
                .addSub(UiText.t("缩放", "Scale"), UiText.t("调整名称标签整体大小", "Adjust name tag size"),
                        new SettingSlider(50.0, 300.0, "%.0f%%", () -> (double) Config.nameTagScale * 100.0, v -> { Config.nameTagScale = v.floatValue() / 100.0f; Config.save(); }))
                .addSub(UiText.t("动态缩放", "Dynamic Scale"), UiText.t("根据距离自动缩放名称标签，让远近大小更接近", "Scale name tags by distance so their screen size stays closer"),
                        new SettingToggle(() -> Config.nameTagDynamicScale, v -> { Config.nameTagDynamicScale = v; Config.save(); })));
    }

    @Override public String getTitle() { return UiText.t("视觉设置", "Render Settings"); }
    @Override public String getSubtitle() { return UiText.t("调整视觉与动画效果", "Adjust visuals and animations"); }
}
