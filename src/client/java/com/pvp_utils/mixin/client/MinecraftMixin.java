package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Combat.MainHandAssistManager;
import com.pvp_utils.client.modules.impl.Tool.AutoChestDepositManager;
import com.pvp_utils.client.modules.impl.Tool.FakePlayerManager;
import com.pvp_utils.client.modules.impl.Tool.TimeWeatherChanger;
import com.pvp_utils.client.modules.impl.Render.DamageNumberRenderer;
import com.pvp_utils.client.modules.impl.Render.DynamicIsland.DynamicIslandRenderer;
import com.pvp_utils.client.modules.impl.Render.HudEditOverlay;
import com.pvp_utils.client.modules.impl.Render.KeystrokesRenderer;
import com.pvp_utils.client.modules.impl.Render.MusicInfoHudRenderer;
import com.pvp_utils.client.modules.impl.Render.PotionStatusRenderer;
import com.pvp_utils.client.modules.impl.Render.PingHudRenderer;
import com.pvp_utils.client.modules.impl.Render.TpsHudRenderer;
import com.pvp_utils.client.modules.impl.Render.ClockHudRenderer;
import com.pvp_utils.client.gui.clickgui.NewSettingsScreen;
import com.pvp_utils.client.NeteaseMusic.NeteaseMusicScreen;
import com.pvp_utils.client.render.MainUI.PVPUtilsMainUI;
import com.pvp_utils.client.render.MainUI.PVPUtilsMultiplayerScreen;
import com.pvp_utils.client.render.MainUI.PVPUtilsSingleplayerScreen;
import com.pvp_utils.client.render.MainUI.PVPUtilsViaFabricPlusScreen;
import com.pvp_utils.client.alt.AltManagerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void pvp_utils$tickToolOverrides(CallbackInfo ci) {
        TimeWeatherChanger.tick((Minecraft) (Object) this);
        DamageNumberRenderer.getInstance().tick((Minecraft) (Object) this);
        if (pvp_utils$shouldShowUseAttackVisual()) {
            pvp_utils$playUseAttackVisual();
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"))
    private void pvp_utils$prepareQuickUseMainHand(CallbackInfo ci) {
        MainHandAssistManager.beforeStartUseItem((Minecraft) (Object) this);
    }

    private boolean pvp_utils$shouldShowUseAttackVisual() {
        Minecraft client = (Minecraft) (Object) this;
        return Config.legacy17Animations
                && Config.legacy17UseSwing
                && client.player != null
                && client.level != null
                && client.player.isUsingItem()
                && !client.player.getItemInHand(client.player.getUsedItemHand()).isEmpty()
                && client.options.keyAttack.isDown();
    }

    private void pvp_utils$playUseAttackVisual() {
        Minecraft client = (Minecraft) (Object) this;
        Player player = client.player;
        if (player == null || client.level == null || player.getUsedItemHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (client.hitResult instanceof BlockHitResult blockHit && !client.level.getBlockState(blockHit.getBlockPos()).isAir()) {
            client.level.addBreakingBlockEffect(blockHit.getBlockPos(), blockHit.getDirection());
        } else {
            return;
        }
        LivingEntitySwingAccessor swingAccessor = (LivingEntitySwingAccessor) player;
        if (!swingAccessor.pvp_utils$isSwinging()
                || swingAccessor.pvp_utils$getSwingTime() >= swingAccessor.pvp_utils$invokeGetCurrentSwingDuration() / 2
                || swingAccessor.pvp_utils$getSwingTime() < 0) {
            swingAccessor.pvp_utils$setSwingTime(-1);
            swingAccessor.pvp_utils$setSwinging(true);
            swingAccessor.pvp_utils$setSwingingArm(InteractionHand.MAIN_HAND);
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void hideAutoChestDepositScreen(Screen screen, CallbackInfo ci) {
        if (AutoChestDepositManager.shouldHideContainerScreen(screen)) {
            ci.cancel();
        }
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void attackFakePlayer(CallbackInfoReturnable<Boolean> cir) {
        if (FakePlayerManager.tryAttack((Minecraft) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    @Redirect(
            method = "startAttack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;attack(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)V"
            )
    )
    private void pvp_utils$attackWithEffects(MultiPlayerGameMode gameMode, Player player, Entity target) {
        gameMode.attack(player, target);
        Minecraft client = (Minecraft) (Object) this;
        if (client.level == null || client.player == null || player != client.player || target == null) {
            return;
        }
        if (Config.attackEffectsCritParticles) {
            spawnAttackParticles(client, target, ParticleTypes.CRIT, Config.attackEffectsCritMultiplier);
        }
        if (Config.attackEffectsSharpnessParticles) {
            spawnAttackParticles(client, target, ParticleTypes.ENCHANTED_HIT, Config.attackEffectsSharpnessMultiplier);
        }
        if (Config.attackEffectsFlameParticles) {
            spawnAttackParticles(client, target, ParticleTypes.FLAME, Config.attackEffectsFlameMultiplier);
        }
        if (Config.attackEffectsBloodParticles) {
            spawnAttackParticles(client, target, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()), Config.attackEffectsBloodMultiplier);
        }
        if (Config.attackEffectsLightning) {
            spawnAttackLightning(client, target);
        }
        DamageNumberRenderer.getInstance().watchAttack(target);
    }

    private void spawnAttackParticles(Minecraft client, Entity target, ParticleOptions particle, float multiplier) {
        int count = getAttackParticleCount(multiplier);
        AABB box = target.getBoundingBox();
        RandomSource random = target.getRandom();
        for (int i = 0; i < count; i++) {
            double x = box.minX + random.nextDouble() * box.getXsize();
            double y = box.minY + random.nextDouble() * box.getYsize();
            double z = box.minZ + random.nextDouble() * box.getZsize();
            double vx = (random.nextDouble() - 0.5D) * 0.08D;
            double vy = random.nextDouble() * 0.08D;
            double vz = (random.nextDouble() - 0.5D) * 0.08D;
            client.level.addParticle(particle, x, y, z, vx, vy, vz);
        }
    }

    private int getAttackParticleCount(float multiplier) {
        if (!Float.isFinite(multiplier)) {
            return 1;
        }
        return Math.max(1, Math.min(32, Math.round(multiplier * 4.0f)));
    }

    private void spawnAttackLightning(Minecraft client, Entity target) {
        int count = Math.max(1, Math.min(5, Config.attackEffectsLightningCount));
        AABB box = target.getBoundingBox();
        RandomSource random = target.getRandom();
        for (int i = 0; i < count; i++) {
            LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, client.level);
            lightning.setVisualOnly(true);
            double x = target.getX() + (random.nextDouble() - 0.5D) * Math.max(0.2D, box.getXsize());
            double z = target.getZ() + (random.nextDouble() - 0.5D) * Math.max(0.2D, box.getZsize());
            lightning.setPos(x, target.getY(), z);
            client.level.addEntity(lightning);
        }
    }

    @Inject(
            method = "runTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen()V"
            )
    )
    private void pvp_utils$renderClickGuiFrameEnd(boolean advanceGameTime, CallbackInfo ci) {
        PotionStatusRenderer.getInstance().renderFrameEnd();
        KeystrokesRenderer.getInstance().renderFrameEnd();
        DynamicIslandRenderer.getInstance().renderFrameEnd();
        PingHudRenderer.getInstance().renderFrameEnd();
        TpsHudRenderer.getInstance().renderFrameEnd();
        ClockHudRenderer.getInstance().renderFrameEnd();
        MusicInfoHudRenderer.getInstance().renderFrameEnd();
        Minecraft client = (Minecraft) (Object) this;
        if (client.screen instanceof NewSettingsScreen settingsScreen) {
            settingsScreen.renderFrameEnd();
        }
        if (client.screen instanceof NeteaseMusicScreen musicScreen) {
            musicScreen.renderFrameEnd();
        }
        if (client.screen instanceof PVPUtilsMainUI mainUI) {
            mainUI.renderFrameEnd();
        }
        if (client.screen instanceof PVPUtilsSingleplayerScreen singleplayerScreen) {
            singleplayerScreen.renderFrameEnd();
        }
        if (client.screen instanceof PVPUtilsMultiplayerScreen multiplayerScreen) {
            multiplayerScreen.renderFrameEnd();
        }
        if (client.screen instanceof AltManagerScreen altManagerScreen) {
            altManagerScreen.renderFrameEnd();
        }
        if (client.screen instanceof PVPUtilsViaFabricPlusScreen viaFabricPlusScreen) {
            viaFabricPlusScreen.renderFrameEnd();
        }
    }

    @Inject(
            method = "runTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/Window;updateDisplay(Lcom/mojang/blaze3d/TracyFrameCapture;)V"
            )
    )
    private void pvp_utils$renderHudEditorFrameEnd(boolean advanceGameTime, CallbackInfo ci) {
        HudEditOverlay.getInstance().renderFrameEnd();
    }
}
