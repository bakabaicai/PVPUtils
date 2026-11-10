package com.pvp_utils;

import com.pvp_utils.compatibility.CompatibilityGate;
import net.fabricmc.api.ClientModInitializer;

public final class PVPUtilsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (CompatibilityGate.isEnabled()) {
            PVPUtilsClientRuntime.initialize();
        }
    }
}
