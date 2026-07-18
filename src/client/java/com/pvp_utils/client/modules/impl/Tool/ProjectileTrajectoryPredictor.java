package com.pvp_utils.client.modules.impl.Tool;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.world.WorldRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
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
import java.util.Map;
import java.util.List;
import java.util.WeakHashMap;

public final class ProjectileTrajectoryPredictor {
    // Bow trajectory colors (unchanged from the old arrow predictor).
    private static final int TRAJECTORY_COLOR = 0xCC55D8FF;
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

    private static final double PROJECTILE_SPREAD = 0.0075;
    private static final int SPREAD_SAMPLE_COUNT = 5;

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
    // Small rightward render offset so the trajectory looks like it leaves the hand.
    private static final double HAND_RIGHT_OFFSET = 0.15;
    // Ticks over which the hand offset fades to zero so the line converges without a kink.
    private static final int HAND_OFFSET_FADE_TICKS = 10;

    private static final double NEARBY_ENTITY_RANGE = 10.0;
    private static final int MOTION_GRACE_TICKS = 2;
    private static final double MOTION_SMOOTHING = 0.35;
    private static final Map<Entity, MotionSample> ENTITY_MOTION_SAMPLES = new WeakHashMap<>();
    private static SpreadSampleCache spreadSampleCache;

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
        if (!Config.projectileTrajectoryPredictBow) {
            return;
        }

        ItemStack bow = player.isUsingItem() ? player.getUseItem() : heldBow(player);
        if (!(bow.getItem() instanceof BowItem)) {
            return;
        }

        float power = player.isUsingItem()
                ? BowItem.getPowerForTime(bow.getUseDuration(player) - player.getUseItemRemainingTicks())
                : 1.0f;
        if (power <= 0.0f) {
            return;
        }

        Vec3 viewVector = player.getViewVector(partialTick);
        Vec3 position = arrowSpawnPosition(player, partialTick);
        Vec3 velocity = launchVelocity(player, viewVector, power * 3.0f);
        double predictionSpeed = velocity.length();
        Vec3 handOffset = rightVector(player.getViewYRot(partialTick)).scale(HAND_RIGHT_OFFSET);
        for (int tick = 0; tick < MAX_TICKS && position.distanceToSqr(player.position()) <= MAX_DISTANCE_SQR; tick++) {
            Vec3 nextPosition = position.add(velocity);
            BlockHitResult blockHit = level.clip(new ClipContext(position, nextPosition, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            EntityHit entityHit = findEntityHit(level, player, position, nextPosition, tick + 0.5, predictionSpeed, partialTick);
            boolean hitBlock = blockHit.getType() == HitResult.Type.BLOCK;
            boolean hitEntityFirst = entityHit != null && (!hitBlock
                    || position.distanceToSqr(entityHit.location()) < position.distanceToSqr(blockHit.getLocation()));
            Vec3 segmentEnd = hitEntityFirst ? entityHit.location() : hitBlock ? blockHit.getLocation() : nextPosition;
            Vec3 lineStart = position.add(handOffset.scale(handWeight(tick)));
            Vec3 lineEnd = (hitBlock || hitEntityFirst) ? segmentEnd : segmentEnd.add(handOffset.scale(handWeight(tick + 1)));
            WorldRender.line(lineStart, lineEnd, TRAJECTORY_COLOR, 2.5f);
            if (hitEntityFirst) {
                List<TrajectoryImpact> impacts = sampleImpacts(level, player, arrowSpawnPosition(player, partialTick), viewVector, power * 3.0f, false, partialTick,
                        new TrajectoryImpact(entityHit, null));
                if (Config.projectileTrajectoryPredictEntity) {
                    renderEntityBox(player, entityHit.entity(), predictionSpeed, partialTick, hitChance(impacts, entityHit.entity()));
                    renderNearbyEntities(level, player, segmentEnd, entityHit.entity(), predictionSpeed, partialTick, impacts);
                }
                break;
            }
            if (hitBlock) {
                if (Config.projectileTrajectoryPredictBlock) {
                    WorldRender.face(new AABB(blockHit.getBlockPos()), blockHit.getDirection(), SNOWBALL_IMPACT_FACE_COLOR, 3.0f);
                }
                if (Config.projectileTrajectoryPredictEntity) {
                    List<TrajectoryImpact> impacts = sampleImpacts(level, player, arrowSpawnPosition(player, partialTick), viewVector, power * 3.0f, false, partialTick,
                            new TrajectoryImpact(null, blockHit));
                    renderNearbyEntities(level, player, segmentEnd, null, predictionSpeed, partialTick, impacts);
                }
                break;
            }

            position = nextPosition;
            double inertia = level.getFluidState(BlockPos.containing(position)).is(FluidTags.WATER) ? ARROW_WATER_INERTIA : ARROW_AIR_INERTIA;
            velocity = velocity.scale(inertia).add(0.0, -ARROW_GRAVITY, 0.0);
        }
    }

    private static void renderIncomingTrajectories(ClientLevel level, LocalPlayer player, float partialTick) {
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
            Vec3 position = arrowSpawnPosition(shooter, partialTick);
            IncomingTrajectory trajectory = findIncomingTrajectory(level, shooter, player, position, launchVelocity(shooter, viewVector, power * 3.0f), partialTick);
            if (trajectory == null) {
                continue;
            }

            for (Segment segment : trajectory.segments()) {
                WorldRender.line(segment.from(), segment.to(), INCOMING_TRAJECTORY_COLOR, 2.0f);
            }
            WorldRender.box(interpolatedBox(player, partialTick).inflate(0.05), INCOMING_HIT_OUTLINE_COLOR, INCOMING_HIT_FILL_COLOR);
        }
    }

