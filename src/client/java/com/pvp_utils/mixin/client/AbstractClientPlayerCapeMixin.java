package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Render.CustomCapeManager;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerCapeMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void pvp_utils$customCape(CallbackInfoReturnable<PlayerSkin> cir) {
        if (!Config.customCape) return;
        Identifier id = CustomCapeManager.texture();
        if (id == null) return;
        PlayerSkin skin = cir.getReturnValue();
        ClientAsset.Texture cape = new ClientAsset.ResourceTexture(id);
        cir.setReturnValue(PlayerSkin.insecure(skin.body(), cape, cape, skin.model()));
    }
}
