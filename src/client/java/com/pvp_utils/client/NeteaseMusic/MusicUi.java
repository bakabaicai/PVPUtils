package com.pvp_utils.client.NeteaseMusic;

import com.pvp_utils.client.render.skia.SkijaUi;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintStrokeCap;
import io.github.humbleui.skija.PaintStrokeJoin;
import io.github.humbleui.skija.Path;
import io.github.humbleui.skija.PathBuilder;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class MusicUi {
    public static final int BG = 0xFF0E0F12;
    public static final int SIDEBAR = 0xFF16171B;
    public static final int PANEL = 0xFF24262C;
    public static final int TEXT = 0xFFF2F3F5;
    public static final int MUTED = 0xFF8A8D94;
    public static final int ACCENT = 0xFFD60017;
    private static final Map<String, Path> ICONS = new HashMap<>();
    private static final Set<String> FILLED_ICONS = Set.of("play", "pause", "previous", "next", "more");
    private static final Set<String> ICON_NAMES = Set.of("search", "home", "settings", "library", "heart", "play",
            "pause", "previous", "next", "shuffle", "repeat", "repeat_one", "volume", "mute", "queue", "close",
            "chevron_left", "chevron_right", "chevron_down", "more", "logout", "refresh", "music", "check", "add");

    private MusicUi() {
    }

    public static void text(Canvas canvas, String value, float x, float y, float size, int color) {
        SkijaUi.textAtTop(canvas, value, x, y, color, textSize(size), false);
    }

    public static void bold(Canvas canvas, String value, float x, float y, float size, int color) {
        SkijaUi.textAtTop(canvas, value, x, y, color, textSize(size), true);
    }

    public static float width(String value, float size) {
        return SkijaUi.textWidth(value, textSize(size));
    }

    public static float boldWidth(String value, float size) {
        return SkijaUi.boldTextWidth(value, textSize(size));
    }

    static SkijaUi.PreparedText prepareBold(String value, float size) {
        return SkijaUi.prepareText(value, textSize(size), true);
    }

    public static String fit(String value, float availableWidth, float size) {
        if (value == null || value.isEmpty() || availableWidth <= 0f) return "";
        if (width(value, size) <= availableWidth) return value;
        String suffix = "…";
        if (width(suffix, size) > availableWidth) return "";
        int[] points = value.codePoints().toArray();
        int low = 0;
        int high = points.length;
        while (low < high) {
            int middle = (low + high + 1) / 2;
            String candidate = new String(points, 0, middle) + suffix;
            if (width(candidate, size) <= availableWidth) low = middle;
            else high = middle - 1;
        }
        return new String(points, 0, low) + suffix;
    }

    public static void rounded(Canvas canvas, float x, float y, float width, float height, float radius, int color) {
        if (width <= 0f || height <= 0f || color >>> 24 == 0) return;
        SkijaUi.rounded(canvas, x, y, width, height,
                Math.max(0f, Math.min(radius, Math.min(width, height) * 0.5f)), color);
    }

    public static void line(Canvas canvas, float x1, float y1, float x2, float y2, float width, int color) {
        if (width <= 0f || color >>> 24 == 0) return;
        SkijaUi.line(canvas, x1, y1, x2, y2, width, color, PaintStrokeCap.ROUND);
    }

    public static void icon(Canvas canvas, String name, float x, float y, float size, int color) {
        if (name == null || size <= 0f || color >>> 24 == 0) return;
        Path path = iconPath(name);
        int save = canvas.save();
        try {
            canvas.translate(x, y);
            canvas.scale(size / 24f, size / 24f);
            if (FILLED_ICONS.contains(name)) SkijaUi.fillPath(canvas, path, color);
            else SkijaUi.strokePath(canvas, path, 1.8f, color, PaintStrokeCap.ROUND, PaintStrokeJoin.ROUND);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    public static void cover(Canvas canvas, Image image, float x, float y, float size, float radius, int opacity) {
        if (size <= 0f || opacity <= 0) return;
        int color = (Math.min(255, opacity) << 24) | 0xFFFFFF;
        if (image == null || image.isClosed()) {
            rounded(canvas, x, y, size, size, radius, alpha(PANEL, opacity / 255f));
            icon(canvas, "music", x + size * 0.28f, y + size * 0.28f, size * 0.44f,
                    alpha(MUTED, opacity / 255f));
            return;
        }
        float crop = Math.min(image.getWidth(), image.getHeight());
        if (crop <= 0f) return;
        Rect source = Rect.makeXYWH((image.getWidth() - crop) * 0.5f,
                (image.getHeight() - crop) * 0.5f, crop, crop);
        int save = canvas.save();
        try (Paint paint = new Paint().setAntiAlias(true).setColor(color)) {
            canvas.clipRRect(RRect.makeXYWH(x, y, size, size, Math.max(0f, Math.min(radius, size * 0.5f))), true);
            canvas.drawImageRect(image, source, Rect.makeXYWH(x, y, size, size), SamplingMode.LINEAR, paint, true);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    public static int alpha(int color, float opacity) {
        return (Math.round((color >>> 24) * Math.max(0f, Math.min(1f, opacity))) << 24) | (color & 0xFFFFFF);
    }

    public static synchronized void close() {
        ICONS.values().forEach(Path::close);
        ICONS.clear();
    }

    private static float textSize(float size) {
        return Math.round(Math.max(1f, Math.min(96f, size)) * 2f) * 0.5f;
    }

    private static synchronized Path iconPath(String name) {
        name = ICON_NAMES.contains(name) ? name : "music";
        Path existing = ICONS.get(name);
        if (existing != null) return existing;
        try (PathBuilder path = new PathBuilder()) {
            switch (name) {
                case "search" -> {
                    path.addCircle(10.5f, 10.5f, 6.5f);
                    path.moveTo(15.3f, 15.3f).lineTo(21f, 21f);
                }
                case "home" -> {
                    path.moveTo(3f, 10.5f).lineTo(12f, 3f).lineTo(21f, 10.5f);
                    path.moveTo(5.5f, 9f).lineTo(5.5f, 21f).lineTo(10f, 21f).lineTo(10f, 14f)
                            .lineTo(14f, 14f).lineTo(14f, 21f).lineTo(18.5f, 21f).lineTo(18.5f, 9f);
                }
                case "settings" -> {
                    for (int i = 0; i < 32; i++) {
                        double angle = Math.PI * i / 16.0 - Math.PI * 0.5;
                        float radius = i % 4 == 1 || i % 4 == 2 ? 10f : 7.7f;
                        float px = 12f + (float) Math.cos(angle) * radius;
                        float py = 12f + (float) Math.sin(angle) * radius;
                        if (i == 0) path.moveTo(px, py);
                        else path.lineTo(px, py);
                    }
                    path.closePath().addCircle(12f, 12f, 3.4f);
                }
                case "library" -> {
                    path.moveTo(4f, 4f).lineTo(4f, 21f).moveTo(9f, 4f).lineTo(9f, 21f);
                    path.moveTo(14f, 5f).lineTo(18f, 4f).lineTo(22f, 20f).lineTo(18f, 21f).closePath();
                }
                case "heart" -> path.moveTo(12f, 21f).cubicTo(9f, 18f, 3f, 14f, 3f, 8.5f)
                        .cubicTo(3f, 3f, 9f, 2f, 12f, 7f).cubicTo(15f, 2f, 21f, 3f, 21f, 8.5f)
                        .cubicTo(21f, 14f, 15f, 18f, 12f, 21f).closePath();
                case "play" -> path.moveTo(7f, 4f).lineTo(20f, 12f).lineTo(7f, 20f).closePath();
                case "pause" -> {
                    rectangle(path, 6f, 4f, 4f, 16f);
                    rectangle(path, 14f, 4f, 4f, 16f);
                }
                case "previous" -> {
                    rectangle(path, 4f, 5f, 2.5f, 14f);
                    path.moveTo(19f, 5f).lineTo(7.5f, 12f).lineTo(19f, 19f).closePath();
                }
                case "next" -> {
                    path.moveTo(5f, 5f).lineTo(16.5f, 12f).lineTo(5f, 19f).closePath();
                    rectangle(path, 17.5f, 5f, 2.5f, 14f);
                }
                case "shuffle" -> {
                    path.moveTo(3f, 6f).lineTo(6f, 6f).cubicTo(10f, 6f, 14f, 18f, 18f, 18f).lineTo(21f, 18f);
                    path.moveTo(3f, 18f).lineTo(6f, 18f).cubicTo(10f, 18f, 14f, 6f, 18f, 6f).lineTo(21f, 6f);
                    path.moveTo(18f, 3f).lineTo(21f, 6f).lineTo(18f, 9f);
                    path.moveTo(18f, 15f).lineTo(21f, 18f).lineTo(18f, 21f);
                }
                case "repeat", "repeat_one" -> {
                    path.moveTo(3f, 10f).lineTo(3f, 8f).quadTo(3f, 5f, 6f, 5f).lineTo(20f, 5f);
                    path.moveTo(17f, 2f).lineTo(20f, 5f).lineTo(17f, 8f);
                    path.moveTo(21f, 14f).lineTo(21f, 16f).quadTo(21f, 19f, 18f, 19f).lineTo(4f, 19f);
                    path.moveTo(7f, 16f).lineTo(4f, 19f).lineTo(7f, 22f);
                    if (name.equals("repeat_one")) path.moveTo(10f, 11f).lineTo(12f, 9f).lineTo(12f, 15f);
                }
                case "volume", "mute" -> {
                    path.moveTo(3f, 9f).lineTo(7f, 9f).lineTo(12f, 5f).lineTo(12f, 19f)
                            .lineTo(7f, 15f).lineTo(3f, 15f).closePath();
                    if (name.equals("volume")) {
                        path.moveTo(16f, 8f).quadTo(20f, 12f, 16f, 16f);
                        path.moveTo(19f, 5f).quadTo(25f, 12f, 19f, 19f);
                    } else {
                        path.moveTo(16f, 9f).lineTo(22f, 15f).moveTo(22f, 9f).lineTo(16f, 15f);
                    }
                }
                case "queue" -> {
                    path.moveTo(3f, 5f).lineTo(21f, 5f).moveTo(3f, 10f).lineTo(21f, 10f);
                    path.moveTo(3f, 15f).lineTo(12f, 15f).moveTo(3f, 20f).lineTo(12f, 20f);
                    path.moveTo(17f, 15f).lineTo(22f, 18f).lineTo(17f, 21f).closePath();
                }
                case "close" -> path.moveTo(6f, 6f).lineTo(18f, 18f).moveTo(18f, 6f).lineTo(6f, 18f);
                case "chevron_left" -> path.moveTo(15f, 5f).lineTo(8f, 12f).lineTo(15f, 19f);
                case "chevron_right" -> path.moveTo(9f, 5f).lineTo(16f, 12f).lineTo(9f, 19f);
                case "chevron_down" -> path.moveTo(5f, 9f).lineTo(12f, 16f).lineTo(19f, 9f);
                case "more" -> path.addCircle(5f, 12f, 1.8f).addCircle(12f, 12f, 1.8f).addCircle(19f, 12f, 1.8f);
                case "logout" -> {
                    path.moveTo(10f, 3f).lineTo(4f, 3f).lineTo(4f, 21f).lineTo(10f, 21f);
                    path.moveTo(9f, 12f).lineTo(22f, 12f).moveTo(18f, 8f).lineTo(22f, 12f).lineTo(18f, 16f);
                }
                case "refresh" -> {
                    path.moveTo(20f, 8f).cubicTo(18f, 2f, 7f, 2f, 4f, 9f);
                    path.moveTo(20f, 3f).lineTo(20f, 8f).lineTo(15f, 8f);
                    path.moveTo(4f, 16f).cubicTo(6f, 22f, 17f, 22f, 20f, 15f);
                    path.moveTo(4f, 21f).lineTo(4f, 16f).lineTo(9f, 16f);
                }
                case "check" -> path.moveTo(4f, 12f).lineTo(9f, 17f).lineTo(20f, 6f);
                case "add" -> path.moveTo(12f, 4f).lineTo(12f, 20f).moveTo(4f, 12f).lineTo(20f, 12f);
                default -> {
                    path.moveTo(9f, 17f).lineTo(9f, 5f).lineTo(20f, 3f).lineTo(20f, 15f);
                    path.moveTo(9f, 9f).lineTo(20f, 7f);
                    path.addCircle(6f, 18f, 3f).addCircle(17f, 16f, 3f);
                }
            }
            Path result = path.detach();
            ICONS.put(name, result);
            return result;
        }
    }

    private static void rectangle(PathBuilder path, float x, float y, float width, float height) {
        path.moveTo(x, y).lineTo(x + width, y).lineTo(x + width, y + height).lineTo(x, y + height).closePath();
    }
}
