package com.pvp_utils.client.gui.clickgui.widget;

import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.modules.impl.Optimize.InputMethodFix.InputMethodFix;
import com.pvp_utils.client.render.font.FontRenderer;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SettingTextBox extends SettingWidget {
    private static SettingTextBox focused;

    private final Supplier<String> valueSupplier;
    private final Consumer<String> valueConsumer;
    private final int maxLength;
    private final Paint bgPaint = new Paint().setAntiAlias(true);
    private final Paint linePaint = new Paint().setAntiAlias(true);
    private final Paint selectionPaint = new Paint().setAntiAlias(true);
    private final Paint cursorPaint = new Paint().setAntiAlias(true);
    private float focusAlpha;
    private float textOffset;
    private float cursorTime;
    private int cursor;
    private int selectionAnchor = -1;

    public SettingTextBox(Supplier<String> valueSupplier, Consumer<String> valueConsumer, int maxLength) {
        this.valueSupplier = valueSupplier;
        this.valueConsumer = valueConsumer;
        this.maxLength = Math.max(1, maxLength);
    }

    @Override public float getWidth() { return 150f; }
    @Override public float getHeight() { return 24f; }

    @Override
    public void draw(Canvas canvas, float x, float y, float alpha) {
        boolean active = focused == this;
        focusAlpha += ((active ? 1f : 0f) - focusAlpha) * 0.22f;

        int background = lerpColor(0xF1F2F5, 0xE9EEFF, focusAlpha);
        bgPaint.setColor(withAlpha(background, alpha));
        canvas.drawRRect(RRect.makeXYWH(x, y, getWidth(), getHeight(), 7f), bgPaint);

        String text = getValue();
        clampCursor(text);
        boolean empty = text.isEmpty();
        String display = empty ? UiText.t("点击输入", "Click to type") : text;
        float textX = x + 9f;
        float textW = getWidth() - 18f;
        float realTextW = FontRenderer.measureTextWidth(text, 10f);
        float cursorTextW = FontRenderer.measureTextWidth(text.substring(0, cursor), 10f);
        float targetOffset = active ? Math.max(0f, cursorTextW - textW + 3f) : 0f;
        textOffset += (targetOffset - textOffset) * 0.22f;

        canvas.save();
        canvas.clipRect(Rect.makeXYWH(textX, y + 2f, textW, getHeight() - 4f));
        if (active) {
            int selectionStart = selectionStart();
            int selectionEnd = selectionEnd();
            if (selectionStart != selectionEnd) {
                float selectionX = textX + FontRenderer.measureTextWidth(text.substring(0, selectionStart), 10f) - textOffset;
                float selectionW = FontRenderer.measureTextWidth(text.substring(selectionStart, selectionEnd), 10f);
                selectionPaint.setColor(withAlpha(0xB9C8FF, alpha));
                canvas.drawRect(Rect.makeXYWH(selectionX, y + 4f, selectionW, 16f), selectionPaint);
            }
        }
        FontRenderer.drawText(canvas, display, textX - (empty ? 0f : textOffset), y + 15.5f, 10f,
                withAlpha(empty ? 0x9BA1AE : 0x343842, alpha));
        if (active) {
            float cursorPulse = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(cursorTime * 6f));
            float cursorX = textX + Math.min(textW - 1f, Math.max(0f, cursorTextW - textOffset));
            cursorPaint.setColor(withAlpha(0xFF5A73E8, alpha * cursorPulse));
            canvas.drawRect(Rect.makeXYWH(cursorX, y + 5f, 1f, 14f), cursorPaint);
        }
        canvas.restore();

        float linePulse = 0.3f + 0.7f * (0.5f + 0.5f * (float) Math.sin(cursorTime * 6f));
        linePaint.setColor(withAlpha(0x5A73E8, alpha * focusAlpha * linePulse));
        canvas.drawRect(Rect.makeXYWH(x + 8f, y + getHeight() - 2f, getWidth() - 16f, 1f), linePaint);
    }

    @Override
    public boolean isAnimating() {
        return focused == this || focusAlpha > 0.01f || Math.abs(textOffset) > 0.01f;
    }

    @Override
    public void update(float dt) {
        cursorTime += dt;
    }

    @Override
    public boolean onClick(float mx, float my, float x, float y, int button) {
        if (button != 0) return false;
        if (mx < x || mx > x + getWidth() || my < y || my > y + getHeight()) {
            if (focused == this) clearFocus();
            return false;
        }
        focus(this);
        moveCursorTo(cursorAt(getValue(), mx - (x + 9f) + textOffset), false);
        return true;
    }

    @Override
    public boolean onDrag(float mx, float my, float x, float y) {
        if (focused != this) return false;
        moveCursorTo(cursorAt(getValue(), mx - (x + 9f) + textOffset), true);
        return true;
    }

    public static boolean keyPressed(KeyEvent event) {
        if (focused == null) return false;
        boolean ctrlDown = isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL);
        boolean shiftDown = isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);
        if (ctrlDown && event.key() == GLFW.GLFW_KEY_V) {
            focused.insert(Minecraft.getInstance().keyboardHandler.getClipboard());
            return true;
        }
        if (ctrlDown && event.key() == GLFW.GLFW_KEY_A) {
            focused.selectAll();
            return true;
        }
        if (ctrlDown && event.key() == GLFW.GLFW_KEY_C) {
            focused.copySelection();
            return true;
        }
        if (ctrlDown && event.key() == GLFW.GLFW_KEY_X) {
            focused.copySelection();
            focused.deleteSelection();
            return true;
        }
        switch (event.key()) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                focused.deletePrevious();
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                focused.deleteNext();
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                focused.moveCursor(-1, shiftDown);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                focused.moveCursor(1, shiftDown);
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                focused.moveCursorTo(0, shiftDown);
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                focused.moveCursorTo(focused.getValue().length(), shiftDown);
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_ESCAPE -> {
                clearFocus();
                return true;
            }
            default -> {
                return true;
            }
        }
    }

    public static boolean charTyped(CharacterEvent event) {
        if (focused == null) return false;
        focused.insert(event.codepointAsString());
        return true;
    }

    public static void clearFocus() {
        if (focused != null) {
            focused = null;
            InputMethodFix.setCustomTextInputActive(false, Minecraft.getInstance());
        }
    }

    public static boolean isFocused() {
        return focused != null;
    }

    private static void focus(SettingTextBox box) {
        focused = box;
        box.cursorTime = 0f;
        InputMethodFix.setCustomTextInputActive(true, Minecraft.getInstance());
    }

    private void deletePrevious() {
        if (deleteSelection()) return;
        String value = getValue();
        if (cursor <= 0) return;
        int start = value.offsetByCodePoints(cursor, -1);
        setValue(value.substring(0, start) + value.substring(cursor));
        cursor = start;
    }

    private void deleteNext() {
        if (deleteSelection()) return;
        String value = getValue();
        if (cursor >= value.length()) return;
        int end = value.offsetByCodePoints(cursor, 1);
        setValue(value.substring(0, cursor) + value.substring(end));
    }

    private void insert(String text) {
        if (text == null || text.isEmpty()) return;
        StringBuilder out = new StringBuilder();
        text.codePoints().forEach(codepoint -> {
            if (isAllowed(codepoint)) out.appendCodePoint(codepoint);
        });
        if (out.isEmpty()) return;
        String value = getValue();
        int start = selectionStart();
        int end = selectionEnd();
        setValue(value.substring(0, start) + out + value.substring(end));
        cursor = Math.min(start + out.length(), getValue().length());
        selectionAnchor = -1;
    }

    private String getValue() {
        String value = valueSupplier.get();
        return value == null ? "" : value;
    }

    private void setValue(String value) {
        valueConsumer.accept(trimToMaxLength(value == null ? "" : value));
    }

    private boolean deleteSelection() {
        int start = selectionStart();
        int end = selectionEnd();
        if (start == end) {
            selectionAnchor = -1;
            return false;
        }
        String value = getValue();
        setValue(value.substring(0, start) + value.substring(end));
        cursor = start;
        selectionAnchor = -1;
        return true;
    }

    private void copySelection() {
        int start = selectionStart();
        int end = selectionEnd();
        if (start != end) {
            Minecraft.getInstance().keyboardHandler.setClipboard(getValue().substring(start, end));
        }
    }

    private void moveCursor(int codePointDelta, boolean selecting) {
        String value = getValue();
        if (!selecting && selectionAnchor >= 0) {
            moveCursorTo(codePointDelta < 0 ? selectionStart() : selectionEnd(), false);
            return;
        }
        int next = cursor;
        if (codePointDelta < 0 && cursor > 0) {
            next = value.offsetByCodePoints(cursor, -1);
        } else if (codePointDelta > 0 && cursor < value.length()) {
            next = value.offsetByCodePoints(cursor, 1);
        }
        moveCursorTo(next, selecting);
    }

    private void moveCursorTo(int next, boolean selecting) {
        if (selecting && selectionAnchor < 0) {
            selectionAnchor = cursor;
        } else if (!selecting) {
            selectionAnchor = -1;
        }
        cursor = Math.max(0, Math.min(next, getValue().length()));
    }

    private void selectAll() {
        selectionAnchor = 0;
        cursor = getValue().length();
    }

    private int selectionStart() {
        return Math.min(selectionAnchor < 0 ? cursor : selectionAnchor, cursor);
    }

    private int selectionEnd() {
        return Math.max(selectionAnchor < 0 ? cursor : selectionAnchor, cursor);
    }

    private int cursorAt(String value, float relativeX) {
        if (relativeX <= 0f || value.isEmpty()) return 0;
        int index = 0;
        while (index < value.length()) {
            int next = value.offsetByCodePoints(index, 1);
            float currentWidth = FontRenderer.measureTextWidth(value.substring(0, index), 10f);
            float nextWidth = FontRenderer.measureTextWidth(value.substring(0, next), 10f);
            if (relativeX < (currentWidth + nextWidth) * 0.5f) return index;
            index = next;
        }
        return value.length();
    }

    private void clampCursor(String value) {
        cursor = Math.max(0, Math.min(cursor, value.length()));
        if (selectionAnchor > value.length()) selectionAnchor = value.length();
    }

    private String trimToMaxLength(String value) {
        if (value.codePointCount(0, value.length()) <= maxLength) return value;
        int end = value.offsetByCodePoints(0, maxLength);
        return value.substring(0, end);
    }

    private static boolean isAllowed(int codepoint) {
        return codepoint >= 32 && codepoint != 127 && codepoint != '\n' && codepoint != '\r' && codepoint != '\t';
    }

    private static boolean isKeyDown(int key) {
        long handle = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
    }

    private static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int)(ar+(br-ar)*t) << 16) | ((int)(ag+(bg-ag)*t) << 8) | (int)(ab+(bb-ab)*t);
    }
}
