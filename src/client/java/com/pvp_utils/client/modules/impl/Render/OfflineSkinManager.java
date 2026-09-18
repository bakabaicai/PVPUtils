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

public final class OfflineSkinManager {
    private static final Path EXTERNAL_PATH = FabricLoader.getInstance().getGameDir().resolve("PVPUtils/skin");
    private static boolean initialized;
    private static DynamicTexture texture;
    private static Identifier loadedTextureId;
    private static String loaded = "";
    private static String lastLoggedTexture = "";
    private static String lastLoggedList = "";

    private OfflineSkinManager() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        try {
            Files.createDirectories(EXTERNAL_PATH);
        } catch (IOException e) {
            System.out.println("[PVPUtils] OfflineSkin init failed: " + e.getMessage());
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
            System.out.println("[PVPUtils] OfflineSkin list failed: " + e.getMessage());
        }
        if (!files.isEmpty() && !files.toString().equals(lastLoggedList)) {
            lastLoggedList = files.toString();
            System.out.println("[PVPUtils] OfflineSkin available pngs: " + lastLoggedList);
        }
        return files;
    }

    public static String currentName() {
        List<String> files = listPngs();
        if (files.isEmpty()) return Config.isChinese ? "无皮肤文件" : "No skin file";
        if (Config.offlineSkinImage == null || Config.offlineSkinImage.isBlank() || !files.contains(Config.offlineSkinImage)) {
            return files.get(0);
        }
        return Config.offlineSkinImage;
    }

    public static void cycleSkin() {
        List<String> files = listPngs();
        if (files.isEmpty()) {
            System.out.println("[PVPUtils] OfflineSkin folder is empty, put a 64x64 PNG in " + EXTERNAL_PATH.toAbsolutePath());
            return;
        }
        int index = files.indexOf(Config.offlineSkinImage);
        Config.offlineSkinImage = files.get((index + 1 + files.size()) % files.size());
        Config.save();
        destroy();
        System.out.println("[PVPUtils] OfflineSkin switched to " + Config.offlineSkinImage);
    }

    public static ClientAsset.Texture texture() {
        if (!Config.offlineSkin) return null;
        init();
        List<String> files = listPngs();
        if (files.isEmpty()) return null;
        String selected = Config.offlineSkinImage;
        if (selected == null || selected.isBlank() || !files.contains(selected)) {
            selected = files.get(0);
            Config.offlineSkinImage = selected;
            Config.save();
        }
        Path path = EXTERNAL_PATH.resolve(selected);
        if (!Files.exists(path)) return null;
        String key = selected.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        Identifier texturePath = Identifier.fromNamespaceAndPath("pvp_utils", "offline_skin/" + key);
        if (texture == null || !selected.equals(loaded)) {
            destroy();
            try {
                NativeImage image = NativeImage.read(Files.newInputStream(path));
                texture = new DynamicTexture("pvp_utils:offline_skin/" + key, image.getWidth(), image.getHeight(), false);
                Minecraft.getInstance().getTextureManager().register(texturePath, texture);
                GpuTexture gpuTexture = texture.getTexture();
                RenderSystem.getDevice().createCommandEncoder().writeToTexture(gpuTexture, image);
                image.close();
                loadedTextureId = texturePath;
                loaded = selected;
            } catch (IOException e) {
                System.out.println("[PVPUtils] OfflineSkin texture upload failed: " + e.getMessage());
                return null;
            }
        }
        String textureLog = selected + " -> " + texturePath + " exists=" + Files.exists(path) + " registered=" + (loadedTextureId != null);
        if (!textureLog.equals(lastLoggedTexture)) {
            lastLoggedTexture = textureLog;
            System.out.println("[PVPUtils] OfflineSkin texture: " + textureLog);
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
