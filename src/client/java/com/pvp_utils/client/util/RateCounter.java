package com.pvp_utils.client.util;

import net.minecraft.client.Minecraft;

import java.util.ArrayDeque;
import java.util.Deque;

public class RateCounter {
    private static final long DEFAULT_WINDOW_MS = 1000L;

    private final Deque<Long> events = new ArrayDeque<>();
    private final long windowMs;
    private boolean wasPressed = false;

    public RateCounter() {
        this(DEFAULT_WINDOW_MS);
    }

    public RateCounter(long windowMs) {
        this.windowMs = windowMs;
    }

    public int updatePressed(boolean pressed) {
        long now = System.currentTimeMillis();
        if (pressed && !wasPressed) {
            events.addLast(now);
        }
        wasPressed = pressed;
        return count(now);
    }

    public void record() {
        events.addLast(System.currentTimeMillis());
    }

    public void record(int count) {
        long now = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            events.addLast(now);
        }
    }

    public int count() {
        return count(System.currentTimeMillis());
    }

    public int count(long now) {
        trim(now);
        return events.size();
    }

    public void resetPressed() {
        wasPressed = false;
    }

    public void clear() {
        events.clear();
        wasPressed = false;
    }

    public static float horizontalBlocksPerSecond(Minecraft client) {
        if (client == null || client.player == null) {
            return 0f;
        }
        return (float) client.player.getDeltaMovement().horizontalDistance() * 20.0f;
    }

    private void trim(long now) {
        while (!events.isEmpty() && now - events.peekFirst() > windowMs) {
            events.removeFirst();
        }
    }
}
