package com.pvp_utils.client.modules.impl.Render;

import io.github.humbleui.types.RRect;

import com.pvp_utils.client.render.skia.SkijaUi;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.theme.ThemeColors;
import io.github.humbleui.skija.Path;
import io.github.humbleui.skija.PathBuilder;
import io.github.humbleui.skija.PaintStrokeCap;
import io.github.humbleui.skija.PaintStrokeJoin;
import io.github.humbleui.types.Rect;
import com.pvp_utils.client.modules.impl.Optimize.BetterScoreboard.BetterScoreboardManager;
import com.pvp_utils.client.modules.impl.Render.DynamicIsland.DynamicIslandRenderer;
import com.pvp_utils.client.modules.impl.Render.ClockHudRenderer;
import com.pvp_utils.client.modules.impl.Render.PingHudRenderer;
import com.pvp_utils.client.modules.impl.Render.TpsHudRenderer;
import com.pvp_utils.client.modules.impl.Tool.BlockCountDisplayRenderer;
import com.pvp_utils.client.render.skia.SkijaRenderer;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.PathEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import com.pvp_utils.client.plugin.PluginManager;

public class HudEditOverlay {
    private enum DragTarget { NONE, TARGET_HUD, KEYSTROKES, BLOCK_COUNT, ARMOR_HUD, ITEM_USE_STATUS, DYNAMIC_ISLAND, ARRAYLIST, NOTIFICATION, POTION_STATUS, LYRICS_DISPLAY, MUSIC_INFO_HUD, BETTER_SCOREBOARD, PING_HUD, TPS_HUD, CLOCK_HUD }

    private static final HudEditOverlay INSTANCE = new HudEditOverlay();
    private static final int TARGET_HUD_WIDTH = 164;
    private static final int TARGET_HUD_HEIGHT = 44;
    private static final int TARGET_HUD_NEW_WIDTH = 190;
    private static final int TARGET_HUD_NEW_HEIGHT = 58;
    private static final float DASH_SPEED = 20f;
    private static final float DASH_LEN = 12f;
    private static final float DASH_GAP = 6f;
    private static final float DASH_PERIOD = DASH_LEN + DASH_GAP;
    private static final float ANIM_DURATION = 0.2f;
    private static final float SNAP_THRESHOLD = 10f;
    private static final float WEAK_SNAP_THRESHOLD = 4f;
    private static final float ELEMENT_SNAP_PROXIMITY = 48f;
    private static final float HINT_TEXT_SIZE = 11f;

    private final Map<Object, Float> hoverAlpha = new HashMap<>();
    private Object dragTarget = DragTarget.NONE;
    private boolean wasMouseDown = false;
    private float dragOffsetX;
    private float dragOffsetY;
    private boolean resizing;
    private float resizeMouseX;
    private float resizeMouseY;
    private float resizeStartScale;
    private RectState resizeStartRect;
    private float dashOffset = 0f;
    private long lastFrameMs = 0;
    private long animStartTime = 0;
    private long animCloseTime = 0;
    private boolean closing = false;
    private boolean active = false;
    private float snapXLine = -1f;
    private float snapYLine = -1f;
    private float snapXAlpha = 0f;
    private float snapYAlpha = 0f;
    private boolean configDirty = false;

    private boolean pendingFrame = false;
    private int pendingGuiW = 0;
    private int pendingGuiH = 0;
    private float pendingProgress = 0f;
    private List<EditItemState> pendingItems = List.of();

    public static HudEditOverlay getInstance() {
        return INSTANCE;
    }

    public boolean isActive() {
        return active;
    }

    public void startOpen() {
        animStartTime = System.currentTimeMillis();
        closing = false;
        active = true;
        NotificationOverlay.getInstance().startEditPreview();
    }

    public void startClose() {
        if (configDirty) {
            Config.save();
            PluginManager.INSTANCE.saveHudLayouts();
            configDirty = false;
        }
        dragTarget = DragTarget.NONE;
        resizing = false;
        snapXLine = -1f;
        snapYLine = -1f;
        animCloseTime = System.currentTimeMillis();
        closing = true;
        active = true;
        NotificationOverlay.getInstance().stopEditPreview();
    }

    public void render(GuiGraphics graphics) {
        render(graphics, null);
    }

