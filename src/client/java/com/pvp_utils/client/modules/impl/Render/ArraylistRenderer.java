package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;

public class ArraylistRenderer {
    private static final ArraylistRenderer INSTANCE = new ArraylistRenderer();
    private static final long START_TIME_NANOS = System.nanoTime();
    private static final int LINE_HEIGHT = 10;
    private static final int PADDING_X = 2;
    private static final int BACKGROUND_PADDING_X = 3;
    private static final int BACKGROUND_TOP_PADDING = 1;
    private static final int BACKGROUND_COLOR = 0x66000000;
    private static final int PREVIEW_WIDTH = 104;
    private static final int PREVIEW_HEIGHT = 52;

    private static final List<Entry> ENTRIES = List.of(
            new Entry("Auto GG", () -> Config.autoGG),
            new Entry("Auto Main Hand", () -> Config.mainHandAssist),
            new Entry("Auto Sprint", () -> Config.autoSprint),
            new Entry("Arrow Trajectory Predict", () -> Config.arrowTrajectoryPredict),
            new Entry("Arraylist", () -> Config.arraylist),
            new Entry("Armor HUD", () -> Config.armorHud),
            new Entry("Attack Effects", () -> Config.attackEffectsCritParticles || Config.attackEffectsSharpnessParticles || Config.attackEffectsFlameParticles || Config.attackEffectsBloodParticles || Config.attackEffectsLightning),
            new Entry("Better Chat", () -> Config.betterChat),
            new Entry("Better Mouse Logic", () -> Config.betterMouseLogic),
            new Entry("Better Ping Display", () -> Config.betterPingDisplay),
            new Entry("Better Scoreboard", () -> Config.betterScoreboard),
            new Entry("Block Count Display", () -> Config.blockCountDisplay),
            new Entry("Custom Cape", () -> Config.customCape),
            new Entry("Damage Numbers", () -> Config.damageNumbers),
            new Entry("Digging Status", () -> Config.diggingStatus),
            new Entry("Dynamic Island", () -> Config.dynamicIsland),
            new Entry("Dynamic Motion Blur", () -> Config.dynamicMotionBlur),
            new Entry("Elytra Improvements", () -> Config.elytraAssist),
            new Entry("Fall Damage Predict", () -> Config.fallDamagePredict),
            new Entry("Food Info", () -> Config.foodInfo),
            new Entry("Freelook", () -> Config.freelook),
            new Entry("Gamma Override", () -> Config.gammaOverride),
            new Entry("Hit Color", () -> Config.hitColor),
            new Entry("Hit Marker", () -> Config.hitMarker),
            new Entry("Hit Sound", () -> Config.hitSound),
            new Entry("Item Physics", () -> Config.itemPhysics),
            new Entry("Dropped Item 2D Render", () -> Config.item2DRender),
            new Entry("Item Use Status", () -> Config.itemUseStatus),
            new Entry("Keystrokes", () -> Config.keystrokes),
            new Entry("Low Health Warning", () -> Config.lowHealthNotify),
            new Entry("Motion camera", () -> Config.motionCamera),
            new Entry("Music Info HUD", () -> Config.musicInfoHud),
            new Entry("Name Tag", () -> Config.nameTag),
            new Entry("No Swimming", () -> Config.noSwimming),
            new Entry("Potion Status", () -> Config.potionStatus),
            new Entry("Rainbow Enchantment Glint", () -> Config.customEnchantmentGlint),
            new Entry("Remove Container Background", () -> Config.removeContainerBackground),
            new Entry("Remove Attack Cooldown Animation", () -> Config.noAttackCooldownAnimation),
            new Entry("Sneak Animation Adjustment", () -> Config.noSneakAnimation),
            new Entry("Sword Blocking Animation", () -> Config.swordBlock),
            new Entry("Time Change", () -> Config.timeChange),
            new Entry("Use Animation", () -> Config.useSwing),
            new Entry("Victory Sound", () -> Config.victorySound),
            new Entry("Weather Change", () -> Config.weatherChange),
            new Entry("Zoom", () -> Config.zoom)
    );

    public static ArraylistRenderer getInstance() {
        return INSTANCE;
    }

    public void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        if (!Config.arraylist && !HudEditOverlay.getInstance().isActive()) return;
        if (client.player == null || client.options.hideGui) return;
        if (client.screen != null && !(client.screen instanceof ChatScreen) && !HudEditOverlay.getInstance().isActive()) return;

