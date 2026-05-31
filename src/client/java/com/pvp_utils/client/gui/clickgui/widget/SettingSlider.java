package com.pvp_utils.client.gui.clickgui.widget;

import com.pvp_utils.client.render.font.FontRenderer;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SettingSlider extends SettingWidget {

    private final Supplier<Double> getter;
    private final Consumer<Double> setter;
    private final double min, max;
    private final String format;
    private boolean dragging = false;

    private static final int COLOR_TRACK    = 0xFFE0E0E0;
    private static final int COLOR_FILL     = 0xFF2F54EB;
    private static final int COLOR_THUMB    = 0xFFFFFFFF;
    private static final int COLOR_TEXT     = 0xFF888888;

    public SettingSlider(double min, double max, String format, Supplier<Double> getter, Consumer<Double> setter) {
        this.min = min;
        this.max = max;
        this.format = format;
        this.getter = getter;
        this.setter = setter;
    }

    @Override public float getWidth() { return 160f; }
    @Override public float getHeight() { return 20f; }

    @Override
    public void draw(Canvas canvas, float x, float y, float alpha) {
        float t = (float)((getter.get() - min) / (max - min));
        float trackY = y + 9f;
        float thumbX = x + t * 160f;

        try (Paint track = new Paint()) {
            track.setColor(withAlpha(0xE0E0E0, alpha));
            canvas.drawRRect(RRect.makeXYWH(x, trackY, 160f, 4f, 2f), track);
        }
        try (Paint fill = new Paint()) {
            fill.setColor(withAlpha(0x2F54EB, alpha));
            canvas.drawRRect(RRect.makeXYWH(x, trackY, t * 160f, 4f, 2f), fill);
        }
        try (Paint thumb = new Paint()) {
            thumb.setColor(withAlpha(0xFFFFFF, alpha));
            canvas.drawRRect(RRect.makeXYWH(thumbX - 8f, y + 2f, 16f, 16f, 8f), thumb);
        }

        String val = String.format(format, getter.get());
        float tw = FontRenderer.measureTextWidth(val, 11f);
        FontRenderer.drawText(canvas, val, x + 160f + 8f, y + 14f, 11f, withAlpha(0x888888, alpha));
    }

    @Override
    public boolean onClick(float mx, float my, float x, float y, int button) {
        if (button != 0) return false;
        dragging = true;
        applyMouse(mx, x);
        return true;
    }

    @Override
    public boolean onDrag(float mx, float my, float x, float y) {
        if (!dragging) return false;
        applyMouse(mx, x);
        return true;
    }

    public void releaseDrag() { dragging = false; }
    public boolean isDragging() { return dragging; }

    private void applyMouse(float mx, float x) {
        float t = Math.max(0f, Math.min(1f, (mx - x) / 160f));
        setter.accept(min + t * (max - min));
    }
}