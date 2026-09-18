package com.pvp_utils.client.modules.impl.Tool.FoodInfo;

import net.minecraft.util.Mth;

public final class FoodInfoColor {
    private FoodInfoColor() {
    }

    public static int argb(float r, float g, float b, float a) {
        return (Mth.floor(a * 255.0f) << 24)
                | (Mth.floor(r * 255.0f) << 16)
                | (Mth.floor(g * 255.0f) << 8)
                | Mth.floor(b * 255.0f);
    }
}
