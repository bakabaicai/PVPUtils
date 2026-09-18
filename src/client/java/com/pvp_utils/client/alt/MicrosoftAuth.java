package com.pvp_utils.client.alt;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class MicrosoftAuth {
    private static final String CLIENT_ID = "00000000402b5328";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private MicrosoftAuth() {
    }

    public static CompletableFuture<Void> login(Consumer<String> onCode, Consumer<String> onStatus, Consumer<AltManager.Account> onSuccess, Consumer<String> onError) {
        return CompletableFuture.runAsync(() -> {
            try {
                status(onStatus, "Requesting device code...");
                JsonObject device = postForm("https://login.live.com/oauth20_connect.srf",
                        "client_id=" + enc(CLIENT_ID) + "&scope=" + enc("XboxLive.signin offline_access") + "&response_type=device_code");
                String deviceCode = required(device, "device_code");
                String userCode = required(device, "user_code");
                String url = value(device, "verification_uri_complete", value(device, "verification_uri", "https://microsoft.com/devicelogin"));
                run(() -> {
                    onCode.accept(userCode + "\n" + url + "\n" + number(device, "expires_in", 900));
                    onStatus.accept("Waiting for browser authorization...");
                    open(url);
                });

                String microsoftToken = null;
                long deadline = System.currentTimeMillis() + number(device, "expires_in", 900) * 1000L;
                int interval = Math.max(2, number(device, "interval", 5));
                while (System.currentTimeMillis() < deadline) {
                    Thread.sleep(interval * 1000L);
                    JsonObject token = postFormResult("https://login.live.com/oauth20_token.srf",
                            "client_id=" + enc(CLIENT_ID) + "&device_code=" + enc(deviceCode)
                                    + "&grant_type=" + enc("urn:ietf:params:oauth:grant-type:device_code"));
                    microsoftToken = value(token, "access_token", null);
                    if (microsoftToken != null) break;
                    String error = value(token, "error", "");
                    if ("slow_down".equals(error)) interval += 5;
                    else if (!"authorization_pending".equals(error)) throw new Exception(value(token, "error_description", "Microsoft login failed."));
                }
                if (microsoftToken == null) throw new Exception("Microsoft login timed out.");

                status(onStatus, "Signing in to Xbox Live...");
                JsonObject xbox = postJson("https://user.auth.xboxlive.com/user/authenticate",
                        "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\"d=" + microsoftToken
                                + "\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}");
                String xboxToken = required(xbox, "Token");
                status(onStatus, "Authorizing Xbox services...");
                JsonObject xsts = postJson("https://xsts.auth.xboxlive.com/xsts/authorize",
                        "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\"" + xboxToken
                                + "\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}");
                String xstsToken = required(xsts, "Token");
                JsonObject xui = xui(xsts);
                String hash = required(xui, "uhs");
                status(onStatus, "Signing in to Minecraft...");
                JsonObject minecraft = postJson("https://api.minecraftservices.com/authentication/login_with_xbox",
                        "{\"identityToken\":\"XBL3.0 x=" + hash + ";" + xstsToken + "\"}");
                String minecraftToken = required(minecraft, "access_token");
                status(onStatus, "Loading Minecraft profile...");
                HttpResponse<String> profileResponse = HTTP.send(HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                        .header("Authorization", "Bearer " + minecraftToken).GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                JsonObject profile = object(profileResponse.body());
                UUID uuid = UUID.fromString(formatUuid(required(profile, "id")));
                AltManager.Account account = AltManager.Account.microsoft(required(profile, "name"), uuid, minecraftToken, value(xui, "xid", ""));
                run(() -> onSuccess.accept(account));
            } catch (Exception exception) {
                run(() -> onError.accept(exception.getMessage() == null ? "Microsoft login failed." : exception.getMessage()));
            }
        });
    }

    private static JsonObject postForm(String url, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        return response(request);
    }

    private static JsonObject postFormResult(String url, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return object(response.body());
    }

    private static JsonObject postJson(String url, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        return response(request);
    }

    private static JsonObject response(HttpRequest request) throws Exception {
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonObject object = object(response.body());
        if (response.statusCode() / 100 != 2) throw new Exception(value(object, "error_description", "Authentication request failed."));
        return object;
    }

    private static JsonObject object(String body) {
        JsonElement element = JsonParser.parseString(body == null || body.isBlank() ? "{}" : body);
        return element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    private static JsonObject xui(JsonObject object) {
        JsonElement displayClaims = object == null ? null : object.get("DisplayClaims");
        if (displayClaims == null || !displayClaims.isJsonObject()) return new JsonObject();
        JsonElement claims = displayClaims.getAsJsonObject().get("xui");
        if (claims == null || !claims.isJsonArray()) return new JsonObject();
        JsonArray array = claims.getAsJsonArray();
        return array.isEmpty() ? new JsonObject() : array.get(0).getAsJsonObject();
    }

    private static String required(JsonObject object, String key) throws Exception {
        String value = value(object, key, null);
        if (value == null || value.isBlank()) throw new Exception("Missing " + key + ".");
        return value;
    }

    private static String value(JsonObject object, String key, String fallback) {
        JsonElement element = object == null ? null : object.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsString();
    }

    private static int number(JsonObject object, String key, int fallback) {
        try { return object.has(key) ? object.get(key).getAsInt() : fallback; } catch (Exception ignored) { return fallback; }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String formatUuid(String value) {
        return value.length() == 32 ? value.substring(0, 8) + "-" + value.substring(8, 12) + "-" + value.substring(12, 16) + "-" + value.substring(16, 20) + "-" + value.substring(20) : value;
    }

    private static void open(String url) {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception ignored) {
        }
    }

    private static void run(Runnable action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) action.run(); else minecraft.execute(action);
    }

    private static void status(Consumer<String> onStatus, String value) {
        run(() -> onStatus.accept(value));
    }
}
