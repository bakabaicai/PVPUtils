package com.pvp_utils.client.gui.clickgui;

import com.pvp_utils.client.net.ChannelChatManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class ChannelChatScreen extends Screen {
    private EditBox channelField;
    private EditBox messageField;
    private String status = "";

    public ChannelChatScreen() {
        super(Component.literal("PVPUtils Channel Chat"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int w = Math.min(420, this.width - 60);
        this.channelField = new EditBox(this.font, cx - w / 2, this.height / 2 - 70, w, 20, Component.literal("频道"));
        this.channelField.setMaxLength(32);
        this.channelField.setValue("pvp");
        this.messageField = new EditBox(this.font, cx - w / 2, this.height / 2 - 30, w, 20, Component.literal("消息"));
        this.messageField.setMaxLength(256);
        this.addWidget(this.channelField);
        this.addWidget(this.messageField);
        this.setInitialFocus(this.messageField);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.fillGradient(0, 0, this.width, this.height, 0xCC101418, 0xCC101418);
        int cx = this.width / 2;
        int w = Math.min(420, this.width - 60);
        graphics.drawString(this.font, "PVPUtils 频道聊天", cx - w / 2, this.height / 2 - 100, 0xFFF2F4F8, false);
        graphics.drawString(this.font, "频道", cx - w / 2, this.height / 2 - 88, 0xFF9AA4B2, false);
        graphics.drawString(this.font, "消息", cx - w / 2, this.height / 2 - 48, 0xFF9AA4B2, false);
        graphics.drawString(this.font, "回车发送 · ESC 关闭", cx - w / 2, this.height / 2 + 24, 0xFF9AA4B2, false);
        if (!this.status.isEmpty()) {
            graphics.drawString(this.font, this.status, cx - w / 2, this.height / 2 + 40, 0xFF6D8CFF, false);
        }
        this.channelField.render(graphics, mouseX, mouseY, partialTick);
        this.messageField.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.channelField.keyPressed(event) || this.messageField.keyPressed(event)) {
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            this.sendMessage();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.channelField.charTyped(event) || this.messageField.charTyped(event)) {
            return true;
        }
        return super.charTyped(event);
    }

    private void sendMessage() {
        String channel = this.channelField.getValue();
        String message = this.messageField.getValue();
        if (message.trim().isEmpty()) {
            this.status = "消息不能为空";
            return;
        }
        if (ChannelChatManager.send(channel, message)) {
            this.messageField.setValue("");
            this.status = "已发送到频道 " + channel + (Minecraft.getInstance().getConnection() == null ? " (未连接服务器)" : "");
        } else {
            this.status = "发送失败：未连接服务器或频道为空";
        }
    }
}
