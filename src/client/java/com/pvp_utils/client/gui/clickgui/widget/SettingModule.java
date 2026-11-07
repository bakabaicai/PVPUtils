package com.pvp_utils.client.gui.clickgui.widget;

import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;

import com.pvp_utils.client.render.skia.SkijaUi;

import com.pvp_utils.client.ModuleKeybindManager;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.theme.ClickGuiThemeColors;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintStrokeCap;
import io.github.humbleui.skija.PaintStrokeJoin;
import io.github.humbleui.skija.Path;
import io.github.humbleui.skija.PathBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

public class SettingModule {
    public static final float MODULE_H = 42f;
    public static final float FLYOUT_W = 288f;

    public final String title;
    public final String subtitle;
    public final SettingWidget mainWidget;

    private static final float TILE_GAP = 6f;
    public static final float TILE_H = MODULE_H - TILE_GAP;
    private static final float SUB_H = 44f;
    private static final float PAD_X = 16f;
    private static final float WIDGET_PAD = 12f;
    private static final float KEYBIND_H = 20f;
    private static final float KEYBIND_BASE_W = 21f;
    private static final float FLYOUT_HEADER_H = 40f;
    private static final float FLYOUT_ANIM_SECONDS = 0.34f;
    private static final float FLYOUT_CLOSE_SECONDS = 0.48f;
    private static final float FLYOUT_PAD = 8f;
    private static final float FLYOUT_RADIUS = 12f;
    private static final float CLOSE_SIZE = 18f;
    private static final String ARROW_EXPANDED = "\uE5CF";
    private static final String ARROW_COLLAPSED = "\uE5CC";
    private static final String KEYBIND_ICON = "\uE9FE";
    private static final String CLEAR_ICON = "\uF508";

    private final List<SubEntry> subEntries = new ArrayList<>();
    private final List<FlyoutRow> flyoutRows = new ArrayList<>();
    private BooleanSupplier visibleSupplier = () -> true;
    private String bindingId = "";
    private String keybindActionId = "";
    private boolean flyoutOpen;
    private float flyoutProgress;
    private float flyoutScroll;
    private float flyoutScrollTarget;
    private float flyoutContentH;
    private float flyoutViewportH = 240f;
    private float flyoutSourceX;
    private float flyoutSourceY;
    private float flyoutSourceW;
    private float flyoutSourceH;
    private float keybindWidth = KEYBIND_BASE_W;
    private float keybindHover;
    private float keybindRed;
    private String cachedTitleSource = "";
    private String cachedTitleRender = "";
    private float cachedTitleMaxWidth = -1f;
    private final Paint modulePaint = new Paint().setAntiAlias(true);
    private final Paint subPaint = new Paint().setAntiAlias(true);
    private final Paint keybindPaint = new Paint().setAntiAlias(true);
    private final Paint flyoutPaint = new Paint().setAntiAlias(true);
    private final Paint flyoutBorderPaint = new Paint().setAntiAlias(true);

    public SettingModule(String title, String subtitle, SettingWidget mainWidget) {
        this.title = title;
        this.subtitle = subtitle;
        this.mainWidget = mainWidget;
    }

    public SettingModule addSub(String title, String subtitle, SettingWidget widget) {
        subEntries.add(new SubEntry(title, subtitle, widget, () -> true, false));
        return this;
    }

    public SettingModule addSub(String title, String subtitle, SettingWidget widget, BooleanSupplier visibleSupplier) {
        subEntries.add(new SubEntry(title, subtitle, widget, visibleSupplier, false));
        return this;
    }

    public SettingModule addSubWhen(BooleanSupplier visibleSupplier, String title, String subtitle, SettingWidget widget) {
        subEntries.add(new SubEntry(title, subtitle, widget, visibleSupplier, false));
        return this;
    }

    public SettingModule addSubGroup(String title, String subtitle) {
        subEntries.add(new SubEntry(title, subtitle, null, () -> true, true));
        return this;
    }

    public SettingModule addSubChild(String title, String subtitle, SettingWidget widget) {
        if (!subEntries.isEmpty()) {
            subEntries.getLast().children.add(new SubEntry(title, subtitle, widget, () -> true, false));
        }
        return this;
    }

