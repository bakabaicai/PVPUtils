package com.pvp_utils.client;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AntiCheat {
    public static final boolean ENABLED = true;
    private static final List<ClientSignature> SIGNATURES = List.of(
            new ClientSignature(
                    "Meteor Client",
                    List.of("meteor-client"),
                    List.of(
                            "meteordevelopment.meteorclient.MeteorClient",
                            "meteordevelopment.meteorclient.Main",
                            "meteordevelopment.meteorclient.MixinPlugin"
                    ),
                    List.of(
                            "meteor-client.mixins.json",
                            "meteor-client-baritone.mixins.json",
                            "meteor-client-indigo.mixins.json",
                            "meteor-client-sodium.mixins.json",
                            "meteor-client-lithium.mixins.json",
                            "meteor-client-viafabricplus.mixins.json",
                            "meteor-client.accesswidener",
                            "assets/meteor-client/icon.png"
                    ),
                    List.of(
                            "meteordevelopment.meteorclient",
                            "assets/meteor-client",
                            "maven.meteordev.org",
                            "meteorclient.com"
                    )
            ),
            new ClientSignature(
                    "LiquidBounce",
                    List.of("liquidbounce"),
                    List.of(
                            "net.ccbluex.liquidbounce.LiquidBounce",
                            "net.ccbluex.liquidbounce.LiquidInstruction",
                            "net.ccbluex.liquidbounce.injection.mixins.lithium.MixinChunkAwareBlockCollisionSweeper"
                    ),
                    List.of(
                            "liquidbounce.mixins.json",
                            "liquidbounce.accesswidener",
                            "resources/liquidbounce/icon_64x64.png",
                            "resources/liquidbounce/client_urls.properties",
                            "resources/liquidbounce/logo_banner.png"
                    ),
                    List.of(
                            "net.ccbluex.liquidbounce",
                            "resources/liquidbounce",
                            "liquidbounce.net",
                            "@ccbluex/liquidbounce-script-api"
                    )
            )
    );

    private AntiCheat() {}

    public static void verifyEnvironment() {
        if (!ENABLED) return;
        DetectionResult result = scan();
        if (!result.detected) return;

        System.err.println("[PVPUtils] FATAL ERROR: Unsupported client environment detected.");
        System.err.println("[PVPUtils] Reason: Incompatible third-party cheat client were found.");
        System.err.println("[PVPUtils] Matched client: " + result.clientName);
        System.err.println("If you have to cheat at a game like this, you must be a total fucking loser in real life too.");

        throw new IllegalStateException("PVPUtils aborted startup due to incompatible client signatures: " + result.clientName);
    }

    private static DetectionResult scan() {
        FabricLoader loader = FabricLoader.getInstance();
        ClassLoader classLoader = AntiCheat.class.getClassLoader();
        List<ModContainer> mods = new ArrayList<>(loader.getAllMods());

        for (ClientSignature signature : SIGNATURES) {
            LinkedHashSet<String> evidence = new LinkedHashSet<>();

            for (ModContainer mod : mods) {
                String modId = mod.getMetadata().getId();
                String name = mod.getMetadata().getName();
                String loweredId = modId == null ? "" : modId.toLowerCase(Locale.ROOT);
                String loweredName = name == null ? "" : name.toLowerCase(Locale.ROOT);

                for (String candidate : signature.modIds) {
                    if (candidate.equals(loweredId) || loweredName.contains(candidate)) {
                        evidence.add("strong/mod-id=" + modId);
                    }
                }
            }

            for (String className : signature.classNames) {
                if (classExists(className, classLoader)) {
                    evidence.add("strong/class=" + className);
                }
            }

            for (String resource : signature.resources) {
                if (resourceExists(resource, classLoader) || modResourceExists(resource, mods)) {
                    evidence.add("medium/resource=" + resource);
                }
            }

            for (String marker : signature.markers) {
                if (markerExists(marker, mods, classLoader)) {
                    evidence.add("weak/marker=" + marker);
                }
            }

            if (shouldBlock(evidence)) {
                return new DetectionResult(true, signature.name, new ArrayList<>(evidence));
            }
        }

        return DetectionResult.none();
    }

    private static boolean shouldBlock(Set<String> evidence) {
        int strong = 0;
        int medium = 0;
        int weak = 0;

        for (String item : evidence) {
            if (item.startsWith("strong/")) strong++;
            else if (item.startsWith("medium/")) medium++;
            else if (item.startsWith("weak/")) weak++;
        }

        return strong >= 1 || medium >= 2 || (medium >= 1 && weak >= 1) || weak >= 3;
    }

    private static boolean classExists(String className, ClassLoader classLoader) {
        try {
            Class.forName(className, false, classLoader);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean resourceExists(String resource, ClassLoader classLoader) {
        String normalized = resource.startsWith("/") ? resource.substring(1) : resource;
        try (InputStream stream = classLoader.getResourceAsStream(normalized)) {
            return stream != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean modResourceExists(String resource, List<ModContainer> mods) {
        for (ModContainer mod : mods) {
            if (mod.findPath(resource).isPresent()) return true;
        }
        return false;
    }

    private static boolean markerExists(String marker, List<ModContainer> mods, ClassLoader classLoader) {
        String loweredMarker = marker.toLowerCase(Locale.ROOT);
        for (ModContainer mod : mods) {
            String id = mod.getMetadata().getId();
            String name = mod.getMetadata().getName();
            if (id != null && id.toLowerCase(Locale.ROOT).contains(loweredMarker)) return true;
            if (name != null && name.toLowerCase(Locale.ROOT).contains(loweredMarker)) return true;

            var contact = mod.getMetadata().getContact();
            for (String key : List.of("homepage", "issues", "sources", "discord", "email")) {
                var value = contact.get(key);
                if (value.isPresent() && value.get().toLowerCase(Locale.ROOT).contains(loweredMarker)) return true;
            }

            for (String author : mod.getMetadata().getAuthors().stream().map(person -> person.getName()).toList()) {
                if (author != null && author.toLowerCase(Locale.ROOT).contains(loweredMarker)) return true;
            }
        }

        String resourcePath = loweredMarker.replace('.', '/');
        return classLoader.getResource(resourcePath) != null;
    }

    private static final class ClientSignature {
        private final String name;
        private final List<String> modIds;
        private final List<String> classNames;
        private final List<String> resources;
        private final List<String> markers;

        private ClientSignature(String name, List<String> modIds, List<String> classNames, List<String> resources, List<String> markers) {
            this.name = name;
            this.modIds = modIds;
            this.classNames = classNames;
            this.resources = resources;
            this.markers = markers;
        }
    }

    private static final class DetectionResult {
        private final boolean detected;
        private final String clientName;
        private final List<String> evidence;

        private DetectionResult(boolean detected, String clientName, List<String> evidence) {
            this.detected = detected;
            this.clientName = clientName;
            this.evidence = evidence;
        }

        private static DetectionResult none() {
            return new DetectionResult(false, "", List.of());
        }
    }
}
