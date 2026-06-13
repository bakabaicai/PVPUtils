package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.widget.SettingModule;
import com.pvp_utils.client.gui.clickgui.widget.SettingToggle;

public class OptimizePage extends BasePage {
    public OptimizePage() {
        modules.add(new SettingModule(UiText.t("游戏内输入法修复", "IME Fix In Game"), UiText.t("修复中文、日文、韩文输入法在游戏中会导致无法操作的问题", "Fix Chinese, Japanese, and Korean input methods causing controls to stop working in game"),
                new SettingToggle(() -> Config.disableImeInGame, v -> { Config.disableImeInGame = v; Config.save(); })));
    }

    @Override public String getTitle() { return UiText.t("优化设置", "Optimize Settings"); }
    @Override public String getSubtitle() { return UiText.t("性能与优化相关参数", "Performance and optimization options"); }
}
