package com.pvp_utils.client.modules.impl.Tool.tactical;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.chroma.ChromaDraw;
import com.pvp_utils.client.render.chroma.world.ChromaWorld;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.PaintStrokeCap;
import io.github.humbleui.skija.PaintStrokeJoin;
import io.github.humbleui.skija.Path;
import io.github.humbleui.skija.PathBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TacticalPingRenderer {
    private static final int[] NAVIGATION_COLORS = {
            0xFFADE8C1, 0xFFAFD7EE, 0xFFF1E2A6, 0xFFD4B9F0,
            0xFFF1BCCD, 0xFFF0C4A3, 0xFF9FE2DF, 0xFFE8EEF3
    };
    private static final int DANGER_COLOR = 0xFFDF2338;
    private static final Map<UUID, Label> LABELS = new HashMap<>();
    private static List<TacticalPingManager.Marker> lastMarkers = List.of();
    private static Path diamond;

    private TacticalPingRenderer() {
    }

    public static void render(Canvas canvas) {
        Minecraft client = Minecraft.getInstance();
        List<TacticalPingManager.Marker> markers = TacticalPingManager.markers();
        if (markers != lastMarkers) {
            LABELS.entrySet().removeIf(entry -> {
                boolean present = markers.stream().anyMatch(marker -> marker.owner().equals(entry.getKey()));
                if (!present) entry.getValue().close();
                return !present;
            });
            lastMarkers = markers;
        }
        if (!Config.tacticalPing || client.level == null || client.player == null || client.options.hideGui
                || client.getOverlay() != null || (client.screen != null && !(client.screen instanceof ChatScreen))) return;
        if (markers.isEmpty()) return;
        prepareResources();
        long now = System.nanoTime();
        String dimension = client.level.dimension().identifier().toString();
        float screenWidth = client.getWindow().getGuiScaledWidth();
        float screenHeight = client.getWindow().getGuiScaledHeight();
        float stemHeight = Math.clamp(screenHeight * 0.22f, 48f, 90f);
        for (TacticalPingManager.Marker marker : markers) {
            if (!dimension.equals(marker.dimension()) || marker.expiresNanos() <= now) continue;
            ChromaWorld.ScreenPoint point = ChromaWorld.project(marker.position());
            if (point == null) continue;
            float age = Math.max(0f, (now - marker.createdNanos()) / 1_000_000_000f);
            float alpha = Math.clamp((marker.expiresNanos() - now) / 300_000_000f, 0f, 1f);
            float reveal = easeOut(Math.clamp(age / 0.09f, 0f, 1f));
            float x = point.x();
            float bottom = point.y();
            float top = Math.max(10f, bottom - stemHeight * reveal);
            int color = color(marker);
            ChromaDraw.line(canvas, x, bottom, x, top + 5f, 1.8f, withAlpha(0xFF101820, alpha * 0.2f));
            ChromaDraw.line(canvas, x, bottom, x, top + 5f, 0.65f, withAlpha(color, alpha * 0.72f));
            if (age < 0.055f) continue;
            float pulse = Math.clamp((age - 0.055f) / 0.26f, 0f, 1f);
            float pulseRadius = 3.2f + 4.2f * (float) Math.sin(Math.PI * pulse);
            if (pulse < 1f) drawDiamond(canvas, x, top, pulseRadius, withAlpha(color, alpha * (1f - pulse) * 0.75f), false);
            drawDiamond(canvas, x, top, marker.danger() ? 1.9f : 1.6f, withAlpha(color, alpha), marker.danger());
            if (!marker.danger()) {
                ChromaDraw.rounded(canvas, x - 0.85f, top - 0.85f, 1.7f, 1.7f, 0.85f, withAlpha(0xFFEAF6FF, alpha));
            }
            float focusDistance = (float) Math.hypot(x - screenWidth * 0.5f, top - screenHeight * 0.5f);
            float detailAlpha = Math.clamp(1f - focusDistance / 64f, 0f, 1f);
            if (detailAlpha <= 0f) continue;
            int distance = (int) Math.min(Integer.MAX_VALUE, Math.round(client.player.position().distanceTo(marker.position())));
            Label label = LABELS.computeIfAbsent(marker.owner(), ignored -> new Label());
            label.prepare(marker.name(), distance);
            float labelX = x + 9f;
            if (labelX + label.width() > screenWidth - 5f) labelX = x - 9f - label.width();
            label.distance.drawTop(canvas, labelX + 0.5f, top - 3.5f, withAlpha(0xFF111820, alpha * detailAlpha * 0.8f));
            label.distance.drawTop(canvas, labelX, top - 4f, withAlpha(color, alpha * detailAlpha));
            if (label.name != null) label.name.drawTop(canvas, labelX, top + 7f, withAlpha(0xFFE0E6EA, alpha * detailAlpha * 0.85f));
        }
    }

    private static void prepareResources() {
        if (diamond == null) {
            try (PathBuilder builder = new PathBuilder()) {
                builder.moveTo(0f, -1f).lineTo(1f, 0f).lineTo(0f, 1f).lineTo(-1f, 0f).closePath();
                diamond = builder.detach();
            }
        }
    }

    public static void renderWorld(ChromaWorld.Frame frame) {
        Minecraft client = Minecraft.getInstance();
        if (!Config.tacticalPing || client.level == null || client.player == null || client.options.hideGui
                || client.getOverlay() != null || (client.screen != null && !(client.screen instanceof ChatScreen))) return;
        long now = System.nanoTime();
        String dimension = client.level.dimension().identifier().toString();
        for (TacticalPingManager.Marker marker : TacticalPingManager.markers()) {
            if (!dimension.equals(marker.dimension()) || marker.expiresNanos() <= now
                    || marker.position().distanceToSqr(frame.cameraPosition()) > 512.0 * 512.0) continue;
            Direction face = marker.face();
            Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
            Vec3 center = marker.position().add(normal.scale(0.006));
            float alpha = Math.clamp((marker.expiresNanos() - now) / 300_000_000f, 0f, 1f);
            float enter = Math.clamp((now - marker.createdNanos()) / 90_000_000f, 0f, 1f);
            double radius = 0.30 * (0.75 + 0.25 * easeOut(enter));
            int color = color(marker);
            ChromaWorld.circle(center, normal, radius, 40, withAlpha(color, alpha * 0.9f), 0, 1.8f, false);
        }
    }

    private static int color(TacticalPingManager.Marker marker) {
        return marker.danger() ? DANGER_COLOR : NAVIGATION_COLORS[Math.clamp(marker.colorCode(), 0, NAVIGATION_COLORS.length - 1)];
    }

    private static void drawDiamond(Canvas canvas, float x, float y, float radius, int color, boolean filled) {
        int save = canvas.save();
        try {
            canvas.translate(x, y);
            canvas.scale(radius, radius);
            if (filled) ChromaDraw.fillPath(canvas, diamond, color);
            else ChromaDraw.strokePath(canvas, diamond, 0.75f / radius, color, PaintStrokeCap.ROUND, PaintStrokeJoin.ROUND);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private static float easeOut(float value) {
        float remaining = 1f - value;
        return 1f - remaining * remaining * remaining;
    }

    private static int withAlpha(int color, float alpha) {
        return (color & 0x00FFFFFF) | (Math.round(Math.clamp(alpha, 0f, 1f) * (color >>> 24)) << 24);
    }

    public static void close() {
        LABELS.values().forEach(Label::close);
        LABELS.clear();
        lastMarkers = List.of();
        if (diamond != null) diamond.close();
        diamond = null;
    }

    private static final class Label {
        private ChromaDraw.PreparedText distance;
        private ChromaDraw.PreparedText name;
        private int blocks = -1;
        private String sender = "";
        private long revision = -1L;

        private void prepare(String value, int currentDistance) {
            long fontRevision = ChromaDraw.textFontRevision();
            String currentName = value == null ? "" : value;
            if (revision != fontRevision) close();
            if (distance == null || blocks != currentDistance) {
                if (distance != null) distance.close();
                distance = ChromaDraw.prepareText(currentDistance + "m", 8.5f, false);
                blocks = currentDistance;
            }
            if (!sender.equals(currentName) || (name == null && !currentName.isEmpty())) {
                if (name != null) name.close();
                name = currentName.isEmpty() ? null : ChromaDraw.prepareText(currentName, 8f, false);
                sender = currentName;
            }
            revision = fontRevision;
        }

        private float width() {
            return Math.max(distance.width(), name == null ? 0f : name.width());
        }

        private void close() {
            if (distance != null) distance.close();
            if (name != null) name.close();
            distance = null;
            name = null;
            blocks = -1;
            sender = "";
            revision = -1L;
        }
    }
}
