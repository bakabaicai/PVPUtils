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
import java.util.ArrayList;
import java.util.List;
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
    private static volatile MutableComponent pendingAutoError;
    private static volatile MutableComponent pendingManualStatus;
    private static volatile boolean shown;

    private Update() {}

    public static void startAutoCheck() {
        runCheck(false, null, null);
    }

    public static void startManualCheck() {
        pendingManualStatus = checkingMessage();
        runCheck(true, Update::deliverToPlayer, Update::deliverToPlayer);
    }

    public static void tick(Minecraft client) {
        MutableComponent manualStatus = pendingManualStatus;
        if (manualStatus != null && client != null && client.player != null && client.level != null) {
            pendingManualStatus = null;
            client.player.displayClientMessage(manualStatus, false);
        }

        MutableComponent autoError = pendingAutoError;
        if (autoError != null && client != null && client.player != null && client.level != null) {
            pendingAutoError = null;
            client.player.displayClientMessage(autoError, false);
        }

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

    public static MutableComponent retryingMessage(int attempt, int maxAttempts) {
        String text = Config.isChinese
                ? "正在重试（" + attempt + "/" + maxAttempts + "）..."
                : "Retrying (" + attempt + "/" + maxAttempts + ")...";
        return Component.literal(text).withStyle(ChatFormatting.GRAY);
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
                System.out.println("[PVPUtils][Update] check start manual=" + manual + " current=" + currentTypedVersion());
                UpdateResult result = fetchResultWithRetry(5, manual);
                cachedResult = result;
                pendingManualStatus = null;
                System.out.println("[PVPUtils][Update] check complete current=" + currentTypedVersion()
                        + " fetched=" + result.fetchedVersion()
                        + " needUpdate=" + result.hasUpdate());
                if (manual && onResult != null) {
                    onResult.accept(result.manualResultMessage());
                }
            } catch (Exception e) {
                cachedResult = null;
                pendingManualStatus = null;
                System.out.println("[PVPUtils][Update] check failed current=" + currentTypedVersion() + " reason=" + e);
                if (!manual) {
                    pendingAutoError = errorMessage(e.getMessage());
                } else if (onError != null) {
                    onError.accept(errorMessage(e.getMessage()));
                }
            }
        }, manual ? "pvp-utils-update-manual" : "pvp-utils-update-auto");
        thread.setDaemon(true);
        thread.start();
    }

    private static UpdateResult fetchResultWithRetry(int maxAttempts, boolean manual) throws IOException, InterruptedException {
        IOException lastIo = null;
        InterruptedException lastInterrupted = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (manual) {
                    System.out.println("[PVPUtils][Update] fetch attempt " + attempt + "/" + maxAttempts);
                }
                return fetchResultOnce();
            } catch (IOException e) {
                lastIo = e;
            } catch (InterruptedException e) {
                lastInterrupted = e;
            }
            if (attempt < maxAttempts) {
                if (manual) {
                    pendingManualStatus = retryingMessage(attempt + 1, maxAttempts);
                }
                Thread.sleep(1200L);
            }
        }
        if (lastInterrupted != null) throw lastInterrupted;
        if (lastIo != null) throw lastIo;
        throw new IOException("Unknown update error");
    }

    private static UpdateResult fetchResultOnce() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(buildUpdateUri())
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "PVPUtils-UpdateChecker")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .GET()
                .build();
        String body = HTTP.send(request, HttpResponse.BodyHandlers.ofString()).body();
        System.out.println("[PVPUtils][Update] raw body received");
        return parse(body);
    }

    private static URI buildUpdateUri() {
        String separator = UPDATE_URL.contains("?") ? "&" : "?";
        return URI.create(UPDATE_URL + separator + "t=" + System.currentTimeMillis());
    }

    private static UpdateResult parse(String body) {
        String releaseLine = findLine(body, "Version:");
        String betaLine = findLine(body, "Version Beta:");
        String alphaLine = findLine(body, "Version Alpha:");

        String releaseVersion = extractVersion(releaseLine);
        String betaVersion = extractVersion(betaLine);
        String alphaVersion = extractVersion(alphaLine);

        System.out.println("[PVPUtils][Update] parse current=" + currentTypedVersion()
                + " release=" + releaseVersion
                + " beta=" + betaVersion
                + " alpha=" + alphaVersion);

        VersionCandidate selected = selectCandidate(releaseVersion, betaVersion, alphaVersion);
        String fetchedVersion = selected == null ? null : selected.version;
        boolean needUpdate = selected != null && isNewer(selected);
        System.out.println("[PVPUtils][Update] selected=" + fetchedVersion + " needUpdate=" + needUpdate);
        return new UpdateResult(selected, needUpdate);
    }

    private static VersionCandidate selectCandidate(String releaseVersion, String betaVersion, String alphaVersion) {
        VersionToken current = VersionToken.parse(currentTypedVersion());
        VersionCandidate release = releaseVersion == null ? null : new VersionCandidate("release", releaseVersion, VersionToken.parse(releaseVersion));
        VersionCandidate beta = betaVersion == null ? null : new VersionCandidate("beta", betaVersion, VersionToken.parse(betaVersion));
        VersionCandidate alpha = alphaVersion == null ? null : new VersionCandidate("alpha", alphaVersion, VersionToken.parse(alphaVersion));

        if (Version.TYPE == 3) {
            return isNewer(release, current) ? release : null;
        }

        if (isNewer(release, current)) {
            return release;
        }

        if (Version.TYPE == 2) {
            return isNewer(beta, current) ? beta : null;
        }

        if (isNewer(beta, current)) {
            return beta;
        }

        return isNewer(alpha, current) ? alpha : null;
    }

    private static boolean isNewer(VersionCandidate candidate) {
        return isNewer(candidate, VersionToken.parse(currentTypedVersion()));
    }

    private static boolean isNewer(VersionCandidate candidate, VersionToken current) {
        if (candidate == null || candidate.version == null || candidate.version.isBlank()) return false;
        VersionToken fetched = candidate.token != null ? candidate.token : VersionToken.parse(candidate.version);
        return current != null && fetched != null && fetched.compareTo(current) > 0;
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
            return Component.literal("QQ群号已经复制至剪切板，但请注意，您需要在中国境内或者拥有合适的VPN才能正常访问腾讯QQ，为您带来不便敬请谅解。")
                    .withStyle(ChatFormatting.GREEN);
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

    private record UpdateResult(VersionCandidate selected, boolean hasUpdate) {
        String fetchedVersion() {
            return selected == null ? null : selected.version;
        }

        MutableComponent updateAvailableMessage() {
            String kind = updateKindText();
            MutableComponent base = Component.literal(Config.isChinese
                    ? "检测到新的" + kind + "更新，点击前往更新 "
                    : "A new " + kind + " update is available, click to update ")
                    .withStyle(ChatFormatting.GREEN);
            return base
                    .append(link("[Curseforge]", CURSEFORGE_URL, ChatFormatting.GOLD, null))
                    .append(link("[Modrinth]", MODRINTH_URL, ChatFormatting.GREEN, null))
                    .append(link("[Github]", GITHUB_URL, ChatFormatting.WHITE, null))
                    .append(qqGroupLink());
        }

        MutableComponent manualResultMessage() {
            if (hasUpdate) {
                return updateAvailableMessage();
            }
            String text = Config.isChinese ? "当前已经是最新版本。" : "You are already on the latest version.";
            return Component.literal(text).withStyle(ChatFormatting.GREEN);
        }

        private String updateKindText() {
            String type = selected == null ? "release" : selected.type;
            return switch (type) {
                case "alpha" -> Config.isChinese ? "测试版" : "alpha";
                case "beta" -> Config.isChinese ? "测试版" : "beta";
                default -> Config.isChinese ? "正式版" : "release";
            };
        }

        private static MutableComponent link(String text, String url, ChatFormatting color, String command) {
            Style style = Style.EMPTY
                    .withColor(color)
                    .withBold(true);
            if (command != null) {
                style = style.withClickEvent(new ClickEvent.RunCommand(command));
            } else {
                style = style.withClickEvent(new ClickEvent.OpenUrl(URI.create(url)));
            }
            return Component.literal(text).withStyle(style);
        }

        private static MutableComponent qqGroupLink() {
            String text = Config.isChinese ? "[QQ群聊(获取测试版)]" : "[QQ Group (Get Test Builds)]";
            return Component.literal(text).withStyle(Style.EMPTY
                    .withColor(0xFFB04DFF)
                    .withBold(true)
                    .withClickEvent(new ClickEvent.RunCommand("/PVPUtils update qqgroup")));
        }
    }

    private record VersionToken(int major, int minor, String type, int revision) implements Comparable<VersionToken> {
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

        @Override
        public int compareTo(VersionToken other) {
            if (other == null) return 1;
            if (major != other.major) return Integer.compare(major, other.major);
            if (minor != other.minor) return Integer.compare(minor, other.minor);
            int rank = rank(type);
            int otherRank = rank(other.type);
            if (rank != otherRank) return Integer.compare(rank, otherRank);
            return Integer.compare(revision, other.revision);
        }

        private static int rank(String type) {
            if (type == null) return 0;
            return switch (type.toLowerCase(Locale.ROOT)) {
                case "release" -> 3;
                case "beta" -> 2;
                case "alpha" -> 1;
                default -> 0;
            };
        }
    }

    private record VersionCandidate(String type, String version, VersionToken token) {}
}
