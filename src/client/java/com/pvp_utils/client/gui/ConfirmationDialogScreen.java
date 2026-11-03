package com.pvp_utils.client.gui;

import com.pvp_utils.client.render.skia.SkijaScreen;
import com.pvp_utils.client.render.skia.SkijaUi;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.types.Rect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

public final class ConfirmationDialogScreen extends Screen implements SkijaScreen {
    private static final int TEXT_COLOR = 0xFF252936;
    private static final int BODY_COLOR = 0xFF687080;
    private static final int MUTED_COLOR = 0xFF7C8190;
    private static final int ACCENT_COLOR = 0xFF687AF2;
    private final Screen parent;
    private final String message;
    private final String note;
    private final String cancelLabel;
    private final String confirmLabel;
    private final List<String> details;
    private final Consumer<Boolean> callback;
    private final List<TextLine> bodyLines = new ArrayList<>();
    private List<String> titleLines = List.of();
    private int focusedButton;
    private boolean keyboardFocus = true;
    private boolean resolved;
    private double mouseX = -1;
    private double mouseY = -1;
    private float scale = 1;
    private float cardX;
    private float cardY;
    private float cardWidth;
    private float cardHeight;
    private float bodyY;
    private float bodyHeight;
    private float bodyViewportHeight;
    private float buttonY;
    private float buttonWidth;
    private float buttonHeight = 42;
    private float titleFontSize = 21;
    private float titleLineHeight = 27;
    private float topPadding = 27;
    private float scrollOffset;
    private int layoutWidth = -1;
    private int layoutHeight = -1;
    private int layoutGuiScale = -1;

    public ConfirmationDialogScreen(Screen parent, String title, String message, String note,
                                    String cancelLabel, String confirmLabel, Consumer<Boolean> callback) {
        this(parent, title, message, note, cancelLabel, confirmLabel, List.of(), callback);
    }

    public ConfirmationDialogScreen(Screen parent, String title, String message, String note,
                                    String cancelLabel, String confirmLabel, List<String> details,
                                    Consumer<Boolean> callback) {
        super(Component.literal(Objects.requireNonNullElse(title, "")));
        this.parent = parent;
        this.message = Objects.requireNonNullElse(message, "");
        this.note = Objects.requireNonNullElse(note, "");
        this.cancelLabel = Objects.requireNonNullElse(cancelLabel, "Cancel");
        this.confirmLabel = Objects.requireNonNullElse(confirmLabel, "Confirm");
        this.details = details == null ? List.of() : details.stream().filter(Objects::nonNull).toList();
        this.callback = Objects.requireNonNull(callback);
    }

    public Screen parent() {
        return parent;
    }

    @Override
    protected void init() {
        ensureLayout();
    }

