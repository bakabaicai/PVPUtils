package com.pvp_utils.client.gui;

import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaScreen;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class NewSettingsScreen extends SkiaScreen {

    private static final String[] TABS = {"战斗", "视觉", "工具", "优化", "世界", "其他"};

    private int selectedTab = 0;
    private int hoveredTab = -1;
    private boolean closeHovered = false;
    private boolean closing = false;

    private final float[] tabHoverAlpha = new float[TABS.length];
    private float closeHoverAlpha = 0f;
    private float indicatorY = -1f;
    private float openProgress = 0f;
    private long lastRenderMs = 0;

    private static final int COLOR_CARD            = 0xFFFFFFFF;
    private static final int COLOR_TAB_ACTIVE_TEXT = 0xFF2F54EB;
    private static final int COLOR_TAB_TEXT        = 0xFF333333;
    private static final int COLOR_TITLE           = 0xFF111111;
    private static final int COLOR_SUBTITLE        = 0xFF888888;
    private static final int COLOR_PLACEHOLDER     = 0xFFAAAAAA;
    private static final int COLOR_CLOSE_TEXT      = 0xFF666666;
    private static final int COLOR_DIVIDER         = 0xFFEEEEEE;

    public NewSettingsScreen(Screen parent) {
        super(Component.literal("Settings"), parent);
    }

    private float[] layout(int width, int height) {
        float cardW = Math.min(width * 0.72f, 720f);
        float cardH = Math.min(height * 0.78f, 520f);
        float cardX = (width - cardW) / 2f;
        float cardY = (height - cardH) / 2f;
        float sidebarW = cardW * 0.33f;
        float tabStartY = cardY + 100f;
        float tabH = 38f;
        float tabGap = 4f;
        float tabW = sidebarW - 28f;
        float closeH = 36f;
        float closeY = cardY + cardH - 52f;
        float closeX = cardX + 14f;
        return new float[]{cardX, cardY, cardW, cardH, sidebarW, tabStartY, tabH, tabGap, tabW, closeX, closeY, closeH};
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.min(t, 1f);
    }

    private static int withAlpha(int color, float alpha) {
        return ((int)(alpha * 255) << 24) | (color & 0x00FFFFFF);
    }

    @Override
    protected void drawSkia(Canvas canvas, int width, int height, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float dt = lastRenderMs == 0 ? 0.016f : Math.min((now - lastRenderMs) / 1000f, 0.1f);
        lastRenderMs = now;

        float speed = 12f;
        if (closing) {
            openProgress = lerp(openProgress, 0f, dt * speed);
            if (openProgress < 0.005f) {
                super.closing();
                return;
            }
        } else {
            openProgress = lerp(openProgress, 1f, dt * speed);
        }

        float[] l = layout(width, height);
        float cardX = l[0], cardY = l[1], cardW = l[2], cardH = l[3];
        float sidebarW = l[4], tabStartY = l[5], tabH = l[6], tabGap = l[7], tabW = l[8];
        float closeX = l[9], closeY = l[10], closeH = l[11];
        float radius = 16f;

        hoveredTab = -1;
        closeHovered = false;
        for (int i = 0; i < TABS.length; i++) {
            float ty = tabStartY + i * (tabH + tabGap);
            if (mouseX >= cardX + 14f && mouseX <= cardX + 14f + tabW && mouseY >= ty && mouseY <= ty + tabH) {
                hoveredTab = i;
            }
        }
        if (mouseX >= closeX && mouseX <= closeX + tabW && mouseY >= closeY && mouseY <= closeY + closeH) {
            closeHovered = true;
        }

        for (int i = 0; i < TABS.length; i++) {
            float target = (i == hoveredTab && i != selectedTab) ? 1f : 0f;
            tabHoverAlpha[i] = lerp(tabHoverAlpha[i], target, dt * speed);
        }
        closeHoverAlpha = lerp(closeHoverAlpha, closeHovered ? 1f : 0f, dt * speed);

        float targetIndicatorY = tabStartY + selectedTab * (tabH + tabGap);
        if (indicatorY < 0f) indicatorY = targetIndicatorY;
        indicatorY = lerp(indicatorY, targetIndicatorY, dt * speed);

        canvas.clear(0x00000000);

        float cx = width / 2f;
        float cy = height / 2f;
        float scale = 0.88f + 0.12f * openProgress;
        float alpha = openProgress;

        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(scale, scale);
        canvas.translate(-cx, -cy);

        try (Paint shadow = new Paint()) {
            shadow.setColor(withAlpha(0x000000, alpha * 0.12f));
            canvas.drawRRect(RRect.makeXYWH(cardX + 3, cardY + 8, cardW, cardH, radius), shadow);
        }

        try (Paint card = new Paint()) {
            card.setColor(withAlpha(0xFFFFFF, alpha));
            canvas.drawRRect(RRect.makeXYWH(cardX, cardY, cardW, cardH, radius), card);
        }

        try (Paint div = new Paint()) {
            div.setColor(withAlpha(COLOR_DIVIDER & 0xFFFFFF, alpha));
            canvas.drawRect(Rect.makeXYWH(cardX + sidebarW, cardY + 16f, 1f, cardH - 32f), div);
        }

        FontRenderer.drawText(canvas, "PVPUtils", cardX + 24f, cardY + 44f, 18f, withAlpha(COLOR_TITLE & 0xFFFFFF, alpha));
        FontRenderer.drawText(canvas, "Combat enhancement mod", cardX + 24f, cardY + 64f, 11f, withAlpha(COLOR_SUBTITLE & 0xFFFFFF, alpha));

        try (Paint indicator = new Paint()) {
            indicator.setColor(withAlpha(0xE8EEFF, alpha));
            canvas.drawRRect(RRect.makeXYWH(cardX + 14f, indicatorY, tabW, tabH, 8f), indicator);
        }

        for (int i = 0; i < TABS.length; i++) {
            float tabY = tabStartY + i * (tabH + tabGap);
            if (tabHoverAlpha[i] > 0.01f) {
                try (Paint hover = new Paint()) {
                    hover.setColor(withAlpha(0xF3F4FF, alpha * tabHoverAlpha[i]));
                    canvas.drawRRect(RRect.makeXYWH(cardX + 14f, tabY, tabW, tabH, 8f), hover);
                }
            }
            boolean active = i == selectedTab;
            int textColor = active
                    ? withAlpha(COLOR_TAB_ACTIVE_TEXT & 0xFFFFFF, alpha)
                    : withAlpha(COLOR_TAB_TEXT & 0xFFFFFF, alpha);
            FontRenderer.drawText(canvas, TABS[i], cardX + 28f, tabY + tabH / 2f + 6f, 13f, textColor);
        }

        int closeBgHex = lerpColor(0xF0F0F0, 0xE0E0E0, closeHoverAlpha);
        try (Paint closePaint = new Paint()) {
            closePaint.setColor(withAlpha(closeBgHex, alpha));
            canvas.drawRRect(RRect.makeXYWH(closeX, closeY, tabW, closeH, 8f), closePaint);
        }
        float closeTextW = FontRenderer.measureTextWidth("× 关闭", 13f);
        FontRenderer.drawText(canvas, "× 关闭", closeX + (tabW - closeTextW) / 2f, closeY + 23f, 13f, withAlpha(COLOR_CLOSE_TEXT & 0xFFFFFF, alpha));

        float contentX = cardX + sidebarW + 28f;
        float contentW = cardW - sidebarW - 28f;
        FontRenderer.drawText(canvas, TABS[selectedTab] + "设置", contentX, cardY + 36f, 20f, withAlpha(COLOR_TITLE & 0xFFFFFF, alpha));
        float phW = FontRenderer.measureTextWidth("功能正在开发中", 13f);
        FontRenderer.drawText(canvas, "功能正在开发中", contentX + (contentW - phW) / 2f, cardY + cardH / 2f + 6f, 13f, withAlpha(COLOR_PLACEHOLDER & 0xFFFFFF, alpha));

        canvas.restore();
    }

    private static int lerpColor(int a, int b, float t) {
        t = Math.min(1f, Math.max(0f, t));
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int)(ar + (br - ar) * t) << 16) | ((int)(ag + (bg - ag) * t) << 8) | (int)(ab + (bb - ab) * t);
    }

    @Override
    public void onClose() {
        closing = true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (event.button() != 0 || closing) return false;
        double mouseX = event.x();
        double mouseY = event.y();

        float[] l = layout(this.width, this.height);
        float cardX = l[0], cardY = l[1], cardW = l[2], cardH = l[3];
        float sidebarW = l[4], tabStartY = l[5], tabH = l[6], tabGap = l[7], tabW = l[8];
        float closeX = l[9], closeY = l[10], closeH = l[11];

        for (int i = 0; i < TABS.length; i++) {
            float tabY = tabStartY + i * (tabH + tabGap);
            if (mouseX >= cardX + 14f && mouseX <= cardX + 14f + tabW && mouseY >= tabY && mouseY <= tabY + tabH) {
                selectedTab = i;
                return true;
            }
        }

        if (mouseX >= closeX && mouseX <= closeX + tabW && mouseY >= closeY && mouseY <= closeY + closeH) {
            closing = true;
            return true;
        }

        return false;
    }
}