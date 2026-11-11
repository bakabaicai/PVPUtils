package com.pvp_utils.client.modules.impl.Combat.combo;

import com.pvp_utils.Config;
import io.github.humbleui.skija.Canvas;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;

import java.util.concurrent.ThreadLocalRandom;

public final class ComboEffectsManager {
    private static ComboRenderer renderer;
    private static ComboAudio audio;
    private static Object world;
    private static Object player;
    private static boolean enabled;
    private static boolean dead;
    private static boolean closed;
    private static int count;
    private static long lastKill;
    private static long animationStarted;
    private static float spinDirection = 1f;

    private ComboEffectsManager() {
    }

    public static void initialize() {
        tick(Minecraft.getInstance());
    }

    public static void tick(Minecraft client) {
        if (closed) return;
        if (!Config.comboEffects) {
            if (enabled) {
                reset();
                dispose();
                enabled = false;
                world = null;
                player = null;
                dead = false;
            }
            return;
        }
        if (!enabled) {
            enabled = true;
            reset();
        }
        if (world != client.level || player != client.player) {
            reset();
            world = client.level;
            player = client.player;
            dead = false;
        }
        boolean currentlyDead = client.player != null && client.player.isDeadOrDying();
        if (currentlyDead && !dead) reset();
        dead = currentlyDead;
        if (Config.comboEffectsIcon && renderer == null) renderer = new ComboRenderer();
        if (!Config.comboEffectsIcon && renderer != null) {
            renderer.close();
            renderer = null;
        }
        if (Config.comboEffectsSound && audio == null) {
            audio = new ComboAudio();
            audio.preload();
        }
        if (!Config.comboEffectsSound && audio != null) {
            audio.close();
            audio = null;
        }
        expire(System.nanoTime());
    }

    public static void onKill(LivingEntity target) {
        if (closed || !Config.comboEffects || target == null) return;
        Minecraft client = Minecraft.getInstance();
        tick(client);
        if (client.level == null || client.player == null || client.player.isDeadOrDying()
                || target == client.player || !accepts(target)) return;
        long now = System.nanoTime();
        expire(now);
        if (count < Integer.MAX_VALUE) count++;
        lastKill = now;
        animationStarted = now;
        spinDirection = ThreadLocalRandom.current().nextBoolean() ? 1f : -1f;
        if (audio != null && Config.comboEffectsSound) {
            float volume = Float.isFinite(Config.comboEffectsVolume) ? Math.clamp(Config.comboEffectsVolume, 0f, 1f) : 1f;
            audio.play(Math.min(count, 5), volume * client.options.getFinalSoundSourceVolume(SoundSource.MASTER));
        }
    }

    private static boolean accepts(LivingEntity target) {
        if (target instanceof Player) return Config.comboEffectsPlayers;
        MobCategory category = target.getType().getCategory();
        if (category == MobCategory.MONSTER) return Config.comboEffectsMonsters;
        if (category == MobCategory.AXOLOTLS || category == MobCategory.WATER_CREATURE
                || category == MobCategory.WATER_AMBIENT || category == MobCategory.UNDERGROUND_WATER_CREATURE) {
            return Config.comboEffectsOther;
        }
        if (target instanceof Animal || category == MobCategory.CREATURE) return Config.comboEffectsAnimals;
        return Config.comboEffectsOther;
    }

    public static void render(Canvas canvas) {
        if (closed || !enabled || !Config.comboEffects || !Config.comboEffectsIcon || renderer == null) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || client.player.isDeadOrDying()
                || client.screen != null || client.options.hideGui || world != client.level || player != client.player) return;
        long now = System.nanoTime();
        expire(now);
        renderer.prepareRenderResources();
        if (count == 0 || animationStarted == 0L) return;
        ComboTimeline.Frame frame = ComboTimeline.sample(now - animationStarted, count, spinDirection);
        renderer.render(canvas, frame, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
    }

    private static void expire(long now) {
        if (count == 0 || lastKill == 0L) return;
        float duration = Float.isFinite(Config.comboEffectsDuration) ? Math.clamp(Config.comboEffectsDuration, 0.1f, 300f) : 300f;
        if (now - lastKill > (long) (duration * 1_000_000_000d)) reset();
    }

    public static void reset() {
        count = 0;
        lastKill = 0L;
        animationStarted = 0L;
        if (audio != null) audio.reset();
    }

    public static int comboCount() {
        return count;
    }

    public static Throwable failure() {
        Throwable imageFailure = renderer == null ? null : renderer.failure();
        return imageFailure != null ? imageFailure : audio == null ? null : audio.failure();
    }

    public static void close() {
        if (closed) return;
        closed = true;
        reset();
        dispose();
        enabled = false;
        world = null;
        player = null;
    }

    private static void dispose() {
        if (renderer != null) renderer.close();
        if (audio != null) audio.close();
        renderer = null;
        audio = null;
    }
}
