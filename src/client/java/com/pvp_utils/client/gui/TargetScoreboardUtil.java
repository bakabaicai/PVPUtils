package com.pvp_utils.client.gui;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.Scoreboard;

public class TargetScoreboardUtil {
    public static int getBelowNameHealth(LivingEntity entity) {
        if (!(entity instanceof Player player)) return -1;

        Scoreboard scoreboard = player.level().getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.BELOW_NAME);

        if (objective != null) {
            ReadOnlyScoreInfo scoreInfo = scoreboard.getPlayerScoreInfo(player, objective);
            if (scoreInfo != null) {
                return scoreInfo.value();
            }
        }
        return -1;
    }
}