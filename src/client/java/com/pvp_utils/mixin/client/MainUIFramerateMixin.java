package com.pvp_utils.mixin.client;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import com.pvp_utils.Config;
import com.pvp_utils.client.render.MainUI.PVPUtilsMainUI;
import com.pvp_utils.client.render.MainUI.PVPUtilsMultiplayerScreen;
import com.pvp_utils.client.render.MainUI.PVPUtilsSingleplayerScreen;
import com.pvp_utils.client.render.MainUI.PVPUtilsViaFabricPlusScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FramerateLimitTracker.class)
public abstract class MainUIFramerateMixin {
    @Shadow
    public abstract FramerateLimitTracker.FramerateThrottleReason getThrottleReason();

    @Inject(method = "getFramerateLimit", at = @At("RETURN"), cancellable = true)
    private void pvp_utils$mainMenuFrameLimit(CallbackInfoReturnable<Integer> cir) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null || getThrottleReason() != FramerateLimitTracker.FramerateThrottleReason.OUT_OF_LEVEL_MENU) return;
        if (!(client.screen instanceof PVPUtilsMainUI)
                && !(client.screen instanceof PVPUtilsSingleplayerScreen)
                && !(client.screen instanceof PVPUtilsMultiplayerScreen)
                && !(client.screen instanceof PVPUtilsViaFabricPlusScreen)) return;
        int limit = client.options.framerateLimit().get();
        if (Config.mainUIFrameLimitEnabled) limit = Math.min(limit, Math.clamp(Config.mainUIFrameLimit, 30, 240));
        cir.setReturnValue(limit);
    }
}
