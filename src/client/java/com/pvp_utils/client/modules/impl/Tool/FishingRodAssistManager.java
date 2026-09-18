package com.pvp_utils.client.modules.impl.Tool;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public final class FishingRodAssistManager {
    private static int lastSelectedSlot = -1;
    private static boolean lastHoldingRod;
    private static int pendingSlot = -1;
    private static int ticksRemaining;

    private FishingRodAssistManager() {}

    public static void tick(Minecraft client) {
        if (!Config.fishingRodAssist || client.player == null || client.gameMode == null || client.screen != null) {
            reset(client);
            return;
        }

        LocalPlayer player = client.player;
        int selectedSlot = player.getInventory().getSelectedSlot();
        boolean holdingRod = player.getMainHandItem().is(Items.FISHING_ROD);

        if (holdingRod && (!lastHoldingRod || selectedSlot != lastSelectedSlot)) {
            pendingSlot = selectedSlot;
            ticksRemaining = Math.max(0, Config.fishingRodAssistUseDelay);
        }

        if (pendingSlot >= 0) {
            if (!holdingRod || selectedSlot != pendingSlot) {
                pendingSlot = -1;
                ticksRemaining = 0;
            } else if (ticksRemaining > 0) {
                ticksRemaining--;
            } else {
                client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
                player.swing(InteractionHand.MAIN_HAND);
                pendingSlot = -1;
            }
        }

        lastHoldingRod = holdingRod;
        lastSelectedSlot = selectedSlot;
    }

    private static void reset(Minecraft client) {
        if (client != null && client.player != null) {
            lastHoldingRod = client.player.getMainHandItem().is(Items.FISHING_ROD);
            lastSelectedSlot = client.player.getInventory().getSelectedSlot();
        } else {
            lastHoldingRod = false;
            lastSelectedSlot = -1;
        }
        pendingSlot = -1;
        ticksRemaining = 0;
    }
}
