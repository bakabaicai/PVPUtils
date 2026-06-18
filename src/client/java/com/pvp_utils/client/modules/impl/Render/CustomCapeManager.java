package com.pvp_utils.client.modules.impl.Render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.pvp_utils.Config;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
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
    private static boolean initLogged;
    private static DynamicTexture texture;
    private static Identifier loadedTextureId;
    private static String loaded = "";
    private static String lastLoggedTexture = "";
    private static String lastLoggedList = "";
    private static boolean initialized;

    private CustomCapeManager() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        try {
            Files.createDirectories(EXTERNAL_PATH);
            Path target = EXTERNAL_PATH.resolve(DEFAULT_IMAGE);
            try (InputStream is = CustomCapeManager.class.getResourceAsStream("/cape/" + DEFAULT_IMAGE)) {
                if (is != null) {
                    Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
                    if (!initLogged) {
                        initLogged = true;
                        System.out.println("[PVPUtils] CustomCape extracted default cape to " + target.toAbsolutePath());
                    }
                } else {
                    System.out.println("[PVPUtils] CustomCape default cape resource missing: /cape/" + DEFAULT_IMAGE);
                }
            }
        } catch (IOException e) {
            System.out.println("[PVPUtils] CustomCape init failed: " + e.getMessage());
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
            System.out.println("[PVPUtils] CustomCape list failed: " + e.getMessage());
            e.printStackTrace();
        }
        if (files.isEmpty()) files.add(DEFAULT_IMAGE);
        String listLog = files.toString();
        if (!listLog.equals(lastLoggedList)) {
            lastLoggedList = listLog;
            System.out.println("[PVPUtils] CustomCape available pngs: " + listLog);
        }
        return files;
    }

    public static void cycleCape() {
        List<String> files = listPngs();
        int index = files.indexOf(Config.customCapeImage);
        Config.customCapeImage = files.get((index + 1 + files.size()) % files.size());
        Config.save();
        destroy();
        System.out.println("[PVPUtils] CustomCape switched to " + Config.customCapeImage);
    }

    public static ClientAsset.Texture texture() {
        if (!Config.customCape) return null;
        init();
        String selected = Config.customCapeImage == null || Config.customCapeImage.isBlank() ? DEFAULT_IMAGE : Path.of(Config.customCapeImage).getFileName().toString();
        Path path = EXTERNAL_PATH.resolve(selected);
        if (!Files.exists(path)) {
            System.out.println("[PVPUtils] CustomCape selected file missing, fallback to default: " + path.toAbsolutePath());
            selected = DEFAULT_IMAGE;
            Config.customCapeImage = selected;
            Config.save();
            path = EXTERNAL_PATH.resolve(selected);
        }
        String key = selected.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        Identifier texturePath = Identifier.fromNamespaceAndPath("pvp_utils", "custom_cape/" + key);
        if (texture == null || !selected.equals(loaded)) {
            destroy();
            try {
                NativeImage image = NativeImage.read(Files.newInputStream(path));
                texture = new DynamicTexture("pvp_utils:custom_cape/" + key, image.getWidth(), image.getHeight(), false);
                Minecraft.getInstance().getTextureManager().register(texturePath, texture);
                GpuTexture gpuTexture = texture.getTexture();
                RenderSystem.getDevice().createCommandEncoder().writeToTexture(gpuTexture, image);
                image.close();
                loadedTextureId = texturePath;
                loaded = selected;
            } catch (IOException e) {
                System.out.println("[PVPUtils] CustomCape texture upload failed: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }
        String textureLog = selected + " -> " + texturePath + " exists=" + Files.exists(path) + " registered=" + (loadedTextureId != null);
        if (!textureLog.equals(lastLoggedTexture)) {
            lastLoggedTexture = textureLog;
            System.out.println("[PVPUtils] CustomCape texture: " + textureLog);
        }
        return new ClientAsset.ResourceTexture(texturePath, texturePath);
    }

    private static void destroy() {
        if (loadedTextureId != null) {
            Minecraft.getInstance().getTextureManager().release(loadedTextureId);
        }
        texture = null;
        loadedTextureId = null;
        loaded = "";
    }
}
