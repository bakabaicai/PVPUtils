package com.pvp_utils.client.gui.clickgui.theme;

/**
 * 淡蓝色主题：柔和天蓝底 + 蓝色强调。
 */
public final class LightBlueTheme implements ClickGuiTheme {
    private static final ClickGuiThemePalette PALETTE = new ClickGuiThemePalette(
            0xFFEAF1FF,
            0xFFE0EBFC,
            0xFFEAF1FF,
            0xFFF4F8FF,
            0xFFE6EEFD,
            0xFF3B7CFF,
            0xFF1A2B4D,
            0xFF6B7FA0,
            0xFFC4D6F5
    );
    private static final ClickGuiThemeMetrics METRICS = new ClickGuiThemeMetrics(16f, 10f, 8f, 1f, 0f);

    @Override
    public String id() {
        return "lightblue";
    }

    @Override
    public String displayName() {
        return "Light Blue";
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
