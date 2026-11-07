package com.pvp_utils.mixin.client;

import com.pvp_utils.client.startup.StartupProgress;
import net.minecraft.client.renderer.texture.SpriteLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpriteLoader.class)
public class StartupAtlasMixin {
    @Inject(method = "stitch", at = @At("HEAD"))
    private void pvp_utils$buildAtlases(CallbackInfoReturnable<SpriteLoader.Preparations> cir) {
        StartupProgress.advance(StartupProgress.Stage.ATLASES);
    }
}
