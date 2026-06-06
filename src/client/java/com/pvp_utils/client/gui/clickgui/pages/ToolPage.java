package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.widget.*;

public class ToolPage extends BasePage {

    public ToolPage() {
        modules.add(new SettingModule("自动截图", "击杀时自动截图保存",
                new SettingToggle(() -> Config.autoScreenshot, v -> { Config.autoScreenshot = v; Config.save(); })));

        modules.add(new SettingModule("摔落伤害预测", "预测摔落伤害数值",
                new SettingToggle(() -> Config.fallDamagePredict, v -> { Config.fallDamagePredict = v; Config.save(); })));

        modules.add(new SettingModule("伤害数值记录", "记录每次造成的伤害",
                new SettingToggle(() -> Config.damageRecord, v -> { Config.damageRecord = v; Config.save(); })));

        modules.add(new SettingModule("左键存入容器", "松开左键后将主手物品放入准星容器",
                new SettingToggle(() -> Config.autoChestDeposit, v -> { Config.autoChestDeposit = v; Config.save(); }))
                .addSub("开箱延迟", "松开左键后等待的 tick",
                        new SettingSlider(0, 40, "%.0f", () -> (double) Config.autoChestDepositOpenDelay,
                                v -> { Config.autoChestDepositOpenDelay = v.intValue(); Config.save(); }))
                .addSub("放入延迟", "打开容器后等待的 tick",
                        new SettingSlider(0, 40, "%.0f", () -> (double) Config.autoChestDepositTransferDelay,
                                v -> { Config.autoChestDepositTransferDelay = v.intValue(); Config.save(); }))
                .addSub("关闭延迟", "放入物品后等待的 tick",
                        new SettingSlider(0, 40, "%.0f", () -> (double) Config.autoChestDepositCloseDelay,
                                v -> { Config.autoChestDepositCloseDelay = v.intValue(); Config.save(); })));
    }

    @Override public String getTitle() { return "工具设置"; }
    @Override public String getSubtitle() { return "实用工具与辅助功能"; }
}
