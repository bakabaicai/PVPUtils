package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
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
        if (player.isFallFlying()) return;
        if (player.hasEffect(MobEffects.SLOW_FALLING)) return;
        if (player.hasEffect(MobEffects.LEVITATION)) return;

        float fallDist = (float) player.fallDistance;
        if (fallDist <= 0.0f) return;

        Vec3 pos = player.position();
        if (isResettingFallDistance(level, player.getBoundingBox())) return;

        LandingInfo landing = findLanding(player, level, pos);
        if (landing == null) return;
        if (willResetBeforeLanding(player, level, pos.y, landing.y)) return;

        double totalFall = fallDist + Math.max(0.0, pos.y - landing.y);
        float safeFall = (float) player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
        float landingMultiplier = getLandingMultiplier(level, landing.pos);

        if (landingMultiplier == IMMUNE) return;

        float fallMultiplier = (float) player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER);
        int rawDamage = (int) Math.floor((totalFall + 1.0E-6 - safeFall) * landingMultiplier * fallMultiplier);

        if (rawDamage <= 0) return;

        if (player.hasEffect(MobEffects.RESISTANCE)) {
            int amplifier = player.getEffect(MobEffects.RESISTANCE).getAmplifier();
            int resistance = (amplifier + 1) * 5;
            rawDamage = Math.max((int) Math.floor(rawDamage * (25 - resistance) / 25.0f), 0);
        }

        if (rawDamage <= 0) return;

        float protection = getFallProtection(player);
        float afterProtection = protection > 0.0f ? rawDamage * (1.0f - Math.min(protection, 20.0f) / 25.0f) : rawDamage;

        if (afterProtection <= 0) return;

        boolean cn = Config.isChinese;
        boolean lethal = afterProtection >= player.getHealth();
        String text = (cn ? "预测伤害: " : "Predicted Damage: ") + String.format("%.1f", afterProtection)
                + (lethal ? (cn ? " (致死)" : " (Lethal)") : "");

        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        int textW = client.font.width(text);
        int textX = (screenW - textW) / 2;
        int textY = screenH / 2 + 16;

        int color = getDamageColor(afterProtection, player.getMaxHealth());
        graphics.drawString(client.font, Component.literal(text), textX, textY, color, true);
    }

    private boolean isResettingFallDistance(Level level, AABB box) {
        int minX = (int) Math.floor(box.minX + 1.0E-7);
        int maxX = (int) Math.floor(box.maxX - 1.0E-7);
        int minY = (int) Math.floor(box.minY + 1.0E-7);
        int maxY = (int) Math.floor(box.maxY - 1.0E-7);
        int minZ = (int) Math.floor(box.minZ + 1.0E-7);
        int maxZ = (int) Math.floor(box.maxZ - 1.0E-7);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isResettingState(level, pos)) return true;
                }
            }
        }
        return false;
    }

    private float getLandingMultiplier(Level level, BlockPos landPos) {
        BlockState state = level.getBlockState(landPos);

        if (!level.getFluidState(landPos).isEmpty()) return IMMUNE;
        if (isResettingBlock(state)) return IMMUNE;

        if (state.is(Blocks.SLIME_BLOCK)) return IMMUNE;
        if (state.is(Blocks.POWDER_SNOW)) return IMMUNE;

        if (state.is(Blocks.HAY_BLOCK)) return HAY_REDUCTION;
        if (state.is(Blocks.HONEY_BLOCK)) return HONEY_REDUCTION;
        if (state.is(BlockTags.BEDS)) return BED_REDUCTION;

        return 1.0f;
    }

    private boolean willResetBeforeLanding(Player player, Level level, double startY, double landingY) {
        AABB box = player.getBoundingBox();
        double width = box.getXsize() / 2.0 - 0.01;
        double depth = box.getZsize() / 2.0 - 0.01;
        double x = player.getX();
        double z = player.getZ();

        for (double y = startY; y >= landingY; y -= 0.25) {
            if (isResettingState(level, BlockPos.containing(x, y, z))) return true;
            if (isResettingState(level, BlockPos.containing(x - width, y, z - depth))) return true;
            if (isResettingState(level, BlockPos.containing(x - width, y, z + depth))) return true;
            if (isResettingState(level, BlockPos.containing(x + width, y, z - depth))) return true;
            if (isResettingState(level, BlockPos.containing(x + width, y, z + depth))) return true;
        }

        return false;
    }

    private boolean isResettingState(Level level, BlockPos pos) {
        if (level.getFluidState(pos).is(FluidTags.WATER)) return true;
        if (level.getFluidState(pos).is(FluidTags.LAVA)) return true;
        return isResettingBlock(level.getBlockState(pos));
    }

    private boolean isResettingBlock(BlockState state) {
        return state.is(BlockTags.FALL_DAMAGE_RESETTING) || state.is(Blocks.POWDER_SNOW);
    }

    private LandingInfo findLanding(Player player, Level level, Vec3 pos) {
        double searchY = pos.y;
        double minY = Math.max(level.getMinY(), searchY - 256);
        AABB playerBox = player.getBoundingBox();
        double width = playerBox.getXsize() / 2.0 - 0.01;
        double depth = playerBox.getZsize() / 2.0 - 0.01;

        for (double y = searchY; y >= minY; y -= 0.5) {
            BlockPos bp = BlockPos.containing(pos.x, y - 0.01, pos.z);
            BlockState state = level.getBlockState(bp);

            if (!level.getFluidState(bp).isEmpty()) return new LandingInfo(y, bp);

            if (!state.isAir() && !state.getCollisionShape(level, bp).isEmpty()) {
                AABB blockBox = state.getCollisionShape(level, bp).bounds().move(bp);
                if (blockBox.maxY >= y) return new LandingInfo(blockBox.maxY, bp);
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
                    if (cb.maxY >= y) return new LandingInfo(cb.maxY, corner);
                }
            }
        }

        return null;
    }

    private float getFallProtection(Player player) {
        float protection = 0.0f;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
            ItemStack stack = player.getItemBySlot(slot);
            ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (Object2IntMap.Entry<net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment>> entry : enchantments.entrySet()) {
                if (entry.getKey().is(Enchantments.PROTECTION)) {
                    protection += entry.getIntValue();
                } else if (entry.getKey().is(Enchantments.FEATHER_FALLING)) {
                    protection += entry.getIntValue() * 3.0f;
                }
            }
        }
        return protection;
    }

    private int getDamageColor(float damage, float maxHealth) {
        float ratio = damage / maxHealth;
        if (ratio < 0.25f) return 0xFFFFFF55;
        if (ratio < 0.5f) return 0xFFFFAA00;
        if (ratio < 0.75f) return 0xFFFF5500;
        return 0xFFFF2222;
    }

    private record LandingInfo(double y, BlockPos pos) {}
}
