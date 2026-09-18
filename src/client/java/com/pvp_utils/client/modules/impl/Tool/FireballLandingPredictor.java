package com.pvp_utils.client.modules.impl.Tool;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.world.WorldRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class FireballLandingPredictor {
    private static final int TRAJECTORY_COLOR = 0xCCFFAA00;
    private static final int IMPACT_OUTLINE_COLOR = 0xFFFF5500;
    private static final int IMPACT_FILL_COLOR = 0x55FF5500;
    private static final int EXPLOSION_RANGE_OUTLINE_COLOR = 0xCCFF5500;
    private static final int EXPLOSION_RANGE_FILL_COLOR = 0x33FF5500;
    private static final int MAX_TICKS = 500;
    private static final double MAX_DISTANCE_SQR = 256.0 * 256.0;
    private static final int EXPLOSION_RADIUS = 2;

    private FireballLandingPredictor() {
    }

    public static void render() {
        if (!Config.fireballLandingPredict) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LargeFireball fireball)) {
                continue;
            }

            Vec3 velocity = fireball.getDeltaMovement();
            if (velocity.lengthSqr() < 0.0001) {
                continue;
            }

            Vec3 position = fireball.position();
            for (int tick = 0; tick < MAX_TICKS && position.distanceToSqr(fireball.position()) <= MAX_DISTANCE_SQR; tick++) {
                FluidState fluidState = level.getFluidState(BlockPos.containing(position));
                double inertia = fluidState.is(FluidTags.WATER) ? 0.8 : 0.95;
                velocity = velocity.add(velocity.normalize().scale(fireball.accelerationPower)).scale(inertia);

                Vec3 nextPosition = position.add(velocity);
                BlockHitResult hit = level.clip(new ClipContext(position, nextPosition, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, fireball));
                Vec3 segmentEnd = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : nextPosition;
                WorldRender.line(position, segmentEnd, TRAJECTORY_COLOR, 2.0f);
                if (hit.getType() == HitResult.Type.BLOCK) {
                    WorldRender.box(new AABB(hit.getBlockPos()), IMPACT_OUTLINE_COLOR, IMPACT_FILL_COLOR);
                    renderExplosionRange(level, hit.getBlockPos());
                    break;
                }
                position = nextPosition;
            }
        }
    }

    private static void renderExplosionRange(ClientLevel level, BlockPos impactPos) {
        for (BlockPos blockPos : BlockPos.betweenClosed(impactPos.offset(-EXPLOSION_RADIUS, -EXPLOSION_RADIUS, -EXPLOSION_RADIUS), impactPos.offset(EXPLOSION_RADIUS, EXPLOSION_RADIUS, EXPLOSION_RADIUS))) {
            BlockState blockState = level.getBlockState(blockPos);
            if (blockState.isCollisionShapeFullBlock(level, blockPos)) {
                WorldRender.box(new AABB(blockPos), EXPLOSION_RANGE_OUTLINE_COLOR, EXPLOSION_RANGE_FILL_COLOR);
            }
        }
    }
}
