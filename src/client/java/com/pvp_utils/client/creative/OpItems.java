package com.pvp_utils.client.creative;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

public final class OpItems {
    private static final int MAX_SAFE_LEVEL = 32767;

    private OpItems() {
    }

    public static Component title() {
        return Component.literal("OP Items").withStyle(style -> style.withColor(0xFFD700).withBold(true));
    }

    public static ItemStack icon() {
        return new ItemStack(Items.BARRIER);
    }

    public static List<ItemStack> stacks(HolderLookup.Provider registries) {
        HolderGetter<Enchantment> enchantments = registries.lookupOrThrow(Registries.ENCHANTMENT);
        return List.of(
                sword(Items.NETHERITE_SWORD, enchantments),
                sword(Items.DIAMOND_SWORD, enchantments),
                knockbackStick(enchantments),
                mace(enchantments),
                bow(enchantments)
        );
    }

    private static ItemStack sword(Item item, HolderGetter<Enchantment> enchantments) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        stack.enchant(enchantments.getOrThrow(Enchantments.SHARPNESS), MAX_SAFE_LEVEL);
        stack.enchant(enchantments.getOrThrow(Enchantments.UNBREAKING), MAX_SAFE_LEVEL);
        stack.enchant(enchantments.getOrThrow(Enchantments.FIRE_ASPECT), MAX_SAFE_LEVEL);
        stack.enchant(enchantments.getOrThrow(Enchantments.LOOTING), 10);
        return stack;
    }

    private static ItemStack knockbackStick(HolderGetter<Enchantment> enchantments) {
        ItemStack stack = new ItemStack(Items.STICK);
        stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        stack.enchant(enchantments.getOrThrow(Enchantments.KNOCKBACK), MAX_SAFE_LEVEL);
        return stack;
    }

    private static ItemStack mace(HolderGetter<Enchantment> enchantments) {
        ItemStack stack = new ItemStack(Items.MACE);
        stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        stack.enchant(enchantments.getOrThrow(Enchantments.WIND_BURST), 3);
        stack.enchant(enchantments.getOrThrow(Enchantments.DENSITY), MAX_SAFE_LEVEL);
        stack.enchant(enchantments.getOrThrow(Enchantments.BREACH), MAX_SAFE_LEVEL);
        stack.enchant(enchantments.getOrThrow(Enchantments.UNBREAKING), MAX_SAFE_LEVEL);
        return stack;
    }

    private static ItemStack bow(HolderGetter<Enchantment> enchantments) {
        ItemStack stack = new ItemStack(Items.BOW);
        stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        stack.enchant(enchantments.getOrThrow(Enchantments.UNBREAKING), MAX_SAFE_LEVEL);
        stack.enchant(enchantments.getOrThrow(Enchantments.POWER), MAX_SAFE_LEVEL);
        stack.enchant(enchantments.getOrThrow(Enchantments.INFINITY), 1);
        return stack;
    }
}
