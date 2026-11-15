package com.pvp_utils.client.combat;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class KillAnnouncementParser {
    private static final String PLAYER = "(?:\\[[^\\]\\r\\n]{1,24}\\]\\s*)?([A-Za-z0-9_]{1,16})";
    private static final String SUFFIX = "[!！。.]?(?:\\s*(?:最终击杀|终结击杀|FINAL KILL)[!！])?";
    private static final Pattern KILLER_FIRST = Pattern.compile("^" + PLAYER + "\\s*击杀了\\s*" + PLAYER
            + SUFFIX + "(?:\\s*\\([1-9][0-9]{0,5} 连杀\\))?$");
    private static final Pattern FORMATTING = Pattern.compile("§[0-9A-FK-ORX]", Pattern.CASE_INSENSITIVE);
    private static final List<Pattern> PATTERNS = List.of(
            Pattern.compile("^" + PLAYER + " was (?:killed|slain|shot) by " + PLAYER
                    + "(?: using \\[[^\\]\\r\\n]{1,128}\\])?[.!](?: FINAL KILL!)?$"),
            Pattern.compile("^" + PLAYER + " was (?:knocked|thrown) into the void by " + PLAYER
                    + "[.!](?: FINAL KILL!)?$"),
            Pattern.compile("^" + PLAYER + " was knocked off a cliff by " + PLAYER
                    + "[.!](?: FINAL KILL!)?$"),
            Pattern.compile("^" + PLAYER + " was killed with magic by " + PLAYER
                    + "[.!](?: FINAL KILL!)?$"),
            Pattern.compile("^" + PLAYER + "\\s*被\\s*" + PLAYER
                    + "\\s*(?:击杀|杀死|射杀|击倒|打满了子弹)(?:了)?" + SUFFIX + "$"),
            Pattern.compile("^" + PLAYER + "\\s*被\\s*" + PLAYER
                    + "\\s*(?:击落|推入|打入|丢进)(?:了)?虚空" + SUFFIX + "$"),
            Pattern.compile("^" + PLAYER + "\\s*在近战中被\\s*" + PLAYER + "\\s*击败" + SUFFIX + "$"),
            Pattern.compile("^" + PLAYER + "\\s*在\\s*" + PLAYER
                    + "\\s*的帮助下跌落到虚空" + SUFFIX + "$"));
    private static final Set<String> ATTRIBUTED_DEATH_KEYS = Set.of(
            "death.attack.player", "death.attack.mob", "death.attack.arrow", "death.attack.trident",
            "death.attack.thorns", "death.attack.fireball", "death.attack.explosion.player",
            "death.attack.sonic_boom", "death.attack.mace_smash");

    private KillAnnouncementParser() {
    }

    static Announcement parse(Component message, String localName) {
        String text = clean(message.getString());
        if (text.length() > 256 || !containsPlayerName(text, localName)) return null;
        if (message.getContents() instanceof TranslatableContents contents) {
            String key = contents.getKey();
            if (key.endsWith(".item")) key = key.substring(0, key.length() - 5);
            Object[] args = contents.getArgs();
            if (ATTRIBUTED_DEATH_KEYS.contains(key) && args.length >= 2) {
                String victim = argumentName(args[0]);
                String killer = argumentName(args[1]);
                if (victim != null && killer != null) return new Announcement(victim, killer);
            }
        }
        for (Pattern pattern : PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.matches()) return new Announcement(matcher.group(1), matcher.group(2));
        }
        Matcher killerFirst = KILLER_FIRST.matcher(text);
        if (killerFirst.matches()) return new Announcement(killerFirst.group(2), killerFirst.group(1));
        return null;
    }

    private static boolean containsPlayerName(String text, String name) {
        if (name == null || name.isEmpty()) return false;
        for (int index = 0; index <= text.length() - name.length(); index++) {
            if (!text.regionMatches(true, index, name, 0, name.length())) continue;
            int end = index + name.length();
            if ((index == 0 || !isNameCharacter(text.charAt(index - 1)))
                    && (end == text.length() || !isNameCharacter(text.charAt(end)))) return true;
        }
        return false;
    }

    private static boolean isNameCharacter(char character) {
        return character >= 'A' && character <= 'Z' || character >= 'a' && character <= 'z'
                || character >= '0' && character <= '9' || character == '_';
    }

    private static String argumentName(Object argument) {
        String name = clean(argument instanceof Component component ? component.getString() : String.valueOf(argument));
        return name.matches("[A-Za-z0-9_]{1,16}") ? name : null;
    }

    private static String clean(String text) {
        return FORMATTING.matcher(text).replaceAll("").strip();
    }

    record Announcement(String victim, String killer) {
    }
}
