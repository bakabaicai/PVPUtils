package com.pvp_utils.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pvp_utils.Config;
import com.pvp_utils.client.gui.SettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Shadow private float mainHandHeight;
    @Shadow private float oMainHandHeight;
    @Shadow protected abstract void applyItemArmTransform(PoseStack p, HumanoidArm a, float f);
    @Shadow public abstract void renderItem(LivingEntity e, ItemStack s, ItemDisplayContext c, PoseStack ps, SubmitNodeCollector snc, int l);

    private boolean isEntityInRange() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return false;
        double r = Config.range;
        Vec3 pos = client.player.position();
        AABB area = new AABB(pos.x - r, pos.y - r, pos.z - r, pos.x + r, pos.y + r, pos.z + r);
        List<Entity> entities = client.level.getEntities(client.player, area, (e) -> e instanceof Player && !e.isSpectator() && e.isAlive());
        for (Entity entity : entities) {
            if (client.player.distanceTo(entity) <= r) {
                return true;
            }
        }
        return false;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void updateHandPosition(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        boolean isSword = client.player.getMainHandItem().is(ItemTags.SWORDS);
        boolean hasTarget = Config.autoMode && isEntityInRange();
        boolean isBlocking = Config.swordBlock && isSword && (client.options.keyUse.isDown() || hasTarget);
        boolean isEatingSwing = Config.useSwing && client.player.isUsingItem();
        if (isBlocking || isEatingSwing || client.screen instanceof SettingsScreen) {
            this.mainHandHeight = 1.0F;
            this.oMainHandHeight = 1.0F;
        }
    }

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void injectOldAnimation(AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand, float swingProgress, ItemStack stack, float equippedProgress, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int combinedLight, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        ItemStack mainHandStack = player.getMainHandItem();
        ItemStack offHandStack = player.getOffhandItem();

        boolean isSword = mainHandStack.is(ItemTags.SWORDS);
        boolean hasShield = offHandStack.is(Items.SHIELD);
        boolean hasTarget = Config.autoMode && isEntityInRange();

        boolean isBlocking = Config.swordBlock && isSword && (client.options.keyUse.isDown() || hasTarget || client.screen instanceof SettingsScreen);

        if (hand == InteractionHand.OFF_HAND && isBlocking && hasShield) {
            ci.cancel();
            return;
        }

        if (hand == InteractionHand.MAIN_HAND) {
            boolean isUseSwinging = Config.useSwing && player.isUsingItem() && swingProgress > 0.0F;

            if (isBlocking || isUseSwinging) {
                ci.cancel();
                HumanoidArm arm = player.getMainArm();
                int i = arm == HumanoidArm.RIGHT ? 1 : -1;

                poseStack.pushPose();
                poseStack.translate(Config.offsetX * i, Config.offsetY, Config.offsetZ);
                this.applyItemArmTransform(poseStack, arm, 0.0F);

                if (Config.animationMode == Config.AnimMode.MODE_1_7) {
                    poseStack.translate(i * 0.430F, -0.190F, 0.520F);
                    poseStack.translate(i * -0.141F, 0.08F, -0.72F);
                    float f17 = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
                    float f22 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
                    poseStack.mulPose(Axis.YP.rotationDegrees(i * (45.0F + f17 * -20.0F)));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(i * f22 * -20.0F));
                    poseStack.mulPose(Axis.XP.rotationDegrees(f22 * -80.0F));
                    poseStack.mulPose(Axis.YP.rotationDegrees(i * -45.0F));
                    renderOldSwordStance(poseStack, i);
                } else if (Config.animationMode == Config.AnimMode.MODE_1_7_PLUS) {
                    poseStack.translate(i * 0.15F, -0.05F, 0.0F);
                    renderOldSwordStance(poseStack, i);
                    float f = Mth.sqrt(swingProgress);
                    float swingAmount = Mth.sin(f * (float) Math.PI);
                    poseStack.mulPose(Axis.XP.rotationDegrees(swingAmount * -45.0F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(i * swingAmount * 20.0F));
                } else {
                    poseStack.translate(i * 0.1F, -0.05F, 0.0F);
                    renderOldSwordStance(poseStack, i);
                    float f = Mth.sqrt(swingProgress);
                    float swingAmount = Mth.sin(f * (float) Math.PI);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-i * swingAmount * 35.0F));
                    poseStack.mulPose(Axis.XP.rotationDegrees(swingAmount * -10.0F));
                }

                this.renderItem(player, stack, arm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, poseStack, submitNodeCollector, combinedLight);
                poseStack.popPose();
            }
        }
    }

    private void renderOldSwordStance(PoseStack poseStack, int i) {
        poseStack.translate(-0.2F, 0.126F, 0.2F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-102.25F));
        poseStack.mulPose(Axis.YP.rotationDegrees(i * 15.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(i * 80.0F));
    }
}