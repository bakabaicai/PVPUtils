package com.pvp_utils.mixin.client;

import com.pvp_utils.client.combat.KillConfirmManager;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public final class KillConfirmRespawnMixin {
    @Inject(method = "handleRespawn", at = @At("RETURN"))
    private void pvp_utils$clearKillWindowRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        KillConfirmManager.INSTANCE.clear();
    }
}
