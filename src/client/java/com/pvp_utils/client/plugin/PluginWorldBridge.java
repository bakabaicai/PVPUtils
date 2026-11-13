package com.pvp_utils.client.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.pvp_utils.client.render.chroma.world.ChromaProjection;
import com.pvp_utils.client.render.chroma.world.ChromaWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.PriorityQueue;

final class PluginWorldBridge {
    private static final int MAX_ENTITIES = 128;
    private static final int MAX_SCANNED = 32_768;
    private static final Comparator<Candidate> NEAREST = Comparator.comparingDouble(Candidate::distanceSquared)
            .thenComparingInt(candidate -> candidate.entity().getId());
    private final Thread owner = Thread.currentThread();
    private final IdentityHashMap<Entity, JsonObject> metadata = new IdentityHashMap<>();
    private final IdentityHashMap<Entity, JsonObject> projected = new IdentityHashMap<>();
    private final LinkedHashMap<Query, String> responses = new LinkedHashMap<>();
    private ClientLevel level;
    private LocalPlayer player;
    private ChromaWorld.ProjectionFrame frame;
    private List<Candidate> nearest;
    private boolean failed;

    void tick() {
        metadata.clear();
        invalidate();
    }

    void clear() {
        level = null;
        player = null;
        metadata.clear();
        invalidate();
    }

    private void invalidate() {
        nearest = null;
        frame = null;
        failed = false;
        responses.clear();
        projected.clear();
    }

