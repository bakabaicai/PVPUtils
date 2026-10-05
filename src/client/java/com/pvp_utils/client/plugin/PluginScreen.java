package com.pvp_utils.client.plugin;

import com.pvp_utils.client.render.skia.SkijaScreen;
import io.github.humbleui.skija.Canvas;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public final class PluginScreen extends Screen implements SkijaScreen {
    private final String plugin;
    private final String id;
    private final Screen parent;
    private boolean removed;

    public PluginScreen(String plugin, String id, String title, Screen parent) {
        super(Component.literal(title));
        this.plugin = plugin;
        this.id = id;
        this.parent = parent;
    }

    public String plugin() { return plugin; }
    public Screen parent() { return parent; }

    @Override
    public void added() {
        removed = false;
        PluginManager.INSTANCE.screenEvent(plugin, id, "onOpen");
    }

    @Override
    public void removed() {
        if (removed) return;
        removed = true;
        PluginManager.INSTANCE.screenEvent(plugin, id, "onClose");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void renderSkija(Canvas canvas) {
        if (!removed && minecraft.screen == this) PluginManager.INSTANCE.renderScreen(plugin, id, canvas, width, height);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return PluginManager.INSTANCE.screenClick(plugin, id, event.x(), event.y(), event.button())
                || PluginManager.INSTANCE.screenInput(plugin, id, "onMouseDown", event.x(), event.y(), event.button())
                || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return PluginManager.INSTANCE.screenInput(plugin, id, "onMouseUp", event.x(), event.y(), event.button())
                || super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return PluginManager.INSTANCE.screenInput(plugin, id, "onDrag", event.x(), event.y(), event.button(), dx, dy)
                || super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        return PluginManager.INSTANCE.screenInput(plugin, id, "onScroll", x, y, horizontal, vertical)
                || super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) return super.keyPressed(event);
        return PluginManager.INSTANCE.screenInput(plugin, id, "onKey", event.key(), event.scancode(), event.modifiers())
                || super.keyPressed(event);
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }
}
