package com.pvp_utils.client.gui.clickgui.theme;

import com.pvp_utils.Config;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ClickGuiThemeManager {
    private static final Map<String, ClickGuiTheme> THEMES = new LinkedHashMap<>();
    private static final ClickGuiTheme FALLBACK = new WhiteTheme();
    private static ClickGuiTheme currentTheme;

    static {
        register(FALLBACK);
        register(new DarkTheme());
        register(new GrayTheme());
        currentTheme = FALLBACK;
    }

    private ClickGuiThemeManager() {
    }

    public static void register(ClickGuiTheme theme) {
        if (theme == null || theme.id() == null || theme.id().isBlank()) return;
        THEMES.put(theme.id(), theme);
    }

    public static Collection<ClickGuiTheme> themes() {
        return List.copyOf(THEMES.values());
    }

    public static ClickGuiTheme current() {
        return currentTheme;
    }

    public static boolean select(String id) {
        ClickGuiTheme theme = THEMES.get(id);
        if (theme == null) return false;
        currentTheme = theme;
        return true;
    }

    public static Optional<ClickGuiTheme> find(String id) {
        return Optional.ofNullable(THEMES.get(id));
    }

    /** 当前选中主题的 id。 */
    public static String currentId() {
        return currentTheme == null ? FALLBACK.id() : currentTheme.id();
    }

    /** 根据 {@link Config#clickGuiTheme} 应用已保存的主题选择。 */
    public static void applyConfig() {
        String id = Config.clickGuiTheme;
        if (id == null || id.isBlank() || !select(id)) {
            currentTheme = FALLBACK;
            Config.clickGuiTheme = FALLBACK.id();
            Config.save();
        }
    }

    /** 选择主题并持久化到配置文件。 */
    public static boolean selectAndSave(String id) {
        if (!select(id)) return false;
        Config.clickGuiTheme = id;
        Config.save();
        return true;
    }
}
