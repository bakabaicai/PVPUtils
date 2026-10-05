package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.theme.ClickGuiThemeColors;
import com.pvp_utils.client.gui.clickgui.widget.SettingButton;
import com.pvp_utils.client.gui.clickgui.widget.SettingModule;
import com.pvp_utils.client.gui.clickgui.widget.SettingToggle;
import com.pvp_utils.client.gui.clickgui.widget.SettingSlider;
import com.pvp_utils.client.gui.clickgui.widget.SettingTextBox;
import com.pvp_utils.client.plugin.PluginDirectory;
import com.pvp_utils.client.plugin.PluginManager;
import com.pvp_utils.client.plugin.PluginSetting;
import com.pvp_utils.client.render.skia.SkijaUi;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.types.Rect;

public final class PluginPage extends BasePage {
    private final SettingButton openDirectory = new SettingButton(
            () -> UiText.t("打开目录", "Open Directory"), PluginDirectory::open);
    private float buttonX;
    private float buttonY;
    private final SettingButton refresh = new SettingButton(
            () -> UiText.t("刷新列表", "Refresh"), () -> PluginManager.INSTANCE.refresh());
    private float refreshX;
    private float refreshY;
    private int revision = -1;

    private void syncPlugins() {
        PluginManager manager = PluginManager.INSTANCE;
        if (revision == manager.revision()) return;
        revision = manager.revision();
        SettingTextBox.clearFocus();
        modules.clear();
        for (var plugin : manager.plugins()) {
            String subtitle = plugin.version() + (plugin.author().isBlank() ? "" : " · " + plugin.author());
            SettingModule module = new SettingModule(plugin.name(), subtitle,
                    plugin.id().startsWith("invalid:") ? null : new SettingToggle(() -> manager.isEnabled(plugin.id()),
                            enabled -> manager.setEnabled(plugin.id(), enabled)));
            module.setBindingId("plugin." + plugin.id());
            if (!plugin.description().isBlank()) {
                module.addSub(UiText.t("简介", "Description"), plugin.description(), null);
            }
            if (!plugin.error().isBlank()) {
                module.addSub(UiText.t("错误", "Error"), plugin.error(), null);
            }
            for (var setting : manager.settings(plugin.id())) {
                var widget = switch (setting.type()) {
                    case "bool" -> new SettingToggle(() -> (Boolean) setting.value(), value -> saveSetting(setting, value));
                    case "number" -> new SettingSlider(setting.min(), setting.max(), "%.2f",
                            () -> ((Number) setting.value()).doubleValue(), value -> saveSetting(setting, value));
                    case "color" -> new SettingTextBox(() -> setting.value().toString(), value -> saveSetting(setting, value), 9) {
                        private String draft = setting.value().toString();

                        @Override
                        protected String getValue() {
                            if (focused != this) draft = setting.value().toString();
                            return draft;
                        }

                        @Override
                        protected void setValue(String value) {
                            draft = value;
                            if (value.matches("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")) saveSetting(setting, value);
                        }
                    };
                    default -> new SettingTextBox(() -> setting.value().toString(), value -> saveSetting(setting, value), 512);
                };
                module.addSub(setting.name(), "", widget);
            }
            module.addSub(UiText.t("重载插件", "Reload Plugin"), plugin.id(),
                    new SettingButton(() -> UiText.t("重载", "Reload"), () -> manager.reload(plugin.id())));
            modules.add(module);
        }
        super.update(0f);
    }

    private static void saveSetting(PluginSetting setting, Object value) {
        try {
            setting.set(value);
        } catch (RuntimeException error) {
            com.pvp_utils.PVPUtils.LOGGER.error("Failed to update plugin setting {}", setting.name(), error);
            com.pvp_utils.client.modules.impl.Render.NotificationOverlay.getInstance()
                    .show(UiText.t("插件设置保存失败", "Failed to save plugin setting"));
        }
    }

    @Override
    public java.util.List<SettingModule> getModules() {
        syncPlugins();
        return super.getModules();
    }

    @Override
    public float getTotalHeight() {
        syncPlugins();
        return modules.isEmpty() ? 0f : super.getTotalHeight() + 40f;
    }

    @Override
    public void update(float dt) {
        syncPlugins();
        super.update(dt);
    }

    @Override
    public String getTitle() {
        return UiText.t("插件", "Plugins");
    }

    @Override
    public String getSubtitle() {
        return UiText.t("添加与管理插件", "Add and manage plugins");
    }

    @Override
    public void draw(Canvas canvas, float x, float y, float contentW, float contentH, float alpha,
                     float scrollOffset, float mouseX, float mouseY) {
        ClickGuiThemeColors colors = ClickGuiThemeColors.current();
        if (!modules.isEmpty()) {
            buttonX = x + 12f;
            buttonY = y + 4f;
            refreshX = buttonX + openDirectory.getWidth() + 12f;
            refreshY = buttonY;
            int save = canvas.save();
            try {
                canvas.clipRect(Rect.makeXYWH(x + 10f, y + 40f,
                        Math.max(0f, contentW - 40f), Math.max(0f, contentH - 40f)));
                super.draw(canvas, x + 10f, y + 40f, contentW - 40f, contentH - 40f,
                        alpha, scrollOffset, mouseX, mouseY);
            } finally {
                canvas.restoreToCount(save);
            }
            openDirectory.draw(canvas, buttonX, buttonY, alpha);
            refresh.draw(canvas, refreshX, refreshY, alpha);
            return;
        }
        float centerX = x + contentW * 0.5f;
        float top = y + Math.max(0f, (contentH - 140f) * 0.5f);
        drawCentered(canvas, "(╥_╥)", centerX, top, 44f, 0x999999, alpha);
        drawCentered(canvas, UiText.t("还未添加任何插件，点击下方添加",
                "No plugins added yet. Click below to add one."), centerX, top + 72f,
                12f, colors.secondaryText, alpha);
        buttonX = centerX - openDirectory.getWidth() * 0.5f;
        buttonY = top + 108f;
        openDirectory.draw(canvas, buttonX, buttonY, alpha);
        refreshX = centerX - refresh.getWidth() * 0.5f;
        refreshY = buttonY + 36f;
        refresh.draw(canvas, refreshX, refreshY, alpha);
    }

    @Override
    public boolean onClick(float mx, float my, float contentX, float contentY, float contentW,
                           float scrollOffset, int button) {
        if (openDirectory.onClick(mx, my, buttonX, buttonY, button)
                || refresh.onClick(mx, my, refreshX, refreshY, button)) return true;
        return !modules.isEmpty() && my >= contentY + 40f && super.onClick(mx, my, contentX, contentY + 40f, contentW,
                scrollOffset, button);
    }

    @Override
    public boolean onDrag(float mx, float my, float contentX, float contentY, float contentW, float scrollOffset) {
        return !modules.isEmpty() && my >= contentY + 40f
                && super.onDrag(mx, my, contentX, contentY + 40f, contentW, scrollOffset);
    }

    @Override
    public boolean hasAnimatingModules() {
        return openDirectory.isAnimating() || refresh.isAnimating() || super.hasAnimatingModules();
    }

    private static void drawCentered(Canvas canvas, String text, float centerX, float top,
                                     float size, int color, float alpha) {
        int argb = (Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f) << 24) | (color & 0xFFFFFF);
        float height = SkijaUi.textMetrics(size).getDescent() - SkijaUi.textMetrics(size).getAscent();
        SkijaUi.textWithFallback(canvas, text, centerX - SkijaUi.textWidthWithFallback(text, size) * 0.5f,
                top, height, argb, size);
    }
}
