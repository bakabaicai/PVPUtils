package com.pvp_utils;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;

public class DamageRecordHandler {
    private static long lastRecordTime = 0;
    private static final long COOLDOWN = 10;

    public static void showDamage(Entity target, float damage, boolean isRanged) {
        if (!Config.damageRecord) return;

        long now = System.currentTimeMillis();
        if (now - lastRecordTime < COOLDOWN) return;
        lastRecordTime = now;

        Minecraft client = Minecraft.getInstance();
        boolean cn = Config.isChinese;

        MutableComponent targetText = Component.literal(target.getDisplayName().getString())
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

        ChatFormatting color;
        if (damage < 10) color = ChatFormatting.GREEN;
        else if (damage < 30) color = ChatFormatting.LIGHT_PURPLE;
        else if (damage < 100) color = ChatFormatting.GOLD;
        else color = ChatFormatting.RED;

        MutableComponent damageText = Component.literal(String.format("%.1f", damage)).withStyle(color);
        String type = cn ? (isRanged ? "远程" : "近战") : (isRanged ? "Ranged" : "Melee");

        MutableComponent finalMsg = Component.literal(cn ? "你对 " : "You dealt ")
                .append(targetText)
                .append(cn ? " 造成 " : " dealt ")
                .append(damageText)
                .append(cn ? " 点 " : " points of ")
                .append(Component.literal(type + (cn ? "伤害" : " damage")));

        client.player.displayClientMessage(finalMsg, false);
        client.gui.setTimes(0, 40, 10);
    }
}