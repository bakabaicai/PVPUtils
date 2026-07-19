package com.pvp_utils.mixin.client;

import com.pvp_utils.client.modules.impl.Tool.HeldItemPositionManager;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(targets = "net.minecraft.client.renderer.feature.ItemFeatureRenderer")
public class ItemFeatureRendererMixin {
    @ModifyArgs(
            method = "render(Lnet/minecraft/client/renderer/SubmitNodeCollection;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/OutlineBufferSource;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderItem(Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II[ILjava/util/List;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V")
    )
    private void pvp_utils$applyHeldItemSubmitTransparency(Args args) {
        int[] colors = args.get(5);
        RenderType renderType = args.get(7);
        args.set(7, HeldItemPositionManager.applySubmittedItemRenderType(renderType, colors));
    }
}
