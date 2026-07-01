package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.widget.SettingModule;
import com.pvp_utils.client.gui.clickgui.widget.SettingSlider;
import com.pvp_utils.client.gui.clickgui.widget.SettingToggle;

public class OptimizePage extends BasePage {
    public OptimizePage() {
        modules.add(new SettingModule(UiText.t("游戏内输入法修复", "IME Fix In Game"), UiText.t("修复中文、日文、韩文输入法在游戏中会导致无法操作的问题", "Fix Chinese, Japanese, and Korean input methods causing controls to stop working in game"),
                new SettingToggle(() -> Config.disableImeInGame, v -> { Config.disableImeInGame = v; Config.save(); })));

        modules.add(new SettingModule(
                UiText.t("更好的聊天栏", "Better Chat"),
                UiText.t("对与聊天栏的优化与改进", "Optimizations and improvements for the chat bar"),
                new SettingToggle(() -> Config.betterChat, v -> { Config.betterChat = v; Config.save(); }))
                .addSub(UiText.t("消息入场动画", "Message Animation"), UiText.t("新消息出现时滑入", "Slide in new chat messages"),
                        new SettingToggle(() -> Config.betterChatMessageAnimation, v -> { Config.betterChatMessageAnimation = v; Config.save(); }))
                .addSub(UiText.t("输入栏动画", "Input Bar Animation"), UiText.t("打开或关闭聊天界面时的动画", "Animate the chat input bar opening and closing"),
                        new SettingToggle(() -> Config.betterChatInputAnimation, v -> { Config.betterChatInputAnimation = v; Config.save(); }))
                .addSub(UiText.t("聊天头像", "Chat Heads"), UiText.t("在聊天消息旁显示玩家头像", "Show player heads next to chat messages"),
                        new SettingToggle(() -> Config.betterChatAvatar, v -> { Config.betterChatAvatar = v; Config.save(); }))
                .addSub(UiText.t("消息动画时间", "Message Fade Time"), UiText.t("新消息的动画时长", "Duration for incoming message animation"),
                        new SettingSlider(100.0, 900.0, "%.0fms", () -> (double) Config.betterChatMessageFadeTime, v -> { Config.betterChatMessageFadeTime = v.intValue(); Config.save(); }))
                .addSub(UiText.t("输入栏动画时间", "Input Fade Time"), UiText.t("聊天输入栏的动画时长", "Duration for chat input bar animation"),
                        new SettingSlider(100.0, 900.0, "%.0fms", () -> (double) Config.betterChatInputFadeTime, v -> { Config.betterChatInputFadeTime = v.intValue(); Config.save(); })));

        modules.add(new SettingModule(
                UiText.t("平滑快捷栏", "Smooth Hotbar"),
                UiText.t("让快捷栏滚轮切换时带有平滑过渡，并支持循环滚动", "Make hotbar scrolling smooth and support rollover"),
                new SettingToggle(() -> Config.smoothHotbarScrolling, v -> { Config.smoothHotbarScrolling = v; Config.save(); })));
    }

    @Override public String getTitle() { return UiText.t("优化设置", "Optimize Settings"); }
    @Override public String getSubtitle() { return UiText.t("性能与优化相关参数", "Performance and optimization options"); }
}
