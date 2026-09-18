package com.pvp_utils.mixin.client;

import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Player.class)
public interface PlayerPoseAccessMixin {
    @Invoker("canPlayerFitWithinBlocksAndEntitiesWhen")
    boolean pvpUtils$canPlayerFitWithinBlocksAndEntitiesWhen(Pose pose);
}
