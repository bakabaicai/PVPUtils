package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Render.BetterTitleRenderer;
import com.pvp_utils.client.render.chroma.ChromaGraphics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class BetterTitleGuiMixin {
    @Shadow private Component title;
    @Shadow private Component subtitle;
    @Shadow private int titleTime;
    @Shadow private int titleFadeInTime;
    @Shadow private int titleStayTime;
    @Shadow private int titleFadeOutTime;
    @Unique private final BetterTitleRenderer pvp_utils$betterTitle = new BetterTitleRenderer();

    @Inject(method = "renderTitle", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$renderBetterTitle(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        if (!Config.betterTitle || ChromaGraphics.hasFailed()) {
            this.pvp_utils$betterTitle.clear();
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.screen != null && !(client.screen instanceof ChatScreen)) {
            ci.cancel();
            return;
        }
        float partial = delta.getGameTimeDeltaPartialTick(false);
        this.pvp_utils$betterTitle.render(this.title, this.subtitle, this.titleTime - partial,
                this.titleFadeInTime, this.titleStayTime, this.titleFadeOutTime,
                client.gui.getGuiTicks() + partial,
                client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
        ci.cancel();
    }

    @Inject(method = "clearTitles", at = @At("TAIL"))
    private void pvp_utils$clearBetterTitle(CallbackInfo ci) {
        this.pvp_utils$betterTitle.dismiss(pvp_utils$titleClock());
    }

    @Inject(method = "setTitle", at = @At("TAIL"))
    private void pvp_utils$updateBetterTitle(Component text, CallbackInfo ci) {
        if (!Config.betterTitle) return;
        this.pvp_utils$betterTitle.setTitle(this.title, this.subtitle, this.titleFadeInTime,
                this.titleStayTime, this.titleFadeOutTime, pvp_utils$titleClock());
    }

    @Inject(method = "setSubtitle", at = @At("TAIL"))
    private void pvp_utils$updateBetterSubtitle(Component text, CallbackInfo ci) {
        if (!Config.betterTitle) return;
        this.pvp_utils$betterTitle.setSubtitle(text, pvp_utils$titleClock());
    }

    @Inject(method = "onDisconnected", at = @At("TAIL"))
    private void pvp_utils$resetBetterTitle(CallbackInfo ci) {
        this.pvp_utils$betterTitle.clear();
    }

    @Unique
    private static float pvp_utils$titleClock() {
        Minecraft client = Minecraft.getInstance();
        return client.gui.getGuiTicks() + client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }
}
