package com.pvp_utils.client.modules.impl.Render.DynamicIsland;

public final class DynamicIslandNotificationCard {
    private final boolean visible;
    private final String icon;
    private final String title;
    private final String message;
    private final int accentColor;
    private final long createdAtMs;

    public static final DynamicIslandNotificationCard EMPTY = new DynamicIslandNotificationCard(false, "", "", "", 0xFFFFFFFF, 0L);

    public DynamicIslandNotificationCard(boolean visible, String icon, String title, String message, int accentColor) {
        this(visible, icon, title, message, accentColor, System.currentTimeMillis());
    }

    public DynamicIslandNotificationCard(boolean visible, String icon, String title, String message, int accentColor, long createdAtMs) {
        this.visible = visible;
        this.icon = icon == null ? "" : icon;
        this.title = title == null ? "" : title;
        this.message = message == null ? "" : message;
        this.accentColor = accentColor;
        this.createdAtMs = createdAtMs;
    }

    public boolean visible() {
        return visible;
    }

    public String icon() {
        return icon;
    }

    public String title() {
        return title;
    }

    public String message() {
        return message;
    }

    public int accentColor() {
        return accentColor;
    }

    public long createdAtMs() {
        return createdAtMs;
    }
}
