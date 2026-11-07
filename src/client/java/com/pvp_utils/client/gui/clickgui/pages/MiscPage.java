package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.ModuleKeybindManager;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.widget.*;
import com.pvp_utils.client.modules.impl.Misc.VictorySound;
import com.pvp_utils.client.modules.impl.Tool.PseudoCreativeManager;
import com.pvp_utils.client.web.WebGUIServer;

import java.util.List;

public class MiscPage extends BasePage {

    public MiscPage() {
        modules.add(new SettingModule("ClickGUI", UiText.t("打开模组设置界面，默认按键右Shift，可在此处修改按键绑定。", "Open the mod settings screen. The default key is Right Shift and can be changed here."), null)
                .keybindAction(ModuleKeybindManager.ACTION_CLICK_GUI));

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

        modules.add(new SettingModule(UiText.t("伪装创造", "Pseudo Creative"), UiText.t("在本地将你的模式改为创造", "Locally switch your game mode to Creative"),
                new SettingToggle(PseudoCreativeManager::isActive, v -> { PseudoCreativeManager.setEnabled(v); Config.save(); })));

        if (Config.restrictedFeaturesUnlocked()) {
            modules.add(new SettingModule(UiText.t("完整性设置", "Integrity Settings"), UiText.t("在此处控制客户端功能的完整性", "Control client feature integrity here"), null)
                    .addSub(UiText.t("完整性切换", "Integrity Switch"), UiText.t("如果你阅读且同意了告知书，则会切换到完整模式，反之则为受限模式", "Switches to full mode if you have read and agreed to the notice, otherwise restricted mode is used"),
                            new SettingCycle(List.of(UiText.t("受限模式", "Restricted Mode"), UiText.t("完整模式", "Full Mode")),
                                    () -> Config.fullMode ? 1 : 0,
                                    i -> { Config.termsRead = true; Config.fullMode = i == 1; Config.save(); })));
        }

        SettingModule fakeFpsModule = new SettingModule(UiText.t("假帧率", "Fake FPS"), UiText.t("伪装运行时帧数", "Spoof the runtime frame rate"),
                new SettingToggle(() -> Config.fakeFps, v -> { Config.fakeFps = v; Config.save(); }))
                .addSub(UiText.t("最小帧数", "Minimum FPS"), UiText.t("关闭随机跳变时固定使用此值", "Used as a fixed value when random jumps are disabled"),
                        new SettingSlider(1, 99999, "%.0f", () -> (double) Config.fakeFpsMin,
                                v -> { Config.fakeFpsMin = Math.min(Config.fakeFpsMax, Config.clampFakeFps((int) Math.round(v))); Config.save(); }))
                .addSub(UiText.t("最大帧数", "Maximum FPS"), UiText.t("随机跳变的帧数上限", "Upper frame-rate limit for random jumps"),
                        new SettingSlider(1, 99999, "%.0f", () -> (double) Config.fakeFpsMax,
                                v -> { Config.fakeFpsMax = Math.max(Config.fakeFpsMin, Config.clampFakeFps((int) Math.round(v))); Config.save(); }))
                .addSub(UiText.t("随机帧数跳变", "Random FPS Jumps"), UiText.t("按随机间隔在帧数区间内切换", "Change FPS within the range at random intervals"),
                        new SettingToggle(() -> Config.fakeFpsRandom, v -> { Config.fakeFpsRandom = v; Config.save(); }))
                .addSub(UiText.t("最小跳变时长", "Minimum Jump Interval"), UiText.t("两次跳变间的最短时间", "Shortest time between changes"),
                        new SettingSlider(0.1, 3.0, "%.1fs", () -> (double) Config.fakeFpsJumpMinSeconds,
                                v -> { Config.fakeFpsJumpMinSeconds = Math.min(Config.fakeFpsJumpMaxSeconds, Config.clampFakeFpsJumpSeconds(v.floatValue())); Config.save(); }),
                        () -> Config.fakeFpsRandom)
                .addSub(UiText.t("最大跳变时长", "Maximum Jump Interval"), UiText.t("两次跳变间的最长时间", "Longest time between changes"),
                        new SettingSlider(0.1, 3.0, "%.1fs", () -> (double) Config.fakeFpsJumpMaxSeconds,
                                v -> { Config.fakeFpsJumpMaxSeconds = Math.max(Config.fakeFpsJumpMinSeconds, Config.clampFakeFpsJumpSeconds(v.floatValue())); Config.save(); }),
                        () -> Config.fakeFpsRandom);
        fakeFpsModule.setBindingId(getClass().getName() + ".fake_fps");
        modules.add(fakeFpsModule);
    }

    @Override public String getTitle() { return UiText.t("其他设置", "Misc Settings"); }
    @Override public String getSubtitle() { return UiText.t("其他杂项功能", "Other miscellaneous features"); }
}
