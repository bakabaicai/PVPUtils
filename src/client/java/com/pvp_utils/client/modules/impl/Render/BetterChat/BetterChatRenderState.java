package com.pvp_utils.client.modules.impl.Render.BetterChat;

public final class BetterChatRenderState {
    private static boolean shiftingAvatarLine;
    private static boolean suppressingBackground;
    private static boolean usingClientFontMetrics;

    private BetterChatRenderState() {}

    public static void setShiftingAvatarLine(boolean shifting) {
        shiftingAvatarLine = shifting;
    }

    public static boolean isShiftingAvatarLine() {
        return shiftingAvatarLine;
    }

    public static void setSuppressingBackground(boolean suppressing) {
        suppressingBackground = suppressing;
    }

    public static boolean isSuppressingBackground() {
        return suppressingBackground;
    }

    public static void setUsingClientFontMetrics(boolean enabled) {
        usingClientFontMetrics = enabled;
    }

    public static boolean isUsingClientFontMetrics() {
        return usingClientFontMetrics;
    }
}
