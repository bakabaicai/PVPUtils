package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.widget.*;

import java.util.List;

public class VisualPage extends BasePage {

    public VisualPage() {
        modules.add(new SettingModule("防砍动画", "模拟旧版格挡动画效果",
                new SettingToggle(() -> Config.swordBlock, v -> { Config.swordBlock = v; Config.save(); }))
                .addSub("动画模式", "选择动画风格",
                        new SettingCycle(List.of("1.7", "Push", "1.7+", "New"),
                                () -> switch (Config.animationMode) { case MODE_1_7 -> 0; case MODE_PUSH -> 1; case MODE_1_7_PLUS -> 2; case MODE_NEW -> 3; },
                                i -> { Config.animationMode = switch (i) { case 1 -> Config.AnimMode.MODE_PUSH; case 2 -> Config.AnimMode.MODE_1_7_PLUS; case 3 -> Config.AnimMode.MODE_NEW; default -> Config.AnimMode.MODE_1_7; }; Config.save(); }))
                .addSub("偏移 X", "水平偏移量",
                        new SettingSlider(-1.0, 1.0, "%.2f", () -> (double) Config.offsetX, v -> { Config.offsetX = v.floatValue(); Config.save(); }))
                .addSub("偏移 Y", "垂直偏移量",
                        new SettingSlider(-1.0, 1.0, "%.2f", () -> (double) Config.offsetY, v -> { Config.offsetY = v.floatValue(); Config.save(); }))
                .addSub("偏移 Z", "深度偏移量",
                        new SettingSlider(-1.0, 1.0, "%.2f", () -> (double) Config.offsetZ, v -> { Config.offsetZ = v.floatValue(); Config.save(); }))
                .addSub("动画速度", "调整动画播放速度",
                        new SettingSlider(0.0, 4.0, "%.2f", () -> (double) Config.animSpeed, v -> { Config.animSpeed = v.floatValue(); Config.save(); })));

        modules.add(new SettingModule("自动格挡", "自动触发格挡动作",
                new SettingToggle(() -> Config.autoMode, v -> { Config.autoMode = v; Config.save(); }))
                .addSub("触发距离", "自定义近战触发距离",
                        new SettingSlider(2.0, 6.0, "%.2f", () -> Config.range, v -> { Config.range = v; Config.save(); })));

        modules.add(new SettingModule("使用动画", "启用物品使用动画",
                new SettingToggle(() -> Config.useSwing, v -> { Config.useSwing = v; Config.save(); })));

        modules.add(new SettingModule("潜行动画", "自定义潜行动画效果",
                new SettingToggle(() -> Config.noSneakAnimation, v -> { Config.noSneakAnimation = v; Config.save(); }))
                .addSub("下降高度", "潜行时的下降幅度",
                        new SettingSlider(0.0, 1.0, "%.0f%%", () -> (double) Config.sneakDropScale, v -> { Config.sneakDropScale = v.floatValue(); Config.save(); }))
                .addSub("动画速度", "潜行动画的过渡速度",
                        new SettingSlider(0.0, 1.0, "%.0f%%", () -> (double) Config.sneakAnimationSpeed, v -> { Config.sneakAnimationSpeed = v.floatValue(); Config.save(); })));

        modules.add(new SettingModule("低血量提示", "血量过低时显示警告",
                new SettingToggle(() -> Config.lowHealthNotify, v -> { Config.lowHealthNotify = v; Config.save(); })));

        modules.add(new SettingModule("目标 HUD", "显示目标信息面板",
                new SettingToggle(() -> Config.targetHud, v -> { Config.targetHud = v; Config.save(); }))
                .addSub("HUD X", "水平位置",
                        new SettingSlider(-500.0, 500.0, "%.0f", () -> (double) Config.targetHudX, v -> { Config.targetHudX = v.floatValue(); Config.save(); }))
                .addSub("HUD Y", "垂直位置",
                        new SettingSlider(-500.0, 500.0, "%.0f", () -> (double) Config.targetHudY, v -> { Config.targetHudY = v.floatValue(); Config.save(); }))
                .addSub("HUD Z", "深度位置",
                        new SettingSlider(-500.0, 500.0, "%.0f", () -> (double) Config.targetHudZ, v -> { Config.targetHudZ = v.floatValue(); Config.save(); })));
    }

    @Override public String getTitle() { return "视觉设置"; }
    @Override public String getSubtitle() { return "调整视觉与动画效果"; }
}