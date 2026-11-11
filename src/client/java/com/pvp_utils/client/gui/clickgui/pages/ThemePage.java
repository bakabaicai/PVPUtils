package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.theme.AccentColors;
import com.pvp_utils.client.gui.clickgui.theme.ClickGuiThemeColors;
import com.pvp_utils.client.gui.clickgui.theme.UiTheme;
import com.pvp_utils.client.gui.clickgui.widget.PageTransition;
import com.pvp_utils.client.gui.clickgui.widget.SettingColorPreview;
import com.pvp_utils.client.gui.clickgui.widget.SettingSlider;
import com.pvp_utils.client.gui.clickgui.widget.SettingToggle;
import com.pvp_utils.client.render.chroma.ChromaDraw;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;

import java.util.ArrayList;
import java.util.List;

public class ThemePage extends BasePage {
    private static final float GAP = 10f;
    private static final float HEADER_HEIGHT = 44f;
    private static final float TAB_HEIGHT = 30f;
    private static final float PREVIEW_HEIGHT = 96f;
    private static final float THEME_HEIGHT = 116f;
    private static final float ACCENT_HEIGHT = 62f;
    private static final float CHIP_HEIGHT = 24f;
    private static final float CHIP_PITCH = 30f;
    private static final float PICKER_TOP = 34f;
    private static final float PADDING = 12f;
    private static final UiTheme[] THEMES = UiTheme.values();
    private static final AccentColors.Tag[] TAGS = AccentColors.Tag.values();
    private final List<AccentColors.Preset> visiblePresets = new ArrayList<>();
    private final PageTransition<Section> sections = new PageTransition<>(Section.THEME);
    private final PageTransition<PaletteTab> palettes = new PageTransition<>(
            Config.accentCustomColors ? PaletteTab.CUSTOM : PaletteTab.PRESETS);
    private final SettingToggle animated = new SettingToggle(() -> Config.accentAnimated,
            value -> { Config.accentAnimated = value; Config.save(); });
    private final SettingSlider speed = new SettingSlider(0.1, 3.0, "%.1fx", () -> (double) Config.accentSpeed,
            value -> { Config.accentSpeed = value.floatValue(); pendingSpeedSave = true; });
    private final SettingColorPreview primaryColor = new SettingColorPreview(() -> Config.accentPrimaryColor,
            value -> { Config.accentPrimaryColor = value; Config.accentCustomColors = true; }, false, Config::save);
    private final SettingColorPreview secondaryColor = new SettingColorPreview(() -> Config.accentSecondaryColor,
            value -> { Config.accentSecondaryColor = value; Config.accentCustomColors = true; }, false, Config::save);
    private AccentColors.Tag selectedTag;
    private float contentWidth = 480f;
    private float viewportHeight;
    private float tabPosition;
    private float speedExpansion = Config.accentAnimated ? 1f : 0f;
    private boolean pendingScrollReset;
    private boolean pendingSpeedSave;

    public ThemePage() {
        filterPresets();
    }

    public void prepareLayout(float width) {
        if (Float.isFinite(width) && width > 0f) contentWidth = width;
    }

    public boolean consumeScrollReset() {
        boolean reset = pendingScrollReset;
        pendingScrollReset = false;
        return reset;
    }

    private void filterPresets() {
        visiblePresets.clear();
        for (AccentColors.Preset preset : AccentColors.Preset.values()) {
            if (preset.contains(selectedTag)) visiblePresets.add(preset);
        }
    }

    private int columns() {
        return Math.clamp((int) ((contentWidth + GAP) / 150f), 1, 3);
    }

    private int tagColumns() {
        return Math.clamp((int) ((contentWidth + GAP) / 78f), 1, 6);
    }

    private float gridHeight(int count, float cardHeight) {
        int rows = (count + columns() - 1) / columns();
        return Math.max(0f, rows * (cardHeight + GAP) - GAP);
    }

    private float tagsHeight() {
        return ((TAGS.length + 1 + tagColumns() - 1) / tagColumns()) * CHIP_PITCH;
    }

    private float paletteTabsOffset() {
        return PREVIEW_HEIGHT + GAP;
    }

