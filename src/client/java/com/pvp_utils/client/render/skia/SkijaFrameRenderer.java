package com.pvp_utils.client.render.skia;

import com.pvp_utils.client.modules.impl.Render.ClockHudRenderer;
import com.pvp_utils.client.modules.impl.Render.DynamicIsland.DynamicIslandRenderer;
import com.pvp_utils.client.modules.impl.Render.HudEditOverlay;
import com.pvp_utils.client.modules.impl.Render.KeystrokesRenderer;
import com.pvp_utils.client.modules.impl.Render.MusicInfoHudRenderer;
import com.pvp_utils.client.modules.impl.Render.PingHudRenderer;
import com.pvp_utils.client.modules.impl.Render.PotionStatusRenderer;
import com.pvp_utils.client.modules.impl.Render.SkiaTextHudRenderer;
import com.pvp_utils.client.modules.impl.Render.TpsHudRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;

public final class SkijaFrameRenderer {
    private SkijaFrameRenderer() {
    }

    public static void render() {
        Minecraft client = Minecraft.getInstance();
        if (client.getWindow().isMinimized()) return;
        Overlay overlay = client.getOverlay();
        if (overlay != null) {
            if (!(overlay instanceof SkijaOverlay skijaOverlay) || !skijaOverlay.isSkijaOverlayActive()) return;
            SkijaRenderer.renderOverlay(canvas -> {
                if (skijaOverlay.rendersScreenBehind()) {
                    if (client.screen instanceof SkijaScreen screen) SkijaRenderer.draw(screen::renderSkija);
                    SkijaRenderer.renderSubmitted();
                }
                SkijaRenderer.draw(skijaOverlay::renderSkija);
            });
            return;
        }
        SkijaRenderer.renderOverlay(canvas -> {
            if (client.level != null && !client.options.hideGui) {
                SkijaRenderer.draw(PotionStatusRenderer.getInstance()::renderSkija);
                SkijaRenderer.draw(KeystrokesRenderer.getInstance()::renderSkija);
                SkijaRenderer.draw(DynamicIslandRenderer.getInstance()::renderSkija);
                SkijaRenderer.draw(hudCanvas -> SkiaTextHudRenderer.renderSkija(hudCanvas,
                PingHudRenderer.getInstance(),
                TpsHudRenderer.getInstance(),
                ClockHudRenderer.getInstance()));
                SkijaRenderer.draw(MusicInfoHudRenderer.getInstance()::renderSkija);
                SkijaRenderer.draw(hudCanvas -> com.pvp_utils.client.plugin.PluginManager.INSTANCE.render(
                        hudCanvas, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight()));
            }
            if (client.screen instanceof SkijaScreen screen) SkijaRenderer.draw(screen::renderSkija);
            SkijaRenderer.renderSubmitted();
            SkijaRenderer.draw(HudEditOverlay.getInstance()::renderSkija);
        });
    }

    public static void close() {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.getOverlay() instanceof SkijaOverlay overlay) overlay.close();
        DynamicIslandRenderer.getInstance().close();
        ClockHudRenderer.getInstance().close();
        SkijaRenderer.close();
    }
}
