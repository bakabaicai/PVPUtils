package com.pvp_utils.client.gui.clickgui;

import com.pvp_utils.Config;
import com.pvp_utils.PVPUtils;
import com.pvp_utils.client.Version;
import com.pvp_utils.client.gui.clickgui.pages.*;
import com.pvp_utils.client.gui.clickgui.widget.SettingModule;
import com.pvp_utils.client.ResetManager;
import com.pvp_utils.client.ModuleKeybindManager;
import com.pvp_utils.client.modules.impl.Optimize.InputMethodFix.InputMethodFix;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaGlBackend;
import com.pvp_utils.client.render.skia.SkiaScreen;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL11.glReadBuffer;
import static org.lwjgl.opengl.GL11.glReadPixels;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER_BINDING;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;

public class NewSettingsScreen extends SkiaScreen {

    private final List<BasePage> pages;

    private static final String[] TAB_ICONS = {"\uE903", "\uE901", "\uE026", "\uE121", "\uE900", "\uE3A9"};
    private static final String[] TAB_ICON_FONTS = {FontRenderer.ICON, FontRenderer.ICON, FontRenderer.MATERIAL_SYMBOLS, FontRenderer.MATERIAL_SYMBOLS, FontRenderer.ICON, FontRenderer.MATERIAL_SYMBOLS};
    private static final String[] TAB_KEYS_ZH = {"战斗", "视觉", "工具", "优化", "其他", "主题"};
    private static final String[] TAB_KEYS_EN = {"Combat", "Render", "Tools", "Optimize", "Misc", "Theme"};

    private int selectedTab = 0;
    private int hoveredTab = -1;
    private boolean closeHovered = false;
    private boolean resetHovered = false;
    private boolean resetConfirm = false;
    private boolean closing = false;
    private boolean searchFocused = false;
    private String searchText = "";
    private BasePage searchResultsPage;

    private final float[] tabHoverAlpha = new float[TAB_KEYS_ZH.length];
    private float closeHoverAlpha = 0f;
    private float resetHoverAlpha = 0f;
    private float indicatorY = -1f;
    private float openProgress = 0f;
    private float searchFocusAlpha = 0f;
    private float searchTextOffset = 0f;
    private float searchCursorTime = 0f;
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
    private final Paint cardPaint = new Paint().setAntiAlias(true);
    private final Paint sidebarPaint = new Paint().setAntiAlias(true);
    private final Paint dividerPaint = new Paint().setAntiAlias(true);
    private final Paint indicatorPaint = new Paint().setAntiAlias(true);
    private final Paint hoverPaint = new Paint().setAntiAlias(true);
    private final Paint resetBgPaint = new Paint().setAntiAlias(true);
    private final Paint closeBgPaint = new Paint().setAntiAlias(true);
    private final Paint scrollbarTrackPaint = new Paint().setAntiAlias(true);
    private final Paint scrollbarThumbPaint = new Paint().setAntiAlias(true);
    private final Paint searchBgPaint = new Paint().setAntiAlias(true);
    private final Paint searchLinePaint = new Paint().setAntiAlias(true);
    private final SkiaGlBackend glBackend = new SkiaGlBackend();
    private final float resetIconWidth = FontRenderer.measureTextWidth("\uE042", 13f, FontRenderer.MATERIAL_SYMBOLS);
    private String cachedResetText = "";
    private float cachedResetTextWidth = 0f;
    private String cachedCloseText = "";
    private float cachedCloseTextWidth = 0f;
    private BasePage cachedScrollPage = null;
    private float cachedScrollContentH = Float.NaN;
    private float cachedContentTotalHeight = 0f;
    private float cachedScrollAreaHeight = 0f;
    private float cachedScrollMax = 0f;
    private int lastHoverSignature = Integer.MIN_VALUE;
    private double debugLastDrawMs = 0.0;
    private double debugLastUpdateMs = 0.0;
    private int debugVisibleModules = 0;
    private int debugExpandedModules = 0;
    private int debugAnimatingModules = 0;
    private boolean pendingFrame = false;
    private int pendingMouseX = 0;
    private int pendingMouseY = 0;
    private float pendingDelta = 0f;
    private final ByteBuffer diagnosticPixel = BufferUtils.createByteBuffer(4);
    private int diagnosticFrame = 0;
    private int diagnosticSampleFrame = -1;
    private int diagnosticSampleX = 0;
    private int diagnosticSampleY = 0;
    private int diagnosticMainColor = 0;
    private int diagnosticMainStatus = 0;
    private int diagnosticReadStatus = 0;

