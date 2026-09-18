package com.pvp_utils.client.render.MainUI;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaGlBackend;
import com.pvp_utils.client.render.skia.SkiaBlurRenderer;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PVPUtilsSingleplayerScreen extends Screen {
    private static final long OPEN_MS = 440L;
    private static final long CLOSE_MS = 440L;
    private final String shaderPath;
    private final Runnable embeddedBack;
    private final SkiaGlBackend glBackend = new SkiaGlBackend();
    private final List<WorldEntry> worlds = new ArrayList<>();
    private final List<Float> worldHover = new ArrayList<>();
    private final Map<Path, Image> worldImages = new HashMap<>();
    private final List<ActionButton> buttons = new ArrayList<>();
    private CompletableFuture<?> loadFuture;
    private long openStartMs;
    private long closeStartMs;
    private boolean closingToMain;
    private boolean pendingFrame;
    private int pendingMouseX;
    private int pendingMouseY;
    private float scroll;
    private float targetScroll;
    private int selected = -1;
    private int lastWorldClick = -1;
    private long lastWorldClickMs;
    private boolean loading = true;
    private String loadError = "";
    private long contentReadyMs = 0L;
    private float contentAlpha;
    private boolean backDispatched;

    public PVPUtilsSingleplayerScreen(Screen parent, String shaderPath) {
        this(parent, shaderPath, null);
    }

    public PVPUtilsSingleplayerScreen(Screen parent, String shaderPath, Runnable embeddedBack) {
        super(Component.literal("Single player"));
        this.shaderPath = shaderPath;
        this.embeddedBack = embeddedBack;
    }

    public void initEmbedded(Minecraft client, int width, int height) {
        init(width, height);
    }

    @Override
    protected void init() {
        openStartMs = System.currentTimeMillis();
        closeStartMs = 0L;
        closingToMain = false;
        backDispatched = false;
        contentAlpha = 0f;
        rebuildButtons();
        loadWorlds();
    }

    private void rebuildButtons() {
        buttons.clear();
        buttons.add(new ActionButton("Play", () -> openSelectedWorld()));
        buttons.add(new ActionButton("Create", () -> {
            if (minecraft != null) CreateWorldScreen.openFresh(minecraft, () -> {
                loadWorlds();
                minecraft.setScreen(this);
            });
        }));
        buttons.add(new ActionButton("Edit", () -> openEditScreen()));
        buttons.add(new ActionButton("Delete", () -> deleteSelectedWorld()));
        buttons.add(new ActionButton("Refresh", this::loadWorlds));
        buttons.add(new ActionButton("Back", this::startClose));
        updateButtonBounds();
    }

    private void updateButtonBounds() {
        if (buttons.isEmpty()) return;
        float x = cardX() + 10f;
        float w = cardW() - 20f;
        float gap = 5f;
        float y1 = cardY() + cardH() + 10f;
        float y2 = y1 + 36f;
        float w1 = (w - gap * 2f) / 3f;
        float w2 = (w - gap * 2f) / 3f;
        for (int i = 0; i < buttons.size(); i++) {
            if (i < 3) {
                buttons.get(i).set(x + i * (w1 + gap), y1, w1, 30f);
            } else {
                buttons.get(i).set(x + (i - 3) * (w2 + gap), y2, w2, 30f);
            }
        }
    }

    private void loadWorlds() {
        if (minecraft == null) return;
        loading = true;
        loadError = "";
        contentReadyMs = 0L;
        LevelStorageSource source = minecraft.getLevelSource();
        loadFuture = source.loadLevelSummaries(source.findLevelCandidates()).thenAccept(summaries -> {
            List<WorldEntry> loaded = summaries.stream()
                    .sorted(Comparator.comparingLong(LevelSummary::getLastPlayed).reversed())
                    .map(WorldEntry::new)
                    .toList();
            Minecraft.getInstance().execute(() -> {
                worlds.clear();
                worlds.addAll(loaded);
                worldHover.clear();
                for (int i = 0; i < worlds.size(); i++) {
                    worldHover.add(0f);
                }
                selected = worlds.isEmpty() ? -1 : Math.max(0, Math.min(selected, worlds.size() - 1));
                loading = false;
                contentReadyMs = System.currentTimeMillis();
                clampScroll();
            });
        }).exceptionally(t -> {
            Minecraft.getInstance().execute(() -> {
                loading = false;
                contentReadyMs = System.currentTimeMillis();
                loadError = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
            });
            return null;
        });
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        updateButtonBounds();
        clampScroll();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (embeddedBack == null && minecraft.screen == this) {
            MainUISharedBackground.render(graphics, mouseX, mouseY);
        }
        scroll += (targetScroll - scroll) * 0.24f;
        float layoutScale = layoutScale();
        pendingMouseX = MainUiScale.pageX(mouseX, width, layoutScale, layoutCenterX());
        pendingMouseY = MainUiScale.pageY(mouseY, height, layoutScale, layoutCenterY());
        pendingFrame = true;
        if (closingToMain && closeProgress() >= 1f && minecraft != null) {
            if (embeddedBack != null) {
                if (!backDispatched) {
                    backDispatched = true;
                    if (minecraft.screen == this) {
                        minecraft.setScreen(PVPUtilsMainUI.returningFromSingleplayer(shaderPath));
                    } else {
                        embeddedBack.run();
                    }
                }
            } else {
                minecraft.setScreen(PVPUtilsMainUI.returningFromSingleplayer(shaderPath));
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    }

    @Override
    protected void renderBlurredBackground(GuiGraphics guiGraphics) {
    }

    @Override
    protected void renderMenuBackground(GuiGraphics guiGraphics) {
    }

    public void renderFrameEnd() {
        if (!pendingFrame || minecraft == null || (embeddedBack == null && minecraft.screen != this)) {
            pendingFrame = false;
            return;
        }
        Canvas canvas = glBackend.begin(mainFramebufferId());
        if (canvas == null) return;
        try {
            float layoutScale = layoutScale();
            if (!closingToMain) {
                SkiaBlurRenderer.getInstance().render(
                        canvas,
                        glBackend.getContext(),
                        Minecraft.getInstance(),
                        mainFramebufferId(),
                        MainUiScale.pageScreenX(cardX(), width, layoutScale, layoutCenterX()),
                        MainUiScale.pageScreenY(cardY(), height, layoutScale, layoutCenterY()),
                        MainUiScale.pageScreenSize(cardW(), layoutScale),
                        MainUiScale.pageScreenSize(cardH(), layoutScale),
                        MainUiScale.pageScreenSize(18f, layoutScale),
                        0x12000000,
                        blurStrength()
                );
            }
            canvas.save();
            MainUiScale.applyPage(canvas, width, height, layoutScale, layoutCenterX(), layoutCenterY());
            draw(canvas);
            canvas.restore();
        } finally {
            glBackend.end();
            pendingFrame = false;
        }
    }

    private void draw(Canvas canvas) {
        float open = ease(openProgress());
        float close = ease(closeProgress());
        float t = 1f;
        float cardW = cardW();
        float cardH = cardH();
        float compactW = Math.max(210f, Math.min(246f, layoutWidth() * 0.36f));
        float compactH = 36f + 6f * 34f + 5f * 2f;
        float angle = 0f;
        float drawW = compactW + (cardW - compactW) * t;
        float drawH = compactH + (cardH - compactH) * t;
        float cx = layoutWidth() * 0.5f;
        float cy = layoutHeight() * 0.5f;
        float y = cardY();
        try (Paint card = new Paint(); Paint stroke = new Paint()) {
            card.setAntiAlias(true);
            card.setColor(0x32101010);
            drawBookPageCard(canvas, cx, y, drawW, drawH, angle, card);
            stroke.setAntiAlias(true);
            stroke.setMode(PaintMode.STROKE);
            stroke.setStrokeWidth(1f);
            stroke.setColor(0x22FFFFFF);
            drawBookPageCard(canvas, cx, y + 0.5f, drawW - 1f, drawH - 1f, angle, stroke);
        }
        float contentTarget = closingToMain ? 0f : (!loading ? 1f : 0f);
        contentAlpha += (contentTarget - contentAlpha) * 0.16f;
        float contentFade = ease(contentAlpha);
        int alpha = Math.round(255f * contentFade);
        float titleSize = 30f;
        String title = "Single player";
        FontRenderer.drawText(canvas, title, (layoutWidth() - FontRenderer.measureTextWidth(title, titleSize)) * 0.5f, 44f, titleSize, (alpha << 24) | 0xFFFFFF);
        drawWorldList(canvas, alpha);
        float buttonsAlpha = contentFade;
        for (ActionButton button : buttons) {
            button.draw(canvas, pendingMouseX, pendingMouseY, buttonsAlpha);
        }
    }

    private float revealFade() {
        long now = System.currentTimeMillis();
        long start = contentReadyMs > 0L ? contentReadyMs : openStartMs;
        long elapsed = now - start;
        if (elapsed <= 0L) return 0f;
        return ease(Math.min(1f, elapsed / 280f));
    }

    private void drawWorldList(Canvas canvas, int alpha) {
        float x = cardX() + 14f;
        float y = cardY() + 16f;
        float w = cardW() - 28f;
        float h = cardH() - 32f;
        canvas.save();
        canvas.clipRect(Rect.makeXYWH(x, y, w, h));
        if (loading) {
            drawCentered(canvas, "Loading worlds...", x + w * 0.5f, y + h * 0.45f, 14f, (alpha << 24) | 0xFFFFFF);
        } else if (!loadError.isBlank()) {
            drawCentered(canvas, loadError, x + w * 0.5f, y + h * 0.45f, 12f, (alpha << 24) | 0xFFB8B8);
        } else if (worlds.isEmpty()) {
            drawCentered(canvas, "No worlds", x + w * 0.5f, y + h * 0.45f, 14f, (alpha << 24) | 0xFFFFFF);
        } else {
            float itemH = 72f;
            float iy = y - scroll;
            for (int i = 0; i < worlds.size(); i++) {
                if (iy + itemH >= y && iy <= y + h) drawWorld(canvas, worlds.get(i), i, x, iy, w, itemH, alpha);
                iy += itemH + 8f;
            }
        }
        canvas.restore();
    }

    private void drawWorld(Canvas canvas, WorldEntry world, int index, float x, float y, float w, float h, int alpha) {
        boolean hover = pendingMouseX >= x && pendingMouseX <= x + w && pendingMouseY >= y && pendingMouseY <= y + h;
        boolean sel = selected == index;
        if (index >= worldHover.size()) {
            while (worldHover.size() <= index) worldHover.add(0f);
        }
        float hoverProgress = worldHover.get(index);
        hoverProgress += ((hover ? 1f : 0f) - hoverProgress) * 0.16f;
        worldHover.set(index, hoverProgress);
        float hoverCurve = hoverProgress * hoverProgress * (3f - 2f * hoverProgress);
        try (Paint bg = new Paint(); Paint icon = new Paint(); Paint imagePaint = new Paint()) {
            bg.setAntiAlias(true);
            float backgroundAlpha = sel ? 0.18f + 0.08f * hoverCurve : 0.07f + 0.11f * hoverCurve;
            bg.setColor((Math.round(alpha * backgroundAlpha) << 24) | 0xFFFFFF);
            canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 14f), bg);
            icon.setAntiAlias(true);
            icon.setColor((Math.round(alpha * (0.10f + 0.12f * hoverCurve)) << 24) | 0xFFFFFF);
            canvas.drawRRect(RRect.makeXYWH(x + 10f, y + 10f, 52f, 52f, 12f), icon);
            Image worldImage = worldImage(world);
            if (worldImage != null) {
                float imageX = x + 10f;
                float imageY = y + 10f;
                float imageSize = 52f;
                canvas.save();
                canvas.clipRRect(RRect.makeXYWH(imageX, imageY, imageSize, imageSize, 12f), true);
                imagePaint.setAntiAlias(true);
                imagePaint.setColor((alpha << 24) | 0xFFFFFF);
                canvas.drawImageRect(
                        worldImage,
                        Rect.makeXYWH(0f, 0f, worldImage.getWidth(), worldImage.getHeight()),
                        Rect.makeXYWH(imageX, imageY, imageSize, imageSize),
                        SamplingMode.LINEAR,
                        imagePaint,
                        true
                );
                canvas.restore();
            }
        }
        if (worldImage(world) == null) {
            String worldIcon = "\uE30A";
            float iconSize = 25f;
            float iconWidth = FontRenderer.measureTextWidth(worldIcon, iconSize, FontRenderer.MATERIAL_SYMBOLS);
            FontRenderer.drawText(canvas, worldIcon, x + 36f - iconWidth * 0.5f, y + 43f, iconSize,
                    (Math.round(alpha * (0.72f + 0.20f * hoverCurve)) << 24) | 0xFFFFFF,
                    FontRenderer.MATERIAL_SYMBOLS);
        }
        FontRenderer.drawText(canvas, world.name(), x + 74f, y + 28f, 15f, (alpha << 24) | 0xFFFFFF);
        FontRenderer.drawText(canvas, world.info(), x + 74f, y + 47f, 11f, (Math.round(alpha * 0.68f) << 24) | 0xFFFFFF);
    }

    private void drawCentered(Canvas canvas, String text, float cx, float y, float size, int color) {
        FontRenderer.drawText(canvas, text, cx - FontRenderer.measureTextWidth(text, size) * 0.5f, y, size, color);
    }

    private void drawBookPageCard(Canvas canvas, float cx, float y, float w, float h, float angle, Paint paint) {
        float sin = (float) Math.sin(angle);
        float cos = (float) Math.cos(angle);
        if (Math.abs(sin) < 0.03f && cos > 0.99f) {
            canvas.drawRRect(RRect.makeXYWH(cx - w * 0.5f, y, w, h, 18f), paint);
            return;
        }
        float scaleX = Math.copySign(Math.max(0.065f, Math.abs(cos)), cos);
        canvas.save();
        canvas.translate(cx, y + h * 0.5f);
        canvas.skew(sin * 0.10f, 0f);
        canvas.scale(scaleX, 1f);
        canvas.drawRRect(RRect.makeXYWH(-w * 0.5f, -h * 0.5f, w, h, 18f), paint);
        canvas.restore();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        targetScroll -= (float) verticalAmount * 48f;
        clampScroll();
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        event = MainUiScale.pageEvent(event, width, height, layoutScale(), layoutCenterX(), layoutCenterY());
        if (closingToMain) return true;
        for (ActionButton button : buttons) {
            if (button.contains((float) event.x(), (float) event.y())) {
                playClickSound();
                button.action.run();
                return true;
            }
        }
        int hit = worldAt((float) event.x(), (float) event.y());
        if (hit >= 0) {
            long now = System.currentTimeMillis();
            if (event.button() == 0 && lastWorldClick == hit && now - lastWorldClickMs <= 350L) {
                lastWorldClick = -1;
                lastWorldClickMs = 0L;
                selected = hit;
                openSelectedWorld();
                return true;
            }
            selected = hit;
            if (event.button() == 0) {
                lastWorldClick = hit;
                lastWorldClickMs = now;
            }
            return true;
        }
        return true;
    }

    private int worldAt(float mx, float my) {
        float x = cardX() + 14f;
        float y = cardY() + 16f;
        float w = cardW() - 28f;
        float h = cardH() - 32f;
        if (mx < x || mx > x + w || my < y || my > y + h) return -1;
        float itemH = 72f;
        float local = my - y + scroll;
        int index = (int) (local / (itemH + 8f));
        float inside = local - index * (itemH + 8f);
        return index >= 0 && index < worlds.size() && inside <= itemH ? index : -1;
    }

    private void openSelectedWorld() {
        if (minecraft == null || selected < 0 || selected >= worlds.size()) return;
        minecraft.createWorldOpenFlows().openWorld(worlds.get(selected).summary().getLevelId(), () -> minecraft.setScreen(this));
    }

    private void openEditScreen() {
        if (minecraft == null || selected < 0 || selected >= worlds.size()) return;
        try {
            LevelStorageSource.LevelStorageAccess access = minecraft.getLevelSource().createAccess(worlds.get(selected).summary().getLevelId());
            minecraft.setScreen(EditWorldScreen.create(minecraft, access, result -> {
                access.safeClose();
                loadWorlds();
                minecraft.setScreen(this);
            }));
        } catch (IOException ignored) {
        }
    }

    private void deleteSelectedWorld() {
        if (minecraft == null || selected < 0 || selected >= worlds.size()) return;
        WorldEntry world = worlds.get(selected);
        minecraft.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                try (LevelStorageSource.LevelStorageAccess access = minecraft.getLevelSource().createAccess(world.summary().getLevelId())) {
                    access.deleteLevel();
                } catch (IOException ignored) {
                }
                selected = -1;
                loadWorlds();
            }
            minecraft.setScreen(this);
        }, Component.literal("Delete World"), Component.literal(world.name())));
    }

    private void clampScroll() {
        float content = Math.max(0f, worlds.size() * 80f - 8f);
        float max = Math.max(0f, content - (cardH() - 32f));
        targetScroll = Math.max(0f, Math.min(max, targetScroll));
        scroll = Math.max(0f, Math.min(max, scroll));
    }

    private void startClose() {
        if (closingToMain) return;
        closingToMain = true;
        closeStartMs = System.currentTimeMillis();
    }

    private void playClickSound() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    @Override
    public void onClose() {
        startClose();
    }

    @Override
    public void removed() {
        pendingFrame = false;
        for (Image image : worldImages.values()) {
            image.close();
        }
        worldImages.clear();
        glBackend.destroy();
        super.removed();
    }

    private int mainFramebufferId() {
        Minecraft client = Minecraft.getInstance();
        if (client.getMainRenderTarget().getColorTexture() instanceof GlTexture texture
                && RenderSystem.getDevice() instanceof GlDevice device) {
            return texture.getFbo(device.directStateAccess(), client.getMainRenderTarget().getDepthTexture());
        }
        return 0;
    }

    private float openProgress() {
        return Math.max(0f, Math.min(1f, (System.currentTimeMillis() - openStartMs) / (float) OPEN_MS));
    }

    private float closeProgress() {
        if (!closingToMain || closeStartMs <= 0L) return 0f;
        return Math.max(0f, Math.min(1f, (System.currentTimeMillis() - closeStartMs) / (float) CLOSE_MS));
    }

    private float blurStrength() {
        float sizeFactor = Math.max(cardW(), cardH()) / 420f;
        return Math.max(0.72f, Math.min(1.35f, 0.72f + sizeFactor * 0.22f));
    }

    private Image worldImage(WorldEntry world) {
        Path iconPath = world.summary().getIcon();
        if (iconPath == null || !Files.isRegularFile(iconPath)) return null;
        if (worldImages.containsKey(iconPath)) return worldImages.get(iconPath);
        try {
            Image image = Image.makeFromEncoded(Files.readAllBytes(iconPath));
            worldImages.put(iconPath, image);
            return image;
        } catch (IOException | RuntimeException ignored) {
            worldImages.put(iconPath, null);
            return null;
        }
    }

    private float cardW() {
        return Math.max(320f, Math.min(500f, layoutWidth() * 0.52f));
    }

    private float cardH() {
        return Math.max(260f, Math.min(layoutHeight() - 154f, layoutHeight() * 0.72f));
    }

    private float cardX() {
        return (layoutWidth() - cardW()) * 0.5f;
    }

    private float cardY() {
        return 76f;
    }

    private int layoutWidth() {
        return MainUiScale.pageWidth();
    }

    private int layoutHeight() {
        return MainUiScale.pageHeight();
    }

    private float layoutScale() {
        float x = cardX();
        return MainUiScale.pageScale(
                x,
                18f,
                x + cardW(),
                cardY() + cardH() + 76f
        );
    }

    private float layoutCenterX() {
        return cardX() + cardW() * 0.5f;
    }

    private float layoutCenterY() {
        return (18f + cardY() + cardH() + 76f) * 0.5f;
    }

    private float ease(float v) {
        float t = 1f - Math.max(0f, Math.min(1f, v));
        return 1f - t * t * t;
    }

    private record WorldEntry(LevelSummary summary) {
        private String name() {
            return summary.getLevelName();
        }

        private String info() {
            return summary.getInfo().getString();
        }
    }

    private class ActionButton {
        private final String label;
        private final Runnable action;
        private float x;
        private float y;
        private float w;
        private float h;
        private float hover;

        private ActionButton(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }

        private void set(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        private boolean contains(float mx, float my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }

        private void draw(Canvas canvas, int mouseX, int mouseY, float alpha) {
            hover += ((contains(mouseX, mouseY) ? 1f : 0f) - hover) * 0.18f;
            float t = ease(hover);
            int a = Math.round(255f * Math.max(0f, Math.min(1f, alpha)));
            try (Paint bg = new Paint()) {
                bg.setAntiAlias(true);
                int color = lerpRgb(0x67B9EA, 0xA7E0FF, t);
                bg.setColor((Math.round(a * (0.20f + 0.12f * t)) << 24) | color);
                canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 10f), bg);
            }
            float size = 13f;
            FontRenderer.drawText(canvas, label, x + (w - FontRenderer.measureTextWidth(label, size)) * 0.5f, y + 20f, size, (a << 24) | 0xFFFFFF);
        }

        private int lerpRgb(int from, int to, float t) {
            int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
            int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
            int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
            return (r << 16) | (g << 8) | b;
        }
    }
}
