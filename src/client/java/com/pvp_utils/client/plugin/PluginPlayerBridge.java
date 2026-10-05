package com.pvp_utils.client.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Set;

import static com.pvp_utils.client.plugin.PluginPlayerArguments.integer;
import static com.pvp_utils.client.plugin.PluginPlayerArguments.number;

public final class PluginPlayerBridge {
    private static final Set<String> READS = Set.of("snapshot", "target", "getItems", "getSlot",
            "getSelectedSlot", "menuSnapshot");
    private final Thread owner = Thread.currentThread();
    private LocalPlayer player;
    private ClientLevel level;
    private AbstractContainerMenu menu;
    private final PluginPlayerSession session = new PluginPlayerSession();

    public void synchronize() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("Player bridge called from another thread");
        Minecraft client = Minecraft.getInstance();
        if (player != client.player || level != client.level) {
            player = client.player;
            level = client.level;
            menu = null;
        }
        AbstractContainerMenu current = player == null ? null : player.containerMenu;
        if (menu != current) {
            menu = current;
        }
        session.update(player, level, menu);
    }

    public String token() {
        synchronize();
        return session.playerToken();
    }

    public String snapshot() {
        return call(token(), "snapshot", new Object[0]);
    }

    public String call(String token, String operation, Object[] args) {
        synchronize();
        if (!session.validPlayer(token)) {
            return READS.contains(operation) ? "null" : result(false, "STALE_PLAYER");
        }
        PluginPlayerArguments.validate(operation, args);
        Minecraft client = Minecraft.getInstance();
        if (!READS.contains(operation)) {
            if (client.gameMode == null || !client.isWindowActive()) return result(false, "INACTIVE");
            if (!player.isAlive() || player.isSpectator()) return result(false, "PLAYER_UNAVAILABLE");
            if (client.screen != null && !operation.equals("click")
                    && !operation.equals("select") && !operation.equals("closeMenu")) {
                return result(false, "SCREEN_OPEN");
            }
        }
        return switch (operation) {
            case "snapshot" -> playerSnapshot(client).toString();
            case "target" -> target(client);
            case "getSelectedSlot" -> Integer.toString(player.getInventory().getSelectedSlot());
            case "getItems" -> {
                JsonArray items = new JsonArray();
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    JsonObject item = item(player.getInventory().getItem(i));
                    item.addProperty("slot", i);
                    items.add(item);
                }
                yield items.toString();
            }
            case "getSlot" -> {
                int slot = integer(args, 0, 0, player.getInventory().getContainerSize() - 1);
                JsonObject item = item(player.getInventory().getItem(slot));
                item.addProperty("slot", slot);
                yield item.toString();
            }
            case "select" -> {
                player.getInventory().setSelectedSlot(integer(args, 0, 0, 8));
                yield result(true, "OK");
            }
            case "menuSnapshot" -> {
                if (menu == null || args.length > 0 && !session.validMenu(PluginRuntime.string(args, 0))) yield "null";
                JsonObject data = new JsonObject();
                data.addProperty("token", session.menuToken());
                data.addProperty("containerId", menu.containerId);
                JsonArray slots = new JsonArray();
                for (int i = 0; i < menu.slots.size(); i++) {
                    JsonObject item = item(menu.slots.get(i).getItem());
                    item.addProperty("slot", i);
                    item.addProperty("inventorySlot", menu.slots.get(i).getContainerSlot());
                    slots.add(item);
                }
                data.add("slots", slots);
                data.add("carried", item(menu.getCarried()));
                yield data.toString();
            }
            case "click" -> click(client, args);
            case "setRotation" -> {
                float yaw = (float) (number(args, 0, 360000) % 360);
                float pitch = (float) number(args, 1, 90);
                player.setYRot(yaw);
                player.setXRot(pitch);
                yield result(true, "OK");
            }
            case "setVelocity" -> {
                double x = number(args, 0, 10);
                double y = number(args, 1, 10);
                double z = number(args, 2, 10);
                player.setDeltaMovement(x, y, z);
                yield result(true, "OK");
            }
            case "setSprinting" -> {
                if (args.length != 1 || !(args[0] instanceof Boolean sprint)) {
                    throw new IllegalArgumentException("setSprinting requires a boolean");
                }
                player.setSprinting(sprint);
                yield result(true, "OK");
            }
            case "jump" -> {
                if (!player.onGround()) yield result(false, "NOT_ON_GROUND");
                player.jumpFromGround();
                yield result(true, "OK");
            }
            case "swing" -> {
                player.swing(hand(args));
                yield result(true, "OK");
            }
            case "useItem" -> {
                InteractionHand hand = hand(args);
                yield interaction(client.gameMode.useItem(player, hand), hand);
            }
            case "stopUsingItem" -> {
                if (!player.isUsingItem()) yield result(false, "NOT_USING_ITEM");
                client.gameMode.releaseUsingItem(player);
                yield result(true, "OK");
            }
            case "closeMenu" -> {
                if (client.screen == null || menu == null || menu == player.inventoryMenu) {
                    yield result(false, "NO_CONTAINER_SCREEN");
                }
                player.closeContainer();
                yield result(true, "OK");
            }
            case "attackTarget" -> {
                if (!(client.hitResult instanceof EntityHitResult hit)) yield result(false, "NO_ENTITY_TARGET");
                if (hit.getEntity().level() != level) yield result(false, "INVALID_TARGET");
                if (hit.getEntity() == player || !hit.getEntity().isAlive()
                        || !player.isWithinAttackRange(hit.getEntity().getBoundingBox(), 0)) {
                    yield result(false, "OUT_OF_REACH");
                }
                client.gameMode.attack(player, hit.getEntity());
                player.swing(InteractionHand.MAIN_HAND);
                yield result(true, "OK");
            }
            case "interactTarget" -> interactTarget(client, hand(args));
            default -> throw new IllegalArgumentException("Unknown player operation: " + operation);
        };
    }

    public void close() {
        player = null;
        level = null;
        menu = null;
        session.clear();
    }

    private JsonObject playerSnapshot(Minecraft client) {
        JsonObject data = new JsonObject();
        data.addProperty("id", player.getId());
        data.addProperty("uuid", player.getUUID().toString());
        data.addProperty("name", player.getName().getString());
        data.addProperty("health", player.getHealth());
        data.addProperty("maxHealth", player.getMaxHealth());
        data.addProperty("absorption", player.getAbsorptionAmount());
        data.addProperty("armor", player.getArmorValue());
        data.addProperty("food", player.getFoodData().getFoodLevel());
        data.addProperty("saturation", player.getFoodData().getSaturationLevel());
        data.addProperty("x", player.getX());
        data.addProperty("y", player.getY());
        data.addProperty("z", player.getZ());
        data.addProperty("yaw", player.getYRot());
        data.addProperty("pitch", player.getXRot());
        JsonObject velocity = new JsonObject();
        velocity.addProperty("x", player.getDeltaMovement().x);
        velocity.addProperty("y", player.getDeltaMovement().y);
        velocity.addProperty("z", player.getDeltaMovement().z);
        data.add("velocity", velocity);
        data.addProperty("onGround", player.onGround());
        data.addProperty("inWater", player.isInWater());
        data.addProperty("sprinting", player.isSprinting());
        data.addProperty("sneaking", player.isShiftKeyDown());
        data.addProperty("alive", player.isAlive());
        data.addProperty("usingItem", player.isUsingItem());
        data.addProperty("spectator", player.isSpectator());
        data.addProperty("selectedSlot", player.getInventory().getSelectedSlot());
        data.addProperty("inScreen", client.screen != null);
        return data;
    }

    private String target(Minecraft client) {
        HitResult hit = client.hitResult;
        if (hit == null || hit.getType() == HitResult.Type.MISS) return "null";
        JsonObject data = new JsonObject();
        data.addProperty("x", hit.getLocation().x);
        data.addProperty("y", hit.getLocation().y);
        data.addProperty("z", hit.getLocation().z);
        if (hit instanceof EntityHitResult entity) {
            data.addProperty("type", "entity");
            data.addProperty("id", entity.getEntity().getId());
            data.addProperty("name", entity.getEntity().getName().getString());
        } else if (hit instanceof BlockHitResult block) {
            data.addProperty("type", "block");
            data.addProperty("blockX", block.getBlockPos().getX());
            data.addProperty("blockY", block.getBlockPos().getY());
            data.addProperty("blockZ", block.getBlockPos().getZ());
            data.addProperty("face", block.getDirection().name());
        }
        return data.toString();
    }

    private String interactTarget(Minecraft client, InteractionHand hand) {
        InteractionResult action;
        if (client.hitResult instanceof EntityHitResult hit) {
            if (hit.getEntity().level() != level
                    || !level.getWorldBorder().isWithinBounds(hit.getEntity().blockPosition())) {
                return result(false, "INVALID_TARGET");
            }
            if (hit.getEntity() == player || !player.isWithinEntityInteractionRange(hit.getEntity(), 0)) {
                return result(false, "OUT_OF_REACH");
            }
            action = client.gameMode.interactAt(player, hit.getEntity(), hit, hand);
            if (!action.consumesAction()) action = client.gameMode.interact(player, hit.getEntity(), hand);
        } else if (client.hitResult instanceof BlockHitResult hit && hit.getType() != HitResult.Type.MISS) {
            if (!player.isWithinBlockInteractionRange(hit.getBlockPos(), 0)) return result(false, "OUT_OF_REACH");
            action = client.gameMode.useItemOn(player, hand, hit);
        } else {
            return result(false, "NO_TARGET");
        }
        return interaction(action, hand);
    }

    private String click(Minecraft client, Object[] args) {
        if (menu == null || !session.validMenu(PluginRuntime.string(args, 0))) return result(false, "STALE_MENU");
        int slot = integer(args, 1, 0, menu.slots.size() - 1);
        String type = PluginRuntime.string(args, 3);
        ClickType clickType = switch (type) {
            case "pickup" -> ClickType.PICKUP;
            case "quick_move" -> ClickType.QUICK_MOVE;
            case "swap" -> ClickType.SWAP;
            case "throw" -> ClickType.THROW;
            default -> throw new IllegalArgumentException("Click type must be pickup, quick_move, swap or throw");
        };
        int button = integer(args, 2, 0, clickType == ClickType.SWAP ? 8 : 1);
        client.gameMode.handleInventoryMouseClick(menu.containerId, slot, button, clickType, player);
        return result(true, "OK");
    }

    private static JsonObject item(ItemStack stack) {
        JsonObject data = new JsonObject();
        data.addProperty("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        data.addProperty("name", stack.getHoverName().getString());
        data.addProperty("count", stack.getCount());
        data.addProperty("empty", stack.isEmpty());
        data.addProperty("damage", stack.getDamageValue());
        data.addProperty("maxDamage", stack.getMaxDamage());
        return data;
    }

    private static InteractionHand hand(Object[] args) {
        String hand = args.length == 0 ? "main" : PluginRuntime.string(args, 0);
        return switch (hand) {
            case "main" -> InteractionHand.MAIN_HAND;
            case "off" -> InteractionHand.OFF_HAND;
            default -> throw new IllegalArgumentException("Hand must be main or off");
        };
    }

    private String interaction(InteractionResult action, InteractionHand hand) {
        if (action instanceof InteractionResult.Success success
                && success.swingSource() == InteractionResult.SwingSource.CLIENT) {
            player.swing(hand);
        }
        return result(action.consumesAction(), action.consumesAction() ? "OK"
                : action instanceof InteractionResult.Fail ? "FAIL" : "PASS");
    }

    private static String result(boolean ok, String code) {
        return "{\"ok\":" + ok + ",\"code\":\"" + code + "\"}";
    }
}
