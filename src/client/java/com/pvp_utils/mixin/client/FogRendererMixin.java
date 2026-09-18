package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Shadow @Final private GpuBuffer emptyBuffer;

    @Inject(method = "getBuffer", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$useEmptyWorldFogBuffer(FogRenderer.FogMode fogMode, CallbackInfoReturnable<GpuBufferSlice> cir) {
        if (Config.hideFog && fogMode == FogRenderer.FogMode.WORLD) {
            cir.setReturnValue(this.emptyBuffer.slice(0L, FogRenderer.FOG_UBO_SIZE));
        }
    }
}
