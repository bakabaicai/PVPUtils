package com.pvp_utils.client.modules.impl.Render.motionblur;

public final class FrameTimer {
    private long lastNano = 0;
    private float currentFPS = 0.0f;

    public void beginFrame() {
        long now = System.nanoTime();
        float delta = (now - lastNano) / 1_000_000_000.0f;
        lastNano = now;
        currentFPS = delta > 0 && delta < 1.0f ? 1.0f / delta : 0.0f;
        MonitorInfoProvider.updateDisplayInfo();
    }

    public float getFPS() { return currentFPS; }
    public int getRefreshRate() { return MonitorInfoProvider.getRefreshRate(); }
}
