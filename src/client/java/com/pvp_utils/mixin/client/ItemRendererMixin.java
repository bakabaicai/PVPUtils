package com.pvp_utils.mixin.client;

import com.pvp_utils.client.modules.impl.Tool.HeldItemPositionManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
    @Inject(
            method = "renderItem(Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II[ILjava/util/List;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V",
            at = @At("HEAD")
    )
    private static void pvp_utils$beginHeldItemAlpha(ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay, int[] colors, List<BakedQuad> quads, RenderType renderType, ItemStackRenderState.FoilType foilType, CallbackInfo ci) {
        HeldItemPositionManager.beginItemRender(colors);
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II[ILjava/util/List;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V",
            at = @At("RETURN")
    )
    private static void pvp_utils$endHeldItemAlpha(ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay, int[] colors, List<BakedQuad> quads, RenderType renderType, ItemStackRenderState.FoilType foilType, CallbackInfo ci) {
        HeldItemPositionManager.endItemRender();
    }

    @ModifyArg(
            method = "renderQuadList",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFII)V"),
            index = 5
    )
    private static float pvp_utils$applyHeldItemQuadAlpha(float alpha) {
        return HeldItemPositionManager.applyQuadAlpha(alpha);
    }
}
