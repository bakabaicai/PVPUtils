package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.DisplaySlot;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TeammateHealthBarRenderer {
    private static final TeammateHealthBarRenderer INSTANCE = new TeammateHealthBarRenderer();
    private static final Pattern HP_PATTERN = Pattern.compile("([A-Za-z0-9_]{2,16})\\D+(\\d{1,3})");

    private TeammateHealthBarRenderer() {}
    public static TeammateHealthBarRenderer getInstance() { return INSTANCE; }

    public void render(GuiGraphics graphics) {
        if (!Config.teammateHealthBar) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        Objective objective = client.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return;

        List<String> lines = new ArrayList<>();
        for (net.minecraft.world.scores.PlayerScoreEntry entry : client.level.getScoreboard().listPlayerScores(objective)) {
            String name = entry.owner();
            int score = entry.value();
            if (score > 0) lines.add(name + " " + score);
        }

        int y = 60;
        for (String line : lines) {
            Matcher m = HP_PATTERN.matcher(line);
            if (m.find()) {
                String name = m.group(1);
                int hp = Integer.parseInt(m.group(2));
                if (hp > 0 && hp <= 20) {
                    graphics.drawString(client.font, name + ": " + hp + "/20", 10, y, 0x55FF55, true);
                    y += 12;
                }
            }
        }
    }
}
