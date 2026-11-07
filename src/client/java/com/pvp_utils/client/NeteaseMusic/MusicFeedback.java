package com.pvp_utils.client.NeteaseMusic;

import com.pvp_utils.client.gui.clickgui.UiText;

public final class MusicFeedback {
    private MusicFeedback() {}

    public static String playbackStatus(String value) {
        if (value == null || value.isBlank()) return "";
        for (String prefix : new String[]{"Load failed: ", "Replay failed: ", "Seek failed: "}) {
            if (value.startsWith(prefix)) {
                String label = switch (prefix) {
                    case "Load failed: " -> UiText.t("加载失败：", "Load failed: ");
                    case "Replay failed: " -> UiText.t("重播失败：", "Replay failed: ");
                    default -> UiText.t("播放位置调整失败：", "Seek failed: ");
                };
                return label + detail(value.substring(prefix.length()));
            }
        }
        return detail(value);
    }

    public static String error(Throwable error) {
        if (error == null) return UiText.t("请求失败，请稍后重试", "Request failed. Please try again");
        Throwable cause = error;
        String context = null;
        String networkReason = null;
        for (int depth = 0; depth < 32; depth++) {
            String message = cause.getMessage();
            if (context == null) context = known(message);
            if (cause instanceof java.net.http.HttpTimeoutException || cause instanceof java.net.SocketTimeoutException) {
                networkReason = UiText.t("请求超时", "Request timed out");
            } else if (cause instanceof java.net.UnknownHostException) {
                networkReason = UiText.t("无法解析音乐服务地址", "Unable to resolve the music service address");
            } else if (cause instanceof java.net.ConnectException) {
                networkReason = UiText.t("无法连接音乐服务", "Unable to connect to the music service");
            }
            if (cause.getCause() == null || cause.getCause() == cause) break;
            cause = cause.getCause();
        }
        String raw = cause.getMessage();
        String reason = networkReason != null ? networkReason
                : raw == null || raw.isBlank() ? cause.getClass().getSimpleName() : detail(raw);
        if (context != null) {
            return context.equals(reason) ? context : context + UiText.t("：", ": ") + reason;
        }
        if (networkReason != null || known(raw) != null) return reason;
        return UiText.t("请求失败：", "Request failed: ") + reason;
    }

    private static String detail(String value) {
        if (value == null || value.isBlank()) return UiText.t("未知原因", "Unknown reason");
        String translated = known(value);
        if (translated != null) return translated;
        if (value.startsWith("HTTP ")) {
            return UiText.t("服务器返回 HTTP ", "Server returned HTTP ") + value.substring(5);
        }
        return value;
    }

    private static String known(String value) {
        if (value == null) return null;
        return switch (value) {
            case "Search failed" -> UiText.t("搜索失败", "Search failed");
            case "Failed to get daily recommendations" -> UiText.t("每日推荐加载失败", "Failed to get daily recommendations");
            case "Failed to get recommended playlists" -> UiText.t("推荐歌单加载失败", "Failed to get recommended playlists");
            case "Failed to get song file" -> UiText.t("歌曲文件获取失败", "Failed to get song file");
            case "Failed to get lyric" -> UiText.t("歌词加载失败", "Failed to get lyric");
            case "QR content is empty" -> UiText.t("登录二维码内容为空", "QR content is empty");
            case "QR key is empty" -> UiText.t("登录二维码标识为空", "QR key is empty");
            case "QR login cancelled" -> UiText.t("二维码登录已取消", "QR login cancelled");
            case "QR login confirmed" -> UiText.t("二维码登录已确认", "QR login confirmed");
            case "Waiting for confirmation" -> UiText.t("等待确认登录", "Waiting for confirmation");
            case "Waiting for scan" -> UiText.t("等待扫码", "Waiting for scan");
            case "QR code expired" -> UiText.t("二维码已过期", "QR code expired");
            case "QR login failed" -> UiText.t("二维码登录失败", "QR login failed");
            case "Failed to create QR login" -> UiText.t("登录二维码生成失败", "Failed to create QR login");
            case "Failed to check QR login" -> UiText.t("二维码登录状态查询失败", "Failed to check QR login");
            case "Cellphone login is not supported. Please use QR code login." ->
                    UiText.t("不支持手机号登录，请使用二维码登录。", "Cellphone login is not supported. Please use QR code login.");
            case "Failed to get user playlists" -> UiText.t("个人歌单加载失败", "Failed to get user playlists");
            case "Failed to get playlist detail" -> UiText.t("歌单内容加载失败", "Failed to get playlist detail");
            case "Song is unavailable" -> UiText.t("歌曲暂不可用", "Song is unavailable");
            default -> null;
        };
    }
}
