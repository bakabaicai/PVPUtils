package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.widget.SettingCycle;
import com.pvp_utils.client.gui.clickgui.widget.SettingModule;
import com.pvp_utils.client.gui.clickgui.widget.SettingSlider;

import java.util.List;

public class ThemePage extends BasePage {
    public ThemePage() {
        modules.add(new SettingModule(UiText.t("全局主题", "Global Theme"), UiText.t("控制所有 HUD 模糊背景和文字颜色", "Control all HUD blur backgrounds and text colors"), null)
                .addSub(UiText.t("主题", "Theme"), UiText.t("选择深色或浅色主题", "Choose dark or light theme"),
                        new SettingCycle(List.of(UiText.t("深色", "Dark"), UiText.t("浅色", "Light")),
                                () -> Config.hudTheme == Config.HudTheme.DARK ? 0 : 1,
                                i -> { Config.hudTheme = i == 1 ? Config.HudTheme.LIGHT : Config.HudTheme.DARK; Config.save(); }))
                .addSub(UiText.t("模糊强度", "Blur Strength"), UiText.t("调整所有 HUD 背景的高斯模糊半径", "Adjust the Gaussian blur radius for all HUD backgrounds"),
                        new SettingSlider(0.0, 200.0, "%.0f%%", () -> (double) Config.skiaBlurStrength * 100.0,
                                v -> { Config.skiaBlurStrength = v.floatValue() / 100.0f; Config.save(); })));
    }

    @Override public String getTitle() { return UiText.t("主题设置", "Theme Settings"); }
    @Override public String getSubtitle() { return UiText.t("全局 HUD 主题与模糊", "Global HUD theme and blur"); }
}
