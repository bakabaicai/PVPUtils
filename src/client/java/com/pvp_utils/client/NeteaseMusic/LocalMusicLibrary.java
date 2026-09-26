package com.pvp_utils.client.NeteaseMusic;

import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class LocalMusicLibrary {
    private static final Path LIBRARY_DIR = FabricLoader.getInstance().getGameDir().resolve("PVPUtils").resolve("local-music");
    private static final List<String> songs = new ArrayList<>();

    static {
        try {
            Files.createDirectories(LIBRARY_DIR);
            refresh();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void refresh() {
        songs.clear();
        try (Stream<Path> stream = Files.list(LIBRARY_DIR)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".mp3"))
                  .forEach(p -> songs.add(p.getFileName().toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getSongs() {
        return new ArrayList<>(songs);
    }

    public static Path getSongPath(String name) {
        return LIBRARY_DIR.resolve(name);
    }

    public static Path getLibraryDir() {
        return LIBRARY_DIR;
    }
}
