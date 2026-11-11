package com.pvp_utils.mixin.client;

import com.pvp_utils.client.combat.KillConfirmManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public final class KillConfirmWorldMixin {
    @Inject(method = "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;Z)V", at = @At("HEAD"))
    private void pvp_utils$clearKillWindowWorld(ClientLevel level, boolean stopSound, CallbackInfo ci) {
        KillConfirmManager.INSTANCE.clear();
    }
}
