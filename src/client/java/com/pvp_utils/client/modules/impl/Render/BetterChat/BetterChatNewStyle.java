package com.pvp_utils.client.modules.impl.Render.BetterChat;

import com.pvp_utils.Config;
import com.pvp_utils.mixin.client.ChatComponentAccessor;
import com.pvp_utils.client.render.chroma.ChromaGraphics;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Path;
import io.github.humbleui.skija.PathBuilder;
import org.joml.Vector2f;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.navigation.ScreenRectangle;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Set;

public final class BetterChatNewStyle {
    private static final Map<GuiMessage.Line, Motion> motions = new IdentityHashMap<>();
    private static long frameTime;
    private static boolean animating;
    private static float entranceDistance;
    private static final PanelSize panelWidth = new PanelSize();
    private static final PanelSize panelHeight = new PanelSize();
    private static boolean scissorActive;
    private static int clipLeft;
    private static int clipTop;
    private static int clipRight;
    private static int clipBottom;

    private BetterChatNewStyle() {}

    public static boolean enabled() {
        return Config.betterChat && Config.betterChatStyle != Config.BetterChatStyle.CLASSIC;
    }

    public static void reset() {
        motions.clear();
        animating = false;
        scissorActive = false;
    }

    public static ActiveTextCollector.Parameters clipText(ActiveTextCollector.Parameters parameters) {
        if (!scissorActive) return parameters;
        int left = clipLeft;
        int top = clipTop;
        int right = clipRight;
        int bottom = clipBottom;
        ScreenRectangle current = parameters.scissor();
        if (current != null) {
            left = Math.max(left, current.left());
            top = Math.max(top, current.top());
            right = Math.min(right, current.right());
            bottom = Math.min(bottom, current.bottom());
        }
        return parameters.withScissor(new ScreenRectangle(left, top, Math.max(0, right - left), Math.max(0, bottom - top)));
    }

    public static void track(List<GuiMessage.Line> lines) {
        retain(lines);
        for (int i = 0; i < lines.size(); i++) {
            Motion motion = motions.computeIfAbsent(lines.get(i), key -> new Motion());
            motion.index = i;
        }
    }

    public static void added(List<GuiMessage.Line> lines, boolean scrolled) {
        if (!enabled() || !Config.betterChatMessageAnimation || scrolled) {
            reset();
            return;
        }
        long now = System.nanoTime();
        int entry = -1;
        for (int i = 0; i < lines.size(); i++) {
            GuiMessage.Line line = lines.get(i);
            if (i == 0 || line.endOfEntry()) entry++;
            Motion motion = motions.get(line);
            if (motion == null) {
                motion = new Motion();
                motion.entered = now;
                motions.put(line, motion);
            } else if (motion.index != i) {
                motion.from = motion.offset(now) + i - motion.index;
                motion.started = now;
                motion.delay = Math.min(entry, 8) * Math.min(18f, Config.betterChatNewAnimationTime * 0.03f);
            }
            motion.index = i;
        }
        retain(lines);
    }

    private static void retain(List<GuiMessage.Line> lines) {
        Set<GuiMessage.Line> retained = Collections.newSetFromMap(new IdentityHashMap<>());
        retained.addAll(lines);
        motions.keySet().retainAll(retained);
    }

    public static void beginFrame(boolean scrolled) {
        frameTime = System.nanoTime();
        animating = enabled() && Config.betterChatMessageAnimation && !scrolled;
        if (!animating) motions.clear();
    }

    public static float offsetY(GuiMessage.Line line, int lineHeight) {
        Motion motion = motions.get(line);
        return animating && motion != null ? motion.offset(frameTime) * lineHeight : 0f;
    }

    public static int renderLineLimit(int pageSize, List<GuiMessage.Line> lines, int scroll, int lineHeight) {
        int limit = pageSize;
        for (int i = pageSize; i < lines.size() - scroll; i++) {
            float position = i - offsetY(lines.get(i + scroll), lineHeight) / Math.max(1, lineHeight);
            if (position <= pageSize + 1f) limit = i + 1;
        }
        return limit;
    }

    public static float offsetX(GuiMessage.Line line) {
        Motion motion = motions.get(line);
        if (!animating || motion == null || motion.entered == 0) return 0f;
        return -entranceDistance * (1f - ease((frameTime - motion.entered) / (Math.max(1, Config.betterChatNewAnimationTime) * 1_000_000f)));
    }

