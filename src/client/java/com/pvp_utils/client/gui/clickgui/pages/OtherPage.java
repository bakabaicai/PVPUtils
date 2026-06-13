package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.widget.*;

import java.util.List;

public class OtherPage extends BasePage {

    public OtherPage() {
        modules.add(new SettingModule(UiText.t("胜利音效", "Victory Sound"), UiText.t("击杀时播放胜利音效", "Play a victory sound after a kill"),
                new SettingToggle(() -> Config.victorySound, v -> { Config.victorySound = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("自动疾跑", "Auto Sprint"), UiText.t("前进时自动进入疾跑状态", "Automatically sprint while moving forward"),
                new SettingToggle(() -> Config.autoSprint, v -> { Config.autoSprint = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("游戏内输入法修复", "IME Fix In Game"), UiText.t("游玩时禁用输入法", "Disable input method while playing"),
                new SettingToggle(() -> Config.disableImeInGame, v -> { Config.disableImeInGame = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("语言切换", "Language Switch"), UiText.t("切换界面显示语言", "Switch the interface display language"), null)
                .addSub(UiText.t("语言", "Language"), UiText.t("选择语言，关闭并重新打开界面后生效", "Choose language, close and reopen the UI to apply changes"),
                        new SettingCycle(List.of(UiText.t("中文", "Chinese"), UiText.t("英文", "English")),
                                () -> Config.isChinese ? 0 : 1,
                                i -> { Config.isChinese = i == 0; Config.save(); })));
    }

    @Override public String getTitle() { return UiText.t("其他设置", "Other Settings"); }
    @Override public String getSubtitle() { return UiText.t("其他杂项功能", "Other miscellaneous features"); }
}
