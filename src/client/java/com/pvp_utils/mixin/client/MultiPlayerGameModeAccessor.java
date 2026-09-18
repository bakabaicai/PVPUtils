package com.pvp_utils.mixin.client;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {
    @Accessor("destroyProgress")
    float pvputils$getDestroyProgress();

    @Accessor("destroyDelay")
    int pvputils$getDestroyDelay();

    @Accessor("isDestroying")
    boolean pvputils$isDestroying();

    @Accessor("destroyBlockPos")
    BlockPos pvputils$getDestroyBlockPos();
}
