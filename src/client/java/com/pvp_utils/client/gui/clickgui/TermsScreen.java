package com.pvp_utils.client.gui.clickgui;

import com.pvp_utils.Config;
import com.pvp_utils.client.TermsManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class TermsScreen extends Screen {
    private final Screen parent;
    private boolean openedRules;
    private int agreeX;
    private int rulesX;
    private int buttonY;
    private final int buttonW = 156;
    private final int buttonH = 24;

    public TermsScreen(Screen parent) {
        super(Component.literal(Config.isChinese ? "使用须知" : "Terms of Use"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.buttonY = this.height - 72;
        this.agreeX = this.width / 2 - buttonW - 8;
        this.rulesX = this.width / 2 + 8;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0xE6000000);
        int center = this.width / 2;
        graphics.drawCenteredString(this.font, this.title, center, 36, 0xFFFFFFFF);

        if (Config.isChinese) {
            drawCentered(graphics, "本模组包含可能被部分服务器限制的功能，请先阅读规则文件。", center, 82, 0xFFFFFFFF);
            drawCentered(graphics, "阅读后点击“我同意以上条款”即可打开设置界面。", center, 98, 0xFFFFFFFF);
        } else {
            drawCentered(graphics, "This mod includes features that may be restricted by some servers.", center, 82, 0xFFFFFFFF);
            drawCentered(graphics, "Read the rules file first, then click “I agree to the terms” to continue.", center, 98, 0xFFFFFFFF);
        }

        if (!openedRules) {
            drawCentered(graphics,
                    Config.isChinese ? "请先打开并阅读规则文件。" : "Open and read the rules file first.",
                    center, this.height - 106, 0xFFFFD166);
        }

        drawButton(graphics, agreeX, buttonY, buttonW, buttonH, mouseX, mouseY,
                0xFF3DBB58, Config.isChinese ? "我同意以上条款" : "I Agree to the Terms", !openedRules);
        drawButton(graphics, rulesX, buttonY, buttonW, buttonH, mouseX, mouseY,
                0xFF3A3A3A, Config.isChinese ? "打开规则文件" : "Open Rules File", false);
    }

    private void drawButton(GuiGraphics graphics, int x, int y, int w, int h,
                            int mouseX, int mouseY, int color, String text, boolean disabled) {
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        int fill = disabled ? 0xFF4A4A4A : color;
        if (hover && !disabled) {
            fill = brighten(fill);
        }
        graphics.fill(x, y, x + w, y + h, fill);
        graphics.drawCenteredString(this.font, text, x + w / 2, y + 7, 0xFFFFFFFF);
    }

    private int brighten(int color) {
        int a = color >>> 24;
        int r = Math.min(255, ((color >> 16) & 255) + 18);
        int g = Math.min(255, ((color >> 8) & 255) + 18);
        int b = Math.min(255, (color & 255) + 18);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void drawCentered(GuiGraphics graphics, String text, int x, int y, int color) {
        graphics.drawCenteredString(this.font, text, x, y, color);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (event.button() != 0) {
            return super.mouseClicked(event, consumed);
        }

        if (hit(rulesX, buttonY, buttonW, buttonH, event.x(), event.y())) {
            openedRules = true;
            TermsManager.open();
            return true;
        }

        if (hit(agreeX, buttonY, buttonW, buttonH, event.x(), event.y())) {
            if (!openedRules) {
                return true;
            }
            Config.termsRead = true;
            Config.fullMode = false;
            Config.save();
            if (this.minecraft != null) {
                this.minecraft.setScreen(new NewSettingsScreen(parent));
            }
            return true;
        }

        return super.mouseClicked(event, consumed);
    }

    private boolean hit(int x, int y, int w, int h, double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
