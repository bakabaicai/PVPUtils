package com.pvp_utils.client.render.MainUI;

import io.github.humbleui.types.RRect;

import com.pvp_utils.client.render.skia.SkijaUi;

import com.pvp_utils.client.render.skia.SkijaRenderer;
import com.pvp_utils.client.via.ViaFabricPlusBridge;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Matrix33;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.PaintStrokeCap;
import io.github.humbleui.skija.PaintStrokeJoin;
import io.github.humbleui.skija.Path;
import io.github.humbleui.skija.PathBuilder;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import net.minecraft.network.chat.Component;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Predicate;

public final class PVPUtilsViaFabricPlusScreen extends Screen implements com.pvp_utils.client.render.skia.SkijaScreen {
    private static final long OPEN_MS = 440L;
    private static final int PAGE_SIZE = 16;
    private final Screen parent;
    private final String shaderPath;
    private final Runnable embeddedBack;
    private final List<ViaFabricPlusBridge.ProtocolEntry> protocols = new ArrayList<>();
    private final Map<String, Image> images = new HashMap<>();
    private final List<Float> hoverAnimations = new ArrayList<>();
    private long openStartMs;
    private long closeStartMs;
    private boolean closing;
    private boolean closeDispatched;
    private int page;
    private int previousPage = -1;
    private int pageDirection = 1;
    private long pageStartNanos;
    private boolean pendingFrame;
    private boolean embedded;
    private int pendingMouseX;
    private int pendingMouseY;
    private int pressedProtocol = -1;
    private long pressedProtocolAt;
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
        closeImages();
        protocols.clear();
        protocols.addAll(ViaFabricPlusBridge.protocols());
        hoverAnimations.clear();
        for (int i = 0; i < protocols.size(); i++) hoverAnimations.add(0f);
        cachedTargetVersion = ViaFabricPlusBridge.targetVersionName();
        nextTargetRefreshAt = System.currentTimeMillis() + 250L;
        page = 0;
        for (int i = 0; i < protocols.size(); i++) {
            if (protocols.get(i).name().equals(cachedTargetVersion)) {
                page = i / PAGE_SIZE;
                break;
            }
        }
        previousPage = -1;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (minecraft != null && minecraft.screen == this) {
            MainUISharedBackground.setActiveShader(shaderPath);
            MainUISharedBackground.render(graphics, mouseX, mouseY);
        }
        float scale = layoutScale();
        pendingMouseX = MainUiScale.pageX(mouseX, width, scale, layoutCenterX());
        pendingMouseY = MainUiScale.pageY(mouseY, height, scale, layoutCenterY());
        if (previousPage >= 0 && pageElapsedMs() >= 250f) previousPage = -1;
        for (int i = 0; i < hoverAnimations.size(); i++) {
            boolean hovered = isProtocolHovered(i);
            float current = hoverAnimations.get(i);
            float target = hovered ? 1f : 0f;
            hoverAnimations.set(i, current + (target - current) * 0.22f);
        }
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
        previousPage = -1;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void renderSkija(Canvas canvas) {
        if (!pendingFrame || minecraft == null || (!embedded && minecraft.screen != this)) {
            pendingFrame = false;
            return;
        }
        try {
            drawPage(canvas, false);
        } finally {
            pendingFrame = false;
        }
    }

    public void renderTransition(Matrix33 cardTransform, RRect cardBounds, float chromeAlpha) {
        if (!pendingFrame || minecraft == null || (!embedded && minecraft.screen != this)) {
            pendingFrame = false;
            return;
        }
        try {
            SkijaRenderer.draw(canvas -> MainUIFlipAnimation.drawPage(canvas, cardTransform, cardBounds,
                    chromeAlpha, target -> drawPage(target, true)));
        } finally {
            pendingFrame = false;
        }
    }

    public void finishTransition() {
        openStartMs = System.currentTimeMillis() - OPEN_MS;
    }

    public RRect transitionBounds() {
        float scale = layoutScale();
        return RRect.makeXYWH(
                MainUiScale.pageScreenX(cardX(), width, scale, layoutCenterX()),
                MainUiScale.pageScreenY(cardY(), height, scale, layoutCenterY()),
                cardW() * scale, cardH() * scale, 20f * scale);
    }

