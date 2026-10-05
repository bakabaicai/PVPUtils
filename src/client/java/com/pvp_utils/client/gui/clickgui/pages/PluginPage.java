package com.pvp_utils.client.gui.clickgui.pages;

import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.theme.ClickGuiThemeColors;
import com.pvp_utils.client.gui.clickgui.widget.SettingButton;
import com.pvp_utils.client.plugin.PluginDirectory;
import com.pvp_utils.client.render.skia.SkijaUi;
import io.github.humbleui.skija.Canvas;

public final class PluginPage extends BasePage {
    private final SettingButton openDirectory = new SettingButton(
            () -> UiText.t("打开目录", "Open Directory"), PluginDirectory::open);
    private float buttonX;
    private float buttonY;

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
        float centerX = x + contentW * 0.5f;
        float top = y + Math.max(0f, (contentH - 140f) * 0.5f);
        drawCentered(canvas, "(╥_╥)", centerX, top, 44f, 0x999999, alpha);
        drawCentered(canvas, UiText.t("还未添加任何插件，点击下方添加",
                "No plugins added yet. Click below to add one."), centerX, top + 72f,
                12f, colors.secondaryText, alpha);
        buttonX = centerX - openDirectory.getWidth() * 0.5f;
        buttonY = top + 108f;
        openDirectory.draw(canvas, buttonX, buttonY, alpha);
    }

    @Override
    public boolean onClick(float mx, float my, float contentX, float contentY, float contentW,
                           float scrollOffset, int button) {
        return openDirectory.onClick(mx, my, buttonX, buttonY, button);
    }

    @Override
    public boolean hasAnimatingModules() {
        return openDirectory.isAnimating();
    }

    private static void drawCentered(Canvas canvas, String text, float centerX, float top,
                                     float size, int color, float alpha) {
        int argb = (Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f) << 24) | (color & 0xFFFFFF);
        float height = SkijaUi.textMetrics(size).getDescent() - SkijaUi.textMetrics(size).getAscent();
        SkijaUi.textWithFallback(canvas, text, centerX - SkijaUi.textWidthWithFallback(text, size) * 0.5f,
                top, height, argb, size);
    }
}
