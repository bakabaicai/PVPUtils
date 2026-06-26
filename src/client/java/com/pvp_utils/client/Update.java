package com.pvp_utils.client;

import com.pvp_utils.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.function.Consumer;

public final class Update {
    private static final String UPDATE_URL = "https://raw.githubusercontent.com/bakabaicai/PVPUtils/main/Update";
    private static final String CURSEFORGE_URL = "https://www.curseforge.com/minecraft/mc-mods/pvputils";
    private static final String MODRINTH_URL = "https://modrinth.com/mod/pvp_utils";
    private static final String GITHUB_URL = "https://github.com/bakabaicai/PVPUtils/releases";
    private static final String QQ_GROUP_NUMBER = "947119584";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static volatile UpdateResult cachedResult;
    private static volatile boolean shown;

    private Update() {}

    public static void startAutoCheck() {
        runCheck(false, null, Update::deliverToPlayer);
    }

    public static void startManualCheck() {
        runCheck(true, Update::deliverToPlayer, Update::deliverToPlayer);
    }

    public static void tick(Minecraft client) {
        if (shown || client == null || client.player == null || client.level == null) return;
        UpdateResult result = cachedResult;
        if (result == null || !result.hasUpdate()) return;
        shown = true;
        client.player.displayClientMessage(result.updateAvailableMessage(), false);
    }

    public static MutableComponent checkingMessage() {
        return Component.literal(Config.isChinese ? "正在检查更新..." : "Checking for updates...")
                .withStyle(ChatFormatting.GRAY);
    }

    public static MutableComponent errorMessage(String reason) {
        String text = Config.isChinese
                ? "更新检查失败，原因：" + normalizeReason(reason, true)
                : "Update check failed, reason: " + normalizeReason(reason, false);
        return Component.literal(text).withStyle(ChatFormatting.RED);
    }

