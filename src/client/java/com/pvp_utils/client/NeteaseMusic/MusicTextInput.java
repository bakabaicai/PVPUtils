package com.pvp_utils.client.NeteaseMusic;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.types.Rect;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class MusicTextInput {
    private static final int MAX_LENGTH = 256;
    private static final int KEY_A = 65;
    private static final int KEY_C = 67;
    private static final int KEY_V = 86;
    private static final int KEY_X = 88;
    private static final int KEY_BACKSPACE = 259;
    private static final int KEY_DELETE = 261;
    private static final int KEY_RIGHT = 262;
    private static final int KEY_LEFT = 263;
    private static final int KEY_HOME = 268;
    private static final int KEY_END = 269;
    private String value = "";
    private int cursor;
    private int anchor = -1;
    private float textX;
    private float textWidth = 1f;
    private float fontSize = 8f;
    private float scroll;
    private long caretStartedAt = System.nanoTime();

    public String value() {
        return value;
    }

    public void setValue(String value) {
        this.value = clean(value, MAX_LENGTH);
        cursor = length();
        anchor = -1;
        scroll = 0f;
        resetCaret();
    }

    public void clear() {
        setValue("");
    }

    public void draw(Canvas canvas, float x, float y, float width, float height, boolean focused, String placeholder) {
        if (width <= 0f || height <= 0f) return;
        float leftPadding = Math.min(18f, width * 0.4f);
        float rightPadding = Math.min(6f, width * 0.15f);
        textX = x + leftPadding;
        textWidth = Math.max(1f, width - leftPadding - rightPadding);
        if (focused) revealCursor();
        else scroll = 0f;
        MusicUi.rounded(canvas, x, y, width, height, 4f,
                focused ? MusicUi.alpha(MusicUi.ACCENT, 0.85f) : 0xFF42444B);
        MusicUi.rounded(canvas, x + 0.8f, y + 0.8f, width - 1.6f, height - 1.6f, 3.2f, MusicUi.PANEL);
        MusicUi.icon(canvas, "search", x + 4f, y + (height - 10f) * 0.5f, 10f, MusicUi.MUTED);
        int save = canvas.save();
        try {
            canvas.clipRect(Rect.makeXYWH(textX, y + 1f, textWidth, height - 2f));
            float top = y + (height - fontSize * 1.25f) * 0.5f;
            if (value.isEmpty() && !focused) {
                MusicUi.text(canvas, placeholder, textX, top, fontSize, MusicUi.MUTED);
            } else {
                if (focused && hasSelection()) {
                    float left = prefixWidth(selectionStart());
                    float right = prefixWidth(selectionEnd());
                    MusicUi.rounded(canvas, textX + left - scroll, y + 3f,
                            right - left, Math.max(1f, height - 6f), 1.5f, MusicUi.alpha(MusicUi.ACCENT, 0.4f));
                }
                MusicUi.text(canvas, value, textX - scroll, top, fontSize, MusicUi.TEXT);
            }
            if (focused && (System.nanoTime() - caretStartedAt) / 500_000_000L % 2L == 0L) {
                float caret = textX + prefixWidth(cursor) - scroll;
                MusicUi.line(canvas, caret, y + 4f, caret, y + height - 4f, 1f, MusicUi.TEXT);
            }
        } finally {
            canvas.restoreToCount(save);
        }
    }

    public void click(float x, boolean shift) {
        int next = positionAt(x);
        if (shift) {
            if (anchor < 0) anchor = cursor;
        } else {
            anchor = next;
        }
        cursor = next;
        resetCaret();
        revealCursor();
    }

    public void drag(float x) {
        if (anchor < 0) anchor = cursor;
        cursor = positionAt(x);
        resetCaret();
        revealCursor();
    }

    public boolean keyPressed(int key, boolean ctrl, boolean shift,
                              Supplier<String> clipboard, Consumer<String> clipboardWriter) {
        if (ctrl) {
            switch (key) {
                case KEY_A -> {
                    anchor = 0;
                    cursor = length();
                    resetCaret();
                    revealCursor();
                    return true;
                }
                case KEY_C -> {
                    copySelection(clipboardWriter);
                    return true;
                }
                case KEY_X -> {
                    if (copySelection(clipboardWriter)) deleteSelection();
                    return true;
                }
                case KEY_V -> {
                    if (clipboard != null) {
                        try {
                            insert(clipboard.get());
                        } catch (RuntimeException ignored) {
                        }
                    }
                    return true;
                }
                default -> {
                }
            }
        }
        switch (key) {
            case KEY_LEFT -> move(!shift && hasSelection() ? selectionStart()
                    : ctrl ? previousWord() : Math.max(0, cursor - 1), shift);
            case KEY_RIGHT -> move(!shift && hasSelection() ? selectionEnd()
                    : ctrl ? nextWord() : Math.min(length(), cursor + 1), shift);
            case KEY_HOME -> move(0, shift);
            case KEY_END -> move(length(), shift);
            case KEY_BACKSPACE -> {
                if (!deleteSelection() && cursor > 0) {
                    anchor = ctrl ? previousWord() : cursor - 1;
                    deleteSelection();
                }
            }
            case KEY_DELETE -> {
                if (!deleteSelection() && cursor < length()) {
                    anchor = ctrl ? nextWord() : cursor + 1;
                    deleteSelection();
                }
            }
            default -> {
                return false;
            }
        }
        resetCaret();
        revealCursor();
        return true;
    }

    public void insert(String inserted) {
        int start = selectionStart();
        int end = selectionEnd();
        int remaining = MAX_LENGTH - length() + end - start;
        String addition = clean(inserted, remaining);
        if (addition.isEmpty()) return;
        value = value.substring(0, offset(start)) + addition + value.substring(offset(end));
        cursor = start + addition.codePointCount(0, addition.length());
        anchor = -1;
        resetCaret();
        revealCursor();
    }

    private boolean copySelection(Consumer<String> writer) {
        if (!hasSelection() || writer == null) return false;
        try {
            writer.accept(value.substring(offset(selectionStart()), offset(selectionEnd())));
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean deleteSelection() {
        if (!hasSelection()) return false;
        int start = selectionStart();
        value = value.substring(0, offset(start)) + value.substring(offset(selectionEnd()));
        cursor = start;
        anchor = -1;
        resetCaret();
        revealCursor();
        return true;
    }

    private void move(int next, boolean shift) {
        if (shift) {
            if (anchor < 0) anchor = cursor;
        } else {
            anchor = -1;
        }
        cursor = Math.max(0, Math.min(length(), next));
    }

    private int previousWord() {
        int[] points = value.codePoints().toArray();
        int result = cursor;
        while (result > 0 && Character.isWhitespace(points[result - 1])) result--;
        if (result == 0) return 0;
        boolean word = wordCharacter(points[result - 1]);
        while (result > 0 && !Character.isWhitespace(points[result - 1])
                && wordCharacter(points[result - 1]) == word) result--;
        return result;
    }

    private int nextWord() {
        int[] points = value.codePoints().toArray();
        int result = cursor;
        if (result < points.length && !Character.isWhitespace(points[result])) {
            boolean word = wordCharacter(points[result]);
            while (result < points.length && !Character.isWhitespace(points[result])
                    && wordCharacter(points[result]) == word) result++;
        }
        while (result < points.length && Character.isWhitespace(points[result])) result++;
        return result;
    }

    private int positionAt(float x) {
        float target = x - textX + scroll;
        if (target <= 0f) return 0;
        int count = length();
        float previous = 0f;
        for (int i = 1; i <= count; i++) {
            float next = prefixWidth(i);
            if (target < (previous + next) * 0.5f) return i - 1;
            previous = next;
        }
        return count;
    }

    private void revealCursor() {
        float caret = prefixWidth(cursor);
        float visibleWidth = Math.max(1f, textWidth - 2f);
        if (caret < scroll) scroll = caret;
        if (caret > scroll + visibleWidth) scroll = caret - visibleWidth;
        scroll = Math.max(0f, Math.min(scroll, Math.max(0f, MusicUi.width(value, fontSize) - visibleWidth)));
    }

    private float prefixWidth(int count) {
        return MusicUi.width(value.substring(0, offset(count)), fontSize);
    }

    private int length() {
        return value.codePointCount(0, value.length());
    }

    private int offset(int count) {
        return value.offsetByCodePoints(0, count);
    }

    private boolean hasSelection() {
        return anchor >= 0 && anchor != cursor;
    }

    private int selectionStart() {
        return anchor < 0 ? cursor : Math.min(anchor, cursor);
    }

    private int selectionEnd() {
        return anchor < 0 ? cursor : Math.max(anchor, cursor);
    }

    private void resetCaret() {
        caretStartedAt = System.nanoTime();
    }

    private static boolean wordCharacter(int point) {
        return Character.isLetterOrDigit(point) || point == '_';
    }

    private static String clean(String input, int maximum) {
        if (input == null || input.isEmpty() || maximum <= 0) return "";
        StringBuilder result = new StringBuilder();
        int count = 0;
        for (int index = 0; index < input.length() && count < maximum;) {
            int point = input.codePointAt(index);
            index += Character.charCount(point);
            if (point >= Character.MIN_SURROGATE && point <= Character.MAX_SURROGATE) continue;
            if (point == '\n' || point == '\r' || point == '\t') point = ' ';
            else if (Character.isISOControl(point)) continue;
            result.appendCodePoint(point);
            count++;
        }
        return result.toString();
    }
}
