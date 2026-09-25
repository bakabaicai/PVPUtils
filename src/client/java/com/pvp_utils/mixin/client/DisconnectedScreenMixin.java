package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.pvp_utils.client.ReconnectionHelper;
import com.pvp_utils.client.modules.impl.Tool.ServerConnectionOverlay;
import com.pvp_utils.client.render.MainUI.PVPUtilsMultiplayerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {
    private Button pvp_utils$reconnectButton;

    protected DisconnectedScreenMixin(Component title) {
        super(title);
    }
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static Screen pvp_utils$returnToCustomMultiplayer(Screen parent) {
        if (parent instanceof JoinMultiplayerScreen) {
            return new PVPUtilsMultiplayerScreen(null);
        }
        return parent;
    }

    @Inject(method = "<init>(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;)V", at = @At("TAIL"))
    private void pvp_utils$logSimpleDisconnect(Screen parent, Component title, Component reason, CallbackInfo ci) {
        ServerConnectionOverlay.logFailure(title, reason);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;)V", at = @At("TAIL"))
    private void pvp_utils$logButtonDisconnect(Screen parent, Component title, Component reason, Component buttonText, CallbackInfo ci) {
        ServerConnectionOverlay.logFailure(title, reason);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/DisconnectionDetails;)V", at = @At("TAIL"))
    private void pvp_utils$logDetailedDisconnect(Screen parent, Component title, DisconnectionDetails details, CallbackInfo ci) {
        ServerConnectionOverlay.logFailure(title, details == null ? null : details.reason());
    }

    @Inject(method = "<init>(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/DisconnectionDetails;Lnet/minecraft/network/chat/Component;)V", at = @At("TAIL"))
    private void pvp_utils$logDetailedButtonDisconnect(Screen parent, Component title, DisconnectionDetails details, Component buttonText, CallbackInfo ci) {
        ServerConnectionOverlay.logFailure(title, details == null ? null : details.reason());
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void pvp_utils$addReconnectButton(CallbackInfo ci) {
        if (!ReconnectionHelper.hasLastServer()) {
            return;
        }
        if (pvp_utils$reconnectButton != null && this.children().contains(pvp_utils$reconnectButton)) {
            return;
        }

        Component reconnectText = Component.literal(Config.isChinese ? "重新连接" : "Reconnect");

        pvp_utils$reconnectButton = this.addRenderableWidget(Button.builder(reconnectText, button -> {
            if (ReconnectionHelper.hasLastServer() && this.minecraft != null) {
                ConnectScreen.startConnecting(
                    this,
                    this.minecraft,
                    ReconnectionHelper.getLastServerAddress(),
                    ReconnectionHelper.getLastServerData(),
                    false,
                    null
                );
            }
        }).bounds(0, 0, 200, 20).build());

        pvp_utils$positionReconnectButton();
    }

    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void pvp_utils$repositionReconnectButton(CallbackInfo ci) {
        pvp_utils$positionReconnectButton();
    }

    private void pvp_utils$positionReconnectButton() {
        if (pvp_utils$reconnectButton == null || !this.children().contains(pvp_utils$reconnectButton)) {
            return;
        }

        Button vanillaButton = null;
        for (GuiEventListener child : this.children()) {
            if (child instanceof Button button && button != pvp_utils$reconnectButton) {
                vanillaButton = button;
            }
        }

        if (vanillaButton != null) {
            pvp_utils$reconnectButton.setX(vanillaButton.getX());
            pvp_utils$reconnectButton.setY(vanillaButton.getY() + vanillaButton.getHeight() + 4);
        } else {
            pvp_utils$reconnectButton.setX(this.width / 2 - 100);
            pvp_utils$reconnectButton.setY(this.height - 28);
        }
    }
}
