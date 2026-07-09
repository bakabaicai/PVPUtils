package com.pvp_utils.client.modules.impl.Combat;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

public final class MainHandAssistManager {
    private static int originalSlot = -1;
    private static int targetSlot = -1;
    private static int pressTicks;
    private static int returnTicks;
    private static boolean switched;
    private static boolean targetUseStarted;
    private static boolean quickUse;
    private static boolean quickUseInvoked;
    private static boolean wasUseDown;

    private MainHandAssistManager() {}

    public static void beforeStartUseItem(Minecraft client) {
        if (!Config.mainHandAssist || !Config.mainHandAssistQuickUse || client.player == null || client.level == null || client.gameMode == null || client.screen != null) {
            return;
        }
        LocalPlayer player = client.player;
        if (hasRightClickUtility(player.getOffhandItem())) {
            return;
        }

        Inventory inventory = player.getInventory();
        int selectedSlot = inventory.getSelectedSlot();
        ItemStack held = inventory.getItem(selectedSlot);
        if (hasRightClickUtility(held)) {
            return;
        }

        int nextTargetSlot = findQuickUseSlot(player);
        if (nextTargetSlot < 0 || nextTargetSlot == selectedSlot) {
            return;
        }

        originalSlot = selectedSlot;
        targetSlot = nextTargetSlot;
        pressTicks = 0;
        returnTicks = 0;
        switched = true;
        targetUseStarted = false;
        quickUse = true;
        quickUseInvoked = true;
        inventory.setSelectedSlot(nextTargetSlot);
    }

    public static void tick(Minecraft client) {
        if (!Config.mainHandAssist || (!Config.mainHandAssistQuickUse && !Config.mainHandAssistMeleeWeapon && !Config.mainHandAssistShield) || client.player == null || client.level == null || client.gameMode == null || client.screen != null) {
            reset();
            return;
        }

        LocalPlayer player = client.player;
        boolean useDown = client.options.keyUse.isDown();
        boolean usePressed = useDown && !wasUseDown;
        wasUseDown = useDown;
        Inventory inventory = player.getInventory();
        int selectedSlot = inventory.getSelectedSlot();

        if (!switched) {
            if (!useDown) {
                reset();
                return;
            }
            if (originalSlot < 0) {
                ItemStack held = inventory.getItem(selectedSlot);
                int nextTargetSlot = -1;

                if (Config.mainHandAssistQuickUse && usePressed && !hasRightClickUtility(player.getOffhandItem()) && !hasRightClickUtility(held)) {
                    nextTargetSlot = findQuickUseSlot(player);
                    quickUse = nextTargetSlot >= 0;
                }

                if (nextTargetSlot < 0 && hasRightClickUtility(player.getOffhandItem())) {
                    reset();
                    return;
                }
                if (nextTargetSlot < 0 && Config.mainHandAssistMeleeWeapon && isMeleeWeapon(held)) {
                    nextTargetSlot = findBestRecoverySlot(player);
                    quickUse = false;
                }
                if (nextTargetSlot < 0 && Config.mainHandAssistShield && !hasRightClickUtility(held)) {
                    nextTargetSlot = findShieldSlot(player);
                    quickUse = false;
                }
                if (nextTargetSlot < 0) {
                    reset();
                    return;
                }
                originalSlot = selectedSlot;
                targetSlot = nextTargetSlot;
                pressTicks = 0;
            }
            if (targetSlot < 0) {
                return;
            }
            if (targetSlot == originalSlot && quickUse) {
                switched = true;
                quickUseInvoked = false;
                returnTicks = 0;
                return;
            }
            if (targetSlot == originalSlot) {
                return;
            }
            if (++pressTicks >= delayTicks()) {
                inventory.setSelectedSlot(targetSlot);
                switched = true;
                targetUseStarted = false;
                quickUseInvoked = false;
                returnTicks = 0;
            }
            return;
        }

        if (selectedSlot != targetSlot) {
            reset();
            return;
        }

        if (quickUse) {
            if (!quickUseInvoked) {
                reset();
                return;
            }
            if (++returnTicks >= delayTicks()) {
                if (originalSlot >= 0 && originalSlot < Inventory.getSelectionSize()) {
                    inventory.setSelectedSlot(originalSlot);
                }
                reset();
            }
            return;
        }

        if (player.isUsingItem()) {
            targetUseStarted = true;
            returnTicks = 0;
            return;
        }

        if (!Config.mainHandAssistSwitchBack) {
            return;
        }

        if (!useDown || targetUseStarted) {
            if (++returnTicks >= delayTicks()) {
                if (originalSlot >= 0 && originalSlot < Inventory.getSelectionSize()) {
                    inventory.setSelectedSlot(originalSlot);
                }
                reset();
            }
        }
    }

