package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Optimize.BetterScoreboard.BetterScoreboardManager;
import com.pvp_utils.client.modules.impl.Render.DynamicIsland.DynamicIslandRenderer;
import com.pvp_utils.client.modules.impl.Tool.BlockCountDisplayRenderer;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaGlBackend;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.PathEffect;
import io.github.humbleui.skija.impl.Library;
import io.github.humbleui.types.RRect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public class HudEditOverlay {

    private enum DragTarget { NONE, TARGET_HUD, KEYSTROKES, BLOCK_COUNT, ARMOR_HUD, ITEM_USE_STATUS, DYNAMIC_ISLAND, ARRAYLIST, NOTIFICATION, POTION_STATUS, LYRICS_DISPLAY, MUSIC_INFO_HUD, BETTER_SCOREBOARD }

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

    private final float[] hoverAlpha = new float[DragTarget.values().length];
    private final SkiaGlBackend glBackend = new SkiaGlBackend();
    private DragTarget dragTarget = DragTarget.NONE;
    private boolean wasMouseDown = false;
    private float dragOffsetX;
    private float dragOffsetY;
    private float dashOffset = 0f;
    private long lastFrameMs = 0;
    private long animStartTime = 0;
    private long animCloseTime = 0;
    private boolean closing = false;
    private boolean active = false;
    private float snapXLine = -1f;
    private float snapYLine = -1f;
    private float gridAlpha = 0f;
    private float snapXAlpha = 0f;
    private float snapYAlpha = 0f;
    private boolean configDirty = false;
    private boolean nativeLoaded = false;
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

        gridAlpha += (((dragTarget != DragTarget.NONE) ? 1f : 0f) - gridAlpha) * Math.min(1f, dt * 12f);
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

    public void renderFrameEnd() {
        if (!pendingFrame) return;
        Minecraft client = Minecraft.getInstance();
        if (!active || client.options.hideGui) {
            pendingFrame = false;
            return;
        }

        ensureNativeLoaded();
        Canvas canvas = glBackend.begin();
        if (canvas == null) return;
        try {
            drawOverlay(canvas, pendingGuiW, pendingGuiH, pendingProgress, pendingItems);
        } finally {
            glBackend.end();
            pendingFrame = false;
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!active || Math.abs(amount) <= 0.001) return false;
        Minecraft mc = Minecraft.getInstance();
        int guiW = mc.getWindow().getGuiScaledWidth();
        int guiH = mc.getWindow().getGuiScaledHeight();
        float mx = (float) mouseX;
        float my = (float) mouseY;
        float delta = amount > 0 ? 0.05f : -0.05f;

        for (EditItemState item : buildEditItems(guiW, guiH)) {
            if (contains(item.rect(), mx, my, 4f) && item.definition().scale().scale(delta)) {
                Config.save();
                return true;
            }
        }
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
        addItem(items, DragTarget.TARGET_HUD, "Target HUD", Config.targetHud, getTargetHudRect(guiW, guiH), this::moveTargetHud, delta -> setScale(v -> Config.targetHudScale = v, Config.targetHudScale, delta));
        return items;
    }

    private void addItem(List<EditItemState> items, DragTarget target, String label, boolean enabled, RectState rect, MoveHandler move, ScaleHandler scale) {
        addItem(items, target, label, enabled, rect, move, scale, false);
    }

    private void addItem(List<EditItemState> items, DragTarget target, String label, boolean enabled, RectState rect, MoveHandler move, ScaleHandler scale, boolean locked) {
        if (enabled && rect != null) {
            items.add(new EditItemState(new EditItem(target, label, move, scale, locked), rect));
        }
    }

    private void updateDrag(List<EditItemState> items, float mx, float my, boolean mouseDown, boolean weakSnap, int guiW, int guiH) {
        if (mouseDown && !wasMouseDown) {
            dragTarget = DragTarget.NONE;
            for (EditItemState item : items) {
                if (item.definition().locked()) continue;
                if (contains(item.rect(), mx, my, 4f)) {
                    dragTarget = item.definition().target();
                    dragOffsetX = mx - item.rect().x;
                    dragOffsetY = my - item.rect().y;
                    break;
                }
            }
        }
        if (!mouseDown) {
            if (configDirty) {
                Config.save();
                configDirty = false;
            }
            dragTarget = DragTarget.NONE;
        }
        wasMouseDown = mouseDown;

        EditItemState draggedItem = itemFor(items, dragTarget);
        if (draggedItem == null) return;
        RectState base = draggedItem.rect();
        RectState dragged = snapRect(clampRect(mx - dragOffsetX, my - dragOffsetY, base.w, base.h, guiW, guiH), guiW, guiH, weakSnap, draggedItem.definition().target(), items);
        draggedItem.setRect(dragged);
        draggedItem.definition().move().move(dragged, guiW, guiH);
        configDirty = true;
    }

    private void updateHover(List<EditItemState> items, float mx, float my, float dt) {
        for (EditItemState item : items) {
            boolean hovered = contains(item.rect(), mx, my, 4f) || dragTarget == item.definition().target();
            int index = item.definition().target().ordinal();
            hoverAlpha[index] += ((hovered ? 1f : 0f) - hoverAlpha[index]) * Math.min(1f, dt * 14f);
        }
    }

    private EditItemState itemFor(List<EditItemState> items, DragTarget target) {
        if (target == DragTarget.NONE) return null;
        for (EditItemState item : items) {
            if (item.definition().target() == target) return item;
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
        float[] xs = {guiW * 0.25f, guiW * 0.5f, guiW * 0.75f};
        float[] ys = {guiH * 0.25f, guiH * 0.5f, guiH * 0.75f};

        try (Paint p = new Paint()) {
            int a = (int) (0x44 * alpha * gridAlpha);
            p.setColor((a << 24) | 0xFFFFFF);
            p.setAntiAlias(true);
            p.setPathEffect(PathEffect.makeDash(new float[]{10f, 10f}, 0f));
            p.setStrokeWidth(1f);
            p.setMode(PaintMode.STROKE);
            for (float x : xs) canvas.drawLine(x, 0, x, guiH, p);
            for (float y : ys) canvas.drawLine(0, y, guiW, y, p);
        }

        try (Paint p = new Paint()) {
            p.setColor((int) (alpha * snapXAlpha * 255) << 24 | 0x7AA2FF);
            p.setAntiAlias(true);
            p.setStrokeWidth(1.5f);
            p.setMode(PaintMode.STROKE);
            if (snapXLine >= 0f) canvas.drawLine(snapXLine, 0, snapXLine, guiH, p);
        }

        try (Paint p = new Paint()) {
            p.setColor((int) (alpha * snapYAlpha * 255) << 24 | 0x7AA2FF);
            p.setAntiAlias(true);
            p.setStrokeWidth(1.5f);
            p.setMode(PaintMode.STROKE);
            if (snapYLine >= 0f) canvas.drawLine(0, snapYLine, guiW, snapYLine, p);
        }
    }

    private void drawOverlay(Canvas canvas, int guiW, int guiH, float progress, List<EditItemState> items) {
        if (gridAlpha > 0.01f) {
            drawGrid(canvas, guiW, guiH, progress);
        }
        for (EditItemState item : items) {
            RectState rect = item.rect();
            float alpha = hoverAlpha[item.definition().target().ordinal()];
            if (item.definition().target() == DragTarget.LYRICS_DISPLAY) {
                drawLyricsPreview(canvas, rect, progress);
            }
            drawOutline(canvas, rect.x, rect.y, rect.w, rect.h, item.definition().label(), alpha, progress);
        }
        drawHint(canvas, guiW, guiH, progress);
    }

    private void drawLyricsPreview(Canvas canvas, RectState rect, float progress) {
        float scale = Math.max(0.5f, Config.lyricsDisplayScale);
        float centerX = rect.x + rect.w * 0.5f;
        float centerY = rect.y + rect.h * 0.5f + 7f * scale;
        String[] lines = Config.isChinese
                ? new String[]{"光从耳边慢慢经过", "这一句会停在屏幕中央", "下一句跟着节拍上移"}
                : new String[]{"Light drifts slowly past my ears", "This line stays in the center", "The next line rises with the beat"};
        float[] offsets = {-27f, 0f, 27f};
        float[] sizes = {15f, 21f, 15f};
        int[] colors = {0xC8CDD8, 0xFFFFFF, 0xC8CDD8};
        int[] alphas = {120, 245, 120};
        for (int i = 0; i < lines.length; i++) {
            float size = sizes[i] * scale;
            String text = trimToWidth(lines[i], Math.max(24f, rect.w - 32f * scale), size);
            float textW = FontRenderer.measureTextWidth(text, size);
            int alpha = Math.round(alphas[i] * progress);
            float x = centerX - textW * 0.5f;
            float y = centerY + offsets[i] * scale;
            FontRenderer.drawText(canvas, text, x + 1.2f * scale, y + 1.2f * scale, size, alpha << 24);
            FontRenderer.drawText(canvas, text, x, y, size, (alpha << 24) | colors[i]);
        }
    }

    private String trimToWidth(String text, float maxWidth, float size) {
        if (FontRenderer.measureTextWidth(text, size) <= maxWidth) return text;
        String ellipsis = "...";
        while (text.length() > 1 && FontRenderer.measureTextWidth(text + ellipsis, size) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + ellipsis;
    }

    private void drawOutline(Canvas canvas, float x, float y, float w, float h, String label, float hoverAlpha, float alpha) {
        float pad = 4f;
        float rw = w + pad * 2f;
        float rh = h + pad * 2f;
        float a = (0.55f + hoverAlpha * 0.45f) * alpha;
        int alphaInt = (int) (a * 255);
        float drawX = Math.round(x) + 0.5f;
        float drawY = Math.round(y) + 0.5f;

        if (alpha > 0.01f) {
            try (Paint fill = new Paint()) {
                fill.setColor((int) (alpha * 30) << 24 | 0xFFFFFF);
                fill.setAntiAlias(true);
                canvas.drawRRect(RRect.makeXYWH(drawX - pad, drawY - pad, rw, rh, 6f), fill);
            }
        }

        try (Paint p = new Paint()) {
            p.setColor((alphaInt << 24) | 0xFFFFFF);
            p.setAntiAlias(true);
            p.setPathEffect(PathEffect.makeDash(new float[]{DASH_LEN, DASH_GAP}, dashOffset));
            p.setStrokeWidth(1.5f);
            p.setMode(PaintMode.STROKE);
            canvas.drawRRect(RRect.makeXYWH(drawX - pad, drawY - pad, rw, rh, 6f), p);
        }

        FontRenderer.drawText(canvas, label, drawX - pad, drawY - pad - 4f, 10f, (alphaInt << 24) | 0xFFFFFF);
    }

    private void drawHint(Canvas canvas, int guiW, int guiH, float progress) {
        String text = Config.isChinese ? "悬浮滚轮调节大小，按住 Shift 弱对齐" : "Hover and scroll to resize, hold Shift for weak snap";
        float textW = FontRenderer.measureTextWidth(text, HINT_TEXT_SIZE);
        float x = (guiW - textW) * 0.5f;
        float y = Math.max(48f, guiH - 92f);
        int alpha = Math.round(210f * Math.max(0f, Math.min(1f, progress)));
        FontRenderer.drawText(canvas, text, x, y, HINT_TEXT_SIZE, (alpha << 24) | 0xFFFFFF);
    }

    private RectState getTargetHudRect(int guiW, int guiH) {
        float scale = Math.max(0.5f, Config.targetHudScale);
        boolean newSize = Config.targetHudMode == Config.TargetHudMode.NEW || Config.targetHudMode == Config.TargetHudMode.BLUR;
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

    private RectState snapRect(RectState rect, int guiW, int guiH, boolean weakSnap, DragTarget draggedTarget, List<EditItemState> refs) {
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

    private float[] buildSnapLines(float a, float b, float c, RectState dragged, List<EditItemState> refs, DragTarget draggedTarget, boolean horizontal) {
        float[] lines = new float[3 + refs.size() * 3];
        int count = 0;
        lines[count++] = a;
        lines[count++] = b;
        lines[count++] = c;
        for (EditItemState item : refs) {
            RectState ref = item.rect();
            if (item.definition().target() == draggedTarget || !isNearForSnap(dragged, ref, horizontal)) continue;
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

    private void ensureNativeLoaded() {
        if (nativeLoaded) return;
        Library.load();
        nativeLoaded = true;
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

    private record EditItem(DragTarget target, String label, MoveHandler move, ScaleHandler scale, boolean locked) {}

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
