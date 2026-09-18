package com.pvp_utils.client.gui.clickgui.theme;

import com.pvp_utils.Config;

public final class CustomTheme implements ClickGuiTheme {
    private static final ClickGuiThemeMetrics METRICS = new ClickGuiThemeMetrics(16f, 10f, 8f, 1f, 0f);

    @Override
    public String id() {
        return "custom";
    }

    @Override
    public String displayName() {
        return "Custom";
    }

    @Override
    public ClickGuiThemePalette palette() {
        int window = 0xFF000000 | (Config.clickGuiCustomWindow & 0xFFFFFF);
        int accent = 0xFF000000 | (Config.clickGuiCustomAccent & 0xFFFFFF);
        return new ClickGuiThemePalette(
                window,
                shade(window, 0.85f),
                window,
                shade(window, 1.12f),
                shade(window, 1.05f),
                accent,
                0xFFF5F7FF,
                0xFFA8AFBF,
                shade(window, 0.75f)
        );
    }

    @Override
    public ClickGuiThemeMetrics metrics() {
        return METRICS;
    }

    private static int shade(int rgb, float factor) {
        int r = Math.min(255, (int) (((rgb >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int) (((rgb >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int) ((rgb & 0xFF) * factor));
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }
}
