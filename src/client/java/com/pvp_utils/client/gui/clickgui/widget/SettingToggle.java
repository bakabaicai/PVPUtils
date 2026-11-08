package com.pvp_utils.client.gui.clickgui.widget;

import com.pvp_utils.client.gui.clickgui.theme.ClickGuiThemeColors;
import com.pvp_utils.client.render.skia.SkijaUi;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.PaintStrokeCap;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SettingToggle extends SettingWidget {

    private final Supplier<Boolean> getter;
    private final Consumer<Boolean> setter;
    private final ToggleAnimation animation;

    public SettingToggle(Supplier<Boolean> getter, Consumer<Boolean> setter) {
        this.getter = getter;
        this.setter = setter;
        animation = new ToggleAnimation(getter.get());
    }

    public void toggle() {
        setter.accept(!getter.get());
        animation.click(getter.get());
    }

    public boolean isOn() {
        return getter.get();
    }

    @Override public float getWidth() { return 44f; }
    @Override public float getHeight() { return 24f; }

    @Override
    public void update(float dt) {
        animation.update(getter.get(), dt);
    }

    @Override
    public void draw(Canvas canvas, float x, float y, float alpha) {
        float progress = animation.progress();
        float press = animation.press();
        ClickGuiThemeColors colors = ClickGuiThemeColors.current();
        int trackColor = lerpColor(colors.scrollbarTrack, colors.accent, progress);
        SkijaUi.rounded(canvas, x, y, 44f, 24f, 12f,
                withAlpha(trackColor, ClickGuiThemeColors.panelBackgroundAlpha(alpha)));
        float centerX = x + 12f + 20f * progress;
        float centerY = y + 12f;
        float thumbWidth = 20f - 1.6f * press;
        float thumbHeight = 20f + 0.8f * press;
        SkijaUi.rounded(canvas, centerX - thumbWidth * 0.5f, centerY - thumbHeight * 0.5f,
                thumbWidth, thumbHeight, 10f, withAlpha(0xFFFFFF, alpha));
        int symbolColor = withAlpha(colors.dark ? colors.window : colors.primaryText, alpha);
        int save = canvas.save();
        try {
            canvas.translate(centerX, centerY);
            canvas.rotate(180f * progress);
            SkijaUi.line(canvas, lerp(-3.4f, 4.2f, progress), lerp(-3.4f, 0f, progress),
                    lerp(3.4f, 1.1f, progress), lerp(3.4f, -2.9f, progress),
                    1.7f, symbolColor, PaintStrokeCap.ROUND);
            SkijaUi.line(canvas, lerp(3.4f, 1.1f, progress), lerp(-3.4f, -2.9f, progress),
                    lerp(-3.4f, -4.6f, progress), 3.4f,
                    1.7f, symbolColor, PaintStrokeCap.ROUND);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    @Override
    public boolean isAnimating() {
        return animation.isAnimating();
    }

    @Override
    public boolean onClick(float mx, float my, float x, float y, int button) {
        if (button != 0) return false;
        toggle();
        return true;
    }

    private static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int)(ar+(br-ar)*t) << 16) | ((int)(ag+(bg-ag)*t) << 8) | (int)(ab+(bb-ab)*t);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
