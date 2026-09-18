package com.pvp_utils.client.alt;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

final class AltAvatarLoader {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private AltAvatarLoader() {
    }

    static CompletableFuture<byte[]> load(AltManager.Account account) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID profileId = account.microsoft() ? account.uuid() : resolveUuid(account.name());
                if (profileId == null) return null;
                String compactUuid = profileId.toString().replace("-", "");
                JsonObject profile = getJson("https://sessionserver.mojang.com/session/minecraft/profile/" + compactUuid);
                JsonArray properties = profile.has("properties") && profile.get("properties").isJsonArray()
                        ? profile.getAsJsonArray("properties")
                        : new JsonArray();
                for (JsonElement element : properties) {
                    if (!element.isJsonObject()) continue;
                    JsonObject property = element.getAsJsonObject();
                    if (!"textures".equals(text(property, "name"))) continue;
                    String encoded = text(property, "value");
                    if (encoded.isBlank()) continue;
                    JsonObject textures = JsonParser.parseString(new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8)).getAsJsonObject();
                    JsonObject textureMap = textures.has("textures") && textures.get("textures").isJsonObject()
                            ? textures.getAsJsonObject("textures")
                            : new JsonObject();
                    JsonObject skin = textureMap.has("SKIN") && textureMap.get("SKIN").isJsonObject()
                            ? textureMap.getAsJsonObject("SKIN")
                            : new JsonObject();
                    String url = text(skin, "url");
                    if (!url.isBlank()) return getBytes(url);
                }
            } catch (Exception ignored) {
            }
            return null;
        });
    }

    private static UUID resolveUuid(String name) throws Exception {
        if (name == null || name.isBlank()) return null;
        JsonObject profile = getJson("https://api.mojang.com/users/profiles/minecraft/" + name);
        String id = text(profile, "id");
        if (id.length() != 32) return null;
        return UUID.fromString(id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16)
                + "-" + id.substring(16, 20) + "-" + id.substring(20));
    }

    private static JsonObject getJson(String url) throws Exception {
        byte[] bytes = getBytes(url);
        JsonElement root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
        return root.isJsonObject() ? root.getAsJsonObject() : new JsonObject();
    }

    private static byte[] getBytes(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "PVPUtils")
                .GET()
                .build();
        HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() / 100 != 2) throw new IllegalStateException();
        return response.body();
    }

    private static String text(JsonObject object, String key) {
        JsonElement element = object == null ? null : object.get(key);
        return element == null || element.isJsonNull() ? "" : element.getAsString();
    }
}
