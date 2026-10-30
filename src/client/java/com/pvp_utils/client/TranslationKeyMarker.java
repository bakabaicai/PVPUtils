package com.pvp_utils.client;

/**
 * Marker applied to component contents that were decoded from a server packet.
 * Only marked contents are filtered, so components created locally by the client
 * (or by other client-side mods) keep their normal translation behaviour.
 */
public interface TranslationKeyMarker {
    void pvp_utils$markTranslationKey();

    boolean pvp_utils$isTranslationKeyMarked();
}
