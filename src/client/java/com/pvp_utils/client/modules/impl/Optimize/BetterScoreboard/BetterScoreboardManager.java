package com.pvp_utils.client.modules.impl.Optimize.BetterScoreboard;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.Comparator;
import java.util.List;

public final class BetterScoreboardManager {
    public static final float VISUAL_PADDING = 7f;
    public static final float VISUAL_OFFSET_Y = 3f;
    private static final Comparator<PlayerScoreEntry> SCORE_ORDER = Comparator.comparingInt(PlayerScoreEntry::value)
            .reversed().thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER);
    private static Rect lastRect = new Rect(0f, 0f, 100f, 30f);
    private static boolean transformPushed;

    private BetterScoreboardManager() {
    }

    public static boolean enabled() {
        return Config.betterScoreboard;
    }

    public static Objective currentObjective() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return null;
        Scoreboard scoreboard = client.level.getScoreboard();
        if (client.player != null) {
            PlayerTeam team = scoreboard.getPlayersTeam(client.player.getScoreboardName());
            if (team != null) {
                DisplaySlot slot = DisplaySlot.teamColorToSlot(team.getColor());
                if (slot != null) {
                    Objective objective = scoreboard.getDisplayObjective(slot);
                    if (objective != null) return objective;
                }
            }
        }
        return scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
    }

    public static void pushTransform(GuiGraphics graphics, Objective objective) {
        transformPushed = false;
        if (!enabled()) return;
        Rect rect = layout(Minecraft.getInstance().font, graphics.guiWidth(), graphics.guiHeight(), objective, false).rect();
        graphics.pose().pushMatrix();
        graphics.pose().translate(rect.x() + Config.betterScoreboardX, rect.y() + Config.betterScoreboardY);
        graphics.pose().scale(getScale(), getScale());
        graphics.pose().translate(-rect.x(), -rect.y());
        transformPushed = true;
    }

    public static void popTransform(GuiGraphics graphics) {
        if (!transformPushed) return;
        transformPushed = false;
        graphics.pose().popMatrix();
    }

    public static float getRenderX(int guiW) {
        return getCurrentRect(guiW, 0).x() + Config.betterScoreboardX;
    }

    public static float getRenderY(int guiH) {
        return getCurrentRect(0, guiH).y() + Config.betterScoreboardY;
    }

    public static float getDefaultX(int guiW) {
        return getCurrentRect(guiW, 0).x();
    }

    public static float getDefaultY(int guiH) {
        return getCurrentRect(0, guiH).y();
    }

    public static float getScaledWidth() {
        return Math.max(1f, lastRect.w()) * getScale();
    }

    public static float getScaledHeight() {
        return Math.max(1f, lastRect.h()) * getScale();
    }

    public static float getScale() {
        return Float.isFinite(Config.betterScoreboardScale) ? Math.clamp(Config.betterScoreboardScale, 0.5f, 4f) : 1f;
    }

    public static int getEditWidth() {
        return Math.round(Math.max(1f, lastRect.w()));
    }

    public static int getEditHeight() {
        return Math.round(Math.max(1f, lastRect.h()));
    }

    public static Rect getCurrentRect(int guiW, int guiH) {
        Minecraft client = Minecraft.getInstance();
        int width = guiW > 0 ? guiW : client.getWindow().getGuiScaledWidth();
        int height = guiH > 0 ? guiH : client.getWindow().getGuiScaledHeight();
        return layout(client.font, width, height, currentObjective(), Config.betterScoreboardVisualImprovement).rect();
    }

    public static Layout layout(Font font, int guiW, int guiH, Objective objective, boolean visual) {
        Component title = objective == null ? Component.literal(Config.isChinese ? "计分板" : "Scoreboard") : objective.getDisplayName().copy();
        List<Row> rows = objective == null
                ? List.of(new Row(Component.literal("Player"), Component.literal("10")),
                          new Row(Component.literal("Kills"), Component.literal("2")))
                : getRows(objective);
        int titleWidth = font.width(title);
        int width = titleWidth;
        int gap = font.width(": ");
        boolean hideScores = visual && Config.betterScoreboardHideScores;
        int[] scoreWidths = new int[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int scoreWidth = hideScores ? 0 : font.width(row.score());
            scoreWidths[i] = scoreWidth;
            width = Math.max(width, font.width(row.name()) + (scoreWidth > 0 ? gap + scoreWidth : 0));
        }
        int titleHeight = visual ? 12 : 10;
        int rowsHeight = rows.size() * 9;
        float x = guiW - width - 5f;
        float y = guiH / 2f + rowsHeight / 3f - rowsHeight - titleHeight;
        lastRect = new Rect(x, y, Math.max(1, width + (visual ? 0 : 4)), rowsHeight + titleHeight);
        return new Layout(lastRect, title, rows, titleWidth, scoreWidths, titleHeight, hideScores);
    }

    public static List<Row> getRows(Objective objective) {
        if (objective == null) return List.of();
        Scoreboard scoreboard = objective.getScoreboard();
        NumberFormat numberFormat = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
        return scoreboard.listPlayerScores(objective).stream()
                .filter(entry -> !entry.isHidden())
                .sorted(SCORE_ORDER)
                .limit(15)
                .map(entry -> new Row(PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(entry.owner()), entry.ownerName()).copy(),
                        entry.formatValue(numberFormat).copy()))
                .toList();
    }

    public record Rect(float x, float y, float w, float h) {
    }

    public record Row(Component name, Component score) {
    }

    public record Layout(Rect rect, Component title, List<Row> rows, int titleWidth,
                         int[] scoreWidths, int titleHeight, boolean hideScores) {
    }
}
