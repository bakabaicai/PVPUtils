package com.pvp_utils.client.plugin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public record PluginManifest(String id, String name, String version, String description, String author,
                             Path directory, Path entry, boolean defaultEnabled) {
    public static PluginManifest read(Path directory) throws IOException {
        Path root = directory.toRealPath();
        Path manifestPath = root.resolve("plugin.json").toRealPath();
        if (!manifestPath.startsWith(root) || Files.size(manifestPath) > 65536) {
            throw new IOException("Invalid plugin manifest path or size");
        }
        JsonObject json = JsonParser.parseString(Files.readString(manifestPath, StandardCharsets.UTF_8)).getAsJsonObject();
        String id = text(json, "id", "");
        if (!id.matches("[a-z][a-z0-9_-]{0,63}")) throw new IOException("Invalid plugin id: " + id);
        if (json.has("apiVersion") && json.get("apiVersion").getAsInt() != 1) {
            throw new IOException("Unsupported plugin API version");
        }
        Path entry = root.resolve(text(json, "main", "main.js")).normalize().toRealPath();
        if (!entry.startsWith(root) || !Files.isRegularFile(entry) || Files.size(entry) > 1048576) {
            throw new IOException("Invalid plugin entry path or size");
        }
        return new PluginManifest(id, text(json, "name", id), text(json, "version", "1.0.0"),
                text(json, "description", ""), text(json, "author", ""), root, entry,
                json.has("enabled") && json.get("enabled").getAsBoolean());
    }

    private static String text(JsonObject json, String key, String fallback) {
        String value = json.has(key) ? json.get(key).getAsString() : fallback;
        if (value.length() > 2048) throw new IllegalArgumentException("Manifest value too long: " + key);
        return value;
    }
}
