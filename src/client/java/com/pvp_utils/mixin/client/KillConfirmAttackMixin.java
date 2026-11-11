package com.pvp_utils.mixin.client;

import com.pvp_utils.client.combat.KillConfirmManager;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public final class KillConfirmAttackMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void pvp_utils$recordKillWindowAttack(Player player, Entity target, CallbackInfo ci) {
        KillConfirmManager.INSTANCE.recordAttack(player, target);
    }
}
