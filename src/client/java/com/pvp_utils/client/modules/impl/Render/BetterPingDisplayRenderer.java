package com.pvp_utils.client.modules.impl.Render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;

public final class BetterPingDisplayRenderer {
    public static final int EXTRA_SLOT_WIDTH = 35;

    private static final int PING_START = 0;
    private static final int PING_MID = 150;
    private static final int PING_END = 300;
    private static final int COLOR_UNKNOWN = 0xFFAAAAAA;
    private static final int COLOR_START = 0xFF00E676;
    private static final int COLOR_MID = 0xFFD6CD30;
    private static final int COLOR_END = 0xFFE53935;

    private BetterPingDisplayRenderer() {
    }

    public static void render(GuiGraphics graphics, int slotWidth, int x, int y, PlayerInfo playerInfo) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || playerInfo == null) {
            return;
        }

        String text = format(playerInfo.getLatency());
        int textWidth = client.font.width(text);
        int textX = x + slotWidth - textWidth - 2;
        graphics.drawString(client.font, text, textX, y, color(playerInfo.getLatency()), true);
    }

    public static String format(int latency) {
        return latency < 0 ? "?ms" : latency + "ms";
    }

    public static int color(int latency) {
        if (latency < PING_START) {
            return COLOR_UNKNOWN;
        }
        if (latency < PING_MID) {
            return interpolate(COLOR_START, COLOR_MID, offset(PING_START, PING_MID, latency));
        }
        return interpolate(COLOR_MID, COLOR_END, offset(PING_MID, PING_END, Math.min(latency, PING_END)));
    }

    private static float offset(int start, int end, int value) {
        return Math.max(0.0f, Math.min(1.0f, (value - start) / (float) (end - start)));
    }

    private static int interpolate(int startColor, int endColor, float offset) {
        int r = Math.round(channel(startColor, 16) + (channel(endColor, 16) - channel(startColor, 16)) * offset);
        int g = Math.round(channel(startColor, 8) + (channel(endColor, 8) - channel(startColor, 8)) * offset);
        int b = Math.round(channel(startColor, 0) + (channel(endColor, 0) - channel(startColor, 0)) * offset);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int channel(int color, int shift) {
        return (color >> shift) & 0xFF;
    }
}
