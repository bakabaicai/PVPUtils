package com.pvp_utils.client.modules.impl.Render.KillBanner;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Render.HudEditOverlay;
import com.pvp_utils.client.render.chroma.ChromaGraphics;
import com.pvp_utils.client.render.chroma.ChromaDraw;
import io.github.humbleui.skija.BlendMode;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.skija.Shader;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.UUID;

public final class KillBannerRenderer {
    private static final KillBannerRenderer INSTANCE = new KillBannerRenderer();

    private static final float MAIN_W = 364f;
    private static final float MAIN_H = 75f;
    private static final float LOWER_W = 518f;
    private static final float LOWER_H = 88f;
    private static final float GAP = 16f;

    private static final long MAIN_GROW_MS = 66L;
    private static final long GLITCH_START_MS = 200L;
    private static final long GLITCH_END_MS = 300L;
    private static final long LOWER_START_MS = 864L;
    private static final long LOWER_GROW_MS = 233L;
    private static final long LOWER_REVEAL_MS = 170L;
    private static final long EXIT_MS = 340L;
    private static final long EXIT_WHITEN_MS = 80L;
    private static final long EXIT_COLLAPSE_MS = 240L;
    private static final long VICTORY_COOLDOWN_MS = 5000L;

    private static final float[] MAIN_GROW = {0f, 0.593f, 0.5f, 0.824f, 1f, 1f};
    private static final float[] LOWER_GROW = {0f, 0.077f, 0.141f, 0.247f, 0.428f, 0.463f, 0.712f, 0.873f, 1f, 1f};

    private static final float GRID_PITCH = 17f;
    private static final float GRID_TILE = 13f;

    private static final int GOLD = 0xFFF3CE69;
    private static final int GOLD_DIM = 0xFFD9B65A;
    private static final int CREAM = 0xFFF0E4C0;
    private static final int WHITE_FLASH = 0xFFF4F2EA;
    private static final int GLITCH_BASE = 0xFFF3F0C6;
    private static final int GLITCH_FRAME = 0xFFE0C86A;
    private static final int GRID_COLOR = 0xFFFFC042;

    private final Paint avatarPaint = new Paint();

    private volatile long startMs = -1L;
    private volatile KillBannerData data;
    private volatile Layout layout;
    private volatile boolean layoutDirty;
    private boolean editPreviewStarted;
    private UUID avatarOwner;
    private Identifier avatarTexture;
    private static long lastVictoryMs;

    private KillBannerRenderer() {
    }

    public static KillBannerRenderer getInstance() {
        return INSTANCE;
    }

    public void show(KillBannerData value) {
        if (value == null) return;
        Layout previous = layout;
        data = value;
        layout = null;
        layoutDirty = true;
        startMs = System.currentTimeMillis();
        if (previous != null) previous.close();
    }

    public void hide() {
        startMs = -1L;
        data = null;
        Layout previous = layout;
        layout = null;
        layoutDirty = false;
        if (previous != null) previous.close();
    }

    public void close() {
        Layout current = layout;
        layout = null;
        layoutDirty = false;
        if (current != null) current.close();
    }

    public void onVictory(String title, String subtitle) {
        if (!Config.killBanner || !Config.killBannerOnVictory) return;
        long now = System.currentTimeMillis();
        if (now - lastVictoryMs < VICTORY_COOLDOWN_MS) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        lastVictoryMs = now;
        String killer = client.player.getName().getString();
        show(new KillBannerData("ROUND WIN", killer, true));
    }

    public boolean isActive() {
        return startMs >= 0L && data != null;
    }

    public static float effectiveScale(float guiW) {
        return Math.min(Math.max(0.5f, Math.min(2f, Config.killBannerScale)),
                Math.max(0.4f, (guiW - 24f) / LOWER_W));
    }

    public static float editTop(float guiH, float scale) {
        return guiH * 0.30f - MAIN_H * scale * 0.5f;
    }

    public static float editWidth(float scale) {
        return LOWER_W * scale;
    }

    public static float editHeight(float scale) {
        return (MAIN_H + GAP + LOWER_H) * scale;
    }

