package com.pvp_utils.client.modules.impl.Tool;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public final class TimeWeatherChanger {
    private TimeWeatherChanger() {}

    public static void tick(Minecraft client) {
        ClientLevel level = client.level;
        if (level == null) return;

        if (Config.timeChange) {
            long time = Math.floorMod(Config.clientTime, 24000);
            level.setTimeFromServer(level.getGameTime(), time, false);
        }

        if (Config.weatherChange) {
            applyWeather(level);
        }
    }

    private static void applyWeather(ClientLevel level) {
        boolean rain = Config.weatherMode != Config.WeatherMode.CLEAR;
        boolean thunder = Config.weatherMode == Config.WeatherMode.THUNDER;

        level.getLevelData().setRaining(rain);
        level.setRainLevel(rain ? 1.0f : 0.0f);
        level.setThunderLevel(thunder ? 1.0f : 0.0f);
    }
}
