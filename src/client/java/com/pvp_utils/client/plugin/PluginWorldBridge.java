package com.pvp_utils.client.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.pvp_utils.client.plugin.PluginPlayerArguments.integer;

final class PluginWorldBridge {
    private static final double MAX_RADIUS = 64.0;
    private static final int MAX_LIMIT = 64;

    String call(String operation, Object[] args) {
        if (isMainThread() || RenderSystem.isOnRenderThread()) {
            return execute(operation, args);
        }
        Minecraft client = Minecraft.getInstance();
        CompletableFuture<String> future = new CompletableFuture<>();
        client.execute(() -> {
            try {
                future.complete(execute(operation, args));
            } catch (Throwable error) {
                future.completeExceptionally(error);
            }
        });
        try {
            return future.get(2L, TimeUnit.SECONDS);
        } catch (ExecutionException error) {
            if (error.getCause() instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("World query failed", error.getCause());
        } catch (Exception error) {
            throw new IllegalStateException("World query timed out", error);
        }
    }

    private static boolean isMainThread() {
        Minecraft client = Minecraft.getInstance();
        return client != null && client.isSameThread();
    }

    private String execute(String operation, Object[] args) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        LocalPlayer self = client.player;
        if (level == null || self == null) {
            return switch (operation) {
                case "entities", "players" -> "[]";
                default -> "null";
            };
        }
        return switch (operation) {
            case "entities" -> entities(level, self, args);
            case "entity" -> entity(level, self, args);
            case "players" -> players(level, self, args);
            case "block" -> block(level, args);
            case "time" -> time(level);
            case "dimension" -> "\"" + level.dimension().identifier() + "\"";
            case "snapshot" -> snapshot(level, self);
            default -> throw new IllegalArgumentException("Unknown world operation: " + operation);
        };
    }

    private static String entities(ClientLevel level, LocalPlayer self, Object[] args) {
        double radius = radius(args);
        int limit = limit(args);
        String filter = args.length > 2 ? PluginRuntime.string(args, 2) : "";
        JsonArray result = new JsonArray();
        for (Entity entity : level.entitiesForRendering()) {
            if (result.size() >= limit) {
                break;
            }
            if (entity == self) {
                continue;
            }
            String type = typeOf(entity);
            if (!filter.isEmpty() && !type.startsWith(filter)) {
                continue;
            }
            if (self.distanceTo(entity) > radius) {
                continue;
            }
            result.add(view(entity, self));
        }
        return result.toString();
    }

    private static String entity(ClientLevel level, LocalPlayer self, Object[] args) {
        Entity target = level.getEntity(integer(args, 0, 0, Integer.MAX_VALUE));
        return target == null ? "null" : view(target, self).toString();
    }

    private static String players(ClientLevel level, LocalPlayer self, Object[] args) {
        double radius = radius(args);
        int limit = limit(args);
        JsonArray result = new JsonArray();
        for (AbstractClientPlayer player : level.players()) {
            if (result.size() >= limit) {
                break;
            }
            if (player != self && self.distanceTo(player) > radius) {
                continue;
            }
            result.add(view(player, self));
        }
        return result.toString();
    }

    private static String block(ClientLevel level, Object[] args) {
        BlockPos pos = new BlockPos(integer(args, 0, -30_000_000, 30_000_000),
                integer(args, 1, -2048, 2048), integer(args, 2, -30_000_000, 30_000_000));
        BlockState state = level.getBlockState(pos);
        JsonObject data = new JsonObject();
        data.addProperty("x", pos.getX());
        data.addProperty("y", pos.getY());
        data.addProperty("z", pos.getZ());
        data.addProperty("block", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        data.addProperty("air", state.isAir());
        data.addProperty("loaded", level.isLoaded(pos));
        return data.toString();
    }

    private static String time(ClientLevel level) {
        long dayTime = level.getDayTime();
        long ticks = Math.floorMod(dayTime, 24000L);
        JsonObject data = new JsonObject();
        data.addProperty("dayTime", dayTime);
        data.addProperty("gameTime", level.getGameTime());
        data.addProperty("day", dayTime / 24000L);
        data.addProperty("timeOfDay", ticks);
        data.addProperty("night", ticks >= 13000L && ticks <= 23000L);
        return data.toString();
    }

    private static String snapshot(ClientLevel level, LocalPlayer self) {
        JsonObject data = new JsonObject();
        data.addProperty("dimension", level.dimension().identifier().toString());
        data.addProperty("difficulty", level.getDifficulty().name());
        data.addProperty("entityCount", level.getEntityCount());
        data.addProperty("playerCount", level.players().size());
        data.addProperty("dayTime", level.getDayTime());
        data.addProperty("rain", level.isRaining());
        data.addProperty("thunder", level.isThundering());
        data.addProperty("renderDistance", Minecraft.getInstance().options.renderDistance().get());
        data.add("self", view(self, self));
        return data.toString();
    }

    private static JsonObject view(Entity entity, LocalPlayer self) {
        JsonObject data = new JsonObject();
        data.addProperty("id", entity.getId());
        data.addProperty("uuid", entity.getUUID().toString());
        data.addProperty("type", typeOf(entity));
        data.addProperty("name", entity.getName().getString());
        data.addProperty("x", entity.getX());
        data.addProperty("y", entity.getY());
        data.addProperty("z", entity.getZ());
        data.addProperty("yaw", entity.getYRot());
        data.addProperty("pitch", entity.getXRot());
        data.addProperty("vx", entity.getDeltaMovement().x);
        data.addProperty("vy", entity.getDeltaMovement().y);
        data.addProperty("vz", entity.getDeltaMovement().z);
        data.addProperty("distance", self.distanceTo(entity));
        data.addProperty("alive", entity.isAlive());
        data.addProperty("removed", entity.isRemoved());
        data.addProperty("player", entity instanceof Player);
        data.addProperty("self", entity == self);
        data.addProperty("sneaking", entity.isShiftKeyDown());
        data.addProperty("sprinting", entity.isSprinting());
        data.addProperty("onGround", entity.onGround());
        data.addProperty("width", entity.getBbWidth());
        data.addProperty("height", entity.getBbHeight());
        if (entity instanceof LivingEntity living) {
            data.addProperty("health", living.getHealth());
            data.addProperty("maxHealth", living.getMaxHealth());
            data.addProperty("armor", living.getArmorValue());
            data.addProperty("usingItem", living.isUsingItem());
            data.addProperty("hurtTime", living.hurtTime);
        }
        return data;
    }

    private static String typeOf(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }

    private static double radius(Object[] args) {
        if (args.length == 0) {
            return 16.0;
        }
        double value = PluginPlayerArguments.number(args, 0, MAX_RADIUS);
        return Math.min(MAX_RADIUS, Math.max(0.0, value));
    }

    private static int limit(Object[] args) {
        return args.length > 1 ? integer(args, 1, 1, MAX_LIMIT) : 16;
    }
}