    private static IncomingTrajectory findIncomingTrajectory(ClientLevel level, Player shooter, LocalPlayer player, Vec3 position, Vec3 velocity, float partialTick) {
        List<Segment> segments = new ArrayList<>();
        for (int tick = 0; tick < MAX_TICKS && position.distanceToSqr(shooter.position()) <= MAX_DISTANCE_SQR; tick++) {
            Vec3 nextPosition = position.add(velocity);
            BlockHitResult blockHit = level.clip(new ClipContext(position, nextPosition, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
            var playerHit = interpolatedBox(player, partialTick).inflate(entityCollisionMargin(tick + 0.5)).clip(position, nextPosition);
            EntityHit entityHit = findIncomingEntityHit(level, shooter, position, nextPosition, tick + 0.5, partialTick);
            boolean hitBlock = blockHit.getType() == HitResult.Type.BLOCK;
            boolean hitPlayerFirst = playerHit.isPresent() && (entityHit == null
                    || position.distanceToSqr(playerHit.get()) < position.distanceToSqr(entityHit.location()))
                    && (!hitBlock || position.distanceToSqr(playerHit.get()) < position.distanceToSqr(blockHit.getLocation()));
            boolean hitEntityFirst = entityHit != null && (!hitBlock
                    || position.distanceToSqr(entityHit.location()) < position.distanceToSqr(blockHit.getLocation()));
            Vec3 segmentEnd = hitPlayerFirst ? playerHit.get() : hitEntityFirst ? entityHit.location() : hitBlock ? blockHit.getLocation() : nextPosition;
            segments.add(new Segment(position, segmentEnd));
            if (hitPlayerFirst) {
                return new IncomingTrajectory(segments);
            }
            if (hitEntityFirst || hitBlock) {
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
        Vec3 velocity = launchVelocity(player, direction, THROWABLE_SHOOT_POWER);
        double predictionSpeed = velocity.length();

        // Physics starts from the real spawn point (eye height - 0.1).
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 spawnPosition = new Vec3(
                Mth_lerp(partialTick, player.xo, player.getX()),
                eye.y - THROWABLE_SPAWN_EYE_OFFSET,
                Mth_lerp(partialTick, player.zo, player.getZ())
        );
        Vec3 position = spawnPosition;

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
            EntityHit entityHit = findEntityHit(level, player, position, nextPosition, tick + 0.5, predictionSpeed, partialTick);
            BlockHitResult blockHit = level.clip(new ClipContext(position, nextPosition, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            boolean hitBlock = blockHit.getType() == HitResult.Type.BLOCK;
            boolean hitEntityFirst = entityHit != null && (!hitBlock
                    || position.distanceToSqr(entityHit.location()) < position.distanceToSqr(blockHit.getLocation()));
            Vec3 segmentEnd = hitEntityFirst ? entityHit.location() : hitBlock ? blockHit.getLocation() : nextPosition;
            Vec3 lineStart = position.add(handOffset.scale(handWeight(tick)));
            Vec3 lineEnd = (hitBlock || hitEntityFirst) ? segmentEnd : segmentEnd.add(handOffset.scale(handWeight(tick + 1)));
            WorldRender.line(lineStart, lineEnd, lineColor, 2.5f);

            if (hitEntityFirst) {
                if (!isPearl && Config.projectileTrajectoryPredictEntity) {
                    List<TrajectoryImpact> impacts = sampleImpacts(level, player, spawnPosition, direction, THROWABLE_SHOOT_POWER, true, partialTick,
                            new TrajectoryImpact(entityHit, null));
                    renderEntityBox(player, entityHit.entity(), predictionSpeed, partialTick, hitChance(impacts, entityHit.entity()));
                    renderNearbyEntities(level, player, segmentEnd, entityHit.entity(), predictionSpeed, partialTick, impacts);
                }
                break;
            }
            if (hitBlock) {
                renderThrowableImpact(isPearl, blockHit);
                if (!isPearl && Config.projectileTrajectoryPredictEntity) {
                    List<TrajectoryImpact> impacts = sampleImpacts(level, player, spawnPosition, direction, THROWABLE_SHOOT_POWER, true, partialTick,
                            new TrajectoryImpact(null, blockHit));
                    renderNearbyEntities(level, player, segmentEnd, null, predictionSpeed, partialTick, impacts);
                }
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

    private static void renderThrowableImpact(boolean isPearl, BlockHitResult hit) {
        AABB blockBox = new AABB(hit.getBlockPos());
        if (isPearl) {
            boolean flat = hit.getDirection().getAxis().isVertical();
            WorldRender.face(blockBox, hit.getDirection(), flat ? PEARL_FLAT_FACE_COLOR : PEARL_SIDE_FACE_COLOR, 3.0f);
            return;
        }
        WorldRender.face(blockBox, hit.getDirection(), SNOWBALL_IMPACT_FACE_COLOR, 3.0f);
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

    private static Vec3 arrowSpawnPosition(Player shooter, float partialTick) {
        Vec3 eye = shooter.getEyePosition(partialTick);
        return new Vec3(
                Mth_lerp(partialTick, shooter.xo, shooter.getX()),
                eye.y - 0.1,
                Mth_lerp(partialTick, shooter.zo, shooter.getZ())
        );
    }

    private static Vec3 launchVelocity(Player shooter, Vec3 direction, double speed) {
        Vec3 knownMovement = shooter.getKnownMovement();
        return direction.scale(speed).add(knownMovement.x, shooter.onGround() ? 0.0 : knownMovement.y, knownMovement.z);
    }

    // ---------------------------------------------------------------------
    // Shared entity helpers.
    // ---------------------------------------------------------------------

    private static EntityHit findEntityHit(ClientLevel level, LocalPlayer player, Vec3 start, Vec3 end, double flightTicks, double projectileSpeed, float partialTick) {
        EntityHit closest = null;
        AABB trajectoryArea = new AABB(start, end).inflate(1.0);
        for (Entity entity : level.entitiesForRendering()) {
            if (entity == player || !entity.isAlive() || !entity.isPickable()
                    || entity instanceof Player target && !player.canHarmPlayer(target)) {
                continue;
            }
            AABB box = displayPredictedBox(player, entity, projectileSpeed, partialTick);
            if (!box.intersects(trajectoryArea)) {
                continue;
            }
            var intersection = box.inflate(entityCollisionMargin(flightTicks)).clip(start, end);
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

    private static EntityHit findIncomingEntityHit(ClientLevel level, Player shooter, Vec3 start, Vec3 end, double flightTicks, float partialTick) {
        EntityHit closest = null;
        AABB trajectoryArea = new AABB(start, end).inflate(1.0);
        for (Entity entity : level.entitiesForRendering()) {
            if (entity == shooter || !entity.isAlive() || !entity.isPickable()
                    || entity instanceof Player target && !shooter.canHarmPlayer(target)) {
                continue;
            }
            AABB box = interpolatedBox(entity, partialTick);
            if (!box.intersects(trajectoryArea)) {
                continue;
            }
            var intersection = box.inflate(entityCollisionMargin(flightTicks)).clip(start, end);
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

    private static double entityCollisionMargin(double flightTicks) {
        return Math.min(0.3, Math.max(0.0, flightTicks * 0.1));
    }

    private static List<TrajectoryImpact> sampleImpacts(ClientLevel level, LocalPlayer player, Vec3 spawnPosition, Vec3 direction,
                                                         double speed, boolean throwable, float partialTick, TrajectoryImpact centerImpact) {
        if (spreadSampleCache != null && spreadSampleCache.tick() == player.tickCount && spreadSampleCache.throwable() == throwable
                && sameImpact(spreadSampleCache.centerImpact(), centerImpact)) {
            return spreadSampleCache.impacts();
        }
        List<TrajectoryImpact> impacts = new ArrayList<>(SPREAD_SAMPLE_COUNT);
        impacts.add(centerImpact);
        Vec3 sideways = Math.abs(direction.y) > 0.99
                ? new Vec3(1.0, 0.0, 0.0)
                : new Vec3(direction.z, 0.0, -direction.x).normalize();
        Vec3 upwards = sideways.cross(direction).normalize();
        for (double[] offset : new double[][]{{2.0, 0.0}, {-2.0, 0.0}, {0.0, 2.0}, {0.0, -2.0}}) {
            Vec3 sampledDirection = direction.add(sideways.scale(offset[0] * PROJECTILE_SPREAD))
                    .add(upwards.scale(offset[1] * PROJECTILE_SPREAD)).normalize();
            TrajectoryImpact impact = simulateImpact(level, player, spawnPosition, sampledDirection, speed, throwable, partialTick);
            if (impact != null) {
                impacts.add(impact);
            }
        }
        spreadSampleCache = new SpreadSampleCache(player.tickCount, throwable, centerImpact, impacts);
        return impacts;
    }

    private static boolean sameImpact(TrajectoryImpact first, TrajectoryImpact second) {
        if (first.entityHit() != null || second.entityHit() != null) {
            return first.entityHit() != null && second.entityHit() != null && first.entityHit().entity() == second.entityHit().entity();
        }
        if (first.blockHit() == null || second.blockHit() == null) {
            return first.blockHit() == second.blockHit();
        }
        return first.blockHit().getBlockPos().equals(second.blockHit().getBlockPos())
                && first.blockHit().getDirection() == second.blockHit().getDirection();
    }

    private static TrajectoryImpact simulateImpact(ClientLevel level, LocalPlayer player, Vec3 spawnPosition, Vec3 direction,
                                                    double speed, boolean throwable, float partialTick) {
        Vec3 position = spawnPosition;
        Vec3 velocity = launchVelocity(player, direction, speed);
        double predictionSpeed = velocity.length();
        for (int tick = 0; tick < MAX_TICKS && position.distanceToSqr(player.position()) <= MAX_DISTANCE_SQR; tick++) {
            if (throwable) {
                double inertia = level.getFluidState(BlockPos.containing(position)).is(FluidTags.WATER) ? THROWABLE_WATER_INERTIA : THROWABLE_AIR_INERTIA;
                velocity = velocity.add(0.0, -THROWABLE_GRAVITY, 0.0).scale(inertia);
            }
            Vec3 nextPosition = position.add(velocity);
            EntityHit entityHit = findEntityHit(level, player, position, nextPosition, tick + 0.5, predictionSpeed, partialTick);
            BlockHitResult blockHit = level.clip(new ClipContext(position, nextPosition, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            boolean hitBlock = blockHit.getType() == HitResult.Type.BLOCK;
            boolean hitEntityFirst = entityHit != null && (!hitBlock
                    || position.distanceToSqr(entityHit.location()) < position.distanceToSqr(blockHit.getLocation()));
            if (hitEntityFirst) {
                return new TrajectoryImpact(entityHit, null);
            }
            if (hitBlock) {
                return new TrajectoryImpact(null, blockHit);
            }
            position = nextPosition;
            if (!throwable) {
                double inertia = level.getFluidState(BlockPos.containing(position)).is(FluidTags.WATER) ? ARROW_WATER_INERTIA : ARROW_AIR_INERTIA;
                velocity = velocity.scale(inertia).add(0.0, -ARROW_GRAVITY, 0.0);
            }
        }
        return null;
    }

    private static ItemStack heldBow(LocalPlayer player) {
        ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (main.getItem() instanceof BowItem) {
            return main;
        }
        ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
        return off.getItem() instanceof BowItem ? off : ItemStack.EMPTY;
    }

    private static void renderNearbyEntities(ClientLevel level, LocalPlayer player, Vec3 impactPosition, Entity hitEntity, double projectileSpeed,
                                             float partialTick, List<TrajectoryImpact> impacts) {
        AABB nearbyArea = new AABB(impactPosition, impactPosition).inflate(NEARBY_ENTITY_RANGE);
        for (Entity entity : level.entitiesForRendering()) {
            if (entity == player || entity == hitEntity || !entity.isAlive() || !entity.isPickable()) {
                continue;
            }
            AABB currentBox = interpolatedBox(entity, partialTick);
            AABB predictedBox = displayPredictedBox(player, entity, projectileSpeed, partialTick);
            if (currentBox.intersects(nearbyArea) || predictedBox.intersects(nearbyArea)) {
                renderEntityBox(player, entity, projectileSpeed, partialTick, hitChance(impacts, entity));
            }
        }
    }

    private static void renderEntityBox(LocalPlayer player, Entity entity, double projectileSpeed, float partialTick, double chance) {
        int rgb = interpolateColor(0xFF5555, 0x55FF55, chance);
        WorldRender.box(displayPredictedBox(player, entity, projectileSpeed, partialTick).inflate(0.05), 0xFF000000 | rgb, 0x33000000 | rgb);
    }

    private static double hitChance(List<TrajectoryImpact> impacts, Entity entity) {
        int hits = 0;
        for (TrajectoryImpact impact : impacts) {
            if (impact.entityHit() != null && impact.entityHit().entity() == entity) {
                hits++;
            }
        }
        return (double) hits / SPREAD_SAMPLE_COUNT;
    }

    private static int interpolateColor(int from, int to, double amount) {
        double progress = Math.clamp(amount, 0.0, 1.0);
        int red = (int) Math.round(((from >>> 16) & 0xFF) + (((to >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)) * progress);
        int green = (int) Math.round(((from >>> 8) & 0xFF) + (((to >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)) * progress);
        int blue = (int) Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * progress);
        return red << 16 | green << 8 | blue;
    }

    private static AABB predictedBox(Entity entity, double flightTicks, float partialTick) {
        AABB currentBox = interpolatedBox(entity, partialTick);
        if (!Config.projectileTrajectoryPredictEntityMovement) {
            return currentBox;
        }
        return currentBox.move(predictionVelocity(entity).scale(flightTicks));
    }

    // Smooth observed velocity once per entity tick. The same value drives the displayed box and
    // collision tests, so movement changes do not make either one jump independently.
    private static Vec3 predictionVelocity(Entity entity) {
        Vec3 observedVelocity = new Vec3(entity.getX() - entity.xo, entity.getY() - entity.yo, entity.getZ() - entity.zo);
        MotionSample sample = ENTITY_MOTION_SAMPLES.get(entity);
        if (sample == null || entity.tickCount - sample.tick() > MOTION_GRACE_TICKS) {
            ENTITY_MOTION_SAMPLES.put(entity, new MotionSample(observedVelocity, entity.tickCount));
            return observedVelocity;
        }
        if (sample.tick() == entity.tickCount) {
            return sample.velocity();
        }
        Vec3 smoothedVelocity = sample.velocity().scale(1.0 - MOTION_SMOOTHING).add(observedVelocity.scale(MOTION_SMOOTHING));
        ENTITY_MOTION_SAMPLES.put(entity, new MotionSample(smoothedVelocity, entity.tickCount));
        return smoothedVelocity;
    }

    // Both the displayed aim box and collision check use this stable target prediction.
    private static AABB displayPredictedBox(LocalPlayer player, Entity entity, double projectileSpeed, float partialTick) {
        AABB currentBox = interpolatedBox(entity, partialTick);
        if (!Config.projectileTrajectoryPredictEntityMovement || projectileSpeed <= 0.0) {
            return currentBox;
        }

        double distance = player.getEyePosition(partialTick).distanceTo(currentBox.getCenter());
        double inertiaDistanceLimit = projectileSpeed / (1.0 - ARROW_AIR_INERTIA);
        double flightTicks = distance >= inertiaDistanceLimit
                ? MAX_TICKS
                : Math.log(1.0 - distance * (1.0 - ARROW_AIR_INERTIA) / projectileSpeed) / Math.log(ARROW_AIR_INERTIA);
        return predictedBox(entity, Math.min(MAX_TICKS, Math.max(0.0, flightTicks)), partialTick);
    }

    private static AABB interpolatedBox(Entity entity, float partialTick) {
        Vec3 observedVelocity = new Vec3(entity.getX() - entity.xo, entity.getY() - entity.yo, entity.getZ() - entity.zo);
        return entity.getBoundingBox().move(observedVelocity.scale(partialTick - 1.0f));
    }

    private record EntityHit(Entity entity, Vec3 location) {
    }

    private record TrajectoryImpact(EntityHit entityHit, BlockHitResult blockHit) {
    }

    private record SpreadSampleCache(int tick, boolean throwable, TrajectoryImpact centerImpact, List<TrajectoryImpact> impacts) {
    }

    private record MotionSample(Vec3 velocity, int tick) {
    }

    private record Segment(Vec3 from, Vec3 to) {
    }

    private record IncomingTrajectory(List<Segment> segments) {
    }
}
