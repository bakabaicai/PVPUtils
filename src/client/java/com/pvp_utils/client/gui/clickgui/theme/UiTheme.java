package com.pvp_utils.client.gui.clickgui.theme;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.UiText;

import java.util.Locale;

public enum UiTheme {
    DARK("深色", "Dark", 0xFF131314, 0xFF222327, 0xFFFFFFFF, 0xFFEBEBEB),
    LIGHT("浅色", "Light", 0xFFFEFEFE, 0xFFEEEEEE, 0xFF363636, 0xFF6B7581),
    DARK_BLUE("深蓝", "Dark Blue", 0xFF161C29, 0xFF1B2434, 0xFF9DAFD3, 0xFF7483A4),
    MIDNIGHT("午夜", "Midnight", 0xFF2F363D, 0xFF24292E, 0xFFFFFFFF, 0xFFEBEBEB),
    DARK_PURPLE("深紫", "Dark Purple", 0xFF2C0E48, 0xFF35185A, 0xFFEAE2FC, 0xFFC2BAD4),
    SEA("海洋", "Sea", 0xFFCBE0FF, 0xFFBED8EE, 0xFFFFFFFF, 0xFFF5F5F5),
    SAKURA("樱花", "Sakura", 0xFFFFBFB2, 0xFFFFDFE2, 0xFFFFFFFF, 0xFFF5F5F5);

    private final String chinese;
    private final String english;
    private final int panel;
    private final int background;
    private final int primaryText;
    private final int secondaryText;
    private final boolean dark;

    UiTheme(String chinese, String english, int panel, int background, int primaryText, int secondaryText) {
        this.chinese = chinese;
        this.english = english;
        this.panel = panel;
        this.background = background;
        this.dark = luminance(background) < 0.35;
        this.primaryText = readableOnSurfaces(primaryText, background, panel);
        this.secondaryText = readableOnSurfaces(secondaryText, background, panel);
    }

    public static UiTheme current() {
        return Config.uiTheme == null ? LIGHT : Config.uiTheme;
    }

    public void select() {
        if (Config.uiTheme == this) return;
        Config.uiTheme = this;
        Config.save();
    }

    public String label() {
        return UiText.t(chinese, english);
    }

    public int background() {
        return background;
    }

    public int panel() {
        return panel;
    }

    public int primaryText() {
        return primaryText;
    }

    public int secondaryText() {
        return secondaryText;
    }

    public boolean isDark() {
        return dark;
    }

    public static UiTheme fromLegacy(String value) {
        if (value == null) return null;
        return switch (value.strip().toLowerCase(Locale.ROOT).replace(' ', '_')) {
            case "white", "default", "light" -> LIGHT;
            case "dark", "vape" -> DARK;
            case "gray", "midnight" -> MIDNIGHT;
            case "dark_blue" -> DARK_BLUE;
            case "dark_purple" -> DARK_PURPLE;
            case "lightblue", "light_blue", "sea" -> SEA;
            case "sakura" -> SAKURA;
            default -> null;
        };
    }

    public static int readableText(int preferred, int background) {
        return readableOnSurfaces(preferred, background, background);
    }

    private static int readableOnSurfaces(int preferred, int first, int second) {
        int opaque = preferred | 0xFF000000;
        if (contrast(opaque, first) >= 4.5 && contrast(opaque, second) >= 4.5) return opaque;
        double lightContrast = Math.min(contrast(0xFFFFFFFF, first), contrast(0xFFFFFFFF, second));
        double darkContrast = Math.min(contrast(0xFF000000, first), contrast(0xFF000000, second));
        int target = lightContrast >= darkContrast ? 0xFFFFFFFF : 0xFF000000;
        float lower = 0f;
        float upper = 1f;
        for (int i = 0; i < 10; i++) {
            float amount = (lower + upper) * 0.5f;
            int candidate = mix(opaque, target, amount);
            if (contrast(candidate, first) >= 4.5 && contrast(candidate, second) >= 4.5) upper = amount;
            else lower = amount;
        }
        return mix(opaque, target, upper);
    }

    private static int mix(int first, int second, float amount) {
        int r = Math.round((first >> 16 & 255) + ((second >> 16 & 255) - (first >> 16 & 255)) * amount);
        int g = Math.round((first >> 8 & 255) + ((second >> 8 & 255) - (first >> 8 & 255)) * amount);
        int b = Math.round((first & 255) + ((second & 255) - (first & 255)) * amount);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static double contrast(int first, int second) {
        double a = luminance(first);
        double b = luminance(second);
        return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
    }

    private static double luminance(int color) {
        return linear(color >> 16 & 255) * 0.2126 + linear(color >> 8 & 255) * 0.7152
                + linear(color & 255) * 0.0722;
    }

    private static double linear(int channel) {
        double value = channel / 255.0;
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }
}
