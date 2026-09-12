package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;

import java.util.List;

public final class PingHudRenderer extends SkiaTextHudRenderer {
    private static final PingHudRenderer INSTANCE = new PingHudRenderer();

    private PingHudRenderer() {
    }

    public static PingHudRenderer getInstance() {
        return INSTANCE;
    }

    @Override
    protected boolean enabled() {
        return Config.pingHud;
    }

    @Override
    protected Config.HudStyle style() {
        return Config.pingHudStyle;
    }

    @Override
    protected boolean backgroundEnabled() {
        return Config.pingHudBackground;
    }

    @Override
    protected float configX() {
        return Config.pingHudX;
    }

    @Override
    protected void setConfigX(float value) {
        Config.pingHudX = value;
    }

    @Override
    protected float configY() {
        return Config.pingHudY;
    }

    @Override
    protected void setConfigY(float value) {
        Config.pingHudY = value;
    }

    @Override
    protected float configScale() {
        return Config.pingHudScale;
    }

    @Override
    protected void setConfigScale(float value) {
        Config.pingHudScale = value;
    }

    @Override
    protected List<Line> lines() {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client.getConnection() == null || client.player == null) {
            return List.of(new Line("Ping: --", 0xFFAAAAAA));
        }
        net.minecraft.client.multiplayer.PlayerInfo info = client.getConnection().getPlayerInfo(client.player.getUUID());
        int latency = info == null ? -1 : info.getLatency();
        String text = latency < 0 ? "--" : latency + "ms";
        return List.of(new Line("Ping: " + text, BetterPingDisplayRenderer.color(Math.max(0, latency))));
    }
}
