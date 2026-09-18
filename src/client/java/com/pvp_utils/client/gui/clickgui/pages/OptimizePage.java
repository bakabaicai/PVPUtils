package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.ConfigPresets;
import com.pvp_utils.client.gui.clickgui.ChatWindowScreen;
import com.pvp_utils.client.gui.clickgui.ChannelChatScreen;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.widget.SettingButton;
import com.pvp_utils.client.gui.clickgui.widget.SettingModule;
import com.pvp_utils.client.gui.clickgui.widget.SettingSlider;
import com.pvp_utils.client.gui.clickgui.widget.SettingToggle;
import net.minecraft.client.Minecraft;

public class OptimizePage extends BasePage {
    public OptimizePage() {
        modules.add(new SettingModule(UiText.t("配置预设", "Config Presets"), UiText.t("一键应用整组配置方案", "Apply a whole config preset in one click"), null)
                .addSub(UiText.t("默认", "Default"), UiText.t("恢复推荐的默认设置", "Restore recommended default settings"),
                        new SettingButton(UiText.t("应用", "Apply"), ConfigPresets::applyDefault))
                .addSub(UiText.t("PVP", "PVP"), UiText.t("开启攻击特效与实用辅助", "Enable attack effects and useful helpers"),
                        new SettingButton(UiText.t("应用", "Apply"), ConfigPresets::applyPvp))
                .addSub(UiText.t("低配优化", "Low-End"), UiText.t("关闭特效并开启实体/粒子/内存优化", "Disable effects and enable entity/particle/memory optimization"),
                        new SettingButton(UiText.t("应用", "Apply"), ConfigPresets::applyLowEnd)));

        modules.add(new SettingModule(UiText.t("粒子优化", "Particle Optimization"), UiText.t("隐藏指定类型的粒子，提升低配设备帧率", "Hide certain particle types to boost FPS on low-end devices"), null)
                .addSub(UiText.t("隐藏爆炸粒子", "Hide Explosions"), UiText.t("隐藏爆炸、烟雾粒子", "Hide explosion and smoke particles"),
                        new SettingToggle(() -> Config.hideExplosionParticles, v -> { Config.hideExplosionParticles = v; Config.save(); }))
                .addSub(UiText.t("隐藏雨滴粒子", "Hide Rain"), UiText.t("隐藏降雨及水滴粒子", "Hide rain and dripping water particles"),
                        new SettingToggle(() -> Config.hideRainParticles, v -> { Config.hideRainParticles = v; Config.save(); }))
                .addSub(UiText.t("隐藏火焰粒子", "Hide Fire"), UiText.t("隐藏火焰、灵魂火、岩浆粒子", "Hide flame, soul fire and lava particles"),
                        new SettingToggle(() -> Config.hideFireParticles, v -> { Config.hideFireParticles = v; Config.save(); }))
                .addSub(UiText.t("隐藏环境粒子", "Hide Ambient"), UiText.t("隐藏气泡、音符、附魔、图腾等环境粒子", "Hide bubble, note, enchant, totem and other ambient particles"),
                        new SettingToggle(() -> Config.hideAmbientParticles, v -> { Config.hideAmbientParticles = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("游戏内输入法修复", "IME Fix In Game"), UiText.t("修复中文、日文、韩文输入法在游戏中会导致无法操作的问题", "Fix Chinese, Japanese, and Korean input methods causing controls to stop working in game"),
                new SettingToggle(() -> Config.disableImeInGame, v -> { Config.disableImeInGame = v; Config.save(); })));

        modules.add(new SettingModule(
                UiText.t("更好的聊天栏", "Better Chat"),
                UiText.t("对于聊天栏的优化与改进", "Optimizations and improvements for the chat bar"),
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
                        new SettingSlider(100.0, 900.0, "%.0fms", () -> (double) Config.betterChatInputFadeTime, v -> { Config.betterChatInputFadeTime = v.intValue(); Config.save(); }))
                .addSub(UiText.t("聊天窗口", "Chat Window"), UiText.t("打开独立的聊天记录窗口", "Open a separate chat history window"),
                        new SettingButton(UiText.t("打开", "Open"), () -> Minecraft.getInstance().setScreen(new ChatWindowScreen())))
                .addSub(UiText.t("频道聊天", "Channel Chat"), UiText.t("PVPUtils 玩家间频道消息，需服务端安装 PVPUtils-Server", "PVPUtils player channel chat, requires PVPUtils-Server on the server"),
                        new SettingButton(UiText.t("打开", "Open"), () -> Minecraft.getInstance().setScreen(new ChannelChatScreen()))));

        modules.add(new SettingModule(
                UiText.t("更好的计分板", "Better Scoreboard"),
                UiText.t("开启后可在 HUD 编辑器中拖动和缩放计分板", "Allows moving and scaling the scoreboard in the HUD editor"),
                new SettingToggle(() -> Config.betterScoreboard, v -> { Config.betterScoreboard = v; Config.save(); }))
                .addSub(UiText.t("隐藏红色数字", "Hide Red Numbers"), "",
                        new SettingToggle(() -> Config.betterScoreboardHideScores, v -> { Config.betterScoreboardHideScores = v; Config.save(); }))
                .addSub(UiText.t("视觉改进", "Visual Improvement"), "",
                        new SettingToggle(() -> Config.betterScoreboardVisualImprovement, v -> { Config.betterScoreboardVisualImprovement = v; Config.save(); })));

        modules.add(new SettingModule(
                UiText.t("更好的物品栏", "Better Item Selector"),
                UiText.t("使用 GPU 渲染圆角快捷栏和动画选中框", "Render a rounded GPU hotbar with an animated selector"),
                new SettingToggle(() -> Config.betterItemSelector, v -> { Config.betterItemSelector = v; Config.save(); })));

        modules.add(new SettingModule(
                UiText.t("更好的鼠标逻辑", "Better Mouse Logic"),
                UiText.t("改进鼠标指针的工作逻辑，让他更加简洁易用。（这只针对于物品栏）", "Improve how the mouse cursor works, making it cleaner and easier to use. (Inventory only)"),
                new SettingToggle(() -> Config.betterMouseLogic, v -> { Config.betterMouseLogic = v; Config.save(); })));

        modules.add(new SettingModule(
                UiText.t("实体渲染优化", "Entity Render Optimize"),
                UiText.t("限制远处实体的渲染距离，减少实体渲染开销提升帧数", "Limit entity render distance to improve FPS"),
                new SettingToggle(() -> Config.entityOptimize, v -> { Config.entityOptimize = v; Config.save(); }))
                .addSub(UiText.t("通用实体距离", "Entity Distance"), UiText.t("0 为关闭，超出此距离的实体不渲染（玩家除外）", "0 to disable, entities beyond this distance are not rendered (players excluded)"),
                        new SettingSlider(0.0, 256.0, "%.0f格", () -> (double) Config.entityRenderDistance, v -> { Config.entityRenderDistance = v.intValue(); Config.save(); }))
                .addSub(UiText.t("掉落物距离", "Item Distance"), UiText.t("0 为关闭，掉落物超出此距离不渲染", "0 to disable, dropped items beyond this distance are not rendered"),
                        new SettingSlider(0.0, 128.0, "%.0f格", () -> (double) Config.itemRenderDistance, v -> { Config.itemRenderDistance = v.intValue(); Config.save(); }))
                .addSub(UiText.t("经验球距离", "XP Orb Distance"), UiText.t("0 为关闭，经验球超出此距离不渲染", "0 to disable, XP orbs beyond this distance are not rendered"),
                        new SettingSlider(0.0, 128.0, "%.0f格", () -> (double) Config.xpOrbRenderDistance, v -> { Config.xpOrbRenderDistance = v.intValue(); Config.save(); }))
                .addSub(UiText.t("箭矢距离", "Arrow Distance"), UiText.t("0 为关闭，箭矢超出此距离不渲染", "0 to disable, arrows beyond this distance are not rendered"),
                        new SettingSlider(0.0, 128.0, "%.0f格", () -> (double) Config.arrowRenderDistance, v -> { Config.arrowRenderDistance = v.intValue(); Config.save(); })));

        modules.add(new SettingModule(
                UiText.t("内存自动清理", "Memory Auto Clean"),
                UiText.t("周期性清理模组产生的缓存，防止长时间游玩内存占用增长", "Periodically clean mod caches to prevent memory growth"),
                new SettingToggle(() -> Config.cacheCleaner, v -> { Config.cacheCleaner = v; Config.save(); }))
                .addSub(UiText.t("清理间隔", "Clean Interval"), UiText.t("每隔多少分钟清理一次缓存", "Clean caches every N minutes"),
                        new SettingSlider(1.0, 60.0, "%.0f分钟", () -> (double) Config.cacheCleanerInterval, v -> { Config.cacheCleanerInterval = v.intValue(); Config.save(); })));

        modules.add(new SettingModule(
                UiText.t("平滑快捷栏", "Smooth Hotbar"),
                UiText.t("让快捷栏滚轮切换时带有平滑过渡", "Make hotbar scrolling smooth"),
                new SettingToggle(() -> Config.smoothHotbarScrolling, v -> { Config.smoothHotbarScrolling = v; Config.save(); }))
                .addSub(UiText.t("动画速度", "Animation Speed"), UiText.t("控制选中框移动到目标格子的平滑速度", "Controls how fast the selector animates to the target slot"),
                        new SettingSlider(0.05, 0.99, "%.2f", () -> (double) Config.smoothHotbarAnimationSpeed, v -> { Config.smoothHotbarAnimationSpeed = v.floatValue(); Config.save(); })));
    }

    @Override public String getTitle() { return UiText.t("优化设置", "Optimize Settings"); }
    @Override public String getSubtitle() { return UiText.t("性能与优化相关参数", "Performance and optimization options"); }
}
