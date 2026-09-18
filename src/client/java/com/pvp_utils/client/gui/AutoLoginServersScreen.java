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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AutoLoginServersScreen extends Screen {
    private static final int[] DELAY_OPTIONS = {0, 1, 2, 3, 5, 10, 15, 30};
    private static final int ROW_HEIGHT = 52;

    private final Screen lastScreen;
    private final List<Map.Entry<String, ServerAutoLoginManager.Rule>> servers = new ArrayList<>();
    private final List<PasswordRow> passwordRows = new ArrayList<>();
    private int renderedRows;

    public AutoLoginServersScreen(Screen lastScreen) {
        super(Component.literal(Config.isChinese ? "自动登录服务器" : "Auto Login Servers"));
        this.lastScreen = lastScreen;
        for (Map.Entry<String, ServerAutoLoginManager.Rule> entry : ServerAutoLoginManager.rules().entrySet()) {
            if (entry.getValue().enabled) {
                servers.add(entry);
            }
        }
    }

    private static final class PasswordRow {
        private final String address;
        private final EditBox box;
        private boolean focused;
        private boolean suppressResponder;
        private boolean dirty;
        private String pending = "";

        private PasswordRow(String address, EditBox box) {
            this.address = address;
            this.box = box;
        }
    }

    @Override
    protected void init() {
        int listWidth = Math.min(440, this.width - 40);
        int listX = this.width / 2 - listWidth / 2;
        int maxRows = Math.max(1, (this.height - 110) / ROW_HEIGHT);
        int y = 44;
        int rows = 0;
        for (int i = 0; i < servers.size() && rows < maxRows; i++) {
            String address = servers.get(i).getKey();
            int delay = Math.max(0, servers.get(i).getValue().delay);

            this.addRenderableWidget(Button.builder(Component.literal(Config.isChinese ? "移除" : "Remove"), button -> {
                ServerAutoLoginManager.removeRule(address);
                this.minecraft.setScreen(new AutoLoginServersScreen(lastScreen));
            }).bounds(listX + listWidth - 80, y, 80, 20).build());

            boolean delayEnabled = Config.serverAutoLoginMode == 0;
            EditBox box = new EditBox(this.font, listX, y + 24, listWidth - 130, 20, Component.literal("Password"));
            box.setMaxLength(64);
            box.setHint(Component.literal(Config.isChinese ? "未设置密码，点击输入" : "No password, click to type"));
            box.addFormatter((text, cursor) -> FormattedCharSequence.forward("*".repeat(text.length()), Style.EMPTY));
            box.setValue(ServerAutoLoginManager.hasPassword(address) ? "********" : "");
            PasswordRow row = new PasswordRow(address, box);
            box.setResponder(text -> {
                if (row.suppressResponder) {
                    return;
                }
                row.pending = text;
                row.dirty = true;
            });
            this.addRenderableWidget(box);
            passwordRows.add(row);

            Button delayButton = Button.builder(delayLabel(delay), button -> {
                int next = nextDelay(ServerAutoLoginManager.delayOf(address));
                ServerAutoLoginManager.setDelay(address, next);
                button.setMessage(delayLabel(next));
            }).bounds(listX + listWidth - 120, y + 24, 120, 20).build();
            delayButton.active = delayEnabled;
            this.addRenderableWidget(delayButton);

            y += ROW_HEIGHT;
            rows++;
        }
        renderedRows = rows;

        this.addRenderableWidget(Button.builder(Component.literal(Config.isChinese ? "添加服务器" : "Add Server"), button ->
                this.minecraft.setScreen(new AutoLoginAddServerScreen(this)))
                .bounds(this.width / 2 - 100, this.height - 56, 200, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(Config.isChinese ? "返回" : "Back"), button -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 28, 200, 20).build());
    }

    @Override
    public void tick() {
        super.tick();
        for (PasswordRow row : passwordRows) {
            boolean focused = row.box.isFocused();
            if (focused && !row.focused) {
                row.suppressResponder = true;
                row.box.setValue("");
                row.suppressResponder = false;
                row.dirty = false;
                row.pending = "";
            } else if (!focused && row.focused) {
                commit(row);
            }
            row.focused = focused;
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if ((event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER)) {
            for (PasswordRow row : passwordRows) {
                if (row.box.isFocused()) {
                    row.box.setFocused(false);
                    commit(row);
                    return true;
                }
            }
        }
        return super.keyPressed(event);
    }

    private void commit(PasswordRow row) {
        if (!row.dirty) {
            return;
        }
        ServerAutoLoginManager.setPassword(row.address, row.pending);
        row.dirty = false;
        row.pending = "";
        row.suppressResponder = true;
        row.box.setValue(ServerAutoLoginManager.hasPassword(row.address) ? "********" : "");
        row.suppressResponder = false;
    }

    private static int nextDelay(int current) {
        for (int option : DELAY_OPTIONS) {
            if (option > current) {
                return option;
            }
        }
        return DELAY_OPTIONS[0];
    }

    private static Component delayLabel(int seconds) {
        return Component.literal((Config.isChinese ? "延迟: " + seconds + " 秒" : "Delay: " + seconds + "s"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);

        int listWidth = Math.min(440, this.width - 40);
        int listX = this.width / 2 - listWidth / 2;
        int y = 44;
        for (int i = 0; i < renderedRows && i < servers.size(); i++) {
            String address = servers.get(i).getKey();
            String line = this.font.plainSubstrByWidth(address, listWidth - 90);
            graphics.drawString(this.font, line, listX + 2, y + 6, 0xFFB8B8B8);
            y += ROW_HEIGHT;
        }

        if (servers.isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Config.isChinese ? "暂无已配置的服务器，点击下方「添加服务器」或用 .autologin <密码>" : "No servers configured; click \"Add Server\" below or use .autologin <password>",
                    this.width / 2, this.height / 2 - 20, 0xFF909090);
        }
    }

    @Override
    public void onClose() {
        for (PasswordRow row : passwordRows) {
            commit(row);
        }
        this.minecraft.setScreen(lastScreen);
    }

    Screen parent() {
        return lastScreen;
    }
}
