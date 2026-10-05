package com.pvp_utils.client.plugin;

import com.pvp_utils.PVPUtils;
import com.pvp_utils.client.modules.impl.Render.NotificationOverlay;
import com.pvp_utils.client.gui.clickgui.UiText;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PluginDirectory {
    private PluginDirectory() {
    }

    public static Path path() {
        return FabricLoader.getInstance().getGameDir().resolve("PVPUtils").resolve("plugin");
    }

    public static boolean ensureExists() {
        try {
            Files.createDirectories(path());
            return true;
        } catch (IOException | SecurityException e) {
            PVPUtils.LOGGER.error("Failed to create plugin directory: {}", path(), e);
            return false;
        }
    }

    public static void open() {
        if (ensureExists()) {
            Util.getPlatform().openPath(path());
        } else {
            NotificationOverlay.getInstance().show(UiText.t("插件目录创建失败，请查看日志", "Failed to create plugin directory; check the log"));
        }
    }
}