    public void render(GuiGraphics graphics, Canvas canvas) {
        if (!active) return;

        Minecraft mc = Minecraft.getInstance();
        int guiW = mc.getWindow().getGuiScaledWidth();
        int guiH = mc.getWindow().getGuiScaledHeight();
        long windowHandle = mc.getWindow().handle();

        long now = System.currentTimeMillis();
        float dt = lastFrameMs == 0 ? 0.016f : Math.min((now - lastFrameMs) / 1000f, 0.05f);
        lastFrameMs = now;

        dashOffset -= DASH_SPEED * dt;
        if (dashOffset < -DASH_PERIOD) dashOffset += DASH_PERIOD;

        float guiScale = (float) mc.getWindow().getGuiScale();
        double[] rawX = new double[1];
        double[] rawY = new double[1];
        GLFW.glfwGetCursorPos(windowHandle, rawX, rawY);
        float mx = (float) (rawX[0] / guiScale);
        float my = (float) (rawY[0] / guiScale);
        boolean mouseDown = GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean weakSnap = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
        || GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

        List<EditItemState> items = buildEditItems(guiW, guiH);
        updateDrag(items, mx, my, mouseDown, weakSnap, guiW, guiH);
        updateHover(items, mx, my, dt);

        snapXAlpha += (((snapXLine >= 0f && dragTarget != DragTarget.NONE) ? 1f : 0f) - snapXAlpha) * Math.min(1f, dt * 18f);
        snapYAlpha += (((snapYLine >= 0f && dragTarget != DragTarget.NONE) ? 1f : 0f) - snapYAlpha) * Math.min(1f, dt * 18f);

        float progress;
        if (closing) {
            progress = 1f - Math.min(1f, (now - animCloseTime) / (ANIM_DURATION * 1000f));
            if (progress <= 0f) {
                active = false;
                return;
            }
        } else {
            progress = Math.min(1f, (now - animStartTime) / (ANIM_DURATION * 1000f));
        }

        pendingGuiW = guiW;
        pendingGuiH = guiH;
        pendingProgress = progress;
        pendingItems = copyItems(items);
        pendingFrame = true;

        if (canvas != null) {
            drawOverlay(canvas, guiW, guiH, progress, pendingItems);
        }
    }

    public void renderSkija(Canvas canvas) {
        if (!pendingFrame) return;
        Minecraft client = Minecraft.getInstance();
        if (!active || client.options.hideGui) {
            pendingFrame = false;
            return;
        }

        drawOverlay(canvas, pendingGuiW, pendingGuiH, pendingProgress, pendingItems);

        pendingFrame = false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return false;
    }

