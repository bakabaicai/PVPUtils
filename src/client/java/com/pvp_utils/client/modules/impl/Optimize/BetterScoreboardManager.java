package com.pvp_utils.client.modules.impl.Optimize;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
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

    public static List<Row> getRows(Objective objective) {
        if (objective == null) {
            return List.of();
        }
        Scoreboard scoreboard = objective.getScoreboard();
        NumberFormat numberFormat = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
        return scoreboard.listPlayerScores(objective).stream()
                .filter(entry -> !entry.isHidden())
                .sorted(Comparator.comparingInt(PlayerScoreEntry::value).reversed().thenComparing(PlayerScoreEntry::owner))
                .limit(15)
                .map(entry -> {
                    PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
                    Component name = PlayerTeam.formatNameForTeam(team, entry.ownerName());
                    Component score = entry.formatValue(numberFormat);
                    return new Row(encodeStyled(name), encodeStyled(score), name, score,
                            needsNativeRendering(name),
                            needsNativeRendering(score));
                })
                .toList();
    }

    public static String encodeStyled(Component component) {
        if (component == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        component.visit((Style style, String text) -> {
            if (style != null && style.getColor() != null) {
                appendHexColor(builder, style.getColor().getValue());
            }
            builder.append(text);
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return builder.toString();
    }

    private static Rect measure(GuiGraphics graphics, Objective objective) {
        return measure(Minecraft.getInstance().gui.getFont(), graphics.guiWidth(), graphics.guiHeight(), objective);
    }

    private static Rect measure(Font font, int guiW, int guiH, Objective objective) {
        if (objective == null) {
            return new Rect(guiW - FALLBACK_WIDTH - RIGHT_MARGIN - 2, (guiH - FALLBACK_HEIGHT) * 0.5f, FALLBACK_WIDTH, FALLBACK_HEIGHT);
        }

        List<Row> entries = getRows(objective);

        int titleWidth = font.width(objective.getDisplayName());
        int width = titleWidth;
        int colonWidth = font.width(":");
        for (Row entry : entries) {
            int scoreWidth = font.width(stripLegacyCodes(entry.score()));
            int rowWidth = font.width(stripLegacyCodes(entry.name())) + (scoreWidth > 0 ? colonWidth + scoreWidth : 0);
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

    public record Row(String name, String score, Component nameComponent, Component scoreComponent, boolean nameFormatted, boolean scoreFormatted) {
        public Row(String name, String score) {
            this(name, score, Component.literal(name), Component.literal(score), containsNativeSymbols(name), containsNativeSymbols(score));
        }
    }

    public static boolean needsNativeRendering(Component component) {
        if (component == null) {
            return false;
        }
        final boolean[] nativeRender = {false};
        component.visit((Style style, String text) -> {
            if (containsNativeSymbols(text) || hasCustomFont(style)) {
                nativeRender[0] = true;
                return java.util.Optional.of(Boolean.TRUE);
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return nativeRender[0];
    }

    private static void appendHexColor(StringBuilder builder, int rgb) {
        String hex = String.format(java.util.Locale.ROOT, "%06X", rgb & 0xFFFFFF);
        builder.append('\u00A7').append('x');
        for (int i = 0; i < hex.length(); i++) {
            builder.append('\u00A7').append(hex.charAt(i));
        }
    }

    private static String stripLegacyCodes(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder clean = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if ((ch == '\u00A7' || ch == '&') && i + 1 < text.length()) {
                if (Character.toLowerCase(text.charAt(i + 1)) == 'x' && hasHexColor(text, i)) {
                    i += 13;
                } else {
                    i++;
                }
                continue;
            }
            clean.append(ch);
        }
        return clean.toString();
    }

    public static boolean containsLegacyCodes(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int i = 0; i + 1 < text.length(); i++) {
            char ch = text.charAt(i);
            if ((ch == '\u00A7' || ch == '&')) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsNativeSymbols(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int offset = 0; offset < text.length(); ) {
            int cp = text.codePointAt(offset);
            if (Character.isSupplementaryCodePoint(cp)
                    || Character.UnicodeBlock.of(cp) == Character.UnicodeBlock.PRIVATE_USE_AREA) {
                return true;
            }
            offset += Character.charCount(cp);
        }
        return false;
    }

    private static boolean hasCustomFont(Style style) {
        return style != null && style.getFont() != null && !style.getFont().equals(Style.EMPTY.getFont());
    }

    private static boolean hasHexColor(String text, int sectionIndex) {
        if (sectionIndex + 13 >= text.length()) return false;
        char marker = text.charAt(sectionIndex);
        for (int i = 0; i < 6; i++) {
            int prefix = sectionIndex + 2 + i * 2;
            int value = prefix + 1;
            if (text.charAt(prefix) != marker || !isHex(text.charAt(value))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHex(char value) {
        return (value >= '0' && value <= '9') || (value >= 'a' && value <= 'f') || (value >= 'A' && value <= 'F');
    }
}
