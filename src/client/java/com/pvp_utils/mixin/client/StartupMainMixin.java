package com.pvp_utils.mixin.client;

import com.pvp_utils.client.startup.StartupProgress;
import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public class StartupMainMixin {
    @Inject(method = "main", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/datafix/DataFixers;optimize(Ljava/util/Set;)Ljava/util/concurrent/CompletableFuture;"))
    private static void pvp_utils$prepareDataFixer(CallbackInfo ci) {
        StartupProgress.advance(StartupProgress.Stage.DATA_FIXER);
    }

    @Inject(method = "main", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/Bootstrap;bootStrap()V"))
    private static void pvp_utils$prepareRegistries(CallbackInfo ci) {
        StartupProgress.advance(StartupProgress.Stage.REGISTRIES);
    }
}
