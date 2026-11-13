package com.pvp_utils.client.modules.impl.Tool.tactical;

import com.pvp_utils.Config;
import com.pvp_utils.client.ModuleKeybindManager;
import com.pvp_utils.client.communication.EncryptedChatManager;
import com.pvp_utils.client.render.chroma.world.ChromaWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.security.SecureRandom;

public final class TacticalPingManager {
    private static final long LIFETIME_NANOS = 10_000_000_000L;
    private static final long SEND_INTERVAL_NANOS = 1_200_000_000L;
    private static final double RANGE = 256.0;
    private static final int MAX_MARKERS = 32;
    private static final LinkedHashMap<UUID, Marker> MARKERS = new LinkedHashMap<>();
    private static final LinkedHashMap<UUID, Integer> PLAYER_COLORS = new LinkedHashMap<>();
    private static final LinkedHashMap<UUID, SimpleSoundInstance> VOICES = new LinkedHashMap<>();
    private static final LinkedHashMap<UUID, Long> VOICE_TIMES = new LinkedHashMap<>();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final SoundEvent NAVIGATION = sound("tactical.navigation");
    private static final SoundEvent DANGER = sound("tactical.danger");
    private static List<Marker> snapshot = List.of();
    private static ClientPacketListener connection;
    private static String sessionKey = "";
    private static String interactionDimension = "";
    private static Pending pendingClick;
    private static Pending pendingSend;
    private static long doubleClickDeadline;
    private static long suppressClickUntil;
    private static long nextSendTime;
    private static ClientLevel colorWorld;
    private static boolean initialized;
    private static ChromaWorld.Registration worldRegistration;

