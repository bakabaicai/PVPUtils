package com.pvp_utils.client.modules.impl.Tool.FoodInfo;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;

public class FoodInfoTooltipComponent implements TooltipComponent, ClientTooltipComponent {
    private final FoodProperties defaultFood;
    private final FoodProperties modifiedFood;
    private final Consumable consumable;
    private final int hungerBars;
    private final String hungerBarsText;
    private final int saturationBars;
    private final String saturationBarsText;

    public FoodInfoTooltipComponent(ItemStack stack, Player player) {
        FoodInfoHelper.QueriedFoodResult result = FoodInfoHelper.query(stack, player);
        if (result == null) {
            this.defaultFood = new FoodProperties(0, 0.0f, false);
            this.modifiedFood = this.defaultFood;
            this.consumable = null;
            this.hungerBars = 0;
            this.hungerBarsText = null;
            this.saturationBars = 0;
            this.saturationBarsText = null;
            return;
        }
        this.defaultFood = result.defaultFood();
        this.modifiedFood = result.modifiedFood();
        this.consumable = result.consumable();

        int biggestHunger = Math.max(defaultFood.nutrition(), modifiedFood.nutrition());
        float biggestSaturation = Math.max(defaultFood.saturation(), modifiedFood.saturation());
        int computedHungerBars = (int) Math.ceil(Math.abs(biggestHunger) / 2.0f);
        String computedHungerText = null;
        if (computedHungerBars > 10) {
            computedHungerText = "x" + ((biggestHunger < 0 ? -1 : 1) * computedHungerBars);
            computedHungerBars = 1;
        }

        int computedSaturationBars = (int) Math.ceil(Math.abs(biggestSaturation) / 2.0f);
        String computedSaturationText = null;
        if (computedSaturationBars > 10 || computedSaturationBars == 0) {
            computedSaturationText = "x" + ((biggestSaturation < 0 ? -1 : 1) * computedSaturationBars);
            computedSaturationBars = 1;
        }
        this.hungerBars = computedHungerBars;
        this.hungerBarsText = computedHungerText;
        this.saturationBars = computedSaturationBars;
        this.saturationBarsText = computedSaturationText;
    }

    public boolean shouldRender() {
        return hungerBars > 0;
    }

    @Override
    public int getHeight(Font font) {
        return 20;
    }

    @Override
    public int getWidth(Font font) {
        int hungerWidth = hungerBars * 9 + (hungerBarsText == null ? 0 : font.width(hungerBarsText));
        int saturationWidth = saturationBars * 7 + (saturationBarsText == null ? 0 : font.width(saturationBarsText));
        return Math.max(hungerWidth, saturationWidth);
    }

    @Override
    public void renderImage(Font font, int x, int y, int width, int height, GuiGraphics graphics) {
        renderHunger(font, graphics, x, y);
        renderSaturation(font, graphics, x, y + 10);
    }

    private void renderHunger(Font font, GuiGraphics graphics, int x, int y) {
        int drawX = x + (hungerBars - 1) * 9;
        boolean rotten = consumable != null && FoodInfoHelper.isRotten(consumable);
        int defaultHunger = defaultFood.nutrition();
        int modifiedHunger = modifiedFood.nutrition();

        for (int i = 0; i < hungerBars * 2; i += 2) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FoodInfoTextures.FOOD_EMPTY, drawX, y, 9, 9);
            FoodOutline outline = FoodOutline.get(modifiedHunger, defaultHunger, i);
            if (outline != FoodOutline.NORMAL) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FoodInfoTextures.HUNGER_OUTLINE_SPRITE, drawX, y, 9, 9, outline.argb());
            }
            boolean defaultHalf = defaultHunger - 1 == i;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FoodInfoTextures.food(rotten, defaultHalf ? FoodInfoTextures.FoodType.HALF : FoodInfoTextures.FoodType.FULL), drawX, y, 9, 9, FoodInfoColor.argb(1.0f, 1.0f, 1.0f, 0.25f));
            if (modifiedHunger > i) {
                boolean modifiedHalf = modifiedHunger - 1 == i;
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FoodInfoTextures.food(rotten, modifiedHalf ? FoodInfoTextures.FoodType.HALF : FoodInfoTextures.FoodType.FULL), drawX, y, 9, 9);
            }
            drawX -= 9;
        }
        if (hungerBarsText != null) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(drawX + 18, y);
            graphics.pose().scale(0.75f, 0.75f);
            graphics.drawString(font, hungerBarsText, 2, 2, 0xFFAAAAAA, true);
            graphics.pose().popMatrix();
        }
    }

    private void renderSaturation(Font font, GuiGraphics graphics, int x, int y) {
        int drawX = x + (saturationBars - 1) * 7;
        float saturation = modifiedFood.saturation();
        float absSaturation = Math.abs(saturation);
        for (int i = 0; i < saturationBars * 2; i += 2) {
            float bar = (absSaturation - i) / 2.0f;
            boolean faded = absSaturation <= i;
            int color = faded ? FoodInfoColor.argb(1.0f, 1.0f, 1.0f, 0.5f) : 0xFFFFFFFF;
            int u = bar >= 1.0f ? 21 : bar > 0.5f ? 14 : bar > 0.25f ? 7 : bar > 0.0f ? 0 : 28;
            int v = saturation >= 0.0f ? 27 : 34;
            graphics.blit(RenderPipelines.GUI_TEXTURED, FoodInfoTextures.MOD_ICONS, drawX, y, u, v, 7, 7, 256, 256, color);
            drawX -= 7;
        }
        if (saturationBarsText != null) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(drawX + 14, y);
            graphics.pose().scale(0.75f, 0.75f);
            graphics.drawString(font, saturationBarsText, 2, 1, 0xFFAAAAAA, true);
            graphics.pose().popMatrix();
        }
    }

    private enum FoodOutline {
        NEGATIVE,
        EXTRA,
        NORMAL,
        PARTIAL,
        MISSING;

        int argb() {
            return switch (this) {
                case NEGATIVE -> FoodInfoColor.argb(1.0f, 1.0f, 1.0f, 1.0f);
                case EXTRA -> FoodInfoColor.argb(0.06f, 0.32f, 0.02f, 1.0f);
                case NORMAL -> FoodInfoColor.argb(0.0f, 0.0f, 0.0f, 1.0f);
                case PARTIAL -> FoodInfoColor.argb(0.53f, 0.21f, 0.08f, 1.0f);
                case MISSING -> FoodInfoColor.argb(0.62f, 0.0f, 0.0f, 0.5f);
            };
        }

        static FoodOutline get(int modifiedFoodHunger, int defaultFoodHunger, int i) {
            if (modifiedFoodHunger < 0) {
                return NEGATIVE;
            } else if (modifiedFoodHunger > defaultFoodHunger && defaultFoodHunger <= i) {
                return EXTRA;
            } else if (modifiedFoodHunger > i + 1 || defaultFoodHunger == modifiedFoodHunger) {
                return NORMAL;
            } else if (modifiedFoodHunger == i + 1) {
                return PARTIAL;
            }
            return MISSING;
        }
    }
}
