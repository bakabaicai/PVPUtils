package com.pvp_utils.client.gui.clickgui.widget;

import com.pvp_utils.client.ModuleKeybindManager;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.theme.ClickGuiThemeColors;
import com.pvp_utils.client.render.skia.SkijaUi;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.types.Rect;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

public class SettingModule {
    public final String title;
    public final String subtitle;
    public final SettingWidget mainWidget;
    private final List<SubEntry> subEntries = new ArrayList<>();
    private BooleanSupplier visibleSupplier = () -> true;
    private String bindingId = "";
    private String keybindActionId = "";
    private boolean expanded;
    private float expandProgress;
    private float contentWidth = 520f;
    private float keybindWidth = 24f;
    private float keybindHover;
    private float keybindRed;
    private boolean keybindHovered;
    private SettingWidget capturedWidget;
    private Frame frame;

    private static final float KEYBIND_H = 21f;
    private static final float KEYBIND_BASE_W = 21f;
    private static final String ARROW_EXPANDED = "\uE5CF";
    private static final String ARROW_COLLAPSED = "\uE5CC";
    private static final String KEYBIND_ICON = "\uE9FE";
    private static final String CLEAR_ICON = "\uF508";

    public SettingModule(String title, String subtitle, SettingWidget mainWidget) {
        this.title = title;
        this.subtitle = subtitle;
        this.mainWidget = mainWidget;
    }

    public SettingModule addSub(String title, String subtitle, SettingWidget widget) {
        subEntries.add(new SubEntry(title, subtitle, widget, () -> true, false));
        frame = null;
        return this;
    }

    public SettingModule addSub(String title, String subtitle, SettingWidget widget, BooleanSupplier visibleSupplier) {
        SubEntry entry = new SubEntry(title, subtitle, widget, visibleSupplier, false);
        if (!subEntries.isEmpty() && subEntries.getLast().widget instanceof SettingToggle)
            subEntries.getLast().children.add(entry);
        else subEntries.add(entry);
        frame = null;
        return this;
    }

    public SettingModule addSubWhen(BooleanSupplier visibleSupplier, String title, String subtitle, SettingWidget widget) {
        return addSub(title, subtitle, widget, visibleSupplier);
    }

    public SettingModule addSubGroup(String title, String subtitle) {
        subEntries.add(new SubEntry(title, subtitle, null, () -> true, true));
        frame = null;
        return this;
    }

    public SettingModule addSubChild(String title, String subtitle, SettingWidget widget) {
        return addSubChildWhen(() -> true, title, subtitle, widget);
    }

    public SettingModule addSubChildWhen(BooleanSupplier visibleSupplier, String title, String subtitle, SettingWidget widget) {
        if (!subEntries.isEmpty()) {
            subEntries.getLast().children.add(new SubEntry(title, subtitle, widget, visibleSupplier, false));
            frame = null;
        }
        return this;
    }

    public SettingModule visibleWhen(BooleanSupplier supplier) {
        visibleSupplier = supplier;
        return this;
    }

    public void setBindingId(String id) {
        if (bindingId.isEmpty()) bindingId = id == null ? "" : id;
    }

    public String getBindingId() {
        return bindingId;
    }

    public boolean isToggleable() {
        return mainWidget instanceof SettingToggle;
    }

    public SettingModule keybindAction(String id) {
        keybindActionId = id == null ? "" : id;
        return this;
    }

    public boolean usesActionKeybind() {
        return !keybindActionId.isBlank();
    }

    public void toggleFromKeybind() {
        if (mainWidget instanceof SettingToggle toggle) toggle.toggle();
    }

    public boolean isVisible() {
        return visibleSupplier.getAsBoolean();
    }

    public void setContentWidth(float width) {
        float next = Math.max(0f, width);
        if (Math.abs(next - contentWidth) > 0.01f) {
            contentWidth = next;
            frame = null;
        }
    }

    private boolean isKeybindable() {
        return isToggleable() || !keybindActionId.isBlank();
    }

    private String keybindId() {
        return keybindActionId.isBlank() ? bindingId : keybindActionId;
    }

    public boolean matchesSearch(String query) {
        if (query == null || query.isBlank()) return true;
        String needle = query.toLowerCase(Locale.ROOT);
        return UiText.matchesSearch(title, needle) || UiText.matchesSearch(subtitle, needle)
                || matchesSearch(subEntries, needle);
    }

    private boolean matchesSearch(List<SubEntry> entries, String needle) {
        for (SubEntry entry : entries) {
            if (UiText.matchesSearch(entry.title, needle) || UiText.matchesSearch(entry.subtitle, needle)
                    || matchesSearch(entry.children, needle)) return true;
        }
        return false;
    }

