package com.pvp_utils.mixin.client;

import com.pvp_utils.client.modules.impl.Tool.HeldItemPositionManager;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState")
public class ItemStackLayerRenderStateMixin {
    @ModifyArg(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;III[ILjava/util/List;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V"),
            index = 5
    )
    private int[] pvp_utils$applyHeldItemAlpha(int[] colors) {
        return HeldItemPositionManager.applyHeldItemAlpha(colors);
    }

    @ModifyArg(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;III[ILjava/util/List;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V"),
            index = 7
    )
    private RenderType pvp_utils$applyHeldItemTranslucentRenderType(RenderType renderType) {
        return HeldItemPositionManager.applyHeldItemRenderType(renderType);
    }
}
