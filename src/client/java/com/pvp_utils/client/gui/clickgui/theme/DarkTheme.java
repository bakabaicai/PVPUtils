package com.pvp_utils.client.gui.clickgui.theme;

/**
 * 暗色主题：深灰蓝底 + 蓝色强调。
 */
public final class DarkTheme implements ClickGuiTheme {
    private static final ClickGuiThemePalette PALETTE = new ClickGuiThemePalette(
            0xFF1E2025,
            0xFF17191E,
            0xFF1E2025,
            0xFF262932,
            0xFF20232B,
            0xFF6D8CFF,
            0xFFF5F7FF,
            0xFFA8AFBF,
            0xFF383E4C
    );
    private static final ClickGuiThemeMetrics METRICS = new ClickGuiThemeMetrics(16f, 10f, 8f, 1f, 0f);

    @Override
    public String id() {
        return "dark";
    }

    @Override
    public String displayName() {
        return "Dark";
    }

    @Override
    public ClickGuiThemePalette palette() {
        return PALETTE;
    }

    @Override
    public ClickGuiThemeMetrics metrics() {
        return METRICS;
    }
}
