package com.pvp_utils.client.modules.impl.Tool;

import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaRenderer;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class ServerConnectionOverlay {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int MAX_LOG_LINES = 8;
    private static final Deque<String> LOG_LINES = new ArrayDeque<>();
    private static final Paint DOT_PAINT = new Paint().setAntiAlias(true);
    private static long openedAtMs;
    private static long failedAtMs = -1L;
    private static String lastStatus = "";

    private ServerConnectionOverlay() {
    }

    public static void begin() {
        openedAtMs = System.currentTimeMillis();
        failedAtMs = -1L;
        lastStatus = "";
        synchronized (LOG_LINES) {
            LOG_LINES.clear();
        }
        log("Opening server connection screen");
        log("Waiting for handshake");
        log("Resolving server address and preparing network connection");
    }

    public static void logStatus(Component status) {
        String text = status == null ? "" : status.getString();
        if (text.isBlank() || text.equals(lastStatus)) return;
        lastStatus = text;
        log("Status: " + text);
    }

    public static void logFailure(Component title, Component reason) {
        if (failedAtMs <= 0L) {
            failedAtMs = System.currentTimeMillis();
        }
        String titleText = title == null ? "Connection failed" : title.getString();
        String reasonText = reason == null ? "" : reason.getString();
        log("Failed: " + titleText);
        if (!reasonText.isBlank()) {
            log("Reason: " + reasonText);
        }
    }

    public static void log(String text) {
        if (text == null || text.isBlank()) return;
        String line = "[" + LocalTime.now().format(TIME_FORMAT) + "] " + text;
        synchronized (LOG_LINES) {
            while (LOG_LINES.size() >= MAX_LOG_LINES) {
                LOG_LINES.removeFirst();
            }
            LOG_LINES.addLast(line);
        }
    }

    public static void render(GuiGraphics graphics, int width, int height, Component status) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;

        Canvas canvas = SkiaRenderer.beginRegion(0, 0, width, height);
        if (canvas == null) return;
        try {
            drawConnectionRow(canvas, width, height, status);
            drawLogLines(canvas, height);
        } finally {
            SkiaRenderer.endRegion(graphics);
        }
    }

    public static void renderFailure(GuiGraphics graphics, int width, int height) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;

        Canvas canvas = SkiaRenderer.beginRegion(0, 0, width, height);
        if (canvas == null) return;
        try {
            drawFailureRow(canvas, width, height);
            drawLogLines(canvas, height);
        } finally {
            SkiaRenderer.endRegion(graphics);
        }
    }

    private static void drawConnectionRow(Canvas canvas, int width, int height, Component status) {
        float iconSize = 34f;
        float y = height / 2f - 3f;
        String leftIcon = "\uF80E";
        String rightIcon = "\uE30C";

        float leftW = FontRenderer.measureTextWidth(leftIcon, iconSize, FontRenderer.MATERIAL_SYMBOLS);
        float rightW = FontRenderer.measureTextWidth(rightIcon, iconSize, FontRenderer.MATERIAL_SYMBOLS);
        float iconGap = 112f;
        float dotsX = width / 2f + (leftW - rightW) / 2f;
        float leftX = dotsX - iconGap - leftW / 2f;
        float rightX = dotsX + iconGap - rightW / 2f;

        FontRenderer.drawText(canvas, leftIcon, leftX, y, iconSize, 0xFFEFF6FF, FontRenderer.MATERIAL_SYMBOLS);
        drawLoadingDots(canvas, dotsX, y - 23f);
        FontRenderer.drawText(canvas, rightIcon, rightX, y, iconSize, 0xFFEFF6FF, FontRenderer.MATERIAL_SYMBOLS);
    }

    private static void drawFailureRow(Canvas canvas, int width, int height) {
        float iconSize = 34f;
        float centerIconSize = 36f;
        float y = height / 2f - 42f;
        String leftIcon = "\uF80E";
        String centerIcon = "\uE5CD";
        String rightIcon = "\uE30C";

        float leftW = FontRenderer.measureTextWidth(leftIcon, iconSize, FontRenderer.MATERIAL_SYMBOLS);
        float rightW = FontRenderer.measureTextWidth(rightIcon, iconSize, FontRenderer.MATERIAL_SYMBOLS);
        float centerW = FontRenderer.measureTextWidth(centerIcon, centerIconSize, FontRenderer.MATERIAL_SYMBOLS);
        float iconGap = 112f;
        float centerX = width / 2f + (leftW - rightW) / 2f;
        float age = failedAtMs <= 0L ? 1f : Math.min(1f, (System.currentTimeMillis() - failedAtMs) / 520f);
        float eased = easeOutBack(age);
        float shake = age < 1f ? (float) Math.sin(age * Math.PI * 9f) * (1f - age) * 8f : 0f;

        float leftX = centerX - iconGap - leftW / 2f + shake;
        float rightX = centerX + iconGap - rightW / 2f + shake;
        float centerY = y + (1f - eased) * 9f;
        float scaledCenterSize = centerIconSize * Math.max(0.2f, eased);
        float scaledCenterW = FontRenderer.measureTextWidth(centerIcon, scaledCenterSize, FontRenderer.MATERIAL_SYMBOLS);

        FontRenderer.drawText(canvas, leftIcon, leftX, y, iconSize, 0xFFEFF6FF, FontRenderer.MATERIAL_SYMBOLS);
        FontRenderer.drawText(canvas, centerIcon, centerX - scaledCenterW / 2f + shake, centerY, scaledCenterSize, 0xFFFF5555, FontRenderer.MATERIAL_SYMBOLS);
        FontRenderer.drawText(canvas, rightIcon, rightX, y, iconSize, 0xFFEFF6FF, FontRenderer.MATERIAL_SYMBOLS);
    }

    private static void drawLoadingDots(Canvas canvas, float x, float y) {
        long elapsed = Math.max(0L, System.currentTimeMillis() - openedAtMs);
        DOT_PAINT.setColor(0xEFFFFFFF);
        float[] offsets = {-24f, 0f, 24f};
        for (int i = 0; i < 3; i++) {
            double phase = (elapsed / 260.0) - i * 0.65;
            float radius = 4.5f + (float) ((Math.sin(phase) + 1.0) * 2.2);
            canvas.drawCircle(x + offsets[i], y + 10f, radius, DOT_PAINT);
        }
    }

    private static void drawLogLines(Canvas canvas, int height) {
        List<String> lines;
        synchronized (LOG_LINES) {
            lines = new ArrayList<>(LOG_LINES);
        }
        if (lines.isEmpty()) return;

        float textSize = 10.5f;
        float lineHeight = 13f;
        float x = 10f;
        float y = height - 10f - (lines.size() - 1) * lineHeight;
        for (String line : lines) {
            FontRenderer.drawText(canvas, trim(line, 86), x, y, textSize, 0xB8FFFFFF);
            y += lineHeight;
        }
    }

    private static String trim(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float p = t - 1f;
        return 1f + c3 * p * p * p + c1 * p * p;
    }
}
