package com.pvp_utils.client.gui.clickgui.theme;

import com.pvp_utils.Config;

public final class ClickGuiThemeColors {
    public final int window;
    public final int sidebar;
    public final int content;
    public final int module;
    public final int subModule;
    public final int accent;
    public final int primaryText;
    public final int secondaryText;
    public final int border;
    public final int mutedText;
    public final int inactiveText;
    public final int inactiveIcon;
    public final int subModuleText;
    public final int indicator;
    public final int hoverBackground;
    public final int buttonBackground;
    public final int buttonText;
    public final int scrollbarTrack;
    public final int scrollbarThumb;
    public final int searchBackground;
    public final int searchFocusedBackground;
    public final int searchIcon;
    public final int searchCursor;
    public final int searchText;
    public final int searchTextPlaceholder;
    public final int dangerHoverBackground;
    public final int dangerHoverText;
    public final boolean dark;

    private ClickGuiThemeColors(UiTheme theme, int accent) {
        dark = theme.isDark();
        window = rgb(theme.background());
        sidebar = rgb(theme.panel());
        content = window;
        module = sidebar;
        subModule = mix(sidebar, window, 0.6f);
        this.accent = rgb(accent);
        primaryText = rgb(theme.primaryText());
        secondaryText = rgb(theme.secondaryText());
        border = mix(window, primaryText, 0.14f);
        mutedText = mix(secondaryText, window, 0.18f);
        inactiveText = mix(primaryText, sidebar, 0.25f);
        inactiveIcon = mix(secondaryText, sidebar, 0.2f);
        subModuleText = mix(primaryText, window, 0.12f);
        indicator = mix(window, this.accent, dark ? 0.24f : 0.18f);
        hoverBackground = mix(sidebar, this.accent, dark ? 0.13f : 0.1f);
        buttonBackground = mix(module, window, 0.6f);
        buttonText = primaryText;
        scrollbarTrack = mix(window, primaryText, 0.12f);
        scrollbarThumb = mix(window, primaryText, 0.35f);
        searchBackground = mix(sidebar, window, 0.72f);
        searchFocusedBackground = mix(searchBackground, this.accent, 0.12f);
        searchIcon = secondaryText;
        searchCursor = this.accent;
        searchText = primaryText;
        searchTextPlaceholder = secondaryText;
        dangerHoverBackground = dark ? 0x3D2226 : 0xFFE5E5;
        dangerHoverText = dark ? 0xFF6B6B : 0xCC2222;
    }

    public static ClickGuiThemeColors of(UiTheme theme, int accent) {
        return new ClickGuiThemeColors(theme == null ? UiTheme.LIGHT : theme, accent);
    }

    public static ClickGuiThemeColors current() {
        return of(UiTheme.current(), AccentColors.accent());
    }

    public static float panelBackgroundAlpha(float alpha) {
        return Config.clickGuiPanelBlur ? alpha * Config.clickGuiAppearance.backgroundOpacity : alpha;
    }

    private static int rgb(int color) {
        return color & 0xFFFFFF;
    }

    private static int mix(int first, int second, float amount) {
        return rgb(AccentColors.mix(first | 0xFF000000, second | 0xFF000000, amount));
    }
}
