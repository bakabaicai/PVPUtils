package com.pvp_utils.mixin.client;

import com.pvp_utils.client.modules.impl.Tool.BlockCountDisplayRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemMixin {
    @Inject(method = "useOn", at = @At("RETURN"))
    private void recordBlockCountPlacement(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        if (player == Minecraft.getInstance().player
                && context.getHand() == InteractionHand.MAIN_HAND
                && cir.getReturnValue().consumesAction()) {
            BlockCountDisplayRenderer.getInstance().recordPlacement(Minecraft.getInstance());
        }
    }
}
