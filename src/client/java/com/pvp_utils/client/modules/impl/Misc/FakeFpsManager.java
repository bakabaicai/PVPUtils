package com.pvp_utils.client.modules.impl.Misc;

import com.pvp_utils.Config;

import java.util.concurrent.ThreadLocalRandom;

public final class FakeFpsManager {
    private static boolean initialized;
    private static boolean random;
    private static int minimum;
    private static int maximum;
    private static int displayedFps;
    private static long minimumDelay;
    private static long maximumDelay;
    private static long nextChange;

    private FakeFpsManager() {
    }

    public static void beginFrame() {
        beginFrame(System.nanoTime());
    }

    static void beginFrame(long now) {
        if (!Config.fakeFps) {
            initialized = false;
            return;
        }
        int min = Config.clampFakeFps(Config.fakeFpsMin);
        int max = Math.max(min, Config.clampFakeFps(Config.fakeFpsMax));
        long minDelay = Math.round(Config.clampFakeFpsJumpSeconds(Config.fakeFpsJumpMinSeconds) * 10f) * 100_000_000L;
        long maxDelay = Math.max(minDelay,
                Math.round(Config.clampFakeFpsJumpSeconds(Config.fakeFpsJumpMaxSeconds) * 10f) * 100_000_000L);
        boolean changed = !initialized || minimum != min || maximum != max || random != Config.fakeFpsRandom
                || minimumDelay != minDelay || maximumDelay != maxDelay;
        minimum = min;
        maximum = max;
        random = Config.fakeFpsRandom;
        minimumDelay = minDelay;
        maximumDelay = maxDelay;
        initialized = true;
        if (!random || minimum == maximum) {
            displayedFps = minimum;
            return;
        }
        if (changed || now - nextChange >= 0L) {
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            if (changed) {
                displayedFps = rng.nextInt(minimum, maximum + 1);
            } else {
                int selected = rng.nextInt(minimum, maximum);
                displayedFps = selected >= displayedFps ? selected + 1 : selected;
            }
            long delay = minimumDelay == maximumDelay ? minimumDelay : rng.nextLong(minimumDelay, maximumDelay + 1L);
            nextChange = now + delay;
        }
    }

    public static int getFps(int actualFps) {
        if (!Config.fakeFps) return actualFps;
        return initialized ? displayedFps : Config.clampFakeFps(Config.fakeFpsMin);
    }
}
