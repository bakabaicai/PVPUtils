package com.pvp_utils.client.render.MainUI;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaGlBackend;
import com.pvp_utils.client.render.skia.SkiaBlurRenderer;
import com.pvp_utils.client.via.ViaFabricPlusBridge;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class PVPUtilsViaFabricPlusScreen extends Screen {
    private static final long OPEN_MS = 440L;
    private final Screen parent;
    private final String shaderPath;
    private final Runnable embeddedBack;
    private final SkiaGlBackend glBackend = new SkiaGlBackend();
    private final List<ViaFabricPlusBridge.ProtocolGroup> groups = new ArrayList<>();
    private final List<Image> images = new ArrayList<>();
    private final List<Float> hoverAnimations = new ArrayList<>();
    private long openStartMs;
    private long closeStartMs;
    private boolean closing;
    private boolean closeDispatched;
    private float scroll;
    private float targetScroll;
    private float detailScroll;
    private float targetDetailScroll;
    private int selectedGroup = -1;
    private boolean pendingFrame;
    private boolean embedded;
    private int pendingMouseX;
    private int pendingMouseY;
    private int pressedGroup = -1;
    private long pressedGroupAt;
    private String cachedTargetVersion = "";
    private long nextTargetRefreshAt;

    public PVPUtilsViaFabricPlusScreen(Screen parent, String shaderPath) {
        this(parent, shaderPath, null);
    }

    public PVPUtilsViaFabricPlusScreen(Screen parent, String shaderPath, Runnable embeddedBack) {
        super(Component.literal("ViaFabricPlus"));
        this.parent = parent;
        this.shaderPath = shaderPath;
        this.embeddedBack = embeddedBack;
    }

    public void initEmbedded(Minecraft client, int width, int height) {
        embedded = true;
        init(width, height);
    }

    @Override
    protected void init() {
        openStartMs = System.currentTimeMillis();
        closeStartMs = 0L;
        closing = false;
        closeDispatched = false;
        groups.clear();
        groups.addAll(ViaFabricPlusBridge.protocolGroups());
        hoverAnimations.clear();
        for (int i = 0; i < groups.size(); i++) hoverAnimations.add(0f);
        images.clear();
        for (ViaFabricPlusBridge.ProtocolGroup group : groups) {
            images.add(loadImage(group.name()));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        scroll += (targetScroll - scroll) * 0.18f;
        detailScroll += (targetDetailScroll - detailScroll) * 0.18f;
        for (int i = 0; i < hoverAnimations.size(); i++) {
            boolean hovered = pendingMouseX >= 0 && pendingMouseY >= 0 && isGroupHovered(i);
            float current = hoverAnimations.get(i);
            float target = hovered ? 1f : 0f;
            hoverAnimations.set(i, current + (target - current) * 0.22f);
        }
        pendingMouseX = MainUiScale.pageX(mouseX, this.width);
        pendingMouseY = MainUiScale.pageY(mouseY, this.height);
        pendingFrame = true;
        if (closing && closeProgress() >= 1f && !closeDispatched) {
            closeDispatched = true;
            if (embeddedBack != null) {
                embeddedBack.run();
            } else if (minecraft != null) {
                minecraft.setScreen(parent);
            }
        }
    }

    @Override
    protected void repositionElements() {
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    }

    public void renderFrameEnd() {
        if (!pendingFrame || minecraft == null || (!embedded && minecraft.screen != this)) {
            pendingFrame = false;
            return;
        }
        Canvas canvas = glBackend.begin(mainFramebufferId());
        if (canvas == null) return;
        try {
            draw(canvas);
        } finally {
            glBackend.end();
            pendingFrame = false;
        }
    }

    private void draw(Canvas canvas) {
        float open = embedded ? 1f : ease(Math.min(1f, (System.currentTimeMillis() - openStartMs) / (float) OPEN_MS));
        float close = closing ? 1f - ease(closeProgress()) : 1f;
        float visibility = Math.min(open, close);
        float cardW = cardW();
        float cardH = cardH();
        float cardX = cardX();
        float cardY = cardY() + (1f - visibility) * 20f;
        int alpha = Math.round(255f * visibility);

        SkiaBlurRenderer.getInstance().render(canvas, glBackend.getContext(), Minecraft.getInstance(), mainFramebufferId(),
                cardX, cardY, cardW, cardH, 20f, 0x1A101010, 1.08f);
        try (Paint bg = new Paint(); Paint stroke = new Paint()) {
            bg.setAntiAlias(true);
            bg.setColor((Math.round(alpha * 0.20f) << 24) | 0x17191D);
            canvas.drawRRect(RRect.makeXYWH(cardX, cardY, cardW, cardH, 20f), bg);
            stroke.setAntiAlias(true);
            stroke.setMode(io.github.humbleui.skija.PaintMode.STROKE);
            stroke.setStrokeWidth(1f);
            stroke.setColor((Math.round(alpha * 0.12f) << 24) | 0xFFFFFF);
            canvas.drawRRect(RRect.makeXYWH(cardX + 0.5f, cardY + 0.5f, cardW - 1f, cardH - 1f, 20f), stroke);
        }

        String title = "ViaFabricPlus";
        FontRenderer.drawText(canvas, title, cardX + 28f, cardY + 34f, 22f, (alpha << 24) | 0xFFFFFF);
        String current = currentTargetVersion();
        FontRenderer.drawText(canvas, current.isBlank() ? "Protocol" : "Current: " + current,
                cardX + cardW - 190f, cardY + 34f, 11f, (Math.round(alpha * 0.72f) << 24) | 0xFFFFFF);

        int columns = 5;
        float gapX = 10f;
        float gapY = 12f;
        float calculatedItemW = (cardW - 44f - gapX * (columns - 1)) / columns;
        float itemW = Math.min(132f, calculatedItemW);
        float gridX = cardX + (cardW - (itemW * columns + gapX * (columns - 1))) * 0.5f;
        float detailY = cardY + 62f;
        float detailH = selectedGroup >= 0 ? 106f : 0f;
        float gridY = detailY + detailH + (selectedGroup >= 0 ? 10f : 0f);
        float itemH = 72f;
        float contentH = Math.max(0f, (float) Math.ceil(groups.size() / (float) columns) * (itemH + gapY) - gapY);
        float viewH = cardY + cardH - 56f - gridY;
        canvas.save();
        canvas.clipRect(Rect.makeXYWH(cardX + 14f, gridY, cardW - 28f, viewH));
        for (int i = 0; i < groups.size(); i++) {
            int column = i % columns;
            int row = i / columns;
            float x = gridX + column * (itemW + gapX);
            float y = gridY + row * (itemH + gapY) - scroll;
            if (y + itemH < gridY || y > gridY + viewH) continue;
            drawGroup(canvas, i, groups.get(i), images.get(i), x, y, itemW, itemH, alpha, current, i == selectedGroup,
                    pendingMouseX >= x && pendingMouseX <= x + itemW && pendingMouseY >= y && pendingMouseY <= y + itemH);
        }
        canvas.restore();

        if (selectedGroup >= 0 && selectedGroup < groups.size()) {
            drawProtocolDetails(canvas, groups.get(selectedGroup), current, cardX + 22f, detailY,
                    cardW - 44f, detailH, alpha);
        }

        float bottomY = cardY + cardH - 42f;
        float backW = bottomButtonW();
        float backX = cardX + 22f;
        float reportW = Math.min(88f, backW);
        float reportX = cardX + cardW - 22f - reportW;
        float serversX = reportX - 8f - backW;
        float settingsX = serversX - 8f - backW;
        drawBottomButton(canvas, backX, bottomY, backW, "Back", alpha, inside(pendingMouseX, pendingMouseY, backX, bottomY, backW, 26f));
        drawBottomButton(canvas, settingsX, bottomY, backW, "Settings", alpha, inside(pendingMouseX, pendingMouseY, settingsX, bottomY, backW, 26f));
        drawBottomButton(canvas, serversX, bottomY, backW, "Servers", alpha, inside(pendingMouseX, pendingMouseY, serversX, bottomY, backW, 26f));
        drawBottomButton(canvas, reportX, bottomY, reportW, "Report", alpha, inside(pendingMouseX, pendingMouseY, reportX, bottomY, reportW, 26f));
    }

    private void drawGroup(Canvas canvas, int index, ViaFabricPlusBridge.ProtocolGroup group, Image image, float x, float y, float w, float h,
                           int alpha, String current, boolean selected, boolean hovered) {
        float hover = index < hoverAnimations.size() ? hoverAnimations.get(index) : 0f;
        float pressed = index == pressedGroup
                ? Math.max(0f, 1f - (System.currentTimeMillis() - pressedGroupAt) / 180f)
                : 0f;
        float state = Math.max(hover, pressed);
        float shade = selected ? 0.34f : 0.14f + state * 0.14f;
        try (Paint bg = new Paint(); Paint imagePaint = new Paint()) {
            bg.setAntiAlias(true);
            bg.setColor(image == null
                    ? ((Math.round(alpha * shade) << 24) | lerpColor(0xFFFFFF, 0x73BDEB, state))
                    : ((alpha << 24) | 0x17191D));
            canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 16f), bg);
            if (image != null) {
                canvas.save();
                canvas.clipRRect(RRect.makeXYWH(x, y, w, h, 16f), true);
                imagePaint.setAntiAlias(true);
                drawAspectCover(canvas, image, imagePaint, x, y, w, h, alpha, state);
                canvas.restore();
            }
            if (state > 0.001f) {
                try (Paint overlay = new Paint()) {
                    overlay.setAntiAlias(true);
                    overlay.setColor((Math.round(alpha * state * 0.16f) << 24) | 0x73BDEB);
                    canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 16f), overlay);
                }
            }
        }
        boolean containsCurrent = group.entries().stream().anyMatch(entry -> entry.name().equals(current));
        int titleColor = containsCurrent ? 0x75F28A : 0xFFFFFF;
        String groupLabel = fit(group.name(), w - 18f, 18f);
        float tw = FontRenderer.measureTextWidth(groupLabel, 18f);
        FontRenderer.drawText(canvas, groupLabel, x + (w - tw) * 0.5f, y + 42f, 18f, (alpha << 24) | titleColor);
    }

    private void drawProtocolDetails(Canvas canvas, ViaFabricPlusBridge.ProtocolGroup group, String current, float x, float y, float w, float h, int alpha) {
        try (Paint panel = new Paint()) {
            panel.setAntiAlias(true);
            panel.setColor((Math.round(alpha * 0.16f) << 24) | 0xFFFFFF);
            canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 14f), panel);
        }
        String detailLabel = fit(group.name() + " versions", w - 28f, 11f);
        FontRenderer.drawText(canvas, detailLabel, x + 14f, y + 20f, 11f,
                (Math.round(alpha * 0.72f) << 24) | 0xFFFFFF);
        List<ViaFabricPlusBridge.ProtocolEntry> entries = group.entries();
        float buttonW = Math.max(72f, Math.min(118f, (w - 28f) / Math.min(7f, Math.max(1, entries.size())) - 6f));
        float buttonX = x + 14f;
        float buttonY = y + 52f - detailScroll;
        float contentBottom = y + h - 8f;
        canvas.save();
        canvas.clipRRect(RRect.makeXYWH(x + 1f, y + 34f, w - 2f, h - 35f, 10f), true);
        for (ViaFabricPlusBridge.ProtocolEntry entry : entries) {
            if (buttonX + buttonW > x + w - 10f) {
                buttonX = x + 14f;
                buttonY += 24f;
            }
            if (buttonY + 6f >= y + 34f && buttonY - 14f <= contentBottom) {
            boolean selected = entry.name().equals(current);
            boolean hovered = inside(pendingMouseX, pendingMouseY, buttonX, buttonY - 14f, buttonW, 20f);
            try (Paint bg = new Paint()) {
                bg.setAntiAlias(true);
                bg.setColor((Math.round(alpha * (selected ? 0.34f : hovered ? 0.25f : 0.12f)) << 24)
                        | (selected ? 0x73BDEB : 0xFFFFFF));
                canvas.drawRRect(RRect.makeXYWH(buttonX, buttonY - 14f, buttonW, 20f, 8f), bg);
            }
            String label = fit(entry.name(), buttonW - 8f, 10f);
            float tw = FontRenderer.measureTextWidth(label, 10f);
            FontRenderer.drawText(canvas, label, buttonX + (buttonW - tw) * 0.5f, buttonY, 10f,
                    (alpha << 24) | 0xFFFFFF);
            }
            buttonX += buttonW + 6f;
        }
        canvas.restore();
    }

    private String fit(String value, float maxWidth, float size) {
        if (FontRenderer.measureTextWidth(value, size) <= maxWidth) return value;
        String result = value;
        while (result.length() > 1 && FontRenderer.measureTextWidth(result + "...", size) > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "...";
    }

    private void drawBottomButton(Canvas canvas, float x, float y, float w, String text, int alpha, boolean hovered) {
        try (Paint bg = new Paint()) {
            bg.setAntiAlias(true);
            bg.setColor((Math.round(alpha * (hovered ? 0.28f : 0.15f)) << 24) | 0x73BDEB);
            canvas.drawRRect(RRect.makeXYWH(x, y, w, 26f, 10f), bg);
        }
        float tw = FontRenderer.measureTextWidth(text, 11f);
        FontRenderer.drawText(canvas, text, x + (w - tw) * 0.5f, y + 17f, 11f, (alpha << 24) | 0xFFFFFF);
    }

    private Image loadImage(String name) {
        try {
            InputStream stream = getClass().getResourceAsStream("/via/" + name + ".png");
            if (stream == null) return null;
            try (stream) {
                return Image.makeFromEncoded(stream.readAllBytes());
            }
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void drawAspectCover(Canvas canvas, Image image, Paint paint, float x, float y, float w, float h,
                                 int alpha, float state) {
        float sourceAspect = image.getWidth() / (float) image.getHeight();
        float destinationAspect = w / h;
        float srcW = image.getWidth();
        float srcH = image.getHeight();
        float srcX = 0f;
        float srcY = 0f;
        if (sourceAspect > destinationAspect) {
            srcW = image.getHeight() * destinationAspect;
            srcX = (image.getWidth() - srcW) * 0.5f;
        } else if (sourceAspect < destinationAspect) {
            srcH = image.getWidth() / destinationAspect;
            srcY = (image.getHeight() - srcH) * 0.5f;
        }
        Rect source = Rect.makeXYWH(srcX, srcY, srcW, srcH);
        Rect destination = Rect.makeXYWH(x, y, w, h);

        paint.setColor((alpha << 24) | 0xFFFFFF);
        canvas.drawImageRect(image, source, destination, SamplingMode.LINEAR, paint, true);
    }

    private int lerpColor(int from, int to, float amount) {
        float t = Math.max(0f, Math.min(1f, amount));
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (r << 16) | (g << 8) | b;
    }

    private boolean isGroupHovered(int index) {
        if (index < 0 || index >= groups.size()) return false;
        float cardW = cardW();
        float cardH = cardH();
        float cardX = cardX();
        float cardY = cardY();
        float detailY = cardY + 62f;
        float detailH = selectedGroup >= 0 ? 106f : 0f;
        float gridY = detailY + detailH + (selectedGroup >= 0 ? 10f : 0f);
        int columns = 5;
        float gapX = 10f;
        float itemW = Math.min(132f, (cardW - 44f - gapX * (columns - 1)) / columns);
        float gridX = cardX + (cardW - (itemW * columns + gapX * (columns - 1))) * 0.5f;
        float itemH = 72f;
        int column = index % columns;
        int row = index / columns;
        float x = gridX + column * (itemW + gapX);
        float y = gridY + row * (itemH + 12f) - scroll;
        float viewH = cardY + cardH - 56f - gridY;
        return y + itemH >= gridY && y <= gridY + viewH
                && inside(pendingMouseX, pendingMouseY, x, y, itemW, itemH);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float pageX = MainUiScale.pageX((int) mouseX, this.width);
        float pageY = MainUiScale.pageY((int) mouseY, this.height);
        float cardW = cardW();
        float cardH = cardH();
        float cardX = cardX();
        float cardY = cardY();
        float detailY = cardY + 62f;
        float detailH = selectedGroup >= 0 ? 106f : 0f;
        if (selectedGroup >= 0
                && inside(pageX, pageY, cardX + 23f, detailY + 35f, cardW - 46f, detailH - 36f)) {
            List<ViaFabricPlusBridge.ProtocolEntry> entries = groups.get(selectedGroup).entries();
            float buttonW = Math.max(72f, Math.min(118f, (cardW - 72f) / Math.min(7f, Math.max(1, entries.size())) - 6f));
            int columns = Math.max(1, (int) Math.floor((cardW - 44f - 28f) / (buttonW + 6f)));
            int rows = (int) Math.ceil(entries.size() / (double) columns);
            float max = Math.max(0f, rows * 24f - (detailH - 52f));
            targetDetailScroll = Math.max(0f, Math.min(max, targetDetailScroll - (float) verticalAmount * 24f));
            return true;
        }
        targetScroll -= (float) verticalAmount * 64f;
        float viewH = cardH - 118f - (selectedGroup >= 0 ? 106f : 0f);
        float rows = (float) Math.ceil(groups.size() / 5f);
        float max = Math.max(0f, rows * 84f - viewH);
        targetScroll = Math.max(0f, Math.min(max, targetScroll));
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (event.button() != 0) return true;
        float pageX = MainUiScale.pageX((int) event.x(), this.width);
        float pageY = MainUiScale.pageY((int) event.y(), this.height);
        float cardW = cardW();
        float cardH = cardH();
        float cardX = cardX();
        float cardY = cardY();
        float bottomY = cardY + cardH - 42f;
        float backW = bottomButtonW();
        float backX = cardX + 22f;
        float reportW = Math.min(88f, backW);
        float reportX = cardX + cardW - 22f - reportW;
        float serversX = reportX - 8f - backW;
        float settingsX = serversX - 8f - backW;
        if (inside(pageX, pageY, backX, bottomY, backW, 26f)) {
            playClick();
            onClose();
            return true;
        }
        if (inside(pageX, pageY, settingsX, bottomY, backW, 26f)) {
            playClick();
            ViaFabricPlusBridge.openSettings(this);
            return true;
        }
        if (inside(pageX, pageY, serversX, bottomY, backW, 26f)) {
            playClick();
            ViaFabricPlusBridge.openServerList(this);
            return true;
        }
        if (inside(pageX, pageY, reportX, bottomY, reportW, 26f)) {
            playClick();
            ViaFabricPlusBridge.openReportIssues(this);
            return true;
        }
        float detailY = cardY + 62f;
        float detailH = selectedGroup >= 0 ? 106f : 0f;
        float gridY = detailY + detailH + (selectedGroup >= 0 ? 10f : 0f);
        int columns = 5;
        float gapX = 10f;
        float gapY = 12f;
        float calculatedItemW = (cardW - 44f - gapX * (columns - 1)) / columns;
        float itemW = Math.min(132f, calculatedItemW);
        float gridX = cardX + (cardW - (itemW * columns + gapX * (columns - 1))) * 0.5f;
        float itemH = 72f;
        if (selectedGroup >= 0 && selectedGroup < groups.size()) {
            float detailX = cardX + 22f;
            float detailW = cardW - 44f;
            List<ViaFabricPlusBridge.ProtocolEntry> entries = groups.get(selectedGroup).entries();
            float buttonW = Math.max(72f, Math.min(118f, (detailW - 28f) / Math.min(7f, Math.max(1, entries.size())) - 6f));
            float buttonX = detailX + 14f;
            float buttonY = detailY + 52f - detailScroll;
            for (ViaFabricPlusBridge.ProtocolEntry entry : entries) {
                if (buttonX + buttonW > detailX + detailW - 10f) {
                    buttonX = detailX + 14f;
                    buttonY += 24f;
                }
                if (buttonY + 6f >= detailY + 34f
                        && buttonY - 14f <= detailY + detailH - 8f
                        && inside(pageX, pageY, buttonX, buttonY - 14f, buttonW, 20f)) {
                    ViaFabricPlusBridge.setTargetVersion(entry);
                    cachedTargetVersion = entry.name();
                    nextTargetRefreshAt = System.currentTimeMillis() + 250L;
                    playClick();
                    return true;
                }
                buttonX += buttonW + 6f;
            }
            if (inside(pageX, pageY, detailX, detailY, detailW, detailH)) {
                return true;
            }
        }
        for (int i = 0; i < groups.size(); i++) {
            int column = i % columns;
            int row = i / columns;
            float x = gridX + column * (itemW + gapX);
            float y = gridY + row * (itemH + gapY) - scroll;
            if (inside(pageX, pageY, x, y, itemW, itemH)) {
                selectedGroup = i;
                pressedGroup = i;
                pressedGroupAt = System.currentTimeMillis();
                detailScroll = 0f;
                targetDetailScroll = 0f;
                playClick();
                return true;
            }
        }
        return true;
    }

    private void playClick() {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
    }

    @Override
    public void onClose() {
        if (closing) return;
        closing = true;
        closeStartMs = System.currentTimeMillis();
    }

    @Override
    public void removed() {
        pendingFrame = false;
        for (Image image : images) if (image != null) image.close();
        images.clear();
        glBackend.destroy();
        super.removed();
    }

    private float closeProgress() {
        if (!closing || closeStartMs <= 0L) return 0f;
        return Math.max(0f, Math.min(1f, (System.currentTimeMillis() - closeStartMs) / (float) OPEN_MS));
    }

    private String currentTargetVersion() {
        long now = System.currentTimeMillis();
        if (now >= nextTargetRefreshAt) {
            cachedTargetVersion = ViaFabricPlusBridge.targetVersionName();
            nextTargetRefreshAt = now + 250L;
        }
        return cachedTargetVersion;
    }

    private int mainFramebufferId() {
        if (minecraft.getMainRenderTarget().getColorTexture() instanceof GlTexture texture
                && RenderSystem.getDevice() instanceof GlDevice device) {
            return texture.getFbo(device.directStateAccess(), minecraft.getMainRenderTarget().getDepthTexture());
        }
        return 0;
    }

    private float cardW() {
        return Math.max(620f, Math.min(900f, width * 0.84f));
    }

    private float cardH() {
        return Math.max(370f, Math.min(height - 100f, height * 0.78f));
    }

    private float cardX() {
        return (width - cardW()) * 0.5f;
    }

    private float cardY() {
        return 76f;
    }

    private float bottomButtonW() {
        return Math.max(56f, Math.min(92f, (cardW() - 44f - 24f) / 4f));
    }

    private boolean inside(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private float ease(float value) {
        float t = 1f - Math.max(0f, Math.min(1f, value));
        return 1f - t * t * t;
    }
}
