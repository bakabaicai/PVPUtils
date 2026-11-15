package com.pvp_utils.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pvp_utils.Config;
import com.pvp_utils.client.util.NameTagPlayerFilterContext;
import com.pvp_utils.client.util.NameTagTextCache;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.feature.NameTagFeatureRenderer$Storage")
public class NameTagFeatureRendererStorageMixin {
    @Unique private float pvp_utils$nameTagScale = 1.0f;

    @Redirect(
            method = "add",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;width(Lnet/minecraft/network/chat/FormattedText;)I")
    )
    private int pvp_utils$cachedNameTagWidth(Font font, FormattedText text) {
        return text instanceof Component component ? NameTagTextCache.width(font, component) : font.width(text);
    }

    @Inject(method = "add", at = @At("HEAD"))
    private void pvp_utils$prepareNameTagScale(PoseStack poseStack, Vec3 attachment, int yOffset, Component component, boolean seeThrough, int light, double distanceToCameraSq, CameraRenderState cameraRenderState, CallbackInfo ci) {
        pvp_utils$nameTagScale = 1.0f;
        if (!Config.nameTag) return;
        if (Config.nameTagOnlyPlayer && !NameTagPlayerFilterContext.isRealPlayer()) return;
        pvp_utils$nameTagScale = clamp(Config.nameTagScale, 0.5f, 3.0f);
        if (Config.nameTagDynamicScale) {
            pvp_utils$nameTagScale *= clamp((float) (Math.sqrt(Math.max(0.0, distanceToCameraSq)) / 8.0), 0.5f, 8.0f);
        }
    }

    @ModifyArg(
            method = "add",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"),
            index = 0
    )
    private float pvp_utils$modifyNameTagScaleX(float scale) {
        return scale * pvp_utils$nameTagScale;
    }

    @ModifyArg(
            method = "add",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"),
            index = 1
    )
    private float pvp_utils$modifyNameTagScaleY(float scale) {
        return scale * pvp_utils$nameTagScale;
    }

    @ModifyArg(
            method = "add",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"),
            index = 2
    )
    private float pvp_utils$modifyNameTagScaleZ(float scale) {
        return scale * pvp_utils$nameTagScale;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
