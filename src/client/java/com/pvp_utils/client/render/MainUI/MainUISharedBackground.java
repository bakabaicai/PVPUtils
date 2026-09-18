package com.pvp_utils.client.render.MainUI;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.MainUI.MainUIBackgrounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.clamp;

public final class MainUISharedBackground {
    private static String activeShaderPath;
    private static MainUIShader shader;
    private static MainUIVideoBackground videoBackground;
    // 必须与 PVPUtilsMainUI.BACKGROUND_TEXTURE_ID 不同:两处各自 register/release,
    // 共用同一 ID 会在主界面 removed() 时把这里仍持有的纹理从管理器中释放掉(纹理丢失/紫黑贴图)
    private static final Identifier BACKGROUND_TEXTURE_ID = Identifier.fromNamespaceAndPath("pvp_utils", "mainui_shared_background");

    private MainUISharedBackground() {
    }

    public static void setActiveShader(String shaderPath) {
        if (shaderPath == null || shaderPath.isBlank()) {
            return;
        }
        activeShaderPath = shaderPath;
    }

    public static String activeShaderPath() {
        return activeShaderPath;
    }

    public static boolean shouldReplace(Screen screen) {
        Minecraft client = Minecraft.getInstance();
        return Config.useMainUI
                && screen != null
                && !(screen instanceof TitleScreen)
                && client.level == null;
    }

    public static boolean shouldReplaceLoadingOverlay() {
        Minecraft client = Minecraft.getInstance();
        return Config.useMainUI
                && client != null
                && client.level == null;
    }

    public static void render(GuiGraphics graphics, int mouseX, int mouseY) {
        render(graphics, mouseX, mouseY, getBackgroundTexture(), backgroundTextureW, backgroundTextureH, backgroundOffsetX, backgroundOffsetY);
    }

    private static DynamicTexture backgroundTexture;
    private static int backgroundTextureW = -1;
    private static int backgroundTextureH = -1;
    private static float backgroundOffsetX;
    private static float backgroundOffsetY;
    private static String loadedBackground = "";
    private static int lastWindowPixelW = -1;
    private static int lastWindowPixelH = -1;

