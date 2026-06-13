package com.pvp_utils.client.gui.clickgui;

import com.pvp_utils.client.gui.clickgui.pages.*;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaScreen;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

public class NewSettingsScreen extends SkiaScreen {

    private final List<BasePage> pages;

    private static final String[] TAB_ICONS = {"\uE903", "\uE901", "\uE026", "\uE121", "\uE900"};
    private static final String[] TAB_ICON_FONTS = {FontRenderer.ICON, FontRenderer.ICON, FontRenderer.MATERIAL_SYMBOLS, FontRenderer.MATERIAL_SYMBOLS, FontRenderer.ICON};
    private static final String[] TAB_KEYS_ZH = {"战斗", "视觉", "工具", "优化", "其他"};
    private static final String[] TAB_KEYS_EN = {"Combat", "Visual", "Tools", "Optimize", "Other"};

    private int selectedTab = 0;
    private int hoveredTab = -1;
    private boolean closeHovered = false;
    private boolean closing = false;

    private final float[] tabHoverAlpha = new float[TAB_KEYS_ZH.length];
    private float closeHoverAlpha = 0f;
    private float indicatorY = -1f;
    private float openProgress = 0f;
    private long lastRenderMs = 0;

    private float contentScrollOffset = 0f;
    private float targetScrollOffset = 0f;
    private boolean draggingInContent = false;
    private static final float OPEN_DURATION = 0.16f;

    public NewSettingsScreen(Screen parent) {
        super(Component.literal("Settings"), parent);
        pages = List.of(
                new CombatPage(), new VisualPage(), new ToolPage(), new OptimizePage(), new OtherPage()
        );
    }

