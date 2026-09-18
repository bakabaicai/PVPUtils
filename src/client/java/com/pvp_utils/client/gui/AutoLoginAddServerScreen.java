package com.pvp_utils.client.gui;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Tool.ServerAutoLoginManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

public class AutoLoginAddServerScreen extends Screen {
    private static final int[] DELAY_OPTIONS = {0, 1, 2, 3, 5, 10, 15, 30};

    private final Screen lastScreen;
    private EditBox addressBox;
    private EditBox passwordBox;
    private int delay = 1;
    private String error = "";

    public AutoLoginAddServerScreen(Screen lastScreen) {
        super(Component.literal(Config.isChinese ? "添加自动登录服务器" : "Add Auto Login Server"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        int boxWidth = Math.min(400, this.width - 40);
        int x = this.width / 2 - boxWidth / 2;

        this.addressBox = new EditBox(this.font, x, this.height / 2 - 64, boxWidth, 20, Component.literal("Address"));
        this.addressBox.setMaxLength(255);
        this.addressBox.setHint(Component.literal(Config.isChinese ? "服务器地址，例如 mc.example.com" : "Server address, e.g. mc.example.com"));
        this.addRenderableWidget(this.addressBox);

        this.passwordBox = new EditBox(this.font, x, this.height / 2 - 32, boxWidth, 20, Component.literal("Password"));
        this.passwordBox.setMaxLength(64);
        this.passwordBox.setHint(Component.literal(Config.isChinese ? "密码（可留空，之后可补填）" : "Password (optional, can be set later)"));
        this.passwordBox.addFormatter((text, cursor) -> FormattedCharSequence.forward("*".repeat(text.length()), Style.EMPTY));
        this.addRenderableWidget(this.passwordBox);

        this.addRenderableWidget(Button.builder(delayLabel(), button -> {
            this.delay = nextDelay(this.delay);
            button.setMessage(delayLabel());
        }).bounds(x, this.height / 2, boxWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(Config.isChinese ? "添加并返回" : "Add & Back"), button -> add())
                .bounds(x, this.height / 2 + 32, boxWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(Config.isChinese ? "返回" : "Back"), button -> this.onClose())
                .bounds(x, this.height / 2 + 64, boxWidth, 20).build());
    }

    private void add() {
        String address = this.addressBox == null ? "" : this.addressBox.getValue().trim();
        if (address.isEmpty()) {
            this.error = Config.isChinese ? "请输入服务器地址" : "Enter a server address";
            return;
        }
        String password = this.passwordBox == null ? "" : this.passwordBox.getValue();
        ServerAutoLoginManager.setRule(address, password, this.delay);
        if (lastScreen instanceof AutoLoginServersScreen listScreen) {
            this.minecraft.setScreen(new AutoLoginServersScreen(listScreen.parent()));
        } else {
            this.minecraft.setScreen(lastScreen);
        }
    }

    private int nextDelay(int current) {
        for (int option : DELAY_OPTIONS) {
            if (option > current) {
                return option;
            }
        }
        return DELAY_OPTIONS[0];
    }

    private Component delayLabel() {
        return Component.literal(Config.isChinese ? "登录延迟: " + delay + " 秒" : "Login delay: " + delay + "s");
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if ((event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER)
                && (this.addressBox != null && this.addressBox.isFocused()
                || this.passwordBox != null && this.passwordBox.isFocused())) {
            add();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 92, 0xFFFFFF);
        if (this.error != null && !this.error.isEmpty()) {
            graphics.drawCenteredString(this.font, this.error, this.width / 2, this.height / 2 + 58, 0xFFFF5555);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(lastScreen);
    }
}
