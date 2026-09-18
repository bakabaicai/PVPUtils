package com.pvp_utils.mixin.client;

import com.pvp_utils.client.modules.impl.Render.ArmorTransparency.ArmorTransparencyManager;
import com.pvp_utils.client.modules.impl.Render.ArmorTransparency.ArmorTransparencyRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class ArmorTransparencyLivingEntityRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void pvp_utils$markArmorTransparencyCombat(LivingEntity entity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (state instanceof ArmorTransparencyRenderState access) {
            access.pvp_utils$setArmorTransparencyInCombat(entity instanceof Player player && ArmorTransparencyManager.isInCombat(player));
        }
    }
}