    public void moveTo(float centerX, float topY, float guiW, float guiH, float scale) {
        Config.killBannerXOffset = centerX - guiW * 0.5f;
        Config.killBannerYOffset = topY - editTop(guiH, scale);
    }

    public void renderChroma(Canvas canvas) {
        if (canvas == null) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.options.hideGui) return;
        HudEditOverlay editOverlay = HudEditOverlay.getInstance();
        boolean editing = Config.killBanner && editOverlay.isActive() && !editOverlay.isClosing();
        if (!editing && editPreviewStarted) {
            editPreviewStarted = false;
            if (startMs >= 0L && data != null) {
                startMs = System.currentTimeMillis() - Math.max(1500L, Math.round(Config.killBannerDuration * 1000f));
            }
        }
        if (!Config.killBanner) return;
        if (editing && !editPreviewStarted) {
            String name = client.player == null ? "Player" : client.player.getName().getString();
            show(new KillBannerData("ROUND WIN", name, true));
            editPreviewStarted = true;
        }

        long start = startMs;
        KillBannerData snapshot = data;
        if (start < 0L || snapshot == null) return;

        long elapsed = System.currentTimeMillis() - start;
        long holdMs = Math.max(1500L, Math.round(Config.killBannerDuration * 1000f));
        if (editing) {
            elapsed = Math.min(elapsed, holdMs);
        } else if (elapsed > holdMs + EXIT_MS) {
            hide();
            return;
        }

        Layout current = layout;
        if (current == null || layoutDirty) {
            current = buildLayout(snapshot);
            layout = current;
            layoutDirty = false;
        }
        float guiW = client.getWindow().getGuiScaledWidth();
        float guiH = client.getWindow().getGuiScaledHeight();
        float scale = effectiveScale(guiW);
        float mainY = editTop(guiH, scale) + Config.killBannerYOffset;

