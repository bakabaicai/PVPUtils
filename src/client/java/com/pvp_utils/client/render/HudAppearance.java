package com.pvp_utils.client.render;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.theme.ThemeColors;
import com.pvp_utils.client.render.skia.GlassStyle;

public final class HudAppearance {
    public boolean shadow = true;
    public float shadowStrength = 0.35f;
    public boolean useTheme = true;
    public int accentColor = 0xFF4794FD;
    public boolean customText;
    public int textColor = 0xFFF3F6FC;
    public boolean customBackground;
    public int backgroundColor = 0xFF111827;
    public float backgroundOpacity = 0.4f;
    public GlassStyle glass = new GlassStyle();

    public int accent() {
        return useTheme ? ThemeColors.accent() : accentColor;
    }

    public int accent(float x, float y) {
        return useTheme ? ThemeColors.accent(x, y) : accentColor;
    }

    public int text() {
        return customText ? textColor : Config.hudPrimaryTextColor();
    }

    public int secondaryText() {
        int color = text();
        return (Math.round((color >>> 24) * 0.7f) << 24) | (color & 0xFFFFFF);
    }

    public int background() {
        int color = customBackground ? backgroundColor : Config.skiaBlurTintColor();
        return (Math.round(Math.clamp(backgroundOpacity, 0f, 1f) * 255f) << 24) | (color & 0xFFFFFF);
    }

    public void normalize() {
        shadowStrength = Float.isFinite(shadowStrength) ? Math.clamp(shadowStrength, 0f, 1f) : 0.35f;
        backgroundOpacity = Float.isFinite(backgroundOpacity) ? Math.clamp(backgroundOpacity, 0f, 1f) : 0.4f;
        if (glass == null) glass = new GlassStyle();
        glass.sanitize();
    }
}
