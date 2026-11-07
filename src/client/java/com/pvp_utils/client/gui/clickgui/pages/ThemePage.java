package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.NewSettingsScreen;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.theme.ClickGuiThemeColors;
import com.pvp_utils.client.gui.clickgui.theme.ClickGuiThemeManager;
import com.pvp_utils.client.gui.clickgui.theme.ThemeColors;
import com.pvp_utils.client.gui.clickgui.widget.SettingColorPreview;
import com.pvp_utils.client.gui.clickgui.widget.SettingCycle;
import com.pvp_utils.client.gui.clickgui.widget.SettingLink;
import com.pvp_utils.client.gui.clickgui.widget.SettingModule;
import com.pvp_utils.client.gui.clickgui.widget.SettingSlider;
import com.pvp_utils.client.gui.clickgui.widget.SettingToggle;
import com.pvp_utils.client.render.skia.SkijaUi;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class ThemePage extends BasePage {
    private static final float GAP = 8f;
    private static final float CARD_HEIGHT = 52f;
    private static final float TAB_HEIGHT = 28f;
    private static final float HEADER_HEIGHT = 44f;
    private static final float CHIP_HEIGHT = 22f;
    private static final float CHIP_PITCH = 28f;
    private static final float CUSTOM_SWITCH_HEIGHT = 40f;
    private static final float PICKER_TOP = 32f;
    private static final float PICKER_PADDING = 12f;
    private final List<ThemeColors.Preset> visiblePresets = new ArrayList<>();
    private final SettingToggle customColors = new SettingToggle(() -> Config.themeCustomColors,
            value -> { Config.themeCustomColors = value; Config.save(); });
    private final SettingColorPreview primaryColor = new SettingColorPreview(() -> Config.themePrimaryColor,
            value -> Config.themePrimaryColor = value, false, Config::save);
    private final SettingColorPreview secondaryColor = new SettingColorPreview(() -> Config.themeSecondaryColor,
            value -> Config.themeSecondaryColor = value, false, Config::save);
    private ThemeColors.Tag selectedTag;
    private boolean customTab = Config.themeCustomColors;
    private boolean pendingScrollReset;
    private float contentWidth = 480f;

    public ThemePage() {
        modules.add(new SettingModule(UiText.t("界面配色", "Surfaces"), "", null)
                .addSub(UiText.t("面板主题", "Panel Theme"), "",
                        new SettingLink(() -> ClickGuiThemeManager.current().displayName(), () -> {
                            Minecraft client = Minecraft.getInstance();
                            if (client.screen instanceof NewSettingsScreen screen) screen.openThemePreview();
                        }))
                .addSub(UiText.t("HUD 底色", "HUD Surface"), "",
                        new SettingCycle(List.of(UiText.t("深色", "Dark"), UiText.t("浅色", "Light")),
                                () -> Config.hudTheme == Config.HudTheme.DARK ? 0 : 1,
                                value -> { Config.hudTheme = value == 1 ? Config.HudTheme.LIGHT : Config.HudTheme.DARK; Config.save(); })));
        modules.add(new SettingModule(UiText.t("主题动画", "Theme Animation"), "", null)
                .addSub(UiText.t("动态渐变", "Animated Gradient"), "",
                        new SettingToggle(() -> Config.themeAnimated, value -> { Config.themeAnimated = value; Config.save(); }))
                .addSubWhen(() -> Config.themeAnimated, UiText.t("变化速度", "Animation Speed"), "",
                        new SettingSlider(0.1, 3.0, "%.1fx", () -> (double) Config.themeSpeed,
                                value -> { Config.themeSpeed = value.floatValue(); Config.save(); })));
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
        for (ThemeColors.Preset preset : ThemeColors.Preset.values()) {
            if (preset.contains(selectedTag)) visiblePresets.add(preset);
        }
    }

    private int columns() {
        return contentWidth < 240f ? 1 : contentWidth < 360f ? 2 : 3;
    }

    private int tagColumns() {
        return Math.clamp((int) ((contentWidth + GAP) / 82f), 1, 5);
    }

    private float presetGridOffset() {
        return ((ThemeColors.Tag.values().length + tagColumns() - 1) / tagColumns()) * CHIP_PITCH + 12f;
    }

    private boolean sideBySidePickers() {
        return contentWidth >= 2f * (primaryColor.getWidth() + PICKER_PADDING * 2f) + GAP;
    }

    private float pickerCardHeight(SettingColorPreview picker) {
        return PICKER_TOP + picker.getHeight() + PICKER_PADDING;
    }

    private float paletteHeight() {
        if (customTab) {
            float primaryHeight = pickerCardHeight(primaryColor);
            float secondaryHeight = pickerCardHeight(secondaryColor);
            return CUSTOM_SWITCH_HEIGHT + GAP + (sideBySidePickers()
                    ? Math.max(primaryHeight, secondaryHeight) : primaryHeight + GAP + secondaryHeight);
        }
        int rows = (visiblePresets.size() + columns() - 1) / columns();
        return presetGridOffset() + Math.max(0f, rows * (CARD_HEIGHT + GAP) - GAP);
    }

    @Override
    public float getTotalHeight() {
        return HEADER_HEIGHT + super.getTotalHeight() + 16f + paletteHeight();
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        customColors.update(dt);
        primaryColor.update(dt);
        secondaryColor.update(dt);
    }

    @Override
    public boolean hasAnimatingModules() {
        return super.hasAnimatingModules() || customTab && (customColors.isAnimating()
                || primaryColor.isAnimating() || secondaryColor.isAnimating());
    }

    @Override
    public void draw(Canvas canvas, float x, float y, float width, float height,
                     float alpha, float scrollOffset, float mouseX, float mouseY) {
        prepareLayout(width);
        if (width <= 0f || height <= 0f) return;
        ClickGuiThemeColors colors = ClickGuiThemeColors.current();
        float bodyY = y + HEADER_HEIGHT;
        float bodyHeight = Math.max(0f, height - HEADER_HEIGHT);
        int save = canvas.save();
        try {
            canvas.clipRect(Rect.makeXYWH(x, bodyY, width, bodyHeight));
            super.draw(canvas, x, bodyY, width, bodyHeight, alpha, scrollOffset, mouseX, mouseY);
            float top = bodyY + super.getTotalHeight() + 16f - scrollOffset;
            if (customTab) {
                drawCustom(canvas, x, top, width, alpha, colors);
            } else {
                drawPresets(canvas, x, top, width, bodyY, y + height, alpha, mouseX, mouseY, colors);
            }
        } finally {
            canvas.restoreToCount(save);
        }
        float tabWidth = (width - GAP) * 0.5f;
        drawTab(canvas, x, y, tabWidth, UiText.t("预设", "Presets"), !customTab, alpha, colors);
        drawTab(canvas, x + tabWidth + GAP, y, tabWidth, UiText.t("自定义", "Custom"), customTab, alpha, colors);
    }

    private void drawTab(Canvas canvas, float x, float y, float width, String label,
                         boolean selected, float alpha, ClickGuiThemeColors colors) {
        int background = selected ? ThemeColors.mix(colors.module | 0xFF000000,
                colors.accent | 0xFF000000, 0.24f) : colors.module | 0xFF000000;
        SkijaUi.rounded(canvas, x, y, width, TAB_HEIGHT, 7f, ThemeColors.withAlpha(background, alpha));
        float textWidth = SkijaUi.textWidth(label, 11f);
        SkijaUi.text(canvas, label, x + (width - textWidth) * 0.5f, y, TAB_HEIGHT,
                ThemeColors.withAlpha(colors.primaryText | 0xFF000000, alpha * (selected ? 1f : 0.65f)), 11f);
    }

    private void drawPresets(Canvas canvas, float x, float top, float width, float viewportTop,
                             float viewportBottom, float alpha, float mouseX, float mouseY,
                             ClickGuiThemeColors colors) {
        int tagColumns = tagColumns();
        float chipWidth = (width - GAP * (tagColumns - 1)) / tagColumns;
        ThemeColors.Tag[] tags = ThemeColors.Tag.values();
        for (int i = 0; i < tags.length; i++) {
            ThemeColors.Tag tag = tags[i];
            float cx = x + (chipWidth + GAP) * (i % tagColumns);
            float cy = top + (i / tagColumns) * CHIP_PITCH;
            if (cy + CHIP_HEIGHT <= viewportTop || cy >= viewportBottom) continue;
            boolean selected = selectedTag == tag;
            int background = selected ? ThemeColors.mix(colors.module | 0xFF000000, tag.color, 0.28f) : colors.module | 0xFF000000;
            SkijaUi.rounded(canvas, cx, cy, chipWidth, CHIP_HEIGHT, 7f, ThemeColors.withAlpha(background, alpha));
            SkijaUi.rounded(canvas, cx + 8f, cy + 7f, 8f, 8f, 4f, ThemeColors.withAlpha(tag.color, alpha));
            SkijaUi.text(canvas, tag.label(), cx + 22f, cy, CHIP_HEIGHT,
                    ThemeColors.withAlpha(colors.primaryText | 0xFF000000, alpha * (selectedTag == null || selected ? 1f : 0.65f)), 10f);
        }
        float startY = top + presetGridOffset();
        int columns = columns();
        float cardWidth = (width - (columns - 1) * GAP) / columns;
        ThemeColors.Preset selected = ThemeColors.current();
        for (int i = 0; i < visiblePresets.size(); i++) {
            ThemeColors.Preset preset = visiblePresets.get(i);
            float cx = x + (cardWidth + GAP) * (i % columns);
            float cy = startY + (CARD_HEIGHT + GAP) * (i / columns);
            if (cy + CARD_HEIGHT <= viewportTop || cy >= viewportBottom) continue;
            boolean chosen = !Config.themeCustomColors && selected == preset;
            boolean hovered = mouseY >= viewportTop && mouseY <= viewportBottom
                    && contains(mouseX, mouseY, cx, cy, cardWidth, CARD_HEIGHT);
            int first = ThemeColors.sample(preset, 0f, 0f);
            int second = ThemeColors.sample(preset, 0f, 50f);
            int surface = ThemeColors.mix(colors.module | 0xFF000000, first, chosen ? 0.22f : hovered ? 0.12f : 0f);
            SkijaUi.rounded(canvas, cx, cy, cardWidth, CARD_HEIGHT, 8f, ThemeColors.withAlpha(surface, alpha));
            SkijaUi.gradient(canvas, cx + 8f, cy + 8f, cardWidth - 16f, 16f,
                    ThemeColors.withAlpha(first, alpha), ThemeColors.withAlpha(second, alpha), false, 5f);
            SkijaUi.text(canvas, preset.label, cx + 9f, cy + 29f, 18f,
                    ThemeColors.withAlpha(colors.primaryText | 0xFF000000, alpha), 10f);
            if (chosen) {
                SkijaUi.icon(canvas, "\uE5CA", cx + cardWidth - 24f, cy + 28f, 18f,
                        ThemeColors.withAlpha(first, alpha), 12f, SkijaUi.IconSet.MATERIAL_SYMBOLS);
            }
        }
    }

    private void drawCustom(Canvas canvas, float x, float top, float width, float alpha, ClickGuiThemeColors colors) {
        SkijaUi.rounded(canvas, x, top, width, CUSTOM_SWITCH_HEIGHT, 8f,
                ThemeColors.withAlpha(colors.module | 0xFF000000, alpha));
        SkijaUi.text(canvas, UiText.t("使用自定义配色", "Use Custom Colors"), x + PICKER_PADDING,
                top, CUSTOM_SWITCH_HEIGHT, ThemeColors.withAlpha(colors.primaryText | 0xFF000000, alpha), 11f);
        customColors.draw(canvas, x + width - PICKER_PADDING - customColors.getWidth(),
                top + (CUSTOM_SWITCH_HEIGHT - customColors.getHeight()) * 0.5f, alpha);
        PickerLayout primary = pickerLayout(x, top, width, false);
        PickerLayout secondary = pickerLayout(x, top, width, true);
        drawPicker(canvas, primary, primaryColor, UiText.t("主色", "Primary Color"), alpha, colors);
        drawPicker(canvas, secondary, secondaryColor, UiText.t("次色", "Secondary Color"), alpha, colors);
    }

    private void drawPicker(Canvas canvas, PickerLayout layout, SettingColorPreview picker,
                            String label, float alpha, ClickGuiThemeColors colors) {
        SkijaUi.rounded(canvas, layout.x(), layout.y(), layout.width(), pickerCardHeight(picker), 8f,
                ThemeColors.withAlpha(colors.module | 0xFF000000, alpha));
        SkijaUi.text(canvas, label, layout.x() + PICKER_PADDING, layout.y() + 8f, 18f,
                ThemeColors.withAlpha(colors.primaryText | 0xFF000000, alpha), 10f);
        picker.draw(canvas, layout.pickerX(), layout.y() + PICKER_TOP, alpha);
    }

    private PickerLayout pickerLayout(float x, float top, float width, boolean secondary) {
        boolean sideBySide = sideBySidePickers();
        float cardWidth = sideBySide ? (width - GAP) * 0.5f : width;
        float cardX = x + (secondary && sideBySide ? cardWidth + GAP : 0f);
        float cardY = top + CUSTOM_SWITCH_HEIGHT + GAP
                + (secondary && !sideBySide ? pickerCardHeight(primaryColor) + GAP : 0f);
        return new PickerLayout(cardX, cardY, cardWidth,
                cardX + Math.max(PICKER_PADDING, (cardWidth - primaryColor.getWidth()) * 0.5f));
    }

    @Override
    public boolean onClick(float mx, float my, float x, float y, float width, float scrollOffset, int button) {
        prepareLayout(width);
        if (button == 0 && my >= y && my <= y + TAB_HEIGHT) {
            float tabWidth = (width - GAP) * 0.5f;
            if (contains(mx, my, x, y, tabWidth, TAB_HEIGHT)) return selectTab(false);
            if (contains(mx, my, x + tabWidth + GAP, y, tabWidth, TAB_HEIGHT)) return selectTab(true);
        }
        if (my < y + HEADER_HEIGHT) return false;
        float bodyY = y + HEADER_HEIGHT;
        float top = bodyY + super.getTotalHeight() + 16f - scrollOffset;
        if (my < top) return super.onClick(mx, my, x, bodyY, width, scrollOffset, button);
        if (customTab) {
            if (button == 0 && contains(mx, my, x, top, width, CUSTOM_SWITCH_HEIGHT)) {
                customColors.toggle();
                return true;
            }
            PickerLayout primary = pickerLayout(x, top, width, false);
            PickerLayout secondary = pickerLayout(x, top, width, true);
            return primaryColor.onClick(mx, my, primary.pickerX(), primary.y() + PICKER_TOP, button)
                    || secondaryColor.onClick(mx, my, secondary.pickerX(), secondary.y() + PICKER_TOP, button);
        }
        if (button != 0) return false;
        int tagColumns = tagColumns();
        float chipWidth = (width - GAP * (tagColumns - 1)) / tagColumns;
        ThemeColors.Tag[] tags = ThemeColors.Tag.values();
        for (int i = 0; i < tags.length; i++) {
            float cx = x + (chipWidth + GAP) * (i % tagColumns);
            float cy = top + (i / tagColumns) * CHIP_PITCH;
            if (contains(mx, my, cx, cy, chipWidth, CHIP_HEIGHT)) {
                selectedTag = selectedTag == tags[i] ? null : tags[i];
                filterPresets();
                pendingScrollReset = true;
                return true;
            }
        }
        int columns = columns();
        float cardWidth = (width - (columns - 1) * GAP) / columns;
        for (int i = 0; i < visiblePresets.size(); i++) {
            float cx = x + (cardWidth + GAP) * (i % columns);
            float cy = top + presetGridOffset() + (CARD_HEIGHT + GAP) * (i / columns);
            if (contains(mx, my, cx, cy, cardWidth, CARD_HEIGHT)) {
                ThemeColors.select(visiblePresets.get(i));
                return true;
            }
        }
        return false;
    }

    private boolean selectTab(boolean custom) {
        if (customTab != custom) {
            closeFlyouts();
            releaseDrag();
            customTab = custom;
            pendingScrollReset = true;
        }
        return true;
    }

    @Override
    public boolean onDrag(float mx, float my, float x, float y, float width, float scrollOffset) {
        prepareLayout(width);
        if (customTab) {
            float top = y + HEADER_HEIGHT + super.getTotalHeight() + 16f - scrollOffset;
            PickerLayout primary = pickerLayout(x, top, width, false);
            PickerLayout secondary = pickerLayout(x, top, width, true);
            if (primaryColor.onDrag(mx, my, primary.pickerX(), primary.y() + PICKER_TOP)
                    || secondaryColor.onDrag(mx, my, secondary.pickerX(), secondary.y() + PICKER_TOP)) return true;
        }
        return super.onDrag(mx, my, x, y + HEADER_HEIGHT, width, scrollOffset);
    }

    @Override
    public void releaseDrag() {
        primaryColor.releaseDrag();
        secondaryColor.releaseDrag();
        super.releaseDrag();
    }

    @Override
    public void updateFlyoutSources(float x, float y, float contentW, float scrollOffset) {
        prepareLayout(contentW);
        super.updateFlyoutSources(x, y + HEADER_HEIGHT, contentW, scrollOffset);
    }

    @Override
    public boolean openFirstFlyout(float contentX, float contentY, float contentW, float contentH, float scrollOffset) {
        prepareLayout(contentW);
        return super.openFirstFlyout(contentX, contentY + HEADER_HEIGHT, contentW,
                Math.max(0f, contentH - HEADER_HEIGHT), scrollOffset);
    }

    private static boolean contains(float mx, float my, float x, float y, float width, float height) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }

    private record PickerLayout(float x, float y, float width, float pickerX) {
    }

    @Override
    public String getTitle() {
        return UiText.t("主题", "Theme");
    }

    @Override
    public String getSubtitle() {
        return UiText.t("选择颜色标签和主题配色", "Browse color tags and accent palettes");
    }
}
