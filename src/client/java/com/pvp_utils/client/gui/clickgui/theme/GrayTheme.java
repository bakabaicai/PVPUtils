package com.pvp_utils.client.gui.clickgui.theme;

/**
 * 灰色主题：中性深灰底 + 银灰强调。
 */
public final class GrayTheme implements ClickGuiTheme {
    private static final ClickGuiThemePalette PALETTE = new ClickGuiThemePalette(
            0xFF2D3036,
            0xFF25282D,
            0xFF2D3036,
            0xFF363A42,
            0xFF2F333A,
            0xFFC0C6D0,
            0xFFE8EAEF,
            0xFF9DA3AD,
            0xFF4A4F57
    );
    private static final ClickGuiThemeMetrics METRICS = new ClickGuiThemeMetrics(16f, 10f, 8f, 1f, 0f);

    @Override
    public String id() {
        return "gray";
    }

    @Override
    public String displayName() {
        return "Gray";
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
