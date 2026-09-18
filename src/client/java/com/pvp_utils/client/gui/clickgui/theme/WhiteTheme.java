package com.pvp_utils.client.gui.clickgui.theme;

/**
 * 白色主题：恢复为旧版 ClickGUI 的经典配色。
 */
public final class WhiteTheme implements ClickGuiTheme {
    private static final ClickGuiThemePalette PALETTE = new ClickGuiThemePalette(
            0xFFF5F5F7,
            0xFFFFFFFF,
            0xFFF5F5F7,
            0xFFFFFFFF,
            0xFFF8F8FF,
            0xFF2F54EB,
            0xFF111111,
            0xFFAAAAAA,
            0xFFEEEEEE
    );
    private static final ClickGuiThemeMetrics METRICS = new ClickGuiThemeMetrics(16f, 10f, 8f, 1f, 0f);

    @Override
    public String id() {
        return "white";
    }

    @Override
    public String displayName() {
        return "White";
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
