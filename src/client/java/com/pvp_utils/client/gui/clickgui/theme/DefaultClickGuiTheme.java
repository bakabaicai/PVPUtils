package com.pvp_utils.client.gui.clickgui.theme;

public final class DefaultClickGuiTheme implements ClickGuiTheme {
    private static final ClickGuiThemePalette PALETTE = new ClickGuiThemePalette(
            0xFFF7F7F8,
            0xFFF2F2F4,
            0xFFF7F7F8,
            0xFFFFFFFF,
            0xFFF8F8FF,
            0xFF7A9BF8,
            0xFF111111,
            0xFFAAAAAA,
            0xFFE7E7EA
    );
    private static final ClickGuiThemeMetrics METRICS = new ClickGuiThemeMetrics(16f, 10f, 8f, 1f, 0f);

    @Override
    public String id() {
        return "default";
    }

    @Override
    public String displayName() {
        return "Default";
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
