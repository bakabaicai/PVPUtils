package com.pvp_utils.client.modules.impl.Tool.FoodInfo;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class FoodInfoHudOverlay {
    private static final FoodInfoHudOverlay INSTANCE = new FoodInfoHudOverlay();
    private static final int ICON_SIZE = 9;

    private final OffsetsCache offsets = new OffsetsCache();
    private float unclampedFlashAlpha;
    private float flashAlpha;
    private byte alphaDir = 1;

    private FoodInfoHudOverlay() {
    }

    public static FoodInfoHudOverlay getInstance() {
        return INSTANCE;
    }

    public void tick(Minecraft client) {
        if (!Config.foodInfo || client.player == null) {
            resetFlash();
            return;
        }
        unclampedFlashAlpha += alphaDir * 0.125f;
        if (unclampedFlashAlpha >= 1.5f) {
            alphaDir = -1;
        } else if (unclampedFlashAlpha <= -0.5f) {
            alphaDir = 1;
        }
        flashAlpha = Mth.clamp(unclampedFlashAlpha, 0.0f, 1.0f) * 0.65f;
    }

    public void onPreRenderFood(GuiGraphics graphics, Player player, int top, int right) {
        if (!Config.foodInfo || player == null) {
            return;
        }
        drawExhaustionOverlay(graphics, FoodInfoHelper.exhaustion(player), right, top);
    }

    public void onRenderFood(GuiGraphics graphics, Player player, int top, int right) {
        if (!Config.foodInfo || player == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        FoodData stats = player.getFoodData();
        int ticks = mc.gui != null ? mc.gui.getGuiTicks() : 0;
        drawSaturationOverlay(graphics, 0.0f, stats.getSaturationLevel(), right, top, 1.0f, ticks, player);

        FoodInfoHelper.QueriedFoodResult food = FoodInfoHelper.queryHeldFood(player);
        if (food == null) {
            resetFlash();
            return;
        }

        int hunger = food.modifiedFood().nutrition();
        float saturation = food.modifiedFood().saturation();
        drawHungerOverlay(graphics, hunger, stats.getFoodLevel(), right, top, flashAlpha, FoodInfoHelper.isRotten(food.consumable()), ticks, player);

        int newFoodValue = stats.getFoodLevel() + hunger;
        float newSaturationValue = stats.getSaturationLevel() + saturation;
        float saturationGained = newSaturationValue > newFoodValue ? newFoodValue - stats.getSaturationLevel() : saturation;
        drawSaturationOverlay(graphics, saturationGained, stats.getSaturationLevel(), right, top, flashAlpha, ticks, player);
    }

    public void onRenderHealth(GuiGraphics graphics, Player player, int left, int top) {
        if (!Config.foodInfo || player == null || !FoodInfoHelper.shouldShowEstimatedHealth(player)) {
            return;
        }
        FoodInfoHelper.QueriedFoodResult food = FoodInfoHelper.queryHeldFood(player);
        if (food == null) {
            return;
        }

        float gained = FoodInfoHelper.estimatedHealthIncrement(player, new ConsumableFood(food.modifiedFood(), food.consumable()));
        float currentHealth = player.getHealth();
        float modifiedHealth = Math.min(currentHealth + gained, player.getMaxHealth());
        drawHealthOverlay(graphics, currentHealth, modifiedHealth, left, top, flashAlpha, Minecraft.getInstance().gui.getGuiTicks(), player);
    }

    private void drawSaturationOverlay(GuiGraphics graphics, float saturationGained, float saturationLevel, int right, int top, float alpha, int ticks, Player player) {
        if (saturationLevel + saturationGained < 0.0f) {
            return;
        }
        int color = FoodInfoColor.argb(1.0f, 1.0f, 1.0f, alpha);
        float modifiedSaturation = Mth.clamp(saturationLevel + saturationGained, 0.0f, 20.0f);
        int start = saturationGained == 0.0f ? 0 : (int) Math.max(saturationLevel / 2.0f, 0.0f);
        int end = (int) Math.ceil(modifiedSaturation / 2.0f);

        List<IntPoint> foodOffsets = offsets.foodOffsets(ticks, player);
        for (int i = start; i < end; i++) {
            IntPoint offset = i < foodOffsets.size() ? foodOffsets.get(i) : new IntPoint(-i * 8 - 9, 0);
            int x = right + offset.x;
            int y = top + offset.y;
            float bar = (modifiedSaturation / 2.0f) - i;
            int u = bar >= 1.0f ? 27 : bar > 0.5f ? 18 : bar > 0.25f ? 9 : 0;
            graphics.blit(RenderPipelines.GUI_TEXTURED, FoodInfoTextures.MOD_ICONS, x, y, u, 0, ICON_SIZE, ICON_SIZE, 256, 256, color);
        }
    }

    private void drawHungerOverlay(GuiGraphics graphics, int hungerRestored, int foodLevel, int right, int top, float alpha, boolean rotten, int ticks, Player player) {
        if (hungerRestored <= 0) {
            return;
        }
        int color = FoodInfoColor.argb(1.0f, 1.0f, 1.0f, alpha);
        int modifiedFood = Mth.clamp(foodLevel + hungerRestored, 0, 20);
        int start = Math.max(0, foodLevel / 2);
        int end = (int) Math.ceil(modifiedFood / 2.0f);

        List<IntPoint> foodOffsets = offsets.foodOffsets(ticks, player);
        for (int i = start; i < end; i++) {
            IntPoint offset = i < foodOffsets.size() ? foodOffsets.get(i) : new IntPoint(-i * 8 - 9, 0);
            int x = right + offset.x;
            int y = top + offset.y;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FoodInfoTextures.food(rotten, FoodInfoTextures.FoodType.EMPTY), x, y, ICON_SIZE, ICON_SIZE, FoodInfoColor.argb(1.0f, 1.0f, 1.0f, alpha * 0.25f));
            boolean half = i * 2 + 1 == modifiedFood;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FoodInfoTextures.food(rotten, half ? FoodInfoTextures.FoodType.HALF : FoodInfoTextures.FoodType.FULL), x, y, ICON_SIZE, ICON_SIZE, color);
        }
    }

    private void drawHealthOverlay(GuiGraphics graphics, float health, float modifiedHealth, int left, int top, float alpha, int ticks, Player player) {
        if (modifiedHealth <= health) {
            return;
        }
        int color = FoodInfoColor.argb(1.0f, 1.0f, 1.0f, alpha);
        int fixedModifiedHealth = (int) Math.ceil(modifiedHealth);
        boolean hardcore = player.level().getLevelData().isHardcore();
        int start = (int) Math.max(0.0f, Math.ceil(health) / 2.0f);
        int end = (int) Math.max(0.0f, Math.ceil(modifiedHealth / 2.0f));

        List<IntPoint> healthOffsets = offsets.healthOffsets(ticks, player);
        for (int i = start; i < end; i++) {
            IntPoint offset = i < healthOffsets.size() ? healthOffsets.get(i) : new IntPoint(i % 10 * 8, i / 10 * -10);
            int x = left + offset.x;
            int y = top + offset.y;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FoodInfoTextures.heart(hardcore, FoodInfoTextures.HeartType.CONTAINER), x, y, ICON_SIZE, ICON_SIZE, FoodInfoColor.argb(1.0f, 1.0f, 1.0f, alpha * 0.25f));
            boolean half = i * 2 + 1 == fixedModifiedHealth;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FoodInfoTextures.heart(hardcore, half ? FoodInfoTextures.HeartType.HALF : FoodInfoTextures.HeartType.FULL), x, y, ICON_SIZE, ICON_SIZE, color);
        }
    }

    private void drawExhaustionOverlay(GuiGraphics graphics, float exhaustion, int right, int top) {
        float ratio = Mth.clamp(exhaustion / FoodInfoHelper.MAX_EXHAUSTION, 0.0f, 1.0f);
        int width = (int) (ratio * 81.0f);
        if (width <= 0) {
            return;
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, FoodInfoTextures.MOD_ICONS, right - width, top, 81 - width, 18, width, 9, 256, 256, FoodInfoColor.argb(1.0f, 1.0f, 1.0f, 0.75f));
    }

    private void resetFlash() {
        unclampedFlashAlpha = 0.0f;
        flashAlpha = 0.0f;
        alphaDir = 1;
    }

    private static final class OffsetsCache {
        private final List<IntPoint> foodOffsets = new ArrayList<>();
        private final List<IntPoint> healthOffsets = new ArrayList<>();
        private final Random random = new Random();
        private int lastTick = -1;

        List<IntPoint> foodOffsets(int tick, Player player) {
            generate(tick, player);
            return foodOffsets;
        }

        List<IntPoint> healthOffsets(int tick, Player player) {
            generate(tick, player);
            return healthOffsets;
        }

        private void generate(int tick, Player player) {
            if (tick == lastTick) {
                return;
            }
            lastTick = tick;
            foodOffsets.clear();
            healthOffsets.clear();

            float maxHealth = player.getMaxHealth();
            float absorption = (float) Math.ceil(player.getAbsorptionAmount());
            int healthBars = (int) Math.ceil((maxHealth + absorption) / 2.0f);
            if (healthBars < 0 || healthBars > 1000) {
                healthBars = 0;
            }
            int healthRows = (int) Math.ceil(healthBars / 10.0f);
            int healthRowHeight = Math.max(10 - (healthRows - 2), 3);
            boolean animateHealth = player.getHealth() + absorption <= 4.0f;

            random.setSeed(tick * 312871L);
            for (int i = 0; i < healthBars; i++) {
                int row = i / 10;
                int x = (i % 10) * 8;
                int y = -row * healthRowHeight;
                if (animateHealth) {
                    y += random.nextInt(2);
                }
                healthOffsets.add(new IntPoint(x, y));
            }

            FoodData food = player.getFoodData();
            boolean animateFood = food.getSaturationLevel() <= 0.0f && tick % (food.getFoodLevel() * 3 + 1) == 0;
            for (int i = 0; i < 10; i++) {
                int y = animateFood ? random.nextInt(3) - 1 : 0;
                foodOffsets.add(new IntPoint(-i * 8 - 9, y));
            }
        }
    }
}
