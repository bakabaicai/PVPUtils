package com.pvp_utils.client.NeteaseMusic;

public record Playlist(long id, String name, String coverUrl, long playCount, int trackCount, String creator) {
    public Playlist(long id, String name, String coverUrl, long playCount) {
        this(id, name, coverUrl, playCount, 0, "");
    }
}