    @Override
    protected void repositionElements() {
        ensureLayout();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (minecraft.level == null) super.renderBackground(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    @Override
    public void renderSkija(Canvas canvas) {
        if (!inputAllowed()) return;
        ensureLayout();
        SkijaUi.fill(canvas, 0, 0, width, height, 0x880D1428);
        int saved = canvas.save();
        try {
            canvas.scale(scale, scale);
            SkijaUi.dropShadowRounded(canvas, cardX, cardY, cardWidth, cardHeight, 24, 7, 28, 0x400B1027);
            SkijaUi.rounded(canvas, cardX, cardY, cardWidth, cardHeight, 24, 0xFFFDFDFF);
            float titleY = cardY + topPadding;
            for (String line : titleLines) {
                SkijaUi.boldText(canvas, line, cardX + 30, titleY, titleLineHeight, TEXT_COLOR, titleFontSize);
                titleY += titleLineHeight;
            }
            drawClose(canvas);
            drawBody(canvas);
            drawButton(canvas, 0, cardX + 24, cancelLabel);
            drawButton(canvas, 1, cardX + cardWidth - 24 - buttonWidth, confirmLabel);
        } finally {
            canvas.restoreToCount(saved);
        }
    }

    private void drawClose(Canvas canvas) {
        float x = cardX + cardWidth - 47;
        float y = cardY + 19;
        boolean hover = hit(x, y, 28, 28, mouseX, mouseY);
        if (hover) SkijaUi.rounded(canvas, x, y, 28, 28, 14, 0xFFF0F1F7);
        if (keyboardFocus && focusedButton == 2) SkijaUi.outline(canvas, x, y, 28, 28, 14, 1.5f, ACCENT_COLOR);
        SkijaUi.line(canvas, x + 9, y + 9, x + 19, y + 19, 1.5f, MUTED_COLOR);
        SkijaUi.line(canvas, x + 19, y + 9, x + 9, y + 19, 1.5f, MUTED_COLOR);
    }

    private void drawBody(Canvas canvas) {
        int saved = canvas.save();
        try {
            canvas.clipRect(Rect.makeXYWH(cardX + 29, bodyY, cardWidth - 58, bodyViewportHeight));
            float y = bodyY - scrollOffset;
            for (TextLine line : bodyLines) {
                if (y + line.height() >= bodyY && y <= bodyY + bodyViewportHeight) {
                    SkijaUi.text(canvas, line.text(), cardX + 30, y, line.height(), line.color(), line.size());
                }
                y += line.height();
            }
        } finally {
            canvas.restoreToCount(saved);
        }
        float maxScroll = maxScroll();
        if (maxScroll > 0) {
            if (scrollOffset > 0) {
                SkijaUi.gradient(canvas, cardX + 29, bodyY, cardWidth - 58, 10, 0xFFFDFDFF, 0x00FDFDFF, true, 0);
            }
            if (scrollOffset < maxScroll) {
                SkijaUi.gradient(canvas, cardX + 29, bodyY + bodyViewportHeight - 10, cardWidth - 58, 10,
                        0x00FDFDFF, 0xFFFDFDFF, true, 0);
            }
            float x = cardX + cardWidth - 20;
            float thumbHeight = Math.max(18, bodyViewportHeight * bodyViewportHeight / bodyHeight);
            float thumbY = bodyY + (bodyViewportHeight - thumbHeight) * scrollOffset / maxScroll;
            SkijaUi.rounded(canvas, x, bodyY, 3, bodyViewportHeight, 1.5f, 0xFFEFF0F6);
            SkijaUi.rounded(canvas, x, thumbY, 3, thumbHeight, 1.5f, 0xFFAEB6D3);
        }
    }

    private void drawButton(Canvas canvas, int index, float x, String label) {
        boolean hover = hit(x, buttonY, buttonWidth, buttonHeight, mouseX, mouseY);
        int color = index == 0 ? (hover ? 0xFFE9EBF3 : 0xFFF1F2F7) : (hover ? 0xFF596BDF : ACCENT_COLOR);
        SkijaUi.rounded(canvas, x, buttonY, buttonWidth, buttonHeight, 16, color);
        if (keyboardFocus && focusedButton == index) {
            SkijaUi.outline(canvas, x - 3, buttonY - 3, buttonWidth + 6, buttonHeight + 6, 19, 1.5f, ACCENT_COLOR);
        }
        float fontSize = 13;
        while (fontSize > 9 && SkijaUi.textWidth(label, fontSize) > buttonWidth - 20) fontSize -= 0.5f;
        String fitted = ellipsize(label, buttonWidth - 20, fontSize, false);
        float textX = x + (buttonWidth - SkijaUi.textWidth(fitted, fontSize)) * 0.5f;
        SkijaUi.text(canvas, fitted, textX, buttonY, buttonHeight, index == 0 ? TEXT_COLOR : 0xFFFFFFFF, fontSize);
    }

    private void ensureLayout() {
        int guiScale = Math.max(1, minecraft.getWindow().getGuiScale());
        if (layoutWidth == width && layoutHeight == height && layoutGuiScale == guiScale) return;
        layoutWidth = width;
        layoutHeight = height;
        layoutGuiScale = guiScale;
        float availableWidth = Math.max(1, width);
        float availableHeight = Math.max(1, height);
        scale = Math.min(2f / guiScale, Math.min(availableWidth / 320f, availableHeight / 240f));
        float viewportWidth = availableWidth / scale;
        float viewportHeight = availableHeight / scale;
        boolean compact = viewportHeight < 280;
        titleFontSize = compact ? 18 : 21;
        titleLineHeight = compact ? 24 : 27;
        topPadding = compact ? 22 : 27;
        buttonHeight = compact ? 36 : 42;
        cardWidth = Math.min(460, viewportWidth - 32);
        titleLines = wrap(title.getString(), cardWidth - 96, titleFontSize, true);
        if (titleLines.size() > 2) {
            titleLines = List.of(titleLines.getFirst(), ellipsize(titleLines.get(1) + "…", cardWidth - 96, titleFontSize, true));
        }
        bodyLines.clear();
        appendLines(message, cardWidth - 64, 13.5f, 21, BODY_COLOR);
        for (String detail : details) {
            appendGap(8);
            appendLines(detail, cardWidth - 64, 11.5f, 18, 0xFF596070);
        }
        if (!note.isBlank()) {
            appendGap(16);
            appendLines(note, cardWidth - 64, 11, 17, MUTED_COLOR);
        }
        bodyHeight = 0;
        for (TextLine line : bodyLines) bodyHeight += line.height();
        float headerHeight = topPadding + titleLines.size() * titleLineHeight + (compact ? 12 : 15);
        float bottomPadding = compact ? 20 : 24;
        float bodyGap = compact ? 14 : 18;
        float footerHeight = buttonHeight + bottomPadding + bodyGap;
        cardHeight = Math.min(viewportHeight - 32, Math.min(390, Math.max(compact ? 200 : 230, headerHeight + bodyHeight + footerHeight)));
        cardX = (viewportWidth - cardWidth) * 0.5f;
        cardY = (viewportHeight - cardHeight) * 0.5f;
        bodyY = cardY + headerHeight;
        buttonY = cardY + cardHeight - buttonHeight - bottomPadding;
        buttonWidth = (cardWidth - 62) * 0.5f;
        bodyViewportHeight = Math.max(1, buttonY - bodyGap - bodyY);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll()));
    }

