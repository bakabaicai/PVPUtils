package com.pvp_utils.client.gui.clickgui;

import com.pvp_utils.Config;

public final class UiText {
    private UiText() {}

    public static String t(String zh, String en) {
        return Config.isChinese ? zh : en;
    }
}
