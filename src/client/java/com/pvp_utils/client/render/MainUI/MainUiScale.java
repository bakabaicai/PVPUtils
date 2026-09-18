package com.pvp_utils.client.render.MainUI;

import io.github.humbleui.skija.Canvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;

public final class MainUiScale {
    private MainUiScale() {
    }

    public static float pageScale() {
        Minecraft minecraft = Minecraft.getInstance();
        float guiScale = minecraft == null ? 2f : Math.max(1f, (float) minecraft.getWindow().getGuiScale());
        return 1.90f / guiScale;
    }

    public static float pageScale(float minX, float minY, float maxX, float maxY) {
        float availableWidth = Math.max(1f, pageWidth() - 24f);
        float availableHeight = Math.max(1f, pageHeight() - 24f);
        float factor = 0.95f;
        factor = Math.min(factor, availableWidth / Math.max(1f, maxX - minX));
        factor = Math.min(factor, availableHeight / Math.max(1f, maxY - minY));
        Minecraft minecraft = Minecraft.getInstance();
        float guiScale = minecraft == null ? 2f : Math.max(1f, (float) minecraft.getWindow().getGuiScale());
        return Math.max(0.05f, factor * 2f / guiScale);
    }

    public static int pageWidth() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft == null ? 960 : Math.max(1, Math.round(minecraft.getWindow().getWidth() * 0.5f));
    }

    public static int pageHeight() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft == null ? 540 : Math.max(1, Math.round(minecraft.getWindow().getHeight() * 0.5f));
    }

    public static void applyPage(Canvas canvas, int guiWidth, int guiHeight) {
        applyPage(canvas, guiWidth, guiHeight, pageScale());
    }

    public static void applyPage(Canvas canvas, int guiWidth, int guiHeight, float scale) {
        applyPage(canvas, guiWidth, guiHeight, scale, pageWidth() * 0.5f, pageHeight() * 0.5f);
    }

    public static void applyPage(Canvas canvas, int guiWidth, int guiHeight, float scale, float centerX, float centerY) {
        canvas.translate(guiWidth * 0.5f, guiHeight * 0.5f);
        canvas.scale(scale, scale);
        canvas.translate(-centerX, -centerY);
    }

    public static int pageX(int mouseX, int guiWidth) {
        return pageX(mouseX, guiWidth, pageScale());
    }

    public static int pageY(int mouseY, int guiHeight) {
        return pageY(mouseY, guiHeight, pageScale());
    }

    public static int pageX(int mouseX, int guiWidth, float scale) {
        return pageX(mouseX, guiWidth, scale, pageWidth() * 0.5f);
    }

    public static int pageY(int mouseY, int guiHeight, float scale) {
        return pageY(mouseY, guiHeight, scale, pageHeight() * 0.5f);
    }

    public static int pageX(int mouseX, int guiWidth, float scale, float centerX) {
        return Math.round(centerX + (mouseX - guiWidth * 0.5f) / scale);
    }

    public static int pageY(int mouseY, int guiHeight, float scale, float centerY) {
        return Math.round(centerY + (mouseY - guiHeight * 0.5f) / scale);
    }

    public static MouseButtonEvent pageEvent(MouseButtonEvent event, int guiWidth, int guiHeight) {
        return pageEvent(event, guiWidth, guiHeight, pageScale());
    }

    public static MouseButtonEvent pageEvent(MouseButtonEvent event, int guiWidth, int guiHeight, float scale) {
        return pageEvent(event, guiWidth, guiHeight, scale, pageWidth() * 0.5f, pageHeight() * 0.5f);
    }

    public static MouseButtonEvent pageEvent(MouseButtonEvent event, int guiWidth, int guiHeight, float scale, float centerX, float centerY) {
        return new MouseButtonEvent(pageX((int) event.x(), guiWidth, scale, centerX), pageY((int) event.y(), guiHeight, scale, centerY), event.buttonInfo());
    }

    public static float pageScreenX(float x, int guiWidth) {
        return pageScreenX(x, guiWidth, pageScale());
    }

    public static float pageScreenY(float y, int guiHeight) {
        return pageScreenY(y, guiHeight, pageScale());
    }

    public static float pageScreenSize(float size) {
        return pageScreenSize(size, pageScale());
    }

    public static float pageScreenX(float x, int guiWidth, float scale) {
        return pageScreenX(x, guiWidth, scale, pageWidth() * 0.5f);
    }

    public static float pageScreenY(float y, int guiHeight, float scale) {
        return pageScreenY(y, guiHeight, scale, pageHeight() * 0.5f);
    }

    public static float pageScreenX(float x, int guiWidth, float scale, float centerX) {
        return guiWidth * 0.5f + (x - centerX) * scale;
    }

    public static float pageScreenY(float y, int guiHeight, float scale, float centerY) {
        return guiHeight * 0.5f + (y - centerY) * scale;
    }

    public static float pageScreenSize(float size, float scale) {
        return size * scale;
    }

    public static void applyTopRight(Canvas canvas, int guiWidth, int guiHeight, float scale) {
        canvas.translate(guiWidth, 0f);
        canvas.scale(scale, scale);
        canvas.translate(-guiWidth, 0f);
    }

    public static void applyBottomLeft(Canvas canvas, int guiWidth, int guiHeight, float scale) {
        canvas.translate(0f, guiHeight);
        canvas.scale(scale, scale);
        canvas.translate(0f, -guiHeight);
    }

    public static int topRightX(int mouseX, int guiWidth, int guiHeight, float scale) {
        return Math.round(guiWidth + (mouseX - guiWidth) / scale);
    }

    public static int topRightY(int mouseY, int guiWidth, int guiHeight, float scale) {
        return Math.round(mouseY / scale);
    }
}
