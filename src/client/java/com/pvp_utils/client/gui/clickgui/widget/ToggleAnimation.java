package com.pvp_utils.client.gui.clickgui.widget;

public final class ToggleAnimation {
    private static final float DURATION = 0.26f;
    private static final float CLICK_DURATION = 0.3f;
    private boolean target;
    private float progress;
    private float origin;
    private float elapsed = DURATION;
    private float clickProgress = 1f;

    public ToggleAnimation(boolean enabled) {
        target = enabled;
        progress = enabled ? 1f : 0f;
        origin = progress;
    }

    public void update(boolean enabled, float dt) {
        retarget(enabled);
        float step = Float.isFinite(dt) ? Math.max(0f, dt) : 0f;
        if (elapsed < DURATION) {
            elapsed = Math.min(DURATION, elapsed + step);
            float eased = SettingModule.cubicBezierEase(elapsed / DURATION);
            progress = origin + ((target ? 1f : 0f) - origin) * eased;
        }
        clickProgress = Math.min(1f, clickProgress + step / CLICK_DURATION);
    }

    public void click(boolean enabled) {
        retarget(enabled);
        clickProgress = 0f;
    }

    private void retarget(boolean enabled) {
        if (target == enabled) return;
        target = enabled;
        origin = progress;
        elapsed = 0f;
    }

    public float progress() {
        return progress;
    }

    public float press() {
        return clickProgress >= 1f ? 0f : (float) Math.sin(Math.PI * clickProgress);
    }

    public boolean isAnimating() {
        return elapsed < DURATION || clickProgress < 1f;
    }
}
