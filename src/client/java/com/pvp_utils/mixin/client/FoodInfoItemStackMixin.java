package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Tool.FoodInfo.FoodInfoHelper;
import com.pvp_utils.client.modules.impl.Tool.FoodInfo.FoodInfoTooltipComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ItemStack.class)
public class FoodInfoItemStackMixin {
    @Inject(method = "getTooltipImage", at = @At("RETURN"), cancellable = true)
    private void pvp_utils$foodInfoTooltipImage(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        if (!Config.foodInfo || cir.getReturnValue().isPresent() || Minecraft.getInstance().player == null) {
            return;
        }
        ItemStack stack = (ItemStack) (Object) this;
        if (!FoodInfoHelper.isFood(stack)) {
            return;
        }
        FoodInfoTooltipComponent component = new FoodInfoTooltipComponent(stack, Minecraft.getInstance().player);
        if (component.shouldRender()) {
            cir.setReturnValue(Optional.of(component));
        }
    }
}
