package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.widget.*;
import com.pvp_utils.client.modules.impl.Misc.VictorySound;
import com.pvp_utils.client.web.WebGUIServer;

import java.util.List;

public class MiscPage extends BasePage {

    public MiscPage() {
        modules.add(new SettingModule(UiText.t("打开 WebGUI", "Open WebGUI"), UiText.t("打开 WebHUD", "Open WebHUD"),
                new SettingToggle(WebGUIServer::isEnabled, WebGUIServer::setEnabled)));

        modules.add(new SettingModule(UiText.t("胜利音效", "Victory Sound"), UiText.t("在你胜利时播放自定义音效", "Play a custom sound when you win"),
                new SettingToggle(() -> Config.victorySound, v -> { Config.victorySound = v; Config.save(); }))
                .addSub(UiText.t("打开自定义音效文件夹", "Open custom sound folder"), "", new SettingButton(UiText.t("打开", "Open"), VictorySound::openSoundsFolder)));

        modules.add(new SettingModule(UiText.t("语言切换", "Language Switch"), UiText.t("切换界面显示语言", "Switch the interface display language"), null)
                .addSub(UiText.t("语言", "Language"), UiText.t("选择语言，关闭并重新打开界面后生效", "Choose language, close and reopen the UI to apply changes"),
                        new SettingCycle(List.of(UiText.t("中文", "Chinese"), UiText.t("英文", "English")),
                                () -> Config.isChinese ? 0 : 1,
                                i -> { Config.isChinese = i == 0; Config.save(); })));

        modules.add(new SettingModule(UiText.t("完整性设置", "Integrity Settings"), UiText.t("在此处控制客户端功能的完整性", "Control client feature integrity here"), null)
                .addSub(UiText.t("完整性切换", "Integrity Switch"), UiText.t("如果你阅读且同意了告知书，则会切换到完整模式，反之则为受限模式", "Switches to full mode if you have read and agreed to the notice, otherwise restricted mode is used"),
                        new SettingCycle(List.of(UiText.t("受限模式", "Restricted Mode"), UiText.t("完整模式", "Full Mode")),
                                () -> Config.fullMode ? 1 : 0,
                                i -> { Config.termsRead = true; Config.fullMode = i == 1; Config.save(); })));
    }

    @Override public String getTitle() { return UiText.t("其他设置", "Misc Settings"); }
    @Override public String getSubtitle() { return UiText.t("其他杂项功能", "Other miscellaneous features"); }
}
