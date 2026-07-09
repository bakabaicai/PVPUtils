package com.pvp_utils.client.modules.impl.Optimize;

import com.mojang.blaze3d.platform.InputConstants;
import com.pvp_utils.Config;
import com.pvp_utils.mixin.client.BetterMouseLogicContainerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class BetterMouseLogicManager {
    private static final Minecraft CLIENT = Minecraft.getInstance();
    private static Screen openScreen;
    private static ContainerHandler handler;
    private static Slot oldSelectedSlot;
    private static double accumulatedScrollDelta;
    private static boolean canDoLmbDrag;
    private static boolean canDoRmbDrag;
    private static boolean rmbTweakLeftOriginalSlot;

    private BetterMouseLogicManager() {
    }

    public static boolean onMouseClicked(AbstractContainerScreen<?> screen, double x, double y, int button) {
        updateScreen(screen);
        if (!enabled()) {
            return false;
        }

        MouseButton mouseButton = MouseButton.from(button);
        if (mouseButton == null) {
            return false;
        }

        oldSelectedSlot = handler.getSlotUnderMouse(x, y);
        ItemStack stackOnMouse = CLIENT.player.containerMenu.getCarried();

        if (mouseButton == MouseButton.LEFT) {
            if (stackOnMouse.isEmpty()) {
                canDoLmbDrag = true;
            }
        } else if (mouseButton == MouseButton.RIGHT) {
            if (!stackOnMouse.isEmpty()) {
                canDoRmbDrag = true;
                rmbTweakLeftOriginalSlot = false;
            }
        }

        return false;
    }

    public static boolean onMouseReleased(AbstractContainerScreen<?> screen, int button) {
        updateScreen(screen);
        if (!enabled()) {
            return false;
        }

        MouseButton mouseButton = MouseButton.from(button);
        if (mouseButton == MouseButton.LEFT) {
            canDoLmbDrag = false;
        } else if (mouseButton == MouseButton.RIGHT) {
            canDoRmbDrag = false;
        }

        return false;
    }

    public static boolean onMouseDragged(AbstractContainerScreen<?> screen, double x, double y, int button) {
        updateScreen(screen);
        if (!enabled()) {
            return false;
        }

        MouseButton mouseButton = MouseButton.from(button);
        if (mouseButton == null) {
            return false;
        }

        Slot selectedSlot = handler.getSlotUnderMouse(x, y);
        if (selectedSlot == oldSelectedSlot) {
            return false;
        }

        ItemStack stackOnMouse = CLIENT.player.containerMenu.getCarried();

        if (canDoRmbDrag && mouseButton == MouseButton.RIGHT && !rmbTweakLeftOriginalSlot) {
            rmbTweakLeftOriginalSlot = true;
            handler.disableRmbDraggingFunctionality();
            rmbTweakMaybeClickSlot(oldSelectedSlot, stackOnMouse);
        }

        oldSelectedSlot = selectedSlot;
        if (selectedSlot == null || handler.isIgnored(selectedSlot)) {
            return false;
        }

        if (mouseButton == MouseButton.LEFT) {
            handleLmbDrag(selectedSlot, stackOnMouse);
        } else if (mouseButton == MouseButton.RIGHT && canDoRmbDrag) {
            rmbTweakMaybeClickSlot(selectedSlot, stackOnMouse);
        }

        return false;
    }

    public static boolean onMouseScrolled(AbstractContainerScreen<?> screen, double x, double y, double scrollDelta) {
        updateScreen(screen);
        if (!enabled()) {
            return false;
        }

        Slot selectedSlot = handler.getSlotUnderMouse(x, y);
        if (selectedSlot == null || handler.isIgnored(selectedSlot)) {
            return false;
        }

        ItemStack selectedSlotStack = selectedSlot.getItem();
        double scaledDelta = scrollDelta;
        if (accumulatedScrollDelta != 0.0 && Math.signum(scaledDelta) != Math.signum(accumulatedScrollDelta)) {
            accumulatedScrollDelta = 0.0;
        }

        accumulatedScrollDelta += scaledDelta;
        int delta = (int) accumulatedScrollDelta;
        accumulatedScrollDelta -= delta;
        if (delta == 0) {
            return true;
        }

        List<Slot> slots = handler.getSlots();
        int numItemsToMove = Math.abs(delta);
        boolean pushItems = delta < 0;

        if (selectedSlotStack.isEmpty()) {
            return true;
        }

        ItemStack stackOnMouse = CLIENT.player.containerMenu.getCarried();

        if (handler.isCraftingOutput(selectedSlot)) {
            handleCraftingOutputScroll(selectedSlot, selectedSlotStack, stackOnMouse, slots, numItemsToMove, pushItems);
            return true;
        }

        if (!stackOnMouse.isEmpty() && areStacksCompatible(selectedSlotStack, stackOnMouse)) {
            return true;
        }

        if (pushItems) {
            handleScrollPush(selectedSlot, selectedSlotStack, stackOnMouse, slots, numItemsToMove);
        } else {
            handleScrollPull(selectedSlot, stackOnMouse, slots, numItemsToMove);
        }
        return true;
    }

    private static boolean enabled() {
        return Config.betterMouseLogic && CLIENT.player != null && handler != null;
    }

    private static void updateScreen(Screen newScreen) {
        if (newScreen == openScreen) {
            return;
        }

        openScreen = newScreen;
        handler = null;
        oldSelectedSlot = null;
        accumulatedScrollDelta = 0.0;
        canDoLmbDrag = false;
        canDoRmbDrag = false;
        rmbTweakLeftOriginalSlot = false;

        if (newScreen instanceof AbstractContainerScreen<?> containerScreen) {
            handler = new ContainerHandler(containerScreen);
        }
    }

    private static void handleLmbDrag(Slot selectedSlot, ItemStack stackOnMouse) {
        if (!canDoLmbDrag) {
            return;
        }

        ItemStack selectedSlotStack = selectedSlot.getItem();
        if (selectedSlotStack.isEmpty()) {
            return;
        }

        boolean shiftIsDown = InputConstants.isKeyDown(CLIENT.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(CLIENT.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);

        if (stackOnMouse.isEmpty()) {
            if (shiftIsDown) {
                handler.clickSlot(selectedSlot, MouseButton.LEFT, true);
            }
            return;
        }

        if (!areStacksCompatible(selectedSlotStack, stackOnMouse)) {
            return;
        }

        if (shiftIsDown) {
            handler.clickSlot(selectedSlot, MouseButton.LEFT, true);
            return;
        }

        if (stackOnMouse.getCount() + selectedSlotStack.getCount() > stackOnMouse.getMaxStackSize()) {
            return;
        }

        handler.clickSlot(selectedSlot, MouseButton.LEFT, false);
        if (!handler.isCraftingOutput(selectedSlot)) {
            handler.clickSlot(selectedSlot, MouseButton.LEFT, false);
        }
    }

    private static void rmbTweakMaybeClickSlot(Slot slot, ItemStack stackOnMouse) {
        if (slot == null || stackOnMouse.isEmpty() || handler.isIgnored(slot) || handler.isCraftingOutput(slot)) {
            return;
        }

        if (!(stackOnMouse.getItem() instanceof BundleItem)) {
            ItemStack selectedSlotStack = slot.getItem();
            if (!areStacksCompatible(selectedSlotStack, stackOnMouse)) {
                return;
            }
            if (selectedSlotStack.getCount() == slot.getMaxStackSize(selectedSlotStack)) {
                return;
            }
        }

        handler.clickSlot(slot, MouseButton.RIGHT, false);
    }

    private static void handleCraftingOutputScroll(Slot selectedSlot, ItemStack selectedSlotStack, ItemStack stackOnMouse, List<Slot> slots, int numItemsToMove, boolean pushItems) {
        if (!areStacksCompatible(selectedSlotStack, stackOnMouse)) {
            return;
        }

        if (stackOnMouse.isEmpty()) {
            if (!pushItems) {
                return;
            }
            while (numItemsToMove-- > 0) {
                List<Slot> targetSlots = findPushSlots(slots, selectedSlot, selectedSlotStack.getCount(), true);
                if (targetSlots == null) {
                    break;
                }

                handler.clickSlot(selectedSlot, MouseButton.LEFT, false);
                for (int i = 0; i < targetSlots.size(); i++) {
                    Slot slot = targetSlots.get(i);
                    if (i == targetSlots.size() - 1) {
                        handler.clickSlot(slot, MouseButton.LEFT, false);
                    } else {
                        int clickTimes = slot.getMaxStackSize(slot.getItem()) - slot.getItem().getCount();
                        while (clickTimes-- > 0) {
                            handler.clickSlot(slot, MouseButton.RIGHT, false);
                        }
                    }
                }
            }
        } else {
            while (numItemsToMove-- > 0) {
                handler.clickSlot(selectedSlot, MouseButton.LEFT, false);
            }
        }
    }

    private static void handleScrollPush(Slot selectedSlot, ItemStack selectedSlotStack, ItemStack stackOnMouse, List<Slot> slots, int numItemsToMove) {
        if (!stackOnMouse.isEmpty() && !selectedSlot.mayPlace(stackOnMouse)) {
            return;
        }

        numItemsToMove = Math.min(numItemsToMove, selectedSlotStack.getCount());
        List<Slot> targetSlots = findPushSlots(slots, selectedSlot, numItemsToMove, false);
        if (targetSlots == null || targetSlots.isEmpty()) {
            return;
        }

        boolean hadItemOnMouse = !stackOnMouse.isEmpty();
        MouseButton pickUpButton = MouseButton.RIGHT;
        if (stackOnMouse.isEmpty() && selectedSlotStack.getCount() <= numItemsToMove) {
            pickUpButton = MouseButton.LEFT;
        }
        handler.clickSlot(selectedSlot, pickUpButton, false);

        ItemStack pickedUpStack = CLIENT.player.containerMenu.getCarried();
        numItemsToMove = Math.min(numItemsToMove, pickedUpStack.getCount());

        for (Slot slot : targetSlots) {
            int clickTimes = slot.getMaxStackSize(pickedUpStack) - slot.getItem().getCount();
            clickTimes = Math.min(clickTimes, numItemsToMove);
            numItemsToMove -= clickTimes;
            while (clickTimes-- > 0) {
                handler.clickSlot(slot, MouseButton.RIGHT, false);
            }
        }

        boolean hasLeftoverItems = !CLIENT.player.containerMenu.getCarried().isEmpty();
        if (hadItemOnMouse || hasLeftoverItems) {
            MouseButton putDownButton = MouseButton.LEFT;
            if (hadItemOnMouse && hasLeftoverItems) {
                putDownButton = MouseButton.RIGHT;
            }
            handler.clickSlot(selectedSlot, putDownButton, false);
        }
    }

    private static void handleScrollPull(Slot selectedSlot, ItemStack stackOnMouse, List<Slot> slots, int numItemsToMove) {
        ItemStack selectedSlotStack = selectedSlot.getItem();
        int maxItemsToMove = selectedSlot.getMaxStackSize(selectedSlotStack) - selectedSlotStack.getCount();
        numItemsToMove = Math.min(numItemsToMove, maxItemsToMove);

        while (numItemsToMove > 0) {
            Slot targetSlot = findPullSlot(slots, selectedSlot);
            if (targetSlot == null) {
                break;
            }

            ItemStack targetSlotStack = targetSlot.getItem();
            int numItemsInTargetSlot = targetSlotStack.getCount();

            if (handler.isCraftingOutput(targetSlot)) {
                if (maxItemsToMove < numItemsInTargetSlot) {
                    break;
                }
                maxItemsToMove -= numItemsInTargetSlot;
                numItemsToMove = Math.min(numItemsToMove - 1, maxItemsToMove);
                if (!stackOnMouse.isEmpty() && !selectedSlot.mayPlace(stackOnMouse)) {
                    break;
                }
                handler.clickSlot(selectedSlot, MouseButton.LEFT, false);
                handler.clickSlot(targetSlot, MouseButton.LEFT, false);
                handler.clickSlot(selectedSlot, MouseButton.LEFT, false);
                continue;
            }

            boolean hadItemOnMouse = !stackOnMouse.isEmpty();
            if (hadItemOnMouse && !targetSlot.mayPlace(stackOnMouse)) {
                break;
            }

            MouseButton pickUpButton = MouseButton.RIGHT;
            if (stackOnMouse.isEmpty() && targetSlotStack.getCount() == 1) {
                pickUpButton = MouseButton.LEFT;
            }
            handler.clickSlot(targetSlot, pickUpButton, false);

            int numPickedUp = CLIENT.player.containerMenu.getCarried().getCount();
            int numItemsToMoveFromTargetSlot = Math.min(numPickedUp, numItemsToMove);
            if (numItemsToMoveFromTargetSlot == numPickedUp) {
                handler.clickSlot(selectedSlot, MouseButton.LEFT, false);
            } else {
                for (int i = 0; i < numItemsToMoveFromTargetSlot; i++) {
                    handler.clickSlot(selectedSlot, MouseButton.RIGHT, false);
                }
            }
            maxItemsToMove -= numItemsToMoveFromTargetSlot;
            numItemsToMove -= numItemsToMoveFromTargetSlot;

            boolean hasLeftoverItems = !CLIENT.player.containerMenu.getCarried().isEmpty();
            if (hadItemOnMouse || hasLeftoverItems) {
                MouseButton putDownButton = MouseButton.LEFT;
                if (hadItemOnMouse && hasLeftoverItems) {
                    putDownButton = MouseButton.RIGHT;
                }
                handler.clickSlot(targetSlot, putDownButton, false);
            }
        }
    }

    private static Slot findPullSlot(List<Slot> slots, Slot selectedSlot) {
        ItemStack selectedSlotStack = selectedSlot.getItem();
        boolean findInPlayerInventory = selectedSlot.container != CLIENT.player.getInventory();

        for (int i = slots.size() - 1; i >= 0; i--) {
            Slot slot = slots.get(i);
            if (handler.isIgnored(slot)) {
                continue;
            }
            boolean slotInPlayerInventory = slot.container == CLIENT.player.getInventory();
            if (findInPlayerInventory != slotInPlayerInventory) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || !areStacksCompatible(selectedSlotStack, stack)) {
                continue;
            }
            return slot;
        }

        return null;
    }

    private static List<Slot> findPushSlots(List<Slot> slots, Slot selectedSlot, int itemCount, boolean mustDistributeAll) {
        ItemStack selectedSlotStack = selectedSlot.getItem();
        boolean findInPlayerInventory = selectedSlot.container != CLIENT.player.getInventory();
        List<Slot> targetSlots = new ArrayList<>();
        List<Slot> goodEmptySlots = new ArrayList<>();

        for (Slot slot : slots) {
            if (itemCount <= 0) {
                break;
            }
            if (handler.isIgnored(slot)) {
                continue;
            }
            boolean slotInPlayerInventory = slot.container == CLIENT.player.getInventory();
            if (findInPlayerInventory != slotInPlayerInventory || handler.isCraftingOutput(slot)) {
                continue;
            }

            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                if (slot.mayPlace(selectedSlotStack)) {
                    goodEmptySlots.add(slot);
                }
            } else if (areStacksCompatible(selectedSlotStack, stack) && stack.getCount() < slot.getMaxStackSize(stack)) {
                targetSlots.add(slot);
                itemCount -= Math.min(itemCount, slot.getMaxStackSize(stack) - stack.getCount());
            }
        }

        for (Slot slot : goodEmptySlots) {
            if (itemCount <= 0) {
                break;
            }
            targetSlots.add(slot);
            itemCount -= Math.min(itemCount, slot.getMaxStackSize(selectedSlotStack));
        }

        if (mustDistributeAll && itemCount > 0) {
            return null;
        }
        return targetSlots;
    }

    private static boolean areStacksCompatible(ItemStack a, ItemStack b) {
        return a.isEmpty() || b.isEmpty() || (ItemStack.isSameItem(a, b) && ItemStack.isSameItemSameComponents(a, b));
    }

    private enum MouseButton {
        LEFT(0),
        RIGHT(1);

        private final int value;

        MouseButton(int value) {
            this.value = value;
        }

        static MouseButton from(int value) {
            if (value == LEFT.value) return LEFT;
            if (value == RIGHT.value) return RIGHT;
            return null;
        }
    }

    private static final class ContainerHandler {
        private final AbstractContainerScreen<?> screen;
        private final BetterMouseLogicContainerAccessor accessor;

        private ContainerHandler(AbstractContainerScreen<?> screen) {
            this.screen = screen;
            this.accessor = (BetterMouseLogicContainerAccessor) screen;
        }

        private List<Slot> getSlots() {
            return screen.getMenu().slots;
        }

        private Slot getSlotUnderMouse(double mouseX, double mouseY) {
            return accessor.pvp_utils$getHoveredSlot(mouseX, mouseY);
        }

        private void disableRmbDraggingFunctionality() {
            accessor.pvp_utils$setSkipNextRelease(true);
            if (accessor.pvp_utils$isQuickCrafting() && accessor.pvp_utils$getQuickCraftingButton() == 1) {
                accessor.pvp_utils$setQuickCrafting(false);
            }
        }

        private void clickSlot(Slot slot, MouseButton mouseButton, boolean shiftPressed) {
            accessor.pvp_utils$slotClicked(slot, slot.index, mouseButton.value, shiftPressed ? ClickType.QUICK_MOVE : ClickType.PICKUP);
        }

        private boolean isCraftingOutput(Slot slot) {
            return slot instanceof ResultSlot || slot instanceof FurnaceResultSlot || slot instanceof MerchantResultSlot;
        }

        private boolean isIgnored(Slot slot) {
            return screen instanceof CreativeModeInventoryScreen && slot.container != CLIENT.player.getInventory();
        }
    }
}
