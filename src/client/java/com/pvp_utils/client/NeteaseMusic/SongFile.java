package com.pvp_utils.client.NeteaseMusic;

public record SongFile(String url, long size) {
    public boolean isPlayable() {
        return url != null && !url.isBlank();
    }
}
