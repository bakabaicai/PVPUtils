package com.pvp_utils.client.skin;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class OfflineSkinManager {
    private static final Gson GSON = new Gson();
    private static final List<OfflineSkin> SKINS = new ArrayList<>();
    private static int selected = 0;

    private OfflineSkinManager() {
    }

    public static void load() {
        SKINS.clear();
        selected = 0;
        Path file = file();
        if (file == null || !Files.exists(file)) return;
        try {
            List<OfflineSkin> loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8),
                    new TypeToken<List<OfflineSkin>>() {
                    }.getType());
            if (loaded != null) SKINS.addAll(loaded);
        } catch (Exception ignored) {
        }
    }

    public static List<OfflineSkin> skins() {
        return List.copyOf(SKINS);
    }

    public static void add(OfflineSkin skin) {
        if (skin == null) return;
        SKINS.add(skin);
        save();
    }

    public static void remove(int index) {
        if (index < 0 || index >= SKINS.size()) return;
        SKINS.remove(index);
        if (selected >= SKINS.size()) selected = Math.max(0, SKINS.size() - 1);
        save();
    }

    public static int selected() {
        return SKINS.isEmpty() ? -1 : Math.max(0, Math.min(selected, SKINS.size() - 1));
    }

    public static void select(int index) {
        if (SKINS.isEmpty()) return;
        selected = Math.max(0, Math.min(index, SKINS.size() - 1));
        save();
    }

    public static void quickSwitchNext() {
        if (SKINS.isEmpty()) return;
        selected = (selected + 1) % SKINS.size();
        save();
    }

    public static void quickSwitchPrevious() {
        if (SKINS.isEmpty()) return;
        selected = (selected - 1 + SKINS.size()) % SKINS.size();
        save();
    }

    public static String folderPath() {
        Path folder = file() == null ? null : file().getParent();
        return folder == null ? "unavailable" : folder.toAbsolutePath().toString();
    }

    public static void openFolder() {
        Path folder = file() == null ? null : file().getParent();
        if (folder == null) return;
        try {
            Files.createDirectories(folder);
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(folder.toFile());
            }
        } catch (Exception ignored) {
        }
    }

    private static void save() {
        Path file = file();
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(SKINS), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static Path file() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gameDirectory == null) return null;
        return client.gameDirectory.toPath().resolve("pvp_utils").resolve("offline_skins.json");
    }

    public record OfflineSkin(String name, String texturePath) {
    }
}
