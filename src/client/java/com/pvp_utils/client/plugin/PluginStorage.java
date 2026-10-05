package com.pvp_utils.client.plugin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class PluginStorage {
    private final Path file;
    private JsonObject data;

    public PluginStorage(Path file) throws IOException {
        this.file = file;
        if (Files.exists(file) && Files.size(file) > 1048576) throw new IOException("Plugin storage exceeds limit");
        data = Files.exists(file)
                ? JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject()
                : new JsonObject();
    }

    public String get(String key) {
        return data.has(key) ? data.get(key).toString() : "null";
    }

    public boolean contains(String key) {
        return data.has(key);
    }

    public void set(String key, String json) throws IOException {
        if (key.isBlank() || key.length() > 128) throw new IllegalArgumentException("Invalid storage key");
        JsonObject next = data.deepCopy();
        next.add(key, JsonParser.parseString(json));
        replace(next);
    }

    public void replace(JsonObject values) throws IOException {
        JsonObject next = values.deepCopy();
        byte[] encoded = next.toString().getBytes(StandardCharsets.UTF_8);
        if (encoded.length > 1048576) throw new IOException("Plugin storage exceeds limit");
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "plugin-", ".tmp");
        try {
            Files.write(temporary, encoded);
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            data = next;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
