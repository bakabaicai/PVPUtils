package com.pvp_utils;

public class TitleDetector {
    private static final String[] KEYWORDS = {"victory", "胜利", "获胜", "恭喜"};

    public static void check(String title, String subtitle) {
        if (matches(title) || matches(subtitle)) {
            VictoryScreenshot.tryCapture();
        }
    }

    private static boolean matches(String text) {
        if (text == null || text.isEmpty()) return false;
        String cleaned = text.replaceAll("§[0-9a-fk-orA-FK-OR]", "").toLowerCase();
        for (String keyword : KEYWORDS) {
            if (cleaned.contains(keyword)) return true;
        }
        return false;
    }
}