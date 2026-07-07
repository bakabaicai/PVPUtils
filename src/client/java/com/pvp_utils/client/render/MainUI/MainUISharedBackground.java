package com.pvp_utils.client.render.MainUI;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;

public final class MainUISharedBackground {
    private static String activeShaderPath;
    private static MainUIShader shader;

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
        if (shader == null || activeShaderPath == null || !activeShaderPath.equals(shader.fragmentPath())) {
            if (shader != null) {
                shader.close();
            }
            shader = activeShaderPath == null ? MainUIShader.random() : MainUIShader.named(activeShaderPath);
        }
        shader.render(graphics, mouseX, mouseY);
    }

    public static void close() {
        if (shader != null) {
            shader.close();
            shader = null;
        }
    }
}
