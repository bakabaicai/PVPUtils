package com.pvp_utils.client.modules.impl.Render.ArmorTransparency;

import com.pvp_utils.Config;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ArmorTransparencyManager {
    private static final long COMBAT_VISIBLE_MS = 3000L;
    private static final Map<UUID, Long> COMBAT = new ConcurrentHashMap<>();

    private ArmorTransparencyManager() {
    }

    public static void markCombat(Player player) {
        if (player != null) {
            COMBAT.put(player.getUUID(), System.currentTimeMillis());
        }
    }

    public static boolean isInCombat(Player player) {
        if (player == null) {
            return false;
        }
        Long last = COMBAT.get(player.getUUID());
        if (last == null) {
            return false;
        }
        if (System.currentTimeMillis() - last <= COMBAT_VISIBLE_MS) {
            return true;
        }
        COMBAT.remove(player.getUUID());
        return false;
    }

    public static int alphaForSlot(EquipmentSlot slot, boolean inCombat) {
        if (!Config.armorTransparency || (Config.armorTransparencyShowInCombat && inCombat)) {
            return 255;
        }
        return switch (slot) {
            case HEAD -> percentToAlpha(Config.armorTransparencyHead);
            case CHEST -> percentToAlpha(Config.armorTransparencyChest);
            case LEGS -> percentToAlpha(Config.armorTransparencyLegs);
            case FEET -> percentToAlpha(Config.armorTransparencyFeet);
            default -> 255;
        };
    }

    public static int applyAlpha(int color, int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return (color & 0x00FFFFFF) | (clamped << 24);
    }

    private static int percentToAlpha(int transparencyPercent) {
        int percent = Math.max(0, Math.min(100, transparencyPercent));
        return Math.round((100 - percent) / 100.0F * 255.0F);
    }
}