    private float paletteOffset() {
        return paletteTabsOffset() + TAB_HEIGHT + GAP;
    }

    private boolean sideBySidePickers() {
        return contentWidth >= 2f * (primaryColor.getWidth() + PADDING * 2f) + GAP;
    }

    private float pickerCardHeight(SettingColorPreview picker) {
        return PICKER_TOP + picker.getHeight() + PADDING;
    }

    private float paletteHeight() {
        if (palettes.displayed() == PaletteTab.PRESETS) {
            return tagsHeight() + GAP + gridHeight(visiblePresets.size(), ACCENT_HEIGHT);
        }
        float first = pickerCardHeight(primaryColor);
        float second = pickerCardHeight(secondaryColor);
        return 42f + GAP + (sideBySidePickers() ? Math.max(first, second) : first + GAP + second);
    }

    @Override
    public float getTotalHeight() {
        float body = sections.displayed() == Section.THEME
                ? PREVIEW_HEIGHT + 20f + gridHeight(THEMES.length, THEME_HEIGHT)
                : paletteOffset() + paletteHeight();
        return HEADER_HEIGHT + body;
    }

    @Override
    public void update(float dt) {
        if (sections.update(dt)) pendingScrollReset = true;
        if (palettes.update(dt)) pendingScrollReset = true;
        animated.update(dt);
        primaryColor.update(dt);
        secondaryColor.update(dt);
        float step = Float.isFinite(dt) ? Math.max(0f, dt) : 0f;
        float factor = 1f - (float) Math.exp(-20f * step);
        float target = sections.target() == Section.THEME ? 0f : 1f;
        tabPosition += (target - tabPosition) * factor;
        if (Math.abs(tabPosition - target) < 0.001f) tabPosition = target;
        float speedTarget = Config.accentAnimated ? 1f : 0f;
        speedExpansion += (speedTarget - speedExpansion) * factor;
        if (Math.abs(speedExpansion - speedTarget) < 0.001f) speedExpansion = speedTarget;
    }

    @Override
    public boolean hasAnimatingModules() {
        return !sections.isInteractive() || !palettes.isInteractive() || animated.isAnimating()
                || primaryColor.isAnimating() || secondaryColor.isAnimating()
                || Math.abs(speedExpansion - (Config.accentAnimated ? 1f : 0f)) > 0.001f;
    }

    @Override
    public void draw(Canvas canvas, float x, float y, float width, float height,
                     float alpha, float scrollOffset, float mouseX, float mouseY) {
        prepareLayout(width);
        viewportHeight = height;
        if (width <= 0f || height <= 0f) return;
        ClickGuiThemeColors colors = ClickGuiThemeColors.current();
        float bodyY = y + HEADER_HEIGHT;
        float top = bodyY - scrollOffset;
        float bodyAlpha = alpha * sections.opacity();
        float pointerX = sections.isInteractive() ? mouseX : Float.NEGATIVE_INFINITY;
        int save = canvas.save();
        try {
            canvas.clipRect(Rect.makeXYWH(x, bodyY, width, Math.max(0f, height - HEADER_HEIGHT)));
            canvas.translate(sections.offsetX(), 0f);
            drawPreview(canvas, x, top, width, bodyAlpha, colors);
            if (sections.displayed() == Section.THEME) {
                drawThemes(canvas, x, top + PREVIEW_HEIGHT + 20f, width, bodyY, y + height,
                        bodyAlpha, pointerX, mouseY, colors);
            } else {
                drawPaletteTabs(canvas, x, top + paletteTabsOffset(), width, bodyAlpha, colors);
                float paletteTop = top + paletteOffset();
                int paletteSave = canvas.save();
                try {
                    canvas.clipRect(Rect.makeXYWH(x, paletteTop, width, paletteHeight()));
                    canvas.translate(palettes.offsetX(), 0f);
                    float paletteAlpha = bodyAlpha * palettes.opacity();
                    if (palettes.displayed() == PaletteTab.CUSTOM) {
                        drawCustom(canvas, x, paletteTop, width, paletteAlpha, colors);
                    } else {
                        drawPresets(canvas, x, paletteTop, width, bodyY, y + height, paletteAlpha,
                                palettes.isInteractive() ? pointerX : Float.NEGATIVE_INFINITY, mouseY, colors);
                    }
                } finally {
                    canvas.restoreToCount(paletteSave);
                }
            }
        } finally {
            canvas.restoreToCount(save);
        }
        drawSectionTabs(canvas, x, y, width, alpha, colors);
    }

