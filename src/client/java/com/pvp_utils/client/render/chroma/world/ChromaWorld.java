package com.pvp_utils.client.render.chroma.world;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.util.ARGB;
import com.pvp_utils.PVPUtils;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class ChromaWorld {
    private static final Matrix4f MODEL_VIEW = new Matrix4f();
    private static final Matrix4f PROJECTION = new Matrix4f();
    private static Vec3 cameraPosition = Vec3.ZERO;
    private static boolean projectionReady;
    private static Vec3 cameraRight = new Vec3(1, 0, 0);
    private static Vec3 cameraUp = new Vec3(0, 1, 0);
    private static final CopyOnWriteArrayList<Registration> RENDERERS = new CopyOnWriteArrayList<>();
    private static final float[][] CIRCLE = circleVertices(24);
    private static final float[][] STAR = starVertices();
    private static final float[][] HEART = heartVertices();
    private static final float[][] QUAD = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};

    private ChromaWorld() {
    }

    public static void capture(Camera camera, Matrix4fc modelViewMatrix, Matrix4fc projectionMatrix) {
        if (camera == null) {
            projectionReady = false;
            return;
        }
        cameraPosition = camera.position();
        Vector3f right = new Vector3f(1f, 0f, 0f).rotate(camera.rotation());
        Vector3f up = new Vector3f(0f, 1f, 0f).rotate(camera.rotation());
        cameraRight = new Vec3(right.x, right.y, right.z);
        cameraUp = new Vec3(up.x, up.y, up.z);
        MODEL_VIEW.set(modelViewMatrix);
        PROJECTION.set(projectionMatrix);
        projectionReady = true;
    }

    public static void line(Vec3 from, Vec3 to, int color, float width) {
        line(from, to, color, width, false);
    }

    public static void line(Vec3 from, Vec3 to, int color, float width, boolean throughWalls) {
        if (throughWalls) {
            Gizmos.line(from, to, color, width).setAlwaysOnTop();
        } else {
            Gizmos.line(from, to, color, width);
        }
    }

    public static void box(AABB box, int color) {
        Gizmos.cuboid(box, GizmoStyle.stroke(color));
    }

    public static void filledBox(AABB box, int color) {
        filledBox(box, color, false);
    }

    public static void filledBox(AABB box, int color, boolean throughWalls) {
        if (throughWalls) {
            Gizmos.cuboid(box, GizmoStyle.fill(color)).setAlwaysOnTop();
        } else {
            Gizmos.cuboid(box, GizmoStyle.fill(color));
        }
    }

    public static void box(AABB box, int outlineColor, int fillColor) {
        Gizmos.cuboid(box, GizmoStyle.strokeAndFill(outlineColor, 1.0f, fillColor));
    }

    public static void face(AABB box, Vec3 viewer, int color, float width) {
        double x = viewer.x - (box.minX + box.maxX) * 0.5;
        double y = viewer.y - (box.minY + box.maxY) * 0.5;
        double z = viewer.z - (box.minZ + box.maxZ) * 0.5;
        double ax = Math.abs(x);
        double ay = Math.abs(y);
        double az = Math.abs(z);
        double offset = 0.002;
        Vec3 a;
        Vec3 b;
        Vec3 c;
        Vec3 d;

        if (ax >= ay && ax >= az) {
            double faceX = x >= 0.0 ? box.maxX + offset : box.minX - offset;
            a = new Vec3(faceX, box.minY, box.minZ);
            b = new Vec3(faceX, box.maxY, box.minZ);
            c = new Vec3(faceX, box.maxY, box.maxZ);
            d = new Vec3(faceX, box.minY, box.maxZ);
        } else if (ay >= az) {
            double faceY = y >= 0.0 ? box.maxY + offset : box.minY - offset;
            a = new Vec3(box.minX, faceY, box.minZ);
            b = new Vec3(box.maxX, faceY, box.minZ);
            c = new Vec3(box.maxX, faceY, box.maxZ);
            d = new Vec3(box.minX, faceY, box.maxZ);
        } else {
            double faceZ = z >= 0.0 ? box.maxZ + offset : box.minZ - offset;
            a = new Vec3(box.minX, box.minY, faceZ);
            b = new Vec3(box.minX, box.maxY, faceZ);
            c = new Vec3(box.maxX, box.maxY, faceZ);
            d = new Vec3(box.maxX, box.minY, faceZ);
        }

        line(a, b, color, width);
        line(b, c, color, width);
        line(c, d, color, width);
        line(d, a, color, width);
    }

    public static void face(AABB box, Direction direction, int color, float width) {
        double offset = 0.002;
        Vec3 a;
        Vec3 b;
        Vec3 c;
        Vec3 d;

        switch (direction) {
            case UP -> {
                double y = box.maxY + offset;
                a = new Vec3(box.minX, y, box.minZ);
                b = new Vec3(box.maxX, y, box.minZ);
                c = new Vec3(box.maxX, y, box.maxZ);
                d = new Vec3(box.minX, y, box.maxZ);
            }
            case DOWN -> {
                double y = box.minY - offset;
                a = new Vec3(box.minX, y, box.minZ);
                b = new Vec3(box.maxX, y, box.minZ);
                c = new Vec3(box.maxX, y, box.maxZ);
                d = new Vec3(box.minX, y, box.maxZ);
            }
            case EAST -> {
                double x = box.maxX + offset;
                a = new Vec3(x, box.minY, box.minZ);
                b = new Vec3(x, box.maxY, box.minZ);
                c = new Vec3(x, box.maxY, box.maxZ);
                d = new Vec3(x, box.minY, box.maxZ);
            }
            case WEST -> {
                double x = box.minX - offset;
                a = new Vec3(x, box.minY, box.minZ);
                b = new Vec3(x, box.maxY, box.minZ);
                c = new Vec3(x, box.maxY, box.maxZ);
                d = new Vec3(x, box.minY, box.maxZ);
            }
            case SOUTH -> {
                double z = box.maxZ + offset;
                a = new Vec3(box.minX, box.minY, z);
                b = new Vec3(box.minX, box.maxY, z);
                c = new Vec3(box.maxX, box.maxY, z);
                d = new Vec3(box.maxX, box.minY, z);
            }
            default -> {
                double z = box.minZ - offset;
                a = new Vec3(box.minX, box.minY, z);
                b = new Vec3(box.minX, box.maxY, z);
                c = new Vec3(box.maxX, box.maxY, z);
                d = new Vec3(box.maxX, box.minY, z);
            }
        }

        line(a, b, color, width);
        line(b, c, color, width);
        line(c, d, color, width);
        line(d, a, color, width);
    }

    public static ScreenPoint project(Vec3 position) {
        if (!projectionReady) {
            return null;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) {
            return null;
        }

        Vector4f clip = new Vector4f(
                (float) (position.x - cameraPosition.x),
                (float) (position.y - cameraPosition.y),
                (float) (position.z - cameraPosition.z),
                1.0f
        );
        clip.mul(MODEL_VIEW).mul(PROJECTION);
        if (clip.w <= 0.01f) {
            return null;
        }

        float x = clip.x / clip.w;
        float y = clip.y / clip.w;
        if (x < -1.0f || x > 1.0f || y < -1.0f || y > 1.0f) {
            return null;
        }
        return new ScreenPoint(
                (x * 0.5f + 0.5f) * client.getWindow().getGuiScaledWidth(),
                (0.5f - y * 0.5f) * client.getWindow().getGuiScaledHeight()
        );
    }

    public static Registration register(Consumer<Frame> renderer) {
        Registration registration = new Registration(Objects.requireNonNull(renderer));
        RENDERERS.add(registration);
        return registration;
    }

    public static void renderRegistered(float partialTick) {
        Frame frame = new Frame(cameraPosition, Math.clamp(partialTick, 0f, 1f));
        for (Registration registration : RENDERERS) {
            if (registration.closed) continue;
            try {
                registration.renderer.accept(frame);
            } catch (RuntimeException error) {
                registration.close();
                PVPUtils.LOGGER.error("World rendering callback {} failed and was removed",
                        registration.renderer.getClass().getName(), error);
            }
        }
    }

    public static void point(Vec3 position, int color, float size, boolean throughWalls) {
        if (!valid(position) || !Float.isFinite(size) || size <= 0f) return;
        depth(Gizmos.point(position, color, size), throughWalls);
    }

    public static void arrow(Vec3 from, Vec3 to, int color, float width, boolean throughWalls) {
        if (!valid(from) || !valid(to) || from.distanceToSqr(to) < 1e-10 || !Float.isFinite(width) || width <= 0f) return;
        depth(Gizmos.arrow(from, to, color, width), throughWalls);
    }

    public static void polyline(List<Vec3> points, int color, float width, boolean closed, boolean throughWalls) {
        if (points == null || points.size() < 2 || points.size() > 4096 || !Float.isFinite(width) || width <= 0f) return;
        Vec3[] vertices = points.toArray(Vec3[]::new);
        for (Vec3 vertex : vertices) if (!valid(vertex)) return;
        depth(Gizmos.addGizmo((primitives, alpha) -> {
            int actualColor = ARGB.multiplyAlpha(color, alpha);
            for (int i = 1; i < vertices.length; i++) primitives.addLine(vertices[i - 1], vertices[i], actualColor, width);
            if (closed) primitives.addLine(vertices[vertices.length - 1], vertices[0], actualColor, width);
        }), throughWalls);
    }

    public static void triangle(Vec3 a, Vec3 b, Vec3 c, int color, boolean throughWalls) {
        if (!valid(a) || !valid(b) || !valid(c)) return;
        Vec3[] vertices = {a, b, c};
        depth(Gizmos.addGizmo((primitives, alpha) -> primitives.addTriangleFan(vertices, ARGB.multiplyAlpha(color, alpha))), throughWalls);
    }

    public static void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, boolean throughWalls) {
        if (!valid(a) || !valid(b) || !valid(c) || !valid(d)) return;
        depth(Gizmos.rect(a, b, c, d, GizmoStyle.fill(color)), throughWalls);
    }

    public static void filledFace(AABB box, Direction face, int color, boolean throughWalls) {
        if (box == null || face == null) return;
        depth(Gizmos.rect(new Vec3(box.minX, box.minY, box.minZ), new Vec3(box.maxX, box.maxY, box.maxZ),
                face, GizmoStyle.fill(color)), throughWalls);
    }

    public static void box(AABB box, int outlineColor, int fillColor, float width, boolean throughWalls) {
        if (box == null || !Float.isFinite(width) || width < 0f) return;
        depth(Gizmos.cuboid(box, GizmoStyle.strokeAndFill(outlineColor, width, fillColor)), throughWalls);
    }

    public static void circle(Vec3 center, Vec3 normal, double radius, int segments,
                              int outlineColor, int fillColor, float width, boolean throughWalls) {
        if (!valid(center) || !valid(normal) || normal.lengthSqr() < 1e-10 || !Double.isFinite(radius) || radius <= 0) return;
        Vec3 n = normal.normalize();
        Vec3 reference = Math.abs(n.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 horizontal = n.cross(reference).normalize().scale(radius);
        Vec3 vertical = n.cross(horizontal).normalize().scale(radius);
        polygon(center, horizontal, vertical, circleVertices(Math.clamp(segments, 3, 128)), outlineColor, fillColor, width, throughWalls);
    }

    public static void sphere(Vec3 center, double radius, int segments, int rings, int color, float width, boolean throughWalls) {
        if (!valid(center) || !Double.isFinite(radius) || radius <= 0 || !Float.isFinite(width) || width <= 0) return;
        int slices = Math.clamp(segments, 6, 64);
        int bands = Math.clamp(rings, 3, 32);
        Vec3[][] vertices = new Vec3[bands + 1][slices];
        for (int latitude = 0; latitude <= bands; latitude++) {
            double angle = Math.PI * latitude / bands;
            double y = radius * Math.cos(angle);
            double ringRadius = radius * Math.sin(angle);
            for (int longitude = 0; longitude < slices; longitude++) {
                double phase = Math.PI * 2 * longitude / slices;
                vertices[latitude][longitude] = center.add(Math.cos(phase) * ringRadius, y, Math.sin(phase) * ringRadius);
            }
        }
        depth(Gizmos.addGizmo((primitives, alpha) -> {
            int actualColor = ARGB.multiplyAlpha(color, alpha);
            for (int latitude = 0; latitude <= bands; latitude++) {
                for (int longitude = 0; longitude < slices; longitude++) {
                    if (latitude > 0 && latitude < bands) {
                        primitives.addLine(vertices[latitude][longitude], vertices[latitude][(longitude + 1) % slices], actualColor, width);
                    }
                    if (latitude < bands) primitives.addLine(vertices[latitude][longitude], vertices[latitude + 1][longitude], actualColor, width);
                }
            }
        }), throughWalls);
    }

    public static void billboard(Vec3 center, float width, float height, float rotation, int color,
                                 ParticleShape shape, boolean throughWalls) {
        if (!projectionReady || !valid(center) || !Float.isFinite(width) || !Float.isFinite(height)
                || !Float.isFinite(rotation) || width <= 0f || height <= 0f || shape == null) return;
        double cosine = Math.cos(rotation);
        double sine = Math.sin(rotation);
        Vec3 horizontal = cameraRight.scale(cosine).add(cameraUp.scale(sine)).scale(width * 0.5);
        Vec3 vertical = cameraUp.scale(cosine).subtract(cameraRight.scale(sine)).scale(height * 0.5);
        float[][] vertices = switch (shape) {
            case QUAD -> QUAD;
            case STAR -> STAR;
            case HEART -> HEART;
            case CIRCLE, BLOOM -> CIRCLE;
        };
        if (shape == ParticleShape.BLOOM) {
            for (int layer = 5; layer >= 1; layer--) {
                double ratio = layer / 5.0;
                int layerColor = ARGB.multiplyAlpha(color, 0.1f + (1f - (float) ratio) * 0.25f);
                polygon(center, horizontal.scale(ratio), vertical.scale(ratio), vertices, 0, layerColor, 0f, throughWalls);
            }
        } else {
            polygon(center, horizontal, vertical, vertices, 0, color, 0f, throughWalls);
        }
    }

    private static void polygon(Vec3 center, Vec3 horizontal, Vec3 vertical, float[][] outline,
                                int stroke, int fill, float width, boolean throughWalls) {
        Vec3[] fan = new Vec3[outline.length + 2];
        fan[0] = center;
        for (int i = 0; i < outline.length; i++) {
            fan[i + 1] = center.add(horizontal.scale(outline[i][0])).add(vertical.scale(outline[i][1]));
        }
        fan[fan.length - 1] = fan[1];
        depth(Gizmos.addGizmo((primitives, alpha) -> {
            if ((fill >>> 24) != 0) primitives.addTriangleFan(fan, ARGB.multiplyAlpha(fill, alpha));
            if ((stroke >>> 24) != 0 && Float.isFinite(width) && width > 0f) {
                int color = ARGB.multiplyAlpha(stroke, alpha);
                for (int i = 1; i < fan.length - 1; i++) primitives.addLine(fan[i], fan[i + 1], color, width);
            }
        }), throughWalls);
    }

    private static float[][] circleVertices(int segments) {
        float[][] vertices = new float[segments][2];
        for (int i = 0; i < segments; i++) {
            double angle = Math.PI * 2 * i / segments;
            vertices[i][0] = (float) Math.cos(angle);
            vertices[i][1] = (float) Math.sin(angle);
        }
        return vertices;
    }

    private static float[][] starVertices() {
        float[][] vertices = new float[10][2];
        for (int i = 0; i < vertices.length; i++) {
            double angle = Math.PI * 0.5 + Math.PI * i / 5;
            float radius = i % 2 == 0 ? 1f : 0.45f;
            vertices[i][0] = (float) Math.cos(angle) * radius;
            vertices[i][1] = (float) Math.sin(angle) * radius;
        }
        return vertices;
    }

    private static float[][] heartVertices() {
        float[][] vertices = new float[32][2];
        for (int i = 0; i < vertices.length; i++) {
            double angle = -Math.PI * 2 * i / vertices.length;
            vertices[i][0] = (float) Math.pow(Math.sin(angle), 3);
            vertices[i][1] = (float) ((13 * Math.cos(angle) - 5 * Math.cos(2 * angle)
                    - 2 * Math.cos(3 * angle) - Math.cos(4 * angle)) / 17.0);
        }
        return vertices;
    }

    static boolean valid(Vec3 position) {
        return position != null && Double.isFinite(position.x) && Double.isFinite(position.y) && Double.isFinite(position.z);
    }

    private static void depth(GizmoProperties properties, boolean throughWalls) {
        if (throughWalls) properties.setAlwaysOnTop();
    }

    public enum ParticleShape {
        QUAD, CIRCLE, STAR, HEART, BLOOM
    }

    public record Frame(Vec3 cameraPosition, float partialTick) {
    }

    public static final class Registration implements AutoCloseable {
        private final Consumer<Frame> renderer;
        private boolean closed;

        private Registration(Consumer<Frame> renderer) {
            this.renderer = renderer;
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            RENDERERS.remove(this);
        }
    }

    public record ScreenPoint(float x, float y) {
    }
}