        int save = canvas.save();
        try {
            canvas.translate(guiW * 0.5f + Config.killBannerXOffset, mainY);
            canvas.scale(scale, scale);
            if (elapsed <= holdMs) {
                drawMain(canvas, current, elapsed);
                drawLower(canvas, current, elapsed);
                return;
            }
            long exitTime = elapsed - holdMs;
            float whiten = clamp(exitTime / (float) EXIT_WHITEN_MS);
            float contentAlpha = 1f - whiten;
            drawStableMain(canvas, current, contentAlpha, elapsed);
            drawStableLower(canvas, current, contentAlpha, elapsed);
            float collapse = (exitTime - EXIT_WHITEN_MS) / (float) EXIT_COLLAPSE_MS;
            float mainWidth = MAIN_W * (1f - easeIn(collapse));
            float lowerWidth = LOWER_W * (1f - easeIn((exitTime - EXIT_WHITEN_MS * 0.5f) / (float) EXIT_COLLAPSE_MS));
            if (whiten > 0f) {
                ChromaDraw.fill(canvas, -mainWidth * 0.5f, 0f, mainWidth, MAIN_H, a(WHITE_FLASH, whiten));
                drawFadedFill(canvas, -lowerWidth * 0.5f, MAIN_H + GAP, lowerWidth, LOWER_H,
                        a(WHITE_FLASH, whiten));
            }
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void drawMain(Canvas canvas, Layout current, long elapsed) {
        if (elapsed < GLITCH_START_MS) {
            float width = MAIN_W * segment(MAIN_GROW, elapsed / (float) MAIN_GROW_MS);
            ChromaDraw.fill(canvas, -width * 0.5f, 0f, width, MAIN_H, WHITE_FLASH);
            return;
        }
        if (elapsed < GLITCH_END_MS) {
            float reveal = (elapsed - GLITCH_START_MS) / (float) (GLITCH_END_MS - GLITCH_START_MS);
            drawGlitch(canvas, elapsed);
            drawStableMain(canvas, current, reveal, elapsed);
            return;
        }
        drawStableMain(canvas, current, 1f, elapsed);
    }

    private void drawGlitch(Canvas canvas, long elapsed) {
        float x = -MAIN_W * 0.5f;
        ChromaDraw.fill(canvas, x, 0f, MAIN_W, MAIN_H, GLITCH_BASE);
        int step = (int) (elapsed / 33f);
        float barX = x + 2f;
        for (int i = 0; i < 9; i++) {
            float r1 = noise(i, step);
            float r2 = noise(i + 61, step);
            float width = 1f + Math.round(r2 * 5f);
            if (barX + width > x + MAIN_W * 0.34f) break;
            int color = switch (i % 3) {
                case 0 -> 0x66FFFFFF;
                case 1 -> 0x308C7434;
                default -> 0x40FFFBE0;
            };
            ChromaDraw.fill(canvas, barX, 0f, width, MAIN_H, color);
            barX += width + 1f + Math.round(r1 * 7f);
        }
        for (int i = 0; i < 11; i++) {
            float r1 = noise(i + 137, step);
            float r2 = noise(i + 211, step);
            float r3 = noise(i + 307, step);
            float smearY = r1 * MAIN_H;
            float smearH = 1.5f + r2 * 7f;
            float smearX = x + 12f + r3 * (MAIN_W - 24f);
            float smearW = 26f + r1 * 120f;
            ChromaDraw.fill(canvas, smearX, smearY, Math.min(smearW, x + MAIN_W - smearX - 2f), smearH,
                    i % 2 == 0 ? 0x46A9924C : 0x3AFFFFFF);
        }
        ChromaDraw.fill(canvas, x + 2f, 0f, 2f, MAIN_H, GLITCH_FRAME);
        ChromaDraw.fill(canvas, x + MAIN_W - 4f, 0f, 2f, MAIN_H, GLITCH_FRAME);
        ChromaDraw.fill(canvas, x, 0f, MAIN_W, 1.5f, GLITCH_FRAME);
        ChromaDraw.fill(canvas, x, MAIN_H - 1.5f, MAIN_W, 1.5f, GLITCH_FRAME);
    }

    private void drawStableMain(Canvas canvas, Layout current, float alpha, long elapsed) {
        if (alpha <= 0f) return;
        float x = -MAIN_W * 0.5f;
        ChromaDraw.gradient(canvas, x, 0f, MAIN_W, MAIN_H, a(0xF0221406, alpha), a(0xF03A240A, alpha), false, 0f);

        ChromaDraw.fill(canvas, x + 2f, 0f, 2f, MAIN_H, a(GOLD, alpha));
        ChromaDraw.fill(canvas, x + MAIN_W - 4f, 0f, 2f, MAIN_H, a(GOLD, alpha));
        ChromaDraw.fill(canvas, x, MAIN_H - 1f, MAIN_W, 1f, a(0x30F3CE69, alpha));
        drawChevrons(canvas, alpha);
        float titleX = -current.title.width() * 0.5f;
        float titleY = (MAIN_H - 44f) * 0.5f - 3f;
        current.title.drawTop(canvas, titleX, titleY, a(GOLD, alpha));
    }

    private void drawChevrons(Canvas canvas, float alpha) {
        float cy = MAIN_H * 0.5f;
        float radius = 7f;
        int color = a(GOLD_DIM, alpha);
        for (int i = 0; i < 2; i++) {
            float leftCx = -MAIN_W * 0.5f + 26f + i * 7f;
            ChromaDraw.line(canvas, leftCx - radius * 0.5f, cy - radius, leftCx + radius * 0.5f, cy, 2.2f, color);
            ChromaDraw.line(canvas, leftCx + radius * 0.5f, cy, leftCx - radius * 0.5f, cy + radius, 2.2f, color);
            float rightCx = MAIN_W * 0.5f - 34f - i * 7f;
            ChromaDraw.line(canvas, rightCx + radius * 0.5f, cy - radius, rightCx - radius * 0.5f, cy, 2.2f, color);
            ChromaDraw.line(canvas, rightCx - radius * 0.5f, cy, rightCx + radius * 0.5f, cy + radius, 2.2f, color);
        }
    }

    private void drawLower(Canvas canvas, Layout current, long elapsed) {
        if (elapsed < LOWER_START_MS) return;
        long t = elapsed - LOWER_START_MS;
        if (t < LOWER_GROW_MS) {
            float width = LOWER_W * segment(LOWER_GROW, t / (float) LOWER_GROW_MS);
            drawFadedFill(canvas, -width * 0.5f, MAIN_H + GAP, width, LOWER_H, WHITE_FLASH);
            return;
        }
        float reveal = clamp((t - LOWER_GROW_MS) / (float) LOWER_REVEAL_MS);
        if (reveal < 1f) {
            drawFadedFill(canvas, -LOWER_W * 0.5f, MAIN_H + GAP, LOWER_W, LOWER_H, WHITE_FLASH);
        }
        drawStableLower(canvas, current, reveal, elapsed);
    }

    private void drawStableLower(Canvas canvas, Layout current, float alpha, long elapsed) {
        if (alpha <= 0f) return;
        float x = -LOWER_W * 0.5f;
        float y = MAIN_H + GAP;
        drawBack(canvas, x, y, LOWER_W, LOWER_H, 0xF52A1804, 0xF53A2408, alpha, elapsed);

        drawAvatar(canvas, x + 110f, y + 10f, 66f, alpha);

        if (current.mvp) {
            float badgeX = x + 191f;
            float badgeY = y + 13f;
            float badgeW = 33f;
            float badgeH = 20f;
            ChromaDraw.fill(canvas, badgeX, badgeY, badgeW, badgeH, a(0xFFE9BE45, alpha));
            current.mvpText.drawTop(canvas, badgeX + (badgeW - current.mvpText.width()) * 0.5f,
                    badgeY + 4f, a(0xFF1A1206, alpha));
        }

        current.killer.drawTop(canvas, x + 191f, y + 36f, a(GOLD, alpha));
    }

    private void drawBack(Canvas canvas, float x, float y, float w, float h,
                          int startColor, int endColor, float alpha, long elapsed) {
        if (alpha <= 0f) return;
        Rect bounds = Rect.makeXYWH(x, y, w, h);
        int layer;
        try (Paint layerPaint = new Paint()) {
            layer = canvas.saveLayer(bounds, layerPaint);
        }
        try {
            ChromaDraw.gradient(canvas, x, y, w, h, a(startColor, alpha), a(endColor, alpha), false, 0f);
            drawGrid(canvas, x, y, w, h, alpha, elapsed);
        } finally {
            applyEdgeMask(canvas, bounds, x, y, w);
            canvas.restoreToCount(layer);
        }
    }

    private void drawFadedFill(Canvas canvas, float x, float y, float w, float h, int color) {
        if (w <= 0.5f || h <= 0f || ((color >>> 24) & 0xFF) == 0) return;
        Rect bounds = Rect.makeXYWH(x, y, w, h);
        int layer;
        try (Paint layerPaint = new Paint()) {
            layer = canvas.saveLayer(bounds, layerPaint);
        }
        try {
            ChromaDraw.fill(canvas, x, y, w, h, color);
        } finally {
            applyEdgeMask(canvas, bounds, x, y, w);
            canvas.restoreToCount(layer);
        }
    }

    private void applyEdgeMask(Canvas canvas, Rect bounds, float x, float y, float w) {
        float fade = Math.min(0.42f, Math.min(0.12f, 30f / Math.max(1f, w)));
        float near = fade * 0.55f;
        try (Paint mask = new Paint().setBlendMode(BlendMode.DST_IN);
             Shader shader = Shader.makeLinearGradient(x, y, x + w, y,
                     new int[]{0x00000000, 0x55000000, 0xFF000000, 0xFF000000, 0x55000000, 0x00000000},
                     new float[]{0f, near, fade, 1f - fade, 1f - near, 1f})) {
            mask.setShader(shader);
            canvas.drawRect(bounds, mask);
        }
    }

    private void drawGrid(Canvas canvas, float x, float y, float w, float h, float alpha, long elapsed) {
        int cols = (int) Math.ceil(w / GRID_PITCH);
        int rows = (int) Math.ceil(h / GRID_PITCH);
        float seconds = elapsed * 0.001f;
        for (int row = 0; row < rows; row++) {
            float tileY = y + row * GRID_PITCH;
            float tileH = Math.min(GRID_TILE, y + h - tileY);
            if (tileH <= 0f) break;
            for (int col = 0; col < cols; col++) {
                float tileX = x + col * GRID_PITCH;
                float tileW = Math.min(GRID_TILE, x + w - tileX);
                if (tileW <= 0f) break;
                int id = row * 197 + col * 31;
                float phase = noise(id, 7) * 6.2831855f;
                float speed = 0.6f + noise(id, 17) * 1.6f;
                float pulse = 0.5f + 0.5f * (float) Math.sin(seconds * speed + phase);
                float level = 0.25f * pulse * pulse;
                ChromaDraw.fill(canvas, tileX, tileY, tileW, tileH, a(GRID_COLOR, alpha * level));
            }
        }
    }

    private void drawAvatar(Canvas canvas, float x, float y, float size, float alpha) {
        ChromaDraw.fill(canvas, x - 2f, y - 2f, size + 4f, size + 4f, a(0xA0120C03, alpha));
        Identifier texture = avatarTexture();
        Rect destination = Rect.makeXYWH(x, y, size, size);
        if (texture == null) {
            ChromaDraw.fill(canvas, x, y, size, size, a(0xFF3A2A12, alpha));
            return;
        }
        int save = canvas.save();
        try (ChromaGraphics.BorrowedImage borrowed = ChromaGraphics.borrowTexture(texture)) {
            if (borrowed == null) {
                ChromaDraw.fill(canvas, x, y, size, size, a(0xFF3A2A12, alpha));
                return;
            }
            avatarPaint.setAlpha(Math.round(clamp(alpha) * 255f));
            canvas.clipRect(destination);
            canvas.drawImageRect(borrowed.image(), Rect.makeXYWH(8f, 8f, 8f, 8f),
                    destination, SamplingMode.DEFAULT, avatarPaint, true);
            canvas.drawImageRect(borrowed.image(), Rect.makeXYWH(40f, 8f, 8f, 8f),
                    destination, SamplingMode.DEFAULT, avatarPaint, true);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private Identifier avatarTexture() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return null;
        UUID owner = client.player.getUUID();
        if (avatarTexture != null && owner.equals(avatarOwner)) return avatarTexture;
        avatarOwner = owner;
        avatarTexture = null;
        try {
            PlayerSkin skin = client.getSkinManager().createLookup(client.player.getGameProfile(), false).get();
            if (skin != null) avatarTexture = skin.body().texturePath();
        } catch (Exception ignored) {
        }
        return avatarTexture;
    }

    private Layout buildLayout(KillBannerData snapshot) {
        return new Layout(
                ChromaDraw.prepareText(snapshot.title(), 36f, true),
                ChromaDraw.prepareText(snapshot.killer(), 20f, true),
                ChromaDraw.prepareText("MVP", 10f, true),
                snapshot.mvp());
    }

    private static final class Layout implements AutoCloseable {
        private final ChromaDraw.PreparedText title;
        private final ChromaDraw.PreparedText killer;
        private final ChromaDraw.PreparedText mvpText;
        private final boolean mvp;

        private Layout(ChromaDraw.PreparedText title, ChromaDraw.PreparedText killer,
                       ChromaDraw.PreparedText mvpText, boolean mvp) {
            this.title = title;
            this.killer = killer;
            this.mvpText = mvpText;
            this.mvp = mvp;
        }

        @Override
        public void close() {
            title.close();
            killer.close();
            mvpText.close();
        }
    }

    private static int a(int color, float factor) {
        int alpha = Math.round(((color >>> 24) & 0xFF) * clamp(factor));
        return (alpha << 24) | (color & 0xFFFFFF);
    }

    private static float segment(float[] keys, float t) {
        int pairs = keys.length / 2;
        float value = clamp(t);
        if (value <= keys[0]) return keys[1];
        for (int i = 1; i < pairs; i++) {
            float end = keys[i * 2];
            if (value <= end) {
                float start = keys[(i - 1) * 2];
                float from = keys[(i - 1) * 2 + 1];
                float to = keys[i * 2 + 1];
                float span = end - start;
                return span <= 0f ? to : from + (to - from) * ((value - start) / span);
            }
        }
        return keys[pairs * 2 - 1];
    }

    private static float easeIn(float value) {
        float t = clamp(value);
        return t * t * t;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static float noise(int a, int b) {
        long value = (a * 73856093L) ^ (b * 19349663L);
        value ^= value >>> 13;
        value *= 1274126177L;
        value ^= value >>> 16;
        return (value & 0xFFFFL) / 65535f;
    }
}
