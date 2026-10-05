package com.pvp_utils.client.gui.clickgui.widget;

import io.github.humbleui.types.RRect;

import com.pvp_utils.client.render.skia.SkijaUi;

import com.pvp_utils.client.gui.clickgui.theme.ClickGuiThemeColors;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;

import java.util.function.Supplier;

/**
 * 子项链接控件：点击打开对应界面（如主题预览界面）。
 * 显示当前值文本 + "›" 箭头，样式与 SettingCycle 一致。
 */
public class SettingLink extends SettingWidget {
    private final Supplier<String> label;
    private final Runnable action;
    private final Paint bgPaint = new Paint().setAntiAlias(true);
    private String cachedText = "";
    private float cachedTextWidth = 0f;

    public SettingLink(Supplier<String> label, Runnable action) {
        this.label = label;
        this.action = action;
    }

    @Override public float getWidth() { return 150f; }
    @Override public float getHeight() { return 24f; }

    @Override
    public void draw(Canvas canvas, float x, float y, float alpha) {
        ClickGuiThemeColors tc = ClickGuiThemeColors.current();
        bgPaint.setColor(withAlpha(tc.buttonBackground, ClickGuiThemeColors.panelBackgroundAlpha(alpha)));
        canvas.drawRRect(RRect.makeXYWH(x, y, getWidth(), getHeight(), 6f), bgPaint);
        String text = label.get() + " \u203A";
        if (!text.equals(cachedText)) {
            cachedText = text;
            cachedTextWidth = SkijaUi.textWidth(text, 12f);
        }
        SkijaUi.text(canvas, text, x + (getWidth() - cachedTextWidth) / 2f, (y + 16f) + SkijaUi.textMetrics(12f).getAscent(), SkijaUi.textMetrics(12f).getDescent() - SkijaUi.textMetrics(12f).getAscent(), withAlpha(tc.subModuleText, alpha), 12f);
    }

    @Override
    public boolean onClick(float mx, float my, float x, float y, int button) {
        if (button != 0) return false;
        action.run();
        return true;
    }
}
