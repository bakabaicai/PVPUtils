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

public final class SkijaFrameRenderer {
    private SkijaFrameRenderer() {
    }

    public static void render() {
        Minecraft client = Minecraft.getInstance();
        if (client.getWindow().isMinimized() || client.getOverlay() != null) return;
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
                LiquidGlassRenderer.getInstance().tick();
            }
            if (client.screen instanceof SkijaScreen screen) SkijaRenderer.draw(screen::renderSkija);
            SkijaRenderer.renderSubmitted();
            SkijaRenderer.draw(HudEditOverlay.getInstance()::renderSkija);
        });
    }

    public static void close() {
        LiquidGlassRenderer.getInstance().destroy();
        GlassCaptureRenderer.getInstance().destroy();
        SkijaRenderer.close();
    }
}
