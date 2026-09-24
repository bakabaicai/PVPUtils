package com.pvp_utils.client.NeteaseMusic;

import net.fabricmc.loader.api.FabricLoader;
import top.fpsmaster.music.AudioQuality;
import top.fpsmaster.music.Lyric;
import top.fpsmaster.music.MusicService;
import top.fpsmaster.music.MusicSource;
import top.fpsmaster.music.PlaylistBrief;
import top.fpsmaster.music.QrCode;
import top.fpsmaster.music.QrLoginState;
import top.fpsmaster.music.SongUrl;
import top.fpsmaster.music.Track;
import top.fpsmaster.music.UserProfile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class NeteaseMusicApi {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final MusicService musicService = new MusicService();
    private static volatile LoginSession loginSession;
    private static volatile boolean profileRefreshInFlight;
    private static volatile long profileRefreshLastAttempt;

    private NeteaseMusicApi() {
    }

    public static List<Song> search(String query) throws IOException, InterruptedException {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        try {
            List<Track> tracks = musicService.getNetease().search(query, 30, 0);
            List<Song> result = new ArrayList<>();
            for (Track track : tracks) {
                result.add(trackToSong(track));
            }
            return result;
        } catch (Exception e) {
            throw new IOException("Search failed", e);
        }
    }

    public static List<Playlist> getRecommendedPlaylists() throws IOException, InterruptedException {
        try {
            if (isLoggedIn()) {
                List<PlaylistBrief> playlists = musicService.getNetease().getRecommendPlaylists();
                List<Playlist> result = new ArrayList<>();
                for (PlaylistBrief p : playlists) {
                    String coverUrl = p.getCoverUrl() != null ? p.getCoverUrl() : "";
                    if (!coverUrl.isBlank() && !coverUrl.contains("param=")) {
                        coverUrl = coverUrl + (coverUrl.contains("?") ? "&" : "?") + "param=512y512";
                    }
                    result.add(new Playlist(Long.parseLong(p.getId()), p.getName(), coverUrl, p.getPlayCount(), p.getTrackCount(), p.getCreator()));
                }
                return result;
            }
            return new ArrayList<>();
        } catch (Exception e) {
            throw new IOException("Failed to get recommended playlists", e);
        }
    }

    public static SongFile getSongFile(long id) throws IOException, InterruptedException {
        try {
            Track track = trackFromId(id);
            SongUrl songUrl = musicService.getSongUrl(track, AudioQuality.HIGH);
            if (!songUrl.getAvailable()) {
                return new SongFile("", 0L);
            }
            return new SongFile(songUrl.getUrl(), songUrl.getSizeBytes());
        } catch (Exception e) {
            throw new IOException("Failed to get song file", e);
        }
    }

    public static List<LyricLine> getLyric(long id) throws IOException, InterruptedException {
        try {
            Track track = trackFromId(id);
            Lyric lyric = musicService.getLyric(track);
            List<LyricLine> result = new ArrayList<>();
            for (top.fpsmaster.music.LyricLine line : lyric.getLines()) {
                String text = line.getText() != null ? line.getText() : "";
                String trans = line.getTranslation() != null ? line.getTranslation() : "";
                if (isInstrumentalPlaceholder(text)) {
                    continue;
                }
                result.add(new LyricLine(text, trans, line.getStartMs()));
            }
            return result;
        } catch (Exception e) {
            throw new IOException("Failed to get lyric", e);
        }
    }

    public static QrLogin createQrLogin() throws IOException, InterruptedException {
        try {
            QrCode qr = musicService.getNetease().createQrCode();
            String qrUrl = qr.getQrContent();
            if (qrUrl == null || qrUrl.isBlank()) {
                throw new IOException("QR content is empty");
            }
            String qrImage = generateQrDataUrl(qrUrl);
            return new QrLogin(qr.getKey(), qrUrl, qrImage);
        } catch (Exception e) {
            throw new IOException("Failed to create QR login", e);
        }
    }

    private static String generateQrDataUrl(String content) throws Exception {
        io.nayuki.qrcodegen.QrCode qr = io.nayuki.qrcodegen.QrCode.encodeText(content, io.nayuki.qrcodegen.QrCode.Ecc.MEDIUM);
        int scale = 8;
        int size = qr.size * scale;
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, size, size);
        g.setColor(java.awt.Color.BLACK);
        for (int y = 0; y < qr.size; y++) {
            for (int x = 0; x < qr.size; x++) {
                if (qr.getModule(x, y)) {
                    g.fillRect(x * scale, y * scale, scale, scale);
                }
            }
        }
        g.dispose();
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "PNG", baos);
        return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    public static QrLoginStatus checkQrLogin(String key) throws IOException, InterruptedException {
        if (key == null || key.isBlank()) {
            throw new IOException("QR key is empty");
        }
        try {
            QrCode qr = new QrCode(key, "https://music.163.com/login?codekey=" + key);
            QrLoginState state = musicService.getNetease().checkQrCode(qr);
            return switch (state) {
                case CONFIRMED -> {
                    String cookie = musicService.getNetease().getCookie();
                    LoginSession session = refreshSessionFromCookie(cookie);
                    if (session == null) {
                        session = new LoginSession(0, "", "", cookie);
                    }
                    loginSession = session;
                    saveSession(session);
                    yield new QrLoginStatus(803, "QR login confirmed", session);
                }
                case SCANNED -> new QrLoginStatus(802, "Waiting for confirmation", null);
                case WAITING -> new QrLoginStatus(801, "Waiting for scan", null);
                case EXPIRED -> new QrLoginStatus(800, "QR code expired", null);
                default -> new QrLoginStatus(-1, "QR login failed", null);
            };
        } catch (Exception e) {
            throw new IOException("Failed to check QR login", e);
        }
    }

    public static LoginSession loginCellphone(String phone, String password) throws IOException, InterruptedException {
        throw new IOException("Cellphone login is not supported. Please use QR code login.");
    }

    public static List<Playlist> getUserPlaylists() throws IOException, InterruptedException {
        LoginSession session = loginSession;
        if (session == null) {
            return List.of();
        }
        try {
            List<PlaylistBrief> playlists = musicService.getNetease().getUserPlaylists(session.uid(), 100, 0);
            List<Playlist> result = new ArrayList<>();
            for (PlaylistBrief p : playlists) {
                String coverUrl = p.getCoverUrl() != null ? p.getCoverUrl() : "";
                if (!coverUrl.isBlank() && !coverUrl.contains("param=")) {
                    coverUrl = coverUrl + (coverUrl.contains("?") ? "&" : "?") + "param=512y512";
                }
                result.add(new Playlist(Long.parseLong(p.getId()), p.getName(), coverUrl, p.getPlayCount(), p.getTrackCount(), p.getCreator()));
            }
            return result;
        } catch (Exception e) {
            throw new IOException("Failed to get user playlists", e);
        }
    }

    public static List<Song> getPlaylistDetail(long id) throws IOException, InterruptedException {
        return getPlaylistDetail(id, 80, 0);
    }

    public static List<Song> getPlaylistDetail(long id, int limit, int offset) throws IOException, InterruptedException {
        try {
            List<Track> tracks = musicService.getNetease().getPlaylistTracks(String.valueOf(id), Math.max(1, limit));
            List<Song> result = new ArrayList<>();
            int end = Math.min(offset + limit, tracks.size());
            for (int i = offset; i < end; i++) {
                result.add(trackToSong(tracks.get(i)));
            }
            return result;
        } catch (Exception e) {
            throw new IOException("Failed to get playlist detail", e);
        }
    }

    public static boolean isLoggedIn() {
        return loginSession != null;
    }

    public static boolean restoreSession() {
        if (loginSession != null) {
            return true;
        }
        Path path = sessionPath();
        if (!Files.isRegularFile(path)) {
            return false;
        }
        try {
            com.google.gson.JsonElement element = com.google.gson.JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
            com.google.gson.JsonObject object = element.getAsJsonObject();
            String cookie = getString(object, "cookie");
            if (cookie.isBlank()) {
                return false;
            }
            musicService.getNetease().setCookie(cookie);
            LoginSession session = refreshSessionFromCookie(cookie);
            if (session == null) {
                return false;
            }
            loginSession = session;
            saveSession(session);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static LoginSession currentSession() {
        LoginSession session = loginSession;
        if (session != null && (session.avatarUrl().isBlank() || session.nickname().isBlank())) {
            requestSessionProfileRefresh(session);
        }
        return session;
    }

    public static void logout() {
        loginSession = null;
        musicService.getNetease().clearLogin();
        try {
            Files.deleteIfExists(sessionPath());
        } catch (IOException ignored) {
        }
    }

    public static byte[] getBytes(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .header("Accept", "*/*")
                .header("User-Agent", "PVPUtils/1.0")
                .build();
        HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode());
        }
        return response.body();
    }

    private static LoginSession refreshSessionFromCookie(String cookie) {
        try {
            musicService.getNetease().setCookie(cookie);
            Long uid = musicService.getNetease().getLoginUid();
            if (uid == null || uid <= 0) {
                return null;
            }
            String nickname = "";
            String avatarUrl = "";
            try {
                UserProfile profile = musicService.getNetease().getUserProfile(uid);
                if (profile != null) {
                    nickname = profile.getNickname() != null ? profile.getNickname() : "";
                    avatarUrl = profile.getAvatarUrl() != null ? profile.getAvatarUrl() : "";
                }
            } catch (Exception ignored) {
            }
            if (!avatarUrl.isBlank() && !avatarUrl.contains("param=")) {
                avatarUrl = avatarUrl + (avatarUrl.contains("?") ? "&" : "?") + "param=512y512";
            }
            return new LoginSession(uid, nickname, avatarUrl, cookie);
        } catch (Exception e) {
            return null;
        }
    }

    private static void requestSessionProfileRefresh(LoginSession session) {
        long now = System.currentTimeMillis();
        if (profileRefreshInFlight || (now - profileRefreshLastAttempt) < 5000) {
            return;
        }
        profileRefreshInFlight = true;
        profileRefreshLastAttempt = now;
        CompletableFuture.runAsync(() -> {
            try {
                LoginSession refreshed = refreshSessionFromCookie(session.cookie());
                if (refreshed != null && refreshed.uid() > 0 && !refreshed.cookie().isBlank()) {
                    loginSession = refreshed;
                    saveSession(refreshed);
                }
            } finally {
                profileRefreshInFlight = false;
            }
        });
    }

    private static void saveSession(LoginSession session) {
        if (session == null || session.cookie().isBlank()) {
            return;
        }
        try {
            Files.createDirectories(sessionPath().getParent());
            com.google.gson.JsonObject object = new com.google.gson.JsonObject();
            object.addProperty("uid", session.uid());
            object.addProperty("nickname", session.nickname());
            object.addProperty("avatarUrl", session.avatarUrl());
            object.addProperty("cookie", session.cookie());
            Files.writeString(sessionPath(), object.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static Path sessionPath() {
        return FabricLoader.getInstance().getGameDir().resolve("PVPUtils").resolve("netease-session.json");
    }

    private static Track trackFromId(long id) {
        return new Track(MusicSource.NETEASE, String.valueOf(id), null, "", "", "", 0, null, false);
    }

    private static Song trackToSong(Track track) {
        return new Song(
                track.getCoverUrl() != null ? track.getCoverUrl() : "",
                track.getName(),
                track.getArtists(),
                Long.parseLong(track.getId()),
                track.getDurationMs()
        );
    }

    private static boolean isInstrumentalPlaceholder(String text) {
        if (text == null) return true;
        String t = text.strip();
        return t.isEmpty() || t.equals("纯音乐，请欣赏") || t.equals("纯音乐，请欣赏。") || t.equals("Instrumental") || t.equals("No lyrics");
    }

    private static String getString(com.google.gson.JsonObject object, String name) {
        if (object == null || !object.has(name) || object.get(name).isJsonNull()) {
            return "";
        }
        try {
            return object.get(name).getAsString();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    public record LoginSession(long uid, String nickname, String avatarUrl, String cookie) {
    }

    public record QrLogin(String key, String qrUrl, String qrImage) {
        public String data() {
            return qrUrl;
        }
    }

    public record QrLoginStatus(int code, String message, LoginSession session) {
    }

    public static io.github.humbleui.skija.Image qrImage(String dataUrl) {
        if (dataUrl == null || dataUrl.isBlank()) return null;
        try {
            byte[] bytes = getBytes(dataUrl);
            return io.github.humbleui.skija.Image.makeFromEncoded(bytes);
        } catch (Exception e) {
            return null;
        }
    }
}
