package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.client.gui.clickgui.widget.SettingModule;
import io.github.humbleui.skija.Canvas;

import java.util.ArrayList;
import java.util.List;

public abstract class BasePage {

    protected final List<SettingModule> modules = new ArrayList<>();
    private final List<SettingModule> visibleModules = new ArrayList<>();
    private final List<Float> visibleModuleHeights = new ArrayList<>();
    private float cachedTotalHeight = -1f;

    public abstract String getTitle();
    public abstract String getSubtitle();

    public List<SettingModule> getModules() { return modules; }

    public float getTotalHeight() {
        ensureLayoutCache();
        return cachedTotalHeight;
    }

    public void update(float dt) {
        rebuildLayoutCache();
        for (SettingModule m : visibleModules) m.update(dt);
        rebuildLayoutCache();
    }

    public boolean hasAnimatingModules() {
        ensureLayoutCache();
        for (SettingModule m : visibleModules) {
            if (m.isAnimating()) return true;
        }
        return false;
    }

    public void draw(Canvas canvas, float x, float y, float contentW, float contentH, float alpha, float scrollOffset) {
        ensureLayoutCache();
        float cy = y - scrollOffset;
        float viewportTop = y;
        float viewportBottom = y + contentH;
        for (int i = 0; i < visibleModules.size(); i++) {
            SettingModule m = visibleModules.get(i);
            float mh = visibleModuleHeights.get(i);
            if (cy + mh > viewportTop && cy < viewportBottom) {
                m.draw(canvas, x, cy, contentW, alpha, viewportTop, viewportBottom);
            }
            cy += mh + 8f;
        }
    }

    public boolean onClick(float mx, float my, float contentX, float contentY, float contentW, float scrollOffset, int button) {
        ensureLayoutCache();
        float cy = contentY - scrollOffset;
        for (int i = 0; i < visibleModules.size(); i++) {
            SettingModule m = visibleModules.get(i);
            float mh = visibleModuleHeights.get(i);
            if (my >= cy && my <= cy + mh) {
                return m.onClick(mx, my, contentX, cy, contentW, button);
            }
            cy += mh + 8f;
        }
        return false;
    }

    public boolean onDrag(float mx, float my, float contentX, float contentY, float contentW, float scrollOffset) {
        ensureLayoutCache();
        float cy = contentY - scrollOffset;
        for (int i = 0; i < visibleModules.size(); i++) {
            SettingModule m = visibleModules.get(i);
            float mh = visibleModuleHeights.get(i);
            if (m.onDrag(mx, my, contentX, cy, contentW)) return true;
            cy += mh + 8f;
        }
        return false;
    }

    public void releaseDrag() {
        ensureLayoutCache();
        for (SettingModule m : visibleModules) m.releaseDrag();
    }

    private void ensureLayoutCache() {
        if (cachedTotalHeight >= 0f) return;
        rebuildLayoutCache();
    }

    private void rebuildLayoutCache() {
        visibleModules.clear();
        visibleModuleHeights.clear();
        float totalHeight = 0f;
        for (SettingModule m : modules) {
            if (!m.isVisible()) continue;
            float moduleHeight = m.getTotalHeight();
            visibleModules.add(m);
            visibleModuleHeights.add(moduleHeight);
            totalHeight += moduleHeight;
        }
        cachedTotalHeight = totalHeight;
    }
}
