package com.pvp_utils.mixin.client;

import com.pvp_utils.client.util.NameTagTextCache;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NameTagFeatureRenderer.class)
public class NameTagFeatureRendererMixin {
    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V")
    )
    private void pvp_utils$drawCachedNameTag(Font font, Component component, float x, float y, int color,
                                           boolean shadow, Matrix4f pose, MultiBufferSource buffers,
                                           Font.DisplayMode mode, int background, int light) {
        NameTagTextCache.draw(font, component, x, y, color, shadow, pose, buffers, mode, background, light);
    }
}
