package com.pvp_utils.client.modules.impl.Render.BetterChat;

public final class BetterChatRenderState {
    private static boolean shiftingAvatarLine;

    private BetterChatRenderState() {}

    public static void setShiftingAvatarLine(boolean shifting) {
        shiftingAvatarLine = shifting;
    }

    public static boolean isShiftingAvatarLine() {
        return shiftingAvatarLine;
    }
}