    private void drawSectionTabs(Canvas canvas, float x, float y, float width, float alpha, ClickGuiThemeColors colors) {
        float tabsWidth = Math.min(width, 232f);
        float segment = (tabsWidth - 6f) * 0.5f;
        ChromaDraw.rounded(canvas, x, y, tabsWidth, 34f, 10f, color(colors.module, alpha));
        ChromaDraw.rounded(canvas, x + 3f + segment * tabPosition, y + 3f, segment, 28f, 8f,
                color(colors.window, alpha));
        centered(canvas, UiText.t("主题", "Theme"), x + 3f, y + 3f, segment, 28f,
                colors.primaryText, alpha * (1f - 0.4f * tabPosition), 11f);
        centered(canvas, UiText.t("强调色", "Accent Color"), x + 3f + segment, y + 3f, segment, 28f,
                colors.primaryText, alpha * (0.6f + 0.4f * tabPosition), 11f);
    }

    private void drawPreview(Canvas canvas, float x, float y, float width, float alpha, ClickGuiThemeColors colors) {
        ChromaDraw.rounded(canvas, x, y, width, PREVIEW_HEIGHT, 12f, color(colors.module, alpha));
        boolean themes = sections.displayed() == Section.THEME;
        String label = themes ? UiText.t("当前主题", "Current Theme") : UiText.t("当前强调色", "Current Accent");
        String selection = themes ? UiTheme.current().label()
                : Config.accentCustomColors ? UiText.t("自定义", "Custom") : AccentColors.current().label;
        ChromaDraw.text(canvas, label, x + 14f, y + 12f, 16f, color(colors.secondaryText, alpha), 9f);
        ChromaDraw.text(canvas, selection, x + 14f, y + 32f, 24f, color(colors.primaryText, alpha), 15f);
        int first = themes ? UiTheme.current().background() : colors.accent;
        int second = themes ? UiTheme.current().panel() : AccentColors.secondary();
        ChromaDraw.gradient(canvas, x + 14f, y + 68f, 72f, 9f,
                color(first, alpha), color(second, alpha), false, 4.5f);
        if (themes) {
            float previewWidth = Math.min(210f, width * 0.46f);
            drawMiniature(canvas, x + width - previewWidth - 12f, y + 12f,
                    previewWidth, PREVIEW_HEIGHT - 24f, UiTheme.current(), colors.accent, alpha);
        } else {
            drawAnimation(canvas, x, y, width, alpha, colors);
        }
    }

    private void drawThemes(Canvas canvas, float x, float top, float width, float viewportTop, float viewportBottom,
                            float alpha, float mouseX, float mouseY, ClickGuiThemeColors colors) {
        int columns = columns();
        float cardWidth = (width - GAP * (columns - 1)) / columns;
        for (int i = 0; i < THEMES.length; i++) {
            UiTheme theme = THEMES[i];
            float cx = x + (cardWidth + GAP) * (i % columns);
            float cy = top + (THEME_HEIGHT + GAP) * (i / columns);
            if (cy + THEME_HEIGHT <= viewportTop || cy >= viewportBottom) continue;
            boolean selected = theme == UiTheme.current();
            boolean hovered = mouseY >= viewportTop && mouseY <= viewportBottom
                    && contains(mouseX, mouseY, cx, cy, cardWidth, THEME_HEIGHT);
            int base = AccentColors.mix(colors.module | 0xFF000000, colors.accent | 0xFF000000,
                    selected ? 0.14f : hovered ? 0.07f : 0f);
            ChromaDraw.rounded(canvas, cx, cy, cardWidth, THEME_HEIGHT, 10f, color(base, alpha));
            drawMiniature(canvas, cx + 7f, cy + 7f, cardWidth - 14f, 76f, theme, colors.accent, alpha);
            ChromaDraw.text(canvas, theme.label(), cx + 11f, cy + 85f, 24f,
                    color(colors.primaryText, alpha), 11f);
            if (selected) drawCheck(canvas, cx + cardWidth - 29f, cy + 89f, 16f, colors.accent, alpha);
        }
    }

