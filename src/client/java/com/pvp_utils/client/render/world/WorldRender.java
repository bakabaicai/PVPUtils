package com.pvp_utils.client.render.world;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

public final class WorldRender {
    private static final Matrix4f MODEL_VIEW = new Matrix4f();
    private static final Matrix4f PROJECTION = new Matrix4f();
    private static Vec3 cameraPosition = Vec3.ZERO;
    private static boolean projectionReady;

    private WorldRender() {
    }

    public static void capture(Camera camera, Matrix4fc modelViewMatrix, Matrix4fc projectionMatrix) {
        if (camera == null) {
            projectionReady = false;
            return;
        }
        cameraPosition = camera.position();
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
        if (client == null) {
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

    public record ScreenPoint(float x, float y) {
    }
}
