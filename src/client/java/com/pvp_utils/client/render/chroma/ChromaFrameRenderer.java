package com.pvp_utils.client.render.chroma;

import com.pvp_utils.client.modules.impl.Combat.combo.ComboEffectsManager;
import com.pvp_utils.client.modules.impl.Render.ArraylistRenderer;
import com.pvp_utils.client.modules.impl.Render.ClockHudRenderer;
import com.pvp_utils.client.modules.impl.Render.DynamicIsland.DynamicIslandRenderer;
import com.pvp_utils.client.modules.impl.Render.HudEditOverlay;
import com.pvp_utils.client.modules.impl.Render.KeystrokesRenderer;
import com.pvp_utils.client.modules.impl.Render.MusicInfoHudRenderer;
import com.pvp_utils.client.modules.impl.Render.KillBanner.KillBannerRenderer;
import com.pvp_utils.client.modules.impl.Render.PingHudRenderer;
import com.pvp_utils.client.modules.impl.Render.PotionStatusRenderer;
import com.pvp_utils.client.modules.impl.Render.SkiaTextHudRenderer;
import com.pvp_utils.client.modules.impl.Render.TpsHudRenderer;
import com.pvp_utils.client.modules.impl.Tool.tactical.TacticalPingRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Overlay;

public final class ChromaFrameRenderer {
    private ChromaFrameRenderer() {
    }

    public static void render() {
        Minecraft client = Minecraft.getInstance();
        if (client.getWindow().isMinimized()) return;
        Overlay overlay = client.getOverlay();
        if (overlay != null) {
            if (!(overlay instanceof ChromaOverlay chromaOverlay) || !chromaOverlay.isChromaOverlayActive()) return;
            ChromaGraphics.renderOverlay(canvas -> {
                if (chromaOverlay.rendersScreenBehind()) {
                    if (client.screen instanceof ChromaScreen screen) ChromaGraphics.draw(screen.getClass().getName(), screen::renderChroma);
                    ChromaGraphics.renderSubmitted();
                }
                ChromaGraphics.draw(chromaOverlay.getClass().getName(), chromaOverlay::renderChroma);
            });
            return;
        }
        ChromaGraphics.renderOverlay(canvas -> {
            if (client.level != null && !client.options.hideGui
                    && (client.screen == null || client.screen instanceof ChatScreen
                    || HudEditOverlay.getInstance().isActive())) {
                ChromaGraphics.draw("TacticalPing", TacticalPingRenderer::render);
                ChromaGraphics.draw("PotionStatus", PotionStatusRenderer.getInstance()::renderChroma);
                ChromaGraphics.draw("Arraylist", ArraylistRenderer.getInstance()::renderChroma);
                ChromaGraphics.draw("Keystrokes", KeystrokesRenderer.getInstance()::renderChroma);
                ChromaGraphics.draw("DynamicIsland", DynamicIslandRenderer.getInstance()::renderChroma);
                ChromaGraphics.draw("TextHUD", hudCanvas -> SkiaTextHudRenderer.renderChroma(hudCanvas,
                PingHudRenderer.getInstance(),
                TpsHudRenderer.getInstance(),
                ClockHudRenderer.getInstance()));
                ChromaGraphics.draw("MusicInfoHUD", MusicInfoHudRenderer.getInstance()::renderChroma);
                ChromaGraphics.draw("ComboEffects", ComboEffectsManager::render);
                ChromaGraphics.draw("KillBanner", KillBannerRenderer.getInstance()::renderChroma);
                ChromaGraphics.draw("Plugins", hudCanvas -> com.pvp_utils.client.plugin.PluginManager.INSTANCE.render(
                        hudCanvas, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight()));
            }
            if (client.screen instanceof ChromaScreen screen) ChromaGraphics.draw(screen.getClass().getName(), screen::renderChroma);
            ChromaGraphics.renderSubmitted();
            ChromaGraphics.draw("HudEditOverlay", HudEditOverlay.getInstance()::renderChroma);
        });
    }

    public static void close() {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.getOverlay() instanceof ChromaOverlay overlay) overlay.close();
        DynamicIslandRenderer.getInstance().close();
        ClockHudRenderer.getInstance().close();
        KillBannerRenderer.getInstance().close();
        ChromaGraphics.close();
    }
}
