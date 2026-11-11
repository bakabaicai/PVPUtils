package com.pvp_utils.client.render.chroma.effects;

import io.github.humbleui.skija.Path;
import io.github.humbleui.skija.PathBuilder;
import io.github.humbleui.skija.PathDirection;
import io.github.humbleui.skija.PathFillMode;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;

public final class ChromaSeparation {
    private static final float DETACH_PROGRESS = 0.9f;

    private ChromaSeparation() {
    }

    public static Geometry create(float x, float y, float width, float height, float radius,
                                  float satelliteRadius, float gap, float progress) {
        return create(x, y, width, height, radius, satelliteRadius, gap, progress, false);
    }

    public static Geometry create(float x, float y, float width, float height, float radius,
                                  float satelliteRadius, float gap, float progress, boolean alignTop) {
        return create(x, y, width, height, radius, satelliteRadius, gap, 0f, progress, alignTop);
    }

    public static Geometry create(float x, float y, float width, float height, float radius,
                                  float satelliteRadius, float gap, float leftProgress, float rightProgress,
                                  boolean alignTop) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(width)
                || !Float.isFinite(height) || width <= 0f || height <= 0f) {
            return new Geometry(new Path(), Rect.makeXYWH(0f, 0f, 0f, 0f), 0f, 0f, 0f, 0f, 0f, 0f);
        }
        float corner = Math.clamp(finite(radius), 0f, Math.min(width, height) * 0.5f);
        float requestedRadius = Math.clamp(finite(satelliteRadius), 0f, height * 0.5f);
        float leftSeparation = Math.clamp(finite(leftProgress), 0f, 1f);
        float rightSeparation = Math.clamp(finite(rightProgress), 0f, 1f);
        float spacing = Math.max(0f, finite(gap));
        float right = x + width;
        float centerY = y + (alignTop ? Math.max(corner, requestedRadius) : height * 0.5f);
        float inset = Math.max(corner, requestedRadius);
        float travel = inset + spacing + requestedRadius;
        float rightCenterX = right - inset + travel * rightSeparation;
        float leftCenterX = x + inset - travel * leftSeparation;
        float rightRadius = visibleRadius(requestedRadius, rightSeparation);
        float leftRadius = visibleRadius(requestedRadius, leftSeparation);
        try (PathBuilder builder = new PathBuilder(PathFillMode.WINDING)) {
            builder.addRRect(RRect.makeXYWH(x, y, width, height, corner), PathDirection.CLOCKWISE);
            addSatellite(builder, right, centerY, height, corner, rightCenterX, rightRadius, rightSeparation, 1f);
            addSatellite(builder, x, centerY, height, corner, leftCenterX, leftRadius, leftSeparation, -1f);
            Path path = builder.detach();
            return new Geometry(path, path.computeTightBounds(), rightCenterX, centerY, rightRadius,
                    leftCenterX, centerY, leftRadius);
        }
    }

    private static float visibleRadius(float radius, float progress) {
        return progress > 0f ? radius * (0.72f + 0.28f * smooth(progress / 0.4f)) : 0f;
    }

    private static void addSatellite(PathBuilder builder, float edgeX, float centerY, float height, float corner,
                                     float centerX, float circleRadius, float progress, float direction) {
        if (circleRadius <= 0f) return;
        builder.addCircle(centerX, centerY, circleRadius, PathDirection.CLOCKWISE);
        float anchorRadius = Math.min(height * 0.5f, Math.max(corner, circleRadius));
        float anchorX = edgeX - direction * anchorRadius;
        addBridge(builder, anchorX, centerY, anchorRadius, centerX, circleRadius, progress, direction);
    }

    private static void addBridge(PathBuilder builder, float anchorX, float centerY, float anchorRadius,
                                  float centerX, float circleRadius, float progress, float direction) {
        float distance = (centerX - anchorX) * direction;
        if (progress >= DETACH_PROGRESS || distance <= Math.abs(anchorRadius - circleRadius) + 0.001f) return;
        float pinch = 1f - smooth((progress - 0.55f) / (DETACH_PROGRESS - 0.55f));
        float blend = 0.55f * pinch;
        double outerAngle = Math.acos(Math.clamp((anchorRadius - circleRadius) / distance, -1f, 1f));
        double leftIntersection = 0.0;
        double rightIntersection = 0.0;
        if (distance < anchorRadius + circleRadius) {
            leftIntersection = Math.acos(Math.clamp((anchorRadius * anchorRadius + distance * distance
                    - circleRadius * circleRadius) / (2f * anchorRadius * distance), -1f, 1f));
            rightIntersection = Math.acos(Math.clamp((circleRadius * circleRadius + distance * distance
                    - anchorRadius * anchorRadius) / (2f * circleRadius * distance), -1f, 1f));
        }
        double leftAngle = leftIntersection + (outerAngle - leftIntersection) * blend;
        double rightAngle = rightIntersection + (Math.PI - rightIntersection - outerAngle) * blend;
        float leftSin = (float) Math.sin(leftAngle);
        float leftCos = (float) Math.cos(leftAngle);
        float rightSin = (float) Math.sin(rightAngle);
        float rightCos = (float) Math.cos(rightAngle);
        float leftX = anchorRadius * leftCos;
        float rightX = distance - circleRadius * rightCos;
        float leftY = anchorRadius * leftSin;
        float rightY = circleRadius * rightSin;
        float span = (float) Math.hypot(rightX - leftX, rightY - leftY);
        float handleRatio = Math.min(blend * 2.4f, span / (anchorRadius + circleRadius));
        float leftHandle = anchorRadius * handleRatio;
        float rightHandle = circleRadius * handleRatio;
        builder.moveTo(anchorX + direction * leftX, centerY - direction * leftY)
                .cubicTo(anchorX + direction * (leftX + leftSin * leftHandle), centerY - direction * (leftY - leftCos * leftHandle),
                        anchorX + direction * (rightX - rightSin * rightHandle), centerY - direction * (rightY - rightCos * rightHandle),
                        anchorX + direction * rightX, centerY - direction * rightY)
                .lineTo(anchorX + direction * rightX, centerY + direction * rightY)
                .cubicTo(anchorX + direction * (rightX - rightSin * rightHandle), centerY + direction * (rightY - rightCos * rightHandle),
                        anchorX + direction * (leftX + leftSin * leftHandle), centerY + direction * (leftY - leftCos * leftHandle),
                        anchorX + direction * leftX, centerY + direction * leftY)
                .closePath();
    }

    private static float finite(float value) {
        return Float.isFinite(value) ? value : 0f;
    }

    private static float smooth(float value) {
        float t = Math.clamp(value, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    public static final class Geometry implements AutoCloseable {
        private final Path path;
        private final Rect bounds;
        private final float satelliteCenterX;
        private final float satelliteCenterY;
        private final float satelliteRadius;
        private final float leftSatelliteCenterX;
        private final float leftSatelliteCenterY;
        private final float leftSatelliteRadius;
        private boolean closed;

        private Geometry(Path path, Rect bounds, float satelliteCenterX, float satelliteCenterY, float satelliteRadius,
                         float leftSatelliteCenterX, float leftSatelliteCenterY, float leftSatelliteRadius) {
            this.path = path;
            this.bounds = bounds;
            this.satelliteCenterX = satelliteCenterX;
            this.satelliteCenterY = satelliteCenterY;
            this.satelliteRadius = satelliteRadius;
            this.leftSatelliteCenterX = leftSatelliteCenterX;
            this.leftSatelliteCenterY = leftSatelliteCenterY;
            this.leftSatelliteRadius = leftSatelliteRadius;
        }

        public Path path() {
            return path;
        }

        public Rect bounds() {
            return bounds;
        }

        public float satelliteCenterX() {
            return satelliteCenterX;
        }

        public float satelliteCenterY() {
            return satelliteCenterY;
        }

        public float satelliteRadius() {
            return satelliteRadius;
        }

        public float leftSatelliteCenterX() {
            return leftSatelliteCenterX;
        }

        public float leftSatelliteCenterY() {
            return leftSatelliteCenterY;
        }

        public float leftSatelliteRadius() {
            return leftSatelliteRadius;
        }

        @Override
        public void close() {
            if (!closed) {
                path.close();
                closed = true;
            }
        }
    }
}
