package com.pvp_utils.client.modules.impl.Tool;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public final class TimeWeatherChanger {
    private static boolean wasWeatherChangeEnabled = false;
    private static boolean savedRaining = false;
    private static float savedRainLevel = 0.0f;
    private static float savedThunderLevel = 0.0f;

    private TimeWeatherChanger() {}

    public static void tick(Minecraft client) {
        ClientLevel level = client.level;
        if (level == null) {
            wasWeatherChangeEnabled = false;
            return;
        }

        if (Config.timeChange) {
            long time = Math.floorMod(Config.clientTime, 24000);
            level.setTimeFromServer(level.getGameTime(), time, false);
        }

        if (Config.weatherChange) {
            if (!wasWeatherChangeEnabled) {
                saveWeather(level);
                wasWeatherChangeEnabled = true;
            }
            applyWeather(level);
        } else if (wasWeatherChangeEnabled) {
            restoreWeather(level);
            wasWeatherChangeEnabled = false;
        }
    }

    private static void saveWeather(ClientLevel level) {
        savedRaining = level.getLevelData().isRaining();
        savedRainLevel = level.getRainLevel(1.0f);
        savedThunderLevel = level.getThunderLevel(1.0f);
    }

    private static void restoreWeather(ClientLevel level) {
        level.getLevelData().setRaining(savedRaining);
        level.setRainLevel(savedRainLevel);
        level.setThunderLevel(savedThunderLevel);
    }

    private static void applyWeather(ClientLevel level) {
        boolean rain = Config.weatherMode != Config.WeatherMode.CLEAR;
        boolean thunder = Config.weatherMode == Config.WeatherMode.THUNDER;

        level.getLevelData().setRaining(rain);
        level.setRainLevel(rain ? 1.0f : 0.0f);
        level.setThunderLevel(thunder ? 1.0f : 0.0f);
    }
}
