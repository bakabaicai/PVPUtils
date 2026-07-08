package com.pvp_utils.client.modules.impl.Optimize;

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
    private static final int FALLBACK_WIDTH = 160;
    private static final int FALLBACK_HEIGHT = 96;
    private static final int RIGHT_MARGIN = 3;
    private static final int HORIZONTAL_PADDING = 4;
    private static final int TITLE_EXTRA_HEIGHT = 10;
    private static Rect lastRect = new Rect(0.0f, 0.0f, FALLBACK_WIDTH, FALLBACK_HEIGHT);

    private BetterScoreboardManager() {
    }

    public static boolean enabled() {
        return Config.betterScoreboard;
    }

    public static void pushTransform(GuiGraphics graphics, Objective objective) {
        if (!enabled()) {
            return;
        }
        Rect rect = measure(graphics, objective);
        lastRect = rect;
        float scale = getScale();

        graphics.pose().pushMatrix();
        graphics.pose().translate(rect.x() + Config.betterScoreboardX, rect.y() + Config.betterScoreboardY);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-rect.x(), -rect.y());
    }

    public static void popTransform(GuiGraphics graphics) {
        if (enabled()) {
            graphics.pose().popMatrix();
        }
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
        return Math.max(1.0f, lastRect.w()) * getScale();
    }

    public static float getScaledHeight() {
        return Math.max(1.0f, lastRect.h()) * getScale();
    }

    public static float getScale() {
        return Math.max(0.5f, Config.betterScoreboardScale);
    }

    public static int getEditWidth() {
        return Math.round(Math.max(1.0f, lastRect.w()));
    }

    public static int getEditHeight() {
        return Math.round(Math.max(1.0f, lastRect.h()));
    }

    public static Rect getCurrentRect(int guiW, int guiH) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null) {
            Objective objective = client.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
            if (objective != null && client.gui != null) {
                lastRect = measure(client.gui.getFont(), guiW > 0 ? guiW : client.getWindow().getGuiScaledWidth(), guiH > 0 ? guiH : client.getWindow().getGuiScaledHeight(), objective);
                return lastRect;
            }
        }
        int width = Math.round(Math.max(1.0f, lastRect.w()));
        int height = Math.round(Math.max(1.0f, lastRect.h()));
        int resolvedGuiW = guiW > 0 ? guiW : Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int resolvedGuiH = guiH > 0 ? guiH : Minecraft.getInstance().getWindow().getGuiScaledHeight();
        return new Rect(resolvedGuiW - width - RIGHT_MARGIN - 2, (resolvedGuiH - height) * 0.5f, width, height);
    }

    private static Rect measure(GuiGraphics graphics, Objective objective) {
        return measure(Minecraft.getInstance().gui.getFont(), graphics.guiWidth(), graphics.guiHeight(), objective);
    }

    private static Rect measure(Font font, int guiW, int guiH, Objective objective) {
        if (objective == null) {
            return new Rect(guiW - FALLBACK_WIDTH - RIGHT_MARGIN - 2, (guiH - FALLBACK_HEIGHT) * 0.5f, FALLBACK_WIDTH, FALLBACK_HEIGHT);
        }

        Scoreboard scoreboard = objective.getScoreboard();
        NumberFormat numberFormat = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
        List<PlayerScoreEntry> entries = scoreboard.listPlayerScores(objective).stream()
                .filter(entry -> !entry.isHidden())
                .sorted(Comparator.comparingInt(PlayerScoreEntry::value).reversed().thenComparing(PlayerScoreEntry::owner))
                .limit(15)
                .toList();

        int titleWidth = font.width(objective.getDisplayName());
        int width = titleWidth;
        int colonWidth = font.width(":");
        for (PlayerScoreEntry entry : entries) {
            PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
            Component name = PlayerTeam.formatNameForTeam(team, entry.ownerName());
            int scoreWidth = font.width(entry.formatValue(numberFormat));
            int rowWidth = font.width(name) + (scoreWidth > 0 ? colonWidth + scoreWidth : 0);
            width = Math.max(width, rowWidth);
        }

        int rowsHeight = entries.size() * 9;
        int bottom = guiH / 2 + rowsHeight / 3;
        int x = guiW - width - RIGHT_MARGIN - 2;
        int y = bottom - rowsHeight - TITLE_EXTRA_HEIGHT;
        int h = rowsHeight + TITLE_EXTRA_HEIGHT;
        return new Rect(x, y, width + HORIZONTAL_PADDING, h);
    }

    public record Rect(float x, float y, float w, float h) {
    }
}
