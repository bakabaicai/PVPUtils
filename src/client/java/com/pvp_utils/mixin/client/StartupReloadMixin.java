package com.pvp_utils.mixin.client;

import com.pvp_utils.client.startup.StartupProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
import net.minecraft.util.Unit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(SimpleReloadInstance.class)
public class StartupReloadMixin {
    @Shadow
    @Final
    private CompletableFuture<Unit> allPreparations;

    @Inject(method = "prepareTasks", at = @At("HEAD"))
    private void pvp_utils$trackResourceApplication(Executor preparationExecutor, Executor applicationExecutor,
                                                  ResourceManager manager, List<PreparableReloadListener> listeners,
                                                  @Coerce Object stateFactory, CompletableFuture<?> initialTask,
                                                  CallbackInfoReturnable<CompletableFuture<?>> cir) {
        if (applicationExecutor instanceof Minecraft client && !client.isGameLoadFinished() && StartupProgress.isRunning()) {
            int generation = StartupProgress.reloadGeneration();
            allPreparations.thenRun(() -> StartupProgress.beginApplying(generation));
        }
    }
}
