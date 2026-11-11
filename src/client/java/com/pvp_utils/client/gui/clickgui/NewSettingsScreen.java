package com.pvp_utils.client.gui.clickgui;

import io.github.humbleui.types.RRect;

import com.pvp_utils.client.render.chroma.ChromaDraw;

import com.pvp_utils.Config;
import com.pvp_utils.client.Version;
import com.pvp_utils.client.gui.clickgui.pages.*;
import com.pvp_utils.client.gui.clickgui.theme.ClickGuiThemeColors;
import com.pvp_utils.client.gui.clickgui.widget.SettingModule;
import com.pvp_utils.client.gui.clickgui.widget.PageTransition;
import com.pvp_utils.client.gui.clickgui.widget.SettingTextBox;
import com.pvp_utils.client.ResetManager;
import com.pvp_utils.client.ModuleKeybindManager;
import com.pvp_utils.client.modules.impl.Optimize.InputMethodFix.InputMethodFix;
import com.pvp_utils.client.render.chroma.ChromaGraphics;
import com.pvp_utils.client.render.chroma.ChromaScreen;
import com.pvp_utils.client.render.chroma.effects.ChromaShadow;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.PaintStrokeCap;
import io.github.humbleui.skija.PaintStrokeJoin;
import io.github.humbleui.skija.Path;
import io.github.humbleui.skija.PathBuilder;
import io.github.humbleui.types.Rect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class NewSettingsScreen extends Screen implements ChromaScreen {
    private final Screen parent;

    private final List<BasePage> pages;
    private final PageTransition<BasePage> pageTransition;

    private static final String[] TAB_ICONS = {"\uE903", "\uE901", "\uE026", "\uE121", "\uE900", "\uE3A9", "\uE87B"};
    private static final ChromaDraw.IconSet[] TAB_ICON_FONTS = {ChromaDraw.IconSet.PVP_CONTROLS, ChromaDraw.IconSet.PVP_CONTROLS, ChromaDraw.IconSet.MATERIAL_SYMBOLS, ChromaDraw.IconSet.MATERIAL_SYMBOLS, ChromaDraw.IconSet.PVP_CONTROLS, ChromaDraw.IconSet.MATERIAL_SYMBOLS, ChromaDraw.IconSet.MATERIAL_SYMBOLS};
    private static final String[] TAB_KEYS_ZH = {"战斗", "视觉", "工具", "优化", "其他", "主题", "插件"};
    private static final String[] TAB_KEYS_EN = {"Combat", "Render", "Tools", "Optimize", "Misc", "Theme", "Plugins"};

    private int selectedTab = 0;
    private int hoveredTab = -1;
    private boolean closeHovered = false;
    private boolean resetHovered = false;
    private boolean resetConfirm = false;
    private boolean closing = false;
    private boolean searchFocused = false;
    private String searchText = "";
    private BasePage searchResultsPage;
    private int pluginRevision = -1;

    private final float[] tabHoverAlpha = new float[TAB_KEYS_ZH.length];
    private float closeHoverAlpha = 0f;
    private float resetHoverAlpha = 0f;
    private float indicatorY = -1f;
    private float openProgress = 0f;
    private float searchFocusAlpha = 0f;
    private float searchTextOffset = 0f;
    private float searchCursorTime = 0f;
    private long lastRenderMs = 0;
    private float animatedClickGuiScale = -1f;

    private float contentScrollOffset = 0f;
    private float targetScrollOffset = 0f;
    private boolean draggingInContent = false;
    private boolean draggingScrollbar = false;
    private float scrollbarDragOffset = 0f;
    private static final float OPEN_DURATION = 0.16f;
    private static final float BASE_CARD_W = 740f;
    private static final float BASE_CARD_H = 500f;
    private static final float SCREEN_MARGIN = 24f;
    private static final float FOOTER_H = 24f;
    private static final float DRAG_HANDLE_H = 56f;
    private static final float FLYOUT_GAP = 12f;
    private static final float MIN_CARD_W = 560f;
    private static final float MIN_CARD_H = 380f;
    private static final float MIN_FLYOUT_W = 220f;
    private static final float MAX_FLYOUT_W = 640f;
    private float cardW = Config.clickGuiCardW > 0f ? Config.clickGuiCardW : 740f;
    private float cardH = Config.clickGuiCardH > 0f ? Config.clickGuiCardH : 500f;
    private float flyoutW = Config.clickGuiFlyoutW > 0f ? Config.clickGuiFlyoutW : 288f;
    private double resizeStartMouseX;
    private double resizeStartMouseY;
    private float resizeStartW;
    private float resizeStartH;
    private float resizeStartFlyoutW;
    private float resizeScale = 1f;
    private int resizeMode;
    private float panelShiftX;
    private boolean draggingWindow;
    private double windowDragStartX;
    private double windowDragStartY;
    private float windowDragStartOffsetX;
    private float windowDragStartOffsetY;
    private float cardOffsetX;
    private float cardOffsetY;
    private SettingModule flyoutModule;
    private final List<SettingModule.FlyoutExit> exitingFlyouts = new ArrayList<>();
    private String cachedFooterSource = "";
    private String cachedFooterRender = "";
    private float cachedFooterMaxWidth = -1f;
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
    private final float resetIconWidth = ChromaDraw.iconWidth("\uE042", 13f, ChromaDraw.IconSet.MATERIAL_SYMBOLS);
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

    public NewSettingsScreen(Screen parent) {
        super(Component.literal("Settings"));
        this.parent = parent;
        pages = new ArrayList<>(List.of(new CombatPage(), new RenderPage(), new ToolPage(), new OptimizePage(), new MiscPage(), new ThemePage(), new PluginPage()));
        pageTransition = new PageTransition<>(pages.get(selectedTab));
    }

    public void rebuildCurrentPage() {
        detachPageFlyouts(pageTransition.displayed(), true);
        pages.set(selectedTab, switch (selectedTab) {
            case 0 -> new CombatPage();
            case 1 -> new RenderPage();
            case 2 -> new ToolPage();
            case 3 -> new OptimizePage();
            case 4 -> new MiscPage();
            case 6 -> new PluginPage();
            default -> new ThemePage();
        });
        invalidateScrollLayout();
        applySearch();
    }

    public void showAddServerPage() {
        pageTransition.displayed().releaseDrag();
        detachPageFlyouts(pageTransition.displayed(), true);
        pages.set(selectedTab, new AddServerPage());
        pageTransition.request(activePage());
        targetScrollOffset = contentScrollOffset;
        invalidateScrollLayout();
    }

    private float[] layout(int width, int height) {
        float cardX = (width - cardW) / 2f + cardOffsetX - panelShiftX;
        float cardY = (height - cardH) / 2f + cardOffsetY;
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

    private float maxPanelShift(int layoutW) {
        float scale = Math.max(0.01f, getVisualScale(this.width, this.height));
        float halfWidth = Math.max(0f, this.width * 0.5f - 8f) / scale;
        float rightLimit = layoutW * 0.5f + halfWidth;
        float leftLimit = layoutW * 0.5f - halfWidth;
        float baseCardX = (layoutW - cardW) / 2f + cardOffsetX;
        float reservedWidth = effectiveFlyoutWidth();
        for (SettingModule.FlyoutExit exit : exitingFlyouts) reservedWidth = Math.max(reservedWidth, exit.width());
        float need = baseCardX + cardW + FLYOUT_GAP + reservedWidth - rightLimit;
        float maxShift = Math.max(0f, baseCardX - leftLimit);
        return Math.min(Math.max(0f, need), maxShift);
    }

    private void clampCardOffset() {
        float scale = Math.max(0.01f, getVisualScale(this.width, this.height));
        int layoutW = layoutWidth();
        int layoutH = layoutHeight();
        float halfWidth = Math.max(0f, this.width * 0.5f - 8f) / scale;
        float halfHeight = Math.max(0f, this.height * 0.5f - 8f) / scale;
        float baseX = (layoutW - cardW) * 0.5f;
        float baseY = (layoutH - cardH) * 0.5f;
        float leftLimit = layoutW * 0.5f - halfWidth;
        float rightLimit = layoutW * 0.5f + halfWidth;
        float topLimit = layoutH * 0.5f - halfHeight;
        float bottomLimit = layoutH * 0.5f + halfHeight;
        float minOffsetX = Math.min(0f, leftLimit - baseX);
        float maxOffsetX = Math.max(0f, rightLimit - baseX - cardW);
        float minOffsetY = Math.min(0f, topLimit - baseY);
        float maxOffsetY = Math.max(0f, bottomLimit - baseY - cardH);
        cardOffsetX = Math.max(minOffsetX, Math.min(maxOffsetX, cardOffsetX));
        cardOffsetY = Math.max(minOffsetY, Math.min(maxOffsetY, cardOffsetY));
    }

    private float getUiScale(int width, int height) {
        int layoutWidth = layoutWidth();
        int layoutHeight = layoutHeight();
        float fitX = Math.max(0.1f, (layoutWidth - SCREEN_MARGIN) / BASE_CARD_W);
        float fitY = Math.max(0.1f, (layoutHeight - SCREEN_MARGIN) / BASE_CARD_H);
        float fit = Math.min(1f, Math.min(fitX, fitY));
        float guiScale = minecraft == null ? 2f : Math.max(1f, (float) minecraft.getWindow().getGuiScale());
        float uiScale = animatedClickGuiScale < 0f ? configuredUiScale() : animatedClickGuiScale;
        return fit * 2f / guiScale * uiScale;
    }

    private float configuredUiScale() {
        return 0.75f;
    }

    private void updateUiScale(float dt) {
        float targetScale = configuredUiScale();
        if (animatedClickGuiScale < 0f) animatedClickGuiScale = targetScale;
        animatedClickGuiScale += (targetScale - animatedClickGuiScale) * Math.min(1f, dt * 12f);
        if (Math.abs(animatedClickGuiScale - targetScale) < 0.001f) animatedClickGuiScale = targetScale;
    }

    private float getVisualScale(int width, int height) {
        return getUiScale(width, height) * (0.88f + 0.12f * easeOutCubic(openProgress));
    }

    private int layoutWidth() {
        return minecraft == null ? this.width : Math.max(1, Math.round(minecraft.getWindow().getWidth() * 0.5f));
    }

    private int layoutHeight() {
        return minecraft == null ? this.height : Math.max(1, Math.round(minecraft.getWindow().getHeight() * 0.5f));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        pendingMouseX = mouseX;
        pendingMouseY = mouseY;
        pendingDelta = delta;
        pendingFrame = true;
    }

    @Override
    public void renderChroma(Canvas canvas) {
        if (!pendingFrame || this.minecraft == null || this.minecraft.screen != this) {
            pendingFrame = false;
            return;
        }

        try {
            drawSettings(canvas, this.width, this.height, pendingMouseX, pendingMouseY, pendingDelta);
        } finally {
            pendingFrame = false;
        }
    }

    private void renderPanelBlur(Canvas canvas, RRect panel) {
        if (!Config.clickGuiPanelBlur || minecraft == null) return;
        ChromaGraphics.drawBlurredBackdrop(canvas, panel, panel.getLeft(), panel.getTop(),
                panel.getWidth(), panel.getHeight(), Math.max(0f, Math.min(2f, Config.skiaBlurStrength)) * 10.5f);
        try (Paint tint = new Paint().setAntiAlias(true).setColor(Config.skiaBlurTintColor())) {
            canvas.drawRRect(panel, tint);
        }
    }

    private void renderExitingFlyoutBlur(Canvas canvas, RRect panel, float alpha) {
        if (!Config.clickGuiPanelBlur || alpha <= 0.01f) return;
        if (alpha >= 0.999f) {
            renderPanelBlur(canvas, panel);
            return;
        }
        try (Paint paint = new Paint().setAlphaf(alpha)) {
            int save = canvas.saveLayer(Rect.makeXYWH(panel.getLeft(), panel.getTop(),
                    panel.getWidth(), panel.getHeight()), paint);
            try {
                renderPanelBlur(canvas, panel);
            } finally {
                canvas.restoreToCount(save);
            }
        }
    }

    private float toLayoutX(double x, int width, float scale) {
        return layoutWidth() * 0.5f + ((float) x - width * 0.5f) / scale;
    }

    private float toLayoutY(double y, int height, float scale) {
        return layoutHeight() * 0.5f + ((float) y - height * 0.5f) / scale;
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
        return page.getTotalHeight() + 12f;
    }

    private void updateScrollCache(BasePage page, float contentH) {
        if (page instanceof ThemePage themePage) {
            themePage.prepareLayout(cardW - 190f - 1f - 40f);
            if (themePage.consumeScrollReset()) {
                detachPageFlyouts(page, false);
                targetScrollOffset = 0f;
                contentScrollOffset = 0f;
                invalidateScrollLayout();
            }
        }
        float contentTotalHeight = getContentTotalHeight(page);
        if (cachedScrollPage == page
        && cachedScrollContentH == contentH
        && Math.abs(cachedContentTotalHeight - contentTotalHeight) < 0.01f) {
            return;
        }
        cachedScrollPage = page;
        cachedScrollContentH = contentH;
        cachedContentTotalHeight = contentTotalHeight;
        cachedScrollAreaHeight = contentH - 54f - FOOTER_H;
        cachedScrollMax = Math.max(0f, cachedContentTotalHeight - cachedScrollAreaHeight);
    }

    private void invalidateScrollLayout() {
        cachedScrollPage = null;
    }

    @Override
    public void removed() {
        pendingFrame = false;
        pageTransition.reset(activePage());
        lastRenderMs = 0L;
        exitingFlyouts.clear();
        for (BasePage page : pages) {
            page.releaseDrag();
            for (SettingModule module : page.getModules()) module.resetFlyout();
        }
        flyoutModule = null;
        super.removed();
    }

    private void updateDebugStats(BasePage page) {
        int visible = 0;
        int expanded = 0;
        int animating = 0;
        for (var module : page.getModules()) {
            if (!module.isVisible()) continue;
            visible++;
            if (module.isFlyoutOpen()) expanded++;
            if (module.isAnimating()) animating++;
        }
        debugVisibleModules = visible;
        debugExpandedModules = expanded;
        debugAnimatingModules = animating;
    }

    private void drawDebugOverlay(Canvas canvas, float cardX, float cardY, float cardW, float alpha, ClickGuiThemeColors tc) {
        if (!Version.DEBUG) return;
        String line1 = String.format("ClickGUI %.2fms draw %.2fms update", debugLastDrawMs, debugLastUpdateMs);
        String line2 = String.format("modules %d visible %d expanded %d anim", debugVisibleModules, debugExpandedModules, debugAnimatingModules);
        String line3 = String.format("scroll %.1f/%.1f full=%s", contentScrollOffset, cachedScrollMax, Config.fullMode ? "Y" : "N");
        float padding = 10f;
        float textSize = 10f;
        float w = Math.max(ChromaDraw.textWidth(line1, textSize), Math.max(ChromaDraw.textWidth(line2, textSize), ChromaDraw.textWidth(line3, textSize))) + padding * 2f;
        float h = 42f;
        float x = cardX + cardW - w - 14f;
        float y = cardY + 14f;
        Paint bg = new Paint().setAntiAlias(true);
        bg.setColor(withAlpha(tc.content, alpha));
        canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 8f), bg);
        ChromaDraw.text(canvas, line1, x + padding, (y + 13f) + ChromaDraw.textMetrics(textSize).getAscent(), ChromaDraw.textMetrics(textSize).getDescent() - ChromaDraw.textMetrics(textSize).getAscent(), withAlpha(tc.mutedText, alpha), textSize);
        ChromaDraw.text(canvas, line2, x + padding, (y + 25f) + ChromaDraw.textMetrics(textSize).getAscent(), ChromaDraw.textMetrics(textSize).getDescent() - ChromaDraw.textMetrics(textSize).getAscent(), withAlpha(tc.secondaryText, alpha), textSize);
        ChromaDraw.text(canvas, line3, x + padding, (y + 37f) + ChromaDraw.textMetrics(textSize).getAscent(), ChromaDraw.textMetrics(textSize).getDescent() - ChromaDraw.textMetrics(textSize).getAscent(), withAlpha(tc.secondaryText, alpha), textSize);
    }

    private int computeHoverSignature(double mouseX, double mouseY) {
        float visualScale = getVisualScale(this.width, this.height);
        float mx = toLayoutX(mouseX, this.width, visualScale);
        float my = toLayoutY(mouseY, this.height, visualScale);
        float[] l = layout(layoutWidth(), layoutHeight());
        float cardX = l[0];
        float tabStartY = l[5], tabH = l[6], tabGap = l[7], tabW = l[8];
        float closeX = l[9], closeY = l[10], closeH = l[11];
        float resetY = l[12], resetH = l[13];

        int hovered = -1;
        for (int i = 0; i < TAB_KEYS_ZH.length; i++) {
            float ty = tabStartY + i * (tabH + tabGap) + 4f;
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

    private void drawSettings(Canvas canvas, int width, int height, int mouseX, int mouseY, float delta) {
        long debugDrawStartNs = Version.DEBUG ? System.nanoTime() : 0L;
        long now = System.currentTimeMillis();
        float dt = lastRenderMs == 0 ? 0.016f : Math.clamp((now - lastRenderMs) / 1000f, 0f, 0.1f);
        lastRenderMs = now;

        if (closing) {
            openProgress = clamp01(openProgress - dt / OPEN_DURATION);
            if (openProgress < 0.005f) {
                if (minecraft != null) minecraft.setScreen(parent);
                return;
            }
        } else {
            openProgress = clamp01(openProgress + dt / OPEN_DURATION);
        }
        updateUiScale(dt);

        int currentPluginRevision = com.pvp_utils.client.plugin.PluginManager.INSTANCE.revision();
        if (pluginRevision != currentPluginRevision) {
            pluginRevision = currentPluginRevision;
            if (!searchText.isBlank()) applySearch();
            invalidateScrollLayout();
        }
        pageTransition.request(activePage());
        if (pageTransition.update(dt)) {
            targetScrollOffset = 0f;
            contentScrollOffset = 0f;
            invalidateScrollLayout();
        }
        BasePage currentPage = pageTransition.displayed();
        exitingFlyouts.removeIf(exit -> exit.module().isFlyoutOpen() || !exit.update(dt));
        long debugUpdateStartNs = Version.DEBUG ? System.nanoTime() : 0L;
        currentPage.update(dt);
        List<SettingModule> currentModules = currentPage.getModules();
        for (SettingModule.FlyoutExit exit : exitingFlyouts) {
            if (!currentModules.contains(exit.module())) exit.module().update(dt);
        }
        SettingModule openFlyout = currentPage.getOpenFlyout();
        if (openFlyout != null) {
            flyoutModule = openFlyout;
        } else if (flyoutModule != null && flyoutModule.getFlyoutProgress() <= 0.01f) {
            flyoutModule = null;
        }
        if (Version.DEBUG) {
            debugLastUpdateMs = (System.nanoTime() - debugUpdateStartNs) / 1_000_000.0;
            updateDebugStats(currentPage);
        }
        float flyoutShiftProgress = 0f;
        for (SettingModule module : currentPage.getModules()) {
            float progress = module.isFlyoutOpen() ? 1f : module.getFlyoutProgress();
            if (progress > flyoutShiftProgress) flyoutShiftProgress = progress;
        }
        for (SettingModule.FlyoutExit exit : exitingFlyouts) {
            flyoutShiftProgress = Math.max(flyoutShiftProgress, exit.progress());
        }
        float shiftTarget = flyoutShiftProgress > 0.001f
                ? maxPanelShift(layoutWidth()) * SettingModule.cubicBezierEase(flyoutShiftProgress)
                : 0f;
        panelShiftX += (shiftTarget - panelShiftX) * Math.min(1f, dt * 12f);
        if (Math.abs(shiftTarget - panelShiftX) < 0.5f) panelShiftX = shiftTarget;

        float animT = easeOutCubic(openProgress);

        float animationScale = 0.88f + 0.12f * animT;
        float visualScale = getUiScale(width, height) * animationScale;
        float cardRadius = 16f / animationScale;
        float layoutMouseX = toLayoutX(mouseX, width, visualScale);
        float layoutMouseY = toLayoutY(mouseY, height, visualScale);
        int layoutWidth = layoutWidth();
        int layoutHeight = layoutHeight();
        float[] l = layout(layoutWidth, layoutHeight);
        updateScrollCache(currentPage, l[17]);
        targetScrollOffset = Math.min(targetScrollOffset, cachedScrollMax);
        contentScrollOffset = lerp(contentScrollOffset, targetScrollOffset, dt * 18f);
        float cardX = l[0], cardY = l[1], cardW = l[2], cardH = l[3];
        float sidebarW = l[4], tabStartY = l[5], tabH = l[6], tabGap = l[7], tabW = l[8];
        float closeX = l[9], closeY = l[10], closeH = l[11], resetY = l[12], resetH = l[13];
        float contentX = l[14], contentY = l[15], contentW = l[16], contentH = l[17];
        updateFlyoutSources(currentPage, l);
        float searchX = cardX + 18f, searchY = cardY + 66f, searchW = sidebarW - 36f, searchH = 28f;

        hoveredTab = -1;
        closeHovered = false;
        resetHovered = false;
        for (int i = 0; i < TAB_KEYS_ZH.length; i++) {
            float ty = tabStartY + i * (tabH + tabGap) + 4f;
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

        float targetIndicatorY = tabStartY + selectedTab * (tabH + tabGap) + 4f;
        if (indicatorY < 0f) indicatorY = targetIndicatorY;
        indicatorY = lerp(indicatorY, targetIndicatorY, dt * 12f);

        BasePage page = currentPage;

        float alpha = animT;
        float cx = width / 2f;
        float cy = height / 2f;
        ClickGuiThemeColors tc = ClickGuiThemeColors.current();

        renderPanelBlur(canvas, RRect.makeXYWH(
                cx + (cardX - layoutWidth * 0.5f) * visualScale,
                cy + (cardY - layoutHeight * 0.5f) * visualScale,
                cardW * visualScale, cardH * visualScale, cardRadius * visualScale));

        for (SettingModule.FlyoutExit exit : exitingFlyouts) {
            float[] r = exit.rect(cardX, cardY);
            float radius = 9f + 3f * SettingModule.cubicBezierEase(exit.progress());
            renderExitingFlyoutBlur(canvas, RRect.makeXYWH(
                    cx + (r[0] - layoutWidth * 0.5f) * visualScale,
                    cy + (r[1] - layoutHeight * 0.5f) * visualScale,
                    r[2] * visualScale, r[3] * visualScale, radius * visualScale), exit.alpha() * alpha);
        }

        float[] fg = flyoutGeometry(cardX, cardY, cardW, cardH);
        for (SettingModule module : page.getModules()) {
            if (module.getFlyoutProgress() > 0.01f) {
                float[] r = module.flyoutMorphRect(fg[0], fg[1], fg[2], fg[3], panelShiftX);
                float radius = 9f + 3f * SettingModule.cubicBezierEase(module.getFlyoutProgress());
                renderPanelBlur(canvas, RRect.makeXYWH(
                        cx + (r[0] - layoutWidth * 0.5f) * visualScale,
                        cy + (r[1] - layoutHeight * 0.5f) * visualScale,
                        r[2] * visualScale, r[3] * visualScale, radius * visualScale));
            }
        }

        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(visualScale, visualScale);
        canvas.translate(-layoutWidth * 0.5f, -layoutHeight * 0.5f);

        if (Config.clickGuiAppearance.shadow) {
            ChromaShadow.drawRounded(canvas, cardX, cardY, cardW, cardH, cardRadius,
                    Config.clickGuiAppearance.shadowStrength * alpha, 0xA0000000);
        }
        float panelAlpha = ClickGuiThemeColors.panelBackgroundAlpha(alpha);
        int surfaceSave = canvas.save();
        try {
            canvas.clipRRect(RRect.makeXYWH(cardX, cardY, cardW, cardH, cardRadius), true);
            cardPaint.setColor(withAlpha(tc.window, panelAlpha));
            canvas.drawRect(Rect.makeXYWH(cardX + sidebarW, cardY, cardW - sidebarW, cardH), cardPaint);
            sidebarPaint.setColor(withAlpha(tc.sidebar, panelAlpha));
            canvas.drawRect(Rect.makeXYWH(cardX, cardY, sidebarW, cardH), sidebarPaint);
        } finally {
            canvas.restoreToCount(surfaceSave);
        }

        float gripLen = 26f;
        float gripThick = 3f;
        float gripInset = 3f;
        float gripRadius = Math.max(2f, cardRadius - gripInset);
        int gripColor = withAlpha(tc.accent, alpha * 0.55f);
        float gripCornerX = cardX + cardW - gripInset;
        float gripCornerY = cardY + cardH - gripInset;
        try (PathBuilder builder = new PathBuilder()) {
            builder.moveTo(gripCornerX - gripLen, gripCornerY);
            builder.lineTo(gripCornerX - gripRadius, gripCornerY);
            builder.arcTo(Rect.makeXYWH(gripCornerX - gripRadius * 2f, gripCornerY - gripRadius * 2f, gripRadius * 2f, gripRadius * 2f), 90f, -90f, false);
            builder.lineTo(gripCornerX, gripCornerY - gripLen);
            try (Path path = builder.detach()) {
                ChromaDraw.strokePath(canvas, path, gripThick, gripColor, PaintStrokeCap.ROUND, PaintStrokeJoin.ROUND);
            }
        }

        canvas.save();
        canvas.clipRRect(RRect.makeXYWH(cardX, cardY, cardW, cardH, cardRadius), true);

        dividerPaint.setColor(withAlpha(tc.border, alpha));
        canvas.drawRect(Rect.makeXYWH(cardX + sidebarW, cardY + 14f, 1f, cardH - 28f), dividerPaint);

        ChromaDraw.text(canvas, "PVPUtils", cardX + 18f, cardY + 22f, 24f, withAlpha(tc.primaryText, alpha), 16f);
        drawDebugOverlay(canvas, cardX, cardY, cardW, alpha, tc);
        ChromaDraw.text(canvas, UiText.t("客户端设置", "Client Settings"), cardX + 18f, cardY + 46f, 16f, withAlpha(tc.secondaryText, alpha), 10f);
        drawSearchBox(canvas, searchX, searchY, searchW, searchH, alpha, dt, tc);

        indicatorPaint.setColor(withAlpha(tc.indicator, ClickGuiThemeColors.panelBackgroundAlpha(alpha)));
        canvas.drawRRect(RRect.makeXYWH(cardX + 12f, indicatorY, tabW, tabH, 8f), indicatorPaint);

        for (int i = 0; i < TAB_KEYS_ZH.length; i++) {
            float tabY = tabStartY + i * (tabH + tabGap);
            if (tabHoverAlpha[i] > 0.01f) {
                hoverPaint.setColor(withAlpha(tc.hoverBackground, ClickGuiThemeColors.panelBackgroundAlpha(alpha * tabHoverAlpha[i])));
                canvas.drawRRect(RRect.makeXYWH(cardX + 12f, tabY + 4f, tabW, tabH, 8f), hoverPaint);
            }
            boolean active = i == selectedTab;
            int iconColor = active ? withAlpha(tc.accent, alpha) : withAlpha(tc.inactiveIcon, alpha);
            int textColor = active ? withAlpha(tc.primaryText, alpha) : withAlpha(tc.inactiveText, alpha);
            ChromaDraw.icon(canvas, TAB_ICONS[i], cardX + 18f, tabY + 4f, tabH, iconColor, 13f, TAB_ICON_FONTS[i]);
            ChromaDraw.text(canvas, UiText.t(TAB_KEYS_ZH[i], TAB_KEYS_EN[i]), cardX + 38f, tabY + 4f, tabH, textColor, 13f);
        }

        int closeBgColor = lerpColor(tc.buttonBackground, tc.dangerHoverBackground, closeHoverAlpha);
        int closeTextColor = lerpColor(tc.buttonText, tc.dangerHoverText, closeHoverAlpha);
        int resetBgColor = lerpColor(tc.buttonBackground, tc.dangerHoverBackground, resetHoverAlpha);
        int resetTextColor = lerpColor(tc.buttonText, tc.dangerHoverText, resetHoverAlpha);
        resetBgPaint.setColor(withAlpha(resetBgColor, ClickGuiThemeColors.panelBackgroundAlpha(alpha)));
        canvas.drawRRect(RRect.makeXYWH(closeX, resetY, tabW, resetH, 8f), resetBgPaint);
        String resetText = resetConfirm ? UiText.t("再次点击以确认", "Click Again to Confirm") : UiText.t("重置所有设置", "Reset All Settings");
        String resetIcon = "\uE042";
        if (!resetText.equals(cachedResetText)) {
            cachedResetText = resetText;
            cachedResetTextWidth = ChromaDraw.textWidth(resetText, 12f);
        }
        float resetTotalW = resetIconWidth + 6f + cachedResetTextWidth;
        float resetStartX = closeX + (tabW - resetTotalW) / 2f;
        ChromaDraw.icon(canvas, resetIcon, resetStartX, resetY, resetH, withAlpha(resetTextColor, alpha), 13f, ChromaDraw.IconSet.MATERIAL_SYMBOLS);
        ChromaDraw.text(canvas, resetText, resetStartX + resetIconWidth + 6f, resetY, resetH, withAlpha(resetTextColor, alpha), 12f);

        closeBgPaint.setColor(withAlpha(closeBgColor, ClickGuiThemeColors.panelBackgroundAlpha(alpha)));
        canvas.drawRRect(RRect.makeXYWH(closeX, closeY, tabW, closeH, 8f), closeBgPaint);
        String closeText = UiText.t("× 关闭", "× Close");
        if (!closeText.equals(cachedCloseText)) {
            cachedCloseText = closeText;
            cachedCloseTextWidth = ChromaDraw.textWidth(closeText, 12f);
        }
        ChromaDraw.text(canvas, closeText, closeX + (tabW - cachedCloseTextWidth) / 2f, closeY, closeH, withAlpha(closeTextColor, alpha), 12f);

        float pageAlpha = alpha * pageTransition.opacity();
        float pageOffset = pageTransition.offsetX();
        float pageMouseX = pageTransition.isInteractive() ? layoutMouseX - pageOffset : Float.NEGATIVE_INFINITY;
        int contentSave = canvas.save();
        try {
            canvas.clipRect(Rect.makeXYWH(contentX, contentY, contentW, contentH));
            canvas.translate(pageOffset, 0f);
            ChromaDraw.text(canvas, page.getTitle(), contentX + 18f, contentY + 8f, 26f, withAlpha(tc.primaryText, pageAlpha), 18f);
            ChromaDraw.text(canvas, page.getSubtitle(), contentX + 18f, contentY + 34f, 16f, withAlpha(tc.secondaryText, pageAlpha), 10f);

            float clipTop = contentY + 54f;
            float clipBottom = contentY + contentH;
            int listClip = canvas.save();
            try {
                canvas.clipRect(Rect.makeXYWH(contentX, clipTop, contentW, clipBottom - clipTop));
                float moduleStartY = contentY + 54f;
                float listHeight = contentH - 54f - FOOTER_H;
                float pageX = page instanceof PluginPage ? contentX : contentX + 10f;
                float pageWidth = page instanceof PluginPage ? contentW : contentW - 40f;
                int listSave = canvas.save();
                try {
                    canvas.clipRect(Rect.makeXYWH(contentX, moduleStartY, contentW, Math.max(0f, listHeight)));
                    page.draw(canvas, pageX, moduleStartY, pageWidth, listHeight, pageAlpha, contentScrollOffset, pageMouseX, layoutMouseY);
                    drawScrollbar(canvas, page, contentX, contentY, contentW, contentH, pageAlpha, tc);
                } finally {
                    canvas.restoreToCount(listSave);
                }
                drawDescriptionFooter(canvas, page, contentX, contentY, contentW, contentH, pageAlpha, tc);
            } finally {
                canvas.restoreToCount(listClip);
            }
        } finally {
            canvas.restoreToCount(contentSave);
        }

        canvas.restore();

        for (SettingModule.FlyoutExit exit : exitingFlyouts) {
            exit.draw(canvas, cardX, cardY, alpha);
        }
        float[] g = flyoutGeometry(cardX, cardY, cardW, cardH);
        for (SettingModule module : page.getModules()) {
            if (module.getFlyoutProgress() > 0.01f) {
                module.draw(canvas, g[0], g[1], g[2], g[3], panelShiftX, alpha, layoutMouseX, layoutMouseY);
            }
        }

        canvas.restore();

        if (Version.DEBUG) {
            debugLastDrawMs = (System.nanoTime() - debugDrawStartNs) / 1_000_000.0;
        }
    }

    private void drawScrollbar(Canvas canvas, BasePage page, float contentX, float contentY, float contentW, float contentH, float alpha, ClickGuiThemeColors tc) {
        updateScrollCache(page, contentH);
        if (cachedContentTotalHeight <= cachedScrollAreaHeight) return;

        float trackX = scrollbarTrackX(contentX, contentW);
        float trackTop = scrollbarTrackTop(contentY);
        float trackH = scrollbarTrackH(contentH);
        float thumbH = Math.max(20f, trackH * cachedScrollAreaHeight / cachedContentTotalHeight);
        float maxScroll = Math.max(1f, cachedScrollMax);
        float progress = Math.min(1f, contentScrollOffset / maxScroll);
        float thumbTop = trackTop + (trackH - thumbH) * progress;
        thumbTop = Math.min(thumbTop, trackTop + trackH - thumbH);

        scrollbarTrackPaint.setColor(withAlpha(tc.scrollbarTrack, alpha * 0.5f));
        canvas.drawRRect(RRect.makeXYWH(trackX, trackTop, 4f, trackH, 2f), scrollbarTrackPaint);
        scrollbarThumbPaint.setColor(withAlpha(tc.scrollbarThumb, alpha));
        canvas.drawRRect(RRect.makeXYWH(trackX, thumbTop, 4f, thumbH, 2f), scrollbarThumbPaint);
    }

    private void drawDescriptionFooter(Canvas canvas, BasePage page, float contentX, float contentY, float contentW, float contentH, float alpha, ClickGuiThemeColors tc) {
        String source = footerSource(page);
        if (source.isEmpty()) return;
        float maxWidth = Math.max(0f, contentW - 36f);
        if (!source.equals(cachedFooterSource) || Math.abs(maxWidth - cachedFooterMaxWidth) > 0.5f) {
            cachedFooterSource = source;
            cachedFooterMaxWidth = maxWidth;
            cachedFooterRender = truncate(source, maxWidth, 10f);
        }
        float ascent = ChromaDraw.textMetrics(10f).getAscent();
        float lineHeight = ChromaDraw.textMetrics(10f).getDescent() - ascent;
        ChromaDraw.text(canvas, cachedFooterRender, contentX + 18f,
                (contentY + contentH - FOOTER_H * 0.5f + 3f) + ascent, lineHeight,
                withAlpha(tc.secondaryText, alpha), 10f);
    }

    private String footerSource(BasePage page) {
        SettingModule hovered = page.getHoveredModule();
        if (hovered != null && hovered.subtitle != null && !hovered.subtitle.isEmpty()) {
            return hovered.subtitle;
        }
        SettingModule flyout = flyoutModule;
        if (flyout != null && flyout.getFlyoutProgress() > 0.01f && flyout.subtitle != null) {
            return flyout.subtitle;
        }
        return "";
    }

    private float maxCardWidth() {
        float scale = Math.max(0.01f, getUiScale(this.width, this.height));
        return Math.max(MIN_CARD_W, this.width / scale - SCREEN_MARGIN);
    }

    private float maxCardHeight() {
        float scale = Math.max(0.01f, getUiScale(this.width, this.height));
        return Math.max(MIN_CARD_H, this.height / scale - SCREEN_MARGIN);
    }

    private float effectiveFlyoutWidth() {
        return Math.min(MAX_FLYOUT_W, Math.max(flyoutW,
                flyoutModule == null ? MIN_FLYOUT_W : flyoutModule.preferredFlyoutWidth()));
    }

    private float[] flyoutGeometry(float cardX, float cardY, float cardW, float cardH) {
        float scale = Math.max(0.01f, getVisualScale(this.width, this.height));
        float halfWidth = Math.max(0f, this.width * 0.5f - 8f) / scale;
        int layoutW = layoutWidth();
        float leftLimit = layoutW * 0.5f - halfWidth;
        float rightLimit = layoutW * 0.5f + halfWidth;
        float targetX = cardX + cardW + FLYOUT_GAP;
        float available = rightLimit - targetX;
        float screenW = Math.max(0f, rightLimit - leftLimit);
        float minW = Math.min(160f, screenW);
        float w = Math.min(Math.min(effectiveFlyoutWidth(), screenW), Math.max(minW, available));
        float x = Math.max(leftLimit, Math.min(targetX, rightLimit - w));
        return new float[]{x, cardY, w, cardH};
    }

    private void closeFlyout() {
        for (BasePage page : pages) page.closeFlyouts();
    }

    private void updateFlyoutSources(BasePage page, float[] geometry) {
        page.setPanelShift(panelShiftX);
        float y = geometry[15] + 54f + (page instanceof PluginPage ? 40f : 0f);
        page.updateFlyoutSources(geometry[14] + 10f, y, geometry[16] - 40f, contentScrollOffset);
    }

    private void detachPageFlyouts(BasePage page, boolean updateSources) {
        float[] geometry = layout(layoutWidth(), layoutHeight());
        if (updateSources) updateFlyoutSources(page, geometry);
        float[] target = flyoutGeometry(geometry[0], geometry[1], geometry[2], geometry[3]);
        for (SettingModule module : page.getModules()) {
            SettingModule.FlyoutExit exit = module.captureFlyoutExit(geometry[0], geometry[1],
                    target[0], target[1], target[2], target[3], panelShiftX);
            if (exit != null) {
                exitingFlyouts.removeIf(previous -> previous.module() == module);
                exitingFlyouts.add(exit);
            }
            module.setFlyoutOpen(false);
            module.resetFlyout();
        }
        flyoutModule = null;
        draggingInContent = false;
        draggingScrollbar = false;
        resizeMode = 0;
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

    private void drawSearchBox(Canvas canvas, float x, float y, float width, float height, float alpha, float dt, ClickGuiThemeColors tc) {
        int background = lerpColor(tc.searchBackground, tc.searchFocusedBackground, searchFocusAlpha);
        searchBgPaint.setColor(withAlpha(background, ClickGuiThemeColors.panelBackgroundAlpha(alpha)));
        canvas.drawRRect(RRect.makeXYWH(x, y, width, height, 7f), searchBgPaint);

        ChromaDraw.icon(canvas, "\uE8B6", x + 9f, y, height, withAlpha(tc.searchIcon, alpha), 12f, ChromaDraw.IconSet.MATERIAL_SYMBOLS);
        float textX = x + 28f;
        float textW = Math.max(1f, width - 36f);
        boolean empty = searchText.isEmpty();
        String display = empty ? UiText.t("\u8F93\u5165\u4EE5\u67E5\u627E...", "Type to search...") : searchText;
        float realTextWidth = ChromaDraw.textWidth(searchText, 10f);
        float targetOffset = empty ? 0f : Math.max(0f, realTextWidth - textW + 3f);
        searchTextOffset = lerp(searchTextOffset, targetOffset, dt * 16f);

        canvas.save();
        canvas.clipRect(Rect.makeXYWH(textX, y + 2f, textW, height - 4f));
        ChromaDraw.text(canvas, display, textX - (empty ? 0f : searchTextOffset), y, height, withAlpha(empty ? tc.searchTextPlaceholder : tc.searchText, alpha), 10f);
        if (searchFocused) {
            float cursorPulse = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(searchCursorTime * 6f));
            float cursorX = textX + Math.min(textW - 1f, Math.max(0f, realTextWidth - searchTextOffset));
            searchLinePaint.setColor(withAlpha(tc.searchCursor, alpha * cursorPulse));
            canvas.drawRect(Rect.makeXYWH(cursorX, y + 7f, 1f, 14f), searchLinePaint);
        }
        canvas.restore();

        float linePulse = 0.3f + 0.7f * (0.5f + 0.5f * (float) Math.sin(searchCursorTime * 6f));
        searchLinePaint.setColor(withAlpha(tc.searchCursor, alpha * searchFocusAlpha * linePulse));
        canvas.drawRect(Rect.makeXYWH(x + 8f, y + height - 2f, width - 16f, 1f), searchLinePaint);
    }

    private void applySearch() {
        pageTransition.displayed().releaseDrag();
        detachPageFlyouts(pageTransition.displayed(), true);
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
        pageTransition.reset(activePage());
        invalidateScrollLayout();
    }

    private BasePage activePage() {
        return searchResultsPage == null ? pages.get(selectedTab) : searchResultsPage;
    }

    private void selectPage(int index) {
        BasePage previous = pageTransition.displayed();
        previous.releaseDrag();
        detachPageFlyouts(previous, true);
        setSearchFocused(false);
        searchText = "";
        searchTextOffset = 0f;
        searchResultsPage = null;
        for (BasePage page : pages) page.setSearchQuery("");
        selectedTab = index;
        if (pages.get(selectedTab) instanceof AddServerPage) pages.set(selectedTab, new ToolPage());
        pageTransition.request(activePage());
        targetScrollOffset = contentScrollOffset;
        draggingWindow = false;
        invalidateScrollLayout();
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
        return contentH - 60f - 8f - FOOTER_H;
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
        if (SettingTextBox.keyPressed(event)) {
            invalidateScrollLayout();
            return true;
        }
        if (flyoutModule != null && flyoutModule.isFlyoutOpen() && event.key() == GLFW.GLFW_KEY_ESCAPE) {
            flyoutModule.setFlyoutOpen(false);
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
        if (SettingTextBox.charTyped(event)) {
            invalidateScrollLayout();
            return true;
        }
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

    @Override
    protected void renderBlurredBackground(GuiGraphics graphics) {
    }

    @Override
    protected void renderMenuBackground(GuiGraphics graphics) {
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override public void onClose() {
        SettingTextBox.clearFocus();
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
        float[] l = layout(layoutWidth(), layoutHeight());
        float cardX = l[0];
        float sidebarW = l[4], tabStartY = l[5], tabH = l[6], tabGap = l[7], tabW = l[8];
        float closeX = l[9], closeY = l[10], closeH = l[11];
        float resetY = l[12], resetH = l[13];
        float contentX = l[14], contentY = l[15], contentW = l[16], contentH = l[17];
        float searchX = cardX + 18f, searchY = l[1] + 66f, searchW = sidebarW - 36f, searchH = 28f;
        BasePage page = activePage();

        if (button == 0 && openProgress > 0.99f) {
            float[] g = flyoutModule != null && flyoutModule.isFlyoutOpen() && flyoutModule.getFlyoutProgress() > 0.5f ? flyoutGeometry(cardX, l[1], l[2], l[3]) : null;
            if (g != null && mx >= g[0] + g[2] - 10f && mx <= g[0] + g[2] + 8f && my >= g[1] + g[3] - 10f && my <= g[1] + g[3] + 8f) {
                resizeMode = 3;
                resizeScale = visualScale;
                resizeStartMouseX = event.x();
                resizeStartMouseY = event.y();
                resizeStartFlyoutW = flyoutW;
                return true;
            }
            if (mx >= cardX + l[2] - 10f && mx <= cardX + l[2] + 8f && my >= l[1] + l[3] - 10f && my <= l[1] + l[3] + 8f) {
                resizeMode = 1;
                resizeScale = visualScale;
                resizeStartMouseX = event.x();
                resizeStartMouseY = event.y();
                resizeStartW = cardW;
                resizeStartH = cardH;
                return true;
            }
        }

        if (pageTransition.isInteractive() && button == 0 && flyoutModule != null && flyoutModule.isFlyoutOpen() && flyoutModule.getFlyoutProgress() >= 0.5f) {
            float[] g = flyoutGeometry(cardX, l[1], l[2], l[3]);
            if (mx >= g[0] && mx <= g[0] + g[2] && my >= g[1] && my <= g[1] + g[3]) {
                flyoutModule.flyoutClick(mx, my, g[0], g[1], g[2], g[3], button);
                invalidateScrollLayout();
                return true;
            }
        }

        if (button == 0 && openProgress > 0.99f
                && mx >= cardX && mx <= cardX + l[2]
                && my >= l[1] && my <= l[1] + DRAG_HANDLE_H) {
            draggingWindow = true;
            windowDragStartX = event.x();
            windowDragStartY = event.y();
            windowDragStartOffsetX = cardOffsetX;
            windowDragStartOffsetY = cardOffsetY;
            return true;
        }

        if (button == 0 && mx >= searchX && mx <= searchX + searchW && my >= searchY && my <= searchY + searchH) {
            closeFlyout();
            SettingTextBox.clearFocus();
            setSearchFocused(true);
            searchCursorTime = 0f;
            invalidateScrollLayout();
            return true;
        }
        setSearchFocused(false);
        SettingTextBox.clearFocus();

        for (int i = 0; i < TAB_KEYS_ZH.length; i++) {
            float ty = tabStartY + i * (tabH + tabGap) + 4f;
            if (mx >= cardX + 12f && mx <= cardX + 12f + tabW && my >= ty && my <= ty + tabH) {
                if (button == 0) {
                    if (i != selectedTab || searchResultsPage != null
                            || pages.get(selectedTab) instanceof AddServerPage) selectPage(i);
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
                rebuildCurrentPage();
            } else {
                resetConfirm = true;
            }
            invalidateScrollLayout();
            return true;
        }

        resetConfirm = false;

        if (mx >= contentX && mx <= contentX + contentW && my >= contentY && my <= contentY + contentH) {
            if (!pageTransition.isInteractive()) return true;
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
            if (my < moduleStartY || my > contentY + contentH - FOOTER_H) return false;
            updateFlyoutSources(page, l);
            boolean hit = page.onClick(mx, my, contentX + 10f, moduleStartY, contentW - 40f, contentScrollOffset, button);
            updateScrollCache(page, contentH);
            SettingModule open = page.getOpenFlyout();
            if (open != null) {
                flyoutModule = open;
                exitingFlyouts.removeIf(exit -> exit.module() == open);
            }
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
        if (resizeMode != 0) {
            float scale = Math.max(0.01f, resizeScale);
            float deltaX = (float) (event.x() - resizeStartMouseX) / scale;
            float deltaY = (float) (event.y() - resizeStartMouseY) / scale;
            if (resizeMode == 1) {
                cardW = Math.max(MIN_CARD_W, Math.min(resizeStartW + deltaX * 2f, maxCardWidth()));
                cardH = Math.max(MIN_CARD_H, Math.min(resizeStartH + deltaY * 2f, maxCardHeight()));
            } else if (resizeMode == 3) {
                flyoutW = Math.max(MIN_FLYOUT_W, Math.min(resizeStartFlyoutW + deltaX, MAX_FLYOUT_W));
            }
            clampCardOffset();
            invalidateScrollLayout();
            return true;
        }
        if (draggingWindow) {
            float scale = Math.max(0.01f, getVisualScale(this.width, this.height));
            cardOffsetX = windowDragStartOffsetX + (float) (event.x() - windowDragStartX) / scale;
            cardOffsetY = windowDragStartOffsetY + (float) (event.y() - windowDragStartY) / scale;
            clampCardOffset();
            invalidateScrollLayout();
            return true;
        }
        if (!pageTransition.isInteractive()) return false;
        if (draggingScrollbar) {
            float visualScale = getVisualScale(this.width, this.height);
            float my = toLayoutY(event.y(), this.height, visualScale);
            float[] l = layout(layoutWidth(), layoutHeight());
            BasePage page = activePage();
            setScrollFromScrollbar(page, my, l[15], l[17]);
            contentScrollOffset = targetScrollOffset;
            invalidateScrollLayout();
            return true;
        }
        if (flyoutModule != null && flyoutModule.isFlyoutOpen() && flyoutModule.getFlyoutProgress() > 0.01f) {
            float visualScale = getVisualScale(this.width, this.height);
            float mx = toLayoutX(event.x(), this.width, visualScale);
            float my = toLayoutY(event.y(), this.height, visualScale);
            float[] l = layout(layoutWidth(), layoutHeight());
            float[] g = flyoutGeometry(l[0], l[1], l[2], l[3]);
            flyoutModule.flyoutDrag(mx, my, g[0], g[1], g[2], g[3]);
        }
        if (draggingInContent) {
            float visualScale = getVisualScale(this.width, this.height);
            float mx = toLayoutX(event.x(), this.width, visualScale);
            float my = toLayoutY(event.y(), this.height, visualScale);
            float[] l = layout(layoutWidth(), layoutHeight());
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
        draggingWindow = false;
        if (resizeMode != 0) {
            Config.clickGuiCardW = cardW;
            Config.clickGuiCardH = cardH;
            Config.clickGuiFlyoutW = flyoutW;
            resizeMode = 0;
            invalidateScrollLayout();
            return true;
        }
        draggingInContent = false;
        draggingScrollbar = false;
        pageTransition.displayed().releaseDrag();
        if (activePage() != pageTransition.displayed()) activePage().releaseDrag();
        invalidateScrollLayout();
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        if (!pageTransition.isInteractive()) return false;
        float visualScale = getVisualScale(this.width, this.height);
        float layoutMx = toLayoutX(mx, this.width, visualScale);
        float layoutMy = toLayoutY(my, this.height, visualScale);
        float[] l = layout(layoutWidth(), layoutHeight());
        float contentX = l[14], contentY = l[15], contentW = l[16], contentH = l[17];

        if (flyoutModule != null && flyoutModule.isFlyoutOpen() && flyoutModule.getFlyoutProgress() > 0.01f) {
            float[] g = flyoutGeometry(l[0], l[1], l[2], l[3]);
            if (layoutMx >= g[0] && layoutMx <= g[0] + g[2] && layoutMy >= g[1] && layoutMy <= g[1] + g[3]) {
                flyoutModule.scrollFlyout((float)(-vScroll * 16f * Math.max(0.2f, Config.clickGuiScrollSpeed)));
                return true;
            }
        }

        if (layoutMx >= contentX && layoutMx <= contentX + contentW && layoutMy >= contentY && layoutMy <= contentY + contentH) {
            BasePage page = activePage();
            updateScrollCache(page, contentH);
            targetScrollOffset = Math.max(0f, Math.min(cachedScrollMax, targetScrollOffset + (float)(-vScroll * 16f * Math.max(0.2f, Config.clickGuiScrollSpeed))));
            invalidateScrollLayout();
            return true;
        }
        return false;
    }
}