    private boolean synchronize() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("World bridge called from another thread");
        Minecraft client = Minecraft.getInstance();
        if (level != client.level || player != client.player) {
            clear();
            level = client.level;
            player = client.player;
        }
        ChromaWorld.ProjectionFrame current = ChromaWorld.projectionFrame();
        if (frame != current) {
            invalidate();
            frame = current;
        }
        return level != null && player != null && !client.getWindow().isMinimized();
    }

    String status() {
        boolean available = synchronize();
        JsonObject result = new JsonObject();
        result.addProperty("allowed", true);
        result.addProperty("available", available);
        result.addProperty("projectionReady", available && frame != null);
        result.add("dimension", available ? new com.google.gson.JsonPrimitive(level.dimension().identifier().toString()) : JsonNull.INSTANCE);
        result.add("frameId", available && frame != null ? new com.google.gson.JsonPrimitive(frame.id()) : JsonNull.INSTANCE);
        return result.toString();
    }

    String entities(double distance, int limit, Runnable budget) {
        if (!synchronize()) return "[]";
        distance = Math.clamp(distance, 1, 512);
        limit = Math.clamp(limit, 1, MAX_ENTITIES);
        Query query = new Query(distance, limit);
        String cached = responses.get(query);
        if (cached != null) return cached;
        select(budget);
        JsonArray result = new JsonArray();
        double squared = distance * distance;
        for (Candidate candidate : nearest) {
            budget.run();
            if (candidate.distanceSquared() > squared || result.size() >= limit) break;
            Entity entity = candidate.entity();
            if (entity.isRemoved() || entity.isInvisibleTo(player)) continue;
            JsonObject data = projected.get(entity);
            if (data == null) {
                data = snapshot(candidate);
                if (data == null) continue;
                projected.put(entity, data);
            }
            result.add(data);
        }
        String json = result.toString();
        if (responses.size() >= 16) responses.remove(responses.keySet().iterator().next());
        responses.put(query, json);
        return json;
    }

    private JsonObject snapshot(Candidate candidate) {
        Entity entity = candidate.entity();
        float yaw = entity.getYRot(candidate.partialTick());
        if (!Float.isFinite(yaw)) return null;
        AABB bounds = entity.getBoundingBox().move(candidate.x() - entity.getX(),
                candidate.y() - entity.getY(), candidate.z() - entity.getZ());
        if (!Double.isFinite(bounds.minX) || !Double.isFinite(bounds.minY) || !Double.isFinite(bounds.minZ)
                || !Double.isFinite(bounds.maxX) || !Double.isFinite(bounds.maxY) || !Double.isFinite(bounds.maxZ)) return null;
        if (!metadata.containsKey(entity) && metadata.size() >= 512) metadata.clear();
        JsonObject data = metadata.computeIfAbsent(entity, PluginWorldBridge::metadata).deepCopy();
        data.addProperty("x", candidate.x());
        data.addProperty("y", candidate.y());
        data.addProperty("z", candidate.z());
        data.addProperty("yaw", yaw);
        data.addProperty("distance", Math.sqrt(candidate.distanceSquared()));
        data.addProperty("width", bounds.getXsize());
        data.addProperty("height", bounds.getYsize());
        JsonObject box = new JsonObject();
        box.addProperty("minX", bounds.minX);
        box.addProperty("minY", bounds.minY);
        box.addProperty("minZ", bounds.minZ);
        box.addProperty("maxX", bounds.maxX);
        box.addProperty("maxY", bounds.maxY);
        box.addProperty("maxZ", bounds.maxZ);
        data.add("bounds", box);
        ChromaProjection.Bounds screen = frame == null ? null : frame.projection().projectBounds(
                bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ);
        data.add("screen", screen == null ? JsonNull.INSTANCE : screen(screen));
        return data;
    }

    private void select(Runnable budget) {
        if (failed) throw new PluginRuntime.ScriptLimitException("World entity scan exceeds limit");
        if (nearest != null) return;
        PriorityQueue<Candidate> selected = new PriorityQueue<>(MAX_ENTITIES, NEAREST.reversed());
        float playerTick = partialTick(player);
        double px = interpolate(player.xOld, player.getX(), playerTick);
        double py = interpolate(player.yOld, player.getY(), playerTick);
        double pz = interpolate(player.zOld, player.getZ(), playerTick);
        int scanned = 0;
        long deadline = System.nanoTime() + 8_000_000L;
        try {
            for (Entity entity : level.entitiesForRendering()) {
                if (++scanned > MAX_SCANNED) throw new PluginRuntime.ScriptLimitException("World entity scan exceeds limit");
                if ((scanned & 63) == 0) {
                    budget.run();
                    if (System.nanoTime() - deadline >= 0) throw new PluginRuntime.ScriptLimitException("World entity scan exceeds limit");
                }
                if (entity == player || entity.isRemoved() || entity.isSpectator() || entity.isInvisibleTo(player)) continue;
                float partialTick = partialTick(entity);
                double x = interpolate(entity.xOld, entity.getX(), partialTick);
                double y = interpolate(entity.yOld, entity.getY(), partialTick);
                double z = interpolate(entity.zOld, entity.getZ(), partialTick);
                double dx = x - px;
                double dy = y - py;
                double dz = z - pz;
                double distance = dx * dx + dy * dy + dz * dz;
                if (!Double.isFinite(distance) || distance > 512 * 512) continue;
                if (selected.size() == MAX_ENTITIES) {
                    Candidate last = selected.peek();
                    int comparison = Double.compare(distance, last.distanceSquared());
                    if (comparison > 0 || comparison == 0 && entity.getId() >= last.entity().getId()) continue;
                    selected.poll();
                }
                selected.add(new Candidate(entity, x, y, z, partialTick, distance));
            }
            budget.run();
            List<Candidate> ordered = new ArrayList<>(selected);
            ordered.sort(NEAREST);
            nearest = List.copyOf(ordered);
        } catch (PluginRuntime.ScriptLimitException error) {
            failed = true;
            throw error;
        }
    }

    String project(double x, double y, double z) {
        if (!synchronize() || frame == null) return "null";
        ChromaProjection.Point point = frame.projection().project(x, y, z);
        return point == null ? "null" : point(point).toString();
    }

    String projectMany(double[] coordinates, Runnable budget) {
        boolean available = synchronize() && frame != null;
        JsonArray result = new JsonArray();
        for (int index = 0; index < coordinates.length; index += 3) {
            budget.run();
            ChromaProjection.Point point = available ? frame.projection().project(
                    coordinates[index], coordinates[index + 1], coordinates[index + 2]) : null;
            result.add(point == null ? JsonNull.INSTANCE : point(point));
        }
        return result.toString();
    }

    private float partialTick(Entity entity) {
        if (frame == null) return 1;
        return level.tickRateManager().isEntityFrozen(entity) ? frame.frozenPartialTick() : frame.partialTick();
    }

    private static double interpolate(double from, double to, float partialTick) {
        return from + (to - from) * partialTick;
    }

    private static JsonObject metadata(Entity entity) {
        JsonObject data = new JsonObject();
        ItemEntity dropped = entity instanceof ItemEntity item ? item : null;
        LivingEntity living = entity instanceof LivingEntity value ? value : null;
        data.addProperty("id", entity.getId());
        data.addProperty("uuid", entity.getUUID().toString());
        String name = dropped == null ? entity.getName().getString() : dropped.getItem().getHoverName().getString();
        data.addProperty("name", name.substring(0, Math.min(name.length(), 128)));
        data.addProperty("type", entity instanceof Player ? "player" : living != null ? "mob" : dropped != null ? "item" : "other");
        data.addProperty("key", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        data.addProperty("alive", entity.isAlive());
        data.addProperty("invisible", entity.isInvisible());
        data.addProperty("player", entity instanceof Player);
        data.addProperty("health", living == null ? null : finite(living.getHealth()));
        data.addProperty("maxHealth", living == null ? null : finite(living.getMaxHealth()));
        data.addProperty("armor", living == null ? null : living.getArmorValue());
        data.addProperty("count", dropped == null ? null : dropped.getItem().getCount());
        data.addProperty("item", dropped == null ? null : BuiltInRegistries.ITEM.getKey(dropped.getItem().getItem()).toString());
        return data;
    }

    private static Number finite(float value) {
        return Float.isFinite(value) ? value : null;
    }

    private static JsonObject point(ChromaProjection.Point point) {
        JsonObject data = new JsonObject();
        data.addProperty("x", point.x());
        data.addProperty("y", point.y());
        data.addProperty("depth", point.depth());
        data.addProperty("onScreen", point.onScreen());
        return data;
    }

    private static JsonObject screen(ChromaProjection.Bounds bounds) {
        JsonObject data = new JsonObject();
        data.addProperty("minX", bounds.minX());
        data.addProperty("minY", bounds.minY());
        data.addProperty("maxX", bounds.maxX());
        data.addProperty("maxY", bounds.maxY());
        data.addProperty("depth", bounds.depth());
        data.addProperty("onScreen", bounds.onScreen());
        return data;
    }

    private record Candidate(Entity entity, double x, double y, double z, float partialTick, double distanceSquared) {
    }

    private record Query(double distance, int limit) {
    }
}