    private List<EditItemState> buildEditItems(int guiW, int guiH) {
        ArrayList<EditItemState> items = new ArrayList<>();
        addItem(items, DragTarget.POTION_STATUS, "Potion Status", Config.potionStatus, getPotionStatusRect(guiW, guiH), this::movePotionStatus, delta -> setScale(v -> Config.potionStatusScale = v, Config.potionStatusScale, delta));
        addItem(items, DragTarget.LYRICS_DISPLAY, "Lyrics Display", Config.lyricsDisplay, getLyricsDisplayRect(guiW, guiH), this::moveLyricsDisplay, delta -> setScale(v -> Config.lyricsDisplayScale = v, Config.lyricsDisplayScale, delta));
        addItem(items, DragTarget.MUSIC_INFO_HUD, "Music Info HUD", Config.musicInfoHud, getMusicInfoHudRect(guiW, guiH), this::moveMusicInfoHud, delta -> setScale(v -> Config.musicInfoHudScale = v, Config.musicInfoHudScale, delta));
        addItem(items, DragTarget.NOTIFICATION, "Notification", true, getNotificationRect(guiW, guiH), this::moveNotification, delta -> setScale(v -> Config.notificationScale = v, Config.notificationScale, delta));
        addItem(items, DragTarget.BETTER_SCOREBOARD, "Better Scoreboard", Config.betterScoreboard, getBetterScoreboardRect(guiW, guiH), this::moveBetterScoreboard, delta -> setScale(v -> Config.betterScoreboardScale = v, Config.betterScoreboardScale, delta));
        addItem(items, DragTarget.BLOCK_COUNT, "Block Count", Config.blockCountDisplay, getBlockCountRect(guiW, guiH), this::moveBlockCount, delta -> setScale(v -> Config.blockCountDisplayScale = v, Config.blockCountDisplayScale, delta));
        addItem(items, DragTarget.ARMOR_HUD, "Armor HUD", Config.armorHud, getArmorHudRect(guiW, guiH), this::moveArmorHud, delta -> setScale(v -> Config.armorHudScale = v, Config.armorHudScale, delta), ArmorHudRenderer.getInstance().isPositionLockedToAdaptiveLayout());
        addItem(items, DragTarget.ITEM_USE_STATUS, "Item Use Status", Config.itemUseStatus, getItemUseStatusRect(guiW, guiH), this::moveItemUseStatus, delta -> setScale(v -> Config.itemUseStatusScale = v, Config.itemUseStatusScale, delta));
        addItem(items, DragTarget.DYNAMIC_ISLAND, "Dynamic Island", Config.dynamicIsland, getDynamicIslandRect(guiW, guiH), this::moveDynamicIsland, delta -> setScale(v -> Config.dynamicIslandScale = v, Config.dynamicIslandScale, delta));
        addItem(items, DragTarget.ARRAYLIST, "Arraylist", Config.arraylist, getArraylistRect(guiW, guiH), this::moveArraylist, delta -> setScale(v -> Config.arraylistScale = v, Config.arraylistScale, delta));
        addItem(items, DragTarget.KEYSTROKES, "Keystrokes", Config.keystrokes, getKeystrokesRect(guiW, guiH), this::moveKeystrokes, delta -> setScale(v -> Config.keystrokesScale = v, Config.keystrokesScale, delta));
        addItem(items, DragTarget.PING_HUD, "Ping HUD", Config.pingHud, getRendererRect(PingHudRenderer.getInstance(), guiW, guiH), this::movePingHud, delta -> setScale(v -> Config.pingHudScale = v, Config.pingHudScale, delta));
        addItem(items, DragTarget.TPS_HUD, "TPS HUD", Config.tpsHud, getRendererRect(TpsHudRenderer.getInstance(), guiW, guiH), this::moveTpsHud, delta -> setScale(v -> Config.tpsHudScale = v, Config.tpsHudScale, delta));
        addItem(items, DragTarget.CLOCK_HUD, "Clock", Config.clockHud, getRendererRect(ClockHudRenderer.getInstance(), guiW, guiH), this::moveClockHud, delta -> setScale(v -> Config.clockHudScale = v, Config.clockHudScale, delta));
        addItem(items, DragTarget.TARGET_HUD, "Target HUD", Config.targetHud, getTargetHudRect(guiW, guiH), this::moveTargetHud, delta -> setScale(v -> Config.targetHudScale = v, Config.targetHudScale, delta));
        for (PluginManager.HudEditItem plugin : PluginManager.INSTANCE.hudEditItems(guiW, guiH)) {
            var layout = plugin.layout();
            RectState rect = clampRect(layout.x(), layout.y(), layout.width() * layout.scale(),
                    layout.height() * layout.scale(), guiW, guiH);
            items.add(new EditItemState(new EditItem(plugin.key(), plugin.label(),
                    (moved, w, h) -> plugin.move().accept(moved.x, moved.y),
                    delta -> {
                        layout.setScale(layout.scale() + delta);
                        plugin.move().accept(layout.x(), layout.y());
                        return true;
                    }, false, layout::scale), rect));
        }
        hoverAlpha.keySet().retainAll(items.stream().map(item -> item.definition().target()).toList());
        return items;
    }

    private void addItem(List<EditItemState> items, DragTarget target, String label, boolean enabled, RectState rect, MoveHandler move, ScaleHandler scale) {
        addItem(items, target, label, enabled, rect, move, scale, false);
    }

    private void addItem(List<EditItemState> items, DragTarget target, String label, boolean enabled, RectState rect, MoveHandler move, ScaleHandler scale, boolean locked) {
        if (enabled && rect != null) {
            items.add(new EditItemState(new EditItem(target, label, move, scale, locked, () -> scaleFor(target)), rect));
        }
    }

