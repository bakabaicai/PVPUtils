package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Tool.TimeWeatherChanger;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Biome.class)
public class BiomeWeatherMixin {
    @Inject(method = "getPrecipitationAt", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$overridePrecipitation(BlockPos pos, int seaLevel, CallbackInfoReturnable<Biome.Precipitation> cir) {
        if (!Config.weatherChange) {
            return;
        }
        if (TimeWeatherChanger.isSnowing()) {
            cir.setReturnValue(Biome.Precipitation.SNOW);
        } else if (TimeWeatherChanger.isRaining()) {
            cir.setReturnValue(Biome.Precipitation.RAIN);
        }
    }
}
