package com.pvp_utils.client.gui.clickgui;

import com.pvp_utils.Config;
import com.pvp_utils.client.TermsManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class TermsScreen extends Screen {
    private final Screen parent;
    private boolean openedTerms;

    private int restrictedX;
    private int fullX;
    private int readX;
    private int buttonY;
    private final int buttonW = 128;
    private final int buttonH = 24;

    public TermsScreen(Screen parent) {
        super(Component.literal(Config.isChinese ? "使用须知" : "Terms of Use"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.buttonY = this.height - 78;
        this.restrictedX = this.width / 2 - buttonW - 8;
        this.fullX = this.width / 2 + 8;
        this.readX = this.width / 2 - buttonW / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0xE6000000);
        int x = this.width / 2;
        int y = 38;
        graphics.drawCenteredString(this.font, this.title, x, y, 0xFFFFFFFF);
        y += 34;
        drawCentered(graphics, "本模组内置了部分争议性功能，虽然他们都只是为了辅助PVP而生，并非作弊功能，", x, y, 0xFFFFFFFF);
        drawCentered(graphics, "并且也对其进行了平衡性调整，但是部分服务器仍有可能将部分功能视为违规功能并处以封禁处理，", x, y + 14, 0xFFFFFFFF);
        drawCentered(graphics, "所以您随时可以选择完整版或者受限版本（后续可以在“其他”分页内重新调整，若选择完整版，则您视为同意此协议）", x, y + 28, 0xFFFFFFFF);
        y += 64;
        drawCentered(graphics, "This mod includes some controversial features. They are meant to assist PvP rather than function as cheats,", x, y, 0xFFFFFFFF);
        drawCentered(graphics, "and they have been balanced as much as possible. However, some servers may still treat certain features as violations and punish players.", x, y + 14, 0xFFFFFFFF);
        drawCentered(graphics, "You can choose the full or restricted version at any time in Misc Settings. Choosing the full version means you agree to this notice.", x, y + 28, 0xFFFFFFFF);
        if (!openedTerms) {
            drawCentered(graphics, Config.isChinese ? "请先阅读协议，否则无法打开 ClickGUI。" : "Please read the terms first, or ClickGUI cannot be opened.", x, this.height - 112, 0xFFFFD166);
        }
        drawButton(graphics, restrictedX, buttonY, buttonW, buttonH, mouseX, mouseY, 0xFFD64040, Config.isChinese ? "受限版本" : "Restricted", !openedTerms);
        drawButton(graphics, fullX, buttonY, buttonW, buttonH, mouseX, mouseY, 0xFF3DBB58, Config.isChinese ? "完整版本" : "Full", !openedTerms);
        drawButton(graphics, readX, buttonY + 34, buttonW, buttonH, mouseX, mouseY, 0xFF3A3A3A, Config.isChinese ? "阅读协议" : "Read Terms", false);
    }

    private void drawButton(GuiGraphics graphics, int x, int y, int w, int h, int mouseX, int mouseY, int color, String text, boolean disabled) {
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        int fill = disabled ? 0xFF4A4A4A : color;
        if (hover && !disabled) fill = brighten(fill);
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
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button != 0) return super.mouseClicked(event, consumed);
        if (hit(restrictedX, buttonY, buttonW, buttonH, mouseX, mouseY)) {
            if (!openedTerms) return true;
            Config.termsRead = true;
            Config.fullMode = false;
            Config.save();
            if (this.minecraft != null) this.minecraft.setScreen(new NewSettingsScreen(parent));
            return true;
        }
        if (hit(fullX, buttonY, buttonW, buttonH, mouseX, mouseY)) {
            if (!openedTerms) return true;
            Config.termsRead = true;
            Config.fullMode = true;
            Config.save();
            if (this.minecraft != null) this.minecraft.setScreen(new NewSettingsScreen(parent));
            return true;
        }
        if (hit(readX, buttonY + 34, buttonW, buttonH, mouseX, mouseY)) {
            openedTerms = true;
            TermsManager.open();
            return true;
        }
        return super.mouseClicked(event, consumed);
    }

    private boolean hit(int x, int y, int w, int h, double mx, double my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
