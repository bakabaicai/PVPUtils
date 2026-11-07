package com.pvp_utils.client.gui.clickgui.widget;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.render.HudAppearance;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class HudAppearanceSettings {
    private HudAppearanceSettings() {
    }

    public static SettingModule module(List<SettingModule> modules, String chinese, String english) {
        String title = UiText.t(chinese, english);
        for (SettingModule module : modules) if (module.title.equals(title)) return module;
        throw new IllegalArgumentException("Unknown settings module: " + title);
    }

    public static <T extends Enum<T>> void style(SettingModule module, List<T> values,
                                                 Supplier<T> getter, Consumer<T> setter) {
        List<String> labels = new ArrayList<>();
        for (T value : values) {
            labels.add(switch (value.name()) {
                case "LITE" -> "Lite";
                case "BLUR" -> "Blur";
                case "MATERIAL" -> "Material 3";
                default -> value.name();
            });
        }
        module.addSub(UiText.t("样式", "Style"), "", new SettingCycle(List.copyOf(labels),
                () -> {
                    int index = values.indexOf(getter.get());
                    return index < 0 ? Math.min(1, values.size() - 1) : index;
                }, index -> {
                    setter.accept(values.get(Math.clamp(index, 0, values.size() - 1)));
                    Config.save();
                }));
    }

    public static void add(SettingModule module, Supplier<HudAppearance> source,
                           BooleanSupplier visible) {
        add(module, source, visible, () -> true);
    }

    public static void add(SettingModule module, Supplier<HudAppearance> source,
                           BooleanSupplier visible, BooleanSupplier opacityVisible) {
        add(module, source, visible, true, UiText.t("强调色", "Accent Color"), opacityVisible);
    }

    public static void hotbar(SettingModule module, Supplier<HudAppearance> source) {
        add(module, source, () -> true, false, UiText.t("选中框颜色", "Selection Color"), () -> true);
    }

    private static void add(SettingModule module, Supplier<HudAppearance> source,
                            BooleanSupplier visible, boolean text, String accentLabel, BooleanSupplier opacityVisible) {
        module.addSubVisible(UiText.t("阴影", "Shadow"), "",
                        new SettingToggle(() -> source.get().shadow, value -> { source.get().shadow = value; Config.save(); }), visible)
                .addSubChildWhen(() -> visible.getAsBoolean() && source.get().shadow,
                        UiText.t("阴影强度", "Shadow Strength"), "",
                        new SettingSlider(0, 1, "%.2f", () -> (double) source.get().shadowStrength,
                                value -> { source.get().shadowStrength = value.floatValue(); Config.save(); }))
                .addSubVisible(UiText.t("跟随主题", "Use Theme Color"), "",
                        new SettingToggle(() -> source.get().useTheme, value -> { source.get().useTheme = value; Config.save(); }), visible)
                .addSubChildWhen(() -> visible.getAsBoolean() && !source.get().useTheme,
                        accentLabel, "",
                        new SettingColorPreview(() -> source.get().accentColor, value -> source.get().accentColor = value, false, Config::save))
                .addSubVisible(UiText.t("自定义文字颜色", "Custom Text Color"), "",
                        new SettingToggle(() -> source.get().customText, value -> { source.get().customText = value; Config.save(); }), () -> visible.getAsBoolean() && text)
                .addSubChildWhen(() -> visible.getAsBoolean() && text && source.get().customText,
                        UiText.t("文字颜色", "Text Color"), "",
                        new SettingColorPreview(() -> source.get().textColor, value -> source.get().textColor = value, false, Config::save))
                .addSubVisible(UiText.t("自定义背景颜色", "Custom Background"), "",
                        new SettingToggle(() -> source.get().customBackground, value -> { source.get().customBackground = value; Config.save(); }), visible)
                .addSubChildWhen(() -> visible.getAsBoolean() && source.get().customBackground,
                        UiText.t("背景颜色", "Background Color"), "",
                        new SettingColorPreview(() -> source.get().backgroundColor, value -> source.get().backgroundColor = value, false, Config::save))
                .addSubChildWhen(() -> visible.getAsBoolean() && opacityVisible.getAsBoolean() && source.get().customBackground,
                        UiText.t("背景不透明度", "Background Opacity"), "",
                        new SettingSlider(0, 100, "%.0f%%", () -> (double) source.get().backgroundOpacity * 100,
                                value -> { source.get().backgroundOpacity = value.floatValue() / 100f; Config.save(); }));
    }

    public static void colors(SettingModule module, Supplier<HudAppearance> source) {
        module.addSub(UiText.t("跟随主题", "Use Theme Color"), "",
                        new SettingToggle(() -> source.get().useTheme, value -> { source.get().useTheme = value; Config.save(); }))
                .addSubWhen(() -> !source.get().useTheme, UiText.t("强调色", "Accent Color"), "",
                        new SettingColorPreview(() -> source.get().accentColor, value -> source.get().accentColor = value, false, Config::save))
                .addSub(UiText.t("自定义文字颜色", "Custom Text Color"), "",
                        new SettingToggle(() -> source.get().customText, value -> { source.get().customText = value; Config.save(); }))
                .addSubWhen(() -> source.get().customText, UiText.t("文字颜色", "Text Color"), "",
                        new SettingColorPreview(() -> source.get().textColor, value -> source.get().textColor = value, false, Config::save));
    }

}
