package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import com.pvp_utils.mixin.client.MultiPlayerGameModeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public class DiggingStatusRenderer {
    private static final DiggingStatusRenderer INSTANCE = new DiggingStatusRenderer();
    private static final float MIN_SPEED_PER_SECOND = 0.0001f;

    private BlockPos lastPos;
    private float lastProgress;
    private long lastSampleTime;
    private float speedPerSecond;

    public static DiggingStatusRenderer getInstance() {
        return INSTANCE;
    }

    public void render(GuiGraphics graphics) {
        if (!Config.diggingStatus) {
            reset();
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.gameMode == null) {
            reset();
            return;
        }

        Screen screen = client.screen;
        if (screen != null) {
            reset();
            return;
        }

        MultiPlayerGameModeAccessor accessor = (MultiPlayerGameModeAccessor) client.gameMode;
        if (!accessor.pvputils$isDestroying() || accessor.pvputils$getDestroyDelay() > 0) {
            reset();
            return;
        }

        BlockPos pos = accessor.pvputils$getDestroyBlockPos();
        float progress = Math.max(0.0f, Math.min(1.0f, accessor.pvputils$getDestroyProgress()));
        if (pos == null || progress <= 0.0f || progress >= 1.0f) {
            reset();
            return;
        }

        updateSpeed(pos, progress);

        if (speedPerSecond <= MIN_SPEED_PER_SECOND) return;

        int percent = Math.max(1, Math.min(99, Math.round(progress * 100.0f)));
        float seconds = Math.max(0.0f, (1.0f - progress) / speedPerSecond);
        String text = percent + "%(" + formatSeconds(seconds) + "s)";

        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        int textX = (screenW - client.font.width(text)) / 2;
        int textY = screenH / 2 + 18;

        graphics.drawString(client.font, Component.literal(text), textX, textY, getProgressColor(progress), true);
    }

    private void updateSpeed(BlockPos pos, float progress) {
        long now = System.nanoTime();
        if (!pos.equals(lastPos) || progress < lastProgress) {
            lastPos = pos;
            lastProgress = progress;
            lastSampleTime = now;
            speedPerSecond = 0.0f;
            return;
        }

        if (progress > lastProgress && lastSampleTime > 0L) {
            float elapsed = (now - lastSampleTime) / 1_000_000_000.0f;
            if (elapsed > 0.0f) {
                float instantSpeed = (progress - lastProgress) / elapsed;
                speedPerSecond = speedPerSecond <= 0.0f ? instantSpeed : speedPerSecond * 0.65f + instantSpeed * 0.35f;
            }
            lastProgress = progress;
            lastSampleTime = now;
        }
    }

    private String formatSeconds(float seconds) {
        if (seconds >= 10.0f) return String.valueOf(Math.round(seconds));
        return String.format(Locale.ROOT, "%.1f", seconds);
    }

    private int getProgressColor(float progress) {
        progress = Math.max(0.0f, Math.min(1.0f, progress));
        int r;
        int g;
        if (progress < 0.5f) {
            float t = progress * 2.0f;
            r = 255;
            g = Math.round(255.0f * t);
        } else {
            float t = (progress - 0.5f) * 2.0f;
            r = Math.round(255.0f * (1.0f - t));
            g = 255;
        }
        return 0xFF000000 | (r << 16) | (g << 8);
    }

    private void reset() {
        lastPos = null;
        lastProgress = 0.0f;
        lastSampleTime = 0L;
        speedPerSecond = 0.0f;
    }
}
