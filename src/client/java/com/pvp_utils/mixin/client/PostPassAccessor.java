package com.pvp_utils.mixin.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(PostPass.class)
public interface PostPassAccessor {
    @Accessor Map<String, GpuBuffer> getCustomUniforms();
    @Accessor List<PostPass.Input> getInputs();
}
