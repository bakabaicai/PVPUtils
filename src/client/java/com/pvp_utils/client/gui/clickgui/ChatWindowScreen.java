package com.pvp_utils.client.gui.clickgui;

import com.mojang.brigadier.Message;
import com.pvp_utils.mixin.client.ChatHudAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ChatWindowScreen extends Screen {
    private static final int MAX_VISIBLE = 18;
    private int scrollOffset = 0;

    public ChatWindowScreen() {
        super(Component.literal("Chat Window"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.fillGradient(0, 0, this.width, this.height, 0xCC101418, 0xCC101418);
        List<Message> messages = currentMessages();
        int total = messages.size();
        if (scrollOffset < 0) {
            scrollOffset = 0;
        }
        int maxScroll = Math.max(0, total - MAX_VISIBLE);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }
        int start = Math.max(0, total - MAX_VISIBLE - scrollOffset);
        int y = 30;
        for (int i = start; i < total; i++) {
            String line = messages.get(i).getString();
            graphics.drawString(this.font, line, 20, y, 0xFFF2F4F8, false);
            y += 13;
            if (y > this.height - 40) {
                break;
            }
        }
        graphics.drawString(this.font, "滚轮滚动 · ESC 关闭 · 最近 " + total + " 条", 20, this.height - 24, 0xFF9AA4B2, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset += (int) Math.round(verticalAmount);
        return true;
    }

    private List<Message> currentMessages() {
        Minecraft client = Minecraft.getInstance();
        if (client.gui == null) {
            return List.of();
        }
        return ((ChatHudAccessor) client.gui.getChat()).getVisibleMessages();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