    private float visibility(boolean transitioning) {
        float open = embedded ? 1f : ease(Math.min(1f, (System.currentTimeMillis() - openStartMs) / (float) OPEN_MS));
        float close = closing ? 1f - ease(closeProgress()) : 1f;
        return transitioning ? 1f : Math.min(open, close);
    }

    private void drawPage(Canvas canvas, boolean transitioning) {
        float visibility = visibility(transitioning);
        float scale = layoutScale();
        float x = MainUiScale.pageScreenX(cardX(), width, scale, layoutCenterX());
        float y = MainUiScale.pageScreenY(cardY() + (1f - visibility) * 20f, height, scale, layoutCenterY());
        float w = cardW() * scale;
        float h = cardH() * scale;
        SkijaRenderer.drawBlurredBackdrop(canvas, RRect.makeXYWH(x, y, w, h, 20f * scale),
                x, y, w, h, 1.08f * 10.5f);
        int save = canvas.save();
        try {
            MainUiScale.applyPage(canvas, width, height, scale, layoutCenterX(), layoutCenterY());
            draw(canvas, visibility);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void draw(Canvas canvas, float visibility) {
        float cardW = cardW();
        float cardH = cardH();
        float cardX = cardX();
        float cardY = cardY() + (1f - visibility) * 20f;
        int alpha = Math.round(255f * visibility);

        SkijaUi.rounded(canvas, cardX, cardY, cardW, cardH, 20f, 0x1A101010);;
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
        SkijaUi.text(canvas, title, cardX + 28f, (cardY + 34f) + SkijaUi.textMetrics(22f).getAscent(), SkijaUi.textMetrics(22f).getDescent() - SkijaUi.textMetrics(22f).getAscent(), (alpha << 24) | 0xFFFFFF, 22f);
        String current = currentTargetVersion();
        SkijaUi.text(canvas, current.isBlank() ? "Protocol" : "Current: " + current, cardX + cardW - 190f, (cardY + 34f) + SkijaUi.textMetrics(11f).getAscent(), SkijaUi.textMetrics(11f).getDescent() - SkijaUi.textMetrics(11f).getAscent(), (Math.round(alpha * 0.72f) << 24) | 0xFFFFFF, 11f);

        int save = canvas.save();
        try {
            canvas.translate(0f, (1f - visibility) * 20f);
            canvas.clipRect(gridBounds());
            if (previousPage >= 0) {
                float elapsed = pageElapsedMs();
                float outgoing = MainUIFlipAnimation.ease(elapsed / 140f);
                float incoming = MainUIFlipAnimation.ease((elapsed - 70f) / 180f);
                if (outgoing < 1f) drawGrid(canvas, previousPage, -pageDirection * 20f * outgoing, alpha * (1f - outgoing), current);
                if (incoming > 0f) drawGrid(canvas, page, pageDirection * 20f * (1f - incoming), alpha * incoming, current);
            } else {
                drawGrid(canvas, page, 0f, alpha, current);
            }
        } finally {
            canvas.restoreToCount(save);
        }
        float pagerY = cardY + cardH - 78f;
        float centerX = cardX + cardW * 0.5f;
        drawPageButton(canvas, centerX - 66f, pagerY, -1, alpha, page > 0);
        String count = (page + 1) + " / " + pageCount();
        SkijaUi.text(canvas, count, centerX - SkijaUi.textWidth(count, 11f) * 0.5f, pagerY, 26f,
                (Math.round(alpha * 0.72f) << 24) | 0xFFFFFF, 11f);
        drawPageButton(canvas, centerX + 34f, pagerY, 1, alpha, page + 1 < pageCount());

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

    private void drawGrid(Canvas canvas, int gridPage, float offset, float alpha, String current) {
        int save = canvas.save();
        try {
            canvas.translate(offset, 0f);
            for (int i = gridPage * PAGE_SIZE; i < Math.min(protocols.size(), (gridPage + 1) * PAGE_SIZE); i++) {
                Rect bounds = versionBounds(i % PAGE_SIZE);
                ViaFabricPlusBridge.ProtocolEntry protocol = protocols.get(i);
                drawVersion(canvas, i, protocol, imageFor(protocol.name()), bounds.getLeft(), bounds.getTop(),
                        bounds.getWidth(), bounds.getHeight(), Math.round(alpha), current, protocol.name().equals(current));
            }
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void drawVersion(Canvas canvas, int index, ViaFabricPlusBridge.ProtocolEntry protocol, Image image, float x, float y, float w, float h,
    int alpha, String current, boolean selected) {
        float hover = index < hoverAnimations.size() ? hoverAnimations.get(index) : 0f;
        float pressed = index == pressedProtocol
        ? Math.max(0f, 1f - (System.currentTimeMillis() - pressedProtocolAt) / 180f)
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
        boolean containsCurrent = protocol.name().equals(current);
        int titleColor = containsCurrent ? 0x75F28A : 0xFFFFFF;
        float textWidth = w - 20f;
        float size = 18f;
        while (size > 11f && SkijaUi.textWidth(protocol.name(), size) > textWidth) size -= 1f;
        String label = fit(protocol.name(), textWidth, size);
        float tw = SkijaUi.textWidth(label, size);
        SkijaUi.text(canvas, label, x + (w - tw) * 0.5f, y, h, (alpha << 24) | titleColor, size);
    }

    private String fit(String value, float maxWidth, float size) {
        if (SkijaUi.textWidth(value, size) <= maxWidth) return value;
        String result = value;
        while (result.length() > 1 && SkijaUi.textWidth(result + "...", size) > maxWidth) {
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
        float tw = SkijaUi.textWidth(text, 11f);
        SkijaUi.text(canvas, text, x + (w - tw) * 0.5f, (y + 17f) + SkijaUi.textMetrics(11f).getAscent(), SkijaUi.textMetrics(11f).getDescent() - SkijaUi.textMetrics(11f).getAscent(), (alpha << 24) | 0xFFFFFF, 11f);
    }

    private void drawPageButton(Canvas canvas, float x, float y, int direction, int alpha, boolean enabled) {
        boolean hovered = enabled && inside(pendingMouseX, pendingMouseY, x, y, 32f, 26f);
        try (Paint background = new Paint(); Paint arrow = new Paint(); PathBuilder builder = new PathBuilder()) {
            background.setAntiAlias(true);
            background.setColor((Math.round(alpha * (enabled ? hovered ? 0.28f : 0.15f : 0.05f)) << 24) | 0x73BDEB);
            canvas.drawRRect(RRect.makeXYWH(x, y, 32f, 26f, 10f), background);
            arrow.setAntiAlias(true);
            arrow.setColor((Math.round(alpha * (enabled ? 0.9f : 0.25f)) << 24) | 0xFFFFFF);
            arrow.setMode(PaintMode.STROKE);
            arrow.setStrokeWidth(1.8f);
            arrow.setStrokeCap(PaintStrokeCap.ROUND);
            arrow.setStrokeJoin(PaintStrokeJoin.ROUND);
            float centerX = x + 16f;
            float centerY = y + 13f;
            builder.moveTo(centerX - direction * 2.5f, centerY - 4.5f);
            builder.lineTo(centerX + direction * 2.5f, centerY);
            builder.lineTo(centerX - direction * 2.5f, centerY + 4.5f);
            try (Path path = builder.detach()) {
                canvas.drawPath(path, arrow);
            }
        }
    }

    private Image imageFor(String name) {
        String resource = ViaProtocolArt.resource(name);
        if (resource == null) return null;
        if (!images.containsKey(resource)) {
            Image image = null;
            try (InputStream stream = getClass().getResourceAsStream(resource)) {
                if (stream != null) image = Image.makeFromEncoded(stream.readAllBytes());
            } catch (Exception ignored) {
            }
            images.put(resource, image);
        }
        return images.get(resource);
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

    private boolean isProtocolHovered(int index) {
        if (index / PAGE_SIZE != page || !gridBounds().contains(pendingMouseX, pendingMouseY)) return false;
        float incoming = previousPage < 0 ? 1f : MainUIFlipAnimation.ease((pageElapsedMs() - 70f) / 180f);
        float offset = pageDirection * 20f * (1f - incoming);
        return incoming > 0f && versionBounds(index % PAGE_SIZE).contains(pendingMouseX - offset, pendingMouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (event.button() != 0 || closing || closeDispatched) return true;
        event = MainUiScale.pageEvent(event, width, height, layoutScale(), layoutCenterX(), layoutCenterY());
        double pageX = event.x();
        double pageY = event.y();
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
            openOverlay(ViaFabricPlusBridge::openSettings);
            return true;
        }
        if (inside(pageX, pageY, serversX, bottomY, backW, 26f)) {
            playClick();
            openOverlay(ViaFabricPlusBridge::openServerList);
            return true;
        }
        if (inside(pageX, pageY, reportX, bottomY, reportW, 26f)) {
            playClick();
            openOverlay(ViaFabricPlusBridge::openReportIssues);
            return true;
        }
        float pagerY = cardY + cardH - 78f;
        float centerX = cardX + cardW * 0.5f;
        if (inside(pageX, pageY, centerX - 66f, pagerY, 32f, 26f)) return turnPage(-1);
        if (inside(pageX, pageY, centerX + 34f, pagerY, 32f, 26f)) return turnPage(1);
        if (!gridBounds().contains((float) pageX, (float) pageY)) return true;
        float incoming = previousPage < 0 ? 1f : MainUIFlipAnimation.ease((pageElapsedMs() - 70f) / 180f);
        if (incoming <= 0f) return true;
        pageX -= pageDirection * 20f * (1f - incoming);
        for (int index = page * PAGE_SIZE; index < Math.min(protocols.size(), (page + 1) * PAGE_SIZE); index++) {
            if (versionBounds(index % PAGE_SIZE).contains((float) pageX, (float) pageY)) {
                ViaFabricPlusBridge.ProtocolEntry protocol = protocols.get(index);
                if (ViaFabricPlusBridge.setTargetVersion(protocol)) {
                    cachedTargetVersion = protocol.name();
                    nextTargetRefreshAt = System.currentTimeMillis() + 250L;
                    pressedProtocol = index;
                    pressedProtocolAt = System.currentTimeMillis();
                    playClick();
                }
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (closing || closeDispatched) return true;
        if (event.key() == GLFW.GLFW_KEY_LEFT) return turnPage(-1);
        if (event.key() == GLFW.GLFW_KEY_RIGHT) return turnPage(1);
        return super.keyPressed(event);
    }

    private boolean turnPage(int delta) {
        int next = Math.max(0, Math.min(pageCount() - 1, page + delta));
        if (next != page) {
            previousPage = page;
            page = next;
            pageDirection = delta;
            pageStartNanos = System.nanoTime();
            playClick();
        }
        return true;
    }

    private int pageCount() {
        return Math.max(1, (protocols.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private float pageElapsedMs() {
        return (System.nanoTime() - pageStartNanos) / 1_000_000f;
    }

    private Rect gridBounds() {
        return Rect.makeXYWH(cardX() + 22f, cardY() + 62f, cardW() - 44f, cardH() - 154f);
    }

    private Rect versionBounds(int slot) {
        Rect grid = gridBounds();
        float itemW = (grid.getWidth() - 48f) / 4f;
        float itemH = (grid.getHeight() - 42f) / 4f;
        return Rect.makeXYWH(grid.getLeft() + slot % 4 * (itemW + 16f),
                grid.getTop() + slot / 4 * (itemH + 14f), itemW, itemH);
    }

    private void openOverlay(Predicate<Screen> opener) {
        if (embedded && parent instanceof PVPUtilsMainUI mainUI) {
            mainUI.preserveEmbeddedPagesForOverlay();
            if (!opener.test(parent)) mainUI.cancelEmbeddedPagePreservation();
        } else {
            opener.test(this);
        }
    }

    private void playClick() {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
    }

    @Override
    public void onClose() {
        if (closing || closeDispatched) return;
        if (embeddedBack != null && minecraft != null && minecraft.screen != this) {
            closeDispatched = true;
            embeddedBack.run();
            return;
        }
        closing = true;
        closeStartMs = System.currentTimeMillis();
    }

    @Override
    public void removed() {
        pendingFrame = false;
        closeImages();
        super.removed();
    }

    private void closeImages() {
        for (Image image : images.values()) if (image != null) image.close();
        images.clear();
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

    private float cardW() {
        return Math.max(620f, Math.min(900f, layoutWidth() * 0.84f));
    }

    private float cardH() {
        return Math.max(370f, Math.min(540f, Math.min(layoutHeight() - 100f, layoutHeight() * 0.78f)));
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
        return MainUiScale.pageScale(cardX(), cardY(), cardX() + cardW(), cardY() + cardH());
    }

    private float layoutCenterX() {
        return cardX() + cardW() * 0.5f;
    }

    private float layoutCenterY() {
        return cardY() + cardH() * 0.5f;
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
