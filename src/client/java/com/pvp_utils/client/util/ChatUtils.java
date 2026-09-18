package com.pvp_utils.client.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ChatUtils {
    public enum Mode {
        DEFAULT(ChatFormatting.GRAY, false),
        ERROR(ChatFormatting.RED, true),
        WARNING(ChatFormatting.YELLOW, true),
        SUCCESS(ChatFormatting.GREEN, true),
        FAILURE(ChatFormatting.RED, true);

        private final ChatFormatting color;
        private final boolean plainTextOnly;

        Mode(ChatFormatting color, boolean plainTextOnly) {
            this.color = color;
            this.plainTextOnly = plainTextOnly;
        }
    }

    private ChatUtils() {
    }

    public static void send(String message) {
        send(Mode.DEFAULT, message);
    }

    public static void send(Component message) {
        send(Mode.DEFAULT, message);
    }

    public static void send(Mode mode, String message) {
        send(mode, Component.literal(message == null ? "" : message));
    }

    public static void send(Mode mode, Component message) {
        Mode resolvedMode = mode == null ? Mode.DEFAULT : mode;
        Component resolvedMessage = message == null ? Component.empty() : message;
        MutableComponent output = prefix().append(formatMessage(resolvedMode, resolvedMessage));
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        client.execute(() -> {
            if (client.gui != null) {
                client.gui.getChat().addMessage(output);
            }
        });
    }

    public static void error(String message) {
        send(Mode.ERROR, message);
    }

    public static void error(Component message) {
        send(Mode.ERROR, message);
    }

    public static void warning(String message) {
        send(Mode.WARNING, message);
    }

    public static void warning(Component message) {
        send(Mode.WARNING, message);
    }

    public static void success(String message) {
        send(Mode.SUCCESS, message);
    }

    public static void success(Component message) {
        send(Mode.SUCCESS, message);
    }

    public static void failure(String message) {
        send(Mode.FAILURE, message);
    }

    public static void failure(Component message) {
        send(Mode.FAILURE, message);
    }

    private static MutableComponent prefix() {
        return Component.literal("[")
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal("P").withStyle(ChatFormatting.GOLD))
                .append(Component.literal("] ").withStyle(ChatFormatting.WHITE));
    }

    private static Component formatMessage(Mode mode, Component message) {
        if (mode.plainTextOnly) {
            return Component.literal(message.getString()).withStyle(mode.color);
        }
        if (hasVanillaStyle(message)) {
            return message;
        }
        return message.copy().withStyle(mode.color);
    }

    private static boolean hasVanillaStyle(Component component) {
        if (!component.getStyle().isEmpty()) {
            return true;
        }
        for (Component sibling : component.getSiblings()) {
            if (hasVanillaStyle(sibling)) {
                return true;
            }
        }
        return false;
    }
}
