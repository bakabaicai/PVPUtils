package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.widget.*;

public class ToolPage extends BasePage {

    public ToolPage() {
        modules.add(new SettingModule(UiText.t("自动截图", "Auto Screenshot"), UiText.t("击杀时自动截图保存", "Automatically save a screenshot after a kill"),
                new SettingToggle(() -> Config.autoScreenshot, v -> { Config.autoScreenshot = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("摔落伤害预测", "Fall Damage Prediction"), UiText.t("预测摔落伤害数值", "Predict fall damage value"),
                new SettingToggle(() -> Config.fallDamagePredict, v -> { Config.fallDamagePredict = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("伤害数值记录", "Damage Record"), UiText.t("记录每次造成的伤害", "Record each damage value dealt"),
                new SettingToggle(() -> Config.damageRecord, v -> { Config.damageRecord = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("左键存入容器", "Left-Click Deposit"), UiText.t("松开左键后将主手物品放入准星容器", "Put the held item into the targeted container after releasing left click"),
                new SettingToggle(() -> Config.autoChestDeposit, v -> { Config.autoChestDeposit = v; Config.save(); }))
                .addSub(UiText.t("阻止移动", "Block Movement"), UiText.t("自动存入期间阻止玩家移动输入", "Block player movement input during auto deposit"),
                        new SettingToggle(() -> Config.autoChestDepositBlockMovement,
                                v -> { Config.autoChestDepositBlockMovement = v; Config.save(); }))
                .addSub(UiText.t("开箱延迟", "Open Delay"), UiText.t("松开左键后等待的 tick", "Ticks to wait after releasing left click"),
                        new SettingSlider(0, 40, "%.0f", () -> (double) Config.autoChestDepositOpenDelay,
                                v -> { Config.autoChestDepositOpenDelay = v.intValue(); Config.save(); }))
                .addSub(UiText.t("放入延迟", "Transfer Delay"), UiText.t("打开容器后等待的 tick", "Ticks to wait after opening the container"),
                        new SettingSlider(0, 40, "%.0f", () -> (double) Config.autoChestDepositTransferDelay,
                                v -> { Config.autoChestDepositTransferDelay = v.intValue(); Config.save(); }))
                .addSub(UiText.t("关闭延迟", "Close Delay"), UiText.t("放入物品后等待的 tick", "Ticks to wait after depositing the item"),
                        new SettingSlider(0, 40, "%.0f", () -> (double) Config.autoChestDepositCloseDelay,
                                v -> { Config.autoChestDepositCloseDelay = v.intValue(); Config.save(); })));
    }

    @Override public String getTitle() { return UiText.t("工具设置", "Tool Settings"); }
    @Override public String getSubtitle() { return UiText.t("实用工具与辅助功能", "Utility and helper features"); }
}
