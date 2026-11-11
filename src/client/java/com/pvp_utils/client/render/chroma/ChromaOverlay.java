package com.pvp_utils.client.render.chroma;

public interface ChromaOverlay extends ChromaScreen, AutoCloseable {
    default boolean isChromaOverlayActive() {
        return true;
    }

    default boolean rendersScreenBehind() {
        return false;
    }

    @Override
    void close();
}
