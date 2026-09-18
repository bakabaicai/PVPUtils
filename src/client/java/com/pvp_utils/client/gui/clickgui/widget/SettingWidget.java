package com.pvp_utils.client.gui.clickgui.widget;

import io.github.humbleui.skija.Canvas;

public abstract class SettingWidget {

    protected static int withAlpha(int color, float alpha) {
        return ((int)(alpha * 255) << 24) | (color & 0x00FFFFFF);
    }

    public abstract float getWidth();
    public abstract float getHeight();
    public abstract void draw(Canvas canvas, float x, float y, float alpha);
    public void update(float dt) { }
    public boolean isAnimating() { return false; }
    public boolean onClick(float mx, float my, float x, float y, int button) { return false; }
    public boolean onDrag(float mx, float my, float x, float y) { return false; }
    public boolean onScroll(float mx, float my, float x, float y, float amount) { return false; }
}
