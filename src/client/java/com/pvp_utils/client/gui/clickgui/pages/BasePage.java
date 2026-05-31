package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.client.gui.clickgui.widget.SettingModule;
import io.github.humbleui.skija.Canvas;

import java.util.ArrayList;
import java.util.List;

public abstract class BasePage {

    protected final List<SettingModule> modules = new ArrayList<>();

    public abstract String getTitle();
    public abstract String getSubtitle();

    public List<SettingModule> getModules() { return modules; }

    public float getTotalHeight() {
        float h = 0;
        for (SettingModule m : modules) h += m.getTotalHeight();
        return h;
    }

    public void update(float dt) {
        for (SettingModule m : modules) m.update(dt);
    }

    public void draw(Canvas canvas, float x, float y, float contentW, float alpha, float scrollOffset) {
        float cy = y - scrollOffset;
        for (SettingModule m : modules) {
            float mh = m.getTotalHeight();
            if (cy + mh > y && cy < y + 9999f) {
                m.draw(canvas, x, cy, contentW, alpha);
            }
            cy += mh + 8f;
        }
    }

    public boolean onClick(float mx, float my, float contentX, float contentY, float contentW, float scrollOffset, int button) {
        float cy = contentY - scrollOffset;
        for (SettingModule m : modules) {
            float mh = m.getTotalHeight();
            if (my >= cy && my <= cy + mh) {
                return m.onClick(mx, my, contentX, cy, contentW, button);
            }
            cy += mh + 8f;
        }
        return false;
    }

    public boolean onDrag(float mx, float my, float contentX, float contentY, float contentW, float scrollOffset) {
        float cy = contentY - scrollOffset;
        for (SettingModule m : modules) {
            float mh = m.getTotalHeight();
            if (m.onDrag(mx, my, contentX, cy, contentW)) return true;
            cy += mh + 8f;
        }
        return false;
    }

    public void releaseDrag() {
        for (SettingModule m : modules) m.releaseDrag();
    }
}