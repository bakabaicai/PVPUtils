package com.pvp_utils.compatibility;

import java.util.Locale;
import java.util.Map;

public record PlatformEnvironment(String name, String version, String architecture, String bitness,
                                  boolean supported) {
    public static PlatformEnvironment current() {
        return inspect(Map.of(
                "os.name", System.getProperty("os.name", "Unknown"),
                "os.version", System.getProperty("os.version", "Unknown"),
                "os.arch", System.getProperty("os.arch", "Unknown"),
                "sun.arch.data.model", System.getProperty("sun.arch.data.model", ""),
                "java.vm.name", System.getProperty("java.vm.name", ""),
                "java.runtime.name", System.getProperty("java.runtime.name", "")
        ));
    }

    public static PlatformEnvironment inspect(Map<String, String> properties) {
        String name = properties.getOrDefault("os.name", "Unknown");
        String version = properties.getOrDefault("os.version", "Unknown");
        String architecture = properties.getOrDefault("os.arch", "Unknown");
        String normalized = architecture.toLowerCase(Locale.ROOT);
        boolean x64 = normalized.equals("amd64") || normalized.equals("x86_64") || normalized.equals("x64");
        String bitness = properties.getOrDefault("sun.arch.data.model", "");
        if (bitness.isBlank()) {
            bitness = normalized.contains("64") ? "64" : "32";
        }
        String runtime = (properties.getOrDefault("java.vm.name", "") + " "
                + properties.getOrDefault("java.runtime.name", "")).toLowerCase(Locale.ROOT);
        boolean windows = name.toLowerCase(Locale.ROOT).startsWith("windows");
        boolean supported = windows && majorVersion(version) >= 10 && x64 && bitness.equals("64")
                && !runtime.contains("android") && !runtime.contains("dalvik");
        return new PlatformEnvironment(name, version, architecture, bitness, supported);
    }

    private static int majorVersion(String version) {
        try {
            return Integer.parseInt(version.split("[.\\s]", 2)[0]);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    public String description() {
        return name + " " + version + " / " + architecture + " / " + bitness + "-bit JVM";
    }
}
