package com.pvp_utils.client.modules.impl.Tool.FoodInfo;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import org.jetbrains.annotations.Nullable;

public final class FoodInfoHelper {
    public static final float REGEN_EXHAUSTION_INCREMENT = 6.0f;
    public static final float MAX_EXHAUSTION = 4.0f;
    private static final FoodProperties EMPTY_FOOD = new FoodProperties(0, 0.0f, false);

    private FoodInfoHelper() {
    }

    public static boolean isFood(ItemStack stack) {
        return stack != null && stack.has(DataComponents.FOOD) && stack.has(DataComponents.CONSUMABLE);
    }

    public static ConsumableFood defaultFood(ItemStack stack) {
        return new ConsumableFood(
                stack.getOrDefault(DataComponents.FOOD, EMPTY_FOOD),
                stack.getOrDefault(DataComponents.CONSUMABLE, Consumables.DEFAULT_FOOD)
        );
    }

    @Nullable
    public static QueriedFoodResult query(ItemStack stack, Player player) {
        if (!isFood(stack)) {
            return null;
        }
        ConsumableFood food = defaultFood(stack);
        return new QueriedFoodResult(food.food(), food.food(), food.consumable(), stack);
    }

    @Nullable
    public static QueriedFoodResult queryHeldFood(Player player) {
        QueriedFoodResult main = query(player.getMainHandItem(), player);
        if (main != null && canConsume(player, main.modifiedFood())) {
            return main;
        }
        QueriedFoodResult off = query(player.getOffhandItem(), player);
        if (off != null && canConsume(player, off.modifiedFood())) {
            return off;
        }
        return null;
    }

    public static boolean canConsume(Player player, FoodProperties food) {
        return player.canEat(food.canAlwaysEat());
    }

    public static float exhaustion(Player player) {
        FoodData foodData = player.getFoodData();
        if (foodData instanceof FoodInfoExhaustionAccess access) {
            return access.pvp_utils$getExhaustion();
        }
        return 0.0f;
    }

    public static boolean isRotten(Consumable consumable) {
        for (var effect : consumable.onConsumeEffects()) {
            if (!(effect instanceof ApplyStatusEffectsConsumeEffect statusEffects)) {
                continue;
            }
            for (var instance : statusEffects.effects()) {
                if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean shouldShowEstimatedHealth(Player player) {
        if (player.level().getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        FoodData stats = player.getFoodData();
        if (stats.getFoodLevel() >= 18) {
            return false;
        }
        return !player.hasEffect(MobEffects.POISON)
                && !player.hasEffect(MobEffects.WITHER)
                && !player.hasEffect(MobEffects.REGENERATION);
    }

    public static float estimatedHealthIncrement(Player player, ConsumableFood food) {
        if (player.getHealth() >= player.getMaxHealth()) {
            return 0.0f;
        }

        FoodData stats = player.getFoodData();
        int foodLevel = Math.min(stats.getFoodLevel() + food.food().nutrition(), 20);
        float healthIncrement = 0.0f;

        if (foodLevel >= 18.0f) {
            float saturationLevel = Math.min(stats.getSaturationLevel() + food.food().saturation(), (float) foodLevel);
            healthIncrement = estimatedHealthIncrement(foodLevel, saturationLevel, exhaustion(player));
        }

        for (var effect : food.consumable().onConsumeEffects()) {
            if (!(effect instanceof ApplyStatusEffectsConsumeEffect statusEffects)) {
                continue;
            }
            for (var instance : statusEffects.effects()) {
                if (instance.is(MobEffects.REGENERATION)) {
                    int amplifier = instance.getAmplifier();
                    int duration = instance.getDuration();
                    healthIncrement += (float) Math.floor(duration / Math.max(50 >> amplifier, 1));
                    break;
                }
            }
        }

        return healthIncrement;
    }

    public static float estimatedHealthIncrement(int foodLevel, float saturationLevel, float exhaustionLevel) {
        float health = 0.0f;
        if (!Float.isFinite(exhaustionLevel) || !Float.isFinite(saturationLevel)) {
            return 0.0f;
        }
        while (foodLevel >= 18) {
            while (exhaustionLevel > MAX_EXHAUSTION) {
                exhaustionLevel -= MAX_EXHAUSTION;
                if (saturationLevel > 0.0f) {
                    saturationLevel = Math.max(saturationLevel - 1.0f, 0.0f);
                } else {
                    foodLevel -= 1;
                }
            }
            if (foodLevel >= 20 && Float.compare(saturationLevel, Float.MIN_NORMAL) > 0) {
                float limitedSaturation = Math.min(saturationLevel, REGEN_EXHAUSTION_INCREMENT);
                float exhaustionUntilAboveMax = Math.nextUp(MAX_EXHAUSTION) - exhaustionLevel;
                int iterations = Math.max(1, (int) Math.ceil(exhaustionUntilAboveMax / limitedSaturation));
                health += (limitedSaturation / REGEN_EXHAUSTION_INCREMENT) * iterations;
                exhaustionLevel += limitedSaturation * iterations;
            } else if (foodLevel >= 18) {
                health += 1.0f;
                exhaustionLevel += REGEN_EXHAUSTION_INCREMENT;
            }
        }
        return health;
    }

    public record QueriedFoodResult(FoodProperties defaultFood, FoodProperties modifiedFood, Consumable consumable, ItemStack itemStack) {
    }
}
