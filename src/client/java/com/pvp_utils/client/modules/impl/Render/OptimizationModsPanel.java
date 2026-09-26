package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class OptimizationModsPanel {
    private static final OptimizationModsPanel INSTANCE = new OptimizationModsPanel();
    private OptimizationModsPanel() {}
    public static OptimizationModsPanel getInstance() { return INSTANCE; }

    private static final String[][] MODS = {
        {"sodium", "Sodium", "0x55FF55"},
        {"lithium", "Lithium", "0x55FF55"},
        {"ferritecore", "FerriteCore", "0x55FF55"},
        {"indium", "Indium", "0x55FF55"},
        {"iris", "Iris", "0x55FF55"},
        {"entityculling", "EntityCulling", "0x55FF55"},
        {"dynamicfps", "Dynamic FPS", "0x55FF55"},
        {"immediatelyfast", "ImmediatelyFast", "0x55FF55"},
        {"fpsreducer", "FPS Reducer", "0x55FF55"},
        {"modernfix", "ModernFix", "0x55FF55"}
    };

    public void render(GuiGraphics graphics) {
        if (!Config.optimizationModsPanel) return;
        Minecraft client = Minecraft.getInstance();
        int y = 10;
        graphics.drawString(client.font, "Optimization Mods:", 10, y, 0xFFFFFF, true);
        y += 12;
        for (String[] mod : MODS) {
            String id = mod[0];
            String name = mod[1];
            int color = Integer.parseInt(mod[2].substring(2), 16) << 8 | 0xFF;
            boolean loaded = FabricLoader.getInstance().isModLoaded(id);
            String status = loaded ? "OK" : "NOT INSTALLED";
            int statusColor = loaded ? 0x55FF55 : 0xFF5555;
            graphics.drawString(client.font, name + ": " + status, 20, y, statusColor, true);
            y += 10;
        }
    }
}
