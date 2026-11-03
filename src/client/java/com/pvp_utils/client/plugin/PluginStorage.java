package com.pvp_utils.client.plugin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;

public final class PluginStorage {
    private final Path file;
    private JsonObject data;

    public PluginStorage(Path file) throws IOException {
        this.file = file;
        if (Files.isSymbolicLink(file)) throw new IOException("Plugin storage is a symbolic link");
        if (Files.exists(file) && Files.size(file) > 1048576) throw new IOException("Plugin storage exceeds limit");
        if (Files.exists(file)) {
            String json = PluginSource.readUtf8(file, 1048576);
            validateJsonText(json);
            data = JsonParser.parseString(json).getAsJsonObject();
            validateJsonTree(data);
        } else {
            data = new JsonObject();
        }
    }

    public String get(String key) {
        return data.has(key) ? data.get(key).toString() : "null";
    }

    public boolean contains(String key) {
        return data.has(key);
    }

    public double number(String key, double fallback) {
        if (!data.has(key) || !data.get(key).isJsonPrimitive() || !data.get(key).getAsJsonPrimitive().isNumber()) {
            return fallback;
        }
        return data.get(key).getAsDouble();
    }

    public void update(JsonObject values) throws IOException {
        validateJsonTree(values);
        JsonObject merged = data.deepCopy();
        values.entrySet().forEach(entry -> merged.add(entry.getKey(), entry.getValue().deepCopy()));
        replace(merged);
    }

    public void set(String key, String json) throws IOException {
        if (key.isBlank() || key.length() > 128) throw new IllegalArgumentException("Invalid storage key");
        validateJsonText(json);
        JsonObject next = data.deepCopy();
        next.add(key, JsonParser.parseString(json));
        replace(next);
    }

    public void replace(JsonObject values) throws IOException {
        if (Files.isSymbolicLink(file)) throw new IOException("Plugin storage is a symbolic link");
        validateJsonTree(values);
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

    private static void validateJsonText(String json) throws IOException {
        if (json == null || json.length() > 1048576) throw new IOException("Plugin storage exceeds limit");
        boolean quoted = false;
        boolean escaped = false;
        int depth = 0;
        for (int i = 0; i < json.length(); i++) {
            char character = json.charAt(i);
            if (quoted) {
                if (escaped) escaped = false;
                else if (character == '\\') escaped = true;
                else if (character == '"') quoted = false;
            } else if (character == '"') {
                quoted = true;
            } else if (character == '{' || character == '[') {
                if (++depth > 64) throw new IOException("Plugin storage nesting exceeds limit");
            } else if (character == '}' || character == ']') {
                depth--;
            }
        }
    }

    private static void validateJsonTree(JsonObject values) throws IOException {
        ArrayDeque<JsonNode> pending = new ArrayDeque<>();
        pending.push(new JsonNode(values, 0));
        int nodes = 0;
        long characters = 0;
        while (!pending.isEmpty()) {
            JsonNode node = pending.pop();
            if (++nodes > 20000 || node.depth > 64) throw new IOException("Plugin storage structure exceeds limit");
            if (node.value.isJsonObject()) {
                for (var entry : node.value.getAsJsonObject().entrySet()) {
                    characters += entry.getKey().length();
                    pending.push(new JsonNode(entry.getValue(), node.depth + 1));
                }
            } else if (node.value.isJsonArray()) {
                for (var item : node.value.getAsJsonArray()) pending.push(new JsonNode(item, node.depth + 1));
            } else if (node.value.isJsonPrimitive()) {
                characters += node.value.getAsString().length();
            }
            if (characters > 1048576 || pending.size() > 20000) throw new IOException("Plugin storage exceeds limit");
        }
    }

    private record JsonNode(com.google.gson.JsonElement value, int depth) {}
}
