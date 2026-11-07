package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.ModuleKeybindManager;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.widget.*;
import com.pvp_utils.client.modules.impl.Misc.VictorySound;
import com.pvp_utils.client.modules.impl.Tool.PseudoCreativeManager;

import java.util.List;

public class MiscPage extends BasePage {

    public MiscPage() {
        SettingModule clickGui = new SettingModule(UiText.t("ClickGUI 设置", "ClickGUI Settings"), "", null)
                .keybindAction(ModuleKeybindManager.ACTION_CLICK_GUI)
                .addSub(UiText.t("面板模糊", "Panel Blur"), "",
                        new SettingToggle(() -> Config.clickGuiPanelBlur, value -> { Config.clickGuiPanelBlur = value; Config.save(); }))
                .addSub(UiText.t("滚动灵敏度", "Scroll Sensitivity"), "",
                        new SettingSlider(0.2, 5.0, "%.1fx", () -> (double) Config.clickGuiScrollSpeed,
                                value -> { Config.clickGuiScrollSpeed = value.floatValue(); Config.save(); }));
        HudAppearanceSettings.add(clickGui, () -> Config.clickGuiAppearance, () -> true);
        modules.add(clickGui);

        modules.add(new SettingModule(UiText.t("主界面", "Main Menu"), "", null)
                .addSub(UiText.t("限制帧率", "Frame Limit"), "",
                        new SettingToggle(() -> Config.mainUIFrameLimitEnabled, value -> { Config.mainUIFrameLimitEnabled = value; Config.save(); }))
                .addSubWhen(() -> Config.mainUIFrameLimitEnabled, UiText.t("最大帧率", "Maximum FPS"), "",
                        new SettingSlider(30, 240, "%.0f", () -> (double) Config.mainUIFrameLimit,
                                value -> { Config.mainUIFrameLimit = value.intValue(); Config.save(); })));

        modules.add(new SettingModule(UiText.t("胜利音效", "Victory Sound"), UiText.t("获胜时播放自定义音效", "Play a custom sound after winning"),
                new SettingToggle(() -> Config.victorySound, v -> { Config.victorySound = v; Config.save(); }))
                .addSub(UiText.t("打开自定义音效文件夹", "Open custom sound folder"), "", new SettingButton(UiText.t("打开", "Open"), VictorySound::openSoundsFolder)));

        modules.add(new SettingModule(UiText.t("语言切换", "Language Switch"), UiText.t("切换界面显示语言", "Switch the interface display language"), null)
                .addSub(UiText.t("语言", "Language"), UiText.t("选择语言，关闭并重新打开界面后生效", "Choose language, close and reopen the UI to apply changes"),
                        new SettingCycle(List.of(UiText.t("中文", "Chinese"), UiText.t("英文", "English")),
                                () -> Config.isChinese ? 0 : 1,
                                i -> { Config.isChinese = i == 0; Config.save(); })));

        modules.add(new SettingModule(UiText.t("伪装创造", "Pseudo Creative"), UiText.t("在本地显示创造模式状态", "Display Creative mode locally"),
                new SettingToggle(PseudoCreativeManager::isActive, v -> { PseudoCreativeManager.setEnabled(v); Config.save(); })));

        if (Config.restrictedFeaturesUnlocked()) {
            modules.add(new SettingModule(UiText.t("完整性设置", "Integrity Settings"), UiText.t("选择客户端功能范围", "Select the available client features"), null)
                    .addSub(UiText.t("完整性切换", "Integrity Switch"), UiText.t("同意使用说明后可切换完整模式", "Full mode is available after accepting the usage notice"),
                            new SettingCycle(List.of(UiText.t("受限模式", "Restricted Mode"), UiText.t("完整模式", "Full Mode")),
                                    () -> Config.fullMode ? 1 : 0,
                                    i -> { Config.termsRead = true; Config.fullMode = i == 1; Config.save(); })));
        }

        SettingModule fakeFpsModule = new SettingModule(UiText.t("假帧率", "Fake FPS"), UiText.t("伪装运行时帧数", "Spoof the runtime frame rate"),
                new SettingToggle(() -> Config.fakeFps, v -> { Config.fakeFps = v; Config.save(); }))
                .addSub(UiText.t("最小帧数", "Minimum FPS"), UiText.t("关闭随机跳变时固定使用此值", "Used as a fixed value when random jumps are disabled"),
                        new SettingSlider(1, 99999, "%.0f", () -> (double) Config.fakeFpsMin,
                                v -> { Config.fakeFpsMin = Math.min(Config.fakeFpsMax, Config.clampFakeFps((int) Math.round(v))); Config.save(); }))
                .addSub(UiText.t("最大帧数", "Maximum FPS"), "",
                        new SettingSlider(1, 99999, "%.0f", () -> (double) Config.fakeFpsMax,
                                v -> { Config.fakeFpsMax = Math.max(Config.fakeFpsMin, Config.clampFakeFps((int) Math.round(v))); Config.save(); }))
                .addSub(UiText.t("随机帧数跳变", "Random FPS Jumps"), UiText.t("按随机间隔在帧数区间内切换", "Change FPS within the range at random intervals"),
                        new SettingToggle(() -> Config.fakeFpsRandom, v -> { Config.fakeFpsRandom = v; Config.save(); }))
                .addSub(UiText.t("最小跳变时长", "Minimum Jump Interval"), "",
                        new SettingSlider(0.1, 3.0, "%.1fs", () -> (double) Config.fakeFpsJumpMinSeconds,
                                v -> { Config.fakeFpsJumpMinSeconds = Math.min(Config.fakeFpsJumpMaxSeconds, Config.clampFakeFpsJumpSeconds(v.floatValue())); Config.save(); }),
                        () -> Config.fakeFpsRandom)
                .addSub(UiText.t("最大跳变时长", "Maximum Jump Interval"), "",
                        new SettingSlider(0.1, 3.0, "%.1fs", () -> (double) Config.fakeFpsJumpMaxSeconds,
                                v -> { Config.fakeFpsJumpMaxSeconds = Math.max(Config.fakeFpsJumpMinSeconds, Config.clampFakeFpsJumpSeconds(v.floatValue())); Config.save(); }),
                        () -> Config.fakeFpsRandom);
        fakeFpsModule.setBindingId(getClass().getName() + ".fake_fps");
        modules.add(fakeFpsModule);
    }

    @Override public String getTitle() { return UiText.t("其他设置", "Misc Settings"); }
    @Override public String getSubtitle() { return UiText.t("其他杂项功能", "Other miscellaneous features"); }
}
