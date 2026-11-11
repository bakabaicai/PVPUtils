package com.pvp_utils.client.gui.clickgui.widget;

import com.pvp_utils.client.gui.clickgui.theme.ClickGuiThemeColors;
import com.pvp_utils.client.render.skia.SkijaUi;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.types.Rect;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Supplier;

public final class SettingLabel extends SettingWidget {
    private static final float TEXT_SIZE = 10f;
    private static final float LINE_HEIGHT = 14f;
    private final Supplier<String> text;
    private final float width;
    private String cachedText;
    private long cachedFontRevision = -1;
    private String[] lines = new String[0];

    public SettingLabel(Supplier<String> text) {
        this(text, 150f);
    }

    public SettingLabel(Supplier<String> text, float width) {
        if (!Float.isFinite(width) || width <= 0f) {
            throw new IllegalArgumentException("Label width must be finite and positive");
        }
        this.text = Objects.requireNonNull(text);
        this.width = width;
    }

    @Override
    public float getWidth() {
        return width;
    }

    @Override
    public float getHeight() {
        refreshLayout();
        return Math.max(24f, lines.length * LINE_HEIGHT);
    }

    @Override
    public void draw(Canvas canvas, float x, float y, float alpha) {
        if (alpha <= 0f) return;
        refreshLayout();
        if (cachedText.isEmpty()) return;
        float height = Math.max(24f, lines.length * LINE_HEIGHT);
        float top = y + (height - lines.length * LINE_HEIGHT) * 0.5f;
        int color = withAlpha(ClickGuiThemeColors.current().secondaryText, alpha);
        int save = canvas.save();
        try {
            canvas.clipRect(Rect.makeXYWH(x, y, width, height));
            for (int index = 0; index < lines.length; index++) {
                SkijaUi.text(canvas, lines[index], x, top + index * LINE_HEIGHT,
                        LINE_HEIGHT, color, TEXT_SIZE);
            }
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void refreshLayout() {
        String value = text.get();
        if (value == null) value = "";
        long revision = SkijaUi.textFontRevision();
        if (value.equals(cachedText) && revision == cachedFontRevision) return;
        ArrayList<String> wrapped = new ArrayList<>();
        for (String paragraph : value.split("\\r\\n|\\r|\\n", -1)) {
            if (paragraph.isEmpty()) {
                wrapped.add("");
                continue;
            }
            int start = 0;
            while (start < paragraph.length()) {
                int end = start;
                while (end < paragraph.length()) {
                    int next = paragraph.offsetByCodePoints(end, 1);
                    if (end > start && SkijaUi.textWidth(paragraph.substring(start, next), TEXT_SIZE) > width) break;
                    end = next;
                }
                wrapped.add(paragraph.substring(start, end));
                start = end;
            }
        }
        cachedText = value;
        cachedFontRevision = revision;
        lines = wrapped.toArray(String[]::new);
    }
}
