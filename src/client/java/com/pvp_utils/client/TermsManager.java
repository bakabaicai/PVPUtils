package com.pvp_utils.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TermsManager {
    private static final String TERMS_TEXT = """
            使用须知

            本模组内置了部分争议性功能，虽然他们都只是为了辅助PVP而生，并非作弊功能，并且也对其进行了平衡性调整，但是部分服务器仍有可能将部分功能视为违规功能并处以封禁处理，所以您随时可以选择完整版或者受限版本（后续可以在“其他”分页内重新调整，若选择完整版，则您视为同意此协议）

            Terms of Use

            This mod includes some controversial features. They are meant to assist PvP rather than function as cheats, and they have been balanced as much as possible. However, some servers may still treat certain features as violations and punish players.

            You can choose the full or restricted version at any time in Misc Settings. Choosing the full version means you agree to this notice.
            """;

    private TermsManager() {}

    public static Path externalPath() {
        return FabricLoader.getInstance().getGameDir().resolve("PVPUtils/Terms of Use.txt");
    }

    public static void ensure() {
        try {
            Path path = externalPath();
            Files.createDirectories(path.getParent());
            try (InputStream in = TermsManager.class.getResourceAsStream("/docs/Terms of Use.txt")) {
                if (in != null) {
                    Files.copy(in, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    return;
                }
            }
            Files.writeString(path, TERMS_TEXT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void open() {
        ensure();
        Util.getPlatform().openPath(externalPath());
    }
}
