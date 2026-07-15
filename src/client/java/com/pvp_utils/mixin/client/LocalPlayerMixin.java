package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Render.ItemUseStatusRenderer;
import com.pvp_utils.client.modules.impl.Tool.AutoChestDepositManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    @Shadow @Final protected Minecraft minecraft;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void handleUseSwingInput(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        AutoChestDepositManager.applyRotationLock(player);
        if (Config.useSwing && player.isUsingItem()) {
            if (this.minecraft.options.keyAttack.isDown()) {
                if (player.swingTime <= 0) {
                    player.swing(InteractionHand.MAIN_HAND);
                }
            }
        }
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void pvp_utils$captureItemUseStatus(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        ItemUseStatusRenderer.getInstance().captureFromMixin(player);
        if (Config.noSwimming && player.isInWater() && player.isSprinting()) {
            player.setSprinting(false);
        }
    }
}
