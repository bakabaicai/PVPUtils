package com.pvp_utils.client.gui.clickgui.widget;

import com.pvp_utils.client.render.font.FontRenderer;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SettingCycle extends SettingWidget {

    private final List<String> options;
    private final Supplier<Integer> getter;
    private final Consumer<Integer> setter;

    private static final int COLOR_BG   = 0xFFF0F0F0;
    private static final int COLOR_TEXT = 0xFF333333;

    public SettingCycle(List<String> options, Supplier<Integer> getter, Consumer<Integer> setter) {
        this.options = options;
        this.getter = getter;
        this.setter = setter;
    }

    @Override public float getWidth() { return 100f; }
    @Override public float getHeight() { return 24f; }

    @Override
    public void draw(Canvas canvas, float x, float y, float alpha) {
        try (Paint bg = new Paint()) {
            bg.setColor(withAlpha(0xF0F0F0, alpha));
            canvas.drawRRect(RRect.makeXYWH(x, y, getWidth(), getHeight(), 6f), bg);
        }
        String label = options.get(getter.get() % options.size());
        float tw = FontRenderer.measureTextWidth(label, 12f);
        FontRenderer.drawText(canvas, label, x + (getWidth() - tw) / 2f, y + 16f, 12f, withAlpha(0x333333, alpha));
    }

    @Override
    public boolean onClick(float mx, float my, float x, float y, int button) {
        if (button != 0) return false;
        setter.accept((getter.get() + 1) % options.size());
        return true;
    }
}