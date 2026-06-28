package com.pvp_utils.mixin.client;

import net.minecraft.client.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiMessage.class)
public interface ChatHudLineAccessor {
    @Accessor("addedTime")
    int getCreationTick();
}