    private static DynamicTexture getBackgroundTexture() {
        if (Config.mainUIBackgroundMode != Config.MainUIBackgroundMode.IMAGE) {
            return null;
        }
        Minecraft client = Minecraft.getInstance();
        int windowPixelW = client.getWindow().getWidth();
        int windowPixelH = client.getWindow().getHeight();
        if (windowPixelW <= 0 || windowPixelH <= 0) return null;
        if (lastWindowPixelW != -1 && (lastWindowPixelW != windowPixelW || lastWindowPixelH != windowPixelH)) {
            destroyBackgroundTexture();
            backgroundOffsetX = 0f;
            backgroundOffsetY = 0f;
        }
        lastWindowPixelW = windowPixelW;
        lastWindowPixelH = windowPixelH;

        String selected = Config.mainUIBackgroundImage == null || Config.mainUIBackgroundImage.isBlank() ? "1.png" : Config.mainUIBackgroundImage;
        if (backgroundTexture != null && selected.equals(loadedBackground)) return backgroundTexture;
        destroyBackgroundTexture();

        Path path = MainUIBackgrounds.resolve(selected);
        if (!Files.exists(path)) {
            List<String> files = MainUIBackgrounds.listPngs();
            selected = files.isEmpty() ? "1.png" : files.get(0);
            Config.mainUIBackgroundImage = selected;
            Config.save();
            path = MainUIBackgrounds.resolve(selected);
        }

        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) return null;
            int width = image.getWidth();
            int height = image.getHeight();
            ByteBuffer buffer = MemoryUtil.memAlloc(width * height * 4);
            for (int py = 0; py < height; py++) {
                for (int px = 0; px < width; px++) {
                    int argb = image.getRGB(px, py);
                    buffer.put((byte) ((argb >> 16) & 255));
                    buffer.put((byte) ((argb >> 8) & 255));
                    buffer.put((byte) (argb & 255));
                    buffer.put((byte) ((argb >>> 24) & 255));
                }
            }
            buffer.flip();
            backgroundTexture = new DynamicTexture("pvp_utils:mainui_custom_background", width, height, false);
            client.getTextureManager().register(BACKGROUND_TEXTURE_ID, backgroundTexture);
            GpuTexture gpuTexture = backgroundTexture.getTexture();
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToTexture(gpuTexture, buffer, NativeImage.Format.RGBA, 0, 0, 0, 0, width, height);
            GL11.glFlush();
            MemoryUtil.memFree(buffer);
            backgroundTextureW = width;
            backgroundTextureH = height;
            loadedBackground = selected;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return backgroundTexture;
    }

    private static void destroyBackgroundTexture() {
        if (backgroundTexture != null) {
            Minecraft.getInstance().getTextureManager().release(BACKGROUND_TEXTURE_ID);
            backgroundTexture = null;
        }
        backgroundTextureW = -1;
        backgroundTextureH = -1;
        loadedBackground = "";
    }

    public static void render(GuiGraphics graphics, int mouseX, int mouseY,
                              DynamicTexture backgroundTexture, int backgroundTextureW, int backgroundTextureH,
                              float backgroundOffsetX, float backgroundOffsetY) {
        Minecraft client = Minecraft.getInstance();
        graphics.fill(0, 0, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight(), 0xFF000000);
        if (Config.mainUIBackgroundMode == Config.MainUIBackgroundMode.VIDEO) {
            if (videoBackground == null) {
                videoBackground = new MainUIVideoBackground();
            }
            if (videoBackground.render(graphics, Config.mainUIVideoBackground)) {
                return;
            }
            renderVideoUnavailable(graphics, client);
            return;
        }
        if (Config.mainUIBackgroundMode == Config.MainUIBackgroundMode.IMAGE && backgroundTexture != null && backgroundTextureW > 0 && backgroundTextureH > 0) {
            float coverScale = Math.max(client.getWindow().getGuiScaledWidth() / (float) backgroundTextureW, client.getWindow().getGuiScaledHeight() / (float) backgroundTextureH);
            if (Config.mainUIMouseEffect) {
                coverScale *= 1.18f;
                float minW = client.getWindow().getGuiScaledWidth() * 1.16f;
                float minH = client.getWindow().getGuiScaledHeight() * 1.16f;
                coverScale = Math.max(coverScale, minW / backgroundTextureW);
                coverScale = Math.max(coverScale, minH / backgroundTextureH);
            } else {
                coverScale *= 1.08f;
            }
            float drawW = backgroundTextureW * coverScale;
            float drawH = backgroundTextureH * coverScale;
            float targetOffsetX = 0f;
            float targetOffsetY = 0f;
            float maxOffsetX = Math.max(0f, (drawW - client.getWindow().getGuiScaledWidth()) * 0.5f);
            float maxOffsetY = Math.max(0f, (drawH - client.getWindow().getGuiScaledHeight()) * 0.5f);
            if (Config.mainUIMouseEffect) {
                float overflowX = Math.max(0f, drawW - client.getWindow().getGuiScaledWidth());
                float overflowY = Math.max(0f, drawH - client.getWindow().getGuiScaledHeight());
                float dragX = Math.max(overflowX * 0.62f, client.getWindow().getGuiScaledWidth() * 0.06f);
                float dragY = Math.max(overflowY * 0.62f, client.getWindow().getGuiScaledHeight() * 0.06f);
                targetOffsetX = ((mouseX / Math.max(1f, (float) client.getWindow().getGuiScaledWidth())) - 0.5f) * -dragX;
                targetOffsetY = ((mouseY / Math.max(1f, (float) client.getWindow().getGuiScaledHeight())) - 0.5f) * -dragY;
            }
            targetOffsetX += backgroundOffsetX;
            targetOffsetY += backgroundOffsetY;
            targetOffsetX = clamp(targetOffsetX, -maxOffsetX, maxOffsetX);
            targetOffsetY = clamp(targetOffsetY, -maxOffsetY, maxOffsetY);

            int x = Math.round((client.getWindow().getGuiScaledWidth() - drawW) * 0.5f + targetOffsetX);
            int y = Math.round((client.getWindow().getGuiScaledHeight() - drawH) * 0.5f + targetOffsetY);
            graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE_ID, x, y, 0f, 0f, Math.round(drawW), Math.round(drawH), backgroundTextureW, backgroundTextureH, backgroundTextureW, backgroundTextureH);
            return;
        }
        String fixedShaderPath = fixedShaderPath();
        if (fixedShaderPath != null && !fixedShaderPath.equals(activeShaderPath)) {
            activeShaderPath = fixedShaderPath;
        }
        if (shader == null) {
            shader = activeShaderPath == null ? MainUIShader.random() : MainUIShader.named(activeShaderPath);
            activeShaderPath = shader.fragmentPath();
        } else if (activeShaderPath != null && !activeShaderPath.equals(shader.fragmentPath())) {
            if (shader != null) {
                shader.close();
            }
            shader = MainUIShader.named(activeShaderPath);
        }
        shader.render(graphics, mouseX, mouseY);
    }

    private static String fixedShaderPath() {
        if (Config.mainUIBackgroundMode != Config.MainUIBackgroundMode.GLSL || Config.mainUIGlslMode != Config.MainUIGlslMode.FIXED) {
            return null;
        }
        Config.mainUIGlslShader = MainUIShader.normalizeShader(Config.mainUIGlslShader);
        return Config.mainUIGlslShader;
    }

    private static void renderVideoUnavailable(GuiGraphics graphics, Minecraft client) {
        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();
        graphics.fill(0, 0, width, height, 0xFF05070A);
        String title = Config.isChinese ? "视频背景不可用" : "Video background unavailable";
        String reason = videoBackground == null || videoBackground.getLastError().isBlank()
                ? (Config.isChinese ? "视频文件无法解码" : "Video file could not be decoded")
                : videoBackground.getLastError();
        int x = width / 2;
        int y = height / 2;
        graphics.drawString(client.font, title, x - client.font.width(title) / 2, y - 12, 0xFFFFD176, true);
        graphics.drawString(client.font, reason, x - client.font.width(reason) / 2, y + 4, 0xFFE5E7EB, true);
    }

    public static void close() {
        if (shader != null) {
            shader.close();
            shader = null;
        }
        if (videoBackground != null) {
            videoBackground.close();
            videoBackground = null;
        }
    }
}
