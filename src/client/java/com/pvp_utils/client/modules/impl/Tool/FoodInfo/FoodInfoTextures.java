package com.pvp_utils.client.modules.impl.Tool.FoodInfo;

import net.minecraft.resources.Identifier;

public final class FoodInfoTextures {
    public static final Identifier MOD_ICONS = Identifier.fromNamespaceAndPath("pvp_utils", "textures/food_info/icons.png");
    public static final Identifier HUNGER_OUTLINE_SPRITE = Identifier.fromNamespaceAndPath("pvp_utils", "tooltip_hunger_outline");

    public static final Identifier FOOD_EMPTY_HUNGER = Identifier.withDefaultNamespace("hud/food_empty_hunger");
    public static final Identifier FOOD_HALF_HUNGER = Identifier.withDefaultNamespace("hud/food_half_hunger");
    public static final Identifier FOOD_FULL_HUNGER = Identifier.withDefaultNamespace("hud/food_full_hunger");
    public static final Identifier FOOD_EMPTY = Identifier.withDefaultNamespace("hud/food_empty");
    public static final Identifier FOOD_HALF = Identifier.withDefaultNamespace("hud/food_half");
    public static final Identifier FOOD_FULL = Identifier.withDefaultNamespace("hud/food_full");

    public static final Identifier HEART_CONTAINER = Identifier.withDefaultNamespace("hud/heart/container");
    public static final Identifier HEART_HARDCORE_CONTAINER = Identifier.withDefaultNamespace("hud/heart/container_hardcore");
    public static final Identifier HEART_FULL = Identifier.withDefaultNamespace("hud/heart/full");
    public static final Identifier HEART_HARDCORE_FULL = Identifier.withDefaultNamespace("hud/heart/hardcore_full");
    public static final Identifier HEART_HALF = Identifier.withDefaultNamespace("hud/heart/half");
    public static final Identifier HEART_HARDCORE_HALF = Identifier.withDefaultNamespace("hud/heart/hardcore_half");

    private FoodInfoTextures() {
    }

    public enum FoodType {
        EMPTY,
        HALF,
        FULL
    }

    public enum HeartType {
        CONTAINER,
        FULL,
        HALF
    }

    public static Identifier food(boolean rotten, FoodType type) {
        return switch (type) {
            case EMPTY -> rotten ? FOOD_EMPTY_HUNGER : FOOD_EMPTY;
            case HALF -> rotten ? FOOD_HALF_HUNGER : FOOD_HALF;
            case FULL -> rotten ? FOOD_FULL_HUNGER : FOOD_FULL;
        };
    }

    public static Identifier heart(boolean hardcore, HeartType type) {
        return switch (type) {
            case CONTAINER -> hardcore ? HEART_HARDCORE_CONTAINER : HEART_CONTAINER;
            case FULL -> hardcore ? HEART_HARDCORE_FULL : HEART_FULL;
            case HALF -> hardcore ? HEART_HARDCORE_HALF : HEART_HALF;
        };
    }
}
