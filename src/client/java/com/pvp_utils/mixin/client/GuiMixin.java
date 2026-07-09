package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Tool.TitleDetector;
import com.pvp_utils.client.modules.impl.Render.NotificationOverlay;
import com.pvp_utils.client.modules.impl.Combat.HitMarkerRenderer;
import com.pvp_utils.client.modules.impl.Render.KeystrokesRenderer;
import com.pvp_utils.client.modules.impl.Render.ArmorHudRenderer;
import com.pvp_utils.client.modules.impl.Render.PotionStatusRenderer;
import com.pvp_utils.client.modules.impl.Render.TargetHudRenderer;
import com.pvp_utils.client.modules.impl.Render.FallDamagePredictor;
import com.pvp_utils.client.modules.impl.Render.DiggingStatusRenderer;
import com.pvp_utils.client.modules.impl.Render.DamageNumberRenderer;
import com.pvp_utils.client.modules.impl.Render.DynamicIsland.DynamicIslandRenderer;
import com.pvp_utils.client.modules.impl.Render.HudEditOverlay;
import com.pvp_utils.client.modules.impl.Render.ItemUseStatusRenderer;
import com.pvp_utils.client.modules.impl.Tool.BlockCountDisplayRenderer;
import com.pvp_utils.client.modules.impl.Optimize.BetterScoreboard.BetterScoreboardRenderer;
import com.pvp_utils.client.render.skia.SkiaRenderer;
import com.pvp_utils.client.render.skia.SkiaScreen;
import io.github.humbleui.skija.Canvas;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "setTitle", at = @At("HEAD"))
    private void onSetTitle(Component title, CallbackInfo ci) {
        TitleDetector.check(title != null ? title.getString() : null, null);
    }

    @Inject(method = "setSubtitle", at = @At("HEAD"))
    private void onSetSubtitle(Component subtitle, CallbackInfo ci) {
        TitleDetector.check(null, subtitle != null ? subtitle.getString() : null);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        int guiWidth = mc.getWindow().getGuiScaledWidth();
        int guiHeight = mc.getWindow().getGuiScaledHeight();
        Canvas canvas = null;

        boolean skiaScreenOpen = mc.screen instanceof SkiaScreen;
        if (!skiaScreenOpen && NotificationOverlay.getInstance().needsStandaloneCanvas()) {
            int[] bounds = NotificationOverlay.getInstance().getCanvasBounds(guiWidth, guiHeight);
            if (bounds != null) {
                canvas = SkiaRenderer.beginRegion(bounds[0], bounds[1], bounds[2], bounds[3]);
            }
        }

        if (mc.options.hideGui) {
            if (canvas != null) {
                SkiaRenderer.endRegion(guiGraphics);
            }
            return;
        }

        if (!skiaScreenOpen) {
            NotificationOverlay.getInstance().render(guiGraphics, canvas);
        }
        HitMarkerRenderer.getInstance().render(guiGraphics);
        TargetHudRenderer.getInstance().render(guiGraphics);
        ItemUseStatusRenderer.getInstance().render(guiGraphics);
        DynamicIslandRenderer.getInstance().render(guiGraphics);
        DamageNumberRenderer.getInstance().render(guiGraphics);
        FallDamagePredictor.getInstance().render(guiGraphics);
        DiggingStatusRenderer.getInstance().render(guiGraphics);
        KeystrokesRenderer.getInstance().render(guiGraphics);
        ArmorHudRenderer.getInstance().render(guiGraphics);
        PotionStatusRenderer.getInstance().render(guiGraphics);
        BetterScoreboardRenderer.getInstance().render(guiGraphics);
        HudEditOverlay.getInstance().render(guiGraphics, canvas);

        if (canvas != null) {
            SkiaRenderer.endRegion(guiGraphics);
        }

        BlockCountDisplayRenderer.getInstance().render(guiGraphics, null);
        guiGraphics.renderDeferredElements();
    }

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$hideVanillaPotionEffects(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (PotionStatusRenderer.getInstance().shouldHideVanillaEffects()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$hideVignette(GuiGraphics guiGraphics, @Nullable Entity entity, CallbackInfo ci) {
        if (Config.hideVignette) {
            ci.cancel();
        }
    }

    @Inject(method = "renderBossOverlay", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$hideBossBar(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (Config.hideBossBar) {
            ci.cancel();
        }
    }
}
