package com.pvp_utils.client.NeteaseMusic;

import com.pvp_utils.client.gui.clickgui.UiText;

public record Song(String image, String name, String artist, long id, long durationMs) {
    public String displayArtist() {
        return artist == null || artist.isBlank() ? UiText.t("未知歌手", "Unknown artist") : artist;
    }
}
