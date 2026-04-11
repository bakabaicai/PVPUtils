package com.pvp_utils.client.gui;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class FallDamagePredictor {
    private static final FallDamagePredictor INSTANCE = new FallDamagePredictor();

    private static final float IMMUNE = 0.0f;
    private static final float HAY_REDUCTION = 0.2f;
    private static final float HONEY_REDUCTION = 0.2f;
    private static final float BED_REDUCTION = 0.5f;

    public static FallDamagePredictor getInstance() {
        return INSTANCE;
    }

    public void render(GuiGraphics graphics) {
        if (!Config.fallDamagePredict) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        Player player = client.player;
        Level level = client.level;

        if (player.onGround()) return;
        if (player.isInWater() || player.isInLava()) return;
        if (player.getAbilities().flying || player.getAbilities().invulnerable) return;
        if (player.hasEffect(MobEffects.SLOW_FALLING)) return;
        if (player.hasEffect(MobEffects.LEVITATION)) return;

        float fallDist = (float) player.fallDistance;
        if (fallDist <= 0.0f) return;

        Vec3 pos = player.position();
        BlockPos blockPos = BlockPos.containing(pos.x, pos.y, pos.z);

        if (isCurrentlyImmune(level, blockPos)) return;

        double groundY = findGroundY(player, level, pos);
        if (groundY == Double.MIN_VALUE) return;

        double totalFall = (pos.y - groundY) + fallDist;
        float safeFall = (float) player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
        float effectiveFall = (float) totalFall - safeFall;

        if (effectiveFall <= 0) return;

        BlockPos landPos = BlockPos.containing(pos.x, groundY, pos.z);
        float landingMultiplier = getLandingMultiplier(level, landPos);

        if (landingMultiplier == IMMUNE) return;

        float rawDamage = effectiveFall * landingMultiplier;

        if (player.hasEffect(MobEffects.RESISTANCE)) {
            int amplifier = player.getEffect(MobEffects.RESISTANCE).getAmplifier();
            rawDamage *= Math.max(0, 1.0f - (amplifier + 1) * 0.2f);
        }

        if (rawDamage <= 0) return;

        float armor = (float) player.getAttributeValue(Attributes.ARMOR);
        float toughness = (float) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float armorReduction = Math.min(20.0f, Math.max(armor / 5.0f, armor - (4.0f * rawDamage) / (toughness + 8.0f)));
        float afterArmor = rawDamage * (1.0f - armorReduction / 25.0f);

        if (afterArmor <= 0) return;

        boolean cn = Config.isChinese;
        boolean lethal = afterArmor >= player.getHealth();
        String text = (cn ? "预测伤害: " : "Predicted Damage: ") + String.format("%.1f", afterArmor)
                + (lethal ? (cn ? " (致死)" : " (Lethal)") : "");

        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        int textW = client.font.width(text);
        int textX = (screenW - textW) / 2;
        int textY = screenH / 2 + 16;

        int color = getDamageColor(afterArmor, player.getMaxHealth());
        graphics.drawString(client.font, Component.literal(text), textX, textY, color, true);
    }

    private boolean isCurrentlyImmune(Level level, BlockPos pos) {
        if (!level.getFluidState(pos).isEmpty()) return true;
        if (!level.getFluidState(pos.below()).isEmpty()) return true;
        BlockState below = level.getBlockState(pos.below());
        if (below.is(Blocks.COBWEB)) return true;
        if (below.is(Blocks.SWEET_BERRY_BUSH)) return true;
        return false;
    }

    private float getLandingMultiplier(Level level, BlockPos landPos) {
        BlockState state = level.getBlockState(landPos);
        BlockState above = level.getBlockState(landPos.above());

        if (!level.getFluidState(landPos).isEmpty()) return IMMUNE;
        if (!level.getFluidState(landPos.above()).isEmpty()) return IMMUNE;

        if (state.is(Blocks.SLIME_BLOCK)) return IMMUNE;
        if (above.is(Blocks.SLIME_BLOCK)) return IMMUNE;
        if (state.is(Blocks.POWDER_SNOW)) return IMMUNE;
        if (state.is(Blocks.COBWEB)) return IMMUNE;
        if (state.is(Blocks.SWEET_BERRY_BUSH)) return IMMUNE;

        if (state.is(Blocks.HAY_BLOCK)) return HAY_REDUCTION;
        if (state.is(Blocks.HONEY_BLOCK)) return HONEY_REDUCTION;
        if (state.is(BlockTags.BEDS)) return BED_REDUCTION;

        return 1.0f;
    }

    private double findGroundY(Player player, Level level, Vec3 pos) {
        double searchY = pos.y;
        double minY = Math.max(level.getMinY(), searchY - 256);
        AABB playerBox = player.getBoundingBox();
        double width = playerBox.getXsize() / 2.0 - 0.01;
        double depth = playerBox.getZsize() / 2.0 - 0.01;

        for (double y = searchY; y >= minY; y -= 0.5) {
            BlockPos bp = BlockPos.containing(pos.x, y - 0.01, pos.z);
            BlockState state = level.getBlockState(bp);

            if (!level.getFluidState(bp).isEmpty()) return y;

            if (!state.isAir() && !state.getCollisionShape(level, bp).isEmpty()) {
                AABB blockBox = state.getCollisionShape(level, bp).bounds().move(bp);
                if (blockBox.maxY >= y) return blockBox.maxY;
            }

            for (BlockPos corner : new BlockPos[]{
                    BlockPos.containing(pos.x - width, y - 0.01, pos.z),
                    BlockPos.containing(pos.x + width, y - 0.01, pos.z),
                    BlockPos.containing(pos.x, y - 0.01, pos.z - depth),
                    BlockPos.containing(pos.x, y - 0.01, pos.z + depth)
            }) {
                BlockState cs = level.getBlockState(corner);
                if (!cs.isAir() && !cs.getCollisionShape(level, corner).isEmpty()) {
                    AABB cb = cs.getCollisionShape(level, corner).bounds().move(corner);
                    if (cb.maxY >= y) return cb.maxY;
                }
            }
        }

        return Double.MIN_VALUE;
    }

    private int getDamageColor(float damage, float maxHealth) {
        float ratio = damage / maxHealth;
        if (ratio < 0.25f) return 0xFFFFFF55;
        if (ratio < 0.5f) return 0xFFFFAA00;
        if (ratio < 0.75f) return 0xFFFF5500;
        return 0xFFFF2222;
    }
}