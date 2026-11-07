package com.pvp_utils.mixin.client;

import com.pvp_utils.client.startup.StartupProgress;
import net.minecraft.client.resources.model.ModelBakery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(ModelBakery.class)
public class StartupModelMixin {
    @Inject(method = "bakeModels", at = @At("HEAD"))
    private void pvp_utils$bakeModels(CallbackInfoReturnable<CompletableFuture<?>> cir) {
        StartupProgress.advance(StartupProgress.Stage.MODELS);
    }
}