    private void appendLines(String text, float width, float size, float height, int color) {
        for (String line : wrap(text, width, size, false)) bodyLines.add(new TextLine(line, size, height, color));
    }

    private void appendGap(float height) {
        if (!bodyLines.isEmpty()) bodyLines.add(new TextLine("", 11, height, MUTED_COLOR));
    }

    private static List<String> wrap(String text, float width, float size, boolean bold) {
        if (text.isEmpty()) return List.of();
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.replace("\r\n", "\n").replace('\r', '\n').replace("\t", "    ").split("\n", -1)) {
            BreakIterator iterator = BreakIterator.getLineInstance(Locale.ROOT);
            iterator.setText(paragraph);
            StringBuilder line = new StringBuilder();
            for (int start = iterator.first(), end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
                String segment = paragraph.substring(start, end);
                if (!line.isEmpty() && textWidth(line + segment, size, bold) > width) {
                    lines.add(line.toString().stripTrailing());
                    line.setLength(0);
                    segment = segment.stripLeading();
                }
                for (int offset = 0; offset < segment.length();) {
                    int codePoint = segment.codePointAt(offset);
                    String character = new String(Character.toChars(codePoint));
                    if (!line.isEmpty() && textWidth(line + character, size, bold) > width) {
                        lines.add(line.toString().stripTrailing());
                        line.setLength(0);
                    }
                    line.append(character);
                    offset += Character.charCount(codePoint);
                }
            }
            lines.add(line.toString().stripTrailing());
        }
        return lines;
    }

    private static String ellipsize(String text, float width, float size, boolean bold) {
        if (textWidth(text, size, bold) <= width) return text;
        String candidate = text;
        while (!candidate.isEmpty() && textWidth(candidate + "…", size, bold) > width) {
            candidate = candidate.substring(0, candidate.offsetByCodePoints(candidate.length(), -1));
        }
        return candidate + "…";
    }

    private static float textWidth(String text, float size, boolean bold) {
        return bold ? SkijaUi.boldTextWidth(text, size) : SkijaUi.textWidth(text, size);
    }

    private float maxScroll() {
        return Math.max(0, bodyHeight - bodyViewportHeight);
    }

    private boolean inputAllowed() {
        return !resolved && minecraft.screen == this && minecraft.getOverlay() == null;
    }

    private boolean hit(float x, float y, float width, float height, double mouseX, double mouseY) {
        double localX = mouseX / scale;
        double localY = mouseY / scale;
        return localX >= x && localX <= x + width && localY >= y && localY <= y + height;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        keyboardFocus = false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!inputAllowed()) return true;
        ensureLayout();
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;
        if (hit(cardX + cardWidth - 47, cardY + 19, 28, 28, event.x(), event.y())
                || hit(cardX + 24, buttonY, buttonWidth, buttonHeight, event.x(), event.y())) {
            finish(false);
        } else if (hit(cardX + cardWidth - 24 - buttonWidth, buttonY, buttonWidth, buttonHeight, event.x(), event.y())) {
            finish(true);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (!inputAllowed()) return true;
        ensureLayout();
        if (hit(cardX, cardY, cardWidth, cardHeight, mouseX, mouseY)) scroll(-vertical * 27);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!inputAllowed()) return true;
        ensureLayout();
        keyboardFocus = true;
        if (event.isEscape()) {
            finish(false);
        } else if (event.isCycleFocus()) {
            focusedButton = Math.floorMod(focusedButton + (event.hasShiftDown() ? -1 : 1), 3);
        } else if (event.isLeft() || event.isRight()) {
            focusedButton = event.isLeft() ? 0 : 1;
        } else if (event.isSelection()) {
            finish(focusedButton == 1);
        } else if (event.isUp() || event.isDown()) {
            scroll(event.isUp() ? -27 : 27);
        } else if (event.key() == GLFW.GLFW_KEY_PAGE_UP || event.key() == GLFW.GLFW_KEY_PAGE_DOWN) {
            scroll(event.key() == GLFW.GLFW_KEY_PAGE_UP ? -bodyViewportHeight : bodyViewportHeight);
        } else if (event.key() == GLFW.GLFW_KEY_HOME || event.key() == GLFW.GLFW_KEY_END) {
            scrollOffset = event.key() == GLFW.GLFW_KEY_HOME ? 0 : maxScroll();
        }
        return true;
    }

    private void scroll(double amount) {
        scrollOffset = (float) Math.max(0, Math.min(maxScroll(), scrollOffset + amount));
    }

    private void finish(boolean confirmed) {
        if (resolved) return;
        resolved = true;
        try {
            if (minecraft.screen == this) minecraft.setScreen(parent);
        } finally {
            callback.accept(confirmed);
        }
    }

    @Override
    public void onClose() {
        finish(false);
    }

    @Override
    public void removed() {
        if (resolved) return;
        resolved = true;
        minecraft.schedule(() -> callback.accept(false));
    }

    @Override
    public Component getNarrationMessage() {
        return Component.literal(title.getString() + "\n" + message + "\n" + String.join("\n", details) + "\n" + note);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record TextLine(String text, float size, float height, int color) {
    }
}