    public float getTotalHeight() {
        return mainRow(0f, 0f).height() + expandProgress * entriesHeight(subEntries, 1);
    }

    public boolean isAnimating() {
        if (Math.abs((expanded ? 1f : 0f) - expandProgress) > 0.001f) return true;
        if (isKeybindable() && (Math.abs(keybindWidth - targetKeybindWidth()) > 0.01f
                || Math.abs(keybindHover - (keybindHovered ? 1f : 0f)) > 0.01f)) return true;
        return mainWidget != null && mainWidget.isAnimating() || entriesAnimating(subEntries);
    }

    private boolean entriesAnimating(List<SubEntry> entries) {
        for (SubEntry entry : entries) {
            if (!entry.isVisible()) continue;
            if (Math.abs((entry.expanded ? 1f : 0f) - entry.childProgress) > 0.001f
                    || entry.widget != null && entry.widget.isAnimating() || entriesAnimating(entry.children)) return true;
        }
        return false;
    }

    public void update(float dt) {
        float step = Math.min(1f, Math.max(0f, dt) * 14f);
        expandProgress = approach(expandProgress, expanded ? 1f : 0f, step);
        if (isKeybindable()) {
            keybindWidth = approach(keybindWidth, targetKeybindWidth(), step);
            keybindHover = approach(keybindHover, keybindHovered ? 1f : 0f, step);
            boolean clear = ModuleKeybindManager.hasBinding(keybindId()) && keybindHovered
                    && !ModuleKeybindManager.isCapturing(keybindId())
                    && !ModuleKeybindManager.ACTION_CLICK_GUI.equals(keybindId());
            keybindRed = approach(keybindRed, clear ? 1f : 0f, step);
        }
        if (mainWidget != null) mainWidget.update(dt);
        updateEntries(subEntries, dt, step);
        frame = null;
    }

    private void updateEntries(List<SubEntry> entries, float dt, float step) {
        for (SubEntry entry : entries) {
            if (!entry.isVisible()) {
                releaseEntry(entry);
                continue;
            }
            entry.childProgress = approach(entry.childProgress, entry.expanded ? 1f : 0f, step);
            if (entry.widget != null) entry.widget.update(dt);
            updateEntries(entry.children, dt, step);
        }
    }

    public void draw(Canvas canvas, float x, float y, float contentW, float alpha,
                     float viewportTop, float viewportBottom, float mouseX, float mouseY) {
        ModuleKeybindManager.registerModule(this);
        Frame current = layout(x, y, contentW);
        SettingRowLayout.Row main = current.main();
        ClickGuiThemeColors colors = ClickGuiThemeColors.current();
        SkijaUi.rounded(canvas, main.x(), main.y(), main.width(), main.bodyHeight(), 10f,
                withAlpha(colors.module, ClickGuiThemeColors.panelBackgroundAlpha(alpha)));
        drawLabels(canvas, main, title, subtitle, alpha, colors.primaryText, colors.secondaryText, true);
        drawWidget(canvas, mainWidget, main, alpha);
        drawKeybind(canvas, main, alpha, mouseX, mouseY);
        if (hasVisibleSubEntries()) drawArrow(canvas, main, expandProgress, alpha);
        for (PlacedEntry placed : current.entries()) {
            SettingRowLayout.Row row = placed.row();
            float top = Math.max(viewportTop, Math.max(row.y(), placed.clipTop()));
            float bottom = Math.min(viewportBottom, Math.min(row.y() + row.bodyHeight(), placed.clipBottom()));
            if (bottom <= top || placed.alpha() <= 0.001f) continue;
            int save = canvas.save();
            try {
                canvas.clipRect(Rect.makeXYWH(x, top, contentW, bottom - top));
                float entryAlpha = alpha * placed.alpha();
                int background = lerpColor(colors.content, colors.subModule, placed.entry().group ? 0.55f : 0.28f);
                SkijaUi.rounded(canvas, row.x(), row.y(), row.width(), row.bodyHeight(), 7f,
                        withAlpha(background, ClickGuiThemeColors.panelBackgroundAlpha(entryAlpha) * 0.62f));
                if (placed.depth() >= 2) {
                    SkijaUi.rounded(canvas, Math.max(x, row.x() - 9f), row.y() + 7f, 2f,
                            Math.max(0f, row.bodyHeight() - 14f), 1f, withAlpha(colors.accent, entryAlpha * 0.22f));
                }
                drawLabels(canvas, row, placed.entry().title, placed.entry().subtitle, entryAlpha,
                        colors.primaryText, colors.secondaryText, false);
                drawWidget(canvas, placed.entry().widget, row, entryAlpha);
                if (placed.entry().group && placed.entry().hasVisibleChildren())
                    drawArrow(canvas, row, placed.entry().childProgress, entryAlpha);
            } finally {
                canvas.restoreToCount(save);
            }
        }
    }

