package com.pvp_utils.client.gui.clickgui.theme;

import com.pvp_utils.Config;

/**
 * 颜色派生层：将 {@link ClickGuiThemePalette} 的 9 个核心色映射到 GUI 全部具体色槽。
 * 次要颜色（指示条、悬停、滚动条、搜索框、危险按钮等）根据主题明暗自动派生，
 * 使每个主题只需声明 9 个核心色即可完整覆盖所有 GUI 选项。
 *
 * <p>所有颜色均为 RGB（不含 alpha），使用方通过 {@code withAlpha(color, alpha)} 叠加透明度。</p>
 */
public final class ClickGuiThemeColors {

    // —— 核心色（直接来自调色板） ——
    public final int window;
    public final int sidebar;
    public final int content;
    public final int module;
    public final int subModule;
    public final int accent;
    public final int primaryText;
    public final int secondaryText;
    public final int border;

    // —— 派生色：文字 ——
    public final int mutedText;        // 箭头、调试次级文本等更弱化的文字
    public final int inactiveText;      // 未选中 Tab 文字
    public final int inactiveIcon;      // 未选中 Tab 图标
    public final int subModuleText;     // 子模块标题文字

    // —— 派生色：背景/指示 ——
    public final int indicator;        // 选中 Tab 指示条背景
    public final int hoverBackground;  // Tab/控件悬停背景
    public final int buttonBackground;  // 普通按钮背景
    public final int buttonText;        // 普通按钮文字

    // —— 派生色：滚动条 ——
    public final int scrollbarTrack;
    public final int scrollbarThumb;

    // —— 派生色：搜索框 ——
    public final int searchBackground;
    public final int searchFocusedBackground;
    public final int searchIcon;
    public final int searchCursor;
    public final int searchText;
    public final int searchTextPlaceholder;

    // —— 派生色：危险按钮（关闭/重置悬停态） ——
    public final int dangerHoverBackground;
    public final int dangerHoverText;

    public final boolean dark;

    private ClickGuiThemeColors(
            int window, int sidebar, int content, int module, int subModule,
            int accent, int primaryText, int secondaryText, int border,
            int mutedText, int inactiveText, int inactiveIcon, int subModuleText,
            int indicator, int hoverBackground, int buttonBackground, int buttonText,
            int scrollbarTrack, int scrollbarThumb,
            int searchBackground, int searchFocusedBackground, int searchIcon,
            int searchCursor, int searchText, int searchTextPlaceholder,
            int dangerHoverBackground, int dangerHoverText,
            boolean dark) {
        this.window = window;
        this.sidebar = sidebar;
        this.content = content;
        this.module = module;
        this.subModule = subModule;
        this.accent = accent;
        this.primaryText = primaryText;
        this.secondaryText = secondaryText;
        this.border = border;
        this.mutedText = mutedText;
        this.inactiveText = inactiveText;
        this.inactiveIcon = inactiveIcon;
        this.subModuleText = subModuleText;
        this.indicator = indicator;
        this.hoverBackground = hoverBackground;
        this.buttonBackground = buttonBackground;
        this.buttonText = buttonText;
        this.scrollbarTrack = scrollbarTrack;
        this.scrollbarThumb = scrollbarThumb;
        this.searchBackground = searchBackground;
        this.searchFocusedBackground = searchFocusedBackground;
        this.searchIcon = searchIcon;
        this.searchCursor = searchCursor;
        this.searchText = searchText;
        this.searchTextPlaceholder = searchTextPlaceholder;
        this.dangerHoverBackground = dangerHoverBackground;
        this.dangerHoverText = dangerHoverText;
        this.dark = dark;
    }

    /**
     * 根据主题解析全部颜色槽。
     */
    public static ClickGuiThemeColors of(ClickGuiTheme theme) {
        if (theme == null) {
            theme = new WhiteTheme();
        }
        ClickGuiThemePalette p = theme.palette();
        boolean dark = luminance(p.primaryText()) > luminance(p.windowBackground());

        int primaryText = rgb(p.primaryText());
        int secondaryText = rgb(p.secondaryText());
        int window = rgb(p.windowBackground());
        int sidebar = rgb(p.sidebarBackground());
        int accent = rgb(p.accent());
        int border = rgb(p.border());

        // 文字派生：弱化与次级
        int mutedText = mix(secondaryText, dark ? window : 0x000000, dark ? 0.35f : 0.25f);
        int inactiveText = mix(primaryText, dark ? window : 0x000000, dark ? 0.45f : 0.40f);
        int inactiveIcon = mix(secondaryText, dark ? window : 0x000000, dark ? 0.25f : 0.20f);
        int subModuleText = mix(primaryText, dark ? window : 0x000000, dark ? 0.40f : 0.35f);

        // 指示/悬停：accent 极淡地叠加在窗口底色上
        int indicator = mix(window, accent, dark ? 0.22f : 0.20f);
        int hoverBackground = mix(window, accent, dark ? 0.12f : 0.15f);

        // 按钮：取模块底色附近
        int buttonBackground = mix(rgb(p.moduleBackground()), dark ? window : 0x000000, dark ? 0.20f : 0.06f);
        int buttonText = secondaryText;

        // 滚动条
        int scrollbarTrack = mix(border, window, 0.5f);
        int scrollbarThumb = mix(secondaryText, dark ? window : 0x000000, dark ? 0.20f : 0.10f);

        // 搜索框
        int searchBackground = mix(sidebar, accent, dark ? 0.06f : 0.03f);
        int searchFocusedBackground = mix(sidebar, accent, dark ? 0.18f : 0.10f);
        int searchIcon = secondaryText;
        int searchCursor = accent;
        int searchText = primaryText;
        int searchTextPlaceholder = mix(secondaryText, window, 0.15f);

        // 危险按钮（关闭/重置悬停）：语义红色，按明暗自适应
        int dangerHoverBackground = dark ? 0x3D2226 : 0xFFE5E5;
        int dangerHoverText = dark ? 0xFF6B6B : 0xCC2222;

        return new ClickGuiThemeColors(
                window, sidebar, rgb(p.contentBackground()), rgb(p.moduleBackground()), rgb(p.subModuleBackground()),
                accent, primaryText, secondaryText, border,
                mutedText, inactiveText, inactiveIcon, subModuleText,
                indicator, hoverBackground, buttonBackground, buttonText,
                scrollbarTrack, scrollbarThumb,
                searchBackground, searchFocusedBackground, searchIcon,
                searchCursor, searchText, searchTextPlaceholder,
                dangerHoverBackground, dangerHoverText,
                dark);
    }

    /** 取当前已选主题的颜色。 */
    public static ClickGuiThemeColors current() {
        return of(ClickGuiThemeManager.current());
    }

    /** Lets blurred game content remain visible through ClickGUI background surfaces. */
    public static float panelBackgroundAlpha(float alpha) {
        return Config.clickGuiPanelBlur ? alpha * 0.46f : alpha;
    }

    // —— 颜色数学工具 ——

    /** 取 RGB（丢弃 alpha 字节）。 */
    private static int rgb(int argb) {
        return argb & 0x00FFFFFF;
    }

    /** 相对亮度（0~1）。 */
    private static float luminance(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f;
    }

    /** 在 a 与 b 之间按 t 线性混合（仅 RGB 通道）。 */
    private static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = Math.round(ar + (br - ar) * t);
        int g = Math.round(ag + (bg - ag) * t);
        int bl = Math.round(ab + (bb - ab) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
