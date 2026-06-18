package com.pvp_utils.client.modules.impl.Render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.pvp_utils.Config;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
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

public final class CustomCapeManager {
    private static final Path EXTERNAL_PATH = FabricLoader.getInstance().getGameDir().resolve("PVPUtils/cape");
    private static final String DEFAULT_IMAGE = "default.png";
    private static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath("pvp_utils", "custom_cape");

    private static DynamicTexture texture;
    private static String loaded = "";

    private CustomCapeManager() {}

    public static void init() {
        try {
            Files.createDirectories(EXTERNAL_PATH);
            Path target = EXTERNAL_PATH.resolve(DEFAULT_IMAGE);
            try (InputStream is = CustomCapeManager.class.getResourceAsStream("/cape/" + DEFAULT_IMAGE)) {
                if (is != null) {
                    Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
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

    public static void cycleCape() {
        List<String> files = listPngs();
        int index = files.indexOf(Config.customCapeImage);
        Config.customCapeImage = files.get((index + 1 + files.size()) % files.size());
        Config.save();
        loaded = "";
    }

    public static Identifier texture() {
        if (!Config.customCape) return null;
        init();
        String selected = Config.customCapeImage == null || Config.customCapeImage.isBlank() ? DEFAULT_IMAGE : Path.of(Config.customCapeImage).getFileName().toString();
        Path path = EXTERNAL_PATH.resolve(selected);
        if (!Files.exists(path)) {
            selected = DEFAULT_IMAGE;
            Config.customCapeImage = selected;
            Config.save();
            path = EXTERNAL_PATH.resolve(selected);
        }
        if (texture != null && selected.equals(loaded)) return TEXTURE_ID;
        destroy();
        try {
            NativeImage image = NativeImage.read(Files.newInputStream(path));
            texture = new DynamicTexture("pvp_utils:custom_cape", image.getWidth(), image.getHeight(), false);
            Minecraft.getInstance().getTextureManager().register(TEXTURE_ID, texture);
            GpuTexture gpuTexture = texture.getTexture();
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToTexture(gpuTexture, image);
            image.close();
            loaded = selected;
            return TEXTURE_ID;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void destroy() {
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(TEXTURE_ID);
            texture = null;
        }
        loaded = "";
    }
}