    public static void copyQqGroupNumber() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        client.keyboardHandler.setClipboard(QQ_GROUP_NUMBER);
        deliverToPlayer(qqClipboardMessage());
    }

    private static void runCheck(boolean manual, Consumer<MutableComponent> onResult, Consumer<MutableComponent> onError) {
        Thread thread = new Thread(() -> {
            try {
                UpdateResult result = fetchResult();
                cachedResult = result;
                if (manual && onResult != null) {
                    onResult.accept(result.manualResultMessage());
                }
            } catch (Exception e) {
                cachedResult = null;
                if (onError != null) {
                    onError.accept(errorMessage(e.getMessage()));
                }
            }
        }, manual ? "pvp-utils-update-manual" : "pvp-utils-update-auto");
        thread.setDaemon(true);
        thread.start();
    }

    private static UpdateResult fetchResult() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(UPDATE_URL))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "PVPUtils-UpdateChecker")
                .GET()
                .build();
        String body = HTTP.send(request, HttpResponse.BodyHandlers.ofString()).body();
        return parse(body);
    }

    private static UpdateResult parse(String body) {
        String releaseLine = findLine(body, "Version:");
        String betaLine = findLine(body, "Version Beta:");
        String alphaLine = findLine(body, "Version Alpha:");

        String fetchedVersion;
        if (Version.TYPE == 1) {
            fetchedVersion = extractVersion(alphaLine);
        } else if (Version.TYPE == 2) {
            fetchedVersion = extractVersion(betaLine);
        } else {
            fetchedVersion = extractVersion(releaseLine);
        }

        return new UpdateResult(fetchedVersion, isNewer(fetchedVersion));
    }

    private static boolean isNewer(String fetchedVersion) {
        if (fetchedVersion == null || fetchedVersion.isBlank()) return false;

        if (Version.TYPE == 1 || Version.TYPE == 2) {
            VersionToken current = VersionToken.parse(currentTypedVersion());
            VersionToken fetched = VersionToken.parse(fetchedVersion);
            if (current == null || fetched == null) return false;
            if (!current.sameFamily(fetched)) return false;
            return fetched.revision > current.revision;
        }

        VersionToken fetched = VersionToken.parse(fetchedVersion);
        VersionToken current = VersionToken.parse("v" + Version.VERSION);
        if (current == null || fetched == null) return false;
        return fetched.major > current.major
                || (fetched.major == current.major && fetched.minor > current.minor);
    }

    private static String currentTypedVersion() {
        return "v" + Version.VERSION + "-" + Version.typeName() + "." + Version.REVISION;
    }

    private static void deliverToPlayer(MutableComponent message) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        client.execute(() -> {
            if (client.player != null) {
                client.player.displayClientMessage(message, false);
            }
        });
    }

    private static MutableComponent qqClipboardMessage() {
        if (Config.isChinese) {
            return Component.literal("QQ群号已经复制至剪切板！").withStyle(ChatFormatting.GREEN);
        }
        return Component.literal("The QQ group number has been copied to your clipboard, but please note that you need to be in mainland China or have a suitable VPN to access Tencent QQ properly. Sorry for the inconvenience.")
                .withStyle(ChatFormatting.GREEN);
    }

    private static String findLine(String body, String prefix) {
        String[] lines = body.split("\\R");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith(prefix)) {
                return trimmed;
            }
        }
        return null;
    }

    private static String extractVersion(String line) {
        if (line == null) return null;
        int idx = line.indexOf(':');
        if (idx < 0) return null;
        String value = line.substring(idx + 1).trim();
        return value.isEmpty() ? null : value;
    }

    private static String normalizeReason(String reason, boolean chinese) {
        if (reason == null || reason.isBlank()) {
            return chinese ? "未知" : "unknown";
        }
        return reason;
    }

    private record UpdateResult(String fetchedVersion, boolean hasUpdate) {
        MutableComponent updateAvailableMessage() {
            MutableComponent base = Component.literal(Config.isChinese
                    ? "有新版本可用！点击前往更新"
                    : "A new version is available! Click to update ")
                    .withStyle(ChatFormatting.GREEN);
            return base
                    .append(link("[Curseforge]", CURSEFORGE_URL, ChatFormatting.GOLD))
                    .append(link("[Modrinth]", MODRINTH_URL, ChatFormatting.GREEN))
                    .append(link("[Github]", GITHUB_URL, ChatFormatting.WHITE))
                    .append(qqGroupLink());
        }

        MutableComponent manualResultMessage() {
            if (hasUpdate) {
                return updateAvailableMessage();
            }
            String text = Config.isChinese ? "当前已经是最新版本。" : "You are already on the latest version.";
            return Component.literal(text).withStyle(ChatFormatting.GREEN);
        }

        private static MutableComponent link(String text, String url, ChatFormatting color) {
            return Component.literal(text).withStyle(Style.EMPTY
                    .withColor(color)
                    .withBold(true)
                    .withClickEvent(new ClickEvent.OpenUrl(URI.create(url))));
        }

        private static MutableComponent qqGroupLink() {
            String text = Config.isChinese ? "[QQ群聊(获取测试版)]" : "[QQ Group (Test Builds)]";
            return Component.literal(text).withStyle(Style.EMPTY
                    .withColor(0xFFB04DFF)
                    .withBold(true)
                    .withClickEvent(new ClickEvent.RunCommand("/PVPUtils update qqgroup")));
        }
    }

    private record VersionToken(int major, int minor, String type, int revision) {
        static VersionToken parse(String text) {
            if (text == null || text.isBlank()) return null;
            String cleaned = text.trim();
            int namePrefix = cleaned.indexOf("-v");
            if (namePrefix >= 0 && namePrefix + 2 < cleaned.length()) {
                cleaned = cleaned.substring(namePrefix + 1);
            }
            if (cleaned.startsWith("v") || cleaned.startsWith("V")) {
                cleaned = cleaned.substring(1);
            }

            String[] parts = cleaned.split("-");
            String[] numbers = parts[0].split("\\.");
            if (numbers.length < 2) return null;

            try {
                int major = Integer.parseInt(numbers[0]);
                int minor = Integer.parseInt(numbers[1]);
                String type = "release";
                int revision = 0;
                if (parts.length > 1) {
                    String suffix = parts[1].toLowerCase(Locale.ROOT);
                    int dot = suffix.indexOf('.');
                    if (dot >= 0) {
                        type = suffix.substring(0, dot);
                        String rev = suffix.substring(dot + 1).replaceAll("\\D", "");
                        if (!rev.isEmpty()) {
                            revision = Integer.parseInt(rev);
                        }
                    } else {
                        type = suffix;
                    }
                }
                return new VersionToken(major, minor, type, revision);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        boolean sameFamily(VersionToken other) {
            return other != null
                    && major == other.major
                    && minor == other.minor
                    && type.equals(other.type);
        }
    }
}
