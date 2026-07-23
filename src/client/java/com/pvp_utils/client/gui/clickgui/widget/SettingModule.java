package com.pvp_utils.client.gui.clickgui.widget;

import com.pvp_utils.client.ModuleKeybindManager;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.render.font.FontRenderer;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;

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
    private float keybindWidth = 24f;
    private float keybindHover;
    private float keybindRed;

    private static final float MODULE_H = 56f;
    private static final float SUB_H = 44f;
    private static final float PAD_X = 20f;
    private static final float KEYBIND_H = 21f;
    private static final float KEYBIND_BASE_W = 21f;
    private static final String ARROW_EXPANDED = "\uE5CF";
    private static final String ARROW_COLLAPSED = "\uE5CC";
    private static final String KEYBIND_ICON = "\uE9FE";
    private static final String CLEAR_ICON = "\uF508";
    private final Paint modulePaint = new Paint().setAntiAlias(true);
    private final Paint subPaint = new Paint().setAntiAlias(true);
    private final Paint keybindPaint = new Paint().setAntiAlias(true);

    public SettingModule(String title, String subtitle, SettingWidget mainWidget) {
        this.title = title;
        this.subtitle = subtitle;
        this.mainWidget = mainWidget;
    }

    public SettingModule addSub(String title, String subtitle, SettingWidget widget) {
        subEntries.add(new SubEntry(title, subtitle, widget, () -> true));
        return this;
    }

    public SettingModule addSub(String title, String subtitle, SettingWidget widget, BooleanSupplier visibleSupplier) {
        subEntries.add(new SubEntry(title, subtitle, widget, visibleSupplier));
        return this;
    }

    public SettingModule addSubWhen(BooleanSupplier visibleSupplier, String title, String subtitle, SettingWidget widget) {
        subEntries.add(new SubEntry(title, subtitle, widget, visibleSupplier));
        return this;
    }

    public SettingModule visibleWhen(BooleanSupplier supplier) {
        visibleSupplier = supplier;
        return this;
    }

    public void setBindingId(String id) {
        if (bindingId.isEmpty()) {
            bindingId = id == null ? "" : id;
        }
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

    private boolean isKeybindable() {
        return isToggleable() || !keybindActionId.isBlank();
    }

    private String keybindId() {
        return keybindActionId.isBlank() ? bindingId : keybindActionId;
    }

    public boolean matchesSearch(String query) {
        if (query == null || query.isBlank()) return true;
        String needle = query.toLowerCase(Locale.ROOT);
        if (UiText.matchesSearch(title, needle) || UiText.matchesSearch(subtitle, needle)) {
            return true;
        }
        for (SubEntry sub : subEntries) {
            if (UiText.matchesSearch(sub.title, needle) || UiText.matchesSearch(sub.subtitle, needle)) {
                return true;
            }
        }
        return false;
    }

    public float getTotalHeight() {
        return MODULE_H + expandProgress * getVisibleSubCount() * SUB_H;
    }

    public boolean isAnimating() {
        if (Math.abs((expanded ? 1f : 0f) - expandProgress) > 0.01f) return true;
        if (isKeybindable() && (Math.abs(keybindWidth - targetKeybindWidth()) > 0.01f || keybindHover > 0.01f || keybindRed > 0.01f)) return true;
        if (mainWidget != null && mainWidget.isAnimating()) return true;
        for (SubEntry sub : subEntries) {
            if (sub.isVisible() && sub.widget != null && sub.widget.isAnimating()) return true;
        }
        return false;
    }

    public void update(float dt) {
        expandProgress += ((expanded ? 1f : 0f) - expandProgress) * Math.min(1f, dt * 14f);
        if (expandProgress < 0.001f) expandProgress = 0f;
        if (expandProgress > 0.999f) expandProgress = 1f;
        if (isKeybindable()) keybindWidth += (targetKeybindWidth() - keybindWidth) * Math.min(1f, dt * 15f);
        if (mainWidget != null) mainWidget.update(dt);
        for (SubEntry sub : subEntries) {
            if (sub.isVisible() && sub.widget != null) sub.widget.update(dt);
        }
    }

    public void draw(Canvas canvas, float x, float y, float contentW, float alpha, float viewportTop, float viewportBottom, float mouseX, float mouseY) {
        ModuleKeybindManager.registerModule(this);
        drawStaticContent(canvas, x, y, contentW, alpha, viewportTop, viewportBottom, expandProgress);
        if (mainWidget != null || isKeybindable()) {
            float widgetWidth = mainWidget == null ? 0f : mainWidget.getWidth();
            float widgetHeight = mainWidget == null ? KEYBIND_H : mainWidget.getHeight();
            float wx = x + contentW - PAD_X - widgetWidth;
            float wy = y + (MODULE_H - 8f - widgetHeight) / 2f;
            drawKeybind(canvas, wx, wy, alpha, mouseX, mouseY);
            if (mainWidget != null) mainWidget.draw(canvas, wx, wy, alpha);
        }
        if (expandProgress > 0.01f) {
            float sy = y + MODULE_H;
            for (SubEntry sub : subEntries) {
                if (!sub.isVisible()) continue;
                float subBottom = sy + SUB_H - 6f;
                if (subBottom > viewportTop && sy < viewportBottom && sub.widget != null) {
                    float wx = x + contentW - PAD_X - sub.widget.getWidth();
                    float wy = sy + (SUB_H - 6f - sub.widget.getHeight()) / 2f;
                    sub.widget.draw(canvas, wx, wy, alpha * expandProgress);
                }
                sy += SUB_H;
            }
        }
    }

    private void drawKeybind(Canvas canvas, float widgetX, float widgetY, float alpha, float mouseX, float mouseY) {
        String id = keybindId();
        if (!isKeybindable() || id.isBlank()) return;
        float x = widgetX - 8f - keybindWidth;
        float y = widgetY + 1.5f;
        boolean hovered = mouseX >= x && mouseX <= x + keybindWidth && mouseY >= y && mouseY <= y + KEYBIND_H;
        boolean bound = ModuleKeybindManager.hasBinding(id);
        boolean capturing = ModuleKeybindManager.isCapturing(id);
        keybindHover += ((hovered ? 1f : 0f) - keybindHover) * 0.2f;
        keybindRed += ((bound && hovered && !capturing && !ModuleKeybindManager.ACTION_CLICK_GUI.equals(id) ? 1f : 0f) - keybindRed) * 0.2f;
        int hoverColor = lerpColor(0xFF777777, 0xFF949494, keybindHover);
        float buttonAlpha = alpha * (0.34f + keybindHover * 0.18f + keybindRed * 0.32f);
        keybindPaint.setColor(withAlpha(lerpColor(hoverColor, 0xFFE14D4D, keybindRed), buttonAlpha));
        canvas.drawRRect(RRect.makeXYWH(x, y, keybindWidth, KEYBIND_H, 5f), keybindPaint);
        if (capturing) {
            String text = UiText.t("按下任意键...", "Press any key...");
            drawCenteredText(canvas, text, x, y, keybindWidth, 9f, alpha);
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
        if (ModuleKeybindManager.isCapturing(id)) {
            return Math.max(110f, FontRenderer.measureTextWidth(UiText.t("按下任意键...", "Press any key..."), 9f) + 20f);
        }
        String keyName = ModuleKeybindManager.keyName(id);
        return keyName.isBlank() ? KEYBIND_BASE_W : Math.max(KEYBIND_BASE_W, FontRenderer.measureTextWidth(keyName, 9f) + 12f);
    }

    private void drawCenteredText(Canvas canvas, String text, float x, float y, float width, float size, float alpha) {
        float textW = FontRenderer.measureTextWidth(text, size);
        FontRenderer.drawText(canvas, text, x + (width - textW) / 2f, y + KEYBIND_H / 2f + 3.5f, size, withAlpha(0xFFFFFF, alpha));
    }

    private void drawCenteredIcon(Canvas canvas, String icon, float x, float y, float width, float alpha) {
        float iconW = FontRenderer.measureTextWidth(icon, 12f, FontRenderer.MATERIAL_SYMBOLS);
        FontRenderer.drawText(canvas, icon, x + (width - iconW) / 2f, y + KEYBIND_H / 2f + 6.2f, 12f, withAlpha(0xFFFFFF, alpha), FontRenderer.MATERIAL_SYMBOLS);
    }

    private void drawStaticContent(Canvas canvas, float x, float y, float contentW, float alpha, float viewportTop, float viewportBottom, float progress) {
        modulePaint.setColor(withAlpha(0xFFFFFF, alpha));
        canvas.drawRRect(RRect.makeXYWH(x, y, contentW, MODULE_H - 8f, 10f), modulePaint);
        FontRenderer.drawText(canvas, title, x + PAD_X, y + 22f, 13f, withAlpha(0x111111, alpha));
        FontRenderer.drawText(canvas, subtitle, x + PAD_X, y + 38f, 10f, withAlpha(0xAAAAAA, alpha));
        if (progress > 0.01f) {
            float sy = y + MODULE_H;
            for (SubEntry sub : subEntries) {
                if (!sub.isVisible()) continue;
                float subBottom = sy + SUB_H - 6f;
                if (subBottom > viewportTop && sy < viewportBottom) {
                    float subAlpha = alpha * progress;
                    subPaint.setColor(withAlpha(0xF8F8FF, subAlpha));
                    canvas.drawRRect(RRect.makeXYWH(x + 8f, sy, contentW - 8f, SUB_H - 6f, 8f), subPaint);
                    if (sub.subtitle == null || sub.subtitle.isEmpty()) {
                        FontRenderer.drawText(canvas, sub.title, x + PAD_X + 8f, sy + (SUB_H - 6f) / 2f + 4.5f, 12f, withAlpha(0x333333, subAlpha));
                    } else {
                        FontRenderer.drawText(canvas, sub.title, x + PAD_X + 8f, sy + 16f, 12f, withAlpha(0x333333, subAlpha));
                        FontRenderer.drawText(canvas, sub.subtitle, x + PAD_X + 8f, sy + 30f, 10f, withAlpha(0xAAAAAA, subAlpha));
                    }
                }
                sy += SUB_H;
            }
        }
        if (hasVisibleSubEntries()) {
            String arrow = progress > 0.5f ? ARROW_EXPANDED : ARROW_COLLAPSED;
            float aw = FontRenderer.measureTextWidth(arrow, 12f, FontRenderer.MATERIAL_SYMBOLS);
            FontRenderer.drawText(canvas, arrow, x + contentW - 5f - aw, y + (MODULE_H - 8f) / 2f + 5.5f, 12f, withAlpha(0xBBBBBB, alpha), FontRenderer.MATERIAL_SYMBOLS);
        }
    }

    public boolean onClick(float mx, float my, float x, float y, float contentW, int button) {
        float moduleBottom = y + MODULE_H - 8f;
        if (my >= y && my <= moduleBottom) {
            if (button == 1 && hasVisibleSubEntries()) {
                expanded = !expanded;
                return true;
            }
            String id = keybindId();
            if (button == 0 && isKeybindable() && !id.isBlank()) {
                float widgetX = x + contentW - PAD_X - (mainWidget == null ? 0f : mainWidget.getWidth());
                float keybindX = widgetX - 8f - keybindWidth;
                float keybindY = y + (MODULE_H - 8f - KEYBIND_H) / 2f + 1.5f;
                if (mx >= keybindX && mx <= keybindX + keybindWidth && my >= keybindY && my <= keybindY + KEYBIND_H) {
                    if (ModuleKeybindManager.hasBinding(id) && !ModuleKeybindManager.ACTION_CLICK_GUI.equals(id)) {
                        ModuleKeybindManager.clearBinding(id);
                    } else {
                        ModuleKeybindManager.beginCapture(id);
                    }
                    return true;
                }
            }
            if (button == 0 && mainWidget != null) {
                float wx = x + contentW - PAD_X - mainWidget.getWidth();
                float wy = y + (MODULE_H - 8f - mainWidget.getHeight()) / 2f;
                return mainWidget.onClick(mx, my, wx, wy, button);
            }
        }
        if (expanded && expandProgress > 0.5f) {
            float sy = y + MODULE_H;
            for (SubEntry sub : subEntries) {
                if (!sub.isVisible()) continue;
                float subBottom = sy + SUB_H - 6f;
                if (my >= sy && my <= subBottom && sub.widget != null) {
                    float wx = x + contentW - PAD_X - sub.widget.getWidth();
                    float wy = sy + (SUB_H - 6f - sub.widget.getHeight()) / 2f;
                    return sub.widget.onClick(mx, my, wx, wy, button);
                }
                sy += SUB_H;
            }
        }
        return false;
    }

    public boolean onDrag(float mx, float my, float x, float y, float contentW) {
        if (mainWidget instanceof SettingTextBox textBox) {
            float wx = x + contentW - PAD_X - textBox.getWidth();
            float wy = y + (MODULE_H - 8f - textBox.getHeight()) / 2f;
            if (textBox.onDrag(mx, my, wx, wy)) return true;
        }
        if (mainWidget instanceof SettingSlider s && s.isDragging()) {
            float wx = x + contentW - PAD_X - s.getWidth();
            float wy = y + (MODULE_H - 8f - s.getHeight()) / 2f;
            return s.onDrag(mx, my, wx, wy);
        }
        if (expanded) {
            float sy = y + MODULE_H;
            for (SubEntry sub : subEntries) {
                if (!sub.isVisible()) continue;
                if (sub.widget instanceof SettingTextBox textBox) {
                    float wx = x + contentW - PAD_X - textBox.getWidth();
                    float wy = sy + (SUB_H - 6f - textBox.getHeight()) / 2f;
                    if (textBox.onDrag(mx, my, wx, wy)) return true;
                }
                if (sub.widget instanceof SettingSlider s && s.isDragging()) {
                    float wx = x + contentW - PAD_X - s.getWidth();
                    float wy = sy + (SUB_H - 6f - s.getHeight()) / 2f;
                    return s.onDrag(mx, my, wx, wy);
                }
                sy += SUB_H;
            }
        }
        return false;
    }

    public void releaseDrag() {
        if (mainWidget instanceof SettingSlider s) s.releaseDrag();
        for (SubEntry sub : subEntries) {
            if (sub.isVisible() && sub.widget instanceof SettingSlider s) s.releaseDrag();
        }
    }

    private int getVisibleSubCount() {
        int count = 0;
        for (SubEntry sub : subEntries) if (sub.isVisible()) count++;
        return count;
    }

    private boolean hasVisibleSubEntries() {
        for (SubEntry sub : subEntries) if (sub.isVisible()) return true;
        return false;
    }

    private static int withAlpha(int color, float alpha) {
        return ((int) (alpha * 255) << 24) | (color & 0x00FFFFFF);
    }

    private static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    private record SubEntry(String title, String subtitle, SettingWidget widget, BooleanSupplier visibleSupplier) {
        private boolean isVisible() {
            return visibleSupplier.getAsBoolean();
        }
    }
}
