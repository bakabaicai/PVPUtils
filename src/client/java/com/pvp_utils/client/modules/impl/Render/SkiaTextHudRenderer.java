package com.pvp_utils.client.modules.impl.Render;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pvp_utils.Config;
import com.pvp_utils.client.NeteaseMusic.NeteaseMusicScreen;
import com.pvp_utils.client.gui.clickgui.theme.ClickGuiThemeColors;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaBlurRenderer;
import com.pvp_utils.client.render.skia.SkiaGlBackend;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.impl.Library;
import io.github.humbleui.types.RRect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public abstract class SkiaTextHudRenderer {
    protected record Line(String text, int color) {}

    private static final int PADDING_X = 8;
    private static final int PADDING_Y = 5;
    private static final float LINE_HEIGHT = 13f;
    private static final float TEXT_SIZE = 10f;
    private static final float PANEL_RADIUS = 6f;
    private static final int LITE_BG_COLOR = 0x66000000;
    private static final int LITE_OUTLINE_COLOR = 0x99FFFFFF;
    private static final int TEXT_COLOR = 0xFFF2F4F8;

    private final SkiaGlBackend glBackend = new SkiaGlBackend();
    private final Paint panelPaint = new Paint().setAntiAlias(true);
    private boolean nativeLoaded = false;

    protected abstract boolean enabled();

    protected abstract boolean backgroundEnabled();

    protected abstract Config.HudStyle style();

    protected abstract float configX();

    protected abstract void setConfigX(float value);

    protected abstract float configY();

    protected abstract void setConfigY(float value);

    protected abstract float configScale();

    protected abstract void setConfigScale(float value);

    protected abstract List<Line> lines();

    protected int extraHeight() {
        return 0;
    }

    protected void drawExtras(Canvas canvas, float width, float top) {
    }

    protected void drawExtrasLite(GuiGraphics graphics, int x, int top, int width) {
    }

    public void render(GuiGraphics graphics) {
        if (!enabled() || style() != Config.HudStyle.LITE) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (!inGame(client)) {
            return;
        }
        List<Line> lines = lines();
        if (lines.isEmpty()) {
            return;
        }
        int width = litePanelWidth(client, lines);
        int height = panelHeight(lines.size()) + extraHeight();
        float scale = scale();
        int x = renderX(client, width);
        int y = renderY(client, height);

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-x, -y);

        if (backgroundEnabled()) {
            graphics.fill(x, y, x + width, y + height, LITE_BG_COLOR);
            graphics.renderOutline(x, y, width, height, LITE_OUTLINE_COLOR);
        }

        int textY = y + PADDING_Y + 1;
        for (Line line : lines) {
            graphics.drawString(client.font, line.text(), x + PADDING_X, textY, line.color(), false);
            textY += Math.round(LINE_HEIGHT);
        }
        drawExtrasLite(graphics, x, textY, width);
        graphics.pose().popMatrix();
    }

    public void renderFrameEnd() {
        if (!enabled() || style() == Config.HudStyle.LITE) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (!inGame(client) || (client.screen != null && !HudEditOverlay.getInstance().isActive())) {
            return;
        }
        List<Line> lines = lines();
        if (lines.isEmpty()) {
            return;
        }

        int width = skiaPanelWidth(lines);
        int height = panelHeight(lines.size()) + extraHeight();
        float scale = scale();
        int x = renderX(client, width);
        int y = renderY(client, height);

        boolean blurred = false;
        if (backgroundEnabled() && style() == Config.HudStyle.BLUR) {
            blurred = SkiaBlurRenderer.getInstance().renderRegions(client,
                    List.of(new SkiaBlurRenderer.Region(x, y, width * scale, height * scale,
                            Math.min(8f, height * 0.4f) * scale)),
                    Config.skiaBlurTintColor(), Config.skiaBlurStrength);
        }

        ensureNativeLoaded();
        int framebufferId = mainFramebufferId(client);
        if (framebufferId == 0) {
            return;
        }
        Canvas canvas = glBackend.begin(framebufferId);
        if (canvas == null) {
            return;
        }
        try {
            canvas.save();
            canvas.translate(x, y);
            canvas.scale(scale, scale);

            if (backgroundEnabled()) {
                ClickGuiThemeColors tc = ClickGuiThemeColors.current();
                panelPaint.setColor(style() == Config.HudStyle.BLUR && blurred
                        ? 0x40101420
                        : tc.dark ? 0xB0101420 : 0xD0101420);
                canvas.drawRRect(RRect.makeXYWH(0.5f, 0.5f, width - 1f, height - 1f, PANEL_RADIUS), panelPaint);
            }

            float textY = PADDING_Y + FontRenderer.getAscent(TEXT_SIZE);
            for (Line line : lines) {
                FontRenderer.drawText(canvas, line.text(), PADDING_X, textY, TEXT_SIZE, line.color());
                textY += LINE_HEIGHT;
            }
            drawExtras(canvas, width, textY);
            canvas.restore();
        } finally {
            glBackend.end();
        }
    }

    public float getEditWidth() {
        List<Line> lines = lines();
        float width = style() == Config.HudStyle.LITE
                ? liteWidth(Minecraft.getInstance(), lines)
                : skiaWidth(lines);
        return width * scale();
    }

    public float getEditHeight() {
        return (panelHeight(Math.max(1, lines().size())) + extraHeight()) * scale();
    }

    public int getRenderX(int screenW) {
        List<Line> lines = lines();
        float width = style() == Config.HudStyle.LITE
                ? liteWidth(Minecraft.getInstance(), lines)
                : skiaWidth(lines);
        return clamp((int) (screenW * 0.5f + configX()), 0, Math.max(0, screenW - Math.round(width * scale())));
    }

    public int getRenderY(int screenH) {
        int height = Math.round((panelHeight(Math.max(1, lines().size())) + extraHeight()) * scale());
        return clamp((int) (screenH * 0.5f + configY()), 0, Math.max(0, screenH - height));
    }

    private int litePanelWidth(Minecraft client, List<Line> lines) {
        return liteWidth(client, lines) + PADDING_X * 2;
    }

    private int liteWidth(Minecraft client, List<Line> lines) {
        int max = 0;
        for (Line line : lines) {
            max = Math.max(max, client.font.width(line.text()));
        }
        return max;
    }

    private int skiaPanelWidth(List<Line> lines) {
        return Math.round(skiaWidth(lines)) + PADDING_X * 2;
    }

    private float skiaWidth(List<Line> lines) {
        float max = 0f;
        for (Line line : lines) {
            max = Math.max(max, FontRenderer.measureTextWidth(line.text(), TEXT_SIZE));
        }
        return max;
    }

    protected int panelHeight(int lineCount) {
        return PADDING_Y * 2 + Math.round(lineCount * LINE_HEIGHT);
    }

    protected float scale() {
        return Math.max(0.5f, configScale());
    }

    private boolean inGame(Minecraft client) {
        if (client.player == null || client.level == null || client.options.hideGui) return false;
        if (HudEditOverlay.getInstance().isActive()) return true;
        return !(client.screen instanceof com.pvp_utils.client.gui.clickgui.NewSettingsScreen)
                && !(client.screen instanceof NeteaseMusicScreen);
    }

    private int renderX(Minecraft client, int width) {
        int screenW = client.getWindow().getGuiScaledWidth();
        return clamp((int) (screenW * 0.5f + configX()), 0, Math.max(0, screenW - width));
    }

    private int renderY(Minecraft client, int height) {
        int screenH = client.getWindow().getGuiScaledHeight();
        return clamp((int) (screenH * 0.5f + configY()), 0, Math.max(0, screenH - height));
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

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
