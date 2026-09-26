package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.NewSettingsScreen;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.theme.ClickGuiThemeManager;
import com.pvp_utils.client.gui.clickgui.widget.SettingCycle;
import com.pvp_utils.client.gui.clickgui.widget.SettingLink;
import com.pvp_utils.client.gui.clickgui.widget.SettingModule;
import com.pvp_utils.client.gui.clickgui.widget.SettingSlider;
import com.pvp_utils.client.gui.clickgui.widget.SettingToggle;
import net.minecraft.client.Minecraft;

import java.util.List;

public class ThemePage extends BasePage {
    public ThemePage() {
        modules.add(new SettingModule(UiText.t("界面主题", "GUI Theme"), UiText.t("面板与 HUD 的配色和模糊设置", "Color and blur settings for the panel and HUDs"), null)
                .addSub(UiText.t("面板主题", "Panel Theme"), UiText.t("点击浏览并切换所有面板主题", "Click to browse and switch all panel themes"),
                        new SettingLink(() -> ClickGuiThemeManager.current().displayName(),
                                () -> {
                                    Minecraft mc = Minecraft.getInstance();
                                    if (mc != null && mc.screen instanceof NewSettingsScreen screen) {
                                        screen.openThemePreview();
                                    }
                                }))
                .addSub(UiText.t("自定义面板色 R", "Custom Panel R"), UiText.t("选择 Custom 主题时生效的面板红色分量", "Panel red channel when Custom theme is active"),
                        new SettingSlider(0, 255, "R:%.0f", () -> (double) ((Config.clickGuiCustomWindow >> 16) & 0xFF),
                                v -> { Config.clickGuiCustomWindow = (Config.clickGuiCustomWindow & 0xFF00FFFF) | (((int) v.doubleValue() & 0xFF) << 16); Config.save(); }))
                .addSub(UiText.t("自定义面板色 G", "Custom Panel G"), UiText.t("选择 Custom 主题时生效的面板绿色分量", "Panel green channel when Custom theme is active"),
                        new SettingSlider(0, 255, "G:%.0f", () -> (double) ((Config.clickGuiCustomWindow >> 8) & 0xFF),
                                v -> { Config.clickGuiCustomWindow = (Config.clickGuiCustomWindow & 0xFFFF00FF) | (((int) v.doubleValue() & 0xFF) << 8); Config.save(); }))
                .addSub(UiText.t("自定义面板色 B", "Custom Panel B"), UiText.t("选择 Custom 主题时生效的面板蓝色分量", "Panel blue channel when Custom theme is active"),
                        new SettingSlider(0, 255, "B:%.0f", () -> (double) (Config.clickGuiCustomWindow & 0xFF),
                                v -> { Config.clickGuiCustomWindow = (Config.clickGuiCustomWindow & 0xFFFFFF00) | ((int) v.doubleValue() & 0xFF); Config.save(); }))
                .addSub(UiText.t("自定义强调色 R", "Custom Accent R"), UiText.t("选择 Custom 主题时生效的强调红色分量", "Accent red channel when Custom theme is active"),
                        new SettingSlider(0, 255, "R:%.0f", () -> (double) ((Config.clickGuiCustomAccent >> 16) & 0xFF),
                                v -> { Config.clickGuiCustomAccent = (Config.clickGuiCustomAccent & 0xFF00FFFF) | (((int) v.doubleValue() & 0xFF) << 16); Config.save(); }))
                .addSub(UiText.t("自定义强调色 G", "Custom Accent G"), UiText.t("选择 Custom 主题时生效的强调绿色分量", "Accent green channel when Custom theme is active"),
                        new SettingSlider(0, 255, "G:%.0f", () -> (double) ((Config.clickGuiCustomAccent >> 8) & 0xFF),
                                v -> { Config.clickGuiCustomAccent = (Config.clickGuiCustomAccent & 0xFFFF00FF) | (((int) v.doubleValue() & 0xFF) << 8); Config.save(); }))
                .addSub(UiText.t("自定义强调色 B", "Custom Accent B"), UiText.t("选择 Custom 主题时生效的强调蓝色分量", "Accent blue channel when Custom theme is active"),
                        new SettingSlider(0, 255, "B:%.0f", () -> (double) (Config.clickGuiCustomAccent & 0xFF),
                                v -> { Config.clickGuiCustomAccent = (Config.clickGuiCustomAccent & 0xFFFFFF00) | ((int) v.doubleValue() & 0xFF); Config.save(); }))
                .addSub(UiText.t("HUD 主题", "HUD Theme"), UiText.t("物品栏、灵动岛等 HUD 的颜色主题", "Color theme for inventory bar, Dynamic Island and other HUDs"),
                        new SettingCycle(List.of(UiText.t("深色", "Dark"), UiText.t("浅色", "Light")),
                                () -> Config.hudTheme == Config.HudTheme.DARK ? 0 : 1,
                                i -> { Config.hudTheme = i == 1 ? Config.HudTheme.LIGHT : Config.HudTheme.DARK; Config.save(); }))
                .addSub(UiText.t("面板模糊", "Panel Blur"), UiText.t("模糊 ClickGUI 面板后的游戏画面", "Blur the game behind the ClickGUI panel"),
                        new SettingToggle(() -> Config.clickGuiPanelBlur,
                                v -> { Config.clickGuiPanelBlur = v; Config.save(); }))
                .addSub(UiText.t("模糊强度", "Blur Strength"), UiText.t("调整所有 HUD 背景的高斯模糊半径", "Adjust the Gaussian blur radius for all HUD backgrounds"),
                        new SettingSlider(0.0, 200.0, "%.0f%%", () -> (double) Config.skiaBlurStrength * 100.0,
                                v -> { Config.skiaBlurStrength = v.floatValue() / 100.0f; Config.save(); })));

        modules.add(new SettingModule(UiText.t("液态玻璃", "Liquid Glass"), UiText.t("灵动岛、音乐信息、按键等 HUD 的液态玻璃效果", "Liquid glass effect for Dynamic Island, Music Info, Keystrokes and other HUDs"), null)
                .addSub(UiText.t("模糊半径", "Blur Radius"), UiText.t("玻璃背景的高斯模糊半径", "Gaussian blur radius of the glass background"),
                        new SettingSlider(0.0, 16.0, "%.1f", () -> (double) Config.liquidGlassBlur, v -> { Config.liquidGlassBlur = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("折射高度", "Refraction Height"), UiText.t("边缘折射带的高度", "Height of the edge refraction band"),
                        new SettingSlider(0.0, 1.0, "%.2f", () -> (double) Config.liquidGlassRefractionHeight, v -> { Config.liquidGlassRefractionHeight = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("折射强度", "Refraction Amount"), UiText.t("边缘折射的位移强度", "Displacement strength of the edge refraction"),
                        new SettingSlider(0.0, 2.0, "%.2f", () -> (double) Config.liquidGlassRefractionAmount, v -> { Config.liquidGlassRefractionAmount = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("折射率", "Refractive Index"), UiText.t("玻璃的折射率，越高边缘折射越强", "Refractive index of the glass; higher means stronger edge refraction"),
                        new SettingSlider(1.0, 3.0, "%.2f", () -> (double) Config.liquidGlassIoR, v -> { Config.liquidGlassIoR = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("立体深度", "Depth Effect"), UiText.t("折射向面板中心的径向分量，越大越有凸透镜的立体感", "Radial component of the refraction toward the panel center; higher means a stronger convex-lens depth"),
                        new SettingSlider(0.0, 1.0, "%.2f", () -> (double) Config.liquidGlassDepthEffect, v -> { Config.liquidGlassDepthEffect = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("色散强度", "Dispersion"), UiText.t("边缘彩色色散的强度", "Strength of the chromatic dispersion"),
                        new SettingSlider(0.0, 4.0, "%.2f", () -> (double) Config.liquidGlassDispersion, v -> { Config.liquidGlassDispersion = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("渲染精度", "Render Precision"), UiText.t("液态玻璃图层的渲染分辨率倍率", "Render resolution multiplier of the liquid glass layer"),
                        new SettingSlider(0.5, 2.0, "%.2f", () -> (double) Config.liquidGlassRenderPrecision, v -> { Config.liquidGlassRenderPrecision = v.floatValue(); Config.save(); }))
                .addSub(UiText.t("高光效果", "Highlight"), UiText.t("玻璃边缘的动态高光", "Dynamic highlight on the glass edge"),
                        new SettingToggle(() -> Config.liquidGlassHighlight, v -> { Config.liquidGlassHighlight = v; Config.save(); }))
                .addSub(UiText.t("高光跟随视角", "Highlight Follow View"), UiText.t("高光角度随玩家视角转动", "Highlight angle follows the player view"),
                        new SettingToggle(() -> Config.liquidGlassHighlightFollowView, v -> { Config.liquidGlassHighlightFollowView = v; Config.save(); }))
                .addSub(UiText.t("投影阴影", "Shadow"), UiText.t("玻璃面板的投影", "Drop shadow of the glass panel"),
                        new SettingToggle(() -> Config.liquidGlassShadow, v -> { Config.liquidGlassShadow = v; Config.save(); })));

        modules.add(new SettingModule(UiText.t("GUI设置", "GUI Settings"), UiText.t("调整 ClickGUI 界面的显示与滚动行为", "Adjust ClickGUI display and scrolling behavior"), null)
                .addSub(UiText.t("界面大小", "GUI Size"), UiText.t("调整 ClickGUI 面板的整体缩放", "Adjust the overall scale of the ClickGUI panel"),
                        new SettingCycle(List.of("50%", "75%", "100%", "125%", "150%", "200%"),
                                () -> Config.clickGuiScale,
                                i -> { Config.clickGuiScale = i; Config.save(); }))
                .addSub(UiText.t("滚动灵敏度", "Scroll Sensitivity"), UiText.t("调整 ClickGUI 滚轮滚动的速度", "Adjust the ClickGUI scroll wheel speed"),
                        new SettingSlider(0.2, 5.0, "%.1fx", () -> (double) Config.clickGuiScrollSpeed,
                                v -> { Config.clickGuiScrollSpeed = v.floatValue(); Config.save(); })));

        modules.add(new SettingModule(UiText.t("HUD 样式", "HUD Styles"), UiText.t("单独调整各个 HUD 组件的显示样式", "Adjust each HUD component display style separately"), null)
                .addSub(UiText.t("目标 HUD", "Target HUD"), UiText.t("选择目标 HUD 样式", "Choose the Target HUD style"),
                        new SettingCycle(List.of("New", "Blur", "Lite"),
                                () -> Config.targetHudMode == Config.TargetHudMode.NEW ? 0 : Config.targetHudMode == Config.TargetHudMode.BLUR ? 1 : 2,
                                i -> { Config.targetHudMode = i == 0 ? Config.TargetHudMode.NEW : i == 1 ? Config.TargetHudMode.BLUR : Config.TargetHudMode.LITE; Config.save(); }))
                .addSub(UiText.t("按键显示", "Keystrokes"), UiText.t("选择按键显示样式", "Choose the Keystrokes style"),
                        new SettingCycle(List.of("New", "Blur", "Liquid", "Lite"),
                                () -> switch (Config.keystrokesMode) {
                                    case NEW -> 0;
                                    case BLUR -> 1;
                                    case LIQUID_GLASS -> 2;
                                    case LITE -> 3;
                                },
                                i -> {
                                    Config.keystrokesMode = switch (i) {
                                        case 1 -> Config.KeystrokesMode.BLUR;
                                        case 2 -> Config.KeystrokesMode.LIQUID_GLASS;
                                        case 3 -> Config.KeystrokesMode.LITE;
                                        default -> Config.KeystrokesMode.NEW;
                                    };
                                    Config.save();
                                }))
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
                                }))
                .addSub(UiText.t("音乐信息显示", "Music Info HUD"), UiText.t("选择音乐信息 HUD 样式", "Choose the Music Info HUD style"),
                        new SettingCycle(List.of("Lite", "New", "Blur", "Liquid"),
                                () -> switch (Config.musicInfoHudMode) {
                                    case LITE -> 0;
                                    case NEW -> 1;
                                    case BLUR -> 2;
                                    case LIQUID_GLASS -> 3;
                                },
                                i -> {
                                    Config.musicInfoHudMode = switch (i) {
                                        case 1 -> Config.MusicInfoHudMode.NEW;
                                        case 2 -> Config.MusicInfoHudMode.BLUR;
                                        case 3 -> Config.MusicInfoHudMode.LIQUID_GLASS;
                                        default -> Config.MusicInfoHudMode.LITE;
                                    };
                                    Config.save();
                                }))
                .addSub(UiText.t("灵动岛背景", "Dynamic Island Background"), UiText.t("选择灵动岛的背景样式", "Choose the Dynamic Island background style"),
                        new SettingCycle(List.of("Blur", "Liquid"),
                                () -> Config.dynamicIslandBackground == Config.DynamicIslandBackground.LIQUID_GLASS ? 1 : 0,
                                i -> {
                                    Config.dynamicIslandBackground = i == 1 ? Config.DynamicIslandBackground.LIQUID_GLASS : Config.DynamicIslandBackground.BLUR;
                                    Config.save();
                                })));
    }

    @Override public String getTitle() { return UiText.t("主题设置", "Theme Settings"); }
    @Override public String getSubtitle() { return UiText.t("全局 HUD 主题与模糊", "Global HUD theme and blur"); }
}
