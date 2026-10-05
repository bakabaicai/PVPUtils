package com.pvp_utils.client.plugin;

import org.mozilla.javascript.Context;

final class PluginPlayerArguments {
    private PluginPlayerArguments() {
    }

    static void validate(String operation, Object[] args) {
        switch (operation) {
            case "snapshot", "target", "getItems", "getSelectedSlot", "jump", "stopUsingItem", "closeMenu", "attackTarget" ->
                    count(args, 0);
            case "setRotation" -> {
                count(args, 2);
                number(args, 0, 360000);
                number(args, 1, 90);
            }
            case "setVelocity" -> {
                count(args, 3);
                for (int i = 0; i < 3; i++) number(args, i, 10);
            }
            case "setSprinting" -> {
                count(args, 1);
                if (!(args[0] instanceof Boolean)) throw new IllegalArgumentException("Expected boolean argument");
            }
            case "getSlot", "select" -> {
                count(args, 1);
                integer(args, 0, 0, operation.equals("select") ? 8 : Integer.MAX_VALUE);
            }
            case "swing", "useItem", "interactTarget" -> {
                if (args.length > 1) throw new IllegalArgumentException("Expected zero or one hand argument");
                if (args.length > 0 && !Context.toString(args[0]).equals("main") && !Context.toString(args[0]).equals("off")) {
                    throw new IllegalArgumentException("Hand must be main or off");
                }
            }
            case "menuSnapshot" -> {
                if (args.length > 1) throw new IllegalArgumentException("Invalid menu arguments");
            }
            case "click" -> {
                count(args, 4);
                integer(args, 1, 0, Integer.MAX_VALUE);
                String type = Context.toString(args[3]);
                if (!type.equals("pickup") && !type.equals("quick_move") && !type.equals("swap") && !type.equals("throw")) {
                    throw new IllegalArgumentException("Click type must be pickup, quick_move, swap or throw");
                }
                integer(args, 2, 0, type.equals("swap") ? 8 : 1);
            }
            default -> throw new IllegalArgumentException("Unknown player operation: " + operation);
        }
    }

    private static void count(Object[] args, int count) {
        if (args.length != count) throw new IllegalArgumentException("Expected " + count + " arguments");
    }

    static double number(Object[] args, int index, double maximum) {
        if (index >= args.length || !(args[index] instanceof Number input)) {
            throw new IllegalArgumentException("Expected numeric argument");
        }
        double value = input.doubleValue();
        if (!Double.isFinite(value) || Math.abs(value) > maximum) throw new IllegalArgumentException("Invalid number");
        return value;
    }

    static int integer(Object[] args, int index, int min, int max) {
        double value = number(args, index, Integer.MAX_VALUE);
        if (value != Math.rint(value) || value < min || value > max) throw new IllegalArgumentException("Invalid slot or button");
        return (int) value;
    }
}
