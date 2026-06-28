package com.pvp_utils.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChatComponent.class)
public interface ChatComponentAccessor {
    @Accessor("minecraft")
    Minecraft pvp_utils$getMinecraft();

    @Invoker("getWidth")
    int pvp_utils$getChatWidth();
}
