package com.pvp_utils.client.modules.impl.Optimize.BetterItemSelector;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pvp_utils.Config;
import com.pvp_utils.client.render.skia.LiquidGlassRenderer;
import com.pvp_utils.client.render.skia.SkiaGlBackend;
import com.pvp_utils.client.render.skia.SkiaBlurRenderer;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.impl.Library;
import io.github.humbleui.types.RRect;
import net.minecraft.client.Minecraft;

public final class BetterItemSelectorRenderer {
    private static final BetterItemSelectorRenderer INSTANCE = new BetterItemSelectorRenderer();
    private static final float SLOT_SIZE = 20.0f;
    private static final float BAR_HEIGHT = 22.0f;
    private static final float BAR_RADIUS = 7.0f;

    private static final LiquidGlassRenderer.SlotGrid SLOTS =
            new LiquidGlassRenderer.SlotGrid(9, 2.0f, 2.0f, 20.0f, 18.0f, 4.5f);
    private final SkiaGlBackend glBackend = new SkiaGlBackend();
    private final Paint backgroundPaint = new Paint().setAntiAlias(true);
    private final Paint slotPaint = new Paint().setAntiAlias(true);
    private final Paint selectorBorderPaint = new Paint().setAntiAlias(true).setMode(PaintMode.STROKE).setStrokeWidth(1.0f);
    private boolean nativeLoaded;

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
            SkiaBlurRenderer.getInstance().render(client, x, y, 182.0f, BAR_HEIGHT, BAR_RADIUS,
                    Config.skiaBlurTintColor(), Config.skiaBlurStrength);
        }
        Canvas canvas = begin(client);
        if (canvas == null) return;
        try {
            if (!glass) {
                backgroundPaint.setColor(0x4D000000);
                canvas.drawRRect(RRect.makeXYWH(x, y, 182.0f, BAR_HEIGHT, BAR_RADIUS), backgroundPaint);
            }
            boolean lightTheme = Config.hudTheme == Config.HudTheme.LIGHT;
            slotPaint.setColor(lightTheme ? 0x14253045 : 0x262F3745);
            for (int slot = 0; slot < 9; slot++) {
                canvas.drawRRect(RRect.makeXYWH(x + 2.0f + slot * SLOT_SIZE, y + 2.0f, 18.0f, 18.0f, 4.5f), slotPaint);
            }
        } finally {
            glBackend.end();
        }
    }


    public void renderSelector(Minecraft client, float x, float y, float slotOffset) {
        Canvas canvas = begin(client);
        if (canvas == null) return;
        try {
            float selectorX = x + slotOffset + 2.0f;
            selectorBorderPaint.setColor(0xFF2F54EB);
            canvas.drawRRect(RRect.makeXYWH(selectorX + 0.5f, y + 2.5f, 19.0f, 19.0f, 5.0f), selectorBorderPaint);
        } finally {
            glBackend.end();
        }
    }

    private Canvas begin(Minecraft client) {
        ensureNativeLoaded();
        return glBackend.begin(mainFramebufferId(client));
    }

    private int mainFramebufferId(Minecraft client) {
        if (client.getMainRenderTarget().getColorTexture() instanceof GlTexture texture
                && RenderSystem.getDevice() instanceof GlDevice device) {
            return texture.getFbo(device.directStateAccess(), client.getMainRenderTarget().getDepthTexture());
        }
        return 0;
    }

    private void ensureNativeLoaded() {
        if (nativeLoaded) return;
        Library.load();
        nativeLoaded = true;
    }
}
