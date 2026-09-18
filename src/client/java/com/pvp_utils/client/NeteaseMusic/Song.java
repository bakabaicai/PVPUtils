package com.pvp_utils.client.NeteaseMusic;

public record Song(String image, String name, String artist, long id, long durationMs) {
    public String displayArtist() {
        return artist == null || artist.isBlank() ? "Unknown artist" : artist;
    }
}
