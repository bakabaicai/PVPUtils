package com.pvp_utils.client.modules.impl.Render;

import io.github.humbleui.types.RRect;

import com.pvp_utils.client.render.skia.SkijaUi;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.HudAppearance;
import com.pvp_utils.client.render.HudPanel;
import com.pvp_utils.client.render.skia.SoftShadow;
import com.pvp_utils.client.NeteaseMusic.NeteaseMusicScreen;
import com.pvp_utils.client.gui.clickgui.theme.ClickGuiThemeColors;
import com.pvp_utils.client.render.skia.SkijaRenderer;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
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


    private record Prepared(SkiaTextHudRenderer renderer, List<Line> lines, int width, int height,
    float scale, int x, int y) {}

    protected abstract HudAppearance appearance();

    protected int minimumWidth() { return 0; }

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

        if (this instanceof ClockHudRenderer && backgroundEnabled() && appearance().shadow) {
            SkijaRenderer.draw(canvas -> SoftShadow.drawRounded(canvas, x, y, width * scale,
                    height * scale, 0f, appearance().shadowStrength, 0xB0000000));
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-x, -y);

        if (backgroundEnabled()) {
            graphics.fill(x, y, x + width, y + height, LITE_BG_COLOR);
            graphics.renderOutline(x, y, width, height, LITE_OUTLINE_COLOR);
        }

        int textY = y + PADDING_Y + Math.round((LINE_HEIGHT - client.font.lineHeight) * 0.5f);
        for (Line line : lines) {
            graphics.drawString(client.font, line.text(), x + PADDING_X, textY,
                    appearance().customText ? appearance().text() : line.color(), false);
            textY += Math.round(LINE_HEIGHT);
        }
        drawExtrasLite(graphics, x, textY, width);
        graphics.pose().popMatrix();
    }

    public void renderSkija(Canvas canvas) {
        renderSkija(canvas, this);
    }

    public static void renderSkija(Canvas canvas, SkiaTextHudRenderer... renderers) {
        Minecraft client = Minecraft.getInstance();
        if (renderers.length == 0 || !inGame(client)
        || (client.screen != null && !HudEditOverlay.getInstance().isActive())) {
            return;
        }

        java.util.ArrayList<Prepared> prepared = new java.util.ArrayList<>(renderers.length);
        for (SkiaTextHudRenderer renderer : renderers) {
            Prepared item = renderer.prepare(client);
            if (item != null) prepared.add(item);
        }
        if (prepared.isEmpty()) return;

        for (Prepared item : prepared) {
            item.renderer().drawBackground(canvas, item.x(), item.y(),
                    item.width() * item.scale(), item.height() * item.scale(), item.scale());
        }

        for (Prepared item : prepared) item.renderer().draw(canvas, item);
    }

    private Prepared prepare(Minecraft client) {
        if (!enabled() || style() == Config.HudStyle.LITE) return null;
        List<Line> lines = lines();
        if (lines.isEmpty()) return null;

        int width = skiaPanelWidth(lines);
        int height = panelHeight(lines.size()) + extraHeight();
        float scale = scale();
        int x = renderX(client, width);
        int y = renderY(client, height);
        return new Prepared(this, lines, width, height, scale, x, y);
    }

    protected void drawBackground(Canvas canvas, float x, float y, float width, float height, float scale) {
        if (!backgroundEnabled()) return;
        HudPanel.draw(canvas, x, y, width, height, PANEL_RADIUS * scale, appearance(), 1f);
    }

    private void draw(Canvas canvas, Prepared item) {
        int save = canvas.save();
        try {
            canvas.translate(item.x, item.y);
            canvas.scale(item.scale, item.scale);
            drawContent(canvas, item.lines, item.width, item.height);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    protected void drawContent(Canvas canvas, List<Line> lines, float width, float height) {
        float top = PADDING_Y;
        for (Line line : lines) {
            int color = appearance().customText ? appearance().text() : line.color();
            SkijaUi.text(canvas, line.text(), PADDING_X, top, LINE_HEIGHT, color, TEXT_SIZE);
            top += LINE_HEIGHT;
        }
        drawExtras(canvas, width, top);
    }

    public float getEditWidth() {
        List<Line> lines = lines();
        float width = style() == Config.HudStyle.LITE
        ? litePanelWidth(Minecraft.getInstance(), lines)
        : skiaPanelWidth(lines);
        return width * scale();
    }

    public float getEditHeight() {
        return (panelHeight(Math.max(1, lines().size())) + extraHeight()) * scale();
    }

    public int getRenderX(int screenW) {
        List<Line> lines = lines();
        float width = style() == Config.HudStyle.LITE
        ? litePanelWidth(Minecraft.getInstance(), lines)
        : skiaPanelWidth(lines);
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

    protected int skiaPanelWidth(List<Line> lines) {
        return Math.max(minimumWidth(), (int) Math.ceil(skiaWidth(lines)) + PADDING_X * 2);
    }

    private float skiaWidth(List<Line> lines) {
        float max = 0f;
        for (Line line : lines) {
            max = Math.max(max, SkijaUi.textWidth(line.text(), TEXT_SIZE));
        }
        return max;
    }

    protected int panelHeight(int lineCount) {
        return PADDING_Y * 2 + Math.round(lineCount * LINE_HEIGHT);
    }

    protected float scale() {
        return Math.max(0.5f, configScale());
    }

    private static boolean inGame(Minecraft client) {
        if (client.player == null || client.level == null || client.options.hideGui) return false;
        if (HudEditOverlay.getInstance().isActive()) return true;
        return !(client.screen instanceof com.pvp_utils.client.gui.clickgui.NewSettingsScreen)
        && !(client.screen instanceof NeteaseMusicScreen);
    }

    private int renderX(Minecraft client, int width) {
        int screenW = client.getWindow().getGuiScaledWidth();
        return clamp((int) (screenW * 0.5f + configX()), 0, Math.max(0, screenW - Math.round(width * scale())));
    }

    private int renderY(Minecraft client, int height) {
        int screenH = client.getWindow().getGuiScaledHeight();
        return clamp((int) (screenH * 0.5f + configY()), 0, Math.max(0, screenH - Math.round(height * scale())));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
