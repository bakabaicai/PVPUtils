package com.pvp_utils.mixin.client;

import com.mojang.brigadier.Message;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ChatComponent.class)
public interface ChatHudAccessor {
    @Accessor("trimmedMessages")
    List<Message> getVisibleMessages();
}
