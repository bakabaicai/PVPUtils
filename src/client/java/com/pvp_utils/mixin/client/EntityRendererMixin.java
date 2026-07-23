package com.pvp_utils.mixin.client;

import com.pvp_utils.client.irc.IrcBridge;
import com.pvp_utils.client.modules.impl.Tool.NickHiderManager;
import com.pvp_utils.client.util.NameTagPlayerFilterState;
import com.pvp_utils.client.util.NameTagPlayerFilterContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void pvp_utils$captureNameTagPlayerFilter(Entity entity, EntityRenderState state, float tickProgress, CallbackInfo ci) {
        ((NameTagPlayerFilterState) state).pvp_utils$setNameTagRealPlayer(isRealPlayer(entity));
    }

    @Inject(method = "submitNameTag", at = @At("HEAD"))
    private void pvp_utils$beginNameTagPlayerFilter(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        NameTagPlayerFilterContext.setRealPlayer(((NameTagPlayerFilterState) state).pvp_utils$isNameTagRealPlayer());
    }

    @Inject(method = "submitNameTag", at = @At("RETURN"))
    private void pvp_utils$endNameTagPlayerFilter(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        NameTagPlayerFilterContext.clear();
    }

    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void pvp_utils$decorateIrcNameTag(Entity entity, CallbackInfoReturnable<Component> cir) {
        if (!(entity instanceof Player)) {
            return;
        }
        Component name = IrcBridge.decorateName(cir.getReturnValue(), entity.getUUID());
        cir.setReturnValue(NickHiderManager.replaceNameTag(name, entity));
    }

    private static boolean isRealPlayer(Entity entity) {
        if (!(entity instanceof Player)) return false;

        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) return true;

        PlayerInfo info = connection.getPlayerInfo(entity.getUUID());
        return info != null;
    }
}
