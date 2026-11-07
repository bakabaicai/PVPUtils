package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ClockHudRenderer extends SkiaTextHudRenderer {
    private static final ClockHudRenderer INSTANCE = new ClockHudRenderer();
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter SECOND_FORMAT = DateTimeFormatter.ofPattern("ss");
    private static final DateTimeFormatter DATE_CHINESE = DateTimeFormatter.ofPattern("M月d日 EEEE", java.util.Locale.CHINA);
    private static final DateTimeFormatter DATE_ENGLISH = DateTimeFormatter.ofPattern("EEE, MMM d", java.util.Locale.ENGLISH);
    private long cachedSecond = Long.MIN_VALUE;
    private boolean cachedChinese;
    private String time = "";
    private String hour = "";
    private String seconds = "";
    private String date = "";

    private void updateClock() {
        long second = System.currentTimeMillis() / 1000L;
        if (cachedSecond == second && cachedChinese == Config.isChinese) return;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        time = now.format(TIME_FORMAT);
        hour = now.format(HOUR_FORMAT);
        seconds = now.format(SECOND_FORMAT);
        date = now.format(Config.isChinese ? DATE_CHINESE : DATE_ENGLISH);
        cachedSecond = second;
        cachedChinese = Config.isChinese;
    }

    private ClockHudRenderer() {
    }

    public static ClockHudRenderer getInstance() {
        return INSTANCE;
    }

    @Override
    protected com.pvp_utils.client.render.HudAppearance appearance() {
        return Config.clockAppearance;
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
        updateClock();
        return List.of(new Line(time, Config.clockAppearance.accent()));
    }

    @Override
    protected int minimumWidth() {
        if (style() != Config.HudStyle.MATERIAL) return 0;
        updateClock();
        float timeWidth = com.pvp_utils.client.render.skia.SkijaUi.boldTextWidth(hour, 27f)
                + com.pvp_utils.client.render.skia.SkijaUi.textWidth(seconds, 11f) + 38f;
        float dateWidth = com.pvp_utils.client.render.skia.SkijaUi.textWidth(date, 10f) + 34f;
        return Math.max(148, (int) Math.ceil(Math.max(timeWidth, dateWidth)));
    }

    @Override
    protected int panelHeight(int lineCount) {
        return style() == Config.HudStyle.MATERIAL ? 78 : super.panelHeight(lineCount);
    }

    @Override
    protected void drawContent(io.github.humbleui.skija.Canvas canvas, List<Line> lines,
                               float width, float height) {
        if (style() != Config.HudStyle.MATERIAL) {
            super.drawContent(canvas, lines, width, height);
            return;
        }
        updateClock();
        int accent = Config.clockAppearance.customText ? Config.clockAppearance.text() : Config.clockAppearance.accent();
        com.pvp_utils.client.render.skia.SkijaUi.boldText(canvas, hour, 16f, 9f, 39f, accent, 27f);
        float hourWidth = com.pvp_utils.client.render.skia.SkijaUi.boldTextWidth(hour, 27f);
        com.pvp_utils.client.render.skia.SkijaUi.text(canvas, seconds, 20f + hourWidth, 24f, 18f, accent, 11f);
        com.pvp_utils.client.render.skia.SkijaUi.text(canvas, date, 17f, 51f, 17f,
                Config.clockAppearance.text(), 10f);
    }
}
