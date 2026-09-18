package com.pvp_utils.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pvp_utils.Config;
import com.pvp_utils.client.util.ItemPhysicsRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {
    @Shadow private ItemModelResolver itemModelResolver;
    @Unique private ItemEntityRenderState pvp_utils$currentItemState;

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V", at = @At("TAIL"))
    private void pvp_utils$extractItemPhysics(ItemEntity itemEntity, ItemEntityRenderState state, float tickProgress, CallbackInfo ci) {
        boolean blockItem = itemEntity.getItem().getItem() instanceof BlockItem;
        if (Config.item2DRender && !blockItem) {
            this.itemModelResolver.updateForNonLiving(state.item, itemEntity.getItem(), ItemDisplayContext.GUI, itemEntity);
        }
        Vec3 delta = itemEntity.getDeltaMovement();
        boolean moving = delta.horizontalDistanceSqr() > 2.5E-3 || Math.abs(delta.y) > 0.04;
        ((ItemPhysicsRenderState) state).pvp_utils$setItemPhysics(itemEntity.onGround(), moving, blockItem, itemEntity.getUUID().hashCode());
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("HEAD"))
    private void pvp_utils$beginItemPhysics(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        pvp_utils$currentItemState = state;
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("RETURN"))
    private void pvp_utils$endItemPhysics(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        pvp_utils$currentItemState = null;
    }

    @Redirect(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;getSpin(FF)F")
    )
    private float pvp_utils$replaceItemSpin(float ageInTicks, float bobOffset) {
        if (Config.item2DRender && pvp_utils$currentItemState != null && !((ItemPhysicsRenderState) pvp_utils$currentItemState).pvp_utils$itemPhysicsBlockItem()) {
            return 0.0f;
        }
        if (!Config.itemPhysics || pvp_utils$currentItemState == null) {
            return ItemEntity.getSpin(ageInTicks, bobOffset);
        }

        ItemPhysicsRenderState state = (ItemPhysicsRenderState) pvp_utils$currentItemState;
        if (state.pvp_utils$itemPhysicsOnGround()) {
            return stableAngleRad(state.pvp_utils$itemPhysicsSeed(), 0);
        }
        return ageInTicks * 0.32f * Config.itemPhysicsRotationSpeed + stableAngleRad(state.pvp_utils$itemPhysicsSeed(), 1);
    }

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemEntityRenderer;submitMultipleFromCount(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/ItemClusterRenderState;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/phys/AABB;)V")
    )
    private void pvp_utils$applyItemPhysicsPose(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (Config.item2DRender && !((ItemPhysicsRenderState) state).pvp_utils$itemPhysicsBlockItem()) {
            poseStack.mulPose(cameraRenderState.orientation);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
            poseStack.scale(0.55f, 0.55f, 0.55f);
            return;
        }
        if (!Config.itemPhysics) {
            return;
        }

        ItemPhysicsRenderState physics = (ItemPhysicsRenderState) state;
        if (physics.pvp_utils$itemPhysicsOnGround()) {
            float bob = (float) Math.sin(state.ageInTicks / 10.0f + state.bobOffset) * 0.1f + 0.1f;
            AABB box = state.item.getModelBoundingBox();
            float groundOffset = box.getZsize() > 0.0625 ? 0.0125f : -0.1200f;
            poseStack.translate(0.0f, -bob + groundOffset, 0.0f);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(stableAngleDeg(physics.pvp_utils$itemPhysicsSeed(), 2) * 0.08f - 14.4f));
        } else {
            float age = state.ageInTicks;
            float speed = Config.itemPhysicsRotationSpeed;
            poseStack.mulPose(Axis.XP.rotationDegrees(age * 9.5f * speed + stableAngleDeg(physics.pvp_utils$itemPhysicsSeed(), 3)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(age * 6.0f * speed + stableAngleDeg(physics.pvp_utils$itemPhysicsSeed(), 4)));
        }
    }

    @Unique
    private float stableAngleRad(int seed, int salt) {
        return (float) Math.toRadians(stableAngleDeg(seed, salt));
    }

    @Unique
    private float stableAngleDeg(int seed, int salt) {
        int mixed = seed ^ (salt * 0x9E3779B9);
        mixed ^= mixed >>> 16;
        mixed *= 0x7FEB352D;
        mixed ^= mixed >>> 15;
        return (mixed & 0xFFFF) / 65535.0f * 360.0f;
    }
}
