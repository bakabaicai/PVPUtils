package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.client.gui.clickgui.widget.SettingModule;
import io.github.humbleui.skija.Canvas;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class BasePage {

    private static final float COLUMN_GAP = 8f;
    private static final float ROW_GAP = 6f;

    protected final List<SettingModule> modules = new ArrayList<>();
    private final List<SettingModule> visibleModules = new ArrayList<>();
    private float cachedTotalHeight = -1f;
    private float panelShift;
    private String searchQuery = "";
    private SettingModule hoveredModule;

    public abstract String getTitle();
    public abstract String getSubtitle();

    public List<SettingModule> getModules() {
        assignBindingIds();
        return modules;
    }

    public void setPanelShift(float shift) {
        panelShift = shift;
    }

    public void setSearchQuery(String query) {
        String normalized = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        if (!searchQuery.equals(normalized)) {
            searchQuery = normalized;
            cachedTotalHeight = -1f;
        }
    }

    public float getTotalHeight() {
        ensureLayoutCache();
        return cachedTotalHeight;
    }

    public boolean hasSearchResults() {
        ensureLayoutCache();
        return !visibleModules.isEmpty();
    }

    public SettingModule getHoveredModule() {
        return hoveredModule;
    }

    public SettingModule getOpenFlyout() {
        for (SettingModule module : modules) {
            if (module.isFlyoutOpen()) return module;
        }
        return null;
    }

    public void closeFlyouts() {
        closeFlyoutsExcept(null);
    }

    public void closeFlyoutsExcept(SettingModule keep) {
        for (SettingModule module : modules) {
            if (module != keep && module.isFlyoutOpen()) module.setFlyoutOpen(false);
        }
    }

    public boolean openFirstFlyout(float contentX, float contentY, float contentW, float contentH, float scrollOffset) {
        ensureLayoutCache();
        float colW = columnWidth(contentW);
        if (colW <= 0f) return false;
        for (int i = 0; i < visibleModules.size(); i++) {
            SettingModule m = visibleModules.get(i);
            float cx = contentX + columnX(i, contentW);
            float cy = contentY + rowY(i) - scrollOffset;
            if (cy > contentY + contentH) break;
            if (cy + SettingModule.MODULE_H < contentY) continue;
            if (!m.hasFlyoutContent()) continue;
            m.openFlyoutInstant(cx + panelShift, cy, colW, SettingModule.TILE_H);
            closeFlyoutsExcept(m);
            return true;
        }
        closeFlyouts();
        return false;
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

    private float columnWidth(float contentW) {
        return Math.max(0f, (contentW - COLUMN_GAP) / 2f);
    }

    private float columnX(int index, float contentW) {
        return (index % 2) * (columnWidth(contentW) + COLUMN_GAP);
    }

    private float rowY(int index) {
        return (index / 2) * (SettingModule.MODULE_H + ROW_GAP);
    }

    public void draw(Canvas canvas, float x, float y, float contentW, float contentH, float alpha, float scrollOffset, float mouseX, float mouseY) {
        ensureLayoutCache();
        float colW = columnWidth(contentW);
        if (colW <= 0f) return;
        float viewportTop = y;
        float viewportBottom = y + contentH;
        hoveredModule = null;
        for (int i = 0; i < visibleModules.size(); i++) {
            SettingModule m = visibleModules.get(i);
            float cx = x + columnX(i, contentW);
            float cy = y + rowY(i) - scrollOffset;
            if (cy + SettingModule.MODULE_H <= viewportTop || cy >= viewportBottom) continue;
            if (mouseY >= viewportTop && mouseY <= viewportBottom
                    && mouseX >= cx && mouseX <= cx + colW
                    && mouseY >= cy && mouseY <= cy + SettingModule.MODULE_H) {
                hoveredModule = m;
            }
            m.draw(canvas, cx, cy, colW, alpha, mouseX, mouseY);
        }
    }

    public boolean onClick(float mx, float my, float contentX, float contentY, float contentW, float scrollOffset, int button) {
        ensureLayoutCache();
        float colW = columnWidth(contentW);
        if (colW <= 0f) return false;
        for (int i = 0; i < visibleModules.size(); i++) {
            SettingModule m = visibleModules.get(i);
            float cx = contentX + columnX(i, contentW);
            float cy = contentY + rowY(i) - scrollOffset;
            if (mx < cx || mx > cx + colW || my < cy || my > cy + SettingModule.MODULE_H) continue;
            boolean flyoutWasOpen = m.isFlyoutOpen();
            boolean hit = m.onClick(mx, my, cx, cy, colW, button);
            if (hit) {
                if (!flyoutWasOpen && m.isFlyoutOpen()) {
                    m.setFlyoutSource(cx + panelShift, cy, colW, SettingModule.TILE_H);
                    closeFlyoutsExcept(m);
                }
            }
            return hit;
        }
        return false;
    }

    public boolean onDrag(float mx, float my, float contentX, float contentY, float contentW, float scrollOffset) {
        ensureLayoutCache();
        float colW = columnWidth(contentW);
        if (colW <= 0f) return false;
        for (int i = 0; i < visibleModules.size(); i++) {
            SettingModule m = visibleModules.get(i);
            float cx = contentX + columnX(i, contentW);
            float cy = contentY + rowY(i) - scrollOffset;
            if (m.onDrag(mx, my, cx, cy, colW)) return true;
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
        assignBindingIds();
        visibleModules.clear();
        for (SettingModule m : modules) {
            if (!m.isVisible() || !m.matchesSearch(searchQuery)) continue;
            visibleModules.add(m);
        }
        int rows = (visibleModules.size() + 1) / 2;
        cachedTotalHeight = rows == 0
                ? 0f
                : rows * SettingModule.MODULE_H + (rows - 1) * ROW_GAP;
    }

    private void assignBindingIds() {
        String pageId = getClass().getName();
        for (int i = 0; i < modules.size(); i++) {
            modules.get(i).setBindingId(pageId + "." + i);
        }
    }
}