    private TacticalPingManager() {
    }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        EncryptedChatManager.setMarkerListener(TacticalPingManager::receive);
    }

    public static boolean onMiddleClick(Minecraft client) {
        if (!canInteract(client)) return false;
        synchronizeSession(client);
        long now = System.nanoTime();
        if (now < suppressClickUntil) return true;
        Target target = target(client);
        if (pendingClick != null && now <= doubleClickDeadline) {
            Pending first = pendingClick;
            pendingClick = null;
            Pending danger = new Pending(target == null ? first.target() : target, first.dimension(), true, now);
            putLocal(client, danger);
            pendingSend = danger;
            suppressClickUntil = now + doubleClickInterval();
            flushSend(client, now);
            return true;
        }
        if (pendingClick != null) {
            pendingSend = pendingClick;
            pendingClick = null;
        }
        if (target == null) return true;
        Pending navigation = new Pending(target, client.level.dimension().identifier().toString(), false, now);
        pendingClick = navigation;
        doubleClickDeadline = now + doubleClickInterval();
        putLocal(client, navigation);
        return true;
    }

    public static boolean canInteract(Minecraft client) {
        return client != null && Config.tacticalPing && EncryptedChatManager.hasValidKey()
                && client.player != null && client.level != null && client.screen == null
                && client.getOverlay() == null && client.isWindowActive()
                && client.mouseHandler.isMouseGrabbed() && !ModuleKeybindManager.isCapturing();
    }

    public static void tick(Minecraft client) {
        synchronizeSession(client);
        if (client == null || client.player == null || client.level == null
                || !Config.tacticalPing || !EncryptedChatManager.hasValidKey()) {
            clearMarkers(client);
            return;
        }
        long now = System.nanoTime();
        if (!VOICES.isEmpty() && !Config.tacticalPingSound) stopVoices(client);
        if (MARKERS.entrySet().removeIf(entry -> entry.getValue().expiresNanos() <= now)) {
            refreshSnapshot();
            if (MARKERS.isEmpty()) closeWorldRegistration();
        }
        if (pendingClick != null && now >= doubleClickDeadline) {
            pendingSend = pendingClick;
            pendingClick = null;
        }
        if (client.screen != null || client.getOverlay() != null || !client.isWindowActive()) {
            pendingClick = null;
            pendingSend = null;
            return;
        }
        flushSend(client, now);
    }

    public static List<Marker> markers() {
        return snapshot;
    }

    public static void close() {
        clearMarkers(Minecraft.getInstance());
        connection = null;
        sessionKey = "";
        interactionDimension = "";
        PLAYER_COLORS.clear();
        colorWorld = null;
        if (initialized) EncryptedChatManager.setMarkerListener(null);
        initialized = false;
    }

    private static void flushSend(Minecraft client, long now) {
        if (pendingSend == null || pendingClick != null || now < nextSendTime) return;
        Pending current = pendingSend;
        if (now - current.createdNanos() >= LIFETIME_NANOS
                || !current.dimension().equals(client.level.dimension().identifier().toString())) {
            pendingSend = null;
            return;
        }
        Target target = current.target();
        Vec3 speaker = client.player.getEyePosition(1f);
        int color = colorFor(client.player.getUUID(), -1);
        if (EncryptedChatManager.sendMarker(target.block(), target.location(), target.face(), current.dimension(), current.danger(), speaker, color)) {
            pendingSend = null;
            nextSendTime = now + SEND_INTERVAL_NANOS;
            playVoice(client, client.player.getUUID(), current.danger(), speaker, now);
        }
    }

    private static void receive(EncryptedChatManager.ReceivedMarker received) {
        Minecraft client = Minecraft.getInstance();
        synchronizeSession(client);
        if (client.player == null || client.level == null || !Config.tacticalPing
                || !EncryptedChatManager.hasValidKey() || received.sender().equals(client.player.getUUID())) return;
        long now = System.nanoTime();
        int color = colorFor(received.sender(), received.colorCode());
        put(new Marker(received.location(), received.face(), received.dimension(), received.sender(),
                received.senderName(), received.danger(), color, now, now + LIFETIME_NANOS));
        if (received.dimension().equals(client.level.dimension().identifier().toString())) {
            playVoice(client, received.sender(), received.danger(), received.senderPosition(), now);
        }
    }

    private static void putLocal(Minecraft client, Pending pending) {
        put(new Marker(pending.target().location(), pending.target().face(), pending.dimension(), client.player.getUUID(),
                client.player.getName().getString(), pending.danger(), colorFor(client.player.getUUID(), -1), pending.createdNanos(),
                pending.createdNanos() + LIFETIME_NANOS));
    }

    private static void put(Marker marker) {
        MARKERS.remove(marker.owner());
        while (MARKERS.size() >= MAX_MARKERS) MARKERS.remove(MARKERS.keySet().iterator().next());
        MARKERS.put(marker.owner(), marker);
        refreshSnapshot();
        if (worldRegistration == null) worldRegistration = ChromaWorld.register(TacticalPingRenderer::renderWorld);
    }

    private static void refreshSnapshot() {
        snapshot = List.copyOf(MARKERS.values());
    }

    private static void synchronizeSession(Minecraft client) {
        ClientPacketListener current = client == null ? null : client.getConnection();
        String key = Config.encryptedChatKey == null ? "" : Config.encryptedChatKey;
        if (connection != current || !sessionKey.equals(key)) {
            clearMarkers(client);
            connection = current;
            sessionKey = key;
            EncryptedChatManager.resetSession();
        }
        String dimension = client == null || client.level == null ? "" : client.level.dimension().identifier().toString();
        ClientLevel currentWorld = client == null ? null : client.level;
        if (colorWorld != currentWorld) {
            clearMarkers(client);
            PLAYER_COLORS.clear();
            colorWorld = currentWorld;
        }
        if (client != null && client.player != null) colorFor(client.player.getUUID(), -1);
        if (!interactionDimension.equals(dimension)) {
            pendingClick = null;
            pendingSend = null;
            doubleClickDeadline = 0L;
            suppressClickUntil = 0L;
            interactionDimension = dimension;
        }
    }

    private static void clearMarkers(Minecraft client) {
        closeWorldRegistration();
        if (!MARKERS.isEmpty()) {
            MARKERS.clear();
            snapshot = List.of();
        }
        pendingClick = null;
        pendingSend = null;
        doubleClickDeadline = 0L;
        suppressClickUntil = 0L;
        nextSendTime = 0L;
        stopVoices(client);
    }

    private static void closeWorldRegistration() {
        if (worldRegistration != null) worldRegistration.close();
        worldRegistration = null;
    }

    private static void stopVoices(Minecraft client) {
        if (client != null) VOICES.values().forEach(sound -> client.getSoundManager().stop(sound));
        VOICES.clear();
        VOICE_TIMES.clear();
    }

    private static void playVoice(Minecraft client, UUID owner, boolean danger, Vec3 speaker, long now) {
        if (!Config.tacticalPingSound || speaker == null || now < VOICE_TIMES.getOrDefault(owner, 0L)) return;
        SimpleSoundInstance previous = VOICES.remove(owner);
        if (previous != null) client.getSoundManager().stop(previous);
        while (VOICES.size() >= 8) {
            UUID retired = VOICES.keySet().iterator().next();
            client.getSoundManager().stop(VOICES.remove(retired));
        }
        double distance = client.gameRenderer.getMainCamera().position().distanceTo(speaker);
        float volume = Math.clamp((float) (1.0 / (1.0 + distance / 12.0)), 0.18f, 1f);
        SimpleSoundInstance voice = new SimpleSoundInstance((danger ? DANGER : NAVIGATION).location(),
                SoundSource.PLAYERS, volume, 1f, SoundInstance.createUnseededRandom(), false, 0,
                SoundInstance.Attenuation.NONE, speaker.x, speaker.y, speaker.z, false);
        VOICES.put(owner, voice);
        client.getSoundManager().play(voice);
        VOICE_TIMES.put(owner, now + SEND_INTERVAL_NANOS);
        if (VOICE_TIMES.size() > 256) VOICE_TIMES.remove(VOICE_TIMES.keySet().iterator().next());
    }

    private static int colorFor(UUID owner, int requested) {
        Integer current = PLAYER_COLORS.get(owner);
        if (current != null) return current;
        int[] usage = new int[8];
        for (int color : PLAYER_COLORS.values()) usage[color]++;
        int selected = requested >= 0 && requested < 8 ? requested : RANDOM.nextInt(8);
        if (usage[selected] > 0) {
            int least = Integer.MAX_VALUE;
            for (int count : usage) least = Math.min(least, count);
            int[] available = new int[8];
            int count = 0;
            for (int i = 0; i < usage.length; i++) if (usage[i] == least) available[count++] = i;
            selected = available[RANDOM.nextInt(count)];
        }
        PLAYER_COLORS.put(owner, selected);
        if (PLAYER_COLORS.size() > 256) PLAYER_COLORS.remove(PLAYER_COLORS.keySet().iterator().next());
        return selected;
    }

    private static long doubleClickInterval() {
        return Math.clamp(Config.tacticalPingDoubleClickInterval, 150, 600) * 1_000_000L;
    }

    private static Target target(Minecraft client) {
        Entity camera = client.getCameraEntity();
        if (camera == null) return null;
        Vec3 start = camera.getEyePosition(1f);
        Vec3 view = camera.getViewVector(1f).scale(RANGE);
        HitResult block = camera.pick(RANGE, 1f, false);
        Vec3 end = block.getType() == HitResult.Type.MISS ? start.add(view) : block.getLocation();
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(camera, start, end,
                camera.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0),
                candidate -> !candidate.isSpectator() && candidate.isPickable(), start.distanceToSqr(end));
        BlockPos position;
        Vec3 location;
        Direction face;
        if (entity != null) {
            location = entity.getEntity().position();
            position = BlockPos.containing(location);
            face = Direction.UP;
        } else if (block instanceof BlockHitResult hit && block.getType() != HitResult.Type.MISS) {
            position = hit.getBlockPos();
            location = hit.getLocation();
            face = hit.getDirection();
        } else {
            return null;
        }
        if (Math.abs((long) position.getX()) > 30_000_000L || Math.abs((long) position.getZ()) > 30_000_000L
                || position.getY() < -2048 || position.getY() > 2047) return null;
        return new Target(position.immutable(), location, face);
    }

    private static SoundEvent sound(String name) {
        return SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("pvp_utils", name));
    }

    public record Marker(Vec3 position, Direction face, String dimension, UUID owner, String name, boolean danger, int colorCode,
                         long createdNanos, long expiresNanos) {
    }

    private record Target(BlockPos block, Vec3 location, Direction face) {
    }

    private record Pending(Target target, String dimension, boolean danger, long createdNanos) {
    }
}
