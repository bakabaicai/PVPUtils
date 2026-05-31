package com.pvp_utils.client.gui.clickgui.widget;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SettingToggle extends SettingWidget {

    private final Supplier<Boolean> getter;
    private final Consumer<Boolean> setter;
    private float thumbX = -1f;
    private float colorT = -1f;

    private static final int COLOR_TRACK_ON  = 0xFF2F54EB;
    private static final int COLOR_TRACK_OFF = 0xFFCCCCCC;
    private static final int COLOR_THUMB     = 0xFFFFFFFF;

    public SettingToggle(Supplier<Boolean> getter, Consumer<Boolean> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    @Override public float getWidth() { return 44f; }
    @Override public float getHeight() { return 24f; }

    @Override
    public void draw(Canvas canvas, float x, float y, float alpha) {
        boolean on = getter.get();

        if (colorT < 0f) colorT = on ? 1f : 0f;
        if (thumbX < 0f) thumbX = on ? x + 22f : x + 2f;

        float targetColorT = on ? 1f : 0f;
        colorT += (targetColorT - colorT) * 0.2f;

        float targetThumbX = on ? x + 22f : x + 2f;
        thumbX += (targetThumbX - thumbX) * 0.2f;

        int trackColor = lerpColor(COLOR_TRACK_OFF, COLOR_TRACK_ON, colorT);

        try (Paint track = new Paint()) {
            track.setColor(withAlpha(trackColor, alpha));
            canvas.drawRRect(RRect.makeXYWH(x, y, 44f, 24f, 12f), track);
        }
        try (Paint thumb = new Paint()) {
            thumb.setColor(withAlpha(COLOR_THUMB, alpha));
            canvas.drawRRect(RRect.makeXYWH(thumbX, y + 2f, 20f, 20f, 10f), thumb);
        }
    }

    @Override
    public boolean onClick(float mx, float my, float x, float y, int button) {
        if (button != 0) return false;
        setter.accept(!getter.get());
        return true;
    }

    private static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int)(ar+(br-ar)*t) << 16) | ((int)(ag+(bg-ag)*t) << 8) | (int)(ab+(bb-ab)*t);
    }
}