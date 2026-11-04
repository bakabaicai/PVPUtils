package com.pvp_utils.client.render.MainUI;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ViaProtocolArt {
    private static final Pattern VERSION = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");
    private static final Set<String> FAMILIES = Set.of("1.7", "1.8", "1.9", "1.10", "1.11", "1.12",
            "1.13", "1.14", "1.15", "1.16", "1.17", "1.18", "1.19", "1.20", "26.2", "26.3");

    private ViaProtocolArt() {
    }

    static String resource(String name) {
        Matcher version = VERSION.matcher(name);
        if (!version.find()) return null;
        String family = version.group(1) + "." + version.group(2);
        if (FAMILIES.contains(family) || family.equals("26.1")) return "/via/" + family + ".png";
        if (!family.equals("1.21")) return null;
        int patch;
        try {
            patch = version.group(3) == null ? 0 : Integer.parseInt(version.group(3));
        } catch (NumberFormatException ignored) {
            return null;
        }
        String release = patch >= 11 ? "1.21.11" : patch >= 9 ? "1.21.9" : patch >= 6 ? "1.21.6"
                : patch >= 5 ? "1.21.5" : patch >= 4 ? "1.21.4" : patch >= 2 ? "1.21.2" : "1.21";
        return "/via/" + release + ".png";
    }
}
