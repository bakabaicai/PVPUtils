package com.pvp_utils.client.modules.impl.Tool;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public final class TimeWeatherChanger {
    private TimeWeatherChanger() {}

    public static void tick(Minecraft client) {
        ClientLevel level = client.level;
        if (level == null) {
            return;
        }

        if (Config.timeChange) {
            long time = Math.floorMod(Config.clientTime, 24000);
            level.setTimeFromServer(level.getGameTime(), time, false);
        }
    }

    public static boolean isRaining() {
        return Config.weatherMode != Config.WeatherMode.CLEAR;
    }

    public static boolean isSnowing() {
        return Config.weatherMode == Config.WeatherMode.SNOW;
    }

    public static boolean isThundering() {
        return Config.weatherMode == Config.WeatherMode.THUNDER;
    }
}
