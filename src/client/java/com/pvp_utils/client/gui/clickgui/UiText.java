package com.pvp_utils.client.gui.clickgui;

import com.pvp_utils.Config;

import java.util.Map;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public final class UiText {
    private static final Map<String, String> counterparts = new ConcurrentHashMap<>();

    private UiText() {}

    public static String t(String zh, String en) {
        counterparts.put(zh, en);
        counterparts.put(en, zh);
        return Config.isChinese ? zh : en;
    }

    public static boolean matchesSearch(String text, String query) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String normalizedText = text.toLowerCase(Locale.ROOT);
        String compactQuery = query.replaceAll("\\s+", "");
        if (normalizedText.contains(query) || normalizedText.replaceAll("\\s+", "").contains(compactQuery)) {
            return true;
        }
        String counterpart = counterparts.get(text);
        if (counterpart == null) {
            return false;
        }
        String normalizedCounterpart = counterpart.toLowerCase(Locale.ROOT);
        return normalizedCounterpart.contains(query) || normalizedCounterpart.replaceAll("\\s+", "").contains(compactQuery);
    }
}
