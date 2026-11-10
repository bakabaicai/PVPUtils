package com.pvp_utils.client.compatibility;

import com.pvp_utils.compatibility.CompatibilityGate;
import net.minecraft.client.Minecraft;

public final class CompatibilityWarningController {
    private CompatibilityWarningController() {
    }

    public static void tick(Minecraft client) {
        if (!client.isRunning() || !client.isGameLoadFinished() || client.getOverlay() != null
                || client.screen instanceof CompatibilityWarningScreen || !CompatibilityGate.hasPendingWarning()) {
            return;
        }
        client.setScreen(new CompatibilityWarningScreen(client.screen));
    }
}
