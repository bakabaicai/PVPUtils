package com.pvp_utils.client.modules.impl.Combat;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;

public class HitMarkerRenderer {
    private static final HitMarkerRenderer INSTANCE = new HitMarkerRenderer();
    private long hitTime = 0;
    private static final long DURATION = 250;
    private int currentColor = 0xFFFFFF;

    public static HitMarkerRenderer getInstance() {
        return INSTANCE;
    }

    public void onHit(boolean isRanged, int color) {
        HitSoundPlayer.play(isRanged);
        if (Config.hitMarker) {
            this.hitTime = System.currentTimeMillis();
            this.currentColor = color;
        }
    }

    public void render(GuiGraphics graphics) {
        if (!Config.hitMarker || hitTime == 0) return;

        long elapsed = System.currentTimeMillis() - hitTime;
        if (elapsed > DURATION) {
            hitTime = 0;
            return;
        }

        float alphaProgress = 1.0f - (float) elapsed / DURATION;
        int alpha = (int) (alphaProgress * 255);
        int color = (alpha << 24) | (currentColor & 0xFFFFFF);

        Minecraft client = Minecraft.getInstance();
        int centerX = client.getWindow().getGuiScaledWidth() / 2;
        int centerY = client.getWindow().getGuiScaledHeight() / 2;

        int length = 5;
        int gap = 3;

        drawDiagonal(graphics, centerX - gap, centerY - gap, -1, -1, length, color);
        drawDiagonal(graphics, centerX + gap, centerY - gap, 1, -1, length, color);
        drawDiagonal(graphics, centerX - gap, centerY + gap, -1, 1, length, color);
        drawDiagonal(graphics, centerX + gap, centerY + gap, 1, 1, length, color);
    }

    private void drawDiagonal(GuiGraphics graphics, int x, int y, int dx, int dy, int len, int color) {
        for (int i = 0; i < len; i++) {
            graphics.fill(x + i * dx, y + i * dy, x + i * dx + 1, y + i * dy + 1, color);
        }
    }
}
