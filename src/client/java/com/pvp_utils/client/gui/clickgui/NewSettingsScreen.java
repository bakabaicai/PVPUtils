package com.pvp_utils.client.gui.clickgui;

import com.pvp_utils.client.gui.clickgui.pages.*;
import com.pvp_utils.client.ResetManager;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaScreen;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class NewSettingsScreen extends SkiaScreen {

    private final List<BasePage> pages;

    private static final String[] TAB_ICONS = {"\uE903", "\uE901", "\uE026", "\uE121", "\uE900"};
    private static final String[] TAB_ICON_FONTS = {FontRenderer.ICON, FontRenderer.ICON, FontRenderer.MATERIAL_SYMBOLS, FontRenderer.MATERIAL_SYMBOLS, FontRenderer.ICON};
    private static final String[] TAB_KEYS_ZH = {"战斗", "视觉", "工具", "优化", "其他"};
    private static final String[] TAB_KEYS_EN = {"Combat", "Render", "Tools", "Optimize", "Misc"};

    private int selectedTab = 0;
    private int hoveredTab = -1;
    private boolean closeHovered = false;
    private boolean resetHovered = false;
    private boolean resetConfirm = false;
    private boolean closing = false;

    private final float[] tabHoverAlpha = new float[TAB_KEYS_ZH.length];
    private float closeHoverAlpha = 0f;
    private float resetHoverAlpha = 0f;
    private float indicatorY = -1f;
    private float openProgress = 0f;
    private long lastRenderMs = 0;

    private float contentScrollOffset = 0f;
    private float targetScrollOffset = 0f;
    private boolean draggingInContent = false;
    private boolean draggingScrollbar = false;
    private float scrollbarDragOffset = 0f;
    private static final float OPEN_DURATION = 0.16f;
    private static final float BASE_CARD_W = 740f;
    private static final float BASE_CARD_H = 500f;
    private static final float SCREEN_MARGIN = 24f;

    public NewSettingsScreen(Screen parent) {
        super(Component.literal("Settings"), parent);
        pages = new ArrayList<>(List.of(new CombatPage(), new VisualPage(), new ToolPage(), new OptimizePage(), new OtherPage()));
    }

    private float[] layout(int width, int height) {
        float cardW = BASE_CARD_W;
        float cardH = BASE_CARD_H;
        float cardX = (width - cardW) / 2f;
        float cardY = (height - cardH) / 2f;
        float sidebarW = 190f;
        float tabStartY = cardY + 110f;
        float tabH = 38f;
        float tabGap = 2f;
        float tabW = sidebarW - 24f;
        float closeH = 34f;
        float resetH = 34f;
        float closeY = cardY + cardH - 48f;
        float resetY = closeY - resetH - 8f;
        float closeX = cardX + 12f;
        float contentX = cardX + sidebarW + 1f;
        float contentW = cardW - sidebarW - 1f;
        float contentY = cardY + 66f;
        float contentH = cardH - 66f - 12f;
        return new float[]{
                cardX, cardY, cardW, cardH,
                sidebarW, tabStartY, tabH, tabGap, tabW,
                closeX, closeY, closeH, resetY, resetH,
                contentX, contentY, contentW, contentH
        };
    }

    private float getUiScale(int width, int height) {
        float scaleX = Math.max(0.1f, (width - SCREEN_MARGIN) / BASE_CARD_W);
        float scaleY = Math.max(0.1f, (height - SCREEN_MARGIN) / BASE_CARD_H);
        return Math.min(1f, Math.min(scaleX, scaleY));
    }

    private float getVisualScale(int width, int height) {
        return getUiScale(width, height) * (0.965f + 0.035f * easeOutCubic(openProgress));
    }

    private float toLayoutX(double x, int width, float scale) {
        float cx = width / 2f;
        return cx + ((float) x - cx) / scale;
    }

    private float toLayoutY(double y, int height, float scale) {
        float cy = height / 2f;
        return cy + ((float) y - cy) / scale;
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

        float visualScale = getUiScale(width, height) * (0.965f + 0.035f * animT);
        float layoutMouseX = toLayoutX(mouseX, width, visualScale);
        float layoutMouseY = toLayoutY(mouseY, height, visualScale);
        float[] l = layout(width, height);
        BasePage currentPage = pages.get(selectedTab);
        float currentScrollAreaH = l[17] - 54f;
        float currentMaxScroll = Math.max(0f, getContentTotalHeight(currentPage) - currentScrollAreaH);
        targetScrollOffset = Math.min(targetScrollOffset, currentMaxScroll);
        contentScrollOffset = lerp(contentScrollOffset, targetScrollOffset, dt * 18f);
        float cardX = l[0], cardY = l[1], cardW = l[2], cardH = l[3];
        float sidebarW = l[4], tabStartY = l[5], tabH = l[6], tabGap = l[7], tabW = l[8];
        float closeX = l[9], closeY = l[10], closeH = l[11], resetY = l[12], resetH = l[13];
        float contentX = l[14], contentY = l[15], contentW = l[16], contentH = l[17];

        hoveredTab = -1;
        closeHovered = false;
        resetHovered = false;
        for (int i = 0; i < TAB_KEYS_ZH.length; i++) {
            float ty = tabStartY + i * (tabH + tabGap);
            if (layoutMouseX >= cardX + 12f && layoutMouseX <= cardX + 12f + tabW && layoutMouseY >= ty && layoutMouseY <= ty + tabH)
                hoveredTab = i;
        }
        if (layoutMouseX >= closeX && layoutMouseX <= closeX + tabW && layoutMouseY >= closeY && layoutMouseY <= closeY + closeH)
            closeHovered = true;
        if (layoutMouseX >= closeX && layoutMouseX <= closeX + tabW && layoutMouseY >= resetY && layoutMouseY <= resetY + resetH)
            resetHovered = true;

        for (int i = 0; i < TAB_KEYS_ZH.length; i++) {
            float target = (i == hoveredTab && i != selectedTab) ? 1f : 0f;
            tabHoverAlpha[i] = lerp(tabHoverAlpha[i], target, dt * 12f);
        }
        closeHoverAlpha = lerp(closeHoverAlpha, closeHovered ? 1f : 0f, dt * 12f);
        resetHoverAlpha = lerp(resetHoverAlpha, resetHovered ? 1f : 0f, dt * 12f);

        float targetIndicatorY = tabStartY + selectedTab * (tabH + tabGap);
        if (indicatorY < 0f) indicatorY = targetIndicatorY;
        indicatorY = lerp(indicatorY, targetIndicatorY, dt * 12f);

        BasePage page = currentPage;
        page.update(dt);

        float alpha = 1f;
        float cx = width / 2f;
        float cy = height / 2f;

        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(visualScale, visualScale);
        canvas.translate(-cx, -cy);

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
        int resetBgColor = lerpColor(0xF0F0F0, 0xFFE0E0, resetHoverAlpha);
        int resetTextColor = lerpColor(0x666666, 0xCC1111, resetHoverAlpha);
        try (Paint resetBg = new Paint()) {
            resetBg.setColor(withAlpha(resetBgColor, alpha));
            canvas.drawRRect(RRect.makeXYWH(closeX, resetY, tabW, resetH, 8f), resetBg);
        }
        String resetText = resetConfirm ? UiText.t("再次点击以确认", "Click Again to Confirm") : UiText.t("重置所有设置", "Reset All Settings");
        String resetIcon = "\uE042";
        float riw = FontRenderer.measureTextWidth(resetIcon, 13f, FontRenderer.MATERIAL_SYMBOLS);
        float rtw = FontRenderer.measureTextWidth(resetText, 12f);
        float resetTotalW = riw + 6f + rtw;
        float resetStartX = closeX + (tabW - resetTotalW) / 2f;
        FontRenderer.drawText(canvas, resetIcon, resetStartX, resetY + 22f, 13f, withAlpha(resetTextColor, alpha), FontRenderer.MATERIAL_SYMBOLS);
        FontRenderer.drawText(canvas, resetText, resetStartX + riw + 6f, resetY + 22f, 12f, withAlpha(resetTextColor, alpha));

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

    private boolean hasScrollbar(BasePage page, float contentH) {
        return getContentTotalHeight(page) > contentH - 54f;
    }

    private float scrollbarTrackX(float contentX, float contentW) {
        return contentX + contentW - 8f;
    }

    private float scrollbarTrackTop(float contentY) {
        return contentY + 60f;
    }

    private float scrollbarTrackH(float contentH) {
        return contentH - 60f - 8f;
    }

    private float scrollbarThumbH(BasePage page, float contentH) {
        float totalH = getContentTotalHeight(page);
        float scrollAreaH = contentH - 54f;
        return Math.max(20f, scrollbarTrackH(contentH) * scrollAreaH / totalH);
    }

    private float scrollbarThumbTop(BasePage page, float contentY, float contentH) {
        float totalH = getContentTotalHeight(page);
        float scrollAreaH = contentH - 54f;
        float trackTop = scrollbarTrackTop(contentY);
        float trackH = scrollbarTrackH(contentH);
        float thumbH = scrollbarThumbH(page, contentH);
        float maxScroll = Math.max(1f, totalH - scrollAreaH);
        float progress = Math.min(1f, targetScrollOffset / maxScroll);
        return Math.min(trackTop + (trackH - thumbH) * progress, trackTop + trackH - thumbH);
    }

    private boolean isInScrollbar(float mx, float my, float contentX, float contentY, float contentW, float contentH) {
        float x = scrollbarTrackX(contentX, contentW) - 6f;
        float top = scrollbarTrackTop(contentY);
        float h = scrollbarTrackH(contentH);
        return mx >= x && mx <= x + 16f && my >= top && my <= top + h;
    }

    private void setScrollFromScrollbar(BasePage page, float my, float contentY, float contentH) {
        float totalH = getContentTotalHeight(page);
        float scrollAreaH = contentH - 54f;
        float maxScroll = Math.max(0f, totalH - scrollAreaH);
        float trackTop = scrollbarTrackTop(contentY);
        float trackH = scrollbarTrackH(contentH);
        float thumbH = scrollbarThumbH(page, contentH);
        float available = Math.max(1f, trackH - thumbH);
        float thumbTop = Math.max(trackTop, Math.min(my - scrollbarDragOffset, trackTop + available));
        targetScrollOffset = maxScroll * ((thumbTop - trackTop) / available);
    }

    @Override public void onClose() { closing = true; }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (closing) return false;
        int button = event.button();

        float visualScale = getVisualScale(this.width, this.height);
        float mx = toLayoutX(event.x(), this.width, visualScale);
        float my = toLayoutY(event.y(), this.height, visualScale);
        float[] l = layout(this.width, this.height);
        float cardX = l[0];
        float sidebarW = l[4], tabStartY = l[5], tabH = l[6], tabGap = l[7], tabW = l[8];
        float closeX = l[9], closeY = l[10], closeH = l[11];
        float resetY = l[12], resetH = l[13];
        float contentX = l[14], contentY = l[15], contentW = l[16], contentH = l[17];
        BasePage page = pages.get(selectedTab);

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

        if (button == 0 && mx >= closeX && mx <= closeX + tabW && my >= resetY && my <= resetY + resetH) {
            if (resetConfirm) {
                ResetManager.resetAll();
                resetConfirm = false;
                pages.set(selectedTab, switch (selectedTab) {
                    case 0 -> new CombatPage();
                    case 1 -> new VisualPage();
                    case 2 -> new ToolPage();
                    case 3 -> new OptimizePage();
                    default -> new OtherPage();
                });
            } else {
                resetConfirm = true;
            }
            return true;
        }

        resetConfirm = false;

        if (mx >= contentX && mx <= contentX + contentW && my >= contentY && my <= contentY + contentH) {
            if (hasScrollbar(page, contentH) && isInScrollbar(mx, my, contentX, contentY, contentW, contentH)) {
                draggingScrollbar = true;
                float thumbTop = scrollbarThumbTop(page, contentY, contentH);
                float thumbH = scrollbarThumbH(page, contentH);
                scrollbarDragOffset = my >= thumbTop && my <= thumbTop + thumbH ? my - thumbTop : thumbH * 0.5f;
                setScrollFromScrollbar(page, my, contentY, contentH);
                contentScrollOffset = targetScrollOffset;
                return true;
            }
            float moduleStartY = contentY + 54f;
            boolean hit = page.onClick(mx, my, contentX + 10f, moduleStartY, contentW - 40f, contentScrollOffset, button);
            if (hit) {
                draggingInContent = true;
            }
            return hit;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingScrollbar) {
            float visualScale = getVisualScale(this.width, this.height);
            float my = toLayoutY(event.y(), this.height, visualScale);
            float[] l = layout(this.width, this.height);
            BasePage page = pages.get(selectedTab);
            setScrollFromScrollbar(page, my, l[15], l[17]);
            contentScrollOffset = targetScrollOffset;
            return true;
        }
        if (draggingInContent) {
            float visualScale = getVisualScale(this.width, this.height);
            float mx = toLayoutX(event.x(), this.width, visualScale);
            float my = toLayoutY(event.y(), this.height, visualScale);
            float[] l = layout(this.width, this.height);
            float contentX = l[14], contentY = l[15], contentW = l[16];
            float moduleStartY = contentY + 54f;
            pages.get(selectedTab).onDrag(mx, my, contentX + 10f, moduleStartY, contentW - 40f, contentScrollOffset);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingInContent = false;
        draggingScrollbar = false;
        pages.get(selectedTab).releaseDrag();
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        float visualScale = getVisualScale(this.width, this.height);
        float layoutMx = toLayoutX(mx, this.width, visualScale);
        float layoutMy = toLayoutY(my, this.height, visualScale);
        float[] l = layout(this.width, this.height);
        float contentX = l[14], contentY = l[15], contentW = l[16], contentH = l[17];

        if (layoutMx >= contentX && layoutMx <= contentX + contentW && layoutMy >= contentY && layoutMy <= contentY + contentH) {
            BasePage page = pages.get(selectedTab);
            float scrollAreaH2 = contentH - 54f;
            float maxScroll = Math.max(0f, getContentTotalHeight(page) - scrollAreaH2);
            targetScrollOffset = Math.max(0f, Math.min(maxScroll, targetScrollOffset + (float)(-vScroll * 16f)));
            return true;
        }
        return false;
    }
}
