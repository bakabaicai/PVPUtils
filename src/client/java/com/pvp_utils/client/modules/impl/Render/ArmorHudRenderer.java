package com.pvp_utils.client.modules.impl.Render;

import io.github.humbleui.types.RRect;

import com.pvp_utils.client.render.skia.SkijaUi;

import com.pvp_utils.client.render.skia.SkijaRenderer;
import com.pvp_utils.Config;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class ArmorHudRenderer {
    private static final float CARD_W = 54f;
    private static final float CARD_H = 18f;
    private static final float CARD_MIN_W = 18f;
    private static final float CARD_MIN_H = 18f;
    private static final float CARD_GAP = 2.5f;
    private static final float CARD_ICON = 16f;
    private static final float CARD_PAD = 2f;
    private static final float HOTBAR_W = 182f;
    private static final float SIDE_GAP = 3f;

    private static final float LITE_ICON = 16f;
    private static final float LITE_GAP = 2.0f;
    private static final float LITE_TEXT_GAP = 2.5f;
    private static final float LITE_TEXT_H = 8f;

    private static final ArmorHudRenderer INSTANCE = new ArmorHudRenderer();

    private final Paint bgPaint = new Paint().setAntiAlias(true);
    private final Paint borderPaint = new Paint().setAntiAlias(true).setMode(PaintMode.STROKE).setStrokeWidth(1f);
    private final Paint barTrackPaint = new Paint().setAntiAlias(true);
    private final Paint barFillPaint = new Paint().setAntiAlias(true);

    public static ArmorHudRenderer getInstance() {
        return INSTANCE;
    }

    public void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        if (!Config.armorHud) {
            return;
        }
        if (client.player == null || client.level == null || client.options.hideGui || client.screen instanceof com.pvp_utils.client.render.skia.SkijaScreen) {
            return;
        }

        ArmorEntry[] entries = collectEntries(client);
        if (entries.length == 0) {
            return;
        }

        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        if (Config.armorHudMode == Config.ArmorHudMode.LITE) {
            renderLite(graphics, client, entries, screenW, screenH);
        } else {
            renderNew(graphics, client, entries, screenW, screenH);
        }
    }

    public boolean isPositionLockedToAdaptiveLayout() {
        return Config.armorHudMode == Config.ArmorHudMode.NEW && Config.armorHudLayout == Config.ArmorHudLayout.SEPARATED;
    }

    public float getEditWidth() {
        float scale = getRenderScale();
        if (Config.armorHudMode == Config.ArmorHudMode.NEW) {
            if (Config.armorHudLayout == Config.ArmorHudLayout.SEPARATED) {
                return CARD_W * scale * 2f + HOTBAR_W + SIDE_GAP * 2f;
            }
            if (Config.armorHudLayout == Config.ArmorHudLayout.VERTICAL) {
                return CARD_W * scale;
            }
            return (CARD_W * scale * 4f + CARD_GAP * 3f * scale);
        }

        Config.ArmorHudLayout layout = liteLayoutMode();
        if (layout == Config.ArmorHudLayout.VERTICAL) {
            return LITE_ICON * scale;
        }
        return (LITE_ICON * 4f + LITE_GAP * 3f) * scale;
    }

    public float getEditHeight() {
        float scale = getRenderScale();
        if (Config.armorHudMode == Config.ArmorHudMode.NEW) {
            if (Config.armorHudLayout == Config.ArmorHudLayout.SEPARATED) {
                return CARD_H * scale * 2f + CARD_GAP * scale;
            }
            if (Config.armorHudLayout == Config.ArmorHudLayout.VERTICAL) {
                return (CARD_H * scale * 4f + CARD_GAP * 3f * scale);
            }
            return CARD_H * scale;
        }

        float itemHeight = liteItemHeight() * scale;
        Config.ArmorHudLayout layout = liteLayoutMode();
        if (layout == Config.ArmorHudLayout.VERTICAL) {
            return itemHeight * 4f + LITE_GAP * 3f * scale;
        }
        return itemHeight;
    }

    public float getRenderX(int screenW) {
        if (isPositionLockedToAdaptiveLayout()) {
            float scale = getRenderScale();
            float hotbarX = (screenW - HOTBAR_W) * 0.5f;
            float leftX = hotbarX - CARD_W * scale - SIDE_GAP;
            return clamp(leftX, 0f, Math.max(0f, screenW - getEditWidth()));
        }
        return clamp((screenW - getEditWidth()) * 0.5f + Config.armorHudX, 0f, Math.max(0f, screenW - getEditWidth()));
    }

    public float getRenderY(int screenH) {
        if (isPositionLockedToAdaptiveLayout()) {
            float scale = getRenderScale();
            float hotbarY = screenH - 22f;
            float topY = hotbarY - CARD_H * scale * 2f - CARD_GAP * scale - 3f;
            return Math.max(0f, topY);
        }
        return clamp(screenH - 22f - getEditHeight() - 6f + Config.armorHudY, 0f, Math.max(0f, screenH - getEditHeight()));
    }

    private void renderNew(GuiGraphics graphics, Minecraft client, ArmorEntry[] entries, int screenW, int screenH) {
        NewLayout layout = makeNewLayout(entries.length, screenW, screenH);

        for (int i = 0; i < entries.length; i++) {
            ArmorEntry entry = entries[i];

            float x = layout.xs[i];
            float y = layout.ys[i];
            renderNewCard(graphics, entry.stack, x, y);
        }
    }

    private void renderNewCard(GuiGraphics graphics, ItemStack stack, float x, float y) {
        float scale = getRenderScale();
        float drawH = CARD_H * scale;
        SkijaRenderer.draw(canvas -> {
            canvas.translate(x, y);
            canvas.scale(scale, scale);
            drawNewCard(canvas, stack);
        });

        float iconX = x + CARD_PAD * scale;
        float iconY = y + (drawH - CARD_ICON * scale) * 0.5f;
        graphics.pose().pushMatrix();
        graphics.pose().translate(iconX, iconY);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-iconX, -iconY);
        graphics.renderFakeItem(stack, Math.round(iconX), Math.round(iconY));
        graphics.pose().popMatrix();
    }

    private void renderLite(GuiGraphics graphics, Minecraft client, ArmorEntry[] entries, int screenW, int screenH) {
        LiteLayout layout = makeLiteLayout(entries.length, screenW, screenH);
        for (int i = 0; i < entries.length; i++) {
            ArmorEntry entry = entries[i];
            float x = layout.xs[i];
            float y = layout.ys[i];
            renderLiteItem(graphics, client, entry.stack, x, y, layout.scale);
            if (Config.armorHudShowPercentage) {
                renderLitePercent(graphics, client, entry.stack, x, y + LITE_ICON * layout.scale + LITE_TEXT_GAP * layout.scale, layout.scale);
            }
        }
    }

    private void renderLiteItem(GuiGraphics graphics, Minecraft client, ItemStack stack, float x, float y, float scale) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-x, -y);
        int ix = Math.round(x);
        int iy = Math.round(y);
        graphics.renderFakeItem(stack, ix, iy);
        if (Config.armorHudShowBar) {
            graphics.renderItemDecorations(client.font, stack, ix, iy);
        }
        graphics.pose().popMatrix();
    }

    private void renderLitePercent(GuiGraphics graphics, Minecraft client, ItemStack stack, float x, float y, float scale) {
        if (!stack.isDamageableItem() || stack.getMaxDamage() <= 0) {
            return;
        }
        float ratio = clamp((stack.getMaxDamage() - stack.getDamageValue()) / (float) stack.getMaxDamage(), 0f, 1f);
        String text = Math.round(ratio * 100f) + "%";
        int textX = Math.round(x + (LITE_ICON * scale - client.font.width(text)) * 0.5f);
        graphics.drawString(client.font, text, textX, Math.round(y), 0xFFFFFFFF, false);
    }

    private void drawNewCard(Canvas canvas, ItemStack stack) {
        bgPaint.setColor(0x7A262D39);
        float cardW = newCardWidth();
        float cardH = newCardHeight();
        canvas.drawRRect(RRect.makeXYWH(0.5f, 0.5f, cardW - 1f, cardH - 1f, 5f), bgPaint);
        borderPaint.setColor(0x4AFFFFFF);
        canvas.drawRRect(RRect.makeXYWH(1f, 1f, cardW - 2f, cardH - 2f, 5f), borderPaint);

        if (!stack.isDamageableItem() || stack.getMaxDamage() <= 0) {
            return;
        }

        float ratio = clamp((stack.getMaxDamage() - stack.getDamageValue()) / (float) stack.getMaxDamage(), 0f, 1f);
        String percent = Math.round(ratio * 100f) + "%";
        if (Config.armorHudShowPercentage) {
            float textW = SkijaUi.textWidth(percent, 8.5f);
            float textX = Math.max(CARD_PAD + CARD_ICON + CARD_PAD, cardW - CARD_PAD - textW);
            SkijaUi.text(canvas, percent, textX, (12.4f) + SkijaUi.textMetrics(8.5f).getAscent(), SkijaUi.textMetrics(8.5f).getDescent() - SkijaUi.textMetrics(8.5f).getAscent(), 0xFFFFFFFF, 8.5f);
        }

        if (Config.armorHudShowBar) {
            float barX = CARD_PAD + CARD_ICON + CARD_PAD;
            float barY = 14.2f;
            float barW = Math.max(6f, cardW - barX - CARD_PAD - 2f);
            barTrackPaint.setColor(0x36FFFFFF);
            canvas.drawRRect(RRect.makeXYWH(barX, barY, barW, 1.8f, 0.9f), barTrackPaint);
            barFillPaint.setColor(durabilityColor(ratio));
            canvas.drawRRect(RRect.makeXYWH(barX, barY, Math.max(1f, barW * ratio), 1.8f, 0.9f), barFillPaint);
        }
    }

    private ArmorEntry[] collectEntries(Minecraft client) {
        ArmorEntry[] raw = new ArmorEntry[] {
            new ArmorEntry(client.player.getItemBySlot(EquipmentSlot.HEAD)),
            new ArmorEntry(client.player.getItemBySlot(EquipmentSlot.CHEST)),
            new ArmorEntry(client.player.getItemBySlot(EquipmentSlot.LEGS)),
            new ArmorEntry(client.player.getItemBySlot(EquipmentSlot.FEET))
        };
        int count = 0;
        for (ArmorEntry entry : raw) {
            if (!entry.stack.isEmpty()) {
                count++;
            }
        }
        ArmorEntry[] entries = new ArmorEntry[count];
        int idx = 0;
        for (ArmorEntry entry : raw) {
            if (!entry.stack.isEmpty()) {
                entries[idx++] = entry;
            }
        }
        return entries;
    }

    private NewLayout makeNewLayout(int count, int screenW, int screenH) {
        float scale = getRenderScale();
        float cardW = CARD_W * scale;
        float cardH = CARD_H * scale;
        float gap = CARD_GAP * scale;
        float hotbarX = (screenW - HOTBAR_W) * 0.5f;
        float hotbarY = screenH - 22f;

        if (Config.armorHudLayout == Config.ArmorHudLayout.VERTICAL) {
            float totalH = count * cardH + Math.max(0, count - 1) * gap;
            float x = clamp((screenW - cardW) * 0.5f + Config.armorHudX, 0f, Math.max(0f, screenW - cardW));
            float y = clamp(screenH - 22f - totalH - 6f + Config.armorHudY, 0f, Math.max(0f, screenH - totalH));
            float[] xs = new float[count];
            float[] ys = new float[count];
            for (int i = 0; i < count; i++) {
                xs[i] = x;
                ys[i] = y + i * (cardH + gap);
            }
            return new NewLayout(xs, ys);
        }

        if (Config.armorHudLayout == Config.ArmorHudLayout.HORIZONTAL) {
            float totalW = count * cardW + Math.max(0, count - 1) * gap;
            float x = clamp((screenW - totalW) * 0.5f + Config.armorHudX, 0f, Math.max(0f, screenW - totalW));
            float y = clamp(screenH - 22f - cardH - 6f + Config.armorHudY, 0f, Math.max(0f, screenH - cardH));
            float[] xs = new float[count];
            float[] ys = new float[count];
            for (int i = 0; i < count; i++) {
                xs[i] = x + i * (cardW + gap);
                ys[i] = y;
            }
            return new NewLayout(xs, ys);
        }

        float leftX = clamp(hotbarX - cardW - SIDE_GAP, 0f, Math.max(0f, screenW - cardW));
        float rightX = clamp(hotbarX + HOTBAR_W + SIDE_GAP, 0f, Math.max(0f, screenW - cardW));
        float topY = Math.max(0f, hotbarY - cardH * 2f - gap - 3f);
        float bottomY = topY + cardH + gap;
        return switch (count) {
            case 1 -> new NewLayout(new float[] {leftX}, new float[] {bottomY});
            case 2 -> new NewLayout(new float[] {leftX, rightX}, new float[] {bottomY, bottomY});
            case 3 -> new NewLayout(new float[] {leftX, leftX, rightX}, new float[] {topY, bottomY, bottomY});
            default -> new NewLayout(new float[] {leftX, leftX, rightX, rightX}, new float[] {topY, bottomY, topY, bottomY});
        };
    }

    private LiteLayout makeLiteLayout(int count, int screenW, int screenH) {
        float scale = getRenderScale();
        float icon = LITE_ICON * scale;
        float gap = LITE_GAP * scale;
        float itemH = liteItemHeight() * scale;
        Config.ArmorHudLayout layout = liteLayoutMode();

        if (layout == Config.ArmorHudLayout.VERTICAL) {
            float totalH = count * itemH + Math.max(0, count - 1) * gap;
            float x = clamp((screenW - icon) * 0.5f + Config.armorHudX, 0f, Math.max(0f, screenW - icon));
            float y = clamp(screenH - 22f - totalH - 6f + Config.armorHudY, 0f, Math.max(0f, screenH - totalH));
            float[] xs = new float[count];
            float[] ys = new float[count];
            for (int i = 0; i < count; i++) {
                xs[i] = x;
                ys[i] = y + i * (itemH + gap);
            }
            return new LiteLayout(xs, ys, scale);
        }

        float totalW = count * icon + Math.max(0, count - 1) * gap;
        float x = clamp((screenW - totalW) * 0.5f + Config.armorHudX, 0f, Math.max(0f, screenW - totalW));
        float y = clamp(screenH - 22f - itemH - 6f + Config.armorHudY, 0f, Math.max(0f, screenH - itemH));
        float[] xs = new float[count];
        float[] ys = new float[count];
        for (int i = 0; i < count; i++) {
            xs[i] = x + i * (icon + gap);
            ys[i] = y;
        }
        return new LiteLayout(xs, ys, scale);
    }

    private Config.ArmorHudLayout liteLayoutMode() {
        return Config.armorHudLayout == Config.ArmorHudLayout.VERTICAL ? Config.ArmorHudLayout.VERTICAL : Config.ArmorHudLayout.HORIZONTAL;
    }

    private float liteItemHeight() {
        return LITE_ICON + (Config.armorHudShowPercentage ? (LITE_TEXT_GAP + LITE_TEXT_H) : 0f);
    }

    private String stackSignature(ItemStack stack) {
        return stack.getItem() + ":" + stack.getDamageValue() + ":" + stack.getMaxDamage();
    }

    private int durabilityColor(float ratio) {
        ratio = clamp(ratio, 0f, 1f);
        int start;
        int end;
        float t;
        if (ratio >= 0.5f) {
            start = 0xFFFFD166;
            end = 0xFF63E37D;
            t = (ratio - 0.5f) / 0.5f;
        } else {
            start = 0xFFFF5C5C;
            end = 0xFFFFD166;
            t = ratio / 0.5f;
        }
        return lerpColor(start, end, t);
    }

    private int lerpColor(int start, int end, float t) {
        t = clamp(t, 0f, 1f);
        int sa = (start >>> 24) & 0xFF;
        int sr = (start >>> 16) & 0xFF;
        int sg = (start >>> 8) & 0xFF;
        int sb = start & 0xFF;
        int ea = (end >>> 24) & 0xFF;
        int er = (end >>> 16) & 0xFF;
        int eg = (end >>> 8) & 0xFF;
        int eb = end & 0xFF;
        int a = Math.round(sa + (ea - sa) * t);
        int r = Math.round(sr + (er - sr) * t);
        int g = Math.round(sg + (eg - sg) * t);
        int b = Math.round(sb + (eb - sb) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private float getRenderScale() {
        return Math.max(0.5f, Config.armorHudScale);
    }

    private float newCardWidth() {
        return CARD_W;
    }

    private float newCardHeight() {
        return CARD_H;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }

    private static final class ArmorEntry {
        final ItemStack stack;

        ArmorEntry(ItemStack stack) {
            this.stack = stack;
        }
    }

    private static final class NewLayout {
        final float[] xs;
        final float[] ys;

        NewLayout(float[] xs, float[] ys) {
            this.xs = xs;
            this.ys = ys;
        }
    }

    private static final class LiteLayout {
        final float[] xs;
        final float[] ys;
        final float scale;

        LiteLayout(float[] xs, float[] ys, float scale) {
            this.xs = xs;
            this.ys = ys;
            this.scale = scale;
        }
    }
}
