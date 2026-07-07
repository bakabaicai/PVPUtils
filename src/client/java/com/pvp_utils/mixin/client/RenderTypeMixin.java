package com.pvp_utils.mixin.client;

import com.mojang.blaze3d.textures.GpuTextureView;
import com.pvp_utils.client.modules.impl.Render.CustomEnchantmentGlint;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(RenderType.class)
public class RenderTypeMixin {
    @Shadow @Final protected String name;

    @ModifyArgs(
            method = "draw(Lcom/mojang/blaze3d/vertex/MeshData;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderPass;bindTexture(Ljava/lang/String;Lcom/mojang/blaze3d/textures/GpuTextureView;Lcom/mojang/blaze3d/textures/GpuSampler;)V"
            )
    )
    private void pvp_utils$replaceGlintTexture(Args args) {
        if (!"Sampler0".equals(args.get(0))) {
            return;
        }
        GpuTextureView textureView = CustomEnchantmentGlint.textureViewFor(name);
        if (textureView != null) {
            args.set(1, textureView);
        }
    }
}