    private void updateDrag(List<EditItemState> items, float mx, float my, boolean mouseDown, boolean weakSnap, int guiW, int guiH) {
        if (mouseDown && !wasMouseDown) {
            dragTarget = DragTarget.NONE;
            resizing = false;
            for (int i = items.size() - 1; i >= 0; i--) {
                EditItemState item = items.get(i);
                boolean grip = containsGrip(item.rect(), mx, my);
                if (!grip && (item.definition().locked() || !contains(item.rect(), mx, my, 4f))) continue;
                dragTarget = item.definition().target();
                resizing = grip;
                dragOffsetX = mx - item.rect().x;
                dragOffsetY = my - item.rect().y;
                resizeMouseX = mx;
                resizeMouseY = my;
                resizeStartRect = item.rect();
                resizeStartScale = item.definition().scaleValue().get();
                break;
            }
        }
        if (!mouseDown) {
            if (configDirty) {
                Config.save();
                PluginManager.INSTANCE.saveHudLayouts();
                configDirty = false;
            }
            dragTarget = DragTarget.NONE;
            resizing = false;
            snapXLine = -1f;
            snapYLine = -1f;
        }
        wasMouseDown = mouseDown;
        EditItemState draggedItem = itemFor(items, dragTarget);
        if (draggedItem == null) return;
        if (resizing && resizeStartRect != null) {
            RectState start = resizeStartRect;
            float denominator = Math.max(1f, start.w * start.w + start.h * start.h);
            float ratio = 1f + ((mx - resizeMouseX) * start.w + (my - resizeMouseY) * start.h) / denominator;
            float desired = clampScale(resizeStartScale * ratio);
            float current = draggedItem.definition().scaleValue().get();
            if (Math.abs(desired - current) > 0.0001f) {
                draggedItem.definition().scale().scale(desired - current);
                configDirty = true;
            }
            float actual = draggedItem.definition().scaleValue().get() / Math.max(0.001f, resizeStartScale);
            RectState resized = clampRect(start.x, start.y, start.w * actual, start.h * actual, guiW, guiH);
            if (!draggedItem.definition().locked()) draggedItem.definition().move().move(resized, guiW, guiH);
            draggedItem.setRect(resized);
            updateSnapGuides(resized, guiW, guiH, weakSnap, dragTarget, items);
            return;
        }
        RectState base = draggedItem.rect();
        RectState dragged = snapRect(clampRect(mx - dragOffsetX, my - dragOffsetY, base.w, base.h, guiW, guiH),
                guiW, guiH, weakSnap, draggedItem.definition().target(), items);
        draggedItem.setRect(dragged);
        draggedItem.definition().move().move(dragged, guiW, guiH);
        configDirty = true;
    }

    private boolean containsGrip(RectState rect, float mouseX, float mouseY) {
        float right = rect.x + rect.w + 4f;
        float bottom = rect.y + rect.h + 4f;
        return mouseX >= right - 27f && mouseX <= right + 5f
                && mouseY >= bottom - 27f && mouseY <= bottom + 5f
                && (mouseX >= right - 10f || mouseY >= bottom - 10f);
    }

    private float scaleFor(DragTarget target) {
        return switch (target) {
            case TARGET_HUD -> Config.targetHudScale;
            case KEYSTROKES -> Config.keystrokesScale;
            case BLOCK_COUNT -> Config.blockCountDisplayScale;
            case ARMOR_HUD -> Config.armorHudScale;
            case ITEM_USE_STATUS -> Config.itemUseStatusScale;
            case DYNAMIC_ISLAND -> Config.dynamicIslandScale;
            case ARRAYLIST -> Config.arraylistScale;
            case NOTIFICATION -> Config.notificationScale;
            case POTION_STATUS -> Config.potionStatusScale;
            case LYRICS_DISPLAY -> Config.lyricsDisplayScale;
            case MUSIC_INFO_HUD -> Config.musicInfoHudScale;
            case BETTER_SCOREBOARD -> Config.betterScoreboardScale;
            case PING_HUD -> Config.pingHudScale;
            case TPS_HUD -> Config.tpsHudScale;
            case CLOCK_HUD -> Config.clockHudScale;
            default -> 1f;
        };
    }

    private void updateHover(List<EditItemState> items, float mx, float my, float dt) {
        for (EditItemState item : items) {
            boolean hovered = containsGrip(item.rect(), mx, my) || contains(item.rect(), mx, my, 4f) || dragTarget == item.definition().target();
            Object key = item.definition().target();
            float alpha = hoverAlpha.getOrDefault(key, 0f);
            hoverAlpha.put(key, alpha + ((hovered ? 1f : 0f) - alpha) * Math.min(1f, dt * 14f));
        }
    }

    private EditItemState itemFor(List<EditItemState> items, Object target) {
        if (target == DragTarget.NONE) return null;
        for (EditItemState item : items) {
            if (Objects.equals(item.definition().target(), target)) return item;
        }
        return null;
    }

    private List<EditItemState> copyItems(List<EditItemState> items) {
        ArrayList<EditItemState> copy = new ArrayList<>(items.size());
        for (EditItemState item : items) {
            copy.add(new EditItemState(item.definition(), item.rect()));
        }
        return copy;
    }

    private boolean setScale(FloatSetter setter, float current, float delta) {
        setter.set(clampScale(current + delta));
        return true;
    }

    private void moveTargetHud(RectState rect, int guiW, int guiH) {
        Config.targetHudX = rect.x - guiW * 0.5f;
        Config.targetHudY = rect.y - guiH * 0.5f;
    }

    private void moveKeystrokes(RectState rect, int guiW, int guiH) {
        Config.keystrokesX = rect.x - guiW * 0.5f;
        Config.keystrokesY = rect.y - guiH * 0.5f;
    }

    private void movePingHud(RectState rect, int guiW, int guiH) {
        moveTextHud(PingHudRenderer.getInstance(), rect, guiW, guiH);
    }

