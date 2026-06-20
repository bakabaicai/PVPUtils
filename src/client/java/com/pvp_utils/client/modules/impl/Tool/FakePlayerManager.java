package com.pvp_utils.client.modules.impl.Tool;

import com.mojang.authlib.GameProfile;
import com.pvp_utils.client.Version;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class FakePlayerManager {
    private static RemotePlayer fakePlayer;
    private static boolean enabled = false;
    private static boolean armor = true;
    private static boolean totem = false;
    private static ClientLevel level;
    private static long respawnAt = 0L;
    private static int regenerationTicks = 0;
    private static int particleTicks = 0;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            remove();
        }
    }

    public static boolean hasArmor() {
        return armor;
    }

    public static void setArmor(boolean value) {
        armor = value;
        applyEquipment();
    }

    public static boolean hasTotem() {
        return totem;
    }

    public static void setTotem(boolean value) {
        totem = value;
        applyEquipment();
    }

    public static void tick(Minecraft client) {
        if (!Version.DEBUG || !enabled) {
            remove();
            return;
        }

        long now = System.currentTimeMillis();
        LocalPlayer player = client.player;
        ClientLevel currentLevel = client.level;
        if (player == null || currentLevel == null) {
            remove();
            return;
        }

        if (level != null && level != currentLevel) {
            remove();
        }

        if (fakePlayer == null && respawnAt > 0L) {
            if (now < respawnAt) return;
            respawnAt = 0L;
        }

        if (fakePlayer == null || fakePlayer.isRemoved()) {
            clearFakePlayerOnly();
            spawn(client, currentLevel, player);
        }

        if (fakePlayer != null) {
            applyEquipment();
            tickTotemEffects(currentLevel);
            if (!fakePlayer.isAlive() || fakePlayer.isRemoved()) {
                scheduleRespawn(currentLevel, fakePlayer);
                return;
            }
        }
    }

    public static boolean tryAttack(Minecraft client) {
        if (!Version.DEBUG || !enabled || fakePlayer == null || client.player == null || client.level == null) return false;
        if (fakePlayer.isRemoved() || !fakePlayer.isAlive()) return false;
        if (!(client.hitResult instanceof EntityHitResult hitResult) || hitResult.getEntity() != fakePlayer) return false;

        client.player.swing(InteractionHand.MAIN_HAND);
        float damage = armor ? 4.0f : 7.0f;
        Vec3 look = client.player.getLookAngle();
        applyDamageSound(fakePlayer);
        fakePlayer.hurtTime = 10;
        fakePlayer.hurtDuration = 10;
        fakePlayer.animateHurt((float) client.player.getYRot());
        fakePlayer.knockback(0.45, -look.x, -look.z);
        if (fakePlayer.getHealth() - damage <= 0.0f && totem && fakePlayer.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            triggerTotem(client);
            return true;
        }

        fakePlayer.setHealth(Math.max(0.0f, fakePlayer.getHealth() - damage));
        if (!fakePlayer.isAlive() || fakePlayer.getHealth() <= 0.0f) {
            fakePlayer.setHealth(0.0f);
            scheduleRespawn(client.level, fakePlayer);
            return true;
        }
        com.pvp_utils.client.modules.impl.Render.TargetHudRenderer.getInstance().onHit(fakePlayer);
        return true;
    }

    private static void spawn(Minecraft client, ClientLevel currentLevel, LocalPlayer player) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), player.getScoreboardName());
        fakePlayer = new RemotePlayer(currentLevel, profile);
        level = currentLevel;

        Vec3 look = player.getLookAngle();
        Vec3 pos = player.position().add(look.x * 2.5, 0.0, look.z * 2.5);
        fakePlayer.setPos(pos.x, player.getY(), pos.z);
        fakePlayer.setYRot(player.getYRot() + 180.0f);
        fakePlayer.setXRot(0.0f);
        fakePlayer.yBodyRot = fakePlayer.getYRot();
        fakePlayer.yHeadRot = fakePlayer.getYRot();
        fakePlayer.setHealth(fakePlayer.getMaxHealth());
        currentLevel.addEntity(fakePlayer);
        applyEquipment();
    }

    private static void applyEquipment() {
        if (fakePlayer == null) return;

        fakePlayer.setItemSlot(EquipmentSlot.HEAD, armor ? new ItemStack(Items.NETHERITE_HELMET) : ItemStack.EMPTY);
        fakePlayer.setItemSlot(EquipmentSlot.CHEST, armor ? new ItemStack(Items.NETHERITE_CHESTPLATE) : ItemStack.EMPTY);
        fakePlayer.setItemSlot(EquipmentSlot.LEGS, armor ? new ItemStack(Items.NETHERITE_LEGGINGS) : ItemStack.EMPTY);
        fakePlayer.setItemSlot(EquipmentSlot.FEET, armor ? new ItemStack(Items.NETHERITE_BOOTS) : ItemStack.EMPTY);
        fakePlayer.setItemSlot(EquipmentSlot.OFFHAND, totem ? new ItemStack(Items.TOTEM_OF_UNDYING) : ItemStack.EMPTY);
    }

    private static void triggerTotem(Minecraft client) {
        if (fakePlayer == null) return;
        fakePlayer.setHealth(1.0f);
        fakePlayer.setAbsorptionAmount(4.0f);
        fakePlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        fakePlayer.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        fakePlayer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        fakePlayer.handleEntityEvent((byte) 35);
        fakePlayer.playSound(SoundEvents.TOTEM_USE, 1.0f, 1.0f);
        regenerationTicks = 900;
        particleTicks = 40;
        spawnTotemParticles(client.level, 45);
        com.pvp_utils.client.modules.impl.Render.TargetHudRenderer.getInstance().onHit(fakePlayer);
    }

    private static void applyDamageSound(RemotePlayer player) {
        player.playSound(SoundEvents.PLAYER_HURT, 1.0f, 1.0f);
    }

    private static void tickTotemEffects(ClientLevel currentLevel) {
        if (fakePlayer == null) return;
        if (regenerationTicks > 0) {
            regenerationTicks--;
            if (fakePlayer.getAbsorptionAmount() < 4.0f) {
                fakePlayer.setAbsorptionAmount(4.0f);
            }
            if (regenerationTicks % 25 == 0 && fakePlayer.getHealth() < fakePlayer.getMaxHealth()) {
                fakePlayer.setHealth(Math.min(fakePlayer.getMaxHealth(), fakePlayer.getHealth() + 1.0f));
                com.pvp_utils.client.modules.impl.Render.TargetHudRenderer.getInstance().onHit(fakePlayer);
            }
        }
        if (particleTicks > 0) {
            particleTicks--;
            spawnTotemParticles(currentLevel, 4);
        }
    }

    private static void spawnTotemParticles(ClientLevel currentLevel, int count) {
        if (currentLevel == null || fakePlayer == null) return;
        for (int i = 0; i < count; i++) {
            double x = fakePlayer.getX() + (currentLevel.random.nextDouble() - 0.5) * fakePlayer.getBbWidth() * 1.7;
            double y = fakePlayer.getY() + currentLevel.random.nextDouble() * fakePlayer.getBbHeight();
            double z = fakePlayer.getZ() + (currentLevel.random.nextDouble() - 0.5) * fakePlayer.getBbWidth() * 1.7;
            double vx = (currentLevel.random.nextDouble() - 0.5) * 0.35;
            double vy = currentLevel.random.nextDouble() * 0.35;
            double vz = (currentLevel.random.nextDouble() - 0.5) * 0.35;
            currentLevel.addParticle(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, vx, vy, vz);
        }
    }

    private static void remove() {
        clearFakePlayerOnly();
        level = null;
        respawnAt = 0L;
        regenerationTicks = 0;
        particleTicks = 0;
    }

    private static void clearFakePlayerOnly() {
        if (fakePlayer != null) {
            ClientLevel currentLevel = level;
            if (currentLevel != null) {
                currentLevel.removeEntity(fakePlayer.getId(), Entity.RemovalReason.DISCARDED);
            } else {
                fakePlayer.remove(Entity.RemovalReason.DISCARDED);
            }
            fakePlayer = null;
        }
    }

    private static void scheduleRespawn(ClientLevel currentLevel, RemotePlayer player) {
        if (currentLevel != null && player != null) {
            currentLevel.removeEntity(player.getId(), Entity.RemovalReason.DISCARDED);
        }
        fakePlayer = null;
        level = currentLevel;
        respawnAt = System.currentTimeMillis() + 3000L;
        regenerationTicks = 0;
        particleTicks = 0;
    }
}