    public SettingModule addSubChildWhen(BooleanSupplier visibleSupplier, String title, String subtitle, SettingWidget widget) {
        if (!subEntries.isEmpty()) {
            subEntries.getLast().children.add(new SubEntry(title, subtitle, widget, visibleSupplier, false));
        }
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

    public boolean isFlyoutOpen() {
        return flyoutOpen;
    }

    public float getFlyoutProgress() {
        return flyoutProgress;
    }

    public void setFlyoutOpen(boolean open) {
        if (open && !hasVisibleSubEntries()) return;
        if (flyoutOpen == open) return;
        flyoutOpen = open;
        if (!open) flyoutScrollTarget = 0f;
    }

    public void resetFlyout() {
        flyoutOpen = false;
        flyoutProgress = 0f;
        flyoutScroll = 0f;
        flyoutScrollTarget = 0f;
    }

    public void openFlyoutInstant(float x, float y, float width, float height) {
        if (!hasVisibleSubEntries()) return;
        flyoutOpen = true;
        flyoutProgress = 1f;
        flyoutScroll = 0f;
        flyoutScrollTarget = 0f;
        setFlyoutSource(x, y, width, height);
    }

    public boolean hasFlyoutContent() {
        return hasVisibleSubEntries();
    }

    public static float cubicBezierEase(float t) {
        float target = Math.max(0f, Math.min(1f, t));
        float low = 0f;
        float high = 1f;
        float mid = target;
        for (int i = 0; i < 18; i++) {
            float x = bezierComponent(mid, 0.4f, 0.2f);
            if (Math.abs(x - target) < 0.0005f) break;
            if (x < target) low = mid;
            else high = mid;
            mid = (low + high) * 0.5f;
        }
        return bezierComponent(mid, 0f, 1f);
    }

    private static float bezierComponent(float t, float c1, float c2) {
        float inv = 1f - t;
        return 3f * inv * inv * t * c1 + 3f * inv * t * t * c2 + t * t * t;
    }

    public float[] flyoutMorphRect(float x, float y, float width, float height, float shiftX) {
        float p = cubicBezierEase(flyoutProgress);
        boolean hasSource = flyoutSourceW > 0f && flyoutSourceH > 0f;
        float sx = hasSource ? flyoutSourceX - shiftX : x;
        float sy = hasSource ? flyoutSourceY : y;
        float sw = hasSource ? flyoutSourceW : width;
        float sh = hasSource ? flyoutSourceH : height;
        return new float[]{
            sx + (x - sx) * p, sy + (y - sy) * p,
            sw + (width - sw) * p, sh + (height - sh) * p
        };
    }

    public void setFlyoutSource(float x, float y, float width, float height) {
        flyoutSourceX = x;
        flyoutSourceY = y;
        flyoutSourceW = width;
        flyoutSourceH = height;
    }

    public void toggleFlyout() {
        setFlyoutOpen(!flyoutOpen);
    }

    public boolean hasSubEntries() {
        return hasVisibleSubEntries();
    }

    private boolean isEnabled() {
        return mainWidget instanceof SettingToggle toggle && toggle.isOn();
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
            for (SubEntry child : sub.children) {
                if (UiText.matchesSearch(child.title, needle) || UiText.matchesSearch(child.subtitle, needle)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isAnimating() {
        if (Math.abs((flyoutOpen ? 1f : 0f) - flyoutProgress) > 0.01f) return true;
        if (Math.abs(flyoutScrollTarget - flyoutScroll) > 0.5f) return true;
        if (isKeybindable() && (Math.abs(keybindWidth - targetKeybindWidth()) > 0.01f || keybindHover > 0.01f || keybindRed > 0.01f)) return true;
        if (mainWidget != null && mainWidget.isAnimating()) return true;
        for (SubEntry sub : subEntries) {
            if (!sub.isVisible()) continue;
            if (Math.abs((sub.expanded ? 1f : 0f) - sub.childProgress) > 0.01f) return true;
            if (sub.widget != null && sub.widget.isAnimating()) return true;
            for (SubEntry child : sub.children) {
                if (child.isVisible() && child.widget != null && child.widget.isAnimating()) return true;
            }
        }
        return false;
    }

    public void update(float dt) {
        float flyoutStep = dt / (flyoutOpen ? FLYOUT_ANIM_SECONDS : FLYOUT_CLOSE_SECONDS);
        flyoutProgress += flyoutOpen ? flyoutStep : -flyoutStep;
        if (flyoutProgress < 0f) flyoutProgress = 0f;
        if (flyoutProgress > 1f) flyoutProgress = 1f;
        flyoutScroll += (flyoutScrollTarget - flyoutScroll) * Math.min(1f, dt * 16f);
        if (Math.abs(flyoutScrollTarget - flyoutScroll) < 0.5f) flyoutScroll = flyoutScrollTarget;
        if (isKeybindable()) keybindWidth += (targetKeybindWidth() - keybindWidth) * Math.min(1f, dt * 15f);
        if (mainWidget != null) mainWidget.update(dt);
        for (SubEntry sub : subEntries) {
            if (!sub.isVisible()) continue;
            sub.childProgress += ((sub.expanded ? 1f : 0f) - sub.childProgress) * Math.min(1f, dt * 14f);
            if (sub.childProgress < 0.001f) sub.childProgress = 0f;
            if (sub.childProgress > 0.999f) sub.childProgress = 1f;
            if (sub.widget != null) sub.widget.update(dt);
            for (SubEntry child : sub.children) {
                if (child.isVisible() && child.widget != null) child.widget.update(dt);
            }
        }
    }

    public void draw(Canvas canvas, float x, float y, float width, float alpha, float mouseX, float mouseY) {
        ModuleKeybindManager.registerModule(this);
        if (flyoutProgress > 0.001f) return;
        ClickGuiThemeColors tc = ClickGuiThemeColors.current();
        int tileColor = tc.module;
        if (isEnabled()) tileColor = lerpColor(tc.module, tc.accent, tc.dark ? 0.34f : 0.20f);
        modulePaint.setColor(withAlpha(tileColor, ClickGuiThemeColors.panelBackgroundAlpha(alpha)));
        canvas.drawRRect(RRect.makeXYWH(x, y, width, TILE_H, 9f), modulePaint);

        float widgetW = mainWidget == null ? 0f : mainWidget.getWidth();
        float widgetH = mainWidget == null ? KEYBIND_H : mainWidget.getHeight();
        float wx = x + width - WIDGET_PAD - widgetW;
        float wy = y + (TILE_H - widgetH) / 2f;
        float keybindSpace = isKeybindable() && !keybindId().isBlank() ? keybindWidth + 6f : 0f;
        float arrowW = hasVisibleSubEntries() ? SkijaUi.iconWidth(ARROW_COLLAPSED, 12f, SkijaUi.IconSet.MATERIAL_SYMBOLS) : 0f;
        float textMax = wx - 8f - keybindSpace - (arrowW > 0f ? arrowW + 4f : 0f) - (x + PAD_X);

        float ascent = SkijaUi.textMetrics(12f).getAscent();
        float lineHeight = SkijaUi.textMetrics(12f).getDescent() - ascent;
        SkijaUi.text(canvas, fitTitle(title, Math.max(0f, textMax)), x + PAD_X,
                (y + TILE_H * 0.5f + 3f) + ascent, lineHeight, withAlpha(tc.primaryText, alpha), 12f);

        if (arrowW > 0f) {
            String arrow = flyoutOpen ? ARROW_EXPANDED : ARROW_COLLAPSED;
            float arrowX = wx - 8f - keybindSpace - arrowW;
            float iconAscent = SkijaUi.iconMetrics(12f, SkijaUi.IconSet.MATERIAL_SYMBOLS).getAscent();
            float iconHeight = SkijaUi.iconMetrics(12f, SkijaUi.IconSet.MATERIAL_SYMBOLS).getDescent() - iconAscent;
            SkijaUi.icon(canvas, arrow, arrowX, (y + TILE_H * 0.5f + 5f) + iconAscent, iconHeight,
                    withAlpha(flyoutOpen ? tc.accent : tc.mutedText, alpha), 12f, SkijaUi.IconSet.MATERIAL_SYMBOLS);
        }

        drawKeybind(canvas, wx, y + (TILE_H - KEYBIND_H) / 2f, alpha, mouseX, mouseY);
        if (mainWidget != null) mainWidget.draw(canvas, wx, wy, alpha);
    }

    private void drawKeybind(Canvas canvas, float widgetX, float widgetY, float alpha, float mouseX, float mouseY) {
        String id = keybindId();
        if (!isKeybindable() || id.isBlank()) return;
        float x = widgetX - 8f - keybindWidth;
        float y = widgetY;
        boolean hovered = mouseX >= x && mouseX <= x + keybindWidth && mouseY >= y && mouseY <= y + KEYBIND_H;
        boolean bound = ModuleKeybindManager.hasBinding(id);
        boolean capturing = ModuleKeybindManager.isCapturing(id);
        keybindHover += ((hovered ? 1f : 0f) - keybindHover) * 0.2f;
        keybindRed += ((bound && hovered && !capturing && !ModuleKeybindManager.ACTION_CLICK_GUI.equals(id) ? 1f : 0f) - keybindRed) * 0.2f;
        ClickGuiThemeColors tc = ClickGuiThemeColors.current();
        int baseGray = tc.dark ? 0x777777 : 0x555555;
        int hoverGray = tc.dark ? 0x949494 : 0x777777;
        int unbindRed = tc.dark ? 0xE14D4D : 0xCC3333;
        int hoverColor = lerpColor(baseGray, hoverGray, keybindHover);
        float buttonAlpha = alpha * (0.34f + keybindHover * 0.18f + keybindRed * 0.32f);
        keybindPaint.setColor(withAlpha(lerpColor(hoverColor, unbindRed, keybindRed), buttonAlpha));
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
            return Math.max(110f, SkijaUi.textWidth(UiText.t("按下任意键...", "Press any key..."), 9f) + 20f);
        }
        String keyName = ModuleKeybindManager.keyName(id);
        return keyName.isBlank() ? KEYBIND_BASE_W : Math.max(KEYBIND_BASE_W, SkijaUi.textWidth(keyName, 9f) + 12f);
    }

    private void drawCenteredText(Canvas canvas, String text, float x, float y, float width, float size, float alpha) {
        float textW = SkijaUi.textWidth(text, size);
        SkijaUi.text(canvas, text, x + (width - textW) / 2f, (y + KEYBIND_H / 2f + 3.5f) + SkijaUi.textMetrics(size).getAscent(), SkijaUi.textMetrics(size).getDescent() - SkijaUi.textMetrics(size).getAscent(), withAlpha(0xFFFFFF, alpha), size);
    }

    private void drawCenteredIcon(Canvas canvas, String icon, float x, float y, float width, float alpha) {
        float iconW = SkijaUi.iconWidth(icon, 12f, SkijaUi.IconSet.MATERIAL_SYMBOLS);
        SkijaUi.icon(canvas, icon, x + (width - iconW) / 2f, (y + KEYBIND_H / 2f + 6.2f) + SkijaUi.iconMetrics(12f, SkijaUi.IconSet.MATERIAL_SYMBOLS).getAscent(), SkijaUi.iconMetrics(12f, SkijaUi.IconSet.MATERIAL_SYMBOLS).getDescent() - SkijaUi.iconMetrics(12f, SkijaUi.IconSet.MATERIAL_SYMBOLS).getAscent(), withAlpha(0xFFFFFF, alpha), 12f, SkijaUi.IconSet.MATERIAL_SYMBOLS);
    }

    private String fitTitle(String text, float maxWidth) {
        if (!text.equals(cachedTitleSource) || Math.abs(maxWidth - cachedTitleMaxWidth) > 0.5f) {
            cachedTitleSource = text;
            cachedTitleMaxWidth = maxWidth;
            cachedTitleRender = truncate(text, maxWidth, 12f);
        }
        return cachedTitleRender;
    }

    private static String truncate(String text, float maxWidth, float size) {
        if (text == null || text.isEmpty()) return "";
        if (SkijaUi.textWidth(text, size) <= maxWidth) return text;
        String ellipsis = "\u2026";
        float ellipsisW = SkijaUi.textWidth(ellipsis, size);
        for (int i = text.length() - 1; i > 0; i--) {
            String candidate = text.substring(0, i);
            if (SkijaUi.textWidth(candidate, size) + ellipsisW <= maxWidth) return candidate + ellipsis;
        }
        return ellipsis;
    }

    public boolean onClick(float mx, float my, float x, float y, float width, int button) {
        if (my < y || my > y + TILE_H) return false;
        String id = keybindId();
        if (button == 0 && isKeybindable() && !id.isBlank()) {
            float widgetX = x + width - WIDGET_PAD - (mainWidget == null ? 0f : mainWidget.getWidth());
            float keybindX = widgetX - 8f - keybindWidth;
            float keybindY = y + (TILE_H - KEYBIND_H) / 2f;
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
            float wx = x + width - WIDGET_PAD - mainWidget.getWidth();
            float wy = y + (TILE_H - mainWidget.getHeight()) / 2f;
            if (mx >= wx && mx <= wx + mainWidget.getWidth() && my >= wy && my <= wy + mainWidget.getHeight()) {
                return mainWidget.onClick(mx, my, wx, wy, button);
            }
        }
        if (hasVisibleSubEntries() && (button == 0 || button == 1)) {
            toggleFlyout();
            return true;
        }
        return false;
    }

    public boolean onDrag(float mx, float my, float x, float y, float width) {
        if (mainWidget instanceof SettingTextBox textBox) {
            float wx = x + width - WIDGET_PAD - textBox.getWidth();
            float wy = y + (TILE_H - textBox.getHeight()) / 2f;
            if (textBox.onDrag(mx, my, wx, wy)) return true;
        }
        if (mainWidget instanceof SettingSlider s && s.isDragging()) {
            float wx = x + width - WIDGET_PAD - s.getWidth();
            float wy = y + (TILE_H - s.getHeight()) / 2f;
            return s.onDrag(mx, my, wx, wy);
        }
        return false;
    }

    public void releaseDrag() {
        if (mainWidget instanceof SettingSlider s) s.releaseDrag();
        for (SubEntry sub : subEntries) {
            if (sub.isVisible() && sub.widget instanceof SettingSlider s) s.releaseDrag();
            for (SubEntry child : sub.children) {
                if (child.isVisible() && child.widget instanceof SettingSlider cs) cs.releaseDrag();
            }
        }
    }

    public void draw(Canvas canvas, float x, float y, float width, float height, float shiftX, float alpha, float mouseX, float mouseY) {
        if (flyoutProgress <= 0.01f) return;
        collectFlyoutRows();
        float p = cubicBezierEase(flyoutProgress);
        ClickGuiThemeColors tc = ClickGuiThemeColors.current();
        float[] rect = flyoutMorphRect(x, y, width, height, shiftX);
        float morphX = rect[0];
        float morphY = rect[1];
        float morphW = rect[2];
        float morphH = rect[3];
        float radius = 9f + (FLYOUT_RADIUS - 9f) * p;
        int tileColor = tc.module;
        if (isEnabled()) tileColor = lerpColor(tc.module, tc.accent, tc.dark ? 0.34f : 0.20f);
        int cardColor = lerpColor(tileColor, tc.content, p);
        float bodyAlpha = ClickGuiThemeColors.panelBackgroundAlpha(alpha);
        float titleAlpha = alpha;
        float a = alpha * Math.max(0f, Math.min(1f, (flyoutProgress - 0.52f) / 0.34f));
        float bodyTop = y + FLYOUT_HEADER_H;
        float bodyH = Math.max(0f, height - FLYOUT_HEADER_H - FLYOUT_PAD);
        flyoutViewportH = height;
        float maxScroll = Math.max(0f, flyoutContentH - bodyH);
        flyoutScrollTarget = Math.min(flyoutScrollTarget, maxScroll);
        flyoutScroll = Math.min(flyoutScroll, maxScroll);

        int cardSave = canvas.save();
        canvas.clipRRect(RRect.makeXYWH(morphX, morphY, morphW, morphH, radius), true);
        flyoutPaint.setColor(withAlpha(cardColor, bodyAlpha));
        canvas.drawRRect(RRect.makeXYWH(morphX, morphY, morphW, morphH, radius), flyoutPaint);
        SkijaUi.outline(canvas, morphX, morphY, morphW, morphH, radius, 1f, withAlpha(tc.border, bodyAlpha * 0.85f));
        if (titleAlpha <= 0.01f) {
            canvas.restoreToCount(cardSave);
            return;
        }

        float ascent = SkijaUi.textMetrics(13f).getAscent();
        float lineHeight = SkijaUi.textMetrics(13f).getDescent() - ascent;
        float headerH = Math.min(FLYOUT_HEADER_H, morphH);
        float headerX = morphX + PAD_X + 2f * p;
        float headerCenterY = morphY + headerH * 0.5f;
        float closeX = morphX + morphW - 12f - CLOSE_SIZE;
        SkijaUi.text(canvas, truncate(title, Math.max(0f, closeX - headerX - 10f), 13f), headerX,
                headerCenterY + 4f + ascent, lineHeight, withAlpha(tc.primaryText, titleAlpha), 13f);

        if (mainWidget != null) {
            mainWidget.draw(canvas, morphX + morphW - WIDGET_PAD - 28f * p - mainWidget.getWidth(),
                    morphY + (headerH - mainWidget.getHeight()) / 2f, alpha);
        }

        float headerContentAlpha = Math.max(0f, alpha - a);
        if (headerContentAlpha > 0.01f) {
            float widgetX = morphX + morphW - WIDGET_PAD - 28f * p - (mainWidget == null ? 0f : mainWidget.getWidth());
            float keybindSpace = isKeybindable() && !keybindId().isBlank() ? keybindWidth + 6f : 0f;
            float arrowW = hasVisibleSubEntries() ? SkijaUi.iconWidth(ARROW_COLLAPSED, 12f, SkijaUi.IconSet.MATERIAL_SYMBOLS) : 0f;
            if (arrowW > 0f) {
                float arrowX = widgetX - 8f - keybindSpace - arrowW;
                float iconAscent = SkijaUi.iconMetrics(12f, SkijaUi.IconSet.MATERIAL_SYMBOLS).getAscent();
                float iconHeight = SkijaUi.iconMetrics(12f, SkijaUi.IconSet.MATERIAL_SYMBOLS).getDescent() - iconAscent;
                SkijaUi.icon(canvas, ARROW_EXPANDED, arrowX, morphY + headerH * 0.5f + 5f + iconAscent, iconHeight,
                        withAlpha(tc.accent, headerContentAlpha), 12f, SkijaUi.IconSet.MATERIAL_SYMBOLS);
            }
            drawKeybind(canvas, widgetX, morphY + (headerH - KEYBIND_H) / 2f, headerContentAlpha, mouseX, mouseY);
        }

        if (a <= 0.01f) {
            canvas.restoreToCount(cardSave);
            return;
        }

        float closeY = y + (FLYOUT_HEADER_H - CLOSE_SIZE) / 2f + 1f;
        float closeHitX = x + width - 12f - CLOSE_SIZE;
        boolean closeHovered = mouseX >= closeHitX && mouseX <= closeHitX + CLOSE_SIZE && mouseY >= closeY && mouseY <= closeY + CLOSE_SIZE;
        if (closeHovered) {
            flyoutBorderPaint.setColor(withAlpha(tc.hoverBackground, ClickGuiThemeColors.panelBackgroundAlpha(a)));
            canvas.drawRRect(RRect.makeXYWH(closeHitX, closeY, CLOSE_SIZE, CLOSE_SIZE, 5f), flyoutBorderPaint);
        }
        float closeAscent = SkijaUi.textMetrics(13f).getAscent();
        SkijaUi.text(canvas, "\u00D7", closeHitX + CLOSE_SIZE * 0.5f - 3.5f,
                (closeY + CLOSE_SIZE * 0.5f) + closeAscent, SkijaUi.textMetrics(13f).getDescent() - closeAscent,
                withAlpha(tc.secondaryText, a), 13f);

        flyoutBorderPaint.setColor(withAlpha(tc.border, a * 0.7f));
        canvas.drawRect(Rect.makeXYWH(x + 12f, y + FLYOUT_HEADER_H - 1f, width - 24f, 1f), flyoutBorderPaint);

        int save = canvas.save();
        try {
            canvas.clipRect(Rect.makeXYWH(x + 4f, bodyTop, width - 8f, bodyH));
            float m12Ascent = SkijaUi.textMetrics(12f).getAscent();
            float m12Line = SkijaUi.textMetrics(12f).getDescent() - m12Ascent;
            float m10Ascent = SkijaUi.textMetrics(10f).getAscent();
            float m10Line = SkijaUi.textMetrics(10f).getDescent() - m10Ascent;
            for (FlyoutRow row : flyoutRows) {
                SubEntry entry = row.entry;
                float ry = bodyTop + 5f + row.y - flyoutScroll;
                if (ry + SUB_H - 6f <= bodyTop || ry >= bodyTop + bodyH) continue;
                float rowX = x + FLYOUT_PAD + row.indent;
                float rowW = width - FLYOUT_PAD * 2f - row.indent;
                subPaint.setColor(withAlpha(tc.subModule, ClickGuiThemeColors.panelBackgroundAlpha(a)));
                canvas.drawRRect(RRect.makeXYWH(rowX, ry, rowW, SUB_H - 6f, 8f), subPaint);
                float textX = rowX + PAD_X;
                if (entry.subtitle == null || entry.subtitle.isEmpty()) {
                    SkijaUi.text(canvas, truncate(entry.title, rowW - PAD_X * 2f - (entry.widget == null ? 34f : entry.widget.getWidth() + 8f), 12f),
                            textX, (ry + (SUB_H - 6f) / 2f + 2f) + m12Ascent, m12Line, withAlpha(tc.subModuleText, a), 12f);
                } else {
                    float titleMax = rowW - PAD_X * 2f - (entry.widget == null ? 34f : entry.widget.getWidth() + 8f);
                    SkijaUi.text(canvas, truncate(entry.title, titleMax, 12f), textX,
                            (ry + 15f) + m12Ascent, m12Line, withAlpha(tc.subModuleText, a), 12f);
                    SkijaUi.text(canvas, truncate(entry.subtitle, titleMax, 10f), textX,
                            (ry + 30f) + m10Ascent, m10Line, withAlpha(tc.secondaryText, a), 10f);
                }
                if (entry.group && entry.hasVisibleChildren()) {
                    String arrow = entry.childProgress > 0.5f ? ARROW_EXPANDED : ARROW_COLLAPSED;
                    float aw = SkijaUi.iconWidth(arrow, 12f, SkijaUi.IconSet.MATERIAL_SYMBOLS);
                    float iconAscent = SkijaUi.iconMetrics(12f, SkijaUi.IconSet.MATERIAL_SYMBOLS).getAscent();
                    float iconHeight = SkijaUi.iconMetrics(12f, SkijaUi.IconSet.MATERIAL_SYMBOLS).getDescent() - iconAscent;
                    SkijaUi.icon(canvas, arrow, rowX + rowW - 12f - aw, (ry + (SUB_H - 6f) / 2f) + iconAscent, iconHeight,
                            withAlpha(tc.mutedText, a), 12f, SkijaUi.IconSet.MATERIAL_SYMBOLS);
                } else if (entry.widget != null) {
                    float wxp = rowX + rowW - PAD_X - entry.widget.getWidth();
                    float wyp = ry + (SUB_H - 6f - entry.widget.getHeight()) / 2f;
                    entry.widget.draw(canvas, wxp, wyp, a);
                }
            }
        } finally {
            canvas.restoreToCount(save);
        }
        canvas.restoreToCount(cardSave);
        float gripLen = 26f;
        float gripThick = 3f;
        float gripInset = 3f;
        float gripRadius = Math.max(2f, radius - gripInset);
        int gripColor = withAlpha(tc.accent, alpha * 0.55f);
        float cornerX = x + width - gripInset;
        float cornerY = y + height - gripInset;
        try (PathBuilder builder = new PathBuilder()) {
            builder.moveTo(cornerX - gripLen, cornerY);
            builder.lineTo(cornerX - gripRadius, cornerY);
            builder.arcTo(Rect.makeXYWH(cornerX - gripRadius * 2f, cornerY - gripRadius * 2f, gripRadius * 2f, gripRadius * 2f), 90f, -90f, false);
            builder.lineTo(cornerX, cornerY - gripLen);
            try (Path path = builder.detach()) {
                SkijaUi.strokePath(canvas, path, gripThick, gripColor, PaintStrokeCap.ROUND, PaintStrokeJoin.ROUND);
            }
        }
    }

    public boolean flyoutClick(float mx, float my, float x, float y, float width, float height, int button) {
        if (flyoutProgress < 0.5f) return false;
        if (button != 0) return false;
        collectFlyoutRows();
        float closeX = x + width - 12f - CLOSE_SIZE;
        float closeY = y + (FLYOUT_HEADER_H - CLOSE_SIZE) / 2f + 1f;
        if (mx >= closeX && mx <= closeX + CLOSE_SIZE && my >= closeY && my <= closeY + CLOSE_SIZE) {
            setFlyoutOpen(false);
            return true;
        }
        if (mainWidget != null) {
            float widgetX = closeX - 10f - mainWidget.getWidth();
            float widgetY = y + (FLYOUT_HEADER_H - mainWidget.getHeight()) / 2f;
            if (mx >= widgetX && mx <= widgetX + mainWidget.getWidth() && my >= widgetY && my <= widgetY + mainWidget.getHeight()) {
                return mainWidget.onClick(mx, my, widgetX, widgetY, button);
            }
        }
        float bodyTop = y + FLYOUT_HEADER_H;
        float bodyH = Math.max(0f, height - FLYOUT_HEADER_H - FLYOUT_PAD);
        for (FlyoutRow row : flyoutRows) {
            SubEntry entry = row.entry;
            float ry = bodyTop + 5f + row.y - flyoutScroll;
            float hitTop = Math.max(ry, bodyTop);
            float hitBottom = Math.min(ry + SUB_H - 6f, bodyTop + bodyH);
            if (hitBottom <= hitTop) continue;
            if (my < hitTop || my > hitBottom) continue;
            float rowX = x + FLYOUT_PAD + row.indent;
            float rowW = width - FLYOUT_PAD * 2f - row.indent;
            if (mx < rowX || mx > rowX + rowW) continue;
            if (entry.group) {
                entry.expanded = !entry.expanded;
                return entry.hasVisibleChildren();
            }
            if (entry.widget == null) return true;
            float widgetX = rowX + rowW - PAD_X - entry.widget.getWidth();
            float widgetY = ry + (SUB_H - 6f - entry.widget.getHeight()) / 2f;
            if (mx < widgetX || mx > widgetX + entry.widget.getWidth()) return true;
            if (my < widgetY || my > widgetY + entry.widget.getHeight()) return true;
            entry.widget.onClick(mx, my, widgetX, widgetY, button);
            return true;
        }
        return false;
    }

    public boolean flyoutDrag(float mx, float my, float x, float y, float width, float height) {
        if (flyoutProgress < 0.5f) return false;
        collectFlyoutRows();
        float bodyTop = y + FLYOUT_HEADER_H;
        for (FlyoutRow row : flyoutRows) {
            SubEntry entry = row.entry;
            float ry = bodyTop + 5f + row.y - flyoutScroll;
            float rowX = x + FLYOUT_PAD + row.indent;
            float rowW = width - FLYOUT_PAD * 2f - row.indent;
            if (entry.widget instanceof SettingTextBox textBox) {
                float widgetX = rowX + rowW - PAD_X - textBox.getWidth();
                float widgetY = ry + (SUB_H - 6f - textBox.getHeight()) / 2f;
                if (textBox.onDrag(mx, my, widgetX, widgetY)) return true;
            }
            if (entry.widget instanceof SettingSlider slider && slider.isDragging()) {
                float widgetX = rowX + rowW - PAD_X - slider.getWidth();
                float widgetY = ry + (SUB_H - 6f - slider.getHeight()) / 2f;
                if (slider.onDrag(mx, my, widgetX, widgetY)) return true;
            }
        }
        return false;
    }

    public void scrollFlyout(float amount) {
        collectFlyoutRows();
        float bodyH = Math.max(0f, flyoutViewportH - FLYOUT_HEADER_H - FLYOUT_PAD);
        float maxScroll = Math.max(0f, flyoutContentH - bodyH);
        flyoutScrollTarget = Math.max(0f, Math.min(maxScroll, flyoutScrollTarget + amount));
    }

    private void collectFlyoutRows() {
        flyoutRows.clear();
        float y = 0f;
        for (SubEntry sub : subEntries) {
            if (!sub.isVisible()) continue;
            flyoutRows.add(new FlyoutRow(sub, y, 0f));
            y += SUB_H;
            if (sub.group && sub.childProgress > 0.01f) {
                for (SubEntry child : sub.children) {
                    if (!child.isVisible()) continue;
                    flyoutRows.add(new FlyoutRow(child, y, 10f));
                    y += SUB_H;
                }
            }
        }
        flyoutContentH = y;
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

    private record FlyoutRow(SubEntry entry, float y, float indent) {
    }

    private final class SubEntry {
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
            this.visibleSupplier = visibleSupplier;
            this.group = group;
        }

        private boolean isVisible() {
            return visibleSupplier.getAsBoolean();
        }

        private boolean hasVisibleChildren() {
            for (SubEntry child : children) if (child.isVisible()) return true;
            return false;
        }
    }
}