    private void moveTpsHud(RectState rect, int guiW, int guiH) {
        moveTextHud(TpsHudRenderer.getInstance(), rect, guiW, guiH);
    }

    private void moveClockHud(RectState rect, int guiW, int guiH) {
        moveTextHud(ClockHudRenderer.getInstance(), rect, guiW, guiH);
    }

    private void moveTextHud(SkiaTextHudRenderer renderer, RectState rect, int guiW, int guiH) {
        renderer.setConfigX(rect.x - guiW * 0.5f);
        renderer.setConfigY(rect.y - guiH * 0.5f);
    }

    private RectState getRendererRect(SkiaTextHudRenderer renderer, int guiW, int guiH) {
        return clampRect(renderer.getRenderX(guiW), renderer.getRenderY(guiH), renderer.getEditWidth(), renderer.getEditHeight(), guiW, guiH);
    }

    private void moveBlockCount(RectState rect, int guiW, int guiH) {
        BlockCountDisplayRenderer renderer = BlockCountDisplayRenderer.getInstance();
        Config.blockCountDisplayX = rect.x - (guiW - renderer.getEditWidth()) * 0.5f;
        Config.blockCountDisplayY = rect.y - renderer.getDefaultY(guiH);
    }

    private void moveArmorHud(RectState rect, int guiW, int guiH) {
        ArmorHudRenderer renderer = ArmorHudRenderer.getInstance();
        Config.armorHudX = rect.x - (guiW - renderer.getEditWidth()) * 0.5f;
        Config.armorHudY = rect.y - (guiH - renderer.getEditHeight()) + 28f;
    }

    private void moveItemUseStatus(RectState rect, int guiW, int guiH) {
        ItemUseStatusRenderer renderer = ItemUseStatusRenderer.getInstance();
        Config.itemUseStatusX = rect.x - (guiW - renderer.getEditWidth()) * 0.5f;
        Config.itemUseStatusY = rect.y - renderer.getDefaultY(guiH);
    }

    private void moveDynamicIsland(RectState rect, int guiW, int guiH) {
        DynamicIslandRenderer renderer = DynamicIslandRenderer.getInstance();
        Config.dynamicIslandX = rect.x - (guiW - renderer.getEditWidth()) * 0.5f;
        Config.dynamicIslandY = rect.y - renderer.getDefaultY();
    }

    private void moveArraylist(RectState rect, int guiW, int guiH) {
        ArraylistRenderer renderer = ArraylistRenderer.getInstance();
        float outset = renderer.getScaledBorderOutset();
        Config.arraylistX = rect.x + outset - renderer.getDefaultX(guiW);
        Config.arraylistY = rect.y + outset - renderer.getDefaultY();
    }

    private void moveNotification(RectState rect, int guiW, int guiH) {
        Config.notificationX = rect.x + rect.w - guiW * 0.5f;
        Config.notificationY = rect.y - guiH * 0.5f;
    }

    private void movePotionStatus(RectState rect, int guiW, int guiH) {
        PotionStatusRenderer renderer = PotionStatusRenderer.getInstance();
        Config.potionStatusX = rect.x - renderer.getDefaultX();
        Config.potionStatusY = rect.y - (guiH - rect.h) * 0.5f;
    }

    private void moveLyricsDisplay(RectState rect, int guiW, int guiH) {
        LyricsDisplayRenderer renderer = LyricsDisplayRenderer.getInstance();
        Config.lyricsDisplayX = rect.x - renderer.getDefaultX(guiW);
        Config.lyricsDisplayY = rect.y - renderer.getDefaultY(guiH);
    }

    private void moveMusicInfoHud(RectState rect, int guiW, int guiH) {
        MusicInfoHudRenderer renderer = MusicInfoHudRenderer.getInstance();
        Config.musicInfoHudX = rect.x - renderer.getDefaultX(guiW);
        Config.musicInfoHudY = rect.y - renderer.getDefaultY(guiH);
    }

    private void moveBetterScoreboard(RectState rect, int guiW, int guiH) {
        BetterScoreboardManager.Rect baseRect = BetterScoreboardManager.getCurrentRect(guiW, guiH);
        float scale = BetterScoreboardManager.getScale();
        float pad = Config.betterScoreboardVisualImprovement ? 7.0f * scale : 0.0f;
        float visualYOffset = Config.betterScoreboardVisualImprovement ? 3.0f * scale : 0.0f;
        Config.betterScoreboardX = rect.x + pad - baseRect.x();
        Config.betterScoreboardY = rect.y + pad - visualYOffset - baseRect.y();
    }

