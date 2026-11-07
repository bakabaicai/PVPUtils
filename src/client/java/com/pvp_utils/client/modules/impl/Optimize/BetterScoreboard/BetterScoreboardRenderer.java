package com.pvp_utils.client.modules.impl.Optimize.BetterScoreboard;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Render.HudEditOverlay;
import com.pvp_utils.client.render.HudAppearance;
import com.pvp_utils.client.render.HudPanel;
import com.pvp_utils.client.render.skia.SkijaRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.scores.Objective;

public final class BetterScoreboardRenderer {
    private static final BetterScoreboardRenderer INSTANCE = new BetterScoreboardRenderer();

    private BetterScoreboardRenderer() {
    }

    public static BetterScoreboardRenderer getInstance() {
        return INSTANCE;
    }

    public void render(GuiGraphics graphics) {
        if (!Config.betterScoreboard || !Config.betterScoreboardVisualImprovement) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.options.hideGui) return;
        boolean editing = HudEditOverlay.getInstance().isActive();
        if (client.screen != null && !editing) return;
        Objective objective = BetterScoreboardManager.currentObjective();
        if (objective == null && !editing) return;
        BetterScoreboardManager.Layout layout = BetterScoreboardManager.layout(client.font,
                graphics.guiWidth(), graphics.guiHeight(), objective, true);
        BetterScoreboardManager.Rect base = layout.rect();
        float scale = BetterScoreboardManager.getScale();
        float padding = BetterScoreboardManager.VISUAL_PADDING;
        float width = (base.w() + padding * 2f) * scale;
        float height = (base.h() + padding * 2f) * scale;
        float x = Math.clamp(base.x() + Config.betterScoreboardX - padding * scale,
                0f, Math.max(0f, graphics.guiWidth() - width));
        float y = Math.clamp(base.y() + Config.betterScoreboardY
                        + (BetterScoreboardManager.VISUAL_OFFSET_Y - padding) * scale,
                0f, Math.max(0f, graphics.guiHeight() - height));
        HudAppearance appearance = Config.scoreboardAppearance;
        SkijaRenderer.draw(canvas -> HudPanel.draw(canvas, x, y, width, height, 9f * scale, appearance, 1f));
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(x, y);
            graphics.pose().scale(scale, scale);
            int textColor = appearance.text();
            graphics.drawString(client.font, layout.title(),
                    Math.round(padding + (base.w() - layout.titleWidth()) * 0.5f), Math.round(padding + 1f),
                    appearance.accent(), false);
            for (int i = 0; i < layout.rows().size(); i++) {
                BetterScoreboardManager.Row row = layout.rows().get(i);
                int rowY = Math.round(padding + layout.titleHeight() + i * 9);
                graphics.drawString(client.font, row.name(), Math.round(padding), rowY, textColor, false);
                if (!layout.hideScores() && layout.scoreWidths()[i] > 0) {
                    graphics.drawString(client.font, row.score(), Math.round(padding + base.w() - layout.scoreWidths()[i]),
                            rowY, textColor, false);
                }
            }
        } finally {
            graphics.pose().popMatrix();
        }
    }
}
