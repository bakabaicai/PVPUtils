package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Render.CustomCapeManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeLayer.class)
public abstract class CapeLayerMixin {
    private static boolean logged;

    @Inject(method = "submit", at = @At("HEAD"))
    private void pvp_utils$customCape(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float limbSwing, float limbSwingAmount, CallbackInfo ci) {
        if (!Config.customCape || state == null) return;
        ClientAsset.Texture cape = CustomCapeManager.texture();
        if (cape == null) return;
        PlayerSkin skin = state.skin;
        if (skin == null) return;
        state.skin = PlayerSkin.insecure(skin.body(), cape, cape, skin.model());
        state.showCape = true;
        if (!logged) {
            logged = true;
            System.out.println("[PVPUtils] CustomCape CapeLayer override applied: " + cape.texturePath());
        }
    }
}