    private void drawGrid(Canvas canvas, int guiW, int guiH, float alpha) {
        int color = ThemeColors.accent();
        if (snapXLine >= 0f) SkijaUi.line(canvas, snapXLine, 0f, snapXLine, guiH, 1.2f,
                ThemeColors.withAlpha(color, alpha * snapXAlpha * 0.8f));
        if (snapYLine >= 0f) SkijaUi.line(canvas, 0f, snapYLine, guiW, snapYLine, 1.2f,
                ThemeColors.withAlpha(color, alpha * snapYAlpha * 0.8f));
    }

    private void drawOverlay(Canvas canvas, int guiW, int guiH, float progress, List<EditItemState> items) {
        if (dragTarget != DragTarget.NONE) drawGrid(canvas, guiW, guiH, progress);
        for (EditItemState item : items) {
            RectState rect = item.rect();
            float hover = hoverAlpha.getOrDefault(item.definition().target(), 0f);
            if (item.definition().target() == DragTarget.LYRICS_DISPLAY) {
                drawLyricsPreview(canvas, rect, progress);
            }
            drawOutline(canvas, rect.x, rect.y, rect.w, rect.h, item.definition().label(), hover, progress,
                    Objects.equals(item.definition().target(), dragTarget));
        }
        drawHint(canvas, guiW, guiH, progress);
    }

    private void drawLyricsPreview(Canvas canvas, RectState rect, float progress) {
        LyricsDisplayRenderer.getInstance().drawPreview(canvas, rect.x, rect.y,
                Math.max(0.5f, Config.lyricsDisplayScale), progress);
    }

    private void drawOutline(Canvas canvas, float x, float y, float width, float height, String label,
                             float hover, float opacity, boolean dragging) {
        float pad = 4f;
        float left = x - pad;
        float top = y - pad;
        float right = x + width + pad;
        float bottom = y + height + pad;
        int color = ThemeColors.withAlpha(ThemeColors.accent(x, y), (0.65f + hover * 0.35f) * opacity);
        if (dragging) {
            try (PathEffect dash = PathEffect.makeDash(new float[]{DASH_LEN, DASH_GAP}, dashOffset);
                 Paint paint = new Paint().setAntiAlias(true).setMode(PaintMode.STROKE).setStrokeWidth(1.2f).setColor(color)) {
                paint.setPathEffect(dash);
                canvas.drawRRect(RRect.makeXYWH(left, top, width + pad * 2f, height + pad * 2f, 9f), paint);
            }
        }
        float gripLength = Math.clamp(Math.min(width, height) * 0.6f, 10f, 25f);
        float radius = Math.min(14f, gripLength * 0.75f);
        try (PathBuilder builder = new PathBuilder()) {
            builder.moveTo(right - gripLength, bottom);
            builder.lineTo(right - radius, bottom);
            builder.arcTo(Rect.makeXYWH(right - radius * 2f, bottom - radius * 2f,
                    radius * 2f, radius * 2f), 90f, -90f, false);
            builder.lineTo(right, bottom - gripLength);
            try (Path path = builder.detach()) {
                SkijaUi.strokePath(canvas, path, 2.5f, color, PaintStrokeCap.ROUND, PaintStrokeJoin.ROUND);
            }
        }
        SkijaUi.text(canvas, label, left, Math.max(1f, top - 18f), 16f, color, 10f);
    }

    private void drawHint(Canvas canvas, int guiW, int guiH, float progress) {
        if (dragTarget == DragTarget.NONE || (snapXLine < 0f && snapYLine < 0f)) return;
        String text = Config.isChinese ? "弱对齐" : "Weak alignment";
        float width = SkijaUi.textWidth(text, HINT_TEXT_SIZE);
        float opacity = progress * Math.max(snapXAlpha, snapYAlpha) * 0.7f;
        SkijaUi.text(canvas, text, (guiW - width) * 0.5f, Math.max(48f, guiH - 92f), 18f,
                ThemeColors.withAlpha(ThemeColors.accent(), opacity), HINT_TEXT_SIZE);
    }

    private RectState getTargetHudRect(int guiW, int guiH) {
        float scale = Math.max(0.5f, Config.targetHudScale);
        boolean newSize = Config.targetHudMode != Config.TargetHudMode.LITE;
        float baseW = newSize ? TARGET_HUD_NEW_WIDTH : TARGET_HUD_WIDTH;
        float baseH = newSize ? TARGET_HUD_NEW_HEIGHT : TARGET_HUD_HEIGHT;
        return clampRect(guiW * 0.5f + Config.targetHudX, guiH * 0.5f + Config.targetHudY, baseW * scale, baseH * scale, guiW, guiH);
    }

