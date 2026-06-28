package com.pvp_utils.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess")
public interface ChatComponentDrawingBackgroundGraphicsAccessor {
    @Accessor("graphics")
    GuiGraphics pvp_utils$getGraphics();
}
