package com.pvp_utils.client.modules.impl.Render.DynamicIsland;

public final class DynamicIslandNotifications {
    private static final long DISPLAY_MS = 4000L;
    private static final int GREEN = 0xFF22C55E;
    private static final int RED = 0xFFFF5555;

    private static DynamicIslandNotificationCard current = DynamicIslandNotificationCard.EMPTY;
    private static long expireAt = 0L;

    private DynamicIslandNotifications() {
    }

    public static void success(String message) {
        show("Success", "\uE5CA", message, GREEN);
    }

    public static void failure(String message) {
        show("Failure", "\uE5CD", message, RED);
    }

    public static void error(String message) {
        show("Error", "\uE002", message, RED);
    }

    public static DynamicIslandNotificationCard snapshot() {
        if (current == null || !current.visible() || System.currentTimeMillis() > expireAt) {
            current = DynamicIslandNotificationCard.EMPTY;
            return DynamicIslandNotificationCard.EMPTY;
        }
        return current;
    }

    private static void show(String title, String icon, String message, int accentColor) {
        long now = System.currentTimeMillis();
        current = new DynamicIslandNotificationCard(true, icon, title, message, accentColor, now);
        expireAt = now + DISPLAY_MS;
    }
}
