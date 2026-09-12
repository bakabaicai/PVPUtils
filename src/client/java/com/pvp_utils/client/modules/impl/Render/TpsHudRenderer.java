package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class TpsHudRenderer extends SkiaTextHudRenderer {
    private static final TpsHudRenderer INSTANCE = new TpsHudRenderer();
    private static final long SAMPLE_TIMEOUT_MS = 5000L;
    private static final float EMA_FACTOR = 0.25f;

    private long lastSampleMs;
    private long lastGameTime = Long.MIN_VALUE;
    private float smoothedTps = Float.NaN;
    private boolean inServer;

    private TpsHudRenderer() {
    }

    public static TpsHudRenderer getInstance() {
        return INSTANCE;
    }

    public void onTimePacket(long gameTime) {
        long now = System.currentTimeMillis();
        if (lastGameTime != Long.MIN_VALUE && gameTime >= lastGameTime) {
            long deltaMs = now - lastSampleMs;
            if (deltaMs >= 250L) {
                float tps = (gameTime - lastGameTime) / (deltaMs / 1000f);
                tps = Math.max(0f, Math.min(40f, tps));
                smoothedTps = Float.isNaN(smoothedTps) ? tps : smoothedTps + (tps - smoothedTps) * EMA_FACTOR;
            }
        }
        lastGameTime = gameTime;
        lastSampleMs = now;
        inServer = true;
    }

    @Override
    protected boolean enabled() {
        return Config.tpsHud;
    }

    @Override
    protected Config.HudStyle style() {
        return Config.tpsHudStyle;
    }

    @Override
    protected boolean backgroundEnabled() {
        return Config.tpsHudBackground;
    }

    @Override
    protected float configX() {
        return Config.tpsHudX;
    }

    @Override
    protected void setConfigX(float value) {
        Config.tpsHudX = value;
    }

    @Override
    protected float configY() {
        return Config.tpsHudY;
    }

    @Override
    protected void setConfigY(float value) {
        Config.tpsHudY = value;
    }

    @Override
    protected float configScale() {
        return Config.tpsHudScale;
    }

    @Override
    protected void setConfigScale(float value) {
        Config.tpsHudScale = value;
    }

    @Override
    protected List<Line> lines() {
        boolean stale = System.currentTimeMillis() - lastSampleMs > SAMPLE_TIMEOUT_MS;
        if (!inServer || stale || Float.isNaN(smoothedTps)) {
            return List.of(new Line("TPS: --", 0xFFAAAAAA));
        }
        float tps = Math.min(20f, smoothedTps);
        int color = tps >= 18.5f ? 0xFF00E676 : tps >= 15f ? 0xFFD6CD30 : 0xFFE53935;
        return List.of(new Line(String.format(java.util.Locale.ROOT, "TPS: %.1f", tps), color));
    }
}