    private float[] layout(int width, int height) {
        float cardW = Math.min(width * 0.70f, 740f);
        float cardH = Math.min(height * 0.76f, 500f);
        float cardX = (width - cardW) / 2f;
        float cardY = (height - cardH) / 2f;
        float sidebarW = 190f;
        float tabStartY = cardY + 110f;
        float tabH = 38f;
        float tabGap = 2f;
        float tabW = sidebarW - 24f;
        float closeH = 34f;
        float closeY = cardY + cardH - 48f;
        float closeX = cardX + 12f;
        float contentX = cardX + sidebarW + 1f;
        float contentW = cardW - sidebarW - 1f;
        float contentY = cardY + 66f;
        float contentH = cardH - 66f - 12f;
        return new float[]{
                cardX, cardY, cardW, cardH,
                sidebarW, tabStartY, tabH, tabGap, tabW,
                closeX, closeY, closeH,
                contentX, contentY, contentW, contentH
        };
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.min(t, 1f);
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private static float easeOutCubic(float t) {
        float x = 1f - clamp01(t);
        return 1f - x * x * x;
    }

    private static int withAlpha(int color, float alpha) {
        return ((int)(alpha * 255) << 24) | (color & 0x00FFFFFF);
    }

    private static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int)(ar+(br-ar)*t) << 16) | ((int)(ag+(bg-ag)*t) << 8) | (int)(ab+(bb-ab)*t);
    }

    private float getContentTotalHeight(BasePage page) {
        float h = 54f;
        for (var m : page.getModules()) h += m.getTotalHeight() + 8f;
        h += 12f;
        return h;
    }

    @Override
    protected void drawSkia(Canvas canvas, int width, int height, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float dt = lastRenderMs == 0 ? 0.016f : Math.min((now - lastRenderMs) / 1000f, 0.033f);
        lastRenderMs = now;

        if (closing) {
            openProgress = clamp01(openProgress - dt / OPEN_DURATION);
            if (openProgress < 0.005f) {
                super.closing();
                return;
            }
        } else {
            openProgress = clamp01(openProgress + dt / OPEN_DURATION);
        }

        float animT = easeOutCubic(openProgress);

        float[] l = layout(width, height);
        BasePage currentPage = pages.get(selectedTab);
        float currentScrollAreaH = l[15] - 54f;
        float currentMaxScroll = Math.max(0f, getContentTotalHeight(currentPage) - currentScrollAreaH);
        targetScrollOffset = Math.min(targetScrollOffset, currentMaxScroll);
        contentScrollOffset = lerp(contentScrollOffset, targetScrollOffset, dt * 18f);
        float cardX = l[0], cardY = l[1], cardW = l[2], cardH = l[3];
        float sidebarW = l[4], tabStartY = l[5], tabH = l[6], tabGap = l[7], tabW = l[8];
        float closeX = l[9], closeY = l[10], closeH = l[11];
        float contentX = l[12], contentY = l[13], contentW = l[14], contentH = l[15];

        hoveredTab = -1;
        closeHovered = false;
        for (int i = 0; i < TAB_KEYS_ZH.length; i++) {
            float ty = tabStartY + i * (tabH + tabGap);
            if (mouseX >= cardX + 12f && mouseX <= cardX + 12f + tabW && mouseY >= ty && mouseY <= ty + tabH)
                hoveredTab = i;
        }
        if (mouseX >= closeX && mouseX <= closeX + tabW && mouseY >= closeY && mouseY <= closeY + closeH)
            closeHovered = true;

        for (int i = 0; i < TAB_KEYS_ZH.length; i++) {
            float target = (i == hoveredTab && i != selectedTab) ? 1f : 0f;
            tabHoverAlpha[i] = lerp(tabHoverAlpha[i], target, dt * 12f);
        }
        closeHoverAlpha = lerp(closeHoverAlpha, closeHovered ? 1f : 0f, dt * 12f);

        float targetIndicatorY = tabStartY + selectedTab * (tabH + tabGap);
        if (indicatorY < 0f) indicatorY = targetIndicatorY;
        indicatorY = lerp(indicatorY, targetIndicatorY, dt * 12f);

        BasePage page = currentPage;
        page.update(dt);

        float alpha = 1f;
        float guiScale = com.pvp_utils.client.render.skia.SkiaRenderer.getScale();
        float cx = width / 2f;
        float cy = height / 2f;
        float sc = 0.965f + 0.035f * animT;

        canvas.save();
        canvas.translate(cx * guiScale, cy * guiScale);
        canvas.scale(sc, sc);
        canvas.translate(-cx * guiScale, -cy * guiScale);

        try (Paint card = new Paint()) {
            card.setColor(withAlpha(0xF5F5F7, alpha));
            canvas.drawRRect(RRect.makeXYWH(cardX, cardY, cardW, cardH, 16f), card);
        }

        try (Paint sidebar = new Paint()) {
            sidebar.setColor(withAlpha(0xFFFFFF, alpha));
            canvas.save();
            canvas.clipRRect(RRect.makeXYWH(cardX, cardY, sidebarW, cardH, 16f));
            canvas.drawRect(Rect.makeXYWH(cardX, cardY, sidebarW, cardH), sidebar);
            canvas.restore();
        }

        try (Paint div = new Paint()) {
            div.setColor(withAlpha(0xEEEEEE, alpha));
            canvas.drawRect(Rect.makeXYWH(cardX + sidebarW, cardY + 14f, 1f, cardH - 28f), div);
        }

        FontRenderer.drawText(canvas, "PVPUtils", cardX + 18f, cardY + 38f, 16f, withAlpha(0x111111, alpha));
        FontRenderer.drawText(canvas, UiText.t("在下方调整设置...", "Adjust the settings below..."), cardX + 18f, cardY + 54f, 10f, withAlpha(0xAAAAAA, alpha));

        try (Paint indicator = new Paint()) {
            indicator.setColor(withAlpha(0xE8EEFF, alpha));
            canvas.drawRRect(RRect.makeXYWH(cardX + 12f, indicatorY, tabW, tabH, 8f), indicator);
        }

        for (int i = 0; i < TAB_KEYS_ZH.length; i++) {
            float tabY = tabStartY + i * (tabH + tabGap);
            if (tabHoverAlpha[i] > 0.01f) {
                try (Paint hover = new Paint()) {
                    hover.setColor(withAlpha(0xF3F4FF, alpha * tabHoverAlpha[i]));
                    canvas.drawRRect(RRect.makeXYWH(cardX + 12f, tabY, tabW, tabH, 8f), hover);
                }
            }
            boolean active = i == selectedTab;
            int iconColor = active ? withAlpha(0x2F54EB, alpha) : withAlpha(0x888888, alpha);
            int textColor = active ? withAlpha(0x2F54EB, alpha) : withAlpha(0x333333, alpha);
            FontRenderer.drawText(canvas, TAB_ICONS[i], cardX + 18f, tabY + tabH / 2f + 6f, 13f, iconColor, TAB_ICON_FONTS[i]);
            FontRenderer.drawText(canvas, UiText.t(TAB_KEYS_ZH[i], TAB_KEYS_EN[i]), cardX + 38f, tabY + tabH / 2f + 6f, 13f, textColor);
        }

        int closeBgColor = lerpColor(0xF0F0F0, 0xFFE5E5, closeHoverAlpha);
        int closeTextColor = lerpColor(0x666666, 0xCC2222, closeHoverAlpha);
        try (Paint closeBg = new Paint()) {
            closeBg.setColor(withAlpha(closeBgColor, alpha));
            canvas.drawRRect(RRect.makeXYWH(closeX, closeY, tabW, closeH, 8f), closeBg);
        }
        String closeText = UiText.t("× 关闭", "× Close");
        float ctw = FontRenderer.measureTextWidth(closeText, 12f);
        FontRenderer.drawText(canvas, closeText, closeX + (tabW - ctw) / 2f, closeY + 22f, 12f, withAlpha(closeTextColor, alpha));

        FontRenderer.drawText(canvas, page.getTitle(), contentX + 18f, contentY + 26f, 18f, withAlpha(0x111111, alpha));
        FontRenderer.drawText(canvas, page.getSubtitle(), contentX + 18f, contentY + 42f, 10f, withAlpha(0xAAAAAA, alpha));

        float clipTop = contentY + 54f;
        float clipBottom = contentY + contentH;
        canvas.save();
        canvas.clipRect(Rect.makeXYWH(contentX, clipTop, contentW, clipBottom - clipTop));

        float moduleStartY = contentY + 54f;
        page.draw(canvas, contentX + 10f, moduleStartY, contentW - 40f, contentH - 54f, alpha, contentScrollOffset);
        drawScrollbar(canvas, page, contentX, contentY, contentW, contentH, alpha);

        canvas.restore();

        canvas.restore();
    }

    private void drawScrollbar(Canvas canvas, BasePage page, float contentX, float contentY, float contentW, float contentH, float alpha) {
        float totalH = getContentTotalHeight(page);
        float scrollAreaH = contentH - 54f;
        if (totalH <= scrollAreaH) return;

        float trackX = contentX + contentW - 8f;
        float trackTop = contentY + 60f;
        float trackH = contentH - 60f - 8f;
        float thumbH = Math.max(20f, trackH * scrollAreaH / totalH);
        float maxScroll = Math.max(1f, totalH - scrollAreaH);
        float progress = Math.min(1f, contentScrollOffset / maxScroll);
        float thumbTop = trackTop + (trackH - thumbH) * progress;
        thumbTop = Math.min(thumbTop, trackTop + trackH - thumbH);

        try (Paint trackP = new Paint()) {
            trackP.setColor(withAlpha(0xE0E0E0, alpha * 0.5f));
            canvas.drawRRect(RRect.makeXYWH(trackX, trackTop, 4f, trackH, 2f), trackP);
        }
        try (Paint thumbP = new Paint()) {
            thumbP.setColor(withAlpha(0xBBBBBB, alpha));
            canvas.drawRRect(RRect.makeXYWH(trackX, thumbTop, 4f, thumbH, 2f), thumbP);
        }
    }

    @Override public void onClose() { closing = true; }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (closing) return false;
        double mx = event.x(), my = event.y();
        int button = event.button();

        float[] l = layout(this.width, this.height);
        float cardX = l[0];
        float sidebarW = l[4], tabStartY = l[5], tabH = l[6], tabGap = l[7], tabW = l[8];
        float closeX = l[9], closeY = l[10], closeH = l[11];
        float contentX = l[12], contentY = l[13], contentW = l[14], contentH = l[15];

        for (int i = 0; i < TAB_KEYS_ZH.length; i++) {
            float ty = tabStartY + i * (tabH + tabGap);
            if (mx >= cardX + 12f && mx <= cardX + 12f + tabW && my >= ty && my <= ty + tabH) {
                if (button == 0) { selectedTab = i; targetScrollOffset = 0f; contentScrollOffset = 0f; }
                return true;
            }
        }

        if (button == 0 && mx >= closeX && mx <= closeX + tabW && my >= closeY && my <= closeY + closeH) {
            closing = true;
            return true;
        }

        if (mx >= contentX && mx <= contentX + contentW && my >= contentY && my <= contentY + contentH) {
            BasePage page = pages.get(selectedTab);
            float moduleStartY = contentY + 54f;
            boolean hit = page.onClick((float)mx, (float)my, contentX + 10f, moduleStartY, contentW - 40f, contentScrollOffset, button);
            if (hit) {
                draggingInContent = true;
            }
            return hit;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingInContent) {
            float[] l = layout(this.width, this.height);
            float contentX = l[12], contentY = l[13], contentW = l[14];
            float moduleStartY = contentY + 54f;
            pages.get(selectedTab).onDrag((float)event.x(), (float)event.y(), contentX + 10f, moduleStartY, contentW - 40f, contentScrollOffset);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingInContent = false;
        pages.get(selectedTab).releaseDrag();
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        float[] l = layout(this.width, this.height);
        float contentX = l[12], contentY = l[13], contentW = l[14], contentH = l[15];

        if (mx >= contentX && mx <= contentX + contentW) {
            BasePage page = pages.get(selectedTab);
            float scrollAreaH2 = contentH - 54f;
            float maxScroll = Math.max(0f, getContentTotalHeight(page) - scrollAreaH2);
            targetScrollOffset = Math.max(0f, Math.min(maxScroll, targetScrollOffset + (float)(-vScroll * 16f)));
            return true;
        }
        return false;
    }
}
