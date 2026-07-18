package com.pvp_utils.client.modules.impl.Tool;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.world.WorldRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class ArrowTrajectoryPredictor {
    private static final int TRAJECTORY_COLOR = 0xCC55D8FF;
    private static final int IMPACT_OUTLINE_COLOR = 0xFF55D8FF;
    private static final int IMPACT_FILL_COLOR = 0x3355D8FF;
    private static final int NEARBY_ENTITY_OUTLINE_COLOR = 0xFFFF5555;
    private static final int NEARBY_ENTITY_FILL_COLOR = 0x33FF5555;
    private static final int HIT_ENTITY_OUTLINE_COLOR = 0xFF55FF55;
    private static final int HIT_ENTITY_FILL_COLOR = 0x3355FF55;
    private static final int INCOMING_TRAJECTORY_COLOR = 0xCCFF3333;
    private static final int INCOMING_HIT_OUTLINE_COLOR = 0xFFFF3333;
    private static final int INCOMING_HIT_FILL_COLOR = 0x33FF3333;
    private static final int MAX_TICKS = 200;
    private static final double MAX_DISTANCE_SQR = 256.0 * 256.0;
    private static final double GRAVITY = 0.05;
    private static final double AIR_INERTIA = 0.99;
    private static final double WATER_INERTIA = 0.6;
    private static final double NEARBY_ENTITY_RANGE = 10.0;

    private ArrowTrajectoryPredictor() {
    }

    public static void render(float partialTick) {
        if (!Config.arrowTrajectoryPredict) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            return;
        }

        renderLocalTrajectory(level, player, partialTick);
        if (Config.arrowTrajectoryPredictOtherPlayers) {
            renderIncomingTrajectories(level, player, partialTick);
        }
    }

    private static void renderLocalTrajectory(ClientLevel level, LocalPlayer player, float partialTick) {
        if (!player.isUsingItem()) {
            return;
        }

        ItemStack bow = player.getUseItem();
        if (!(bow.getItem() instanceof BowItem)) {
            return;
        }

        int useTicks = bow.getUseDuration(player) - player.getUseItemRemainingTicks();
        float power = BowItem.getPowerForTime(useTicks);
        if (power <= 0.0f) {
            return;
        }

        Vec3 viewVector = player.getViewVector(partialTick);
        Vec3 position = player.getEyePosition(partialTick).add(viewVector.scale(0.16));
        Vec3 velocity = viewVector.scale(power * 3.0f);
        for (int tick = 0; tick < MAX_TICKS && position.distanceToSqr(player.position()) <= MAX_DISTANCE_SQR; tick++) {
            Vec3 nextPosition = position.add(velocity);
            BlockHitResult blockHit = level.clip(new ClipContext(position, nextPosition, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            EntityHit entityHit = findEntityHit(level, player, position, nextPosition, tick + 0.5, partialTick);
            if (entityHit != null) {
                double estimatedArrivalTicks = arrivalTicks(position, nextPosition, entityHit.location(), tick);
                entityHit = findEntityHit(level, player, position, nextPosition, estimatedArrivalTicks, partialTick);
            }
            boolean hitBlock = blockHit.getType() == HitResult.Type.BLOCK;
            boolean hitEntityFirst = entityHit != null && (!hitBlock
                    || position.distanceToSqr(entityHit.location()) < position.distanceToSqr(blockHit.getLocation()));
            Vec3 segmentEnd = hitEntityFirst ? entityHit.location() : hitBlock ? blockHit.getLocation() : nextPosition;
            double arrivalTicks = arrivalTicks(position, nextPosition, segmentEnd, tick);
            WorldRender.line(position, segmentEnd, TRAJECTORY_COLOR, 2.0f);
            if (hitEntityFirst) {
                if (Config.arrowTrajectoryPredictEntity) {
                    WorldRender.box(predictedBox(entityHit.entity(), arrivalTicks, partialTick).inflate(0.05), HIT_ENTITY_OUTLINE_COLOR, HIT_ENTITY_FILL_COLOR);
                    renderNearbyEntities(level, player, segmentEnd, arrivalTicks, entityHit.entity(), partialTick);
                }
                break;
            }
            if (hitBlock) {
                if (Config.arrowTrajectoryPredictBlock) {
                    WorldRender.box(new AABB(blockHit.getBlockPos()), IMPACT_OUTLINE_COLOR, IMPACT_FILL_COLOR);
                }
                if (Config.arrowTrajectoryPredictEntity) {
                    renderNearbyEntities(level, player, segmentEnd, arrivalTicks, null, partialTick);
                }
                break;
            }

            position = nextPosition;
            double inertia = level.getFluidState(BlockPos.containing(position)).is(FluidTags.WATER) ? WATER_INERTIA : AIR_INERTIA;
            velocity = velocity.scale(inertia).add(0.0, -GRAVITY, 0.0);
        }
    }

    private static void renderIncomingTrajectories(ClientLevel level, LocalPlayer player, float partialTick) {
        AABB playerBox = interpolatedBox(player, partialTick).inflate(0.3);
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof Player shooter) || shooter == player || !shooter.isUsingItem()) {
                continue;
            }

            ItemStack bow = shooter.getUseItem();
            if (!(bow.getItem() instanceof BowItem)) {
                continue;
            }

            int useTicks = bow.getUseDuration(shooter) - shooter.getUseItemRemainingTicks();
            float power = BowItem.getPowerForTime(useTicks);
            if (power <= 0.0f) {
                continue;
            }

            Vec3 viewVector = shooter.getViewVector(partialTick);
            Vec3 position = shooter.getEyePosition(partialTick).add(viewVector.scale(0.16));
            IncomingTrajectory trajectory = findIncomingTrajectory(level, shooter, position, viewVector.scale(power * 3.0f), playerBox);
            if (trajectory == null) {
                continue;
            }

            for (Segment segment : trajectory.segments()) {
                WorldRender.line(segment.from(), segment.to(), INCOMING_TRAJECTORY_COLOR, 1.5f);
            }
            WorldRender.box(playerBox.inflate(0.05), INCOMING_HIT_OUTLINE_COLOR, INCOMING_HIT_FILL_COLOR);
        }
    }

    private static IncomingTrajectory findIncomingTrajectory(ClientLevel level, Player shooter, Vec3 position, Vec3 velocity, AABB playerBox) {
        List<Segment> segments = new ArrayList<>();
        for (int tick = 0; tick < MAX_TICKS && position.distanceToSqr(shooter.position()) <= MAX_DISTANCE_SQR; tick++) {
            Vec3 nextPosition = position.add(velocity);
            BlockHitResult blockHit = level.clip(new ClipContext(position, nextPosition, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
            var playerHit = playerBox.clip(position, nextPosition);
            boolean hitBlock = blockHit.getType() == HitResult.Type.BLOCK;
            boolean hitPlayerFirst = playerHit.isPresent() && (!hitBlock
                    || position.distanceToSqr(playerHit.get()) < position.distanceToSqr(blockHit.getLocation()));
            Vec3 segmentEnd = hitPlayerFirst ? playerHit.get() : hitBlock ? blockHit.getLocation() : nextPosition;
            segments.add(new Segment(position, segmentEnd));
            if (hitPlayerFirst) {
                return new IncomingTrajectory(segments);
            }
            if (hitBlock) {
                return null;
            }

            position = nextPosition;
            double inertia = level.getFluidState(BlockPos.containing(position)).is(FluidTags.WATER) ? WATER_INERTIA : AIR_INERTIA;
            velocity = velocity.scale(inertia).add(0.0, -GRAVITY, 0.0);
        }
        return null;
    }

    private static EntityHit findEntityHit(ClientLevel level, LocalPlayer player, Vec3 start, Vec3 end, double flightTicks, float partialTick) {
        EntityHit closest = null;
        AABB trajectoryArea = new AABB(start, end).inflate(1.0);
        for (Entity entity : level.entitiesForRendering()) {
            if (entity == player || !entity.isAlive() || !entity.isPickable()) {
                continue;
            }
            AABB currentBox = interpolatedBox(entity, partialTick);
            AABB box = predictedBox(entity, flightTicks, partialTick);
            if (!currentBox.intersects(trajectoryArea) && !box.intersects(trajectoryArea)) {
                continue;
            }
            var intersection = box.inflate(0.3).clip(start, end);
            if (intersection.isEmpty()) {
                continue;
            }
            EntityHit candidate = new EntityHit(entity, intersection.get());
            if (closest == null || start.distanceToSqr(candidate.location()) < start.distanceToSqr(closest.location())) {
                closest = candidate;
            }
        }
        return closest;
    }

    private static void renderNearbyEntities(ClientLevel level, LocalPlayer player, Vec3 impactPosition, double flightTicks, Entity hitEntity, float partialTick) {
        AABB nearbyArea = new AABB(impactPosition, impactPosition).inflate(NEARBY_ENTITY_RANGE);
        for (Entity entity : level.entitiesForRendering()) {
            if (entity == player || entity == hitEntity || !entity.isAlive() || !entity.isPickable()) {
                continue;
            }
            AABB currentBox = interpolatedBox(entity, partialTick);
            AABB predictedBox = predictedBox(entity, flightTicks, partialTick);
            if (currentBox.intersects(nearbyArea) || predictedBox.intersects(nearbyArea)) {
                WorldRender.box(predictedBox.inflate(0.05), NEARBY_ENTITY_OUTLINE_COLOR, NEARBY_ENTITY_FILL_COLOR);
            }
        }
    }

    private static AABB predictedBox(Entity entity, double flightTicks, float partialTick) {
        AABB currentBox = interpolatedBox(entity, partialTick);
        if (!Config.arrowTrajectoryPredictEntityMovement) {
            return currentBox;
        }
        Vec3 observedVelocity = new Vec3(entity.getX() - entity.xo, entity.getY() - entity.yo, entity.getZ() - entity.zo);
        return currentBox.move(observedVelocity.scale(flightTicks));
    }

    private static AABB interpolatedBox(Entity entity, float partialTick) {
        Vec3 observedVelocity = new Vec3(entity.getX() - entity.xo, entity.getY() - entity.yo, entity.getZ() - entity.zo);
        return entity.getBoundingBox().move(observedVelocity.scale(partialTick - 1.0f));
    }

    private static double arrivalTicks(Vec3 start, Vec3 end, Vec3 arrivalPosition, int tick) {
        double segmentLength = start.distanceTo(end);
        if (segmentLength <= 0.0) {
            return tick;
        }
        double fraction = Math.max(0.0, Math.min(1.0, start.distanceTo(arrivalPosition) / segmentLength));
        return tick + fraction;
    }

    private record EntityHit(Entity entity, Vec3 location) {
    }

    private record Segment(Vec3 from, Vec3 to) {
    }

    private record IncomingTrajectory(List<Segment> segments) {
    }
}
