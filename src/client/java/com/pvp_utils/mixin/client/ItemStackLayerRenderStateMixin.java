package com.pvp_utils.mixin.client;

import com.pvp_utils.client.modules.impl.Tool.HeldItemPositionManager;
import com.pvp_utils.Config;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(targets = "net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState")
public class ItemStackLayerRenderStateMixin {
    @Shadow(aliases = "field_55345") @Final ItemStackRenderState itemStackRenderState;

    @ModifyArg(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;III[ILjava/util/List;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V"),
            index = 6
    )
    private List<BakedQuad> pvp_utils$legacy17FlatDropQuads(List<BakedQuad> quads) {
        if (Config.item2DRender && Config.legacy17Animations
                && ((ItemStackRenderStateAccessor) this.itemStackRenderState).pvp_utils$getDisplayContext()
                == net.minecraft.world.item.ItemDisplayContext.GROUND) {
            return quads.stream().filter(quad -> quad.direction() == Direction.SOUTH).collect(Collectors.toList());
        }
        return quads;
    }

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
