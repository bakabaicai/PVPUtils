package com.pvp_utils.client.NeteaseMusic;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.humbleui.skija.Image;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class NeteaseMusicCovers {
    public static final int TEXTURE_SIZE = 256;

    private static final ExecutorService IO = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "PVPUtils-NeteaseCover");
        thread.setDaemon(true);
        return thread;
    });
    private static final AtomicInteger IDS = new AtomicInteger();
    private static final Map<String, CoverTexture> CACHE = new ConcurrentHashMap<>();

    private NeteaseMusicCovers() {
    }

    public static Identifier texture(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        CoverTexture cover = CACHE.computeIfAbsent(url, NeteaseMusicCovers::request);
        requestMinecraftTexture(cover);
        return cover.location;
    }

    public static Image skiaImage(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        return CACHE.computeIfAbsent(url, NeteaseMusicCovers::request).skiaImage;
    }

    public static void preload(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        CACHE.computeIfAbsent(url, NeteaseMusicCovers::request);
    }

    public static void clear() {
        Minecraft client = Minecraft.getInstance();
        for (CoverTexture cover : CACHE.values()) {
            Identifier location = cover.location;
            if (location != null && client.getTextureManager() != null) {
                client.getTextureManager().release(location);
            }
            if (cover.skiaImage != null) {
                cover.skiaImage.close();
                cover.skiaImage = null;
            }
        }
        CACHE.clear();
    }

    private static CoverTexture request(String url) {
        CoverTexture cover = new CoverTexture();
        CompletableFuture.supplyAsync(() -> load(url), IO).whenComplete((loaded, throwable) -> {
            if (throwable != null || loaded == null) {
                cover.failed = true;
                return;
            }
            cover.encoded = loaded.encoded();
            cover.skiaImage = loaded.skiaImage();
            if (cover.minecraftTextureRequested) {
                requestMinecraftTexture(cover);
            }
        });
        return cover;
    }

    private static LoadedCover load(String url) {
        try {
            byte[] bytes = url.startsWith("data:image/") ? decodeDataImage(url) : NeteaseMusicApi.getBytes(url);
            Image image = Image.makeFromEncoded(bytes);
            return image == null ? null : new LoadedCover(bytes, image);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void requestMinecraftTexture(CoverTexture cover) {
        byte[] encoded;
        synchronized (cover) {
            cover.minecraftTextureRequested = true;
            if (cover.location != null || cover.minecraftTextureLoading || cover.encoded == null) {
                return;
            }
            cover.minecraftTextureLoading = true;
            encoded = cover.encoded;
        }
        CompletableFuture.supplyAsync(() -> makeNativeImage(encoded), IO).whenComplete((image, throwable) -> {
            if (throwable != null || image == null) {
                synchronized (cover) {
                    cover.minecraftTextureLoading = false;
                }
                return;
            }
            Minecraft.getInstance().execute(() -> {
                Identifier id = Identifier.fromNamespaceAndPath("pvp_utils", "netease_music/cover_" + IDS.incrementAndGet());
                DynamicTexture texture = new DynamicTexture(() -> id.toString(), image);
                Minecraft.getInstance().getTextureManager().register(id, texture);
                synchronized (cover) {
                    cover.location = id;
                    cover.minecraftTextureLoading = false;
                }
            });
        });
    }

    private static NativeImage makeNativeImage(byte[] bytes) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
            if (source == null) {
                return null;
            }
            BufferedImage scaled = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = scaled.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.drawImage(source, 0, 0, TEXTURE_SIZE, TEXTURE_SIZE, null);
            } finally {
                graphics.dispose();
            }
            NativeImage image = new NativeImage(TEXTURE_SIZE, TEXTURE_SIZE, true);
            int[] row = new int[TEXTURE_SIZE];
            for (int y = 0; y < TEXTURE_SIZE; y++) {
                scaled.getRGB(0, y, TEXTURE_SIZE, 1, row, 0, TEXTURE_SIZE);
                for (int x = 0; x < TEXTURE_SIZE; x++) {
                    image.setPixel(x, y, row[x]);
                }
            }
            return image;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static byte[] decodeDataImage(String url) {
        int comma = url.indexOf(',');
        if (comma < 0) {
            return new byte[0];
        }
        return Base64.getDecoder().decode(url.substring(comma + 1));
    }

    private static final class CoverTexture {
        private volatile Identifier location;
        private volatile Image skiaImage;
        private volatile byte[] encoded;
        private volatile boolean failed;
        private boolean minecraftTextureRequested;
        private boolean minecraftTextureLoading;
    }

    private record LoadedCover(byte[] encoded, Image skiaImage) {
    }
}
