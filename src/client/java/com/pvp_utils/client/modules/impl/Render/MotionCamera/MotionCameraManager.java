package com.pvp_utils.client.modules.impl.Render.MotionCamera;

import com.pvp_utils.Config;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class MotionCameraManager {
    private static final double STILL_EPSILON_SQR = 0.000004;
    private static final double MAX_FRAME_TIME_SECONDS = 0.05;
    private static final float DISTANCE_EPSILON = 0.001f;
    private static final float PULL_DISTANCE_SPEED = 0.015f;

    private static Vec3 anchor;
    private static Entity trackedEntity;
    private static long lastUpdateNanos;
    private static float currentCameraDistance;

    private MotionCameraManager() {
    }

    public static boolean active(boolean detached, Entity entity) {
        return Config.motionCamera && detached && entity instanceof LocalPlayer;
    }

    public static boolean transitioning() {
        return Config.motionCamera && currentCameraDistance > DISTANCE_EPSILON;
    }

    public static Vec3 updateAnchor(Entity entity, float partialTick) {
        Vec3 target = entity.getEyePosition(partialTick);
        if (trackedEntity != entity || anchor == null) {
            trackedEntity = entity;
            anchor = target;
            lastUpdateNanos = System.nanoTime();
            return anchor;
        }

        Vec3 delta = target.subtract(anchor);
        double distanceSqr = delta.lengthSqr();
        if (distanceSqr < STILL_EPSILON_SQR) {
            return anchor;
        }

        double dt = frameTimeSeconds();
        double distance = Math.sqrt(distanceSqr);
        double slider = clamp(Config.motionCameraFollowSpeed, 0.0f, 1.0f);
        double curved = slider * slider * slider;
        double baseRate = 0.18 + curved * 14.0;
        double distanceRate = Math.min(distance, 6.0) * (0.35 + curved * 4.5);
        double factor = 1.0 - Math.exp(-(baseRate + distanceRate) * dt);
        anchor = anchor.add(delta.scale(factor));
        return anchor;
    }

    public static float cameraDistance() {
        return clamp(Config.motionCameraDistance, 1.0f, 8.0f);
    }

    public static float smoothDistance(float partialTick, boolean detached) {
        float target = Config.motionCamera && detached ? cameraDistance() : 0.0f;
        float speed = PULL_DISTANCE_SPEED * Math.max(0.0f, partialTick) * 10.0f;
        currentCameraDistance += (target - currentCameraDistance) * speed;
        if (Math.abs(currentCameraDistance - target) < DISTANCE_EPSILON) {
            currentCameraDistance = target;
        }
        return currentCameraDistance;
    }

    public static void reset() {
        anchor = null;
        trackedEntity = null;
        lastUpdateNanos = 0L;
        currentCameraDistance = 0.0f;
    }

    private static double frameTimeSeconds() {
        long now = System.nanoTime();
        if (lastUpdateNanos == 0L) {
            lastUpdateNanos = now;
            return 1.0 / 60.0;
        }
        double dt = (now - lastUpdateNanos) / 1_000_000_000.0;
        lastUpdateNanos = now;
        return Math.max(0.0, Math.min(MAX_FRAME_TIME_SECONDS, dt));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
