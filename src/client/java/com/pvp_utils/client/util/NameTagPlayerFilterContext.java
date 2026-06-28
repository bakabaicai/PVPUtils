package com.pvp_utils.client.util;

public final class NameTagPlayerFilterContext {
    private static final ThreadLocal<Boolean> REAL_PLAYER = ThreadLocal.withInitial(() -> true);

    private NameTagPlayerFilterContext() {}

    public static void setRealPlayer(boolean realPlayer) {
        REAL_PLAYER.set(realPlayer);
    }

    public static boolean isRealPlayer() {
        return REAL_PLAYER.get();
    }

    public static void clear() {
        REAL_PLAYER.remove();
    }
}
