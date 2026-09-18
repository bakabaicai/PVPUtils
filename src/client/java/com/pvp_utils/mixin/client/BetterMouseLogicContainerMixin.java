package com.pvp_utils.mixin.client;

import com.pvp_utils.client.modules.impl.Optimize.BetterMouseLogicManager;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class BetterMouseLogicContainerMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$betterMouseClicked(MouseButtonEvent event, boolean consumed, CallbackInfoReturnable<Boolean> cir) {
        if (BetterMouseLogicManager.onMouseClicked((AbstractContainerScreen<?>) (Object) this, event.x(), event.y(), event.button())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$betterMouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (BetterMouseLogicManager.onMouseReleased((AbstractContainerScreen<?>) (Object) this, event.button())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$betterMouseDragged(MouseButtonEvent event, double dragX, double dragY, CallbackInfoReturnable<Boolean> cir) {
        if (BetterMouseLogicManager.onMouseDragged((AbstractContainerScreen<?>) (Object) this, event.x(), event.y(), event.button())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("RETURN"), cancellable = true)
    private void pvp_utils$betterMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && BetterMouseLogicManager.onMouseScrolled((AbstractContainerScreen<?>) (Object) this, mouseX, mouseY, verticalAmount)) {
            cir.setReturnValue(true);
        }
    }
}
