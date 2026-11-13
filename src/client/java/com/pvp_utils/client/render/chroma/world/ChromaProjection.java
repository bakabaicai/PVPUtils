package com.pvp_utils.client.render.chroma.world;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class ChromaProjection {
    private static final double EPSILON = 1e-7;
    private final Matrix4f view;
    private final Matrix4f transform;
    private final double cameraX;
    private final double cameraY;
    private final double cameraZ;
    private final int width;
    private final int height;

    public ChromaProjection(Matrix4fc modelView, Matrix4fc projection,
                            double cameraX, double cameraY, double cameraZ, int width, int height) {
        if (modelView == null || projection == null || !finite(cameraX, cameraY, cameraZ)
                || width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid projection frame");
        this.view = new Matrix4f(modelView);
        this.transform = new Matrix4f(projection).mul(modelView);
        this.cameraX = cameraX;
        this.cameraY = cameraY;
        this.cameraZ = cameraZ;
        this.width = width;
        this.height = height;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public Point project(double x, double y, double z) {
        if (!finite(x, y, z)) return null;
        double dx = x - cameraX;
        double dy = y - cameraY;
        double dz = z - cameraZ;
        double cx = transform.m00() * dx + transform.m10() * dy + transform.m20() * dz + transform.m30();
        double cy = transform.m01() * dx + transform.m11() * dy + transform.m21() * dz + transform.m31();
        double cz = transform.m02() * dx + transform.m12() * dy + transform.m22() * dz + transform.m32();
        double cw = transform.m03() * dx + transform.m13() * dy + transform.m23() * dz + transform.m33();
        if (!finite(cx, cy, cz) || !Double.isFinite(cw) || cw <= EPSILON || cz < -cw - EPSILON) return null;
        double sx = (cx / cw * 0.5 + 0.5) * width;
        double sy = (0.5 - cy / cw * 0.5) * height;
        double depth = depth(dx, dy, dz);
        if (!finite(sx, sy, depth)) return null;
        return new Point(sx, sy, depth, sx >= 0 && sx <= width && sy >= 0 && sy <= height && cz <= cw + EPSILON);
    }

    public Bounds projectBounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (!finite(minX, minY, minZ) || !finite(maxX, maxY, maxZ)
                || minX > maxX || minY > maxY || minZ > maxZ) return null;
        double[] vertices = new double[32];
        for (int corner = 0; corner < 8; corner++) {
            double x = ((corner & 1) == 0 ? minX : maxX) - cameraX;
            double y = ((corner & 2) == 0 ? minY : maxY) - cameraY;
            double z = ((corner & 4) == 0 ? minZ : maxZ) - cameraZ;
            int offset = corner * 4;
            vertices[offset] = transform.m00() * x + transform.m10() * y + transform.m20() * z + transform.m30();
            vertices[offset + 1] = transform.m01() * x + transform.m11() * y + transform.m21() * z + transform.m31();
            vertices[offset + 2] = transform.m02() * x + transform.m12() * y + transform.m22() * z + transform.m32();
            vertices[offset + 3] = transform.m03() * x + transform.m13() * y + transform.m23() * z + transform.m33();
            for (int component = 0; component < 4; component++) {
                if (!Double.isFinite(vertices[offset + component])) return null;
            }
        }
        double left = Double.POSITIVE_INFINITY;
        double top = Double.POSITIVE_INFINITY;
        double right = Double.NEGATIVE_INFINITY;
        double bottom = Double.NEGATIVE_INFINITY;
        for (int corner = 0; corner < 8; corner++) {
            for (int axis = 1; axis <= 4; axis <<= 1) {
                if ((corner & axis) != 0) continue;
                int from = corner * 4;
                int to = (corner | axis) * 4;
                double start = 0;
                double end = 1;
                for (int plane = 0; plane < 3; plane++) {
                    double a = planeDistance(vertices, from, plane);
                    double b = planeDistance(vertices, to, plane);
                    if (a < 0 && b < 0) {
                        end = -1;
                        break;
                    }
                    if (a < 0) start = Math.max(start, a / (a - b));
                    else if (b < 0) end = Math.min(end, a / (a - b));
                }
                if (start > end) continue;
                for (int endpoint = 0; endpoint < 2; endpoint++) {
                    double t = endpoint == 0 ? start : end;
                    double w = interpolate(vertices[from + 3], vertices[to + 3], t);
                    if (w < EPSILON * 0.5) continue;
                    double x = (interpolate(vertices[from], vertices[to], t) / w * 0.5 + 0.5) * width;
                    double y = (0.5 - interpolate(vertices[from + 1], vertices[to + 1], t) / w * 0.5) * height;
                    if (!Double.isFinite(x) || !Double.isFinite(y)) continue;
                    left = Math.min(left, x);
                    top = Math.min(top, y);
                    right = Math.max(right, x);
                    bottom = Math.max(bottom, y);
                }
            }
        }
        if (!Double.isFinite(left)) return null;
        double depth = depth(minX * 0.5 + maxX * 0.5 - cameraX,
                minY * 0.5 + maxY * 0.5 - cameraY, minZ * 0.5 + maxZ * 0.5 - cameraZ);
        if (!Double.isFinite(depth)) return null;
        boolean onScreen = right >= 0 && bottom >= 0 && left <= width && top <= height;
        if (onScreen) {
            left = Math.max(0, left);
            top = Math.max(0, top);
            right = Math.min(width, right);
            bottom = Math.min(height, bottom);
        }
        return new Bounds(left, top, right, bottom, depth, onScreen);
    }

    private double depth(double x, double y, double z) {
        return -(view.m02() * x + view.m12() * y + view.m22() * z + view.m32());
    }

    private static double planeDistance(double[] vertices, int offset, int plane) {
        return switch (plane) {
            case 0 -> vertices[offset + 3] + vertices[offset + 2];
            case 1 -> vertices[offset + 3] - vertices[offset + 2];
            default -> vertices[offset + 3] - EPSILON;
        };
    }

    private static double interpolate(double from, double to, double amount) {
        return from + (to - from) * amount;
    }

    private static boolean finite(double x, double y, double z) {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
    }

    public record Point(double x, double y, double depth, boolean onScreen) {
    }

    public record Bounds(double minX, double minY, double maxX, double maxY, double depth, boolean onScreen) {
    }
}
