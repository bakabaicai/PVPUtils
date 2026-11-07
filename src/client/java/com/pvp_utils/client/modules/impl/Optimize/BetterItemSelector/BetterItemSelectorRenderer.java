package com.pvp_utils.client.modules.impl.Optimize.BetterItemSelector;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.HudPanel;
import com.pvp_utils.client.render.skia.SkijaRenderer;
import com.pvp_utils.client.render.skia.SkijaUi;
import net.minecraft.client.Minecraft;

public final class BetterItemSelectorRenderer {
    private static final BetterItemSelectorRenderer INSTANCE = new BetterItemSelectorRenderer();

    private BetterItemSelectorRenderer() {
    }

    public static BetterItemSelectorRenderer getInstance() {
        return INSTANCE;
    }

    public void renderBackground(Minecraft client, float x, float y) {
        SkijaRenderer.draw(canvas -> HudPanel.draw(canvas, x, y, 182f, 22f, 7f,
                Config.hotbarAppearance, 1f));
    }

    public void renderSelector(Minecraft client, float x, float y, float slotOffset) {
        SkijaRenderer.draw(canvas -> SkijaUi.outline(canvas, x + slotOffset + 2.5f,
                y + 2.5f, 19f, 19f, 5f, 1f, Config.hotbarAppearance.accent()));
    }
}
