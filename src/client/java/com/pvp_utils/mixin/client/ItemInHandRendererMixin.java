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
    @Shadow private void applyItemArmTransform(PoseStack poseStack, HumanoidArm humanoidArm, float f) {}
    @Shadow public abstract void renderItem(LivingEntity livingEntity, ItemStack itemStack, ItemDisplayContext itemDisplayContext, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i);

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
    private void injectOldAnimation(AbstractClientPlayer abstractClientPlayer, float f, float g, InteractionHand interactionHand, float h, ItemStack itemStack, float i, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int j, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        ItemStack mainHandStack = abstractClientPlayer.getMainHandItem();
        ItemStack offHandStack = abstractClientPlayer.getOffhandItem();

        boolean isSword = mainHandStack.is(ItemTags.SWORDS);
        boolean hasShield = offHandStack.is(Items.SHIELD);
        boolean hasTarget = Config.autoMode && isEntityInRange();

        boolean isBlocking = Config.swordBlock && isSword && (client.options.keyUse.isDown() || hasTarget || client.screen instanceof SettingsScreen);

        if (interactionHand == InteractionHand.OFF_HAND && isBlocking && hasShield) {
            ci.cancel();
            return;
        }

        if (interactionHand == InteractionHand.MAIN_HAND) {
            boolean isUseSwinging = Config.useSwing && abstractClientPlayer.isUsingItem();

            if (isBlocking || isUseSwinging) {
                ci.cancel();
                HumanoidArm arm = abstractClientPlayer.getMainArm();
                int side = arm == HumanoidArm.RIGHT ? 1 : -1;

                poseStack.pushPose();

                if (isBlocking) {
                    poseStack.translate(Config.offsetX * side, Config.offsetY, Config.offsetZ);
                    this.applyItemArmTransform(poseStack, arm, 0.0F);

                    if (Config.animationMode == Config.AnimMode.MODE_1_7) {
                        float factor = Mth.sin(Mth.sqrt(h) * (float) Math.PI);
                        float blend = 1.0F - factor;
                        poseStack.translate(side * (-0.139F * blend), 0.06F * blend, 0.20F * blend);

                        poseStack.translate(side * 0.430F, -0.190F, 0.520F);
                        poseStack.translate(side * -0.141F, 0.08F, -0.72F);
                        float f17 = Mth.sin(h * h * (float) Math.PI);
                        float f22 = Mth.sin(Mth.sqrt(h) * (float) Math.PI);
                        poseStack.mulPose(Axis.YP.rotationDegrees(side * (45.0F + f17 * -20.0F)));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(side * f22 * -20.0F));
                        poseStack.mulPose(Axis.XP.rotationDegrees(f22 * -80.0F));
                        poseStack.mulPose(Axis.YP.rotationDegrees(side * -45.0F));
                        renderOldSwordStance(poseStack, side);
                    } else if (Config.animationMode == Config.AnimMode.MODE_1_7_PLUS) {
                        poseStack.translate(side * 0.15F, -0.05F, 0.0F);
                        renderOldSwordStance(poseStack, side);

                        float sqrtSwing = Mth.sqrt(h);
                        float swingAmount = Mth.sin(sqrtSwing * (float) Math.PI);
                        poseStack.mulPose(Axis.XP.rotationDegrees(swingAmount * -45.0F));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(side * swingAmount * 20.0F));
                    } else if (Config.animationMode == Config.AnimMode.MODE_NEW) {
                        poseStack.translate(side * 0.15F, -0.05F, 0.0F);
                        renderOldSwordStance(poseStack, side);
                        if (h > 0.0F) {
                            float swingFactor = Mth.sin(Mth.sqrt(h) * (float) Math.PI);
                            poseStack.translate(side * 0.430F * swingFactor, -0.190F * swingFactor, 0.520F * swingFactor);
                            poseStack.translate(side * -0.141F * swingFactor, 0.08F * swingFactor, -0.72F * swingFactor);
                            float f17 = Mth.sin(h * h * (float) Math.PI);
                            float f22 = Mth.sin(Mth.sqrt(h) * (float) Math.PI);
                            poseStack.mulPose(Axis.YP.rotationDegrees(side * (45.0F + f17 * -20.0F)));
                            poseStack.mulPose(Axis.ZP.rotationDegrees(side * f22 * -20.0F));
                            poseStack.mulPose(Axis.XP.rotationDegrees(f22 * -80.0F));
                            poseStack.mulPose(Axis.YP.rotationDegrees(side * -45.0F));
                        }
                    } else {
                        poseStack.translate(side * 0.15F, -0.05F, 0.0F);
                        renderOldSwordStance(poseStack, side);

                        float sqrtSwing = Mth.sqrt(h);
                        float swingAmount = Mth.sin(sqrtSwing * (float) Math.PI);
                        poseStack.mulPose(Axis.ZP.rotationDegrees(-side * swingAmount * 35.0F));
                        poseStack.mulPose(Axis.XP.rotationDegrees(swingAmount * -10.0F));
                    }
                } else {
                    poseStack.translate(side * -0.66F, -0.06F, 0.0F);
                    this.applyItemArmTransform(poseStack, arm, 0.0F);

                    float speedH = h * 1.17F;
                    float f1 = Mth.sin(speedH * (float) Math.PI);
                    float f2 = Mth.sin(Mth.sqrt(speedH) * (float) Math.PI);
                    poseStack.translate(side * -0.4F * f2, 0.4F * f1, -0.3F * f2);
                    poseStack.mulPose(Axis.YP.rotationDegrees(side * 180.0F));
                    poseStack.mulPose(Axis.YP.rotationDegrees(side * 45.0F));
                    float f17 = Mth.sin(speedH * speedH * (float) Math.PI);
                    float f22 = Mth.sin(Mth.sqrt(speedH) * (float) Math.PI);
                    poseStack.mulPose(Axis.YP.rotationDegrees(side * (f17 * -20.0F)));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(side * f22 * -20.0F));
                    poseStack.mulPose(Axis.XP.rotationDegrees(f22 * -80.0F));
                    poseStack.mulPose(Axis.YP.rotationDegrees(side * -45.0F));
                    poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                }

                this.renderItem(abstractClientPlayer, itemStack, arm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, poseStack, submitNodeCollector, j);
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