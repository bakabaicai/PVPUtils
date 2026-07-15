package com.pvp_utils.client.render.world;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class CustomBlockOutlineRenderer {
    private static BlockPos targetBlockPos;
    private static BlockState targetBlockState;
    private static Vec3 renderedPosition;
    private static float visibility;
    private static long lastRenderTime;

    private CustomBlockOutlineRenderer() {
    }

    public static void render() {
        if (!Config.customBlockOutline) {
            reset();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            reset();
            return;
        }

        boolean hasTarget = minecraft.hitResult instanceof BlockHitResult && minecraft.hitResult.getType() == HitResult.Type.BLOCK;
        if (hasTarget) {
            BlockHitResult hit = (BlockHitResult) minecraft.hitResult;
            targetBlockPos = hit.getBlockPos();
            targetBlockState = level.getBlockState(targetBlockPos);
        }
        if (targetBlockPos == null || targetBlockState == null) {
            return;
        }

        Vec3 targetPosition = new Vec3(targetBlockPos.getX(), targetBlockPos.getY(), targetBlockPos.getZ());
        long now = System.nanoTime();
        double deltaSeconds = lastRenderTime == 0L ? 0.0 : Math.min(0.1, (now - lastRenderTime) / 1_000_000_000.0);
        lastRenderTime = now;
        if (renderedPosition == null) {
            renderedPosition = targetPosition;
        }

        if (!Config.customBlockOutlineAnimation) {
            visibility = hasTarget ? 1.0f : 0.0f;
            renderedPosition = targetPosition;
        } else {
            visibility = approach(visibility, hasTarget ? 1.0f : 0.0f, Config.customBlockOutlineAnimationSpeed, deltaSeconds);
            renderedPosition = approach(renderedPosition, targetPosition, Config.customBlockOutlineMoveSpeed, deltaSeconds);
        }
        if (visibility <= 0.001f) {
            return;
        }

        float scale = 0.1f + visibility * 0.9f;
        int outlineColor = color(Config.customBlockOutlineRed, Config.customBlockOutlineGreen, Config.customBlockOutlineBlue, Math.round(Config.customBlockOutlineAlpha * visibility));
        int fillColor = color(Config.customBlockOutlineFillRed, Config.customBlockOutlineFillGreen, Config.customBlockOutlineFillBlue, Math.round(Config.customBlockOutlineFillAlpha * visibility));
        for (AABB box : targetBlockState.getShape(level, targetBlockPos).toAabbs()) {
            AABB worldBox = box.move(renderedPosition.x, renderedPosition.y, renderedPosition.z);
            worldBox = scale(worldBox, scale);
            if (Config.customBlockOutlineFill) {
                WorldRender.filledBox(worldBox, fillColor, true);
            }
            renderBoxLines(worldBox, outlineColor);
        }
    }

    private static int color(int red, int green, int blue, int alpha) {
        return ((alpha & 0xFF) << 24)
                | ((red & 0xFF) << 16)
                | ((green & 0xFF) << 8)
                | (blue & 0xFF);
    }

    private static void renderBoxLines(AABB box, int color) {
        float width = Config.customBlockOutlineWidth;
        WorldRender.line(new Vec3(box.minX, box.minY, box.minZ), new Vec3(box.maxX, box.minY, box.minZ), color, width, true);
        WorldRender.line(new Vec3(box.maxX, box.minY, box.minZ), new Vec3(box.maxX, box.minY, box.maxZ), color, width, true);
        WorldRender.line(new Vec3(box.maxX, box.minY, box.maxZ), new Vec3(box.minX, box.minY, box.maxZ), color, width, true);
        WorldRender.line(new Vec3(box.minX, box.minY, box.maxZ), new Vec3(box.minX, box.minY, box.minZ), color, width, true);
        WorldRender.line(new Vec3(box.minX, box.maxY, box.minZ), new Vec3(box.maxX, box.maxY, box.minZ), color, width, true);
        WorldRender.line(new Vec3(box.maxX, box.maxY, box.minZ), new Vec3(box.maxX, box.maxY, box.maxZ), color, width, true);
        WorldRender.line(new Vec3(box.maxX, box.maxY, box.maxZ), new Vec3(box.minX, box.maxY, box.maxZ), color, width, true);
        WorldRender.line(new Vec3(box.minX, box.maxY, box.maxZ), new Vec3(box.minX, box.maxY, box.minZ), color, width, true);
        WorldRender.line(new Vec3(box.minX, box.minY, box.minZ), new Vec3(box.minX, box.maxY, box.minZ), color, width, true);
        WorldRender.line(new Vec3(box.maxX, box.minY, box.minZ), new Vec3(box.maxX, box.maxY, box.minZ), color, width, true);
        WorldRender.line(new Vec3(box.maxX, box.minY, box.maxZ), new Vec3(box.maxX, box.maxY, box.maxZ), color, width, true);
        WorldRender.line(new Vec3(box.minX, box.minY, box.maxZ), new Vec3(box.minX, box.maxY, box.maxZ), color, width, true);
    }

    private static float approach(float value, float target, float speed, double deltaSeconds) {
        float progress = easeOutCubic((float) (1.0 - Math.exp(-speed * deltaSeconds)));
        return value + (target - value) * progress;
    }

    private static Vec3 approach(Vec3 value, Vec3 target, float speed, double deltaSeconds) {
        float progress = easeOutCubic((float) (1.0 - Math.exp(-speed * deltaSeconds)));
        return value.lerp(target, progress);
    }

    private static float easeOutCubic(float value) {
        float inverse = 1.0f - value;
        return 1.0f - inverse * inverse * inverse;
    }

    private static AABB scale(AABB box, float scale) {
        double centerX = (box.minX + box.maxX) * 0.5;
        double centerY = (box.minY + box.maxY) * 0.5;
        double centerZ = (box.minZ + box.maxZ) * 0.5;
        double halfX = (box.maxX - box.minX) * scale * 0.5;
        double halfY = (box.maxY - box.minY) * scale * 0.5;
        double halfZ = (box.maxZ - box.minZ) * scale * 0.5;
        return new AABB(centerX - halfX, centerY - halfY, centerZ - halfZ, centerX + halfX, centerY + halfY, centerZ + halfZ);
    }

    private static void reset() {
        targetBlockPos = null;
        targetBlockState = null;
        renderedPosition = null;
        visibility = 0.0f;
        lastRenderTime = 0L;
    }
}