    private RectState getKeystrokesRect(int guiW, int guiH) {
        KeystrokesRenderer renderer = KeystrokesRenderer.getInstance();
        return new RectState(renderer.getRenderX(guiW), renderer.getRenderY(guiH), renderer.getScaledWidth(), renderer.getScaledHeight());
    }

    private RectState getNotificationRect(int guiW, int guiH) {
        NotificationOverlay renderer = NotificationOverlay.getInstance();
        int w = renderer.getEditWidth();
        int h = renderer.getEditHeight();
        return clampRect(renderer.getRenderX(guiW, w), renderer.getRenderY(guiH), w, h, guiW, guiH);
    }

    private RectState getBlockCountRect(int guiW, int guiH) {
        BlockCountDisplayRenderer renderer = BlockCountDisplayRenderer.getInstance();
        return clampRect(renderer.getRenderX(guiW), renderer.getRenderY(guiH), renderer.getEditWidth(), renderer.getEditHeight(), guiW, guiH);
    }

    private RectState getArmorHudRect(int guiW, int guiH) {
        ArmorHudRenderer renderer = ArmorHudRenderer.getInstance();
        return clampRect(renderer.getRenderX(guiW), renderer.getRenderY(guiH), renderer.getEditWidth(), renderer.getEditHeight(), guiW, guiH);
    }

    private RectState getItemUseStatusRect(int guiW, int guiH) {
        ItemUseStatusRenderer renderer = ItemUseStatusRenderer.getInstance();
        return clampRect(renderer.getRenderX(guiW), renderer.getRenderY(guiH), renderer.getEditWidth(), renderer.getEditHeight(), guiW, guiH);
    }

    private RectState getDynamicIslandRect(int guiW, int guiH) {
        DynamicIslandRenderer renderer = DynamicIslandRenderer.getInstance();
        return clampRect(renderer.getRenderX(guiW), renderer.getRenderY(guiH), renderer.getEditWidth(), renderer.getEditHeight(), guiW, guiH);
    }

    private RectState getArraylistRect(int guiW, int guiH) {
        ArraylistRenderer renderer = ArraylistRenderer.getInstance();
        return clampRect(renderer.getEditX(guiW), renderer.getEditY(guiH), renderer.getEditWidth(), renderer.getEditHeight(), guiW, guiH);
    }

    private RectState getPotionStatusRect(int guiW, int guiH) {
        PotionStatusRenderer renderer = PotionStatusRenderer.getInstance();
        return clampRect(renderer.getRenderX(guiW), renderer.getRenderY(guiH), renderer.getEditWidth(), renderer.getEditHeight(), guiW, guiH);
    }

    private RectState getLyricsDisplayRect(int guiW, int guiH) {
        LyricsDisplayRenderer renderer = LyricsDisplayRenderer.getInstance();
        return clampRect(renderer.getRenderX(guiW), renderer.getRenderY(guiH), renderer.getEditWidth(), renderer.getEditHeight(), guiW, guiH);
    }

    private RectState getMusicInfoHudRect(int guiW, int guiH) {
        MusicInfoHudRenderer renderer = MusicInfoHudRenderer.getInstance();
        return clampRect(renderer.getRenderX(guiW), renderer.getRenderY(guiH), renderer.getEditWidth(), renderer.getEditHeight(), guiW, guiH);
    }

    private RectState getBetterScoreboardRect(int guiW, int guiH) {
        BetterScoreboardManager.Rect rect = BetterScoreboardManager.getCurrentRect(guiW, guiH);
        float scale = BetterScoreboardManager.getScale();
        float pad = Config.betterScoreboardVisualImprovement ? 7.0f * scale : 0.0f;
        float visualYOffset = Config.betterScoreboardVisualImprovement ? 3.0f * scale : 0.0f;
        return clampRect(rect.x() + Config.betterScoreboardX - pad, rect.y() + Config.betterScoreboardY + visualYOffset - pad,
        rect.w() * scale + pad * 2.0f, rect.h() * scale + pad * 2.0f, guiW, guiH);
    }

    private RectState clampRect(float x, float y, float w, float h, int guiW, int guiH) {
        float clampedX = Math.max(0, Math.min(x, guiW - w));
        float clampedY = Math.max(0, Math.min(y, guiH - h));
        return new RectState(clampedX, clampedY, w, h);
    }

