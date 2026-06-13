package com.pvp_utils.client.gui.clickgui.widget;

import com.pvp_utils.client.render.font.FontRenderer;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;

public class SettingButton extends SettingWidget {
    private final String label;
    private final Runnable action;
    private float pressT = 0f;

    public SettingButton(String label, Runnable action) {
        this.label = label;
        this.action = action;
    }

    @Override public float getWidth() { return 58f; }
    @Override public float getHeight() { return 24f; }

    @Override
    public void draw(Canvas canvas, float x, float y, float alpha) {
        pressT += (0f - pressT) * 0.18f;
        int bgColor = lerpColor(0xFF2F54EB, 0xFF1D39C4, pressT);
        try (Paint bg = new Paint()) {
            bg.setColor(withAlpha(bgColor, alpha));
            canvas.drawRRect(RRect.makeXYWH(x, y, getWidth(), getHeight(), 8f), bg);
        }
        float textW = FontRenderer.measureTextWidth(label, 11f);
        FontRenderer.drawText(canvas, label, x + (getWidth() - textW) * 0.5f, y + 15.5f, 11f, withAlpha(0xFFFFFF, alpha));
    }

    @Override
    public boolean onClick(float mx, float my, float x, float y, int button) {
        if (button != 0) return false;
        if (mx < x || mx > x + getWidth() || my < y || my > y + getHeight()) return false;
        pressT = 1f;
        action.run();
        return true;
    }

    private static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int)(ar+(br-ar)*t) << 16) | ((int)(ag+(bg-ag)*t) << 8) | (int)(ab+(bb-ab)*t);
    }
}
