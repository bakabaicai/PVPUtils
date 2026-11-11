package com.pvp_utils.client.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public final class KillConfirmManager {
    public static final KillConfirmManager INSTANCE = new KillConfirmManager();
    private static final int MAX_TRACKED_TARGETS = 32;
    private final Map<UUID, TrackedTarget> trackedTargets = new LinkedHashMap<>();
    private final CopyOnWriteArrayList<KillListener> listeners = new CopyOnWriteArrayList<>();
    private volatile double confirmWindow = 6.0;
    private volatile double comboWindow = 4.0;
    private long lastKillNanos;
    private int comboCount;
    private ConfirmedKill lastPlayerKill;
    private ClientLevel level;
    private Player localPlayer;
    private long contextRevision;

    public interface KillListener {
        void onKillConfirmed(LivingEntity target, String name, float distance, int combo);
    }

    public record ConfirmedKill(Vec3 position, ClientLevel level, long confirmedAtNanos) {
    }

    public void subscribe(KillListener listener) {
        listeners.addIfAbsent(Objects.requireNonNull(listener));
    }

    public void unsubscribe(KillListener listener) {
        listeners.remove(listener);
    }

    public void setConfirmWindow(double seconds) {
        confirmWindow = validateWindow(seconds);
    }

    public void setComboWindow(double seconds) {
        comboWindow = validateWindow(seconds);
    }

    public void recordAttack(Player player, Entity entity) {
        Minecraft client = Minecraft.getInstance();
        if (!synchronizeContext(client) || player != client.player
                || !(entity instanceof LivingEntity target) || target == client.player
                || target.level() != client.level || !target.isAlive()) {
            return;
        }
        TrackedTarget tracked = new TrackedTarget(target, System.nanoTime(), displayName(target),
                client.player.distanceTo(target));
        trackedTargets.remove(target.getUUID());
        trackedTargets.put(target.getUUID(), tracked);
        while (trackedTargets.size() > MAX_TRACKED_TARGETS) {
            Iterator<UUID> iterator = trackedTargets.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
    }

    public void tick(Minecraft client) {
        if (!synchronizeContext(client)) return;
        long now = System.nanoTime();
        long confirmationNanos = secondsToNanos(confirmWindow);
        List<TrackedTarget> confirmed = null;
        Iterator<TrackedTarget> iterator = trackedTargets.values().iterator();
        while (iterator.hasNext()) {
            TrackedTarget tracked = iterator.next();
            LivingEntity target = tracked.entity();
            if (target.level() != client.level || now - tracked.attackedAtNanos() > confirmationNanos) {
                iterator.remove();
                continue;
            }
            if (isDeathLike(target)) {
                iterator.remove();
                if (confirmed == null) confirmed = new ArrayList<>();
                confirmed.add(tracked);
            }
        }
        if (confirmed != null) {
            ClientLevel tickLevel = client.level;
            Player tickPlayer = client.player;
            long tickRevision = contextRevision;
            for (TrackedTarget tracked : confirmed) {
                if (contextRevision != tickRevision || client.level != tickLevel || client.player != tickPlayer) {
                    synchronizeContext(client);
                    break;
                }
                confirmKill(tracked, now);
            }
        }
    }

    public ConfirmedKill recentPlayerKill() {
        Minecraft client = Minecraft.getInstance();
        if (!synchronizeContext(client)) return null;
        long now = System.nanoTime();
        long window = secondsToNanos(confirmWindow);
        ConfirmedKill recent = lastPlayerKill;
        long latestAttack = Long.MIN_VALUE;
        for (TrackedTarget tracked : trackedTargets.values()) {
            if (tracked.entity() instanceof Player && tracked.entity().level() == client.level
                    && now - tracked.attackedAtNanos() <= window && isDeathLike(tracked.entity())
                    && tracked.attackedAtNanos() > latestAttack) {
                latestAttack = tracked.attackedAtNanos();
                recent = new ConfirmedKill(tracked.entity().position(), client.level, now);
            }
        }
        return recent != null && recent.level() == client.level
                && now - recent.confirmedAtNanos() <= window ? recent : null;
    }

    public void clear() {
        contextRevision++;
        trackedTargets.clear();
        lastKillNanos = 0L;
        comboCount = 0;
        lastPlayerKill = null;
        level = null;
        localPlayer = null;
    }

    private boolean synchronizeContext(Minecraft client) {
        if (client != null && !client.isSameThread()) {
            throw new IllegalStateException("Kill detection must run on the client thread");
        }
        if (client == null || client.level == null || client.player == null) {
            clear();
            return false;
        }
        if (level != client.level || localPlayer != client.player) {
            clear();
            level = client.level;
            localPlayer = client.player;
        }
        return true;
    }

    private void confirmKill(TrackedTarget tracked, long now) {
        if (tracked.entity() instanceof Player) {
            lastPlayerKill = new ConfirmedKill(tracked.entity().position(), level, now);
        }
        long comboNanos = secondsToNanos(comboWindow);
        comboCount = lastKillNanos != 0L && now - lastKillNanos <= comboNanos
                ? comboCount + 1 : 1;
        lastKillNanos = now;
        int confirmedCombo = comboCount;
        for (KillListener listener : listeners) {
            listener.onKillConfirmed(tracked.entity(), tracked.name(), tracked.distance(), confirmedCombo);
        }
    }

    private static boolean isDeathLike(LivingEntity target) {
        if (target.isDeadOrDying() || target.getHealth() <= 0.0F || !target.isAlive()) return true;
        if (target instanceof Player player && player.isSpectator()) return true;
        return target.isInvisible();
    }

    private static String displayName(LivingEntity target) {
        String name = target.getDisplayName().getString();
        return name.isBlank() ? target.getName().getString() : name;
    }

    private static double validateWindow(double seconds) {
        if (!Double.isFinite(seconds) || seconds <= 0.0 || seconds > Long.MAX_VALUE / 1_000_000_000.0) {
            throw new IllegalArgumentException("The kill window must be a finite positive duration");
        }
        return seconds;
    }

    private static long secondsToNanos(double seconds) {
        return (long) (seconds * 1_000_000_000.0);
    }

    private record TrackedTarget(LivingEntity entity, long attackedAtNanos, String name, float distance) {
    }
}