    private void drawLabels(Canvas canvas, SettingRowLayout.Row row, String name, String description,
                            float alpha, int primary, int secondary, boolean main) {
        if (row.textWidth() <= 0f) return;
        int save = canvas.save();
        try {
            canvas.clipRect(Rect.makeXYWH(row.textX(), row.y(), row.textWidth(), row.headerHeight()));
            if (description == null || description.isEmpty()) {
                SkijaUi.text(canvas, name, row.textX(), row.y(), row.headerHeight(), withAlpha(primary, alpha), main ? 13f : 12f);
            } else {
                SkijaUi.text(canvas, name, row.textX(), row.y() + (main ? 7f : 3f), 18f, withAlpha(primary, alpha), main ? 13f : 12f);
                SkijaUi.text(canvas, description, row.textX(), row.y() + (main ? 25f : 21f), 14f, withAlpha(secondary, alpha), 10f);
            }
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void drawWidget(Canvas canvas, SettingWidget widget, SettingRowLayout.Row row, float alpha) {
        if (widget == null || row.controlScale() <= 0f) return;
        if (row.controlScale() >= 0.999f) {
            widget.draw(canvas, row.controlX(), row.controlY(), alpha);
            return;
        }
        int save = canvas.save();
        try {
            canvas.translate(row.controlX(), row.controlY());
            canvas.scale(row.controlScale(), row.controlScale());
            canvas.translate(-row.controlX(), -row.controlY());
            widget.draw(canvas, row.controlX(), row.controlY(), alpha);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void drawArrow(Canvas canvas, SettingRowLayout.Row row, float progress, float alpha) {
        String arrow = progress > 0.5f ? ARROW_EXPANDED : ARROW_COLLAPSED;
        SkijaUi.icon(canvas, arrow, row.x() + row.width() - 15f, row.y(), row.headerHeight(),
                withAlpha(ClickGuiThemeColors.current().mutedText, alpha), 12f, SkijaUi.IconSet.MATERIAL_SYMBOLS);
    }

    private void drawKeybind(Canvas canvas, SettingRowLayout.Row row, float alpha, float mouseX, float mouseY) {
        String id = keybindId();
        if (!isKeybindable() || id.isBlank()) return;
        float x = row.controlX() - 8f - keybindWidth;
        float y = keybindY(row);
        keybindHovered = mouseX >= x && mouseX <= x + keybindWidth && mouseY >= y && mouseY <= y + KEYBIND_H;
        boolean bound = ModuleKeybindManager.hasBinding(id);
        boolean capturing = ModuleKeybindManager.isCapturing(id);
        ClickGuiThemeColors colors = ClickGuiThemeColors.current();
        int base = colors.dark ? 0x777777 : 0x555555;
        int hover = colors.dark ? 0x949494 : 0x777777;
        int unbind = colors.dark ? 0xE14D4D : 0xCC3333;
        float buttonAlpha = alpha * (0.34f + keybindHover * 0.18f + keybindRed * 0.32f);
        SkijaUi.rounded(canvas, x, y, keybindWidth, KEYBIND_H, 5f,
                withAlpha(lerpColor(lerpColor(base, hover, keybindHover), unbind, keybindRed), buttonAlpha));
        if (capturing) {
            drawCenteredText(canvas, UiText.t("按下任意键...", "Press any key..."), x, y, keybindWidth, 9f, alpha);
        } else if (bound && keybindRed > 0.12f) {
            drawCenteredIcon(canvas, CLEAR_ICON, x, y, keybindWidth, alpha);
        } else if (bound) {
            drawCenteredText(canvas, ModuleKeybindManager.keyName(id), x, y, keybindWidth, 9f, alpha);
        } else {
            drawCenteredIcon(canvas, KEYBIND_ICON, x, y, keybindWidth, alpha);
        }
    }

    private float targetKeybindWidth() {
        String id = keybindId();
        if (!isKeybindable() || id.isBlank()) return KEYBIND_BASE_W;
        if (ModuleKeybindManager.isCapturing(id))
            return Math.max(110f, SkijaUi.textWidth(UiText.t("按下任意键...", "Press any key..."), 9f) + 20f);
        String name = ModuleKeybindManager.keyName(id);
        return name.isBlank() ? KEYBIND_BASE_W : Math.max(KEYBIND_BASE_W, SkijaUi.textWidth(name, 9f) + 12f);
    }

    private void drawCenteredText(Canvas canvas, String text, float x, float y, float width, float size, float alpha) {
        SkijaUi.text(canvas, text, x + (width - SkijaUi.textWidth(text, size)) * 0.5f,
                y, KEYBIND_H, withAlpha(0xFFFFFF, alpha), size);
    }

    private void drawCenteredIcon(Canvas canvas, String icon, float x, float y, float width, float alpha) {
        SkijaUi.icon(canvas, icon, x + (width - SkijaUi.iconWidth(icon, 12f, SkijaUi.IconSet.MATERIAL_SYMBOLS)) * 0.5f,
                y, KEYBIND_H, withAlpha(0xFFFFFF, alpha), 12f, SkijaUi.IconSet.MATERIAL_SYMBOLS);
    }

    public boolean onClick(float mx, float my, float x, float y, float contentW, int button) {
        Frame current = layout(x, y, contentW);
        SettingRowLayout.Row main = current.main();
        if (main.contains(mx, my)) {
            if (button == 1 && my <= main.y() + main.headerHeight() && hasVisibleSubEntries()) {
                expanded = !expanded;
                releaseDrag();
                return true;
            }
            String id = keybindId();
            float keybindX = main.controlX() - 8f - keybindWidth;
            float keybindY = keybindY(main);
            if (button == 0 && isKeybindable() && !id.isBlank() && mx >= keybindX
                    && mx <= keybindX + keybindWidth && my >= keybindY && my <= keybindY + KEYBIND_H) {
                if (ModuleKeybindManager.hasBinding(id) && !ModuleKeybindManager.ACTION_CLICK_GUI.equals(id))
                    ModuleKeybindManager.clearBinding(id);
                else ModuleKeybindManager.beginCapture(id);
                return true;
            }
            if (button == 0 && hasVisibleSubEntries() && mx >= main.x() + main.width() - 18f
                    && my <= main.y() + main.headerHeight()) {
                expanded = !expanded;
                releaseDrag();
                return true;
            }
            return clickWidget(mainWidget, main, mx, my, button);
        }
        if (!expanded || expandProgress <= 0.01f) return false;
        for (PlacedEntry placed : current.entries()) {
            SettingRowLayout.Row row = placed.row();
            if (my < placed.clipTop() || my > placed.clipBottom() || !row.contains(mx, my)) continue;
            if (placed.entry().group && my <= row.y() + row.headerHeight() && (button == 0 || button == 1)) {
                placed.entry().expanded = !placed.entry().expanded;
                releaseDrag();
                return true;
            }
            return clickWidget(placed.entry().widget, row, mx, my, button);
        }
        return false;
    }

    private boolean clickWidget(SettingWidget widget, SettingRowLayout.Row row, float mx, float my, int button) {
        if (widget == null || !row.containsControl(mx, my)) return false;
        boolean accepted = widget.onClick(row.widgetMouseX(mx), row.widgetMouseY(my), row.controlX(), row.controlY(), button);
        if (accepted && button == 0) capturedWidget = widget;
        return accepted;
    }

    public boolean onDrag(float mx, float my, float x, float y, float contentW) {
        if (capturedWidget == null) return false;
        Frame current = layout(x, y, contentW);
        if (capturedWidget == mainWidget) return dragWidget(mainWidget, current.main(), mx, my);
        if (expanded) {
            for (PlacedEntry placed : current.entries()) {
                if (placed.entry().widget == capturedWidget)
                    return dragWidget(capturedWidget, placed.row(), mx, my);
            }
        }
        capturedWidget.releaseDrag();
        capturedWidget = null;
        return false;
    }

    private boolean dragWidget(SettingWidget widget, SettingRowLayout.Row row, float mx, float my) {
        return row.controlScale() > 0f && widget.onDrag(row.widgetMouseX(mx), row.widgetMouseY(my), row.controlX(), row.controlY());
    }

    public boolean onScroll(float mx, float my, float x, float y, float contentW, float amount) {
        Frame current = layout(x, y, contentW);
        if (scrollWidget(mainWidget, current.main(), mx, my, amount)) return true;
        if (expanded) {
            for (PlacedEntry placed : current.entries()) {
                if (my >= placed.clipTop() && my <= placed.clipBottom()
                        && scrollWidget(placed.entry().widget, placed.row(), mx, my, amount)) return true;
            }
        }
        return false;
    }

    private boolean scrollWidget(SettingWidget widget, SettingRowLayout.Row row, float mx, float my, float amount) {
        return widget != null && row.containsControl(mx, my)
                && widget.onScroll(row.widgetMouseX(mx), row.widgetMouseY(my), row.controlX(), row.controlY(), amount);
    }

    public void releaseDrag() {
        if (mainWidget != null) mainWidget.releaseDrag();
        for (SubEntry entry : subEntries) releaseEntry(entry);
        capturedWidget = null;
    }

    private void releaseEntry(SubEntry entry) {
        if (entry.widget != null) entry.widget.releaseDrag();
        for (SubEntry child : entry.children) releaseEntry(child);
    }

    private SettingRowLayout.Row mainRow(float x, float y) {
        return SettingRowLayout.measure(x, y, contentWidth, 0, mainWidget, isKeybindable() ? keybindWidth + 8f : 0f);
    }

    private float entriesHeight(List<SubEntry> entries, int depth) {
        float height = 0f;
        for (SubEntry entry : entries) {
            if (!entry.isVisible()) continue;
            height += SettingRowLayout.measure(0f, 0f, contentWidth, depth, entry.widget, entry.group ? 14f : 0f).height();
            if (!entry.children.isEmpty()) height += entry.childrenProgress() * entriesHeight(entry.children, depth + 1);
        }
        return height;
    }

    private Frame layout(float x, float y, float width) {
        setContentWidth(width);
        if (frame != null && frame.main().x() == x && frame.main().y() == y) return frame;
        SettingRowLayout.Row main = mainRow(x, y);
        List<PlacedEntry> rows = new ArrayList<>();
        float start = y + main.height();
        float bottom = start + expandProgress * entriesHeight(subEntries, 1);
        if (expandProgress > 0.001f) appendEntries(rows, subEntries, x, start, 1, expandProgress, start, bottom);
        frame = new Frame(main, rows);
        return frame;
    }

    private float appendEntries(List<PlacedEntry> rows, List<SubEntry> entries, float x, float y, int depth,
                                float alpha, float clipTop, float clipBottom) {
        float currentY = y;
        for (SubEntry entry : entries) {
            if (!entry.isVisible()) continue;
            SettingRowLayout.Row row = SettingRowLayout.measure(x, currentY, contentWidth, depth,
                    entry.widget, entry.group ? 14f : 0f);
            rows.add(new PlacedEntry(entry, row, depth, alpha, clipTop, clipBottom));
            currentY += row.height();
            if (!entry.children.isEmpty() && entry.childrenProgress() > 0.001f) {
                float childProgress = entry.childrenProgress();
                float childHeight = childProgress * entriesHeight(entry.children, depth + 1);
                appendEntries(rows, entry.children, x, currentY, depth + 1, alpha * childProgress,
                        Math.max(clipTop, currentY), Math.min(clipBottom, currentY + childHeight));
                currentY += childHeight;
            }
        }
        return currentY - y;
    }

    private float keybindY(SettingRowLayout.Row row) {
        return mainWidget == null ? row.y() + (row.headerHeight() - KEYBIND_H) * 0.5f
                : row.controlY() + (row.controlHeight() - KEYBIND_H) * 0.5f;
    }

    private boolean hasVisibleSubEntries() {
        for (SubEntry entry : subEntries) if (entry.isVisible()) return true;
        return false;
    }

    private static float approach(float current, float target, float step) {
        float next = current + (target - current) * step;
        return Math.abs(next - target) < 0.001f ? target : next;
    }

    private static int withAlpha(int color, float alpha) {
        return (Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f) << 24) | (color & 0x00FFFFFF);
    }

    private static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    private record PlacedEntry(SubEntry entry, SettingRowLayout.Row row, int depth, float alpha, float clipTop, float clipBottom) {
    }

    private record Frame(SettingRowLayout.Row main, List<PlacedEntry> entries) {
    }

    private static final class SubEntry {
        private final String title;
        private final String subtitle;
        private final SettingWidget widget;
        private final BooleanSupplier visibleSupplier;
        private final boolean group;
        private final List<SubEntry> children = new ArrayList<>();
        private boolean expanded;
        private float childProgress;

        private SubEntry(String title, String subtitle, SettingWidget widget, BooleanSupplier visibleSupplier, boolean group) {
            this.title = title;
            this.subtitle = subtitle;
            this.widget = widget;
            this.visibleSupplier = visibleSupplier == null ? () -> true : visibleSupplier;
            this.group = group;
        }

        private boolean isVisible() {
            return visibleSupplier.getAsBoolean();
        }

        private boolean hasVisibleChildren() {
            for (SubEntry child : children) if (child.isVisible()) return true;
            return false;
        }

        private float childrenProgress() {
            return group ? childProgress : 1f;
        }
    }
}
