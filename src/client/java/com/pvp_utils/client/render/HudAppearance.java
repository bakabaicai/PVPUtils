package com.pvp_utils.client.render;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.theme.AccentColors;
import com.pvp_utils.client.gui.clickgui.theme.UiTheme;

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
    private transient int cachedTextBackground;
    private transient UiTheme cachedTextTheme;
    private transient int cachedPrimaryText;
    private transient int cachedSecondaryText;

    public int accent() {
        return useTheme ? AccentColors.accent() : accentColor;
    }

    public int accent(float x, float y) {
        return useTheme ? AccentColors.accent(x, y) : accentColor;
    }

    public int text() {
        if (customText) return textColor;
        if (!customBackground) return UiTheme.current().primaryText();
        resolveTextColors();
        return cachedPrimaryText;
    }

    public int secondaryText() {
        int color;
        if (customText) color = textColor;
        else if (customBackground) {
            resolveTextColors();
            color = cachedSecondaryText;
        } else color = UiTheme.current().secondaryText();
        return (Math.round((color >>> 24) * 0.7f) << 24) | (color & 0xFFFFFF);
    }

    private void resolveTextColors() {
        UiTheme theme = UiTheme.current();
        if (cachedTextTheme == theme && cachedTextBackground == backgroundColor) return;
        cachedTextTheme = theme;
        cachedTextBackground = backgroundColor;
        cachedPrimaryText = UiTheme.readableText(theme.primaryText(), backgroundColor);
        cachedSecondaryText = UiTheme.readableText(theme.secondaryText(), backgroundColor);
    }

    public int background() {
        int color = customBackground ? backgroundColor : Config.skiaBlurTintColor();
        return (Math.round(Math.clamp(backgroundOpacity, 0f, 1f) * 255f) << 24) | (color & 0xFFFFFF);
    }

    public void normalize() {
        shadowStrength = Float.isFinite(shadowStrength) ? Math.clamp(shadowStrength, 0f, 1f) : 0.35f;
        backgroundOpacity = Float.isFinite(backgroundOpacity) ? Math.clamp(backgroundOpacity, 0f, 1f) : 0.4f;
    }
}
