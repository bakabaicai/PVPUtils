package com.pvp_utils.client.modules.impl.Tool.FoodInfo;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.Consumable;

public record ConsumableFood(FoodProperties food, Consumable consumable) {
}