    public NewSettingsScreen(Screen parent) {
        super(Component.literal("Settings"), parent);
        pages = new ArrayList<>(List.of(new CombatPage(), new RenderPage(), new ToolPage(), new OptimizePage(), new MiscPage(), new ThemePage()));
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
        return getUiScale(width, height) * (0.9f + 0.1f * easeOutCubic(openProgress));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        pendingMouseX = mouseX;
        pendingMouseY = mouseY;
        pendingDelta = delta;
        pendingFrame = true;
    }

    public void renderFrameEnd() {
        if (!pendingFrame || this.minecraft == null || this.minecraft.screen != this) {
            pendingFrame = false;
            return;
        }
        int framebufferId = mainFramebufferId();
        Canvas canvas = glBackend.begin(framebufferId);
        if (canvas == null) {
            return;
        }
        try {
            drawSkia(canvas, this.width, this.height, pendingMouseX, pendingMouseY, pendingDelta);
        } finally {
            glBackend.end();
            pendingFrame = false;
        }
        diagnosticFrame++;
        if (diagnosticFrame <= 5 || diagnosticFrame % 120 == 0) {
            captureMainFramebuffer(framebufferId);
        }
    }

    public void tracePresentedFramebuffer() {
        if (diagnosticSampleFrame != diagnosticFrame) return;
        int presentedColor = readFramebufferColor(0, diagnosticSampleX, diagnosticSampleY);
        int presentedStatus = diagnosticReadStatus;
        PVPUtils.LOGGER.info(
                "[ClickGUI GPU] frame={} pixel={},{} main={} presented={} mainStatus={} presentedStatus={} complete={}",
                diagnosticFrame,
                diagnosticSampleX,
                diagnosticSampleY,
                String.format("%08X", diagnosticMainColor),
                String.format("%08X", presentedColor),
                Integer.toHexString(diagnosticMainStatus),
                Integer.toHexString(presentedStatus),
                diagnosticMainStatus == GL_FRAMEBUFFER_COMPLETE && presentedStatus == GL_FRAMEBUFFER_COMPLETE
        );
        diagnosticSampleFrame = -1;
    }

    private void captureMainFramebuffer(int framebufferId) {
        var window = minecraft.getWindow();
        float[] l = layout(width, height);
        float visualScale = getVisualScale(width, height);
        float guiX = width * 0.5f + (l[0] + l[4] + 12f - width * 0.5f) * visualScale;
        float guiY = height * 0.5f + (l[1] + 12f - height * 0.5f) * visualScale;
        diagnosticSampleX = Math.max(0, Math.min(window.getWidth() - 1, Math.round(guiX * (float) window.getGuiScale())));
        diagnosticSampleY = Math.max(0, Math.min(window.getHeight() - 1, window.getHeight() - 1 - Math.round(guiY * (float) window.getGuiScale())));
        diagnosticMainColor = readFramebufferColor(framebufferId, diagnosticSampleX, diagnosticSampleY);
        diagnosticMainStatus = diagnosticReadStatus;
        diagnosticSampleFrame = diagnosticFrame;
    }

