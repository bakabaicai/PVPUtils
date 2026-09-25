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

            本模组内置了部分争议性功能，虽然他们只是为了辅助PVP而生，并非作弊功能，并且也对其进行了平衡性调整，但是部分服务器仍有可能将部分功能视为违规功能并处以封禁处理，所以当前版本仅提供受限模式，争议功能已隐藏。
            音乐平台免责声明：本 Mod 只调用各音乐平台客户端可见的公开接口，不破解、不绕过付费内容，也不提供任何音频文件；所有音频由各平台 CDN 直发。使用者需遵守各平台服务条款与所在地版权法规。本 Mod 与各音乐平台无隶属或合作关系，所有平台名称与商标归各自权利人所有。

            Terms of Use

            This mod includes some controversial features. They are meant to assist PvP rather than function as cheats, and they have been balanced as much as possible. However, some servers may still treat certain features as violations and punish players.

            This version of PVPUtils runs in Restricted Mode only: controversial features are hidden.
            Music Platform Disclaimer: This mod only calls the public APIs visible to each music platform's client. It does not crack or bypass paid content, and provides no audio files; all audio is delivered directly by each platform's CDN. Users must comply with each platform's terms of service and local copyright laws. This mod has no affiliation with any music platform; all platform names and trademarks belong to their respective rights holders.
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
