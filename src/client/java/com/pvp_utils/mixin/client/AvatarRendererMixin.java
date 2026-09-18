package com.pvp_utils.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pvp_utils.Config;
import com.pvp_utils.client.skin.SkinManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
    private static ItemModelResolver pvp_utils$skinResolver;

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void pvp_utils$applySkinCosmetic(Avatar entity, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || entity.getId() != minecraft.player.getId()) return;
        ItemStack cosmetic = SkinManager.cosmeticStack();
        if (!cosmetic.isEmpty()) {
            if (pvp_utils$skinResolver == null) pvp_utils$skinResolver = new ItemModelResolver(minecraft.getModelManager());
            pvp_utils$skinResolver.updateForLiving(state.headItem, cosmetic, ItemDisplayContext.HEAD, entity);
        }
        if (Config.swordBlock && minecraft.options.keyUse.isDown() && entity.getMainHandItem().is(ItemTags.SWORDS)) {
            if (entity.getMainArm() == HumanoidArm.RIGHT) {
                state.rightArmPose = HumanoidModel.ArmPose.BLOCK;
            } else {
                state.leftArmPose = HumanoidModel.ArmPose.BLOCK;
            }
        }
    }
}
