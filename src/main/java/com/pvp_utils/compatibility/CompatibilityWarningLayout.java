package com.pvp_utils.compatibility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record CompatibilityWarningLayout(Rect panel, Rect content, Rect selector, List<Rect> actions,
                                         int titleX, int titleY) {
    public static CompatibilityWarningLayout create(int screenWidth, int screenHeight, int contentHeight,
                                                     int[] actionWidths, boolean selectorVisible) {
        int margin = Math.min(12, Math.max(2, Math.min(screenWidth, screenHeight) / 16));
        int panelWidth = Math.max(1, Math.min(620, screenWidth - margin * 2));
        int padding = Math.min(16, Math.max(4, panelWidth / 20));
        int innerWidth = Math.max(1, panelWidth - padding * 2);
        int gap = 6;
        boolean stacked = Arrays.stream(actionWidths).sum() + Math.max(0, actionWidths.length - 1) * gap > innerWidth;
        int rows = stacked ? actionWidths.length : 1;
        int availableHeight = Math.max(1, screenHeight - margin * 2);
        int buttonHeight = Math.max(10, Math.min(20, (availableHeight - 60) / Math.max(1, rows)));
        int actionHeight = rows * buttonHeight + Math.max(0, rows - 1) * gap;
        int header = 28;
        int selectorHeight = selectorVisible ? 24 : 0;
        int desired = padding * 2 + header + selectorHeight + contentHeight + 12 + actionHeight;
        int panelHeight = Math.max(1, Math.min(availableHeight, desired));
        int x = (screenWidth - panelWidth) / 2;
        int y = (screenHeight - panelHeight) / 2;
        int actionTop = y + panelHeight - padding - actionHeight;
        int contentTop = y + padding + header + selectorHeight;
        Rect content = new Rect(x + padding, contentTop, innerWidth, Math.max(1, actionTop - 12 - contentTop));
        Rect selector = selectorVisible ? new Rect(x + padding, contentTop - selectorHeight, innerWidth, 20) : null;
        List<Rect> actions = new ArrayList<>();
        int currentX = x + padding;
        int currentY = actionTop;
        int total = Arrays.stream(actionWidths).sum();
        int extra = stacked || actionWidths.length == 0 ? 0
                : Math.max(0, (innerWidth - total - Math.max(0, actionWidths.length - 1) * gap) / actionWidths.length);
        for (int width : actionWidths) {
            int actionWidth = stacked ? innerWidth : width + extra;
            actions.add(new Rect(currentX, currentY, actionWidth, buttonHeight));
            if (stacked) currentY += buttonHeight + gap;
            else currentX += actionWidth + gap;
        }
        return new CompatibilityWarningLayout(new Rect(x, y, panelWidth, panelHeight), content, selector,
                List.copyOf(actions), x + padding, y + padding + 2);
    }

    public record Rect(int x, int y, int width, int height) {
        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }

        public boolean contains(double px, double py) {
            return px >= x && px < right() && py >= y && py < bottom();
        }
    }
}
