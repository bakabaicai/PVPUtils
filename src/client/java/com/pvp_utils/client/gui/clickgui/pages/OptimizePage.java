package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.widget.SettingModule;
import com.pvp_utils.client.gui.clickgui.widget.SettingCycle;
import com.pvp_utils.client.gui.clickgui.widget.HudAppearanceSettings;
import java.util.List;
import com.pvp_utils.client.gui.clickgui.widget.SettingSlider;
import com.pvp_utils.client.gui.clickgui.widget.SettingToggle;

public class OptimizePage extends BasePage {
    public OptimizePage() {
        modules.add(new SettingModule(UiText.t("游戏内输入法修复", "IME Fix In Game"), UiText.t("避免输入法影响游戏按键操作", "Prevent input methods from interfering with game controls"),
                new SettingToggle(() -> Config.disableImeInGame, v -> { Config.disableImeInGame = v; Config.save(); })));

        modules.add(new SettingModule(
                UiText.t("更好的聊天栏", "Better Chat"),
                UiText.t("聊天动画与玩家头像", "Chat animations and player heads"),
                new SettingToggle(() -> Config.betterChat, v -> { Config.betterChat = v; Config.save(); }))
                .addSub(UiText.t("消息入场动画", "Message Animation"), UiText.t("新消息出现时滑入", "Slide in new chat messages"),
                        new SettingToggle(() -> Config.betterChatMessageAnimation, v -> { Config.betterChatMessageAnimation = v; Config.save(); }))
                .addSub(UiText.t("输入栏动画", "Input Bar Animation"), UiText.t("打开或关闭聊天界面时的动画", "Animate the chat input bar opening and closing"),
                        new SettingToggle(() -> Config.betterChatInputAnimation, v -> { Config.betterChatInputAnimation = v; Config.save(); }))
                .addSub(UiText.t("聊天头像", "Chat Heads"), UiText.t("在聊天消息旁显示玩家头像", "Show player heads next to chat messages"),
                        new SettingToggle(() -> Config.betterChatAvatar, v -> { Config.betterChatAvatar = v; Config.save(); }))
                .addSub(UiText.t("消息动画时间", "Message Fade Time"), "",
                        new SettingSlider(100.0, 900.0, "%.0fms", () -> (double) Config.betterChatMessageFadeTime, v -> { Config.betterChatMessageFadeTime = v.intValue(); Config.save(); }))
                .addSub(UiText.t("输入栏动画时间", "Input Fade Time"), "",
                        new SettingSlider(100.0, 900.0, "%.0fms", () -> (double) Config.betterChatInputFadeTime, v -> { Config.betterChatInputFadeTime = v.intValue(); Config.save(); })));

        modules.add(new SettingModule(
                UiText.t("更好的计分板", "Better Scoreboard"),
                UiText.t("调整计分板样式、位置和大小", "Customize the scoreboard appearance, position, and size"),
                new SettingToggle(() -> Config.betterScoreboard, v -> { Config.betterScoreboard = v; Config.save(); }))
                .addSub(UiText.t("隐藏红色数字", "Hide Red Numbers"), "",
                        new SettingToggle(() -> Config.betterScoreboardHideScores, v -> { Config.betterScoreboardHideScores = v; Config.save(); }))
                .addSub(UiText.t("视觉改进", "Visual Improvement"), "",
                        new SettingToggle(() -> Config.betterScoreboardVisualImprovement, v -> { Config.betterScoreboardVisualImprovement = v; Config.save(); })));

        modules.add(new SettingModule(
                UiText.t("更好的物品栏", "Better Item Selector"),
                UiText.t("圆角快捷栏与平滑选中框", "Rounded hotbar with a smooth selection indicator"),
                new SettingToggle(() -> Config.betterItemSelector, v -> { Config.betterItemSelector = v; Config.save(); })));

        modules.add(new SettingModule(
                UiText.t("更好的鼠标逻辑", "Better Mouse Logic"),
                UiText.t("优化物品栏内的鼠标交互", "Improve mouse interaction in inventories"),
                new SettingToggle(() -> Config.betterMouseLogic, v -> { Config.betterMouseLogic = v; Config.save(); })));

        modules.add(new SettingModule(
                UiText.t("平滑快捷栏", "Smooth Hotbar"),
                UiText.t("平滑快捷栏选中框的移动", "Smooth the hotbar selection transition"),
                new SettingToggle(() -> Config.smoothHotbarScrolling, v -> { Config.smoothHotbarScrolling = v; Config.save(); }))
                .addSub(UiText.t("动画速度", "Animation Speed"), "",
                        new SettingSlider(0.05, 0.99, "%.2f", () -> (double) Config.smoothHotbarAnimationSpeed, v -> { Config.smoothHotbarAnimationSpeed = v.floatValue(); Config.save(); })));
        SettingModule scoreboard = HudAppearanceSettings.module(modules, "更好的计分板", "Better Scoreboard");
        scoreboard.addSubVisible(UiText.t("背景样式", "Background Style"), "",
                new SettingCycle(List.of("Blur", "Liquid"), () -> Config.betterScoreboardLiquidGlass ? 1 : 0,
                        value -> { Config.betterScoreboardLiquidGlass = value == 1; Config.save(); }),
                () -> Config.betterScoreboardVisualImprovement);
        HudAppearanceSettings.add(scoreboard, () -> Config.scoreboardAppearance,
                () -> Config.betterScoreboardVisualImprovement, () -> Config.betterScoreboardLiquidGlass);
        SettingModule hotbar = HudAppearanceSettings.module(modules, "更好的物品栏", "Better Item Selector");
        hotbar.addSub(UiText.t("背景样式", "Background Style"), "",
                new SettingCycle(List.of("Blur", "Liquid"), () -> Config.betterItemSelectorLiquidGlass ? 1 : 0,
                        value -> { Config.betterItemSelectorLiquidGlass = value == 1; Config.save(); }));
        HudAppearanceSettings.hotbar(hotbar, () -> Config.hotbarAppearance, () -> Config.betterItemSelectorLiquidGlass);

    }

    @Override public String getTitle() { return UiText.t("优化设置", "Optimize Settings"); }
    @Override public String getSubtitle() { return UiText.t("性能与优化相关参数", "Performance and optimization options"); }
}
