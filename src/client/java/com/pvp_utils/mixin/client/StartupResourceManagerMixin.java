package com.pvp_utils.mixin.client;

import com.pvp_utils.client.startup.StartupProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.util.Unit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ReloadableResourceManager.class)
public class StartupResourceManagerMixin {
    @Inject(method = "createReload", at = @At("HEAD"))
    private void pvp_utils$loadAssets(Executor preparationExecutor, Executor applicationExecutor,
                                    CompletableFuture<Unit> initialTask, List<PackResources> packs,
                                    CallbackInfoReturnable<ReloadInstance> cir) {
        if (applicationExecutor instanceof Minecraft client && !client.isGameLoadFinished() && StartupProgress.isRunning()) {
            StartupProgress.beginResourceReload();
        }
    }
}