    private RectState snapRect(RectState rect, int guiW, int guiH, boolean weakSnap, Object draggedTarget, List<EditItemState> refs) {
        snapXLine = -1f;
        snapYLine = -1f;
        float[] xLines = buildSnapLines(guiW * 0.25f, guiW * 0.5f, guiW * 0.75f, rect, refs, draggedTarget, true);
        float[] yLines = buildSnapLines(guiH * 0.25f, guiH * 0.5f, guiH * 0.75f, rect, refs, draggedTarget, false);
        float threshold = weakSnap ? WEAK_SNAP_THRESHOLD : SNAP_THRESHOLD;
        SnapResult sx = snapAxis(rect.x, rect.w, xLines, threshold);
        SnapResult sy = snapAxis(rect.y, rect.h, yLines, threshold);
        snapXLine = sx.line;
        snapYLine = sy.line;
        return clampRect(sx.position, sy.position, rect.w, rect.h, guiW, guiH);
    }

    private void updateSnapGuides(RectState rect, int guiW, int guiH, boolean weakSnap,
                                  Object target, List<EditItemState> refs) {
        float threshold = weakSnap ? WEAK_SNAP_THRESHOLD : SNAP_THRESHOLD;
        snapXLine = snapAxis(rect.x, rect.w,
                buildSnapLines(guiW * 0.25f, guiW * 0.5f, guiW * 0.75f, rect, refs, target, true), threshold).line;
        snapYLine = snapAxis(rect.y, rect.h,
                buildSnapLines(guiH * 0.25f, guiH * 0.5f, guiH * 0.75f, rect, refs, target, false), threshold).line;
    }

    private float[] buildSnapLines(float a, float b, float c, RectState dragged, List<EditItemState> refs, Object draggedTarget, boolean horizontal) {
        float[] lines = new float[3 + refs.size() * 3];
        int count = 0;
        lines[count++] = a;
        lines[count++] = b;
        lines[count++] = c;
        for (EditItemState item : refs) {
            RectState ref = item.rect();
            if (Objects.equals(item.definition().target(), draggedTarget) || !isNearForSnap(dragged, ref, horizontal)) continue;
            float start = horizontal ? ref.x : ref.y;
            float size = horizontal ? ref.w : ref.h;
            lines[count++] = start;
            lines[count++] = start + size * 0.5f;
            lines[count++] = start + size;
        }
        float[] compact = new float[count];
        System.arraycopy(lines, 0, compact, 0, count);
        return compact;
    }

    private boolean isNearForSnap(RectState dragged, RectState ref, boolean horizontal) {
        if (horizontal) {
            return rangeDistance(dragged.y, dragged.y + dragged.h, ref.y, ref.y + ref.h) <= ELEMENT_SNAP_PROXIMITY;
        }
        return rangeDistance(dragged.x, dragged.x + dragged.w, ref.x, ref.x + ref.w) <= ELEMENT_SNAP_PROXIMITY;
    }

    private float rangeDistance(float a0, float a1, float b0, float b1) {
        if (a1 < b0) return b0 - a1;
        if (b1 < a0) return a0 - b1;
        return 0f;
    }

    private SnapResult snapAxis(float position, float size, float[] lines, float threshold) {
        float snappedPosition = position;
        float snappedLine = -1f;
        float bestDistance = threshold + 1f;
        float[] offsets = {0f, size * 0.5f, size};
        for (float line : lines) {
            for (float offset : offsets) {
                float distance = Math.abs(position + offset - line);
                if (distance <= threshold && distance < bestDistance) {
                    bestDistance = distance;
                    snappedPosition = line - offset;
                    snappedLine = line;
                }
            }
        }
        return new SnapResult(snappedPosition, snappedLine);
    }

    private boolean contains(RectState rect, float mx, float my, float pad) {
        return mx >= rect.x - pad && mx <= rect.x + rect.w + pad && my >= rect.y - pad && my <= rect.y + rect.h + pad;
    }

    private float clampScale(float scale) {
        return Math.max(0.5f, Math.min(2.0f, scale));
    }

    private interface MoveHandler {
        void move(RectState rect, int guiW, int guiH);
    }

    private interface ScaleHandler {
        boolean scale(float delta);
    }

    private interface FloatSetter {
        void set(float value);
    }

    private interface FloatGetter {
        float get();
    }

    private record EditItem(Object target, String label, MoveHandler move, ScaleHandler scale, boolean locked, FloatGetter scaleValue) {}

    private static final class EditItemState {
        private final EditItem definition;
        private RectState rect;

        EditItemState(EditItem definition, RectState rect) {
            this.definition = definition;
            this.rect = rect;
        }

        EditItem definition() {
            return definition;
        }

        RectState rect() {
            return rect;
        }

        void setRect(RectState rect) {
            this.rect = rect;
        }
    }

    private static final class RectState {
        final float x;
        final float y;
        final float w;
        final float h;

        RectState(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    private record SnapResult(float position, float line) {}
}
