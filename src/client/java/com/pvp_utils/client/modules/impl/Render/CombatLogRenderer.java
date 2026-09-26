package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import java.util.ArrayDeque;
import java.util.Deque;

public final class CombatLogRenderer {
    private static final CombatLogRenderer INSTANCE = new CombatLogRenderer();
    private final Deque<LogEntry> entries = new ArrayDeque<>();

    private CombatLogRenderer() {}
    public static CombatLogRenderer getInstance() { return INSTANCE; }

    public void addEntry(String text, int color) {
        entries.addLast(new LogEntry(text, color, System.currentTimeMillis()));
        while (entries.size() > 10) entries.removeFirst();
    }

    public void render(GuiGraphics graphics) {
        if (!Config.combatLog) return;
        Minecraft client = Minecraft.getInstance();
        if (entries.isEmpty()) return;

        int x = 10;
        int y = 10;
        int maxWidth = 0;
        for (LogEntry e : entries) {
            int w = client.font.width(e.text);
            if (w > maxWidth) maxWidth = w;
        }
        int boxW = maxWidth + 8;
        int boxH = entries.size() * 10 + 6;

        graphics.fill(x - 2, y - 2, x + boxW, y + boxH, 0x80000000);
        graphics.drawString(client.font, "Combat Log", x, y - 10, 0xFFFFFF, true);

        int ly = y;
        for (LogEntry e : entries) {
            graphics.drawString(client.font, e.text, x + 4, ly, e.color, true);
            ly += 10;
        }
    }

    private static class LogEntry {
        final String text;
        final int color;
        final long time;
        LogEntry(String text, int color, long time) {
            this.text = text;
            this.color = color;
            this.time = time;
        }
    }
}
