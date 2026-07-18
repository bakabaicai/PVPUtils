package com.pvp_utils.client.modules.impl.Tool;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.world.WorldRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class ProjectileTrajectoryPredictor {
    // Bow trajectory colors (unchanged from the old arrow predictor).
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

    // Snowball: white line, green impact face.
    private static final int SNOWBALL_TRAJECTORY_COLOR = 0xFFFFFFFF;
    private static final int SNOWBALL_IMPACT_FACE_COLOR = 0xFF55FF55;

    // Ender pearl: purple line, single grid face (green flat / red side).
    private static final int PEARL_TRAJECTORY_COLOR = 0xFFAA33FF;
    private static final int PEARL_FLAT_FACE_COLOR = 0xFF55FF55;
    private static final int PEARL_SIDE_FACE_COLOR = 0xFFFF5555;

    private static final int MAX_TICKS = 200;
    private static final double MAX_DISTANCE_SQR = 256.0 * 256.0;

    // Arrow physics (bow).
    private static final double ARROW_GRAVITY = 0.05;
    private static final double ARROW_AIR_INERTIA = 0.99;
    private static final double ARROW_WATER_INERTIA = 0.6;

    // Throwable item physics (snowball / ender pearl), taken from ThrowableProjectile.
    private static final double THROWABLE_GRAVITY = 0.03;
    private static final double THROWABLE_AIR_INERTIA = 0.99;
    private static final double THROWABLE_WATER_INERTIA = 0.8;
    private static final float THROWABLE_SHOOT_POWER = 1.5f;
    // Spawn height offset: eye height - 0.1 (ThrowableItemProjectile constructor).
    private static final double THROWABLE_SPAWN_EYE_OFFSET = 0.1;
    // Snowball / ender pearl hitbox is 0.25 x 0.25 (EntityType.sized).
    private static final double THROWABLE_HALF_WIDTH = 0.125;
    private static final double THROWABLE_HEIGHT = 0.25;
    // Sub-step length for the swept hitbox collision check.
    private static final double SWEEP_STEP = 0.05;
    // Small rightward render offset so the trajectory looks like it leaves the hand.
    private static final double HAND_RIGHT_OFFSET = 0.15;
    // Ticks over which the hand offset fades to zero so the line converges without a kink.
    private static final int HAND_OFFSET_FADE_TICKS = 10;

    private static final double NEARBY_ENTITY_RANGE = 10.0;

    private ProjectileTrajectoryPredictor() {
    }

    public static void render(float partialTick) {
        if (!Config.projectileTrajectoryPredict) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            return;
        }

        renderLocalBowTrajectory(level, player, partialTick);
        renderLocalThrowableTrajectory(level, player, partialTick);
        if (Config.projectileTrajectoryPredictOtherPlayers) {
            renderIncomingTrajectories(level, player, partialTick);
        }
    }

    // ---------------------------------------------------------------------
    // Bow (unchanged behaviour).
    // ---------------------------------------------------------------------

    private static void renderLocalBowTrajectory(ClientLevel level, LocalPlayer player, float partialTick) {
        if (!Config.projectileTrajectoryPredictBow || !player.isUsingItem()) {
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
                if (Config.projectileTrajectoryPredictEntity) {
                    WorldRender.box(predictedBox(entityHit.entity(), arrivalTicks, partialTick).inflate(0.05), HIT_ENTITY_OUTLINE_COLOR, HIT_ENTITY_FILL_COLOR);
                    renderNearbyEntities(level, player, segmentEnd, arrivalTicks, entityHit.entity(), partialTick);
                }
                break;
            }
            if (hitBlock) {
                if (Config.projectileTrajectoryPredictBlock) {
                    WorldRender.box(new AABB(blockHit.getBlockPos()), IMPACT_OUTLINE_COLOR, IMPACT_FILL_COLOR);
                }
                if (Config.projectileTrajectoryPredictEntity) {
                    renderNearbyEntities(level, player, segmentEnd, arrivalTicks, null, partialTick);
                }
                break;
            }

            position = nextPosition;
            double inertia = level.getFluidState(BlockPos.containing(position)).is(FluidTags.WATER) ? ARROW_WATER_INERTIA : ARROW_AIR_INERTIA;
            velocity = velocity.scale(inertia).add(0.0, -ARROW_GRAVITY, 0.0);
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
            double inertia = level.getFluidState(BlockPos.containing(position)).is(FluidTags.WATER) ? ARROW_WATER_INERTIA : ARROW_AIR_INERTIA;
            velocity = velocity.scale(inertia).add(0.0, -ARROW_GRAVITY, 0.0);
        }
        return null;
    }

    // ---------------------------------------------------------------------
    // Throwable items: snowball (held) and ender pearl (held).
    // ---------------------------------------------------------------------

    private static void renderLocalThrowableTrajectory(ClientLevel level, LocalPlayer player, float partialTick) {
        ItemStack held = heldThrowable(player);
        if (held == null) {
            return;
        }
        boolean isPearl = held.getItem() instanceof EnderpearlItem;
        if (isPearl && !Config.projectileTrajectoryPredictEnderPearl) {
            return;
        }
        if (!isPearl && !Config.projectileTrajectoryPredictSnowball) {
            return;
        }

        // Reproduce shootFromRotation: direction from the player rotation, scaled by
        // the shoot power, plus the player's own movement (Y only while airborne).
        float xRot = player.getViewXRot(partialTick);
        float yRot = player.getViewYRot(partialTick);
        Vec3 direction = directionFromRotation(xRot, yRot);
        Vec3 knownMovement = player.getKnownMovement();
        Vec3 velocity = direction.scale(THROWABLE_SHOOT_POWER)
                .add(knownMovement.x, player.onGround() ? 0.0 : knownMovement.y, knownMovement.z);

        // Physics starts from the real spawn point (eye height - 0.1).
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 position = new Vec3(
                Mth_lerp(partialTick, player.xo, player.getX()),
                eye.y - THROWABLE_SPAWN_EYE_OFFSET,
                Mth_lerp(partialTick, player.zo, player.getZ())
        );

        int lineColor = isPearl ? PEARL_TRAJECTORY_COLOR : SNOWBALL_TRAJECTORY_COLOR;
        // The whole line is nudged toward the right hand and the offset decays with distance,
        // so it looks like it leaves the hand and smoothly converges onto the true trajectory
        // (no visible kink). Physics still uses the true spawn point.
        Vec3 handOffset = rightVector(yRot).scale(HAND_RIGHT_OFFSET);

        for (int tick = 0; tick < MAX_TICKS && position.distanceToSqr(player.position()) <= MAX_DISTANCE_SQR; tick++) {
            // Match ThrowableProjectile.tick() exactly: apply gravity, then inertia, then move.
            double inertia = level.getFluidState(BlockPos.containing(position)).is(FluidTags.WATER) ? THROWABLE_WATER_INERTIA : THROWABLE_AIR_INERTIA;
            velocity = velocity.add(0.0, -THROWABLE_GRAVITY, 0.0).scale(inertia);

            Vec3 nextPosition = position.add(velocity);
            EntityHit entityHit = findEntityHit(level, player, position, nextPosition, tick + 0.5, partialTick);
            SweepHit sweepHit = sweepBlockHit(level, player, position, nextPosition);
            boolean hitBlock = sweepHit != null;
            boolean hitEntityFirst = entityHit != null && (!hitBlock
                    || position.distanceToSqr(entityHit.location()) < position.distanceToSqr(sweepHit.stop()));
            Vec3 segmentEnd = hitEntityFirst ? entityHit.location() : hitBlock ? sweepHit.stop() : nextPosition;
            Vec3 lineStart = position.add(handOffset.scale(handWeight(tick)));
            Vec3 lineEnd = (hitBlock || hitEntityFirst) ? segmentEnd : segmentEnd.add(handOffset.scale(handWeight(tick + 1)));
            WorldRender.line(lineStart, lineEnd, lineColor, 2.0f);

            if (hitEntityFirst) {
                break;
            }
            if (hitBlock) {
                renderThrowableImpact(isPearl, sweepHit);
                break;
            }

            position = nextPosition;
        }
    }

    // Weight of the hand offset for a given step: full at the start, fading to zero over the
    // first few ticks so the rendered line converges onto the true trajectory without a kink.
    private static double handWeight(int tick) {
        return tick >= HAND_OFFSET_FADE_TICKS ? 0.0 : 1.0 - (double) tick / HAND_OFFSET_FADE_TICKS;
    }

    private static void renderThrowableImpact(boolean isPearl, SweepHit hit) {
        AABB blockBox = new AABB(hit.blockPos());
        Direction face = hit.face();
        if (isPearl) {
            // Ender pearl: grid-aligned single face on the block it comes to rest against.
            // Flat top/bottom surfaces are green, side walls are red.
            boolean flat = face == Direction.UP || face == Direction.DOWN;
            WorldRender.face(blockBox, face, flat ? PEARL_FLAT_FACE_COLOR : PEARL_SIDE_FACE_COLOR, 3.0f);
            return;
        }
        // Snowball: single green face on the surface it hits.
        WorldRender.face(blockBox, face, SNOWBALL_IMPACT_FACE_COLOR, 3.0f);
    }

    // Sweeps the projectile's own hitbox from start to end in small sub-steps and returns
    // the first blocking position plus the block/face it rests against. Using the hitbox
    // (rather than a centre ray) keeps the marker flush against the surface it stops on.
    private static SweepHit sweepBlockHit(ClientLevel level, LocalPlayer player, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        double distance = delta.length();
        if (distance <= 1.0e-6) {
            return null;
        }
        int steps = Math.max(1, (int) Math.ceil(distance / SWEEP_STEP));
        Vec3 previous = start;
        for (int i = 1; i <= steps; i++) {
            Vec3 sample = start.add(delta.scale((double) i / steps));
            if (level.noCollision(player, hitboxAt(sample))) {
                previous = sample;
                continue;
            }
            // Resolve which axis is blocked by testing each axis independently.
            Direction face = resolveFace(level, player, previous, sample);
            Vec3 stop = previous;
            BlockPos blockPos = blockAgainst(stop, face);
            return new SweepHit(stop, blockPos, face);
        }
        return null;
    }

    private static Direction resolveFace(ClientLevel level, LocalPlayer player, Vec3 free, Vec3 blocked) {
        double dx = blocked.x - free.x;
        double dy = blocked.y - free.y;
        double dz = blocked.z - free.z;
        boolean yBlocked = !level.noCollision(player, hitboxAt(new Vec3(free.x, blocked.y, free.z)));
        boolean xBlocked = !level.noCollision(player, hitboxAt(new Vec3(blocked.x, free.y, free.z)));
        boolean zBlocked = !level.noCollision(player, hitboxAt(new Vec3(free.x, free.y, blocked.z)));
        if (yBlocked) {
            return dy < 0.0 ? Direction.UP : Direction.DOWN;
        }
        if (xBlocked) {
            return dx < 0.0 ? Direction.EAST : Direction.WEST;
        }
        if (zBlocked) {
            return dz < 0.0 ? Direction.SOUTH : Direction.NORTH;
        }
        // Corner case: fall back to the dominant movement axis.
        double ax = Math.abs(dx);
        double ay = Math.abs(dy);
        double az = Math.abs(dz);
        if (ay >= ax && ay >= az) {
            return dy < 0.0 ? Direction.UP : Direction.DOWN;
        }
        if (ax >= az) {
            return dx < 0.0 ? Direction.EAST : Direction.WEST;
        }
        return dz < 0.0 ? Direction.SOUTH : Direction.NORTH;
    }

    // The solid block the resting hitbox sits against, found by nudging just past the face.
    private static BlockPos blockAgainst(Vec3 stop, Direction face) {
        Vec3 probe = switch (face) {
            case UP -> new Vec3(stop.x, stop.y - 0.02, stop.z);
            case DOWN -> new Vec3(stop.x, stop.y + THROWABLE_HEIGHT + 0.02, stop.z);
            case EAST -> new Vec3(stop.x - THROWABLE_HALF_WIDTH - 0.02, stop.y + THROWABLE_HEIGHT * 0.5, stop.z);
            case WEST -> new Vec3(stop.x + THROWABLE_HALF_WIDTH + 0.02, stop.y + THROWABLE_HEIGHT * 0.5, stop.z);
            case SOUTH -> new Vec3(stop.x, stop.y + THROWABLE_HEIGHT * 0.5, stop.z - THROWABLE_HALF_WIDTH - 0.02);
            case NORTH -> new Vec3(stop.x, stop.y + THROWABLE_HEIGHT * 0.5, stop.z + THROWABLE_HALF_WIDTH + 0.02);
        };
        return BlockPos.containing(probe);
    }

    // Projectile hitbox centred horizontally on the position; y is the bottom (feet).
    private static AABB hitboxAt(Vec3 center) {
        return new AABB(
                center.x - THROWABLE_HALF_WIDTH, center.y, center.z - THROWABLE_HALF_WIDTH,
                center.x + THROWABLE_HALF_WIDTH, center.y + THROWABLE_HEIGHT, center.z + THROWABLE_HALF_WIDTH
        );
    }

    // Rightward horizontal unit vector for a given yaw (forward = (-sinYaw, .., cosYaw)).
    private static Vec3 rightVector(float yRot) {
        float yRad = yRot * ((float) Math.PI / 180.0f);
        return new Vec3(-Math.cos(yRad), 0.0, -Math.sin(yRad));
    }

    private static ItemStack heldThrowable(LocalPlayer player) {
        ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (isThrowable(main)) {
            return main;
        }
        ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
        if (isThrowable(off)) {
            return off;
        }
        return null;
    }

    private static boolean isThrowable(ItemStack stack) {
        return stack != null && !stack.isEmpty() && (stack.getItem() instanceof SnowballItem || stack.getItem() instanceof EnderpearlItem);
    }

    private static Vec3 directionFromRotation(float xRot, float yRot) {
        float xRad = xRot * ((float) Math.PI / 180.0f);
        float yRad = yRot * ((float) Math.PI / 180.0f);
        double x = -Math.sin(yRad) * Math.cos(xRad);
        double y = -Math.sin(xRad);
        double z = Math.cos(yRad) * Math.cos(xRad);
        return new Vec3(x, y, z).normalize();
    }

    private static double Mth_lerp(float delta, double start, double end) {
        return start + delta * (end - start);
    }

    // ---------------------------------------------------------------------
    // Shared entity helpers.
    // ---------------------------------------------------------------------

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
        if (!Config.projectileTrajectoryPredictEntityMovement) {
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

    private record SweepHit(Vec3 stop, BlockPos blockPos, Direction face) {
    }

    private record Segment(Vec3 from, Vec3 to) {
    }

    private record IncomingTrajectory(List<Segment> segments) {
    }
}
