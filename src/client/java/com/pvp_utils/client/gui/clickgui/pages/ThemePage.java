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

        modules.add(new SettingModule(UiText.t("HUD 样式", "HUD Styles"), UiText.t("单独调整各个 HUD 组件的显示样式", "Adjust each HUD component display style separately"), null)
                .addSubWhen(() -> Config.fullMode, UiText.t("目标 HUD", "Target HUD"), UiText.t("选择目标 HUD 样式", "Choose the Target HUD style"),
                        new SettingCycle(List.of("New", "Blur", "Lite"),
                                () -> Config.targetHudMode == Config.TargetHudMode.NEW ? 0 : Config.targetHudMode == Config.TargetHudMode.BLUR ? 1 : 2,
                                i -> { Config.targetHudMode = i == 0 ? Config.TargetHudMode.NEW : i == 1 ? Config.TargetHudMode.BLUR : Config.TargetHudMode.LITE; Config.save(); }))
                .addSub(UiText.t("按键显示", "Keystrokes"), UiText.t("选择按键显示样式", "Choose the Keystrokes style"),
                        new SettingCycle(List.of("New", "Lite"),
                                () -> Config.keystrokesMode == Config.KeystrokesMode.NEW ? 0 : 1,
                                i -> { Config.keystrokesMode = i == 0 ? Config.KeystrokesMode.NEW : Config.KeystrokesMode.LITE; Config.save(); }))
                .addSub(UiText.t("方块数量显示", "Block Count Display"), UiText.t("选择方块数量显示样式", "Choose the Block Count Display style"),
                        new SettingCycle(List.of("New", "Blur"),
                                () -> Config.blockCountDisplayMode == Config.BlockCountDisplayMode.NEW ? 0 : 1,
                                i -> { Config.blockCountDisplayMode = i == 0 ? Config.BlockCountDisplayMode.NEW : Config.BlockCountDisplayMode.BLUR; Config.save(); }))
                .addSub(UiText.t("盔甲 HUD", "Armor HUD"), UiText.t("在 New 和 Lite 之间切换", "Switch between New and Lite"),
                        new SettingCycle(List.of("New", "Lite"),
                                () -> Config.armorHudMode == Config.ArmorHudMode.NEW ? 0 : 1,
                                i -> {
                                    Config.armorHudMode = i == 0 ? Config.ArmorHudMode.NEW : Config.ArmorHudMode.LITE;
                                    if (Config.armorHudMode == Config.ArmorHudMode.LITE && Config.armorHudLayout == Config.ArmorHudLayout.SEPARATED) {
                                        Config.armorHudLayout = Config.ArmorHudLayout.HORIZONTAL;
                                    }
                                    Config.save();
                                }))
                .addSub(UiText.t("物品使用状态", "Item Use Status"), UiText.t("选择物品使用状态显示样式", "Choose the item use status style"),
                        new SettingCycle(List.of("Lite", "New"),
                                () -> Config.itemUseStatusMode == Config.ItemUseStatusMode.NEW ? 1 : 0,
                                i -> {
                                    Config.itemUseStatusMode = switch (i) {
                                        case 1 -> Config.ItemUseStatusMode.NEW;
                                        default -> Config.ItemUseStatusMode.LITE;
                                    };
                                    Config.save();
                                })));
    }

    @Override public String getTitle() { return UiText.t("主题设置", "Theme Settings"); }
    @Override public String getSubtitle() { return UiText.t("全局 HUD 主题与模糊", "Global HUD theme and blur"); }
}
