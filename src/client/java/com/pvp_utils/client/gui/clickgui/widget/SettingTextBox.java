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
    private final Paint borderPaint = new Paint().setAntiAlias(true);
    private final Paint cursorPaint = new Paint().setAntiAlias(true);
    private float focusAlpha;
    private float textOffset;
    private float cursorTime;

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
        cursorTime += 0.016f;

        int bg = lerpColor(0xFFF5F6FA, 0xFFEFF3FF, focusAlpha);
        bgPaint.setColor(withAlpha(bg, alpha));
        canvas.drawRRect(RRect.makeXYWH(x, y, getWidth(), getHeight(), 7f), bgPaint);

        borderPaint.setColor(withAlpha(0xFF5A73E8, alpha * focusAlpha));
        canvas.drawRRect(RRect.makeXYWH(x, y, getWidth(), getHeight(), 7f), borderPaint);
        bgPaint.setColor(withAlpha(bg, alpha));
        canvas.drawRRect(RRect.makeXYWH(x + 1f, y + 1f, getWidth() - 2f, getHeight() - 2f, 6f), bgPaint);

        String text = getValue();
        boolean empty = text.isEmpty();
        String display = empty && !active ? UiText.t("点击输入", "Click to type") : text;
        float textX = x + 9f;
        float textW = getWidth() - 18f;
        float realTextW = FontRenderer.measureTextWidth(text, 10f);
        float targetOffset = active ? Math.max(0f, realTextW - textW + 3f) : 0f;
        textOffset += (targetOffset - textOffset) * 0.22f;

        canvas.save();
        canvas.clipRect(Rect.makeXYWH(textX, y + 2f, textW, getHeight() - 4f));
        FontRenderer.drawText(canvas, display, textX - (active ? textOffset : 0f), y + 15.5f, 10f,
                withAlpha(empty && !active ? 0x999999 : 0x333333, alpha));
        if (active) {
            float cursorPulse = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(cursorTime * 6f));
            float cursorX = textX + Math.min(textW - 1f, Math.max(0f, realTextW - textOffset));
            cursorPaint.setColor(withAlpha(0xFF5A73E8, alpha * cursorPulse));
            canvas.drawRect(Rect.makeXYWH(cursorX, y + 5f, 1f, 14f), cursorPaint);
        }
        canvas.restore();
    }

    @Override
    public boolean isAnimating() {
        return focused == this || focusAlpha > 0.01f || Math.abs(textOffset) > 0.01f;
    }

    @Override
    public boolean onClick(float mx, float my, float x, float y, int button) {
        if (button != 0) return false;
        if (mx < x || mx > x + getWidth() || my < y || my > y + getHeight()) {
            if (focused == this) clearFocus();
            return false;
        }
        focus(this);
        return true;
    }

    public static boolean keyPressed(KeyEvent event) {
        if (focused == null) return false;
        boolean ctrlDown = isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL);
        if (ctrlDown && event.key() == GLFW.GLFW_KEY_V) {
            focused.insert(Minecraft.getInstance().keyboardHandler.getClipboard());
            return true;
        }
        if (ctrlDown && event.key() == GLFW.GLFW_KEY_A) {
            focused.setValue("");
            return true;
        }
        switch (event.key()) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                focused.deletePrevious();
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
        String value = getValue();
        if (value.isEmpty()) return;
        int end = value.offsetByCodePoints(value.length(), -1);
        setValue(value.substring(0, end));
    }

    private void insert(String text) {
        if (text == null || text.isEmpty()) return;
        StringBuilder out = new StringBuilder(getValue());
        text.codePoints().forEach(codepoint -> {
            if (out.codePointCount(0, out.length()) >= maxLength) return;
            if (isAllowed(codepoint)) out.appendCodePoint(codepoint);
        });
        setValue(out.toString());
    }

    private String getValue() {
        String value = valueSupplier.get();
        return value == null ? "" : value;
    }

    private void setValue(String value) {
        valueConsumer.accept(trimToMaxLength(value == null ? "" : value));
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
