package com.pvp_utils.mixin.client;

import com.pvp_utils.client.modules.impl.Render.ArmorTransparency.ArmorTransparencyManager;
import com.pvp_utils.client.modules.impl.Render.ArmorTransparency.ArmorTransparencyRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.Equippable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jspecify.annotations.Nullable;

@Mixin(EquipmentLayerRenderer.class)
public class ArmorTransparencyEquipmentLayerRendererMixin {
    private static final ThreadLocal<Integer> pvp_utils$armorAlpha = ThreadLocal.withInitial(() -> 255);

    @Inject(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At("HEAD")
    )
    private <S> void pvp_utils$captureArmorTransparencyAlpha(EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> resourceKey, Model<? super S> model, S renderState, ItemStack itemStack, PoseStack poseStack, SubmitNodeCollector collector, int light, @Nullable Identifier texture, int outlineColor, int order, CallbackInfo ci) {
        pvp_utils$armorAlpha.set(pvp_utils$alpha(renderState, itemStack));
    }

    @Inject(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At("RETURN")
    )
    private <S> void pvp_utils$clearArmorTransparencyAlpha(EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> resourceKey, Model<? super S> model, S renderState, ItemStack itemStack, PoseStack poseStack, SubmitNodeCollector collector, int light, @Nullable Identifier texture, int outlineColor, int order, CallbackInfo ci) {
        pvp_utils$armorAlpha.remove();
    }

    @Redirect(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;")
    )
    private RenderType pvp_utils$useTranslucentArmorRenderType(Identifier texture) {
        int alpha = pvp_utils$armorAlpha.get();
        return alpha < 255 ? RenderTypes.armorTranslucent(texture) : RenderTypes.armorCutoutNoCull(texture);
    }

    @ModifyArg(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"),
            index = 6
    )
    private int pvp_utils$applyArmorTransparency(int color) {
        return ArmorTransparencyManager.applyAlpha(color, pvp_utils$armorAlpha.get());
    }

    private int pvp_utils$alpha(Object renderState, ItemStack itemStack) {
        if (!(renderState instanceof LivingEntityRenderState livingState)) {
            return 255;
        }
        Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
        if (equippable == null) {
            return 255;
        }
        EquipmentSlot slot = equippable.slot();
        if (slot != EquipmentSlot.HEAD && slot != EquipmentSlot.CHEST && slot != EquipmentSlot.LEGS && slot != EquipmentSlot.FEET) {
            return 255;
        }
        boolean inCombat = livingState instanceof ArmorTransparencyRenderState access && access.pvp_utils$isArmorTransparencyInCombat();
        return ArmorTransparencyManager.alphaForSlot(slot, inCombat);
    }
}
