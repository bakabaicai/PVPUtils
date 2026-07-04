package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.Version;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.widget.*;
import com.pvp_utils.client.modules.impl.Render.CustomCapeManager;
import com.pvp_utils.client.modules.impl.Tool.FakePlayerManager;

import java.util.List;

public class ToolPage extends BasePage {

    public ToolPage() {
        modules.add(new SettingModule(UiText.t("自动截图", "Auto Screenshot"), UiText.t("在你胜利时自动截图并保存至桌面", "Automatically take a screenshot when you win and save it to the desktop"),
                new SettingToggle(() -> Config.autoScreenshot, v -> { Config.autoScreenshot = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("摔落伤害预测", "Fall Damage Prediction"), UiText.t("预测摔落伤害数值", "Predict fall damage value"),
                new SettingToggle(() -> Config.fallDamagePredict, v -> { Config.fallDamagePredict = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("自动疾跑", "Auto Sprint"), UiText.t("前进时自动进入疾跑状态", "Automatically sprint while moving forward"),
                new SettingToggle(() -> Config.autoSprint, v -> { Config.autoSprint = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("钓鱼竿辅助", "Fishing Rod Assist"), UiText.t("切换到钓鱼竿时自动右键使用", "Automatically right-click when switching to a fishing rod"),
                new SettingToggle(() -> Config.fishingRodAssist, v -> { Config.fishingRodAssist = v; Config.save(); }))
                .addSub(UiText.t("使用间隔(tick)", "Use Delay (tick)"), UiText.t("切换到钓鱼竿格子后等待多久再使用", "Ticks to wait after switching to a fishing rod slot before using it"),
                        new SettingSlider(0, 20, "%.0f", () -> (double) Config.fishingRodAssistUseDelay,
                                v -> { Config.fishingRodAssistUseDelay = v.intValue(); Config.save(); }))
                .visibleWhen(() -> Config.fullMode));

        modules.add(new SettingModule(UiText.t("方块数量显示", "Block Count Display"), UiText.t("右键放置方块时显示方块数量、放置速度和点击速度", "Show block count, placement speed, and click speed while right-clicking blocks"),
                new SettingToggle(() -> Config.blockCountDisplay, v -> { Config.blockCountDisplay = v; Config.save(); }))
                .addSub(UiText.t("样式", "Mode"), UiText.t("选择方块数量显示样式", "Choose the Block Count Display style"),
                        new SettingCycle(List.of("New", "Blur"),
                                () -> Config.blockCountDisplayMode == Config.BlockCountDisplayMode.NEW ? 0 : 1,
                                i -> { Config.blockCountDisplayMode = i == 0 ? Config.BlockCountDisplayMode.NEW : Config.BlockCountDisplayMode.BLUR; Config.save(); })));

        modules.add(new SettingModule(UiText.t("改变客户端时间", "Time Change"), UiText.t("强制修改客户端显示的世界时间", "Force the client-side world time"),
                new SettingToggle(() -> Config.timeChange, v -> { Config.timeChange = v; Config.save(); }))
                .addSub(UiText.t("时间", "Time"), UiText.t("选择客户端显示的世界时间", "Choose the client-side world time"),
                        new SettingSlider(0, 23999, "%.0f", () -> (double) Config.clientTime,
                                v -> { Config.clientTime = v.intValue(); Config.save(); })));

        modules.add(new SettingModule(UiText.t("改变客户端天气", "Weather Change"), UiText.t("强制修改客户端显示的天气", "Force the client-side weather"),
                new SettingToggle(() -> Config.weatherChange, v -> { Config.weatherChange = v; Config.save(); }))
                .addSub(UiText.t("天气模式", "Weather Mode"), UiText.t("选择客户端显示的天气", "Choose the client-side weather"),
                        new SettingCycle(List.of(
                                UiText.t("晴天", "Clear"),
                                UiText.t("雨天", "Rain"),
                                UiText.t("雪天", "Snow"),
                                UiText.t("雷暴", "Thunder")),
                                () -> Config.weatherMode.ordinal(),
                                i -> { Config.weatherMode = Config.WeatherMode.values()[i % Config.WeatherMode.values().length]; Config.save(); })));

        modules.add(new SettingModule(UiText.t("缩放", "Zoom"), UiText.t("使用快捷键进行缩放，在设置中可以调整键位", "Use a keybind to zoom. The key can be changed in controls"),
                new SettingToggle(() -> Config.zoom, v -> { Config.zoom = v; Config.save(); }))
                .addSub(UiText.t("缩放倍率", "Zoom Amount"), UiText.t("按下缩放键时的初始缩放倍率", "Initial zoom multiplier while holding the zoom key"),
                        new SettingSlider(2.0, 20.0, "%.0fx", () -> (double) Config.zoomAmount,
                                v -> { Config.zoomAmount = Math.max(2, Math.min(20, v.intValue())); Config.save(); }))
                .addSub(UiText.t("滚轮缩放", "Scroll Zoom"), UiText.t("按住缩放键时使用滚轮调整缩放倍率", "Use the mouse wheel to adjust zoom while holding the zoom key"),
                        new SettingToggle(() -> Config.zoomScroll, v -> { Config.zoomScroll = v; Config.save(); }))
                .addSubWhen(() -> Config.zoomScroll, UiText.t("滚轮档位", "Scroll Steps"), UiText.t("滚轮缩放允许的最大档位", "Maximum number of scroll zoom steps"),
                        new SettingSlider(1.0, 20.0, "%.0f", () -> (double) Config.zoomScrollSteps,
                                v -> { Config.zoomScrollSteps = Math.max(1, Math.min(20, v.intValue())); Config.save(); }))
                .addSubWhen(() -> Config.zoomScroll, UiText.t("每档倍率", "Step Multiplier"), UiText.t("每滚动一档增加的缩放倍率", "Zoom multiplier added per scroll step"),
                        new SettingSlider(110.0, 250.0, "%.0f%%", () -> (double) Config.zoomPerStep,
                                v -> { Config.zoomPerStep = Math.max(110, Math.min(250, v.intValue())); Config.save(); }))
                .addSub(UiText.t("进入时间", "Zoom In Time"), UiText.t("缩放进入动画耗时", "Zoom-in transition duration"),
                        new SettingSlider(0.0, 1.5, "%.2fs", () -> (double) Config.zoomInTime,
                                v -> { Config.zoomInTime = Math.max(0.0f, Math.min(1.5f, v.floatValue())); Config.save(); }))
                .addSub(UiText.t("退出时间", "Zoom Out Time"), UiText.t("缩放退出动画耗时", "Zoom-out transition duration"),
                        new SettingSlider(0.0, 1.5, "%.2fs", () -> (double) Config.zoomOutTime,
                                v -> { Config.zoomOutTime = Math.max(0.0f, Math.min(1.5f, v.floatValue())); Config.save(); }))
                .addSub(UiText.t("缩放相机灵敏度", "Zoom Camera Sensitivity"), "",
                        new SettingSlider(0.0, 100.0, "%.0f%%", () -> (double) Config.zoomRelativeSensitivity,
                                v -> { Config.zoomRelativeSensitivity = Math.max(0, Math.min(100, v.intValue())); Config.save(); })));

        modules.add(new SettingModule(UiText.t("自定义披风", "Custom Cape"), UiText.t("加载本地的自定义皮肤文件", "Load a local custom skin file"),
                new SettingToggle(() -> Config.customCape, v -> { Config.customCape = v; Config.save(); }))
                .addSub(UiText.t("打开目录", "Open Folder"), UiText.t("打开自定义披风文件夹", "Open the custom cape folder"),
                        new SettingButton(UiText.t("打开", "Open"), CustomCapeManager::openFolder))
                .addSub(UiText.t("切换披风", "Switch Cape"), UiText.t("切换当前使用的披风文件", "Switch the selected cape file"),
                        new SettingButton(() -> Config.customCapeImage, CustomCapeManager::cycleCape)));

        if (Version.DEBUG) {
            modules.add(new SettingModule(UiText.t("FakePlayer", "FakePlayer"), UiText.t("测试功能请勿开启", "Test feature, do not enable"),
                    new SettingToggle(FakePlayerManager::isEnabled, FakePlayerManager::setEnabled))
                    .addSub(UiText.t("盔甲", "Armor"), UiText.t("控制假玩家是否穿着下界合金甲", "Control whether the fake player wears netherite armor"),
                            new SettingToggle(FakePlayerManager::hasArmor, FakePlayerManager::setArmor))
                    .addSub(UiText.t("不死图腾", "Totem"), UiText.t("控制假玩家手上是否持有不死图腾", "Control whether the fake player holds a Totem of Undying"),
                            new SettingToggle(FakePlayerManager::hasTotem, FakePlayerManager::setTotem)));
        }

        modules.add(new SettingModule(UiText.t("快捷存入", "Quick Deposit"), UiText.t("手持物品并左键点击容器时快捷存入手中的物品", "Quickly deposit the held item when left-clicking a container while holding an item"),
                new SettingToggle(() -> Config.autoChestDeposit, v -> { Config.autoChestDeposit = v; Config.save(); }))
                .addSub(UiText.t("仅限资源", "Resources Only"), UiText.t("只存入资源", "Only deposit resources"),
                        new SettingToggle(() -> Config.autoChestDepositResourcesOnly,
                                v -> { Config.autoChestDepositResourcesOnly = v; Config.save(); }))
                .addSub(UiText.t("存入延迟", "Deposit Delay"), UiText.t("打开容器后存入物品的时间(tick)", "Ticks to wait after opening the container before depositing the item"),
                        new SettingSlider(0, 40, "%.0f", () -> (double) Config.autoChestDepositDepositDelay,
                                v -> { Config.autoChestDepositDepositDelay = v.intValue(); Config.save(); }))
                .addSub(UiText.t("关闭容器延迟", "Close Container Delay"), UiText.t("存入物品后关闭容器所等待的时间(tick)", "Ticks to wait after depositing the item before closing the container"),
                        new SettingSlider(0, 40, "%.0f", () -> (double) Config.autoChestDepositCloseDelay,
                                v -> { Config.autoChestDepositCloseDelay = v.intValue(); Config.save(); }))
                .visibleWhen(() -> Config.fullMode));
    }

    @Override public String getTitle() { return UiText.t("工具设置", "Tool Settings"); }
    @Override public String getSubtitle() { return UiText.t("实用工具与辅助功能", "Utility and helper features"); }
}
