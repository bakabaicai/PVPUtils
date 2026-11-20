package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.widget.SettingModule;
import com.pvp_utils.client.gui.clickgui.widget.HudAppearanceSettings;
import com.pvp_utils.client.gui.clickgui.widget.SettingSlider;
import com.pvp_utils.client.gui.clickgui.widget.SettingToggle;
import com.pvp_utils.client.gui.clickgui.widget.SettingCycle;
import java.util.List;

public class OptimizePage extends BasePage {
    public OptimizePage() {
        modules.add(ModuleBindingIds.preserve(OptimizePage.class, 0, new SettingModule(UiText.t("游戏内输入法修复", "IME Fix In Game"), UiText.t("避免输入法影响游戏按键操作", "Prevent input methods from interfering with game controls"),
                new SettingToggle(() -> Config.disableImeInGame, v -> { Config.disableImeInGame = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(OptimizePage.class, 1, new SettingModule(
                UiText.t("更好的聊天栏", "Better Chat"),
                UiText.t("聊天动画与玩家头像", "Chat animations and player heads"),
                new SettingToggle(() -> Config.betterChat, v -> { Config.betterChat = v; Config.save(); }))
                .addSub(UiText.t("样式", "Style"), "",
                        new SettingCycle(List.of("Classic", "New", "Blur"), () -> Config.betterChatStyle.ordinal(),
                                 i -> { Config.betterChatStyle = Config.BetterChatStyle.values()[i]; Config.save(); }))
                .addSub(UiText.t("更好的字体", "Better Font"), UiText.t("仅 New / Blur 样式使用客户端字体", "Use the client font in New / Blur styles only"),
                        new SettingToggle(() -> Config.betterChatBetterFont, v -> { Config.betterChatBetterFont = v; Config.save(); }))
                .addSub(UiText.t("消息入场动画", "Message Animation"), UiText.t("新消息出现时滑入", "Slide in new chat messages"),
                        new SettingToggle(() -> Config.betterChatMessageAnimation, v -> { Config.betterChatMessageAnimation = v; Config.save(); }))
                .addSub(UiText.t("输入栏动画", "Input Bar Animation"), UiText.t("打开或关闭聊天界面时的动画", "Animate the chat input bar opening and closing"),
                        new SettingToggle(() -> Config.betterChatInputAnimation, v -> { Config.betterChatInputAnimation = v; Config.save(); }))
                .addSub(UiText.t("聊天头像", "Chat Heads"), UiText.t("在聊天消息旁显示玩家头像", "Show player heads next to chat messages"),
                        new SettingToggle(() -> Config.betterChatAvatar, v -> { Config.betterChatAvatar = v; Config.save(); }))
                .addSub(UiText.t("消息动画时间", "Message Fade Time"), "",
                         new SettingSlider(100.0, 900.0, "%.0fms", () -> (double) (Config.betterChatStyle != Config.BetterChatStyle.CLASSIC ? Config.betterChatNewAnimationTime : Config.betterChatMessageFadeTime), v -> {
                             if (Config.betterChatStyle != Config.BetterChatStyle.CLASSIC) Config.betterChatNewAnimationTime = v.intValue();
                             else Config.betterChatMessageFadeTime = v.intValue();
                             Config.save();
                         }))
                .addSub(UiText.t("输入栏动画时间", "Input Fade Time"), "",
                        new SettingSlider(100.0, 900.0, "%.0fms", () -> (double) Config.betterChatInputFadeTime, v -> { Config.betterChatInputFadeTime = v.intValue(); Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(OptimizePage.class, 6, new SettingModule(
                UiText.t("更好的标题", "Better Title"),
                UiText.t("倾斜滚动切换与可选的字体、渐变和光晕", "Tilted scrolling transitions with optional fonts, gradients, and glow"),
                new SettingToggle(() -> Config.betterTitle, v -> { Config.betterTitle = v; Config.save(); }))
                .addSub(UiText.t("更好的字体", "Better Font"), UiText.t("关闭后使用原版／资源包字体", "Use the vanilla or resource-pack font when disabled"),
                        new SettingToggle(() -> Config.betterTitleBetterFont, v -> { Config.betterTitleBetterFont = v; Config.save(); }))
                .addSub(UiText.t("文字光晕", "Text Glow"), UiText.t("独立控制光晕，不影响切换模糊", "Toggle glow independently of transition blur"),
                        new SettingToggle(() -> Config.betterTitleGlow, v -> { Config.betterTitleGlow = v; Config.save(); }))
                .addSub(UiText.t("流动渐变", "Flowing Gradient"), UiText.t("关闭后保留服务器文字颜色", "Keep server text colors when disabled"),
                        new SettingToggle(() -> Config.betterTitleGradient, v -> { Config.betterTitleGradient = v; Config.save(); }))
                .addSub(UiText.t("缩放", "Scale"), "",
                        new SettingSlider(50, 150, "%.0f%%", () -> (double) Config.betterTitleScale * 100,
                                v -> { Config.betterTitleScale = v.floatValue() / 100f; Config.save(); }))
                .addSub(UiText.t("动画速度", "Animation Speed"), "",
                        new SettingSlider(0.5, 2, "%.1fx", () -> (double) Config.betterTitleAnimationSpeed,
                                v -> { Config.betterTitleAnimationSpeed = v.floatValue(); Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(OptimizePage.class, 2, new SettingModule(
                UiText.t("更好的计分板", "Better Scoreboard"),
                UiText.t("调整计分板样式、位置和大小", "Customize the scoreboard appearance, position, and size"),
                new SettingToggle(() -> Config.betterScoreboard, v -> { Config.betterScoreboard = v; Config.save(); }))
                .addSub(UiText.t("隐藏红色数字", "Hide Red Numbers"), "",
                        new SettingToggle(() -> Config.betterScoreboardHideScores, v -> { Config.betterScoreboardHideScores = v; Config.save(); }))
                .addSub(UiText.t("视觉改进", "Visual Improvement"), "",
                        new SettingToggle(() -> Config.betterScoreboardVisualImprovement, v -> { Config.betterScoreboardVisualImprovement = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(OptimizePage.class, 3, new SettingModule(
                UiText.t("更好的物品栏", "Better Item Selector"),
                UiText.t("圆角快捷栏与平滑选中框", "Rounded hotbar with a smooth selection indicator"),
                new SettingToggle(() -> Config.betterItemSelector, v -> { Config.betterItemSelector = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(OptimizePage.class, 4, new SettingModule(
                UiText.t("更好的鼠标逻辑", "Better Mouse Logic"),
                UiText.t("优化物品栏内的鼠标交互", "Improve mouse interaction in inventories"),
                new SettingToggle(() -> Config.betterMouseLogic, v -> { Config.betterMouseLogic = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(OptimizePage.class, 5, new SettingModule(
                UiText.t("平滑快捷栏", "Smooth Hotbar"),
                UiText.t("平滑快捷栏选中框的移动", "Smooth the hotbar selection transition"),
                new SettingToggle(() -> Config.smoothHotbarScrolling, v -> { Config.smoothHotbarScrolling = v; Config.save(); }))
                .addSub(UiText.t("动画速度", "Animation Speed"), "",
                        new SettingSlider(0.05, 0.99, "%.2f", () -> (double) Config.smoothHotbarAnimationSpeed, v -> { Config.smoothHotbarAnimationSpeed = v.floatValue(); Config.save(); }))));
        SettingModule scoreboard = HudAppearanceSettings.module(modules, "更好的计分板", "Better Scoreboard");
        HudAppearanceSettings.add(scoreboard, () -> Config.scoreboardAppearance,
                () -> Config.betterScoreboardVisualImprovement);
        SettingModule hotbar = HudAppearanceSettings.module(modules, "更好的物品栏", "Better Item Selector");
        HudAppearanceSettings.hotbar(hotbar, () -> Config.hotbarAppearance);

        modules.add(ModuleBindingIds.preserve(RenderPage.class, 13, new SettingModule(UiText.t("更好的延迟显示", "Better Ping Display"), UiText.t("在玩家列表中用数字显示延迟", "Show latency as numbers in the player list"),
                new SettingToggle(() -> Config.betterPingDisplay, v -> { Config.betterPingDisplay = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(ToolPage.class, 6 + (Config.restrictedFeaturesUnlocked() ? 2 : 0), new SettingModule(UiText.t("自动疾跑", "Auto Sprint"), UiText.t("前进时自动进入疾跑状态", "Automatically sprint while moving forward"),
                new SettingToggle(() -> Config.autoSprint, v -> { Config.autoSprint = v; Config.save(); }))));

        modules.add(ModuleBindingIds.preserve(ToolPage.class, 7 + (Config.restrictedFeaturesUnlocked() ? 2 : 0), new SettingModule(UiText.t("禁止游泳", "No Swimming"), UiText.t("禁止进入游泳姿态", "Prevent entering the swimming pose"),
                new SettingToggle(() -> Config.noSwimming, v -> { Config.noSwimming = v; Config.save(); }))));

    }

    @Override public String getTitle() { return UiText.t("优化设置", "Optimize Settings"); }
    @Override public String getSubtitle() { return UiText.t("性能与优化相关参数", "Performance and optimization options"); }
}
