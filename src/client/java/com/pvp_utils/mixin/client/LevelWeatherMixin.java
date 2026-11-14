package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Tool.TimeWeatherChanger;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class LevelWeatherMixin {
    @Inject(method = "isRaining", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$overrideRaining(CallbackInfoReturnable<Boolean> cir) {
        if (Config.weatherChange) {
            cir.setReturnValue(TimeWeatherChanger.isRaining());
        }
    }

    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$overrideRainLevel(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (Config.weatherChange) {
            cir.setReturnValue(TimeWeatherChanger.isRaining() ? 1.0f : 0.0f);
        }
    }

    @Inject(method = "isThundering", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$overrideThundering(CallbackInfoReturnable<Boolean> cir) {
        if (Config.weatherChange) {
            cir.setReturnValue(TimeWeatherChanger.isThundering());
        }
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$overrideThunderLevel(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (Config.weatherChange) {
            cir.setReturnValue(TimeWeatherChanger.isThundering() ? 1.0f : 0.0f);
        }
    }
}
