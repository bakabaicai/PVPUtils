package com.pvp_utils.client.via;

import net.minecraft.client.gui.screens.Screen;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ViaFabricPlusBridge {
    private static final Pattern MAJOR_PATTERN = Pattern.compile("^(\\d+\\.\\d+)");
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+\\.\\d+)(?:\\.(\\d+))?.*");
    private static final Pattern PARENTHESIS_PATTERN = Pattern.compile("\\s*\\([^)]*\\)");

    private ViaFabricPlusBridge() {
    }

    public static boolean isInstalled() {
        try {
            if (!FabricLoader.getInstance().isModLoaded("viafabricplus")) {
                return false;
            }
        } catch (Throwable ignored) {
            return false;
        }
        try {
            Class.forName("com.viaversion.viafabricplus.ViaFabricPlus", false, ViaFabricPlusBridge.class.getClassLoader());
            Class.forName("com.viaversion.viaversion.api.protocol.version.ProtocolVersion", false, ViaFabricPlusBridge.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isModMenuInstalled() {
        try {
            return FabricLoader.getInstance().isModLoaded("modmenu")
                    && Class.forName("com.terraformersmc.modmenu.gui.ModsScreen", false,
                    ViaFabricPlusBridge.class.getClassLoader()) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean openModMenu(Screen parent) {
        if (!isModMenuInstalled()) return false;
        try {
            Class<?> screenClass = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
            Object screen = screenClass.getConstructor(Screen.class).newInstance(parent);
            net.minecraft.client.Minecraft.getInstance().setScreen((Screen) screen);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static List<ProtocolGroup> protocolGroups() {
        Map<String, List<ProtocolEntry>> grouped = new LinkedHashMap<>();
        try {
            Class<?> protocolClass = Class.forName("com.viaversion.viaversion.api.protocol.version.ProtocolVersion");
            Method reversed = protocolClass.getMethod("getReversedProtocols");
            Object value = reversed.invoke(null);
            if (value instanceof Iterable<?> iterable) {
                for (Object protocol : iterable) {
                    String name = String.valueOf(protocol.getClass().getMethod("getName").invoke(protocol));
                    String major = majorName(name);
                    grouped.computeIfAbsent(major, ignored -> new ArrayList<>()).add(new ProtocolEntry(name, protocol));
                }
            }
        } catch (Throwable ignored) {
        }
        return grouped.entrySet().stream()
                .map(entry -> {
                    List<ProtocolEntry> entries = new ArrayList<>(entry.getValue());
                    entries.sort((left, right) -> sortKey(left.name()).compareTo(sortKey(right.name())));
                    return new ProtocolGroup(displayName(entry.getKey()), List.copyOf(entries));
                })
                .sorted((left, right) -> compareGroups(left.name(), right.name()))
                .toList();
    }

    public static String targetVersionName() {
        try {
            Object impl = implementation();
            Object version = impl.getClass().getMethod("getTargetVersion").invoke(impl);
            return version == null ? "" : String.valueOf(version.getClass().getMethod("getName").invoke(version));
        } catch (Throwable ignored) {
            return "";
        }
    }

    public static boolean setTargetVersion(ProtocolEntry entry) {
        if (entry == null || entry.value() == null) return false;
        try {
            Object impl = implementation();
            Method method = findMethod(impl.getClass(), "setTargetVersion", 1);
            if (method == null) return false;
            method.invoke(impl, entry.value());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void openSettings(Screen parent) {
        try {
            Object impl = implementation();
            impl.getClass().getMethod("openSettingsScreen", Screen.class).invoke(impl, parent);
        } catch (Throwable ignored) {
        }
    }

    public static void openServerList(Screen parent) {
        openStaticScreen("com.viaversion.viafabricplus.screen.impl.ServerListScreen", parent);
    }

    public static void openReportIssues(Screen parent) {
        openStaticScreen("com.viaversion.viafabricplus.screen.impl.ReportIssuesScreen", parent);
    }

    private static void openStaticScreen(String className, Screen parent) {
        try {
            Class<?> type = Class.forName(className);
            Object instance = type.getField("INSTANCE").get(null);
            type.getMethod("open", Screen.class).invoke(instance, parent);
        } catch (Throwable ignored) {
        }
    }

    private static Object implementation() throws Exception {
        Class<?> holder = Class.forName("com.viaversion.viafabricplus.ViaFabricPlus");
        return holder.getMethod("getImpl").invoke(null);
    }

    private static Method findMethod(Class<?> type, String name, int parameterCount) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                return method;
            }
        }
        return null;
    }

    private static String majorName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("26w")
                || normalized.startsWith("25w")
                || normalized.startsWith("20w")
                || normalized.contains("combat test")
                || normalized.contains("experimental")) {
            return "Beta";
        }
        if (normalized.contains("bedrock")) {
            return "Bedrock";
        }
        if (normalized.startsWith("3d")
                || normalized.contains("shareware")
                || normalized.contains("conbotest")) {
            return "Ancient";
        }
        if (normalized.startsWith("alpha")
                || normalized.startsWith("beta")
                || normalized.startsWith("classic")
                || normalized.startsWith("indev")
                || normalized.startsWith("infdev")
                || normalized.startsWith("rd-")
                || normalized.startsWith("c0.")
                || normalized.startsWith("b1.")
                || normalized.startsWith("a1.")) {
            return "Ancient";
        }
        Matcher matcher = MAJOR_PATTERN.matcher(name);
        if (matcher.find()) return matcher.group(1);
        return normalized;
    }

    private static String displayName(String name) {
        return switch (name) {
            case "Beta" -> "Beta";
            case "Bedrock" -> "Bedrock";
            case "Ancient" -> "Ancient";
            default -> cleanName(name);
        };
    }

    private static String cleanName(String name) {
        String cleaned = PARENTHESIS_PATTERN.matcher(name).replaceAll("").trim();
        return cleaned.isBlank() ? name : cleaned;
    }

    private static int groupRank(String name) {
        return switch (name) {
            case "Beta" -> 1;
            case "Bedrock" -> 2;
            case "Ancient" -> 3;
            default -> 0;
        };
    }

    private static int compareGroups(String left, String right) {
        int leftRank = groupRank(left);
        int rightRank = groupRank(right);
        if (leftRank != rightRank) {
            if (leftRank == 0) return -1;
            if (rightRank == 0) return 1;
            return Integer.compare(leftRank, rightRank);
        }
        if (leftRank != 0) return 0;
        return sortKey(right).compareTo(sortKey(left));
    }

    public static String sortKey(String name) {
        Matcher matcher = VERSION_PATTERN.matcher(name);
        if (!matcher.matches()) return name;
        int major = Integer.parseInt(matcher.group(1).substring(0, matcher.group(1).indexOf('.')));
        String minorPart = matcher.group(1).substring(matcher.group(1).indexOf('.') + 1);
        int minor = Integer.parseInt(minorPart);
        int patch = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
        return String.format(Locale.ROOT, "%05d.%05d.%05d", major, minor, patch);
    }

    public record ProtocolEntry(String name, Object value) {
    }

    public record ProtocolGroup(String name, List<ProtocolEntry> entries) {
    }
}
