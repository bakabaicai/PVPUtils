package com.pvp_utils.client.NeteaseMusic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class NeteaseMusicApi {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private NeteaseMusicApi() {
    }

    public static List<Song> getTopNewSongs() throws IOException, InterruptedException {
        JsonObject root = getJsonObject("/top/song?type=0");
        List<Song> result = new ArrayList<>();
        JsonArray data = array(root, "data");
        for (int i = 0; i < Math.min(30, data.size()); i++) {
            JsonObject song = object(data.get(i));
            JsonObject album = object(song.get("album"));
            result.add(new Song(
                    appendImageSize(string(album, "picUrl")),
                    string(song, "name"),
                    firstArtist(array(song, "artists")),
                    number(song, "id"),
                    number(song, "duration")
            ));
        }
        return result;
    }

    public static List<Song> search(String query) throws IOException, InterruptedException {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        JsonObject root = getJsonObject("/cloudsearch?keywords=" + encode(query));
        JsonArray songs = array(object(root.get("result")), "songs");
        List<Song> result = new ArrayList<>();
        for (JsonElement element : songs) {
            JsonObject song = object(element);
            JsonObject album = object(song.get("al"));
            result.add(new Song(
                    appendImageSize(string(album, "picUrl")),
                    string(song, "name"),
                    firstArtist(array(song, "ar")),
                    number(song, "id"),
                    number(song, "dt")
            ));
        }
        return result;
    }

    public static List<Playlist> getRecommendedPlaylists() throws IOException, InterruptedException {
        JsonObject root = getJsonObject(withSession("/personalized?limit=36"));
        List<Playlist> result = new ArrayList<>();
        JsonArray playlists = array(root, "result");
        for (JsonElement element : playlists) {
            JsonObject playlist = object(element);
            result.add(new Playlist(
                    number(playlist, "id"),
                    string(playlist, "name"),
                    appendImageSize(string(playlist, "picUrl")),
                    number(playlist, "playCount"),
                    (int) number(playlist, "trackCount"),
                    string(playlist, "copywriter")
            ));
        }
        return result;
    }

    public static SongFile getSongFile(long id) throws IOException, InterruptedException {
        JsonObject root = getJsonObject("/song/url/v1?id=" + id + "&level=exhigh&UnblockNeteaseMusic=true");
        JsonArray data = array(root, "data");
        if (data.isEmpty()) {
            return new SongFile("", 0L);
        }
        JsonObject file = object(data.get(0));
        return new SongFile(string(file, "url"), number(file, "size"));
    }

    public static List<LyricLine> getLyric(long id) throws IOException, InterruptedException {
        JsonObject root = getJsonObject("/lyric?id=" + id);
        return LyricLineProcessor.parse(string(object(root.get("lrc")), "lyric"));
    }

    public static QrLogin createQrLogin() throws IOException, InterruptedException {
        JsonObject keyRoot = getJsonObject("/login/qr/key?timestamp=" + System.currentTimeMillis());
        String key = string(object(keyRoot.get("data")), "unikey");
        if (key.isBlank()) {
            throw new IOException("Failed to create QR key");
        }
        JsonObject qrRoot = getJsonObject("/login/qr/create?key=" + encode(key)
                + "&qrimg=true&timestamp=" + System.currentTimeMillis());
        JsonObject data = object(qrRoot.get("data"));
        String qrImage = string(data, "qrimg");
        String qrUrl = string(data, "qrurl");
        if (qrImage.isBlank() && qrUrl.isBlank()) {
            throw new IOException("Failed to create QR code");
        }
        return new QrLogin(key, qrUrl, qrImage);
    }

    public static QrLoginStatus checkQrLogin(String key) throws IOException, InterruptedException {
        if (key == null || key.isBlank()) {
            throw new IOException("QR key is empty");
        }
        JsonObject root = getJsonObject("/login/qr/check?key=" + encode(key)
                + "&timestamp=" + System.currentTimeMillis());
        int code = (int) number(root, "code");
        String message = errorMessage(root, switch (code) {
            case 800 -> "QR code expired";
            case 801 -> "Waiting for scan";
            case 802 -> "Waiting for confirmation";
            case 803 -> "QR login confirmed";
            default -> "QR login failed: " + code;
        });
        if (code != 803) {
            return new QrLoginStatus(code, message, null);
        }
        String cookie = string(root, "cookie");
        if (cookie.isBlank()) {
            throw new IOException("QR login did not return cookie");
        }
        LoginSession session = loadSessionFromCookie(cookie);
        loginSession = session;
        saveSession(session);
        return new QrLoginStatus(code, message, session);
    }

    public static LoginSession loginCellphone(String phone, String password) throws IOException, InterruptedException {
        if (phone == null || phone.isBlank()) {
            throw new IOException("Phone number is empty");
        }
        if (password == null || password.isBlank()) {
            throw new IOException("Password is empty");
        }
        JsonObject root = getJsonObject("/login/cellphone?phone=" + encode(phone.trim())
                + "&password=" + encode(password)
                + "&timestamp=" + System.currentTimeMillis());
        JsonObject profile = object(root.get("profile"));
        LoginSession session = new LoginSession(number(profile, "userId"), string(profile, "nickname"), appendImageSize(string(profile, "avatarUrl")), string(root, "cookie"));
        if (session.uid() <= 0L || session.cookie().isBlank()) {
            throw new IOException("Netease login failed");
        }
        loginSession = session;
        saveSession(session);
        return session;
    }

    public static List<Playlist> getUserPlaylists() throws IOException, InterruptedException {
        LoginSession session = loginSession;
        if (session == null) {
            return List.of();
        }
        JsonObject root = getJsonObject("/user/playlist?uid=" + session.uid()
                + "&limit=100&cookie=" + encode(session.cookie())
                + "&timestamp=" + System.currentTimeMillis());
        List<Playlist> result = new ArrayList<>();
        JsonArray playlists = array(root, "playlist");
        for (JsonElement element : playlists) {
            JsonObject playlist = object(element);
            result.add(new Playlist(
                    number(playlist, "id"),
                    string(playlist, "name"),
                    appendImageSize(string(playlist, "coverImgUrl")),
                    number(playlist, "playCount"),
                    (int) number(playlist, "trackCount"),
                    string(object(playlist.get("creator")), "nickname")
            ));
        }
        return result;
    }

    public static List<Song> getPlaylistDetail(long id) throws IOException, InterruptedException {
        JsonObject root = getJsonObject(withSession("/playlist/track/all?id=" + id + "&limit=80"));
        List<Song> result = new ArrayList<>();
        JsonArray songs = array(root, "songs");
        for (JsonElement element : songs) {
            JsonObject song = object(element);
            JsonObject album = object(song.get("al"));
            result.add(new Song(appendImageSize(string(album, "picUrl")), string(song, "name"), firstArtist(array(song, "ar")), number(song, "id"), number(song, "dt")));
        }
        return result;
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
            JsonElement element = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
            JsonObject object = object(element);
            LoginSession session = new LoginSession(number(object, "uid"), string(object, "nickname"), string(object, "avatarUrl"), string(object, "cookie"));
            if (session.uid() <= 0L || session.cookie().isBlank()) {
                return false;
            }
            loginSession = session;
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static LoginSession currentSession() {
        return loginSession;
    }

    public static void logout() {
        loginSession = null;
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

    private static JsonObject getJsonObject(String path) throws IOException, InterruptedException {
        if (!NeteaseMusicLocalService.isServiceAvailable()) {
            throw new IOException("Local Netease service is unavailable");
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(NeteaseMusicLocalService.baseUrl() + path))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .header("Accept", "application/json")
                .header("User-Agent", "PVPUtils/1.0")
                .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode());
        }
        JsonElement element = JsonParser.parseString(response.body());
        if (!element.isJsonObject()) {
            throw new IOException("Expected JSON object");
        }
        return element.getAsJsonObject();
    }

    private static LoginSession loadSessionFromCookie(String cookie) throws IOException, InterruptedException {
        JsonObject root = getJsonObject("/user/account?cookie=" + encode(cookie)
                + "&timestamp=" + System.currentTimeMillis());
        JsonObject profile = object(root.get("profile"));
        LoginSession session = new LoginSession(number(profile, "userId"), string(profile, "nickname"), appendImageSize(string(profile, "avatarUrl")), cookie);
        if (session.uid() <= 0L || session.cookie().isBlank()) {
            throw new IOException("Netease account response did not include a usable session");
        }
        return session;
    }

    private static void saveSession(LoginSession session) {
        if (session == null || session.cookie().isBlank()) {
            return;
        }
        try {
            Files.createDirectories(sessionPath().getParent());
            JsonObject object = new JsonObject();
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

    private static String errorMessage(JsonObject root, String fallback) {
        String message = string(root, "message");
        if (message.isBlank()) {
            message = string(root, "msg");
        }
        return message.isBlank() ? fallback : message;
    }

    private static volatile LoginSession loginSession;

    private static String withSession(String path) {
        LoginSession session = loginSession;
        if (session == null || session.cookie().isBlank()) {
            return path;
        }
        return path + (path.contains("?") ? "&" : "?")
                + "cookie=" + encode(session.cookie())
                + "&timestamp=" + System.currentTimeMillis();
    }

    public record LoginSession(long uid, String nickname, String avatarUrl, String cookie) {
    }

    public record QrLogin(String key, String qrUrl, String qrImage) {
    }

    public record QrLoginStatus(int code, String message, LoginSession session) {
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String firstArtist(JsonArray artists) {
        if (artists.isEmpty()) {
            return "";
        }
        return string(object(artists.get(0)), "name");
    }

    private static String appendImageSize(String url) {
        if (url == null || url.isBlank() || url.contains("?param=")) {
            return url == null ? "" : url;
        }
        return url + "?param=512y512";
    }

    private static JsonArray array(JsonObject object, String name) {
        if (object == null || !object.has(name) || !object.get(name).isJsonArray()) {
            return new JsonArray();
        }
        return object.getAsJsonArray(name);
    }

    private static JsonObject object(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    private static String string(JsonObject object, String name) {
        if (object == null || !object.has(name) || object.get(name).isJsonNull()) {
            return "";
        }
        try {
            return object.get(name).getAsString();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static long number(JsonObject object, String name) {
        if (object == null || !object.has(name) || object.get(name).isJsonNull()) {
            return 0L;
        }
        try {
            return object.get(name).getAsLong();
        } catch (RuntimeException ignored) {
            return 0L;
        }
    }
}
