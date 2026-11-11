package com.pvp_utils.client.gui.clickgui.widget;

import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;

import com.pvp_utils.client.render.chroma.ChromaDraw;

import com.pvp_utils.Config;
import com.pvp_utils.client.ModuleKeybindManager;
import com.pvp_utils.client.render.chroma.effects.ChromaShadow;
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
    private float flyoutLayoutWidth = FLYOUT_W;
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
    private long cachedTitleRevision = -1;
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
        SubEntry entry = new SubEntry(title, subtitle, widget, visibleSupplier, false);
        if (!subEntries.isEmpty() && subEntries.getLast().widget instanceof SettingToggle) {
            subEntries.getLast().children.add(entry);
        } else {
            subEntries.add(entry);
        }
        return this;
    }

    public SettingModule addSubWhen(BooleanSupplier visibleSupplier, String title, String subtitle, SettingWidget widget) {
        SubEntry entry = new SubEntry(title, subtitle, widget, visibleSupplier, false);
        if (!subEntries.isEmpty() && subEntries.getLast().widget instanceof SettingToggle) {
            subEntries.getLast().children.add(entry);
        } else {
            subEntries.add(entry);
        }
        return this;
    }

    public SettingModule addSubVisible(String title, String subtitle, SettingWidget widget, BooleanSupplier visible) {
        subEntries.add(new SubEntry(title, subtitle, widget, visible, false));
        return this;
    }

    public SettingModule addSubGroupWhen(BooleanSupplier visible, String title, String subtitle) {
        subEntries.add(new SubEntry(title, subtitle, null, visible, true));
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
        if (!open) {
            SettingTextBox.clearFocus();
            releaseDrag();
            flyoutScrollTarget = 0f;
        }
    }

    public void resetFlyout() {
        releaseDrag();
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
        boolean hasSource = flyoutSourceW > 0f && flyoutSourceH > 0f;
        float sx = hasSource ? flyoutSourceX - shiftX : x;
        float sy = hasSource ? flyoutSourceY : y;
        float sw = hasSource ? flyoutSourceW : width;
        float sh = hasSource ? flyoutSourceH : height;
        return morphRect(sx, sy, sw, sh, x, y, width, height, flyoutProgress);
    }

    private static float[] morphRect(float sx, float sy, float sw, float sh,
                                     float x, float y, float width, float height, float progress) {
        float p = cubicBezierEase(progress);
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

    public FlyoutExit captureFlyoutExit(float cardX, float cardY, float x, float y,
                                        float width, float height, float shiftX) {
        if (flyoutProgress <= 0.01f) return null;
        collectFlyoutRows(width);
        return new FlyoutExit(cardX, cardY, x, y, width, height, shiftX);
    }

    public final class FlyoutExit {
        private final float sourceX;
        private final float sourceY;
        private final float sourceW;
        private final float sourceH;
        private final float targetX;
        private final float targetY;
        private final float width;
        private final float height;
        private final float scroll;
        private final float fadeStart;
        private final List<FlyoutRow> rows;
        private float progress;

        private FlyoutExit(float cardX, float cardY, float x, float y, float width,
                           float height, float shiftX) {
            boolean hasSource = flyoutSourceW > 0f && flyoutSourceH > 0f;
            sourceX = (hasSource ? flyoutSourceX - shiftX : x) - cardX;
            sourceY = (hasSource ? flyoutSourceY : y) - cardY;
            sourceW = hasSource ? flyoutSourceW : width;
            sourceH = hasSource ? flyoutSourceH : height;
            targetX = x - cardX;
            targetY = y - cardY;
            this.width = width;
            this.height = height;
            scroll = flyoutScroll;
            progress = flyoutProgress;
            fadeStart = Math.min(0.6f, progress);
            rows = List.copyOf(flyoutRows);
        }

        public SettingModule module() {
            return SettingModule.this;
        }

        public boolean update(float dt) {
            progress = Math.max(0f, progress - dt / FLYOUT_CLOSE_SECONDS);
            return progress > 0.01f;
        }

        public float progress() {
            return progress;
        }

        public float width() {
            return width;
        }

        public float alpha() {
            float value = Math.clamp(progress / fadeStart, 0f, 1f);
            return value * value * (3f - 2f * value);
        }

        public float[] rect(float cardX, float cardY) {
            return morphRect(cardX + sourceX, cardY + sourceY, sourceW, sourceH,
                    cardX + targetX, cardY + targetY, width, height, progress);
        }

        public void draw(Canvas canvas, float cardX, float cardY, float alpha) {
            drawFlyout(canvas, cardX + targetX, cardY + targetY, width, height,
                    rect(cardX, cardY), progress, scroll, rows, alpha * alpha(),
                    Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        }
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
            if (Math.abs((!sub.group || sub.expanded ? 1f : 0f) - sub.childProgress) > 0.01f) return true;
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
            sub.childProgress += ((!sub.group || sub.expanded ? 1f : 0f) - sub.childProgress) * Math.min(1f, dt * 14f);
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
        float arrowW = hasVisibleSubEntries() ? ChromaDraw.iconWidth(ARROW_COLLAPSED, 12f, ChromaDraw.IconSet.MATERIAL_SYMBOLS) : 0f;
        float textMax = wx - 8f - keybindSpace - (arrowW > 0f ? arrowW + 4f : 0f) - (x + PAD_X);

        float ascent = ChromaDraw.textMetrics(12f).getAscent();
        float lineHeight = ChromaDraw.textMetrics(12f).getDescent() - ascent;
        ChromaDraw.text(canvas, fitTitle(title, Math.max(0f, textMax)), x + PAD_X,
                y, TILE_H, withAlpha(tc.primaryText, alpha), 12f);

        if (arrowW > 0f) {
            String arrow = flyoutOpen ? ARROW_EXPANDED : ARROW_COLLAPSED;
            float arrowX = wx - 8f - keybindSpace - arrowW;
            float iconAscent = ChromaDraw.iconMetrics(12f, ChromaDraw.IconSet.MATERIAL_SYMBOLS).getAscent();
            float iconHeight = ChromaDraw.iconMetrics(12f, ChromaDraw.IconSet.MATERIAL_SYMBOLS).getDescent() - iconAscent;
            ChromaDraw.icon(canvas, arrow, arrowX, y, TILE_H,
                    withAlpha(flyoutOpen ? tc.accent : tc.mutedText, alpha), 12f, ChromaDraw.IconSet.MATERIAL_SYMBOLS);
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
            return Math.max(110f, ChromaDraw.textWidth(UiText.t("按下任意键...", "Press any key..."), 9f) + 20f);
        }
        String keyName = ModuleKeybindManager.keyName(id);
        return keyName.isBlank() ? KEYBIND_BASE_W : Math.max(KEYBIND_BASE_W, ChromaDraw.textWidth(keyName, 9f) + 12f);
    }

    private void drawCenteredText(Canvas canvas, String text, float x, float y, float width, float size, float alpha) {
        float textW = ChromaDraw.textWidth(text, size);
        ChromaDraw.text(canvas, text, x + (width - textW) / 2f, y, KEYBIND_H, withAlpha(0xFFFFFF, alpha), size);
    }

    private void drawCenteredIcon(Canvas canvas, String icon, float x, float y, float width, float alpha) {
        float iconW = ChromaDraw.iconWidth(icon, 12f, ChromaDraw.IconSet.MATERIAL_SYMBOLS);
        ChromaDraw.icon(canvas, icon, x + (width - iconW) / 2f, y, KEYBIND_H, withAlpha(0xFFFFFF, alpha), 12f, ChromaDraw.IconSet.MATERIAL_SYMBOLS);
    }

    private String fitTitle(String text, float maxWidth) {
        long revision = ChromaDraw.textFontRevision();
        if (!text.equals(cachedTitleSource) || Math.abs(maxWidth - cachedTitleMaxWidth) > 0.5f || cachedTitleRevision != revision) {
            cachedTitleRevision = revision;
            cachedTitleSource = text;
            cachedTitleMaxWidth = maxWidth;
            cachedTitleRender = truncate(text, maxWidth, 12f);
        }
        return cachedTitleRender;
    }

    private static String truncate(String text, float maxWidth, float size) {
        if (text == null || text.isEmpty()) return "";
        if (ChromaDraw.textWidth(text, size) <= maxWidth) return text;
        String ellipsis = "\u2026";
        float ellipsisW = ChromaDraw.textWidth(ellipsis, size);
        for (int i = text.length() - 1; i > 0; i--) {
            String candidate = text.substring(0, i);
            if (ChromaDraw.textWidth(candidate, size) + ellipsisW <= maxWidth) return candidate + ellipsis;
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
        if (mainWidget != null) mainWidget.releaseDrag();
        for (SubEntry sub : subEntries) {
            if (sub.widget != null) sub.widget.releaseDrag();
            for (SubEntry child : sub.children) {
                if (child.widget != null) child.widget.releaseDrag();
            }
        }
    }

    public float preferredFlyoutWidth() {
        float width = Math.max(320f, ChromaDraw.textWidth(title, 13f) + 120f);
        for (SubEntry sub : subEntries) {
            if (!sub.isVisible()) continue;
            width = Math.max(width, preferredRowWidth(sub, 0f));
            for (SubEntry child : sub.children) {
                if (child.isVisible()) width = Math.max(width, preferredRowWidth(child, SettingRowLayout.LEVEL_OFFSET));
            }
        }
        return Math.min(620f, width);
    }

    private float preferredRowWidth(SubEntry entry, float indent) {
        float labelWidth = entry.preferredLabelWidth();
        return labelWidth + (entry.widget == null ? 24f : entry.widget.getWidth()) + 76f + indent;
    }

    public void draw(Canvas canvas, float x, float y, float width, float height, float shiftX, float alpha, float mouseX, float mouseY) {
        if (flyoutProgress <= 0.01f) return;
        collectFlyoutRows(width);
        flyoutViewportH = height;
        clampFlyoutScroll(Math.max(0f, height - FLYOUT_HEADER_H - FLYOUT_PAD));
        drawFlyout(canvas, x, y, width, height, flyoutMorphRect(x, y, width, height, shiftX),
                flyoutProgress, flyoutScroll, flyoutRows, alpha, mouseX, mouseY);
    }

    private void drawFlyout(Canvas canvas, float x, float y, float width, float height,
                            float[] rect, float animationProgress, float scroll,
                            List<FlyoutRow> rows, float alpha, float mouseX, float mouseY) {
        float progress = cubicBezierEase(animationProgress);
        ClickGuiThemeColors colors = ClickGuiThemeColors.current();
        float radius = 9f + (FLYOUT_RADIUS - 9f) * progress;
        int tileColor = isEnabled() ? lerpColor(colors.module, colors.accent, colors.dark ? 0.34f : 0.20f) : colors.module;
        int cardColor = lerpColor(tileColor, colors.content, progress);
        float bodyAlpha = ClickGuiThemeColors.panelBackgroundAlpha(alpha);
        float contentAlpha = alpha * Math.clamp((animationProgress - 0.52f) / 0.34f, 0f, 1f);
        float bodyTop = y + FLYOUT_HEADER_H;
        float bodyHeight = Math.max(0f, height - FLYOUT_HEADER_H - FLYOUT_PAD);
        if (Config.clickGuiAppearance.shadow) {
            ChromaShadow.drawRounded(canvas, rect[0], rect[1], rect[2], rect[3], radius,
                    Config.clickGuiAppearance.shadowStrength * alpha, 0xA0000000);
        }
        int cardSave = canvas.save();
        try {
            canvas.clipRRect(RRect.makeXYWH(rect[0], rect[1], rect[2], rect[3], radius), true);
            ChromaDraw.rounded(canvas, rect[0], rect[1], rect[2], rect[3], radius, withAlpha(cardColor, bodyAlpha));
            float headerHeight = Math.min(FLYOUT_HEADER_H, rect[3]);
            float closeX = rect[0] + rect[2] - 12f - CLOSE_SIZE;
            float widgetWidth = mainWidget == null ? 0f : mainWidget.getWidth() + 10f;
            ChromaDraw.text(canvas, truncate(title, Math.max(0f, closeX - rect[0] - PAD_X - widgetWidth - 10f), 13f),
                    rect[0] + PAD_X, rect[1], headerHeight, withAlpha(colors.primaryText, alpha), 13f);
            if (mainWidget != null) {
                mainWidget.draw(canvas, closeX - 10f - mainWidget.getWidth(),
                        rect[1] + (headerHeight - mainWidget.getControlHeight()) * 0.5f, alpha);
            }
            if (contentAlpha <= 0.01f) return;
            float closeY = y + (FLYOUT_HEADER_H - CLOSE_SIZE) * 0.5f;
            float closeHitX = x + width - 12f - CLOSE_SIZE;
            boolean hovered = mouseX >= closeHitX && mouseX <= closeHitX + CLOSE_SIZE
                    && mouseY >= closeY && mouseY <= closeY + CLOSE_SIZE;
            if (hovered) ChromaDraw.rounded(canvas, closeHitX, closeY, CLOSE_SIZE, CLOSE_SIZE, 5f,
                    withAlpha(colors.hoverBackground, contentAlpha));
            int closeColor = withAlpha(colors.secondaryText, contentAlpha);
            ChromaDraw.line(canvas, closeHitX + 5f, closeY + 5f, closeHitX + 13f, closeY + 13f,
                    1.4f, closeColor, PaintStrokeCap.ROUND);
            ChromaDraw.line(canvas, closeHitX + 13f, closeY + 5f, closeHitX + 5f, closeY + 13f,
                    1.4f, closeColor, PaintStrokeCap.ROUND);
            int contentSave = canvas.save();
            try {
                canvas.clipRect(Rect.makeXYWH(x + 4f, bodyTop, width - 8f, bodyHeight));
                for (FlyoutRow row : rows) {
                    SettingRowLayout.Row layout = row.layout;
                    float rowY = bodyTop + 5f + layout.y() - scroll;
                    float visibleHeight = layout.bodyHeight() * row.visibility;
                    if (rowY + visibleHeight <= bodyTop || rowY >= bodyTop + bodyHeight) continue;
                    int rowSave = canvas.save();
                    try {
                        canvas.clipRect(Rect.makeXYWH(x, rowY, width, visibleHeight));
                        float rowAlpha = contentAlpha * row.visibility;
                        ChromaDraw.rounded(canvas, x + layout.x(), rowY, layout.width(), layout.bodyHeight(), 8f,
                                withAlpha(colors.subModule, ClickGuiThemeColors.panelBackgroundAlpha(rowAlpha)));
                        if (row.depth >= 2) {
                            ChromaDraw.line(canvas, x + layout.x() - 6f, rowY + 6f,
                                    x + layout.x() - 6f, rowY + layout.bodyHeight() - 6f, 1.5f,
                                    withAlpha(colors.accent, rowAlpha * 0.48f), PaintStrokeCap.ROUND);
                        }
                        SubEntry entry = row.entry;
                        float titleTop = row.description.length == 0
                                ? rowY + (layout.headerHeight() - 18f) * 0.5f : rowY + 6f;
                        ChromaDraw.text(canvas, truncate(entry.title, layout.textWidth(), 12f), x + layout.textX(),
                                titleTop, 18f, withAlpha(colors.subModuleText, rowAlpha), 12f);
                        for (int i = 0; i < row.description.length; i++) {
                            ChromaDraw.text(canvas, row.description[i], x + layout.textX(), rowY + 24f + i * 13f,
                                    13f, withAlpha(colors.secondaryText, rowAlpha), 10f);
                        }
                        if (entry.group && entry.hasVisibleChildren()) {
                            ChromaDraw.icon(canvas, entry.expanded ? ARROW_EXPANDED : ARROW_COLLAPSED,
                                    x + layout.x() + layout.width() - 28f, rowY, layout.headerHeight(),
                                    withAlpha(colors.mutedText, rowAlpha), 12f, ChromaDraw.IconSet.MATERIAL_SYMBOLS);
                        } else if (entry.widget != null) {
                            drawRowWidget(canvas, row, x, bodyTop + 5f - scroll, rowAlpha);
                        }
                    } finally {
                        canvas.restoreToCount(rowSave);
                    }
                }
            } finally {
                canvas.restoreToCount(contentSave);
            }
        } finally {
            canvas.restoreToCount(cardSave);
        }
        float cornerX = x + width - 3f;
        float cornerY = y + height - 3f;
        float gripRadius = Math.max(2f, radius - 3f);
        try (PathBuilder builder = new PathBuilder()) {
            builder.moveTo(cornerX - 26f, cornerY);
            builder.lineTo(cornerX - gripRadius, cornerY);
            builder.arcTo(Rect.makeXYWH(cornerX - gripRadius * 2f, cornerY - gripRadius * 2f,
                    gripRadius * 2f, gripRadius * 2f), 90f, -90f, false);
            builder.lineTo(cornerX, cornerY - 26f);
            try (Path path = builder.detach()) {
                ChromaDraw.strokePath(canvas, path, 3f, withAlpha(colors.accent, alpha * 0.55f),
                        PaintStrokeCap.ROUND, PaintStrokeJoin.ROUND);
            }
        }
    }

    private void drawRowWidget(Canvas canvas, FlyoutRow row, float x, float y, float alpha) {
        SettingRowLayout.Row layout = row.layout;
        float controlX = x + layout.controlX();
        float controlY = y + layout.controlY();
        int save = canvas.save();
        try {
            canvas.translate(controlX, controlY);
            canvas.scale(layout.controlScale(), layout.controlScale());
            row.entry.widget.draw(canvas, 0f, 0f, alpha);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    public boolean flyoutClick(float mx, float my, float x, float y, float width, float height, int button) {
        if (flyoutProgress < 0.5f || button != 0) return false;
        collectFlyoutRows(width);
        float closeX = x + width - 12f - CLOSE_SIZE;
        float closeY = y + (FLYOUT_HEADER_H - CLOSE_SIZE) * 0.5f;
        if (mx >= closeX && mx <= closeX + CLOSE_SIZE && my >= closeY && my <= closeY + CLOSE_SIZE) {
            setFlyoutOpen(false);
            return true;
        }
        if (mainWidget != null) {
            float widgetX = closeX - 10f - mainWidget.getWidth();
            float widgetY = y + (FLYOUT_HEADER_H - mainWidget.getControlHeight()) * 0.5f;
            if (mx >= widgetX && mx <= widgetX + mainWidget.getWidth()
                    && my >= widgetY && my <= widgetY + mainWidget.getControlHeight()) {
                return mainWidget.onClick(mx, my, widgetX, widgetY, button);
            }
        }
        float bodyTop = y + FLYOUT_HEADER_H;
        float bodyHeight = Math.max(0f, height - FLYOUT_HEADER_H - FLYOUT_PAD);
        if (my < bodyTop || my > bodyTop + bodyHeight) return false;
        float localX = mx - x;
        float localY = my - bodyTop - 5f + flyoutScroll;
        for (FlyoutRow row : flyoutRows) {
            SettingRowLayout.Row layout = row.layout;
            if (!layout.contains(localX, localY) || localY > layout.y() + layout.bodyHeight() * row.visibility) continue;
            if (row.entry.group) {
                row.entry.expanded = !row.entry.expanded;
                return true;
            }
            if (row.entry.widget == null || !layout.containsControl(localX, localY)) return true;
            row.entry.widget.onClick((localX - layout.controlX()) / layout.controlScale(),
                    (localY - layout.controlY()) / layout.controlScale(), 0f, 0f, button);
            return true;
        }
        return false;
    }

    public boolean flyoutDrag(float mx, float my, float x, float y, float width, float height) {
        if (flyoutProgress < 0.5f) return false;
        collectFlyoutRows(width);
        float localX = mx - x;
        float localY = my - y - FLYOUT_HEADER_H - 5f + flyoutScroll;
        for (FlyoutRow row : flyoutRows) {
            SettingWidget widget = row.entry.widget;
            if (widget == null || !(widget.isDragging() || widget instanceof SettingTextBox)) continue;
            SettingRowLayout.Row layout = row.layout;
            if (widget.onDrag((localX - layout.controlX()) / layout.controlScale(),
                    (localY - layout.controlY()) / layout.controlScale(), 0f, 0f)) return true;
        }
        return false;
    }

    public void scrollFlyout(float amount) {
        collectFlyoutRows(flyoutLayoutWidth);
        float bodyHeight = Math.max(0f, flyoutViewportH - FLYOUT_HEADER_H - FLYOUT_PAD);
        float maxScroll = Math.max(0f, flyoutContentH - bodyHeight);
        flyoutScrollTarget = Math.clamp(flyoutScrollTarget + amount, 0f, maxScroll);
    }

    private void clampFlyoutScroll(float bodyHeight) {
        float maxScroll = Math.max(0f, flyoutContentH - bodyHeight);
        flyoutScrollTarget = Math.clamp(flyoutScrollTarget, 0f, maxScroll);
        flyoutScroll = Math.clamp(flyoutScroll, 0f, maxScroll);
    }

    private void collectFlyoutRows(float width) {
        flyoutLayoutWidth = width;
        flyoutRows.clear();
        float y = 0f;
        for (SubEntry sub : subEntries) {
            if (!sub.isVisible()) continue;
            y = appendFlyoutRow(sub, y, width, 1, 1f);
            float visibility = sub.group ? sub.childProgress : 1f;
            if (visibility <= 0.001f) continue;
            for (SubEntry child : sub.children) {
                if (child.isVisible()) y = appendFlyoutRow(child, y, width, 2, visibility);
            }
        }
        flyoutContentH = y + 5f;
    }

    private float appendFlyoutRow(SubEntry entry, float y, float width, int depth, float visibility) {
        SettingRowLayout.Row layout = SettingRowLayout.measure(FLYOUT_PAD, y, width - FLYOUT_PAD * 2f,
                depth - 1, entry.widget, entry.group ? 24f : 0f);
        String[] description = entry.descriptionLines(layout.textWidth());
        if (description.length > 0) {
            layout = SettingRowLayout.measure(FLYOUT_PAD, y, width - FLYOUT_PAD * 2f,
                    depth - 1, entry.widget, entry.group ? 24f : 0f, 30f + description.length * 13f);
        }
        flyoutRows.add(new FlyoutRow(entry, layout, depth, visibility, description));
        return y + layout.height() * visibility;
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

    private record FlyoutRow(SubEntry entry, SettingRowLayout.Row layout, int depth, float visibility, String[] description) {
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
        private long labelRevision = -1;
        private float labelWidth;
        private float descriptionWidth = -1f;

        private float preferredLabelWidth() {
            long revision = ChromaDraw.textFontRevision();
            if (labelRevision != revision) {
                labelRevision = revision;
                labelWidth = Math.max(ChromaDraw.textWidth(title, 12f),
                        Math.min(280f, ChromaDraw.textWidth(subtitle == null ? "" : subtitle, 10f)));
            }
            return labelWidth;
        }
        private long descriptionRevision = -1;
        private String[] description = new String[0];

        private String[] descriptionLines(float width) {
            long revision = ChromaDraw.textFontRevision();
            if (Math.abs(descriptionWidth - width) < 0.01f && descriptionRevision == revision) return description;
            descriptionWidth = width;
            descriptionRevision = revision;
            if (subtitle == null || subtitle.isBlank() || width <= 1f) return description = new String[0];
            ArrayList<String> lines = new ArrayList<>();
            int start = 0;
            while (start < subtitle.length()) {
                int end = start;
                int next = start;
                while (next < subtitle.length()) {
                    int after = next + Character.charCount(subtitle.codePointAt(next));
                    if (end > start && ChromaDraw.textWidth(subtitle.substring(start, after), 10f) > width) break;
                    end = after;
                    next = after;
                }
                lines.add(subtitle.substring(start, Math.max(start + 1, end)));
                start = Math.max(start + 1, end);
            }
            return description = lines.toArray(String[]::new);
        }

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