    private static int findBestRecoverySlot(LocalPlayer player) {
        Inventory inventory = player.getInventory();
        int bestSlot = -1;
        int bestPriority = Integer.MAX_VALUE;
        boolean canUseEnchantedApple = player.getHealth() < 6.0f;
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            int priority = recoveryPriority(stack, canUseEnchantedApple);
            if (priority >= 0 && priority < bestPriority) {
                bestPriority = priority;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private static int findShieldSlot(LocalPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            if (inventory.getItem(slot).is(Items.SHIELD)) {
                return slot;
            }
        }
        return -1;
    }

    private static int findQuickUseSlot(LocalPlayer player) {
        Inventory inventory = player.getInventory();
        int bestSlot = -1;
        int bestPriority = Integer.MAX_VALUE;
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            int priority = quickUsePriority(player, stack);
            if (priority >= 0 && priority < bestPriority) {
                bestPriority = priority;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private static int quickUsePriority(LocalPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return -1;
        }
        if (isPotion(stack) && hasMissingUsefulPotionEffect(player, stack)) {
            return stack.is(Items.SPLASH_POTION) ? 0 : stack.is(Items.LINGERING_POTION) ? 1 : 2;
        }
        if (isPotion(stack) && hasInstantHealing(stack)) {
            return stack.is(Items.SPLASH_POTION) ? 3 : stack.is(Items.LINGERING_POTION) ? 4 : 5;
        }
        if (stack.is(Items.MUSHROOM_STEW) || stack.is(Items.SUSPICIOUS_STEW)) {
            return 6;
        }
        if (stack.is(Items.SPLASH_POTION)) {
            return 7;
        }
        if (stack.is(Items.LINGERING_POTION)) {
            return 8;
        }
        return -1;
    }

    private static boolean isPotion(ItemStack stack) {
        return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
    }

    private static Iterable<MobEffectInstance> potionEffects(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents == null ? java.util.List.of() : contents.getAllEffects();
    }

    private static boolean hasMissingUsefulPotionEffect(LocalPlayer player, ItemStack stack) {
        for (MobEffectInstance effect : potionEffects(stack)) {
            MobEffect mobEffect = effect.getEffect().value();
            if (!mobEffect.isInstantenous() && mobEffect.isBeneficial() && !player.hasEffect(effect.getEffect())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasInstantHealing(ItemStack stack) {
        for (MobEffectInstance effect : potionEffects(stack)) {
            if (effect.getEffect().value().isInstantenous() && effect.getEffect().value().isBeneficial()) {
                return true;
            }
        }
        return false;
    }

    private static int recoveryPriority(ItemStack stack, boolean canUseEnchantedApple) {
        if (stack.isEmpty()) {
            return -1;
        }
        if (canUseEnchantedApple && stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            return 0;
        }
        if (stack.is(Items.GOLDEN_APPLE)) {
            return 1;
        }
        if (stack.is(Items.MUSHROOM_STEW) || stack.is(Items.SUSPICIOUS_STEW)) {
            return 2;
        }
        if (stack.is(Items.POTION)) {
            return 3;
        }
        if (isSafeFood(stack)) {
            return 4;
        }
        return -1;
    }

    private static boolean isSafeFood(ItemStack stack) {
        if (stack.get(DataComponents.FOOD) == null) {
            return false;
        }
        Item item = stack.getItem();
        return item != Items.ENCHANTED_GOLDEN_APPLE
                && item != Items.PUFFERFISH
                && item != Items.POISONOUS_POTATO
                && item != Items.ROTTEN_FLESH
                && item != Items.SPIDER_EYE
                && item != Items.CHICKEN
                && item != Items.CHORUS_FRUIT
                && item != Items.SUSPICIOUS_STEW;
    }

    private static boolean isMeleeWeapon(ItemStack stack) {
        if (stack.isEmpty() || stack.get(DataComponents.WEAPON) == null || hasRightClickUtility(stack)) {
            return false;
        }
        Item item = stack.getItem();
        return item != Items.BOW && item != Items.CROSSBOW && item != Items.TRIDENT && !stack.is(ItemTags.SPEARS);
    }

    private static boolean hasRightClickUtility(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return item instanceof BlockItem
                || stack.get(DataComponents.FOOD) != null
                || stack.get(DataComponents.CONSUMABLE) != null
                || stack.is(Items.SHIELD)
                || stack.is(Items.BOW)
                || stack.is(Items.CROSSBOW)
                || stack.is(Items.TRIDENT)
                || stack.is(ItemTags.SPEARS)
                || stack.is(Items.POTION)
                || stack.is(Items.SPLASH_POTION)
                || stack.is(Items.LINGERING_POTION)
                || stack.is(Items.ENDER_PEARL)
                || stack.is(Items.FISHING_ROD)
                || stack.is(Items.FIREWORK_ROCKET)
                || stack.is(Items.WIND_CHARGE);
    }

    private static int delayTicks() {
        return Math.max(2, Config.mainHandAssistSwitchDelayTicks);
    }

    private static void reset() {
        originalSlot = -1;
        targetSlot = -1;
        pressTicks = 0;
        returnTicks = 0;
        switched = false;
        targetUseStarted = false;
        quickUse = false;
        quickUseInvoked = false;
    }
}
