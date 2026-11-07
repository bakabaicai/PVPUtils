package com.pvp_utils.client.render.skia;

public interface SkijaOverlay extends SkijaScreen, AutoCloseable {
    default boolean isSkijaOverlayActive() {
        return true;
    }

    default boolean rendersScreenBehind() {
        return false;
    }

    @Override
    void close();
}
