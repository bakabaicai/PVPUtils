package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.MultiplayerCompatibilityScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin extends Screen {
    protected JoinMultiplayerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void pvp_utils$addCompatibilitySettingsButton(CallbackInfo ci) {
        int x = 5;
        int y = 6;
        this.addRenderableWidget(Button.builder(Component.literal(Config.isChinese ? "联机设置" : "Multiplayer Settings"), button ->
                this.minecraft.setScreen(new MultiplayerCompatibilityScreen(this)))
                .bounds(x, y, 105, 20).build());
    }
}
