package com.pvp_utils.client.onboarding;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.UiText;
import com.pvp_utils.client.gui.clickgui.theme.AccentColors;
import com.pvp_utils.client.gui.clickgui.theme.UiTheme;
import com.pvp_utils.client.render.chroma.ChromaDraw;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.PaintStrokeCap;
import io.github.humbleui.skija.Path;
import io.github.humbleui.skija.PathBuilder;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OnboardingChoices implements AutoCloseable {
    public enum Mode {
        LANGUAGE, THEME, ACCENT
    }

    private static final int TEXT_CAPACITY = 128;
    private static final int PAGE_SIZE = 12;
    private static final UiTheme[] THEMES = UiTheme.values();
    private static final AccentColors.Preset[] PRESETS = AccentColors.Preset.values();
    private static final int PAGE_COUNT = (PRESETS.length + PAGE_SIZE - 1) / PAGE_SIZE;
    private static final int[] RAINBOW = {
            0xFFFF5050, 0xFFFFCF50, 0xFF72DE70, 0xFF50C8FF, 0xFF8D76FF, 0xFFF07DCF
    };

    private final Map<TextKey, ChromaDraw.PreparedText> textCache = new LinkedHashMap<>(32, 0.75f, true);
    private final Map<LabelKey, String> fittedLabels = new LinkedHashMap<>(32, 0.75f, true);
    private final List<Hit> hits = new ArrayList<>(PAGE_SIZE + 2);
    private long fontRevision = -1L;
    private boolean cachedChinese;
    private boolean languageChosen;
    private boolean accentPageInitialized;
    private int accentPage;
    private Mode visibleMode;
    private Frame frame;

    public void draw(Canvas canvas, float x, float y, float width, float height, Mode mode,
                     float mouseX, float mouseY, double seconds) {
        hits.clear();
        frame = null;
        visibleMode = mode;
        if (canvas == null || mode == null || !Float.isFinite(x) || !Float.isFinite(y)
                || !Float.isFinite(width) || !Float.isFinite(height) || width <= 0f || height <= 0f) return;
        refreshTextCache();
        if (mode == Mode.ACCENT && !accentPageInitialized) {
            accentPage = AccentColors.current().ordinal() / PAGE_SIZE;
            accentPageInitialized = true;
        }
        Layout layout = layout(mode, width, height);
        float scale = Math.min(1f, Math.min(width / layout.width(), height / layout.height()));
        if (!Float.isFinite(scale) || scale <= 0f) return;
        frame = new Frame(x + (width - layout.width() * scale) * 0.5f,
                y + (height - layout.height() * scale) * 0.5f, scale);
        float localMouseX = (mouseX - frame.x()) / scale;
        float localMouseY = (mouseY - frame.y()) / scale;
        UiTheme theme = UiTheme.DARK;
        int accent = 0xFFF1F2F4;
        int secondary = accent;
        int save = canvas.save();
        try {
            canvas.clipRect(Rect.makeXYWH(x, y, width, height));
            canvas.translate(frame.x(), frame.y());
            canvas.scale(scale, scale);
            switch (mode) {
                case LANGUAGE -> drawLanguages(canvas, layout, localMouseX, localMouseY, theme, accent);
                case THEME -> drawThemes(canvas, layout, localMouseX, localMouseY, theme, accent);
                case ACCENT -> drawAccents(canvas, layout, localMouseX, localMouseY,
                        theme, accent, secondary, seconds);
            }
        } finally {
            canvas.restoreToCount(save);
        }
    }

    public boolean click(float mouseX, float mouseY) {
        if (!Float.isFinite(mouseX) || !Float.isFinite(mouseY)) return false;
        for (Hit hit : hits) {
            if (!hit.contains(mouseX, mouseY)) continue;
            switch (hit.kind()) {
                case LANGUAGE -> {
                    Config.isChinese = hit.value() == 1;
                    Config.save();
                    languageChosen = true;
                    return true;
                }
                case THEME -> {
                    THEMES[hit.value()].select();
                    return true;
                }
                case ACCENT -> {
                    AccentColors.select(PRESETS[hit.value()]);
                    return true;
                }
                case PREVIOUS -> turnPage(-1);
                case NEXT -> turnPage(1);
            }
            return false;
        }
        return false;
    }

    public void scroll(double amount) {
        if (visibleMode != Mode.ACCENT || !Double.isFinite(amount) || amount == 0d) return;
        turnPage(amount > 0d ? -1 : 1);
    }

    public boolean languageChosen() {
        return languageChosen;
    }

    private Layout layout(Mode mode, float width, float height) {
        float cardWidth = switch (mode) {
            case LANGUAGE -> 216f;
            case THEME -> 142f;
            case ACCENT -> 113f;
        };
        float cardHeight = switch (mode) {
            case LANGUAGE -> 184f;
            case THEME -> 106f;
            case ACCENT -> 44f;
        };
        float gap = mode == Mode.LANGUAGE ? 12f : 8f;
        int count = switch (mode) {
            case LANGUAGE -> 2;
            case THEME -> THEMES.length;
            case ACCENT -> PAGE_SIZE;
        };
        int maximumColumns = mode == Mode.LANGUAGE ? 2 : 4;
        int minimumColumns = mode == Mode.LANGUAGE ? 1 : 2;
        Layout best = null;
        float bestScale = -1f;
        for (int columns = maximumColumns; columns >= minimumColumns; columns--) {
            int rows = (count + columns - 1) / columns;
            float contentWidth = cardWidth * columns + gap * (columns - 1);
            float contentHeight = cardHeight * rows + gap * (rows - 1);
            if (mode == Mode.ACCENT) contentHeight += 100f;
            float scale = Math.min(1f, Math.min(width / contentWidth, height / contentHeight));
            if (scale > bestScale) {
                best = new Layout(contentWidth, contentHeight, columns, cardWidth, cardHeight, gap);
                bestScale = scale;
            }
        }
        return best;
    }

    private void drawLanguages(Canvas canvas, Layout layout, float mouseX, float mouseY,
                               UiTheme theme, int accent) {
        for (int i = 0; i < 2; i++) {
            float x = i % layout.columns() * (layout.cardWidth() + layout.gap());
            float y = i / layout.columns() * (layout.cardHeight() + layout.gap());
            boolean chinese = i == 0;
            boolean selected = languageChosen && Config.isChinese == chinese;
            drawCard(canvas, x, y, layout.cardWidth(), layout.cardHeight(), 14f,
                    selected, contains(mouseX, mouseY, x, y, layout.cardWidth(), layout.cardHeight(), 14f),
                    theme, accent);
            float flagWidth = chinese ? 96f : 128f;
            float flagX = x + (layout.cardWidth() - flagWidth) * 0.5f;
            drawFlag(canvas, flagX, y + 24f, flagWidth, 64f, chinese);
            drawLabel(canvas, chinese ? "简体中文" : "English", x + 16f, y + 108f,
                    layout.cardWidth() - 32f, 16f, true, theme.primaryText(), true);
            drawLabel(canvas, chinese ? UiText.t("中国", "China") : UiText.t("英国", "United Kingdom"),
                    x + 16f, y + 139f, layout.cardWidth() - 32f, 10f, false, theme.secondaryText(), true);
            if (selected) drawCheck(canvas, x + layout.cardWidth() - 28f, y + 12f, 16f, accent);
            addHit(x, y, layout.cardWidth(), layout.cardHeight(), 14f, Kind.LANGUAGE, chinese ? 1 : 0);
        }
    }

    private void drawThemes(Canvas canvas, Layout layout, float mouseX, float mouseY,
                            UiTheme current, int accent) {
        for (int i = 0; i < THEMES.length; i++) {
            int row = i / layout.columns();
            int rowCount = Math.min(layout.columns(), THEMES.length - row * layout.columns());
            float rowWidth = rowCount * layout.cardWidth() + (rowCount - 1) * layout.gap();
            float x = (layout.width() - rowWidth) * 0.5f
                    + i % layout.columns() * (layout.cardWidth() + layout.gap());
            float y = row * (layout.cardHeight() + layout.gap());
            UiTheme theme = THEMES[i];
            boolean selected = theme == UiTheme.current();
            drawCard(canvas, x, y, layout.cardWidth(), layout.cardHeight(), 10f,
                    selected, contains(mouseX, mouseY, x, y, layout.cardWidth(), layout.cardHeight(), 10f),
                    current, accent);
            drawMiniature(canvas, x + 7f, y + 7f, layout.cardWidth() - 14f, 68f, theme, accent);
            drawLabel(canvas, theme.label(), x + 10f, y + 84f,
                    layout.cardWidth() - 39f, 11f, false, current.primaryText(), false);
            if (selected) drawCheck(canvas, x + layout.cardWidth() - 26f, y + 82f, 16f, accent);
            addHit(x, y, layout.cardWidth(), layout.cardHeight(), 10f, Kind.THEME, i);
        }
    }

    private void drawAccents(Canvas canvas, Layout layout, float mouseX, float mouseY,
                             UiTheme theme, int accent, int secondary, double seconds) {
        drawAccentPreview(canvas, layout.width(), theme, AccentColors.accent(), AccentColors.secondary(), seconds);
        int first = accentPage * PAGE_SIZE;
        int last = Math.min(PRESETS.length, first + PAGE_SIZE);
        for (int i = first; i < last; i++) {
            int item = i - first;
            float x = item % layout.columns() * (layout.cardWidth() + layout.gap());
            float y = 66f + item / layout.columns() * (layout.cardHeight() + layout.gap());
            AccentColors.Preset preset = PRESETS[i];
            boolean selected = !Config.accentCustomColors && preset == AccentColors.current();
            drawCard(canvas, x, y, layout.cardWidth(), layout.cardHeight(), 8f,
                    selected, contains(mouseX, mouseY, x, y, layout.cardWidth(), layout.cardHeight(), 8f),
                    theme, accent);
            drawSwatch(canvas, preset, x + 7f, y + 7f, layout.cardWidth() - 14f, 11f);
            drawLabel(canvas, preset.label, x + 8f, y + 25f,
                    layout.cardWidth() - (selected ? 29f : 16f), 9f, false, theme.primaryText(), false);
            if (selected) drawCheck(canvas, x + layout.cardWidth() - 20f, y + 24f, 12f, accent);
            addHit(x, y, layout.cardWidth(), layout.cardHeight(), 8f, Kind.ACCENT, i);
        }
        float navigationY = layout.height() - 24f;
        float center = layout.width() * 0.5f;
        drawPageButton(canvas, center - 102f, navigationY, false, accentPage > 0,
                mouseX, mouseY, theme, accent);
        drawPageButton(canvas, center + 26f, navigationY, true, accentPage + 1 < PAGE_COUNT,
                mouseX, mouseY, theme, accent);
        drawLabel(canvas, (accentPage + 1) + " / " + PAGE_COUNT, center - 26f, navigationY + 5f,
                52f, 10f, false, theme.secondaryText(), true);
    }

    private void drawAccentPreview(Canvas canvas, float width, UiTheme theme,
                                   int accent, int secondary, double seconds) {
        ChromaDraw.rounded(canvas, 0f, 0f, width, 56f, 10f, theme.panel());
        float hudWidth = width >= 355f ? 180f : 126f;
        float hudX = width - hudWidth - 8f;
        float labelWidth = hudX - 24f;
        drawLabel(canvas, UiText.t("当前强调色", "Current accent"), 12f, 9f,
                labelWidth, 9f, false, theme.secondaryText(), false);
        String name = Config.accentCustomColors ? UiText.t("自定义", "Custom") : AccentColors.current().label;
        drawLabel(canvas, name, 12f, 24f, labelWidth, 11f, true, theme.primaryText(), false);
        ChromaDraw.gradient(canvas, 12f, 43f, Math.min(108f, labelWidth), 4f,
                accent, secondary, false, 2f);
        ChromaDraw.rounded(canvas, hudX, 7f, hudWidth, 42f, 8f, theme.background());
        ChromaDraw.gradient(canvas, hudX + 8f, 11f, 3f, 34f, accent, secondary, true, 1.5f);
        float hudLabelWidth = hudWidth == 180f ? 105f : hudWidth - 28f;
        drawLabel(canvas, "PVPUtils", hudX + 18f, 13f, hudLabelWidth, 11f, true,
                UiTheme.readableText(accent, theme.background()), false);
        drawLabel(canvas, "144 FPS · 8 ms", hudX + 18f, 30f, hudLabelWidth,
                8f, false, theme.secondaryText(), false);
        float phase = Double.isFinite(seconds) ? (float) (Math.sin(seconds * 1.4d) * 0.5d + 0.5d) : 0.5f;
        for (int i = 0; hudWidth == 180f && i < 3; i++) {
            float barX = hudX + 139f + i * 9f;
            float barHeight = 11f + i * 5f + phase * 4f;
            ChromaDraw.rounded(canvas, barX, 37f - barHeight, 5f, barHeight,
                    2.5f, AccentColors.mix(accent, secondary, i * 0.5f));
        }
    }

    private void drawPageButton(Canvas canvas, float x, float y, boolean next, boolean enabled,
                                float mouseX, float mouseY, UiTheme theme, int accent) {
        boolean hovered = enabled && contains(mouseX, mouseY, x, y, 76f, 24f, 6f);
        ChromaDraw.rounded(canvas, x, y, 76f, 24f, 6f,
                hovered ? AccentColors.mix(theme.panel(), accent, 0.13f) : theme.panel());
        int color = enabled ? theme.primaryText() : AccentColors.withAlpha(theme.secondaryText(), 0.4f);
        float arrowX = x + (next ? 62f : 14f);
        float direction = next ? 1f : -1f;
        ChromaDraw.line(canvas, arrowX - direction * 2f, y + 8f,
                arrowX + direction * 2f, y + 12f, 1.3f, color, PaintStrokeCap.ROUND);
        ChromaDraw.line(canvas, arrowX + direction * 2f, y + 12f,
                arrowX - direction * 2f, y + 16f, 1.3f, color, PaintStrokeCap.ROUND);
        drawLabel(canvas, next ? UiText.t("下一页", "Next") : UiText.t("上一页", "Previous"),
                x + (next ? 7f : 24f),
                y + (24f - ChromaDraw.textMetrics(9f).getDescent() + ChromaDraw.textMetrics(9f).getAscent()) * 0.5f,
                45f, 9f, false, color, true);
        if (enabled) addHit(x, y, 76f, 24f, 6f, next ? Kind.NEXT : Kind.PREVIOUS, 0);
    }

    private void drawCard(Canvas canvas, float x, float y, float width, float height, float radius,
                          boolean selected, boolean hovered, UiTheme theme, int accent) {
        float tint = selected ? 0.13f : hovered ? 0.07f : 0f;
        ChromaDraw.rounded(canvas, x, y, width, height, radius,
                AccentColors.mix(theme.panel(), accent, tint));
        if (selected) {
            ChromaDraw.rounded(canvas, x + 12f, y + height - 3f, width - 24f, 3f, 1.5f, accent);
        }
    }

    private void drawMiniature(Canvas canvas, float x, float y, float width, float height,
                               UiTheme theme, int accent) {
        int save = canvas.save();
        try {
            canvas.clipRRect(RRect.makeXYWH(x, y, width, height, 7f), true);
            float sidebar = 29f;
            ChromaDraw.fill(canvas, x, y, width, height, theme.background());
            ChromaDraw.fill(canvas, x, y, sidebar, height, theme.panel());
            ChromaDraw.rounded(canvas, x + 7f, y + 8f, 15f, 4f, 2f,
                    AccentColors.withAlpha(theme.primaryText(), 0.65f));
            for (int i = 0; i < 3; i++) {
                ChromaDraw.rounded(canvas, x + 5f, y + 23f + i * 12f, 19f, 7f, 3f,
                        i == 0 ? accent : AccentColors.withAlpha(theme.secondaryText(), 0.16f));
            }
            float contentX = x + sidebar + 6f;
            float contentWidth = width - sidebar - 12f;
            for (int i = 0; i < 2; i++) {
                float rowY = y + 8f + i * 29f;
                ChromaDraw.rounded(canvas, contentX, rowY, contentWidth, 24f, 5f, theme.panel());
                ChromaDraw.rounded(canvas, contentX + 6f, rowY + 6f, contentWidth * 0.4f, 3f, 1.5f,
                        AccentColors.withAlpha(theme.primaryText(), 0.72f));
                ChromaDraw.rounded(canvas, contentX + 6f, rowY + 14f, contentWidth * 0.48f, 3f, 1.5f,
                        AccentColors.withAlpha(theme.secondaryText(), 0.3f));
                float toggleX = contentX + contentWidth - 22f;
                ChromaDraw.rounded(canvas, toggleX, rowY + 7f, 16f, 10f, 5f,
                        i == 0 ? accent : AccentColors.withAlpha(theme.secondaryText(), 0.22f));
                ChromaDraw.rounded(canvas, toggleX + (i == 0 ? 7f : 1.5f), rowY + 8.5f,
                        7f, 7f, 3.5f, UiTheme.readableText(0xFFFFFFFF, i == 0 ? accent : theme.panel()));
            }
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void drawSwatch(Canvas canvas, AccentColors.Preset preset, float x, float y,
                            float width, float height) {
        int[] colors = preset == AccentColors.Preset.TEST ? RAINBOW : preset.colors;
        int save = canvas.save();
        try {
            canvas.clipRRect(RRect.makeXYWH(x, y, width, height, 4f), true);
            int segments = Math.max(1, colors.length - 1);
            float segmentWidth = width / segments;
            for (int i = 0; i < segments; i++) {
                ChromaDraw.gradient(canvas, x + i * segmentWidth, y, segmentWidth + 0.25f, height,
                        colors[i], colors[Math.min(i + 1, colors.length - 1)], false, 0f);
            }
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void drawFlag(Canvas canvas, float x, float y, float width, float height, boolean chinese) {
        int save = canvas.save();
        try {
            canvas.clipRRect(RRect.makeXYWH(x, y, width, height, 6f), true);
            if (chinese) {
                ChromaDraw.fill(canvas, x, y, width, height, 0xFFDE2910);
                float unit = width / 30f;
                float largeX = x + unit * 5f;
                float largeY = y + unit * 5f;
                drawStar(canvas, largeX, largeY, unit * 3f, -Math.PI * 0.5d);
                float[][] stars = {{10f, 2f}, {12f, 4f}, {12f, 7f}, {10f, 9f}};
                for (float[] star : stars) {
                    float starX = x + star[0] * unit;
                    float starY = y + star[1] * unit;
                    drawStar(canvas, starX, starY, unit, Math.atan2(largeY - starY, largeX - starX));
                }
            } else {
                ChromaDraw.fill(canvas, x, y, width, height, 0xFF012169);
                ChromaDraw.line(canvas, x, y, x + width, y + height, height * 0.2f, 0xFFFFFFFF);
                ChromaDraw.line(canvas, x + width, y, x, y + height, height * 0.2f, 0xFFFFFFFF);
                ChromaDraw.line(canvas, x, y - 2f, x + width, y + height - 2f,
                        height * 0.075f, 0xFFC8102E);
                ChromaDraw.line(canvas, x + width, y + 2f, x, y + height + 2f,
                        height * 0.075f, 0xFFC8102E);
                ChromaDraw.fill(canvas, x, y + height * 0.34f, width, height * 0.32f, 0xFFFFFFFF);
                ChromaDraw.fill(canvas, x + width * 0.42f, y, width * 0.16f, height, 0xFFFFFFFF);
                ChromaDraw.fill(canvas, x, y + height * 0.4f, width, height * 0.2f, 0xFFC8102E);
                ChromaDraw.fill(canvas, x + width * 0.45f, y, width * 0.1f, height, 0xFFC8102E);
            }
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void drawStar(Canvas canvas, float x, float y, float radius, double rotation) {
        try (PathBuilder builder = new PathBuilder()) {
            for (int i = 0; i < 10; i++) {
                double angle = rotation + i * Math.PI / 5d;
                float length = i % 2 == 0 ? radius : radius * 0.381966f;
                float pointX = x + (float) Math.cos(angle) * length;
                float pointY = y + (float) Math.sin(angle) * length;
                if (i == 0) builder.moveTo(pointX, pointY);
                else builder.lineTo(pointX, pointY);
            }
            builder.closePath();
            try (Path path = builder.detach()) {
                ChromaDraw.fillPath(canvas, path, 0xFFFFDE00);
            }
        }
    }

    private void drawCheck(Canvas canvas, float x, float y, float size, int accent) {
        ChromaDraw.rounded(canvas, x, y, size, size, size * 0.5f, accent);
        int color = UiTheme.readableText(0xFFFFFFFF, accent);
        ChromaDraw.line(canvas, x + size * 0.27f, y + size * 0.52f,
                x + size * 0.44f, y + size * 0.68f, size * 0.11f, color, PaintStrokeCap.ROUND);
        ChromaDraw.line(canvas, x + size * 0.44f, y + size * 0.68f,
                x + size * 0.75f, y + size * 0.34f, size * 0.11f, color, PaintStrokeCap.ROUND);
    }

    private void drawLabel(Canvas canvas, String value, float x, float y, float width,
                           float size, boolean bold, int color, boolean centered) {
        if (width <= 0f) return;
        String fitted = fitLabel(value, width, size, bold);
        ChromaDraw.PreparedText text = text(fitted, size, bold);
        text.drawTop(canvas, centered ? x + (width - text.width()) * 0.5f : x, y, color);
    }

    private String fitLabel(String value, float width, float size, boolean bold) {
        ChromaDraw.PreparedText complete = text(value, size, bold);
        if (complete.width() <= width) return value;
        LabelKey key = new LabelKey(value, width, size, bold);
        String cached = fittedLabels.get(key);
        if (cached != null) return cached;
        String suffix = "…";
        String result = "";
        if (text(suffix, size, bold).width() <= width) {
            int lower = 0;
            int upper = value.codePointCount(0, value.length());
            while (lower < upper) {
                int length = (lower + upper + 1) / 2;
                String candidate = value.substring(0, value.offsetByCodePoints(0, length)) + suffix;
                try (ChromaDraw.PreparedText prepared = ChromaDraw.prepareText(candidate, size, bold)) {
                    if (prepared.width() <= width) lower = length;
                    else upper = length - 1;
                }
            }
            result = value.substring(0, value.offsetByCodePoints(0, lower)) + suffix;
        }
        if (fittedLabels.size() >= TEXT_CAPACITY) {
            fittedLabels.remove(fittedLabels.keySet().iterator().next());
        }
        fittedLabels.put(key, result);
        return result;
    }

    private ChromaDraw.PreparedText text(String value, float size, boolean bold) {
        TextKey key = new TextKey(value, size, bold);
        ChromaDraw.PreparedText cached = textCache.get(key);
        if (cached != null) return cached;
        ChromaDraw.PreparedText prepared = ChromaDraw.prepareText(value, size, bold);
        if (textCache.size() >= TEXT_CAPACITY) {
            TextKey oldest = textCache.keySet().iterator().next();
            textCache.remove(oldest).close();
        }
        textCache.put(key, prepared);
        return prepared;
    }

    private void refreshTextCache() {
        long revision = ChromaDraw.textFontRevision();
        if (fontRevision == revision && cachedChinese == Config.isChinese) return;
        clearTextCache();
        fontRevision = revision;
        cachedChinese = Config.isChinese;
    }

    private void clearTextCache() {
        for (ChromaDraw.PreparedText text : textCache.values()) text.close();
        textCache.clear();
        fittedLabels.clear();
    }

    private void turnPage(int amount) {
        accentPage = Math.clamp(accentPage + amount, 0, PAGE_COUNT - 1);
    }

    private void addHit(float x, float y, float width, float height, float radius, Kind kind, int value) {
        hits.add(new Hit(frame.x() + x * frame.scale(), frame.y() + y * frame.scale(),
                width * frame.scale(), height * frame.scale(), radius * frame.scale(), kind, value));
    }

    private static boolean contains(float mouseX, float mouseY, float x, float y,
                                    float width, float height, float radius) {
        if (mouseX < x || mouseY < y || mouseX > x + width || mouseY > y + height) return false;
        float closestX = Math.clamp(mouseX, x + radius, x + width - radius);
        float closestY = Math.clamp(mouseY, y + radius, y + height - radius);
        float dx = mouseX - closestX;
        float dy = mouseY - closestY;
        return dx * dx + dy * dy <= radius * radius;
    }

    @Override
    public void close() {
        clearTextCache();
        hits.clear();
        frame = null;
        visibleMode = null;
        fontRevision = -1L;
    }

    private enum Kind {
        LANGUAGE, THEME, ACCENT, PREVIOUS, NEXT
    }

    private record Layout(float width, float height, int columns, float cardWidth, float cardHeight, float gap) {
    }

    private record Frame(float x, float y, float scale) {
    }

    private record Hit(float x, float y, float width, float height, float radius, Kind kind, int value) {
        private boolean contains(float mouseX, float mouseY) {
            return OnboardingChoices.contains(mouseX, mouseY, x, y, width, height, radius);
        }
    }

    private record TextKey(String value, float size, boolean bold) {
    }

    private record LabelKey(String value, float width, float size, boolean bold) {
    }
}
