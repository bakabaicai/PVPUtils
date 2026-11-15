package com.pvp_utils.client.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
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
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public final class KillConfirmManager {
    public static final KillConfirmManager INSTANCE = new KillConfirmManager();
    private static final int MAX_TRACKED_TARGETS = 32;
    private static final long DEDUPLICATION_NANOS = 15_000_000_000L;
    private final Map<UUID, TrackedTarget> trackedTargets = new LinkedHashMap<>();
    private final Map<UUID, TrackedTarget> confirmedTargets = new LinkedHashMap<>();
    private final Map<String, Long> confirmedPlayerNames = new LinkedHashMap<>();
    private final Map<UUID, Long> directDamage = new LinkedHashMap<>();
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
        trackTarget(client, target);
    }

    private void trackTarget(Minecraft client, LivingEntity target) {
        long now = System.nanoTime();
        TrackedTarget confirmed = confirmedTargets.get(target.getUUID());
        if (confirmed != null && confirmed.entity() != target && target.isAlive() && target.deathTime == 0) {
            confirmedTargets.remove(target.getUUID());
            if (target instanceof Player player) {
                confirmedPlayerNames.remove(player.getName().getString().toLowerCase(Locale.ROOT));
            }
            confirmed = null;
        }
        if (confirmed != null && confirmed.entity() == target
                && now - confirmed.attackedAtNanos() <= secondsToNanos(confirmWindow)) return;
        TrackedTarget tracked = new TrackedTarget(target, now, displayName(target),
                client.player.distanceTo(target));
        trackedTargets.remove(target.getUUID());
        trackedTargets.put(target.getUUID(), tracked);
        while (trackedTargets.size() > MAX_TRACKED_TARGETS) {
            Iterator<UUID> iterator = trackedTargets.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
    }

    public void onSystemMessage(Component message) {
        Minecraft client = Minecraft.getInstance();
        if (!synchronizeContext(client)) return;
        String localName = client.player.getName().getString();
        KillAnnouncementParser.Announcement announcement = KillAnnouncementParser.parse(message, localName);
        if (announcement == null || !announcement.killer().equalsIgnoreCase(localName)
                || announcement.victim().equalsIgnoreCase(localName)) return;
        long now = System.nanoTime();
        String nameKey = announcement.victim().toLowerCase(Locale.ROOT);
        Long previous = confirmedPlayerNames.get(nameKey);
        if (previous != null && now - previous <= DEDUPLICATION_NANOS) return;
        Iterator<TrackedTarget> iterator = trackedTargets.values().iterator();
        while (iterator.hasNext()) {
            TrackedTarget tracked = iterator.next();
            if (tracked.entity() instanceof Player victim
                    && victim.getName().getString().equalsIgnoreCase(announcement.victim())
                    && now - tracked.attackedAtNanos() <= secondsToNanos(confirmWindow)) {
                iterator.remove();
                confirmKill(tracked, now);
                return;
            }
        }
        Player victim = client.level.players().stream()
                .filter(player -> player.getName().getString().equalsIgnoreCase(announcement.victim()))
                .findFirst().orElse(null);
        if (victim != null) {
            confirmKill(new TrackedTarget(victim, now, displayName(victim), client.player.distanceTo(victim)), now);
        } else {
            rememberPlayerName(nameKey, now);
            notifyKill(null, announcement.victim(), 0f, now);
        }
    }

    public void onDamage(Entity target, Entity attacker) {
        Minecraft client = Minecraft.getInstance();
        if (!synchronizeContext(client) || !(target instanceof LivingEntity living)
                || target == client.player || target.level() != client.level) return;
        if (attacker == client.player) {
            trackTarget(client, living);
            directDamage.put(target.getUUID(), System.nanoTime());
            while (directDamage.size() > MAX_TRACKED_TARGETS) {
                Iterator<UUID> iterator = directDamage.keySet().iterator();
                iterator.next();
                iterator.remove();
            }
        } else {
            directDamage.remove(target.getUUID());
        }
    }

    public void onDeathEvent(Entity entity) {
        Minecraft client = Minecraft.getInstance();
        if (!synchronizeContext(client) || entity == null || entity.level() != client.level) return;
        TrackedTarget tracked = trackedTargets.get(entity.getUUID());
        long now = System.nanoTime();
        if (tracked == null || tracked.entity() != entity || !hasDirectDamage(tracked, now)) return;
        trackedTargets.remove(entity.getUUID());
        confirmKill(tracked, now);
    }

    private boolean hasDirectDamage(TrackedTarget tracked, long now) {
        Long damagedAt = directDamage.get(tracked.entity().getUUID());
        return damagedAt != null && now - damagedAt <= secondsToNanos(confirmWindow);
    }

    public void tick(Minecraft client) {
        if (!synchronizeContext(client)) return;
        long now = System.nanoTime();
        long confirmationNanos = secondsToNanos(confirmWindow);
        confirmedTargets.values().removeIf(tracked -> now - tracked.attackedAtNanos() > DEDUPLICATION_NANOS);
        confirmedPlayerNames.values().removeIf(time -> now - time > DEDUPLICATION_NANOS);
        directDamage.values().removeIf(time -> now - time > confirmationNanos);
        List<TrackedTarget> confirmed = null;
        Iterator<TrackedTarget> iterator = trackedTargets.values().iterator();
        while (iterator.hasNext()) {
            TrackedTarget tracked = iterator.next();
            LivingEntity target = tracked.entity();
            if (target.level() != client.level || now - tracked.attackedAtNanos() > confirmationNanos) {
                iterator.remove();
                continue;
            }
            if (hasDirectDamage(tracked, now) && !target.isRemoved()
                    && (target.getHealth() <= 0.0F || target.deathTime > 0)) {
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
        tick(client);
        long now = System.nanoTime();
        long window = secondsToNanos(confirmWindow);
        ConfirmedKill recent = lastPlayerKill;
        return recent != null && recent.level() == client.level
                && now - recent.confirmedAtNanos() <= window ? recent : null;
    }

    public void clear() {
        contextRevision++;
        trackedTargets.clear();
        confirmedTargets.clear();
        confirmedPlayerNames.clear();
        directDamage.clear();
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
        TrackedTarget previous = confirmedTargets.get(tracked.entity().getUUID());
        if (previous != null && previous.entity() == tracked.entity()
                && now - previous.attackedAtNanos() <= DEDUPLICATION_NANOS) return;
        if (tracked.entity() instanceof Player player) {
            String key = player.getName().getString().toLowerCase(Locale.ROOT);
            Long confirmedAt = confirmedPlayerNames.get(key);
            if (confirmedAt != null && now - confirmedAt <= DEDUPLICATION_NANOS) return;
            rememberPlayerName(key, now);
        }
        directDamage.remove(tracked.entity().getUUID());
        confirmedTargets.put(tracked.entity().getUUID(),
                new TrackedTarget(tracked.entity(), now, tracked.name(), tracked.distance()));
        while (confirmedTargets.size() > MAX_TRACKED_TARGETS) {
            Iterator<UUID> iterator = confirmedTargets.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
        if (tracked.entity() instanceof Player) {
            lastPlayerKill = new ConfirmedKill(tracked.entity().position(), level, now);
        }
        notifyKill(tracked.entity(), tracked.name(), tracked.distance(), now);
    }

    private void rememberPlayerName(String name, long now) {
        confirmedPlayerNames.put(name, now);
        while (confirmedPlayerNames.size() > MAX_TRACKED_TARGETS) {
            Iterator<String> iterator = confirmedPlayerNames.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
    }

    private void notifyKill(LivingEntity target, String name, float distance, long now) {
        long comboNanos = secondsToNanos(comboWindow);
        comboCount = lastKillNanos != 0L && now - lastKillNanos <= comboNanos
                ? comboCount + 1 : 1;
        lastKillNanos = now;
        int confirmedCombo = comboCount;
        for (KillListener listener : listeners) {
            listener.onKillConfirmed(target, name, distance, confirmedCombo);
        }
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