    private void drawMiniature(Canvas canvas, float x, float y, float width, float height,
                               UiTheme theme, int accent, float alpha) {
        ClickGuiThemeColors colors = ClickGuiThemeColors.of(theme, accent);
        float sidebar = width * 0.22f;
        int save = canvas.save();
        try {
            canvas.clipRRect(RRect.makeXYWH(x, y, width, height, 7f), true);
            ChromaDraw.fill(canvas, x + sidebar, y, width - sidebar, height, color(colors.window, alpha));
            ChromaDraw.fill(canvas, x, y, sidebar, height, color(colors.sidebar, alpha));
            ChromaDraw.rounded(canvas, x + 7f, y + 9f, sidebar - 14f, 4f, 2f, color(colors.primaryText, alpha * 0.55f));
            for (int i = 0; i < 3; i++) {
                ChromaDraw.rounded(canvas, x + 6f, y + 25f + i * 12f, sidebar - 12f, 7f, 3f,
                        color(i == 0 ? colors.accent : colors.primaryText, alpha * (i == 0 ? 0.75f : 0.12f)));
            }
            float rowX = x + sidebar + 7f;
            float rowWidth = width - sidebar - 14f;
            for (int i = 0; i < 2; i++) {
                float rowY = y + 9f + i * 31f;
                ChromaDraw.rounded(canvas, rowX, rowY, rowWidth, 25f, 5f, color(colors.module, alpha));
                ChromaDraw.rounded(canvas, rowX + 6f, rowY + 6f, rowWidth * 0.35f, 4f, 2f,
                        color(colors.primaryText, alpha * 0.65f));
                ChromaDraw.rounded(canvas, rowX + 6f, rowY + 15f, rowWidth * 0.46f, 3f, 1.5f,
                        color(colors.secondaryText, alpha * 0.3f));
                ChromaDraw.rounded(canvas, rowX + rowWidth - 25f, rowY + 7f, 19f, 11f, 5.5f,
                        color(i == 0 ? colors.accent : colors.scrollbarTrack, alpha));
                ChromaDraw.rounded(canvas, rowX + rowWidth - (i == 0 ? 15f : 23f), rowY + 8.5f,
                        8f, 8f, 4f, color(0xFFFFFF, alpha));
            }
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void drawAnimation(Canvas canvas, float x, float y, float width, float alpha, ClickGuiThemeColors colors) {
        float controlsX = x + width - PADDING - speed.getWidth();
        ChromaDraw.text(canvas, UiText.t("动态渐变", "Animated Gradient"), controlsX, y + 12f, 24f,
                color(colors.primaryText, alpha), 10f);
        animated.draw(canvas, x + width - PADDING - animated.getWidth(), y + 12f, alpha);
        if (speedExpansion <= 0f) return;
        ChromaDraw.text(canvas, UiText.t("变化速度", "Animation Speed"), controlsX, y + 43f, 14f,
                color(colors.secondaryText, alpha * speedExpansion), 9f);
        speed.draw(canvas, controlsX, y + 63f, alpha * speedExpansion);
    }

    private void drawPaletteTabs(Canvas canvas, float x, float y, float width, float alpha, ClickGuiThemeColors colors) {
        float tabWidth = Math.min(104f, (width - GAP) * 0.5f);
        drawPaletteTab(canvas, x, y, tabWidth, UiText.t("预设", "Presets"), PaletteTab.PRESETS, alpha, colors);
        drawPaletteTab(canvas, x + tabWidth + GAP, y, tabWidth, UiText.t("自定义", "Custom"), PaletteTab.CUSTOM, alpha, colors);
    }

    private void drawPaletteTab(Canvas canvas, float x, float y, float width, String text, PaletteTab tab,
                                float alpha, ClickGuiThemeColors colors) {
        boolean selected = palettes.target() == tab;
        if (selected) ChromaDraw.rounded(canvas, x, y, width, TAB_HEIGHT, 8f, color(colors.module, alpha));
        centered(canvas, text, x, y, width, TAB_HEIGHT, selected ? colors.primaryText : colors.secondaryText, alpha, 10f);
    }

    private void drawPresets(Canvas canvas, float x, float top, float width, float viewportTop, float viewportBottom,
                             float alpha, float mouseX, float mouseY, ClickGuiThemeColors colors) {
        int tagColumns = tagColumns();
        float chipWidth = (width - GAP * (tagColumns - 1)) / tagColumns;
        for (int i = 0; i <= TAGS.length; i++) {
            AccentColors.Tag tag = i == 0 ? null : TAGS[i - 1];
            float cx = x + (chipWidth + GAP) * (i % tagColumns);
            float cy = top + (i / tagColumns) * CHIP_PITCH;
            if (cy + CHIP_HEIGHT <= viewportTop || cy >= viewportBottom) continue;
            boolean selected = selectedTag == tag;
            int chipColor = tag == null ? colors.accent : tag.color;
            int base = AccentColors.mix(colors.module | 0xFF000000, chipColor | 0xFF000000, selected ? 0.2f : 0f);
            ChromaDraw.rounded(canvas, cx, cy, chipWidth, CHIP_HEIGHT, 7f, color(base, alpha));
            ChromaDraw.rounded(canvas, cx + 7f, cy + 8f, 8f, 8f, 4f, color(chipColor, alpha));
            ChromaDraw.text(canvas, tag == null ? UiText.t("全部", "All") : tag.label(), cx + 21f, cy, CHIP_HEIGHT,
                    color(selected ? colors.primaryText : colors.secondaryText, alpha), 9f);
        }
        int columns = columns();
        float cardWidth = (width - GAP * (columns - 1)) / columns;
        float startY = top + tagsHeight() + GAP;
        for (int i = 0; i < visiblePresets.size(); i++) {
            AccentColors.Preset preset = visiblePresets.get(i);
            float cx = x + (cardWidth + GAP) * (i % columns);
            float cy = startY + (ACCENT_HEIGHT + GAP) * (i / columns);
            if (cy + ACCENT_HEIGHT <= viewportTop || cy >= viewportBottom) continue;
            boolean selected = !Config.accentCustomColors && AccentColors.current() == preset;
            boolean hovered = mouseY >= viewportTop && mouseY <= viewportBottom
                    && contains(mouseX, mouseY, cx, cy, cardWidth, ACCENT_HEIGHT);
            int first = AccentColors.sample(preset, 0f, 0f);
            int second = AccentColors.sample(preset, 0f, 50f);
            int base = AccentColors.mix(colors.module | 0xFF000000, first, selected ? 0.16f : hovered ? 0.07f : 0f);
            ChromaDraw.rounded(canvas, cx, cy, cardWidth, ACCENT_HEIGHT, 9f, color(base, alpha));
            ChromaDraw.gradient(canvas, cx + 7f, cy + 7f, cardWidth - 14f, 22f,
                    color(first, alpha), color(second, alpha), false, 6f);
            ChromaDraw.text(canvas, preset.label, cx + 10f, cy + 34f, 20f, color(colors.primaryText, alpha), 9.5f);
            if (selected) drawCheck(canvas, cx + cardWidth - 27f, cy + 10f, 16f, first, alpha);
        }
    }

    private void drawCustom(Canvas canvas, float x, float top, float width, float alpha, ClickGuiThemeColors colors) {
        ChromaDraw.text(canvas, UiText.t("自定义强调色", "Custom Accent"), x + 2f, top, 34f,
                color(colors.primaryText, alpha), 11f);
        float buttonX = x + width - 82f;
        ChromaDraw.rounded(canvas, buttonX, top + 2f, 82f, 30f, 8f, color(colors.module, alpha));
        centered(canvas, Config.accentCustomColors ? UiText.t("已应用", "Applied") : UiText.t("应用", "Apply"),
                buttonX, top + 2f, 82f, 30f, colors.primaryText, alpha, 10f);
        drawPicker(canvas, pickerLayout(x, top, width, false), primaryColor, UiText.t("主色", "Primary"), alpha, colors);
        drawPicker(canvas, pickerLayout(x, top, width, true), secondaryColor, UiText.t("次色", "Secondary"), alpha, colors);
    }

    private void drawPicker(Canvas canvas, PickerLayout layout, SettingColorPreview picker, String label,
                            float alpha, ClickGuiThemeColors colors) {
        ChromaDraw.rounded(canvas, layout.x(), layout.y(), layout.width(), pickerCardHeight(picker), 10f,
                color(colors.module, alpha));
        ChromaDraw.text(canvas, label, layout.x() + PADDING, layout.y() + 8f, 18f,
                color(colors.secondaryText, alpha), 10f);
        picker.draw(canvas, layout.pickerX(), layout.y() + PICKER_TOP, alpha);
    }

    private PickerLayout pickerLayout(float x, float top, float width, boolean secondary) {
        boolean sideBySide = sideBySidePickers();
        float cardWidth = sideBySide ? (width - GAP) * 0.5f : width;
        float cardX = x + (secondary && sideBySide ? cardWidth + GAP : 0f);
        float cardY = top + 42f + GAP
                + (secondary && !sideBySide ? pickerCardHeight(primaryColor) + GAP : 0f);
        return new PickerLayout(cardX, cardY, cardWidth,
                cardX + Math.max(PADDING, (cardWidth - primaryColor.getWidth()) * 0.5f));
    }

    @Override
    public boolean onClick(float mx, float my, float x, float y, float width, float scrollOffset, int button) {
        prepareLayout(width);
        if (mx < x || mx > x + width || my < y || my > y + viewportHeight) return false;
        float tabsWidth = Math.min(width, 232f);
        if (button == 0 && contains(mx, my, x, y, tabsWidth, 34f)) {
            releaseDrag();
            sections.request(mx < x + tabsWidth * 0.5f ? Section.THEME : Section.ACCENT);
            return true;
        }
        if (my < y + HEADER_HEIGHT) return false;
        if (!sections.isInteractive()) return true;
        float top = y + HEADER_HEIGHT - scrollOffset;
        if (sections.displayed() == Section.THEME) {
            if (button != 0) return false;
            int columns = columns();
            float cardWidth = (width - GAP * (columns - 1)) / columns;
            for (int i = 0; i < THEMES.length; i++) {
                float cx = x + (cardWidth + GAP) * (i % columns);
                float cy = top + PREVIEW_HEIGHT + 20f + (THEME_HEIGHT + GAP) * (i / columns);
                if (contains(mx, my, cx, cy, cardWidth, THEME_HEIGHT)) {
                    THEMES[i].select();
                    return true;
                }
            }
            return false;
        }
        float speedX = x + width - PADDING - speed.getWidth();
        if (button == 0 && contains(mx, my, speedX, top + 12f, speed.getWidth(), 24f)) {
            animated.toggle();
            return true;
        }
        if (Config.accentAnimated && speedExpansion > 0.95f
                && contains(mx, my, speedX, top + 63f, speed.getWidth(), speed.getHeight())) {
            return speed.onClick(mx, my, speedX, top + 63f, button);
        }
        float tabWidth = Math.min(104f, (width - GAP) * 0.5f);
        float tabY = top + paletteTabsOffset();
        if (button == 0 && contains(mx, my, x, tabY, tabWidth * 2f + GAP, TAB_HEIGHT)) {
            if (mx > x + tabWidth && mx < x + tabWidth + GAP) return false;
            releaseDrag();
            palettes.request(mx < x + tabWidth ? PaletteTab.PRESETS : PaletteTab.CUSTOM);
            return true;
        }
        if (!palettes.isInteractive()) return true;
        float paletteTop = top + paletteOffset();
        if (palettes.displayed() == PaletteTab.CUSTOM) {
            if (button == 0 && contains(mx, my, x + width - 82f, paletteTop + 2f, 82f, 30f)) {
                Config.accentCustomColors = true;
                Config.save();
                return true;
            }
            PickerLayout first = pickerLayout(x, paletteTop, width, false);
            PickerLayout second = pickerLayout(x, paletteTop, width, true);
            return primaryColor.onClick(mx, my, first.pickerX(), first.y() + PICKER_TOP, button)
                    || secondaryColor.onClick(mx, my, second.pickerX(), second.y() + PICKER_TOP, button);
        }
        if (button != 0) return false;
        int tagColumns = tagColumns();
        float chipWidth = (width - GAP * (tagColumns - 1)) / tagColumns;
        for (int i = 0; i <= TAGS.length; i++) {
            float cx = x + (chipWidth + GAP) * (i % tagColumns);
            float cy = paletteTop + (i / tagColumns) * CHIP_PITCH;
            if (contains(mx, my, cx, cy, chipWidth, CHIP_HEIGHT)) {
                AccentColors.Tag tag = i == 0 ? null : TAGS[i - 1];
                selectedTag = selectedTag == tag ? null : tag;
                filterPresets();
                return true;
            }
        }
        int columns = columns();
        float cardWidth = (width - GAP * (columns - 1)) / columns;
        for (int i = 0; i < visiblePresets.size(); i++) {
            float cx = x + (cardWidth + GAP) * (i % columns);
            float cy = paletteTop + tagsHeight() + GAP + (ACCENT_HEIGHT + GAP) * (i / columns);
            if (contains(mx, my, cx, cy, cardWidth, ACCENT_HEIGHT)) {
                AccentColors.select(visiblePresets.get(i));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onDrag(float mx, float my, float x, float y, float width, float scrollOffset) {
        prepareLayout(width);
        if (sections.displayed() != Section.ACCENT || !sections.isInteractive()) return false;
        float top = y + HEADER_HEIGHT - scrollOffset;
        if (Config.accentAnimated && speed.onDrag(mx, my, x + width - PADDING - speed.getWidth(),
                top + 63f)) return true;
        if (palettes.displayed() != PaletteTab.CUSTOM || !palettes.isInteractive()) return false;
        float paletteTop = top + paletteOffset();
        PickerLayout first = pickerLayout(x, paletteTop, width, false);
        PickerLayout second = pickerLayout(x, paletteTop, width, true);
        return primaryColor.onDrag(mx, my, first.pickerX(), first.y() + PICKER_TOP)
                || secondaryColor.onDrag(mx, my, second.pickerX(), second.y() + PICKER_TOP);
    }

    @Override
    public void releaseDrag() {
        speed.releaseDrag();
        primaryColor.releaseDrag();
        secondaryColor.releaseDrag();
        if (pendingSpeedSave) {
            pendingSpeedSave = false;
            Config.save();
        }
    }

    private static void drawCheck(Canvas canvas, float x, float y, float size, int accent, float alpha) {
        ChromaDraw.rounded(canvas, x, y, size, size, size * 0.5f, color(accent, alpha));
        int foreground = UiTheme.readableText(0xFFFFFFFF, accent);
        ChromaDraw.icon(canvas, "\uE5CA", x + 2f, y, size, color(foreground, alpha), size - 4f,
                ChromaDraw.IconSet.MATERIAL_SYMBOLS);
    }

    private static void centered(Canvas canvas, String label, float x, float y, float width, float height,
                                  int rgb, float alpha, float size) {
        ChromaDraw.text(canvas, label, x + (width - ChromaDraw.textWidth(label, size)) * 0.5f,
                y, height, color(rgb, alpha), size);
    }

    private static int color(int rgb, float alpha) {
        return AccentColors.withAlpha(rgb | 0xFF000000, alpha);
    }

    private static boolean contains(float mx, float my, float x, float y, float width, float height) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }

    private enum Section { THEME, ACCENT }
    private enum PaletteTab { PRESETS, CUSTOM }
    private record PickerLayout(float x, float y, float width, float pickerX) { }

    @Override
    public String getTitle() {
        return UiText.t("主题", "Appearance");
    }

    @Override
    public String getSubtitle() {
        return UiText.t("分别设置界面底色与强调色", "Choose surface colors and accents independently");
    }
}
