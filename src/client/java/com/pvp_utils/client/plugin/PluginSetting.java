package com.pvp_utils.client.plugin;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.mozilla.javascript.Context;

import java.io.IOException;

public final class PluginSetting {
    private final String name;
    private final String type;
    private final double min;
    private final double max;
    private final PluginStorage storage;
    private Object value;

    public PluginSetting(String name, String type, Object defaultValue, double min, double max,
                         PluginStorage storage) {
        if (name.isBlank() || name.length() > 96) throw new IllegalArgumentException("Invalid setting name");
        this.name = name;
        this.type = type;
        this.min = min;
        this.max = max;
        this.storage = storage;
        value = normalize(defaultValue);
        JsonElement saved = JsonParser.parseString(storage.get("setting:" + name));
        if (!saved.isJsonNull()) {
            Object restored = switch (type) {
                case "bool" -> saved.getAsBoolean();
                case "number" -> saved.getAsDouble();
                default -> saved.getAsString();
            };
            value = normalize(restored);
        }
    }

    public String name() {
        return name;
    }

    public String type() {
        return type;
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    public Object value() {
        return value;
    }

    public void set(Object next) {
        Object normalized = normalize(next);
        JsonPrimitive json = switch (normalized) {
            case Boolean bool -> new JsonPrimitive(bool);
            case Number number -> new JsonPrimitive(number);
            default -> new JsonPrimitive(normalized.toString());
        };
        try {
            storage.set("setting:" + name, json.toString());
            value = normalized;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save plugin setting", e);
        }
    }

    private Object normalize(Object input) {
        return switch (type) {
            case "bool" -> Context.toBoolean(input);
            case "number" -> {
                double number = Context.toNumber(input);
                if (!Double.isFinite(number)) throw new IllegalArgumentException("Setting must be finite");
                yield Math.max(min, Math.min(max, number));
            }
            case "text", "color" -> {
                String text = Context.toString(input);
                if (text.length() > 512) throw new IllegalArgumentException("Setting text exceeds limit");
                if (type.equals("color") && !text.matches("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")) {
                    throw new IllegalArgumentException("Invalid setting color");
                }
                yield text;
            }
            default -> throw new IllegalArgumentException("Unknown setting type");
        };
    }
}