        List<String> names = activeNames();
        if (names.isEmpty() && HudEditOverlay.getInstance().isActive()) {
            names = previewNames();
        }
        if (names.isEmpty()) return;

        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        float scale = getScale();
        float x = getRenderX(screenW);
        float y = getRenderY(screenH);
        boolean alignRight = x + getEditWidth() * 0.5f >= screenW * 0.5f;

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);

        int shadowColor = Config.hudTheme == Config.HudTheme.LIGHT ? 0x33000000 : 0x66000000;
        int width = Math.round(baseWidth(names));
        drawBackground(graphics, client, names, width, alignRight);
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            int textW = textWidth(name);
            int textX = alignRight ? width - textW - PADDING_X : PADDING_X;
            int textY = i * LINE_HEIGHT;
            int color = textColor(i, names.size());
            graphics.drawString(client.font, name, textX + 1, textY + 1, shadowColor, false);
            graphics.drawString(client.font, name, textX, textY, color, false);
        }

        graphics.pose().popMatrix();
    }

    public float getEditWidth() {
        return (baseWidth(activeOrPreviewNames()) + borderOutset() * 2f) * getScale();
    }

    public float getEditHeight() {
        return (Math.max(PREVIEW_HEIGHT, activeOrPreviewNames().size() * LINE_HEIGHT) + borderOutset() * 2f) * getScale();
    }

    public float getEditX(int screenW) {
        return getRenderX(screenW) - getScaledBorderOutset();
    }

    public float getEditY(int screenH) {
        return getRenderY(screenH) - getScaledBorderOutset();
    }

    public float getScaledBorderOutset() {
        return borderOutset() * getScale();
    }

    public float getDefaultX(int screenW) {
        return 8f;
    }

    public float getDefaultY() {
        return 28f;
    }

    public float getRenderX(int screenW) {
        float scale = getScale();
        float contentW = baseWidth(activeOrPreviewNames()) * scale;
        float outset = borderOutset() * scale;
        return clamp(getDefaultX(screenW) + Config.arraylistX, outset, Math.max(outset, screenW - contentW - outset));
    }

    public float getRenderY(int screenH) {
        float scale = getScale();
        float contentH = Math.max(PREVIEW_HEIGHT, activeOrPreviewNames().size() * LINE_HEIGHT) * scale;
        float outset = borderOutset() * scale;
        return clamp(getDefaultY() + Config.arraylistY, outset, Math.max(outset, screenH - contentH - outset));
    }

    private List<String> activeNames() {
        ArrayList<String> names = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            if (entry.enabled().getAsBoolean()) {
                names.add(entry.name());
            }
        }
        names.sort(Comparator.comparingInt(this::textWidth).reversed().thenComparing(String::compareTo));
        return names;
    }

    private List<String> activeOrPreviewNames() {
        List<String> names = activeNames();
        return names.isEmpty() ? previewNames() : names;
    }

    private List<String> previewNames() {
        return List.of("Dynamic Motion Blur", "Better Scoreboard", "Keystrokes", "Zoom");
    }

    private float baseWidth(List<String> names) {
        int max = PREVIEW_WIDTH;
        for (String name : names) {
            max = Math.max(max, textWidth(name) + PADDING_X * 2);
        }
        return max;
    }

    private int textWidth(String text) {
        return Minecraft.getInstance().font.width(text);
    }

    private void drawBackground(GuiGraphics graphics, Minecraft client, List<String> names, int width, boolean alignRight) {
        for (int i = 0; i < names.size(); i++) {
            Bounds bounds = innerBoundsFor(client, names, width, alignRight, i);
            graphics.fill(bounds.x1(), bounds.y1(), bounds.x2(), bounds.y2(), BACKGROUND_COLOR);
        }
        drawBorder(graphics, client, names, width, alignRight);
    }

    private void drawBorder(GuiGraphics graphics, Minecraft client, List<String> names, int width, boolean alignRight) {
        if (!Config.arraylistBorder) return;
        int borderWidth = borderOutset();
        for (int i = 0; i < names.size(); i++) {
            Bounds outer = boundsFor(client, names, width, alignRight, i);
            Bounds inner = innerBoundsFor(client, names, width, alignRight, i);
            int color = textColor(i, names.size());

            fillRect(graphics, outer.x1(), outer.y1(), inner.x1(), outer.y2(), color);
            fillRect(graphics, inner.x2(), outer.y1(), outer.x2(), outer.y2(), color);
            if (i == 0) {
                fillRect(graphics, inner.x1(), outer.y1(), inner.x2(), inner.y1(), color);
            }
            if (i == names.size() - 1) {
                fillRect(graphics, inner.x1(), inner.y2(), inner.x2(), outer.y2(), color);
            }
            if (i + 1 < names.size()) {
                Bounds nextOuter = boundsFor(client, names, width, alignRight, i + 1);
                Bounds nextInner = innerBoundsFor(client, names, width, alignRight, i + 1);
                int sharedY1 = inner.y2() - borderWidth;
                int sharedY2 = inner.y2();
                fillRect(graphics, Math.min(inner.x1(), nextInner.x1()), sharedY1, Math.max(inner.x1(), nextInner.x1()), sharedY2, color);
                fillRect(graphics, Math.min(inner.x2(), nextInner.x2()), sharedY1, Math.max(inner.x2(), nextInner.x2()), sharedY2, color);
                fillRect(graphics, Math.min(outer.x1(), nextOuter.x1()), sharedY1, Math.max(outer.x1(), nextOuter.x1()), sharedY2, color);
                fillRect(graphics, Math.min(outer.x2(), nextOuter.x2()), sharedY1, Math.max(outer.x2(), nextOuter.x2()), sharedY2, color);
            }
        }
    }

    private void fillRect(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        if (x2 <= x1 || y2 <= y1) return;
        graphics.fill(x1, y1, x2, y2, color);
    }

    private Bounds boundsFor(Minecraft client, List<String> names, int width, boolean alignRight, int index) {
        int outset = borderOutset();
        String name = names.get(index);
        int textW = textWidth(name);
        int textX = alignRight ? width - textW - PADDING_X : PADDING_X;
        int bgX1 = textX - BACKGROUND_PADDING_X - outset;
        int bgX2 = textX + textW + BACKGROUND_PADDING_X + outset;
        int bgY1 = index * LINE_HEIGHT - (index == 0 ? BACKGROUND_TOP_PADDING + outset : 0);
        int bgY2 = (index + 1) * LINE_HEIGHT + (index == names.size() - 1 ? outset : 0);
        return new Bounds(bgX1, bgY1, bgX2, bgY2);
    }

    private Bounds innerBoundsFor(Minecraft client, List<String> names, int width, boolean alignRight, int index) {
        Bounds outer = boundsFor(client, names, width, alignRight, index);
        int borderWidth = borderOutset();
        if (borderWidth <= 0) {
            return outer;
        }
        int x1 = outer.x1() + borderWidth;
        int x2 = outer.x2() - borderWidth;
        int y1 = outer.y1() + (index == 0 ? borderWidth : 0);
        int y2 = outer.y2() - (index == names.size() - 1 ? borderWidth : 0);
        return new Bounds(x1, y1, x2, y2);
    }

    private int borderOutset() {
        return Config.arraylistBorder ? Math.max(1, Math.round(Config.arraylistBorderWidth)) : 0;
    }

    private int textColor(int index, int count) {
        int first = rgb(Config.arraylistColorRed, Config.arraylistColorGreen, Config.arraylistColorBlue);
        if (!Config.arraylistGradient) {
            return first;
        }
        int second = rgb(Config.arraylistGradientRed, Config.arraylistGradientGreen, Config.arraylistGradientBlue);
        float speed = Math.max(0.0f, Config.arraylistGradientSpeed);
        float y = index * LINE_HEIGHT + LINE_HEIGHT * 0.5f;
        float period = Math.max(LINE_HEIGHT * 6.0f, count * LINE_HEIGHT * 0.75f);
        float seconds = (System.nanoTime() - START_TIME_NANOS) / 1_000_000_000.0f;
        float offset = seconds * speed * LINE_HEIGHT * 3.0f;
        float phase = positiveModulo(y + offset, period) / period;
        float t = phase < 0.5f ? phase * 2.0f : (1.0f - phase) * 2.0f;
        t = smoothstep(t);
        return lerpColor(first, second, t);
    }

    private float positiveModulo(float value, float modulo) {
        float result = value % modulo;
        return result < 0f ? result + modulo : result;
    }

    private float smoothstep(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    private int rgb(int red, int green, int blue) {
        return 0xFF000000
                | (clampColor(red) << 16)
                | (clampColor(green) << 8)
                | clampColor(blue);
    }

    private int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int r = Math.round(ar + (br - ar) * t);
        int g = Math.round(ag + (bg - ag) * t);
        int bl = Math.round(ab + (bb - ab) * t);
        return rgb(r, g, bl);
    }

    private int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private float getScale() {
        return Math.max(0.5f, Config.arraylistScale);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Entry(String name, BooleanSupplier enabled) {
    }

    private record Bounds(int x1, int y1, int x2, int y2) {
    }
}
