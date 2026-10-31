package com.pvp_utils.client.modules.impl.Tool;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;

import java.util.Optional;
import java.util.UUID;

public final class NickHiderManager {
    private static final int MAX_NICKNAME_LENGTH = 32;

    private NickHiderManager() {
    }

    public static Component replaceChat(Component component) {
        boolean disguise = Config.nickHider && Config.nickHiderChat;
        if (!disguise && !Config.sunWorldRankSpoof) {
            return component;
        }
        return replace(component, disguise);
    }

    public static Component replaceTabName(Component component, PlayerInfo playerInfo) {
        boolean disguise = Config.nickHider && Config.nickHiderTab;
        if ((!disguise && !Config.sunWorldRankSpoof) || !isLocalPlayer(playerInfo)) {
            return component;
        }
        return replace(component, disguise);
    }

    public static Component replaceNameTag(Component component, Entity entity) {
        boolean disguise = Config.nickHider && Config.nickHiderNametag;
        if ((!disguise && !Config.sunWorldRankSpoof) || !isLocalPlayer(entity)) {
            return component;
        }
        return replace(component, disguise);
    }

    public static String normalizeNickname(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        value.codePoints().forEach(codepoint -> {
            if (out.codePointCount(0, out.length()) >= MAX_NICKNAME_LENGTH) return;
            if (codepoint >= 32 && codepoint != 127 && codepoint != '\n' && codepoint != '\r' && codepoint != '\t') {
                out.appendCodePoint(codepoint);
            }
        });
        return out.toString();
    }

    private static Component replace(Component component, boolean disguise) {
        if (component == null) {
            return null;
        }
        String realName = realName();
        String nickname = disguise ? replacementName() : realName;
        String replacement = rankPrefix() + nickname;
        if (realName.isBlank() || nickname.isBlank() || realName.equals(replacement)) {
            return component;
        }

        MutableComponent replaced = Component.empty().setStyle(component.getStyle());
        boolean[] changed = {false};
        component.visit((Style style, String text) -> {
            String newText = replaceName(text, realName, replacement);
            if (!newText.equals(text)) {
                changed[0] = true;
            }
            replaced.append(Component.literal(newText).withStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return changed[0] ? replaced : component;
    }

    private static String replacementName() {
        return normalizeNickname(Config.nickHiderNickname);
    }

    private static String rankPrefix() {
        if (!Config.sunWorldRankSpoof) {
            return "";
        }
        StringBuilder prefix = new StringBuilder();
        if (Config.sunWorldRankVip1) prefix.append('\uE011');
        if (Config.sunWorldRankVip2) prefix.append('\uE012');
        if (Config.sunWorldRankVip3) prefix.append('\uE013');
        if (Config.sunWorldRankVip4) prefix.append('\uE014');
        if (Config.sunWorldRankSvip) prefix.append('\uE015');
        if (Config.sunWorldRankCreator) prefix.append('\uE016');
        if (Config.sunWorldRankGuide) prefix.append('\uE018');
        if (Config.sunWorldRankAdmin) prefix.append('\uE017');
        return prefix.length() == 0 ? "" : prefix + " ";
    }

    private static String replaceName(String text, String realName, String nickname) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        int index = 0;
        while (index < text.length()) {
            int found = text.indexOf(realName, index);
            if (found < 0) {
                out.append(text, index, text.length());
                break;
            }
            int end = found + realName.length();
            if (isNameBoundary(text, found - 1) && isNameBoundary(text, end)) {
                out.append(text, index, found).append(nickname);
                index = end;
            } else {
                out.append(text, index, end);
                index = end;
            }
        }
        return out.toString();
    }

    private static boolean isNameBoundary(String text, int index) {
        if (index < 0 || index >= text.length()) {
            return true;
        }
        char c = text.charAt(index);
        return !(c == '_' || Character.isLetterOrDigit(c));
    }

    private static boolean isLocalPlayer(PlayerInfo playerInfo) {
        if (playerInfo == null || playerInfo.getProfile() == null) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        return client.player != null && playerInfo.getProfile().id().equals(client.player.getUUID());
    }

    private static boolean isLocalPlayer(Entity entity) {
        Minecraft client = Minecraft.getInstance();
        return entity != null && client.player != null && entity.getUUID().equals(client.player.getUUID());
    }

    private static String realName() {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null && client.player.getGameProfile() != null) {
            String name = client.player.getGameProfile().name();
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        String userName = client == null ? null : client.getUser().getName();
        return userName == null ? "" : userName;
    }
}