    public static boolean drawPanel(GuiGraphics graphics, ChatComponent chat, List<GuiMessage.Line> lines,
                                 int scroll, int tick, boolean focused, int lineHeight) {
        ChatComponentAccessor access = (ChatComponentAccessor) chat;
        if (!enabled() || access.pvp_utils$isChatHidden() || lines.isEmpty()) {
            panelWidth.reset();
            panelHeight.reset();
            scissorActive = false;
            return false;
        }
        int count = Math.min(chat.getLinesPerPage(), lines.size() - scroll);
        if (count <= 0) {
            panelWidth.reset();
            panelHeight.reset();
            scissorActive = false;
            return false;
        }
        int alpha = Math.round(255f * Minecraft.getInstance().options.textBackgroundOpacity().get().floatValue());
        float scale = (float) access.pvp_utils$getChatScale();
        entranceDistance = graphics.guiWidth() / scale + 8f;
        int bottom = (int) Math.floor((graphics.guiHeight() - 40) / scale);
        int top = bottom - count * lineHeight - 6;
        int right = (int) Math.ceil(access.pvp_utils$getChatWidth() / scale) + 8;
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(4f, 0f);
        Vector2f start = graphics.pose().transformPosition(-4f, top, new Vector2f());
        Vector2f end = graphics.pose().transformPosition(right, bottom + 6f, new Vector2f());
        float width = panelWidth.update(end.x - start.x, frameTime);
        float height = panelHeight.update(end.y - start.y, frameTime);
        float panelTop = end.y - height;
        float radius = Math.min(14f * scale, Math.min(width, height) * 0.5f);
        float opacity = 1f;
        boolean blur = Config.betterChatStyle == Config.BetterChatStyle.BLUR;
        ChromaGraphics.draw("better-chat-panel", canvas -> {
            try (Path bounds = panelPath(start.x, panelTop, width, height, radius)) {
                if (blur) {
                    try (Paint paint = new Paint().setAlphaf(opacity)) {
                        int save = canvas.saveLayer(null, paint);
                        try {
                            ChromaGraphics.drawBlurredBackdrop(canvas, bounds,
                                    start.x, panelTop, width, height, 10.5f);
                        } finally {
                            canvas.restoreToCount(save);
                        }
                    }
                }
                if (alpha > 0) {
                    try (Paint paint = new Paint().setAntiAlias(true).setColor((alpha << 24) | 0x10120E)) {
                        canvas.drawPath(bounds, paint);
                    }
                }
            }
        });
        graphics.pose().popMatrix();
        clipLeft = Math.max(0, Math.round(start.x));
        clipTop = Math.max(0, Math.round(panelTop));
        clipRight = Math.min(graphics.guiWidth(), Math.round(start.x + width));
        clipBottom = Math.min(graphics.guiHeight(), Math.round(end.y));
        scissorActive = clipRight > clipLeft && clipBottom > clipTop;
        if (scissorActive) {
            graphics.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
        }
        return scissorActive;
    }

    private static Path panelPath(float x, float y, float width, float height, float radius) {
        float right = x + width;
        float bottom = y + height;
        float handle = radius * 0.55228475f;
        try (PathBuilder builder = new PathBuilder()) {
            builder.moveTo(x, y).lineTo(right - radius, y)
                    .cubicTo(right - radius + handle, y, right, y + radius - handle, right, y + radius)
                    .lineTo(right, bottom - radius)
                    .cubicTo(right, bottom - radius + handle, right - radius + handle, bottom, right - radius, bottom)
                    .lineTo(x, bottom).closePath();
            return builder.detach();
        }
    }

    private static final class PanelSize {
        private float from;
        private float target;
        private long started;
        private boolean initialized;

        private void reset() {
            initialized = false;
        }

        private float update(float next, long now) {
            if (!initialized) {
                initialized = true;
                from = next;
                target = next;
                started = now;
            }
            float value = from + (target - from) * ease((now - started) / 300_000_000f);
            if (next != target) {
                from = value;
                target = next;
                started = now;
            }
            return value;
        }
    }

    private static float ease(float progress) {
        if (progress <= 0f) return 0f;
        if (progress >= 1f) return 1f;
        float inv = 1f - progress;
        return 1f - inv * inv * inv;
    }

    private static final class Motion {
        private int index;
        private float from;
        private float delay;
        private long started;
        private long entered;

        private float offset(long now) {
            float elapsed = (now - started) / 1_000_000f - delay;
            return from * (1f - ease(elapsed / Math.max(1, Config.betterChatNewAnimationTime)));
        }
    }
}
