package com.pvp_utils.client.modules.impl.Optimize.BetterItemSelector;

import io.github.humbleui.types.RRect;

import com.pvp_utils.client.render.skia.SkijaUi;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.skia.LiquidGlassRenderer;
import com.pvp_utils.client.render.skia.SkijaRenderer;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import net.minecraft.client.Minecraft;

public final class BetterItemSelectorRenderer {
    private static final BetterItemSelectorRenderer INSTANCE = new BetterItemSelectorRenderer();
    private static final float SLOT_SIZE = 20.0f;
    private static final float BAR_HEIGHT = 22.0f;
    private static final float BAR_RADIUS = 7.0f;

    private static final LiquidGlassRenderer.SlotGrid SLOTS =
    new LiquidGlassRenderer.SlotGrid(9, 2.0f, 2.0f, 20.0f, 18.0f, 4.5f);
    private final Paint backgroundPaint = new Paint().setAntiAlias(true);
    private final Paint slotPaint = new Paint().setAntiAlias(true);
    private final Paint selectorBorderPaint = new Paint().setAntiAlias(true).setMode(PaintMode.STROKE).setStrokeWidth(1.0f);

    private BetterItemSelectorRenderer() {
    }

    public static BetterItemSelectorRenderer getInstance() {
        return INSTANCE;
    }

    public void renderBackground(Minecraft client, float x, float y) {
        boolean glass = Config.betterItemSelectorLiquidGlass
        && LiquidGlassRenderer.getInstance().renderPanel(client, x, y, 182.0f, BAR_HEIGHT, BAR_RADIUS,
        LiquidGlassRenderer.panelTint(), Config.liquidGlassShadow, Config.liquidGlassHighlight, 0f, 1, SLOTS);
        if (!glass) {
            SkijaRenderer.draw(blurCanvas -> {
                SkijaRenderer.drawBlurredBackdrop(blurCanvas, RRect.makeXYWH(x, y, 182.0f, BAR_HEIGHT, BAR_RADIUS), x, y, 182.0f, BAR_HEIGHT, Math.max(0f, Math.min(2f, Config.skiaBlurStrength)) * 10.5f);
                SkijaUi.rounded(blurCanvas, x, y, 182.0f, BAR_HEIGHT, BAR_RADIUS, Config.skiaBlurTintColor());
            });
        }
        SkijaRenderer.draw(canvas -> {
            if (!glass) {
                backgroundPaint.setColor(0x4D000000);
                canvas.drawRRect(RRect.makeXYWH(x, y, 182.0f, BAR_HEIGHT, BAR_RADIUS), backgroundPaint);
            }
            boolean lightTheme = Config.hudTheme == Config.HudTheme.LIGHT;
            slotPaint.setColor(lightTheme ? 0x14253045 : 0x262F3745);
            for (int slot = 0; slot < 9; slot++) {
                canvas.drawRRect(RRect.makeXYWH(x + 2.0f + slot * SLOT_SIZE, y + 2.0f, 18.0f, 18.0f, 4.5f), slotPaint);
            }
        });
    }

    public void renderSelector(Minecraft client, float x, float y, float slotOffset) {
        SkijaRenderer.draw(canvas -> {
            float selectorX = x + slotOffset + 2.0f;
            selectorBorderPaint.setColor(0xFF2F54EB);
            canvas.drawRRect(RRect.makeXYWH(selectorX + 0.5f, y + 2.5f, 19.0f, 19.0f, 5.0f), selectorBorderPaint);
        });
    }
}
