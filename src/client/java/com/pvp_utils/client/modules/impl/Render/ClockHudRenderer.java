package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ClockHudRenderer extends SkiaTextHudRenderer {
    private static final ClockHudRenderer INSTANCE = new ClockHudRenderer();
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private ClockHudRenderer() {
    }

    public static ClockHudRenderer getInstance() {
        return INSTANCE;
    }

    @Override
    protected boolean enabled() {
        return Config.clockHud;
    }

    @Override
    protected Config.HudStyle style() {
        return Config.clockHudStyle;
    }

    @Override
    protected boolean backgroundEnabled() {
        return Config.clockHudBackground;
    }

    @Override
    protected float configX() {
        return Config.clockHudX;
    }

    @Override
    protected void setConfigX(float value) {
        Config.clockHudX = value;
    }

    @Override
    protected float configY() {
        return Config.clockHudY;
    }

    @Override
    protected void setConfigY(float value) {
        Config.clockHudY = value;
    }

    @Override
    protected float configScale() {
        return Config.clockHudScale;
    }

    @Override
    protected void setConfigScale(float value) {
        Config.clockHudScale = value;
    }

    @Override
    protected List<Line> lines() {
        return List.of(new Line(LocalTime.now().format(TIME_FORMAT), 0xFFF2F4F8));
    }
}
