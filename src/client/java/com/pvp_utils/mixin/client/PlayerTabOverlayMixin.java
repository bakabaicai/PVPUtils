package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.pvp_utils.client.irc.IrcBridge;
import com.pvp_utils.client.modules.impl.Render.BetterPingDisplayRenderer;
import com.pvp_utils.client.modules.impl.Render.DynamicIsland.DynamicIslandRenderer;
import com.pvp_utils.client.modules.impl.Tool.NickHiderManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {
    @ModifyConstant(method = "render", constant = @Constant(intValue = 13))
    private int pvp_utils$betterPingDisplaySlotWidth(int original) {
        return Config.betterPingDisplay ? original + BetterPingDisplayRenderer.EXTRA_SLOT_WIDTH : original;
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$dynamicIslandTabList(GuiGraphics guiGraphics, int width, Scoreboard scoreboard, Objective objective, CallbackInfo ci) {
        if (!Config.dynamicIsland) {
            return;
        }

        DynamicIslandRenderer.getInstance().requestTabListFrame();
        ci.cancel();
    }

    @Inject(method = "renderPingIcon", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$betterPingDisplay(GuiGraphics guiGraphics, int slotWidth, int x, int y, PlayerInfo playerInfo, CallbackInfo ci) {
        if (!Config.betterPingDisplay || Config.dynamicIsland) {
            return;
        }
        BetterPingDisplayRenderer.render(guiGraphics, slotWidth, x, y, playerInfo);
        ci.cancel();
    }

    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void pvp_utils$decorateIrcTabName(PlayerInfo playerInfo, CallbackInfoReturnable<Component> cir) {
        if (playerInfo == null || playerInfo.getProfile() == null) {
            return;
        }
        Component name = IrcBridge.decorateName(cir.getReturnValue(), playerInfo.getProfile().id());
        cir.setReturnValue(NickHiderManager.replaceTabName(name, playerInfo));
    }
}
