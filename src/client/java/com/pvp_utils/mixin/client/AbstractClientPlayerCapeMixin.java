package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Render.CustomCapeManager;
import com.pvp_utils.client.modules.impl.Render.OfflineSkinManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerCapeMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void pvp_utils$customCape(CallbackInfoReturnable<PlayerSkin> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        PlayerSkin skin = cir.getReturnValue();
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || (Object) this != client.player) return;
        ClientAsset.Texture offline = Config.offlineSkin ? OfflineSkinManager.texture() : null;
        if (offline != null) {
            cir.setReturnValue(PlayerSkin.insecure(offline, offline, skin.cape(), skin.model()));
            return;
        }
        if (!Config.customCape) return;
        ClientAsset.Texture cape = CustomCapeManager.texture();
        if (cape == null) return;
        cir.setReturnValue(PlayerSkin.insecure(skin.body(), cape, cape, skin.model()));
    }
}