    private int readFramebufferColor(int framebufferId, int x, int y) {
        int previousFramebuffer = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, framebufferId);
        glReadBuffer(framebufferId == 0 ? GL_BACK : GL_COLOR_ATTACHMENT0);
        diagnosticReadStatus = glCheckFramebufferStatus(GL_READ_FRAMEBUFFER);
        diagnosticPixel.clear();
        glReadPixels(x, y, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, diagnosticPixel);
        int color = (diagnosticPixel.get(0) & 0xFF) << 24
                | (diagnosticPixel.get(1) & 0xFF) << 16
                | (diagnosticPixel.get(2) & 0xFF) << 8
                | diagnosticPixel.get(3) & 0xFF;
        glBindFramebuffer(GL_READ_FRAMEBUFFER, previousFramebuffer);
        return color;
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
        return 54f + page.getTotalHeight() + getVisibleModuleGapTotal(page) + 12f;
    }

    private float getVisibleModuleGapTotal(BasePage page) {
        int visibleCount = 0;
        for (var m : page.getModules()) if (m.isVisible()) visibleCount++;
        return visibleCount * 8f;
    }

    private void updateScrollCache(BasePage page, float contentH) {
        float contentTotalHeight = getContentTotalHeight(page);
        if (cachedScrollPage == page
                && cachedScrollContentH == contentH
                && Math.abs(cachedContentTotalHeight - contentTotalHeight) < 0.01f) {
            return;
        }
        cachedScrollPage = page;
        cachedScrollContentH = contentH;
        cachedContentTotalHeight = contentTotalHeight;
        cachedScrollAreaHeight = contentH - 54f;
        cachedScrollMax = Math.max(0f, cachedContentTotalHeight - cachedScrollAreaHeight);
    }

    private void invalidateScrollLayout() {
        cachedScrollPage = null;
    }

    private int mainFramebufferId() {
        if (minecraft.getMainRenderTarget().getColorTexture() instanceof GlTexture texture
                && RenderSystem.getDevice() instanceof GlDevice device) {
            return texture.getFbo(device.directStateAccess(), minecraft.getMainRenderTarget().getDepthTexture());
        }
        return 0;
    }

    @Override
    public void removed() {
        pendingFrame = false;
        glBackend.destroy();
        super.removed();
    }

    private void updateDebugStats(BasePage page) {
        int visible = 0;
        int expanded = 0;
        int animating = 0;
        for (var module : page.getModules()) {
            if (!module.isVisible()) continue;
            visible++;
            if (module.getTotalHeight() > 56.5f) expanded++;
            if (module.isAnimating()) animating++;
        }
        debugVisibleModules = visible;
        debugExpandedModules = expanded;
        debugAnimatingModules = animating;
    }

    private void drawDebugOverlay(Canvas canvas, float cardX, float cardY, float cardW, float alpha) {
        if (!Version.DEBUG) return;
        String line1 = String.format("ClickGUI %.2fms draw %.2fms update", debugLastDrawMs, debugLastUpdateMs);
        String line2 = String.format("modules %d visible %d expanded %d anim", debugVisibleModules, debugExpandedModules, debugAnimatingModules);
        String line3 = String.format("scroll %.1f/%.1f full=%s", contentScrollOffset, cachedScrollMax, Config.fullMode ? "Y" : "N");
        float padding = 10f;
        float textSize = 10f;
        float w = Math.max(FontRenderer.measureTextWidth(line1, textSize), Math.max(FontRenderer.measureTextWidth(line2, textSize), FontRenderer.measureTextWidth(line3, textSize))) + padding * 2f;
        float h = 42f;
        float x = cardX + cardW - w - 14f;
        float y = cardY + 14f;
        Paint bg = new Paint().setAntiAlias(true);
        bg.setColor(withAlpha(0xF7F7F8, alpha));
        canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 8f), bg);
        FontRenderer.drawText(canvas, line1, x + padding, y + 13f, textSize, withAlpha(0x444444, alpha));
        FontRenderer.drawText(canvas, line2, x + padding, y + 25f, textSize, withAlpha(0x666666, alpha));
        FontRenderer.drawText(canvas, line3, x + padding, y + 37f, textSize, withAlpha(0x666666, alpha));
    }

    private int computeHoverSignature(double mouseX, double mouseY) {
        float visualScale = getVisualScale(this.width, this.height);
        float mx = toLayoutX(mouseX, this.width, visualScale);
        float my = toLayoutY(mouseY, this.height, visualScale);
        float[] l = layout(this.width, this.height);
        float cardX = l[0];
        float tabStartY = l[5], tabH = l[6], tabGap = l[7], tabW = l[8];
        float closeX = l[9], closeY = l[10], closeH = l[11];
        float resetY = l[12], resetH = l[13];

        int hovered = -1;
        for (int i = 0; i < TAB_KEYS_ZH.length; i++) {
            float ty = tabStartY + i * (tabH + tabGap);
            if (mx >= cardX + 12f && mx <= cardX + 12f + tabW && my >= ty && my <= ty + tabH) {
                hovered = i;
                break;
            }
        }

        boolean close = mx >= closeX && mx <= closeX + tabW && my >= closeY && my <= closeY + closeH;
        boolean reset = mx >= closeX && mx <= closeX + tabW && my >= resetY && my <= resetY + resetH;

        int signature = hovered + 2;
        if (close) signature |= 1 << 8;
        if (reset) signature |= 1 << 9;
        return signature;
    }

    @Override
    protected boolean needsContinuousRedraw() {
        if (closing) return true;
        if (openProgress < 0.999f) return true;
        if (draggingInContent || draggingScrollbar) return true;
        if (Math.abs(contentScrollOffset - targetScrollOffset) > 0.35f) return true;
        if (closeHoverAlpha > 0.01f || closeHovered) return true;
        if (resetHoverAlpha > 0.01f || resetHovered) return true;
        if (searchFocused || searchFocusAlpha > 0.01f || Math.abs(searchTextOffset) > 0.01f) return true;
        if (indicatorY < 0f) return true;
        float[] l = layout(this.width, this.height);
        float targetIndicatorY = l[5] + selectedTab * (l[6] + l[7]);
        if (Math.abs(indicatorY - targetIndicatorY) > 0.35f) return true;
        for (float alpha : tabHoverAlpha) if (alpha > 0.01f && alpha < 0.99f) return true;
        return activePage().hasAnimatingModules();
    }

    @Override
    protected void drawSkia(Canvas canvas, int width, int height, int mouseX, int mouseY, float delta) {
        long debugDrawStartNs = Version.DEBUG ? System.nanoTime() : 0L;
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

        float visualScale = getUiScale(width, height) * (0.9f + 0.1f * animT);
        float layoutMouseX = toLayoutX(mouseX, width, visualScale);
        float layoutMouseY = toLayoutY(mouseY, height, visualScale);
        float[] l = layout(width, height);
        BasePage currentPage = activePage();
        updateScrollCache(currentPage, l[17]);
        targetScrollOffset = Math.min(targetScrollOffset, cachedScrollMax);
        contentScrollOffset = lerp(contentScrollOffset, targetScrollOffset, dt * 18f);
        float cardX = l[0], cardY = l[1], cardW = l[2], cardH = l[3];
        float sidebarW = l[4], tabStartY = l[5], tabH = l[6], tabGap = l[7], tabW = l[8];
        float closeX = l[9], closeY = l[10], closeH = l[11], resetY = l[12], resetH = l[13];
        float contentX = l[14], contentY = l[15], contentW = l[16], contentH = l[17];
        float searchX = cardX + 18f, searchY = cardY + 66f, searchW = sidebarW - 36f, searchH = 28f;

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
        searchFocusAlpha = lerp(searchFocusAlpha, searchFocused ? 1f : 0f, dt * 14f);
        searchCursorTime += dt;

        float targetIndicatorY = tabStartY + selectedTab * (tabH + tabGap);
        if (indicatorY < 0f) indicatorY = targetIndicatorY;
        indicatorY = lerp(indicatorY, targetIndicatorY, dt * 12f);

        BasePage page = currentPage;
        long debugUpdateStartNs = Version.DEBUG ? System.nanoTime() : 0L;
        page.update(dt);
        if (Version.DEBUG) {
            debugLastUpdateMs = (System.nanoTime() - debugUpdateStartNs) / 1_000_000.0;
            updateDebugStats(page);
        }

        float alpha = 1f;
        float cx = width / 2f;
        float cy = height / 2f;

        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(visualScale, visualScale);
        canvas.translate(-cx, -cy);

        cardPaint.setColor(withAlpha(0xF5F5F7, alpha));
        canvas.drawRRect(RRect.makeXYWH(cardX, cardY, cardW, cardH, 16f), cardPaint);

        sidebarPaint.setColor(withAlpha(0xFFFFFF, alpha));
        canvas.save();
        canvas.clipRRect(RRect.makeXYWH(cardX, cardY, sidebarW, cardH, 16f));
        canvas.drawRect(Rect.makeXYWH(cardX, cardY, sidebarW, cardH), sidebarPaint);
        canvas.restore();

        dividerPaint.setColor(withAlpha(0xEEEEEE, alpha));
        canvas.drawRect(Rect.makeXYWH(cardX + sidebarW, cardY + 14f, 1f, cardH - 28f), dividerPaint);

        FontRenderer.drawText(canvas, "PVPUtils", cardX + 18f, cardY + 38f, 16f, withAlpha(0x111111, alpha));
        drawDebugOverlay(canvas, cardX, cardY, cardW, alpha);
        FontRenderer.drawText(canvas, UiText.t("在下方调整设置...", "Adjust the settings below..."), cardX + 18f, cardY + 54f, 10f, withAlpha(0xAAAAAA, alpha));
        drawSearchBox(canvas, searchX, searchY, searchW, searchH, alpha, dt);

        indicatorPaint.setColor(withAlpha(0xE8EEFF, alpha));
        canvas.drawRRect(RRect.makeXYWH(cardX + 12f, indicatorY, tabW, tabH, 8f), indicatorPaint);

        for (int i = 0; i < TAB_KEYS_ZH.length; i++) {
            float tabY = tabStartY + i * (tabH + tabGap);
            if (tabHoverAlpha[i] > 0.01f) {
                hoverPaint.setColor(withAlpha(0xF3F4FF, alpha * tabHoverAlpha[i]));
                canvas.drawRRect(RRect.makeXYWH(cardX + 12f, tabY, tabW, tabH, 8f), hoverPaint);
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
        resetBgPaint.setColor(withAlpha(resetBgColor, alpha));
        canvas.drawRRect(RRect.makeXYWH(closeX, resetY, tabW, resetH, 8f), resetBgPaint);
        String resetText = resetConfirm ? UiText.t("再次点击以确认", "Click Again to Confirm") : UiText.t("重置所有设置", "Reset All Settings");
        String resetIcon = "\uE042";
        if (!resetText.equals(cachedResetText)) {
            cachedResetText = resetText;
            cachedResetTextWidth = FontRenderer.measureTextWidth(resetText, 12f);
        }
        float resetTotalW = resetIconWidth + 6f + cachedResetTextWidth;
        float resetStartX = closeX + (tabW - resetTotalW) / 2f;
        FontRenderer.drawText(canvas, resetIcon, resetStartX, resetY + 22f, 13f, withAlpha(resetTextColor, alpha), FontRenderer.MATERIAL_SYMBOLS);
        FontRenderer.drawText(canvas, resetText, resetStartX + resetIconWidth + 6f, resetY + 22f, 12f, withAlpha(resetTextColor, alpha));

        closeBgPaint.setColor(withAlpha(closeBgColor, alpha));
        canvas.drawRRect(RRect.makeXYWH(closeX, closeY, tabW, closeH, 8f), closeBgPaint);
        String closeText = UiText.t("× 关闭", "× Close");
        if (!closeText.equals(cachedCloseText)) {
            cachedCloseText = closeText;
            cachedCloseTextWidth = FontRenderer.measureTextWidth(closeText, 12f);
        }
        FontRenderer.drawText(canvas, closeText, closeX + (tabW - cachedCloseTextWidth) / 2f, closeY + 22f, 12f, withAlpha(closeTextColor, alpha));

        FontRenderer.drawText(canvas, page.getTitle(), contentX + 18f, contentY + 26f, 18f, withAlpha(0x111111, alpha));
        FontRenderer.drawText(canvas, page.getSubtitle(), contentX + 18f, contentY + 42f, 10f, withAlpha(0xAAAAAA, alpha));

        float clipTop = contentY + 54f;
        float clipBottom = contentY + contentH;
        Rect contentClip = Rect.makeXYWH(contentX, clipTop, contentW, clipBottom - clipTop);
        canvas.save();
        canvas.clipRect(contentClip);

        float moduleStartY = contentY + 54f;
        page.draw(canvas, contentX + 10f, moduleStartY, contentW - 40f, contentH - 54f, alpha, contentScrollOffset, layoutMouseX, layoutMouseY);
        drawScrollbar(canvas, page, contentX, contentY, contentW, contentH, alpha);

        canvas.restore();

        canvas.restore();

        if (Version.DEBUG) {
            debugLastDrawMs = (System.nanoTime() - debugDrawStartNs) / 1_000_000.0;
        }
    }

    private void drawScrollbar(Canvas canvas, BasePage page, float contentX, float contentY, float contentW, float contentH, float alpha) {
        updateScrollCache(page, contentH);
        if (cachedContentTotalHeight <= cachedScrollAreaHeight) return;

        float trackX = contentX + contentW - 8f;
        float trackTop = contentY + 60f;
        float trackH = contentH - 60f - 8f;
        float thumbH = Math.max(20f, trackH * cachedScrollAreaHeight / cachedContentTotalHeight);
        float maxScroll = Math.max(1f, cachedScrollMax);
        float progress = Math.min(1f, contentScrollOffset / maxScroll);
        float thumbTop = trackTop + (trackH - thumbH) * progress;
        thumbTop = Math.min(thumbTop, trackTop + trackH - thumbH);

        scrollbarTrackPaint.setColor(withAlpha(0xE0E0E0, alpha * 0.5f));
        canvas.drawRRect(RRect.makeXYWH(trackX, trackTop, 4f, trackH, 2f), scrollbarTrackPaint);
        scrollbarThumbPaint.setColor(withAlpha(0xBBBBBB, alpha));
        canvas.drawRRect(RRect.makeXYWH(trackX, thumbTop, 4f, thumbH, 2f), scrollbarThumbPaint);
    }

    private void drawSearchBox(Canvas canvas, float x, float y, float width, float height, float alpha, float dt) {
        int background = lerpColor(0xF1F2F5, 0xE9EEFF, searchFocusAlpha);
        searchBgPaint.setColor(withAlpha(background, alpha));
        canvas.drawRRect(RRect.makeXYWH(x, y, width, height, 7f), searchBgPaint);

        FontRenderer.drawText(canvas, "\uE8B6", x + 9f, y + 19f, 12f, withAlpha(0x7D8493, alpha), FontRenderer.MATERIAL_SYMBOLS);
        float textX = x + 28f;
        float textW = Math.max(1f, width - 36f);
        boolean empty = searchText.isEmpty();
        String display = empty ? UiText.t("\u8F93\u5165\u4EE5\u67E5\u627E...", "Type to search...") : searchText;
        float realTextWidth = FontRenderer.measureTextWidth(searchText, 10f);
        float targetOffset = empty ? 0f : Math.max(0f, realTextWidth - textW + 3f);
        searchTextOffset = lerp(searchTextOffset, targetOffset, dt * 16f);

        canvas.save();
        canvas.clipRect(Rect.makeXYWH(textX, y + 2f, textW, height - 4f));
        FontRenderer.drawText(canvas, display, textX - (empty ? 0f : searchTextOffset), y + 18.5f, 10f,
                withAlpha(empty ? 0x9BA1AE : 0x343842, alpha));
        if (searchFocused) {
            float cursorPulse = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(searchCursorTime * 6f));
            float cursorX = textX + Math.min(textW - 1f, Math.max(0f, realTextWidth - searchTextOffset));
            searchLinePaint.setColor(withAlpha(0x5A73E8, alpha * cursorPulse));
            canvas.drawRect(Rect.makeXYWH(cursorX, y + 7f, 1f, 14f), searchLinePaint);
        }
        canvas.restore();

        float linePulse = 0.3f + 0.7f * (0.5f + 0.5f * (float) Math.sin(searchCursorTime * 6f));
        searchLinePaint.setColor(withAlpha(0x5A73E8, alpha * searchFocusAlpha * linePulse));
        canvas.drawRect(Rect.makeXYWH(x + 8f, y + height - 2f, width - 16f, 1f), searchLinePaint);
    }

    private void applySearch() {
        for (BasePage page : pages) {
            page.setSearchQuery(searchText);
        }
        if (searchText.isBlank()) {
            searchResultsPage = null;
        } else {
            List<SettingModule> results = new ArrayList<>();
            for (BasePage page : pages) {
                for (SettingModule module : page.getModules()) {
                    if (module.isVisible() && module.matchesSearch(searchText)) {
                        results.add(module);
                    }
                }
            }
            searchResultsPage = new SearchResultsPage(searchText, results);
        }
        targetScrollOffset = 0f;
        contentScrollOffset = 0f;
        invalidateScrollLayout();
    }

    private BasePage activePage() {
        return searchResultsPage == null ? pages.get(selectedTab) : searchResultsPage;
    }

    private void clearSearch() {
        setSearchFocused(false);
        searchText = "";
        searchTextOffset = 0f;
        applySearch();
    }

    private void setSearchFocused(boolean focused) {
        if (searchFocused == focused) {
            return;
        }
        searchFocused = focused;
        InputMethodFix.setCustomTextInputActive(focused, minecraft);
    }

    private boolean hasScrollbar(BasePage page, float contentH) {
        updateScrollCache(page, contentH);
        return cachedContentTotalHeight > cachedScrollAreaHeight;
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
        updateScrollCache(page, contentH);
        return Math.max(20f, scrollbarTrackH(contentH) * cachedScrollAreaHeight / cachedContentTotalHeight);
    }

    private float scrollbarThumbTop(BasePage page, float contentY, float contentH) {
        updateScrollCache(page, contentH);
        float trackTop = scrollbarTrackTop(contentY);
        float trackH = scrollbarTrackH(contentH);
        float thumbH = scrollbarThumbH(page, contentH);
        float maxScroll = Math.max(1f, cachedScrollMax);
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
        updateScrollCache(page, contentH);
        float maxScroll = cachedScrollMax;
        float trackTop = scrollbarTrackTop(contentY);
        float trackH = scrollbarTrackH(contentH);
        float thumbH = scrollbarThumbH(page, contentH);
        float available = Math.max(1f, trackH - thumbH);
        float thumbTop = Math.max(trackTop, Math.min(my - scrollbarDragOffset, trackTop + available));
        targetScrollOffset = maxScroll * ((thumbTop - trackTop) / available);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (ModuleKeybindManager.captureKey(event.key())) {
            invalidateScrollLayout();
            return true;
        }
        if (searchFocused) {
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE && !searchText.isEmpty()) {
                int end = searchText.offsetByCodePoints(searchText.length(), -1);
                searchText = searchText.substring(0, end);
                applySearch();
            } else if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                clearSearch();
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!searchFocused) {
            return super.charTyped(event);
        }
        String typed = event.codepointAsString();
        if (typed != null && !typed.isEmpty()) {
            searchText += typed;
            applySearch();
        }
        return true;
    }

    @Override public void onClose() {
        clearSearch();
        closing = true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        int hoverSignature = computeHoverSignature(mouseX, mouseY);
        if (hoverSignature != lastHoverSignature) {
            lastHoverSignature = hoverSignature;
            invalidateScrollLayout();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (closing) return false;
        int button = event.button();
        if (ModuleKeybindManager.captureMouseButton(button)) {
            draggingInContent = false;
            draggingScrollbar = false;
            invalidateScrollLayout();
            return true;
        }
        if (button > GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return true;
        }

        float visualScale = getVisualScale(this.width, this.height);
        float mx = toLayoutX(event.x(), this.width, visualScale);
        float my = toLayoutY(event.y(), this.height, visualScale);
        float[] l = layout(this.width, this.height);
        float cardX = l[0];
        float sidebarW = l[4], tabStartY = l[5], tabH = l[6], tabGap = l[7], tabW = l[8];
        float closeX = l[9], closeY = l[10], closeH = l[11];
        float resetY = l[12], resetH = l[13];
        float contentX = l[14], contentY = l[15], contentW = l[16], contentH = l[17];
        float searchX = cardX + 18f, searchY = l[1] + 66f, searchW = sidebarW - 36f, searchH = 28f;
        BasePage page = activePage();

        if (button == 0 && mx >= searchX && mx <= searchX + searchW && my >= searchY && my <= searchY + searchH) {
            setSearchFocused(true);
            searchCursorTime = 0f;
            invalidateScrollLayout();
            return true;
        }
        if (button == 0) {
            setSearchFocused(false);
        }

        for (int i = 0; i < TAB_KEYS_ZH.length; i++) {
            float ty = tabStartY + i * (tabH + tabGap);
            if (mx >= cardX + 12f && mx <= cardX + 12f + tabW && my >= ty && my <= ty + tabH) {
                if (button == 0) {
                    clearSearch();
                    selectedTab = i;
                    targetScrollOffset = 0f;
                    contentScrollOffset = 0f;
                    invalidateScrollLayout();
                }
                return true;
            }
        }

        if (button == 0 && mx >= closeX && mx <= closeX + tabW && my >= closeY && my <= closeY + closeH) {
            closing = true;
            invalidateScrollLayout();
            return true;
        }

        if (button == 0 && mx >= closeX && mx <= closeX + tabW && my >= resetY && my <= resetY + resetH) {
            if (resetConfirm) {
                ResetManager.resetAll();
                resetConfirm = false;
                pages.set(selectedTab, switch (selectedTab) {
                    case 0 -> new CombatPage();
                    case 1 -> new RenderPage();
                    case 2 -> new ToolPage();
                    case 3 -> new OptimizePage();
                    case 4 -> new MiscPage();
                    default -> new ThemePage();
                });
                applySearch();
            } else {
                resetConfirm = true;
            }
            invalidateScrollLayout();
            return true;
        }

        resetConfirm = false;

        if (mx >= contentX && mx <= contentX + contentW && my >= contentY && my <= contentY + contentH) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && hasScrollbar(page, contentH) && isInScrollbar(mx, my, contentX, contentY, contentW, contentH)) {
                draggingScrollbar = true;
                float thumbTop = scrollbarThumbTop(page, contentY, contentH);
                float thumbH = scrollbarThumbH(page, contentH);
                scrollbarDragOffset = my >= thumbTop && my <= thumbTop + thumbH ? my - thumbTop : thumbH * 0.5f;
                setScrollFromScrollbar(page, my, contentY, contentH);
                contentScrollOffset = targetScrollOffset;
                invalidateScrollLayout();
                return true;
            }
            float moduleStartY = contentY + 54f;
            boolean hit = page.onClick(mx, my, contentX + 10f, moduleStartY, contentW - 40f, contentScrollOffset, button);
            if (hit && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                draggingInContent = true;
                invalidateScrollLayout();
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
            BasePage page = activePage();
            setScrollFromScrollbar(page, my, l[15], l[17]);
            contentScrollOffset = targetScrollOffset;
            invalidateScrollLayout();
            return true;
        }
        if (draggingInContent) {
            float visualScale = getVisualScale(this.width, this.height);
            float mx = toLayoutX(event.x(), this.width, visualScale);
            float my = toLayoutY(event.y(), this.height, visualScale);
            float[] l = layout(this.width, this.height);
            float contentX = l[14], contentY = l[15], contentW = l[16];
            float moduleStartY = contentY + 54f;
            activePage().onDrag(mx, my, contentX + 10f, moduleStartY, contentW - 40f, contentScrollOffset);
            invalidateScrollLayout();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingInContent = false;
        draggingScrollbar = false;
        activePage().releaseDrag();
        invalidateScrollLayout();
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
            BasePage page = activePage();
            updateScrollCache(page, contentH);
            targetScrollOffset = Math.max(0f, Math.min(cachedScrollMax, targetScrollOffset + (float)(-vScroll * 16f)));
            invalidateScrollLayout();
            return true;
        }
        return false;
    }
}
