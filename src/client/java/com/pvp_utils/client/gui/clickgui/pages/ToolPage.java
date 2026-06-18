package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.Version;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.widget.*;
import com.pvp_utils.client.modules.impl.Tool.FakePlayerManager;

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

        modules.add(new SettingModule(UiText.t("方块数量显示", "Block Count Display"), UiText.t("右键放置方块时显示方块数量、移动速度和点击速度", "Show block count, movement speed, and click speed while right-clicking blocks"),
                new SettingToggle(() -> Config.blockCountDisplay, v -> { Config.blockCountDisplay = v; Config.save(); })));

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
