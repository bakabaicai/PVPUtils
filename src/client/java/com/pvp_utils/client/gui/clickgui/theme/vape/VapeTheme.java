package com.pvp_utils.client.gui.clickgui.theme.vape;

import com.pvp_utils.client.gui.clickgui.theme.ClickGuiTheme;
import com.pvp_utils.client.gui.clickgui.theme.ClickGuiThemeMetrics;
import com.pvp_utils.client.gui.clickgui.theme.ClickGuiThemePalette;

public final class VapeTheme implements ClickGuiTheme {
    private static final ClickGuiThemePalette PALETTE = new ClickGuiThemePalette(
            0xFF1B1D23,
            0xFF15171C,
            0xFF20232B,
            0xFF292D37,
            0xFF232731,
            0xFF6D8CFF,
            0xFFF5F7FF,
            0xFFA8AFBF,
            0xFF383E4C
    );
    private static final ClickGuiThemeMetrics METRICS = new ClickGuiThemeMetrics(8f, 5f, 4f, 1f, 14f);

    @Override
    public String id() {
        return "vape";
    }

    @Override
    public String displayName() {
        return "Vape";
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
