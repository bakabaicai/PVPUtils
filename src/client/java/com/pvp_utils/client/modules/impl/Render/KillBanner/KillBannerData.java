package com.pvp_utils.client.modules.impl.Render.KillBanner;

public record KillBannerData(String title, String killer, boolean mvp) {
    public KillBannerData {
        title = title == null || title.isBlank() ? "ROUND WIN" : title;
        killer = killer == null || killer.isBlank() ? "Player" : killer;
    }
}
