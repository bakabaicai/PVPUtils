package com.pvp_utils.client.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.mozilla.javascript.Context;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class PluginInput {
    private static final long HOLD_NANOS = 5_000_000_000L;
    private static final List<String> KEYS = List.of("forward", "back", "left", "right", "jump", "sneak", "sprint");
    private final Map<String, Long> held = new LinkedHashMap<>();

    String call(String operation, Object[] arguments) {
        return switch (operation) {
            case "hold" -> hold(arguments);
            case "release" -> release(arguments);
            case "releaseAll" -> releaseAllCommand();
            case "status" -> status();
            default -> throw new IllegalArgumentException("Unknown input operation: " + operation);
        };
    }

    void apply(Minecraft client) {
        if (held.isEmpty()) {
            return;
        }
        long now = System.nanoTime();
        held.entrySet().removeIf(entry -> entry.getValue() < now);
        if (held.isEmpty()) {
            return;
        }
        for (String name : held.keySet()) {
            KeyMapping key = key(client, name);
            if (key != null) {
                key.setDown(true);
            }
        }
    }

    void releaseAll() {
        if (held.isEmpty()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        for (String name : List.copyOf(held.keySet())) {
            KeyMapping key = key(client, name);
            if (key != null) {
                key.setDown(false);
            }
        }
        held.clear();
    }

    private String hold(Object[] arguments) {
        String name = normalize(PluginRuntime.string(arguments, 0));
        boolean pressed = arguments.length < 2 || Context.toBoolean(arguments[1]);
        if (!KEYS.contains(name)) {
            throw new IllegalArgumentException("Unknown input key, available: " + String.join(", ", KEYS));
        }
        if (pressed) {
            held.put(name, System.nanoTime() + HOLD_NANOS);
            KeyMapping key = key(Minecraft.getInstance(), name);
            if (key != null) {
                key.setDown(true);
            }
        } else {
            held.remove(name);
            KeyMapping key = key(Minecraft.getInstance(), name);
            if (key != null) {
                key.setDown(false);
            }
        }
        return status();
    }

    private String release(Object[] arguments) {
        String name = normalize(PluginRuntime.string(arguments, 0));
        held.remove(name);
        KeyMapping key = key(Minecraft.getInstance(), name);
        if (key != null) {
            key.setDown(false);
        }
        return status();
    }

    private String releaseAllCommand() {
        releaseAll();
        return status();
    }

    private String status() {
        JsonObject data = new JsonObject();
        JsonArray active = new JsonArray();
        held.keySet().forEach(active::add);
        data.add("held", active);
        data.addProperty("holdSeconds", HOLD_NANOS / 1_000_000_000L);
        data.addProperty("available", String.join(",", KEYS));
        return data.toString();
    }

    private static String normalize(String name) {
        return name.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static KeyMapping key(Minecraft client, String name) {
        if (client == null || client.options == null) {
            return null;
        }
        return switch (name) {
            case "forward" -> client.options.keyUp;
            case "back" -> client.options.keyDown;
            case "left" -> client.options.keyLeft;
            case "right" -> client.options.keyRight;
            case "jump" -> client.options.keyJump;
            case "sneak" -> client.options.keyShift;
            case "sprint" -> client.options.keySprint;
            default -> null;
        };
    }
}
