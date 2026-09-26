package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class FpsHudRenderer {
    private static final FpsHudRenderer INSTANCE = new FpsHudRenderer();
    private FpsHudRenderer() {}
    public static FpsHudRenderer getInstance() { return INSTANCE; }

    public void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        if (Config.fpsHud) {
            String fps = "FPS: " + client.getFps();
            graphics.drawString(client.font, fps, (int) Config.fpsHudX, (int) Config.fpsHudY, 0x55FF55, true);
        }
        if (Config.memoryHud) {
            Runtime rt = Runtime.getRuntime();
            long used = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
            long max = rt.maxMemory() / 1024 / 1024;
            String mem = "Mem: " + used + "MB / " + max + "MB";
            graphics.drawString(client.font, mem, (int) Config.fpsHudX, (int) Config.fpsHudY + 10, 0x55FFFF, true);
        }
    }
}
