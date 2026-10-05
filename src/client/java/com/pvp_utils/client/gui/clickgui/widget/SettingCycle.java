package com.pvp_utils.client.gui.clickgui.widget;

import io.github.humbleui.types.RRect;

import com.pvp_utils.client.render.skia.SkijaUi;

import com.pvp_utils.client.gui.clickgui.theme.ClickGuiThemeColors;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SettingCycle extends SettingWidget {
    private final List<String> options;
    private final Supplier<Integer> getter;
    private final Consumer<Integer> setter;
    private final Paint bgPaint = new Paint().setAntiAlias(true);
    private int cachedIndex = Integer.MIN_VALUE;
    private String cachedLabel = "";
    private float cachedTextWidth = 0f;

    public SettingCycle(List<String> options, Supplier<Integer> getter, Consumer<Integer> setter) {
        this.options = options;
        this.getter = getter;
        this.setter = setter;
    }

    @Override public float getWidth() { return 100f; }
    @Override public float getHeight() { return 24f; }

    @Override
    public void draw(Canvas canvas, float x, float y, float alpha) {
        int index = getter.get() % options.size();
        if (index != cachedIndex) {
            cachedIndex = index;
            cachedLabel = options.get(index);
            cachedTextWidth = SkijaUi.textWidth(cachedLabel, 12f);
        }
        ClickGuiThemeColors tc = ClickGuiThemeColors.current();
        bgPaint.setColor(withAlpha(tc.buttonBackground, ClickGuiThemeColors.panelBackgroundAlpha(alpha)));
        canvas.drawRRect(RRect.makeXYWH(x, y, getWidth(), getHeight(), 6f), bgPaint);
        SkijaUi.text(canvas, cachedLabel, x + (getWidth() - cachedTextWidth) / 2f, (y + 16f) + SkijaUi.textMetrics(12f).getAscent(), SkijaUi.textMetrics(12f).getDescent() - SkijaUi.textMetrics(12f).getAscent(), withAlpha(tc.subModuleText, alpha), 12f);
    }

    @Override
    public boolean onClick(float mx, float my, float x, float y, int button) {
        if (button != 0) return false;
        setter.accept((getter.get() + 1) % options.size());
        return true;
    }
}
