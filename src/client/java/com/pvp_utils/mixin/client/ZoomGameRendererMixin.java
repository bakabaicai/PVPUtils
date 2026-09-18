package com.pvp_utils.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.pvp_utils.client.modules.impl.Tool.Zoom.ZoomManager;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public abstract class ZoomGameRendererMixin {
    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private float pvp_utils$applyZoomFov(float fov, Camera camera, float tickDelta, boolean changingFov) {
        return fov / ZoomManager.getZoomDivisor(tickDelta);
    }
}
