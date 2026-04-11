package com.pvp_utils.mixin.client;

import com.pvp_utils.TitleDetector;
import com.pvp_utils.client.gui.NotificationOverlay;
import com.pvp_utils.client.gui.HitMarkerRenderer;
import com.pvp_utils.client.gui.TargetHudRenderer;
import com.pvp_utils.client.gui.FallDamagePredictor;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.network.chat.Component;
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
        NotificationOverlay.getInstance().render(guiGraphics);
        HitMarkerRenderer.getInstance().render(guiGraphics);
        TargetHudRenderer.getInstance().render(guiGraphics);
        FallDamagePredictor.getInstance().render(guiGraphics);
    }
}