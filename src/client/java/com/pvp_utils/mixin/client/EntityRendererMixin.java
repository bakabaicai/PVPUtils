package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Tool.NickHiderManager;
import com.pvp_utils.client.util.NameTagPlayerFilterState;
import com.pvp_utils.client.util.NameTagPlayerFilterContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @Inject(method = "shouldRender(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/culling/Frustum;DDD)Z", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$entityRenderDistanceCull(Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.entityOptimize || entity instanceof Player) {
            return;
        }
        int dist = Config.entityRenderDistance;
        if (entity instanceof ItemEntity) {
            dist = Config.itemRenderDistance;
        } else if (entity instanceof ExperienceOrb) {
            dist = Config.xpOrbRenderDistance;
        } else if (entity instanceof AbstractArrow) {
            dist = Config.arrowRenderDistance;
        }
        if (dist > 0 && x * x + y * y + z * z > (double) dist * dist) {
            cir.setReturnValue(false);
        }
    }

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
    private void pvp_utils$replaceNameTag(Entity entity, CallbackInfoReturnable<Component> cir) {
        if (!(entity instanceof Player)) {
            return;
        }
        cir.setReturnValue(NickHiderManager.replaceNameTag(cir.getReturnValue(), entity));
    }

    private static boolean isRealPlayer(Entity entity) {
        if (!(entity instanceof Player)) return false;

        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) return true;

        PlayerInfo info = connection.getPlayerInfo(entity.getUUID());
        return info != null;
    }
}
