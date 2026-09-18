package com.pvp_utils.mixin.client;

import com.pvp_utils.client.skin.SkinManager;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemModelResolver.class)
public class ItemModelResolverMixin {
    @ModifyVariable(method = "appendItemLayers", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private ItemStack pvp_utils$applySkin(ItemStack stack) {
        return SkinManager.replaceItemModel(stack);
    }
}
