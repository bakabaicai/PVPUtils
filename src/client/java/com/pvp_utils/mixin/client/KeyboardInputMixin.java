package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.pvp_utils.client.AutoChestDepositManager;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin extends ClientInput {
    @Inject(method = "tick", at = @At("TAIL"))
    private void blockMovementWhileDepositing(CallbackInfo ci) {
        if (AutoChestDepositManager.shouldBlockMovementInput()) {
            this.keyPresses = Input.EMPTY;
            this.moveVector = Vec2.ZERO;
            return;
        }

        if (Config.autoSprint && this.keyPresses.forward() && !this.keyPresses.shift()) {
            this.keyPresses = new Input(
                    this.keyPresses.forward(),
                    this.keyPresses.backward(),
                    this.keyPresses.left(),
                    this.keyPresses.right(),
                    this.keyPresses.jump(),
                    this.keyPresses.shift(),
                    true
            );
        }
    }
}
