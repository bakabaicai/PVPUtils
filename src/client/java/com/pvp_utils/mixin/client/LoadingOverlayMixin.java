package com.pvp_utils.mixin.client;

import com.pvp_utils.client.render.MainUI.MainUISharedBackground;
import com.pvp_utils.client.render.chroma.ChromaOverlay;
import com.pvp_utils.client.render.chroma.ChromaGraphics;
import com.pvp_utils.client.startup.StartupOverlayRenderer;
import com.pvp_utils.client.startup.StartupProgress;
import io.github.humbleui.skija.Canvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingOverlay.class)
public class LoadingOverlayMixin implements ChromaOverlay {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private long fadeOutStart;

    @Unique
    private StartupOverlayRenderer pvp_utils$startupRenderer;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void pvp_utils$initializeStartup(CallbackInfo ci) {
        if (!minecraft.isGameLoadFinished() && StartupProgress.isRunning()) {
            pvp_utils$startupRenderer = new StartupOverlayRenderer();
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$renderStartup(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        StartupOverlayRenderer renderer = pvp_utils$startupRenderer;
        if (renderer == null) return;
        renderer.updateFrame(graphics.guiWidth(), graphics.guiHeight(), System.nanoTime(),
                fadeOutStart >= 0L && minecraft.isGameLoadFinished());
        StartupOverlayRenderer.Frame frame = renderer.frame();
        if (frame.rendersScreenBehind()) {
            if (minecraft.screen != null) {
                minecraft.screen.renderWithTooltipAndSubtitles(graphics, 0, 0, partialTick);
            } else {
                minecraft.gui.renderDeferredSubtitles();
            }
        }
        if (minecraft.getOverlay() != (Object) this) {
            ci.cancel();
            return;
        }
        if (frame.finished()) {
            StartupProgress.complete();
            minecraft.setOverlay(null);
        } else if (ChromaGraphics.hasFailed()) {
            pvp_utils$renderStartupFallback(graphics, frame);
        }
        ci.cancel();
    }

    @Unique
    private void pvp_utils$renderStartupFallback(GuiGraphics graphics, StartupOverlayRenderer.Frame frame) {
        graphics.nextStratum();
        graphics.fill(0, 0, frame.width(), frame.height(), Math.round(frame.backgroundAlpha() * 255f) << 24);
        int y = frame.height() / 2 - minecraft.font.lineHeight / 2;
        if (frame.loadingAlpha() > 0f) {
            String title = StartupOverlayRenderer.TITLE + ".".repeat(frame.dots());
            graphics.drawString(minecraft.font, title, (frame.width() - minecraft.font.width(title)) / 2, y,
                    Math.round(frame.loadingAlpha() * 255f) << 24 | 0xFFFFFF, false);
            String status = frame.stage().label();
            graphics.drawString(minecraft.font, status, (frame.width() - minecraft.font.width(status)) / 2,
                    y + minecraft.font.lineHeight + 9, Math.round(frame.loadingAlpha() * 255f) << 24 | 0x888888, false);
        }
        if (frame.welcomeAlpha() > 0f) {
            graphics.drawString(minecraft.font, StartupOverlayRenderer.WELCOME,
                    (frame.width() - minecraft.font.width(StartupOverlayRenderer.WELCOME)) / 2, y,
                    Math.round(frame.welcomeAlpha() * 255f) << 24 | 0xFFFFFF, false);
        }
    }

    @Override
    public boolean isChromaOverlayActive() {
        return pvp_utils$startupRenderer != null;
    }

    @Override
    public boolean rendersScreenBehind() {
        return pvp_utils$startupRenderer != null && pvp_utils$startupRenderer.frame() != null
                && pvp_utils$startupRenderer.frame().rendersScreenBehind();
    }

    @Override
    public void renderChroma(Canvas canvas) {
        if (pvp_utils$startupRenderer != null) pvp_utils$startupRenderer.renderChroma(canvas);
    }

    @Override
    public void close() {
        if (pvp_utils$startupRenderer != null) pvp_utils$startupRenderer.close();
        pvp_utils$startupRenderer = null;
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V",
                    ordinal = 0
            )
    )
    private void pvp_utils$renderMainUiGlslBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (MainUISharedBackground.shouldReplaceLoadingOverlay()) {
            MainUISharedBackground.render(guiGraphics, mouseX, mouseY);
        }
    }
}
