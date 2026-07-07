package com.pvp_utils.client.render.MainUI;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;

public final class MainUISharedBackground {
    private static String activeShaderPath;
    private static MainUIShader shader;
    private static MainUIVideoBackground videoBackground;

    private MainUISharedBackground() {
    }

    public static void setActiveShader(String shaderPath) {
        if (shaderPath == null || shaderPath.isBlank()) {
            return;
        }
        activeShaderPath = shaderPath;
    }

    public static boolean shouldReplace(Screen screen) {
        Minecraft client = Minecraft.getInstance();
        return Config.useMainUI
                && screen != null
                && !(screen instanceof TitleScreen)
                && client.level == null;
    }

    public static void render(GuiGraphics graphics, int mouseX, int mouseY) {
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
        if (shader == null || activeShaderPath == null || !activeShaderPath.equals(shader.fragmentPath())) {
            if (shader != null) {
                shader.close();
            }
            shader = activeShaderPath == null ? MainUIShader.random() : MainUIShader.named(activeShaderPath);
        }
        shader.render(graphics, mouseX, mouseY);
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
