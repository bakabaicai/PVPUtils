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
        long now = System.currentTimeMillis();
        int y = 10;
        entries.removeIf(e -> now - e.time > Config.combatLogMaxSeconds * 1000L);
        for (LogEntry e : entries) {
            graphics.drawString(client.font, e.text, 10, y, e.color, true);
            y += 10;
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
