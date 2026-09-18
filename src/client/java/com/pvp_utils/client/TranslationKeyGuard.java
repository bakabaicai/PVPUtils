package com.pvp_utils.client;

import com.google.gson.JsonParser;
import com.pvp_utils.PVPUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public final class TranslationKeyGuard {
    private static final Set<String> VANILLA_TRANSLATION_KEYS = loadVanillaTranslationKeys();
    private static final Map<String, Set<String>> SERVER_PACK_TRANSLATION_KEYS = new HashMap<>();

    private TranslationKeyGuard() {
    }

    public static String getSafeText(Component component) {
        return sanitize(component, new IdentityHashMap<>()).getString();
    }

    private static Component sanitize(Component component, IdentityHashMap<Component, Component> sanitizedComponents) {
        Component cached = sanitizedComponents.get(component);
        if (cached != null) {
            return cached;
        }

        ComponentContents contents = component.getContents();
        MutableComponent sanitized;
        if (contents instanceof TranslatableContents translation) {
            if (isAllowedTranslationKey(translation.getKey())) {
                Object[] arguments = translation.getArgs().clone();
                for (int index = 0; index < arguments.length; index++) {
                    if (arguments[index] instanceof Component argument) {
                        arguments[index] = sanitize(argument, sanitizedComponents);
                    }
                }
                sanitized = MutableComponent.create(new TranslatableContents(translation.getKey(), translation.getFallback(), arguments));
            } else {
                sanitized = Component.literal(translation.getFallback() != null ? translation.getFallback() : translation.getKey());
            }
        } else {
            sanitized = MutableComponent.create(contents);
        }

        sanitized.setStyle(component.getStyle());
        sanitizedComponents.put(component, sanitized);
        for (Component sibling : component.getSiblings()) {
            sanitized.append(sanitize(sibling, sanitizedComponents));
        }
        return sanitized;
    }

    private static Set<String> loadVanillaTranslationKeys() {
        IoSupplier<InputStream> resource = Minecraft.getInstance().getVanillaPackResources().getResource(
                PackType.CLIENT_RESOURCES,
                Identifier.withDefaultNamespace("lang/en_us.json")
        );
        if (resource == null) {
            PVPUtils.LOGGER.warn("Could not find the vanilla English language file");
            return Set.of();
        }

        try (Reader reader = new InputStreamReader(resource.get(), StandardCharsets.UTF_8)) {
            return Set.copyOf(JsonParser.parseReader(reader).getAsJsonObject().keySet());
        } catch (IOException exception) {
            PVPUtils.LOGGER.warn("Could not load vanilla translation keys", exception);
            return Set.of();
        }
    }

    private static synchronized boolean isAllowedTranslationKey(String key) {
        if (VANILLA_TRANSLATION_KEYS.contains(key)) {
            return true;
        }

        Set<String> activePackIds = new HashSet<>();
        Set<String> translationKeys = new HashSet<>();
        Minecraft.getInstance().getResourceManager().listPacks().forEach(pack -> {
            if (pack.location().source() != PackSource.SERVER) {
                return;
            }
            activePackIds.add(pack.packId());
            translationKeys.addAll(SERVER_PACK_TRANSLATION_KEYS.computeIfAbsent(pack.packId(), ignored -> loadTranslationKeys(pack)));
        });
        SERVER_PACK_TRANSLATION_KEYS.keySet().retainAll(activePackIds);
        return translationKeys.contains(key);
    }

    private static Set<String> loadTranslationKeys(PackResources pack) {
        Set<String> keys = new HashSet<>();
        for (String namespace : pack.getNamespaces(PackType.CLIENT_RESOURCES)) {
            pack.listResources(PackType.CLIENT_RESOURCES, namespace, "lang", (id, resource) -> {
                if (!id.getPath().endsWith(".json")) {
                    return;
                }
                try (Reader reader = new InputStreamReader(resource.get(), StandardCharsets.UTF_8)) {
                    keys.addAll(JsonParser.parseReader(reader).getAsJsonObject().keySet());
                } catch (IOException | IllegalStateException exception) {
                    PVPUtils.LOGGER.warn("Could not load translation keys from server resource pack {}", pack.packId(), exception);
                }
            });
        }
        return Set.copyOf(keys);
    }

}
