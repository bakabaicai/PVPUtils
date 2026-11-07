package com.pvp_utils.client.gui.clickgui.widget;

import java.util.Objects;

public final class PageTransition<T> {
    private static final float EXIT_DURATION = 0.12f;
    private static final float ENTER_DURATION = 0.18f;
    private T displayed;
    private T target;
    private float progress = 1f;

    public PageTransition(T initial) {
        reset(initial);
    }

    public void request(T page) {
        target = Objects.requireNonNull(page);
    }

    public void reset(T page) {
        displayed = Objects.requireNonNull(page);
        target = displayed;
        progress = 1f;
    }

    public boolean update(float dt) {
        T previous = displayed;
        float step = Float.isFinite(dt) ? Math.max(0f, dt) : 0f;
        if (!Objects.equals(displayed, target)) {
            float remaining = progress * EXIT_DURATION;
            if (step < remaining) {
                progress = Math.max(0f, progress - step / EXIT_DURATION);
            } else {
                displayed = target;
                progress = Math.min(1f, (step - remaining) / ENTER_DURATION);
            }
        } else {
            progress = Math.min(1f, progress + step / ENTER_DURATION);
        }
        return !Objects.equals(previous, displayed);
    }

    public T displayed() {
        return displayed;
    }

    public T target() {
        return target;
    }

    public float opacity() {
        return SettingModule.cubicBezierEase(progress);
    }

    public float offsetX() {
        return (1f - opacity()) * 9f;
    }

    public boolean isInteractive() {
        return Objects.equals(displayed, target) && progress >= 1f;
    }
}
