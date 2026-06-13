package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.widget.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

import java.util.List;

public class VisualPage extends BasePage {

    public VisualPage() {
        modules.add(new SettingModule(UiText.t("UI 编辑", "UI Editor"), UiText.t("打开 HUD 位置编辑器", "Open the HUD position editor"),
                new SettingToggle(() -> false, v -> Minecraft.getInstance().setScreen(new ChatScreen("", false)))));

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

        modules.add(new SettingModule(UiText.t("自动格挡", "Auto Block"), UiText.t("自动触发格挡动作", "Automatically trigger blocking"),
                new SettingToggle(() -> Config.autoMode, v -> { Config.autoMode = v; Config.save(); }))
                .addSub(UiText.t("触发距离", "Trigger Range"), UiText.t("自定义近战触发距离", "Customize melee trigger range"),
                        new SettingSlider(2.0, 6.0, "%.2f", () -> Config.range, v -> { Config.range = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("使用动画", "Use Animation"), UiText.t("启用物品使用动画", "Enable item use animation"),
                new SettingToggle(() -> Config.useSwing, v -> { Config.useSwing = v; Config.save(); })));

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

        modules.add(new SettingModule(UiText.t("低血量提示", "Low Health Warning"), UiText.t("血量过低时显示警告", "Show a warning when health is low"),
                new SettingToggle(() -> Config.lowHealthNotify, v -> { Config.lowHealthNotify = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("目标 HUD", "Target HUD"), UiText.t("显示目标信息面板", "Show target information panel"),
                new SettingToggle(() -> Config.targetHud, v -> { Config.targetHud = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("按键显示", "Keystrokes"), UiText.t("显示 WASD 和鼠标按键状态", "Show WASD and mouse button states"),
                new SettingToggle(() -> Config.keystrokes, v -> { Config.keystrokes = v; Config.save(); })));
    }

    @Override public String getTitle() { return UiText.t("视觉设置", "Visual Settings"); }
    @Override public String getSubtitle() { return UiText.t("调整视觉与动画效果", "Adjust visuals and animations"); }
}
