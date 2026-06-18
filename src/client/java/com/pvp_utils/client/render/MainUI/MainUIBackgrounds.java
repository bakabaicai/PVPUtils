package com.pvp_utils.client.render.MainUI;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class MainUIBackgrounds {
    private static final Path EXTERNAL_PATH = FabricLoader.getInstance().getGameDir().resolve("PVPUtils/backgrounds");
    private static final String DEFAULT_IMAGE = "1.png";

    private MainUIBackgrounds() {}

    public static void init() {
        try {
            Files.createDirectories(EXTERNAL_PATH);
            Path target = EXTERNAL_PATH.resolve(DEFAULT_IMAGE);
            if (!Files.exists(target)) {
                try (InputStream is = MainUIBackgrounds.class.getResourceAsStream("/backgrounds/" + DEFAULT_IMAGE)) {
                    if (is != null) {
                        Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void openFolder() {
        init();
        Util.getPlatform().openPath(EXTERNAL_PATH);
    }

    public static List<String> listPngs() {
        init();
        List<String> files = new ArrayList<>();
        try (var stream = Files.list(EXTERNAL_PATH)) {
            stream.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.toLowerCase(Locale.ROOT).endsWith(".png"))
                    .sorted(Comparator.naturalOrder())
                    .forEach(files::add);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (files.isEmpty()) files.add(DEFAULT_IMAGE);
        return files;
    }

    public static Path resolve(String name) {
        init();
        String safeName = name == null || name.isBlank() ? DEFAULT_IMAGE : Path.of(name).getFileName().toString();
        return EXTERNAL_PATH.resolve(safeName);
    }
}
