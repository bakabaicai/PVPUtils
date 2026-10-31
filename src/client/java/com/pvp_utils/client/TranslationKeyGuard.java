package com.pvp_utils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.packs.repository.PackSource;
import com.pvp_utils.mixin.client.ClientLanguageAccessor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public final class TranslationKeyGuard {
    private static final ThreadLocal<Boolean> FROM_PACKET = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final Set<String> VANILLA_TRANSLATION_KEYS = new HashSet<>();
    private static final Map<String, String> SERVER_TRANSLATIONS = new HashMap<>();
    private static Language serverLanguage;

    private TranslationKeyGuard() {
    }

    public static String getSafeText(Component component) {
        return sanitize(component, new IdentityHashMap<>()).getString();
    }

    public static void setFromPacket(boolean fromPacket) {
        FROM_PACKET.set(fromPacket);
    }

    public static boolean isFromPacket() {
        return FROM_PACKET.get();
    }

    public static Component markFromNetwork(Component component) {
        MutableComponent copy = MutableComponent.create(markContents(component.getContents()));
        copy.setStyle(component.getStyle());
        for (Component sibling : component.getSiblings()) {
            copy.append(markFromNetwork(sibling));
        }
        return copy;
    }

    private static ComponentContents markContents(ComponentContents contents) {
        if (contents instanceof TranslationKeyMarker marker) {
            marker.pvp_utils$markTranslationKey();
        }
        return contents;
    }

    private static Component sanitize(Component component, IdentityHashMap<Component, Component> sanitizedComponents) {
        Component cached = sanitizedComponents.get(component);
        if (cached != null) {
            return cached;
        }

        ComponentContents contents = component.getContents();
        MutableComponent sanitized;
        if (contents instanceof TranslatableContents translation) {
            Object[] arguments = translation.getArgs().clone();
            for (int index = 0; index < arguments.length; index++) {
                if (arguments[index] instanceof Component argument) {
                    arguments[index] = sanitize(argument, sanitizedComponents);
                }
            }
            sanitized = MutableComponent.create(markContents(new TranslatableContents(translation.getKey(), translation.getFallback(), arguments)));
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

    public static String getTranslation(String key, String fallback) {
        return getServerLanguage().getOrDefault(key, fallback);
    }

    public static synchronized void beginLanguageLoad() {
        VANILLA_TRANSLATION_KEYS.clear();
        SERVER_TRANSLATIONS.clear();
        serverLanguage = null;
    }

    public static synchronized void recordTranslation(String key, String value, boolean vanilla, PackSource source) {
        if (source == PackSource.SERVER) {
            SERVER_TRANSLATIONS.put(key, value);
        } else if (vanilla) {
            VANILLA_TRANSLATION_KEYS.add(key);
        }
        serverLanguage = null;
    }

    public static synchronized boolean isAllowedTranslationKey(String key) {
        return VANILLA_TRANSLATION_KEYS.contains(key) || SERVER_TRANSLATIONS.containsKey(key);
    }

    private static synchronized Language getServerLanguage() {
        if (serverLanguage == null) {
            serverLanguage = ClientLanguageAccessor.pvp_utils$create(Map.copyOf(SERVER_TRANSLATIONS), Language.getInstance().isDefaultRightToLeft());
        }
        return serverLanguage;
    }
}
