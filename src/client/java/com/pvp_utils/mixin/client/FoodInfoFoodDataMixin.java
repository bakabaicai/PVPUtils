package com.pvp_utils.mixin.client;

import com.pvp_utils.client.modules.impl.Tool.FoodInfo.FoodInfoExhaustionAccess;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FoodData.class)
public class FoodInfoFoodDataMixin implements FoodInfoExhaustionAccess {
    @Shadow
    private float exhaustionLevel;

    @Override
    public float pvp_utils$getExhaustion() {
        return exhaustionLevel;
    }
}
