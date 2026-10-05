package com.pvp_utils.client.render.skia;

import io.github.humbleui.types.RRect;

import com.pvp_utils.PVPUtils;
import net.minecraft.client.Minecraft;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.BlendMode;
import io.github.humbleui.skija.Data;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontEdging;
import io.github.humbleui.skija.FontHinting;
import io.github.humbleui.skija.FontMetrics;
import io.github.humbleui.skija.FontMgr;
import io.github.humbleui.skija.FontStyle;
import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.ImageFilter;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.Shader;
import io.github.humbleui.skija.Typeface;
import io.github.humbleui.types.Rect;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class SkijaUi {
    public static final String CLIENT_FONT = "harmony";
    private static final float FONT_SIZE = 9.0F;
    private static final long MAX_IMPORTED_FONT_BYTES = 64L * 1024L * 1024L;
    private static final String[] FONT_FAMILIES = {
        "Microsoft YaHei UI",
        "Microsoft YaHei",
            "Segoe UI"
    };

    private static final Paint SHAPE_PAINT = new Paint().setAntiAlias(false);
    private static final Paint GRADIENT_PAINT = new Paint().setAntiAlias(true).setDither(true);
    private static final Paint TEXT_PAINT = new Paint().setAntiAlias(true);

    private static final Typeface REGULAR_TYPEFACE = loadTypeface("/fonts/harmony.ttf", FontStyle.NORMAL);
    private static final Typeface BOLD_TYPEFACE = loadTypeface("/fonts/harmony_bold.ttf", FontStyle.BOLD);
    private static final Typeface TEXT_FALLBACK_TYPEFACE = findTypeface(FontStyle.NORMAL);
    private static final Typeface PVP_CONTROLS_TYPEFACE = loadTypeface("/fonts/icon.ttf", FontStyle.NORMAL);
    private static final Typeface MATERIAL_SYMBOLS_TYPEFACE = loadTypeface("/fonts/MaterialSymbolsRounded.ttf", FontStyle.NORMAL);
    private static final Map<Integer, Font> PVP_CONTROL_FONTS = new HashMap<>();
    private static final Map<Integer, Font> MATERIAL_SYMBOL_FONTS = new HashMap<>();
    private static final Map<FontKey, Font> TEXT_FONTS = new HashMap<>();
    private static final Map<Integer, Font> TEXT_FALLBACK_FONTS = new HashMap<>();
    private static final Map<String, Typeface> IMPORTED_TYPEFACES = new LinkedHashMap<>();
    private static final Map<String, Path> IMPORTED_FILES = new LinkedHashMap<>();
    private static final List<Font> RETIRED_TEXT_FONTS = new ArrayList<>();
    private static final List<Typeface> RETIRED_IMPORTED_TYPEFACES = new ArrayList<>();
    private static final ThreadLocal<String> FONT_OVERRIDE = new ThreadLocal<>();
    private static String activeFont = CLIENT_FONT;
    private static boolean importedFontsLoaded;

    private SkijaUi() {
    }

    public static void fill(Canvas canvas, float x, float y, float width, float height, int color) {
        SHAPE_PAINT.setAntiAlias(false).setColor(color);
        canvas.drawRect(Rect.makeXYWH(x, y, width, height), SHAPE_PAINT);
    }

    public static void rounded(Canvas canvas, float x, float y, float width, float height, float radius, int color) {
        SHAPE_PAINT.setAntiAlias(true).setColor(color);
        canvas.drawRRect(RRect.makeXYWH(x, y, width, height, radius), SHAPE_PAINT);
    }

    public static void gradient(
    Canvas canvas,
    float x,
    float y,
    float width,
    float height,
    int startColor,
    int endColor,
    boolean vertical,
    float radius) {
        if (width <= 0.0F || height <= 0.0F) return;
        float x1 = vertical ? x : x + width;
        float y1 = vertical ? y + height : y;
        try (Shader shader = Shader.makeLinearGradient(
        x, y, x1, y1, new int[]{startColor, endColor})) {
            GRADIENT_PAINT.setShader(shader).setColor(0xFFFFFFFF).setAlpha(255);
            if (radius > 0.0F) {
                canvas.drawRRect(RRect.makeXYWH(x, y, width, height, radius), GRADIENT_PAINT);
            } else {
                canvas.drawRect(Rect.makeXYWH(x, y, width, height), GRADIENT_PAINT);
            }
        } finally {
            GRADIENT_PAINT.setShader(null).setAlpha(255);
        }
    }

    public static void gradientDiagonal(
    Canvas canvas,
    float x,
    float y,
    float width,
    float height,
    int startColor,
    int endColor,
    float radius) {
        if (width <= 0.0F || height <= 0.0F) return;
        try (Shader shader = Shader.makeLinearGradient(
        x, y, x + width, y + height, new int[]{startColor, endColor})) {
            GRADIENT_PAINT.setShader(shader).setColor(0xFFFFFFFF).setAlpha(255);
            if (radius > 0.0F) {
                canvas.drawRRect(RRect.makeXYWH(x, y, width, height, radius), GRADIENT_PAINT);
            } else {
                canvas.drawRect(Rect.makeXYWH(x, y, width, height), GRADIENT_PAINT);
            }
        } finally {
            GRADIENT_PAINT.setShader(null).setAlpha(255);
        }
    }

    public static void line(Canvas canvas, float x1, float y1, float x2, float y2,
    float thickness, int color) {
        try {
            SHAPE_PAINT.setAntiAlias(true).setMode(PaintMode.STROKE)
            .setStrokeWidth(Math.max(0.1F, thickness)).setColor(color);
            canvas.drawLine(x1, y1, x2, y2, SHAPE_PAINT);
        } finally {
            SHAPE_PAINT.setMode(PaintMode.FILL).setStrokeWidth(1.0F);
        }
    }

    public static void outline(Canvas canvas, float x, float y, float width, float height,
    float radius, float thickness, int color) {
        try {
            SHAPE_PAINT.setAntiAlias(true).setMode(PaintMode.STROKE)
            .setStrokeWidth(Math.max(0.1F, thickness)).setColor(color);
            if (radius > 0.0F) {
                canvas.drawRRect(RRect.makeXYWH(x, y, width, height, radius), SHAPE_PAINT);
            } else {
                canvas.drawRect(Rect.makeXYWH(x, y, width, height), SHAPE_PAINT);
            }
        } finally {
            SHAPE_PAINT.setMode(PaintMode.FILL).setStrokeWidth(1.0F);
        }
    }

    public static void fillPath(Canvas canvas, io.github.humbleui.skija.Path path, int color) {
        SHAPE_PAINT.setAntiAlias(true).setColor(color);
        canvas.drawPath(path, SHAPE_PAINT);
    }

    public static void dropShadowPath(Canvas canvas, io.github.humbleui.skija.Path path,
    float blur, int color) {
        if (path == null || ((color >>> 24) & 0xFF) == 0) return;
        float sigma = Math.max(0.1F, blur * 0.5F);
        try (ImageFilter filter = ImageFilter.makeDropShadowOnly(
        0.0F, 0.0F, sigma, sigma, color)) {
            SHAPE_PAINT.setAntiAlias(true).setImageFilter(filter)
            .setColor(0xFFFFFFFF).setAlpha(255);
            canvas.drawPath(path, SHAPE_PAINT);
        } finally {
            SHAPE_PAINT.setImageFilter(null).setAlpha(255);
        }
    }

    public static void dropShadowRounded(Canvas canvas, float x, float y, float width, float height,
    float radius, float offsetY, float blur, int color) {
        if (width <= 0.0F || height <= 0.0F || ((color >>> 24) & 0xFF) == 0) return;
        float sigma = Math.max(0.1F, blur * 0.5F);
        try (ImageFilter filter = ImageFilter.makeDropShadowOnly(
        0.0F, offsetY, sigma, sigma, color)) {
            SHAPE_PAINT.setAntiAlias(true).setImageFilter(filter)
            .setColor(0xFFFFFFFF).setAlpha(255);
            canvas.drawRRect(RRect.makeXYWH(x, y, width, height, radius), SHAPE_PAINT);
        } finally {
            SHAPE_PAINT.setImageFilter(null).setAlpha(255);
        }
    }

    public static void glowLayer(Canvas canvas, float x, float y, float width, float height,
    float blur, int strength, Runnable drawing) {
        if (canvas == null || drawing == null || width <= 0.0F || height <= 0.0F) return;
        float radius = Math.max(1.0F, blur);
        int level = Math.max(1, Math.min(10, strength));
        float padding = radius * 1.75F;
        Rect bounds = Rect.makeXYWH(x - padding, y - padding,
        width + padding * 2.0F, height + padding * 2.0F);
        int alpha = 70 + (level - 1) * 185 / 9;
        drawGlowPass(canvas, bounds, Math.max(0.8F, radius * 0.38F),
        alpha, BlendMode.PLUS, drawing);
    }

    private static void drawGlowPass(Canvas canvas, Rect bounds, float sigma,
    int alpha, BlendMode blendMode, Runnable drawing) {
        try (ImageFilter filter = ImageFilter.makeBlur(
        sigma, sigma, FilterTileMode.DECAL);
        Paint layerPaint = new Paint().setImageFilter(filter)
        .setBlendMode(blendMode).setAlpha(Math.max(0, Math.min(255, alpha)))) {
            int save = canvas.saveLayer(bounds, layerPaint);
            try {
                drawing.run();
            } finally {
                canvas.restoreToCount(save);
            }
        }
    }

    public static void text(Canvas canvas, String text, float x, float top, float height, int color) {
        drawText(canvas, text, x, top, height, color, false, FONT_SIZE);
    }

    public static void boldText(Canvas canvas, String text, float x, float top, float height, int color) {
        drawText(canvas, text, x, top, height, color, true, FONT_SIZE);
    }

    public static void text(Canvas canvas, String text, float x, float top, float height, int color, float size) {
        drawText(canvas, text, x, top, height, color, false, size);
    }

    public static void text(Canvas canvas, String text, float x, float top, float height, int color, float size,
    String fontName) {
        drawText(canvas, text, x, top, height, color, false, size, fontName);
    }

    public static void boldText(Canvas canvas, String text, float x, float top, float height, int color, float size) {
        drawText(canvas, text, x, top, height, color, true, size);
    }

    public static void textShadow(Canvas canvas, String text, float x, float top, float height, int color) {
        textShadow(canvas, text, x, top, height, color, FONT_SIZE);
    }

    public static void textShadow(Canvas canvas, String text, float x, float top, float height, int color,
    float size) {
        float offset = Math.max(0.6F, size / 9.0F);
        drawText(canvas, text, x + offset, top + offset, height, shadowColor(color), false, size);
        drawText(canvas, text, x, top, height, color, false, size);
    }

    public static void boldTextShadow(Canvas canvas, String text, float x, float top, float height, int color,
    float size) {
        float offset = Math.max(0.6F, size / 9.0F);
        drawText(canvas, text, x + offset, top + offset, height, shadowColor(color), true, size);
        drawText(canvas, text, x, top, height, color, true, size);
    }

    private static int shadowColor(int color) {
        int alpha = (color >>> 24) & 0xFF;
        int red = ((color >> 16) & 0xFF) >> 2;
        int green = ((color >> 8) & 0xFF) >> 2;
        int blue = (color & 0xFF) >> 2;
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public static float textWidth(String text) {
        return textWidth(text, FONT_SIZE);
    }

    public static float boldTextWidth(String text) {
        return boldTextWidth(text, FONT_SIZE);
    }

    public static float textWidth(String text, float size) {
        return textWidthWithFallback(text, size, effectiveFontName(), false);
    }

    public static float textWidthWithFallback(String text, float size) {
        return textWidthWithFallback(text, size, effectiveFontName());
    }

    public static float textWidthWithFallback(String text, float size, String fontName) {
        return textWidthWithFallback(text, size, fontName, false);
    }

    private static float textWidthWithFallback(String text, float size, String fontName,
    boolean bold) {
        String safeText = text == null ? "" : text;
        if (safeText.isEmpty()) return 0.0F;
        Font primary = font(bold, size, fontName);
        Font fallback = fallbackFont(size);
        if (!needsFallback(safeText, primary, fallback)) {
            return primary.measureTextWidth(safeText);
        }
        float width = 0.0F;
        for (TextRun run : fallbackRuns(safeText, primary, fallback)) {
            width += run.font().measureTextWidth(run.text());
        }
        return width;
    }

    public static float textWidth(String text, float size, String fontName) {
        return textWidthWithFallback(text, size, fontName, false);
    }

    public static float boldTextWidth(String text, float size) {
        return textWidthWithFallback(text, size, effectiveFontName(), true);
    }

    public static void textWithFallback(Canvas canvas, String text, float x, float top,
    float height, int color, float size) {
        textWithFallback(canvas, text, x, top, height, color, size, effectiveFontName());
    }

    public static void textWithFallback(Canvas canvas, String text, float x, float top,
    float height, int color, float size, String fontName) {
        drawTextWithFallback(canvas, text, x, top, height, color, size, fontName);
    }

    public static void textShadowWithFallback(Canvas canvas, String text, float x, float top,
    float height, int color, float size) {
        textShadowWithFallback(canvas, text, x, top, height, color, size, effectiveFontName());
    }

    public static void textShadowWithFallback(Canvas canvas, String text, float x, float top,
    float height, int color, float size, String fontName) {
        float offset = Math.max(0.6F, size / 9.0F);
        drawTextWithFallback(canvas, text, x + offset, top + offset, height,
        shadowColor(color), size, fontName);
        drawTextWithFallback(canvas, text, x, top, height, color, size, fontName);
    }

    public static void textShadow(Canvas canvas, String text, float x, float top, float height, int color,
    float size, String fontName) {
        float offset = Math.max(0.6F, size / 9.0F);
        drawText(canvas, text, x + offset, top + offset, height, shadowColor(color), false, size, fontName);
        drawText(canvas, text, x, top, height, color, false, size, fontName);
    }

    public static synchronized List<String> availableFontNames() {
        ensureImportedFontsLoaded();
        List<String> names = new ArrayList<>(IMPORTED_TYPEFACES.size() + 1);
        names.add(CLIENT_FONT);
        names.addAll(IMPORTED_TYPEFACES.keySet());
        return List.copyOf(names);
    }

    public static synchronized void selectFont(String fontName) {
        ensureImportedFontsLoaded();
        String resolved = resolveFontName(fontName);
        if (!activeFont.equals(resolved)) {
            activeFont = resolved;
        }
    }

    public static void withFont(String fontName, Runnable drawing) {
        String previous = FONT_OVERRIDE.get();
        FONT_OVERRIDE.set(fontName);
        try {
            drawing.run();
        } finally {
            if (previous == null) {
                FONT_OVERRIDE.remove();
            } else {
                FONT_OVERRIDE.set(previous);
            }
        }
    }

    public static synchronized Path fontDirectory() throws IOException {
        Path directory = Minecraft.getInstance().gameDirectory.toPath().resolve(PVPUtils.MOD_ID).resolve("fonts");
        Files.createDirectories(directory);
        return directory;
    }

    public static synchronized String importFont(Path source) throws IOException {
        if (source == null || !Files.isRegularFile(source) || !isFontFile(source)) {
            throw new IOException("Only .ttf, .otf and .ttc files can be imported");
        }
        long size = Files.size(source);
        if (size <= 0L || size > MAX_IMPORTED_FONT_BYTES) {
            throw new IOException("Font file has an invalid size");
        }

        Path target = fontDirectory().resolve(source.getFileName().toString());
        boolean sameFile = Files.exists(target) && Files.isSameFile(source, target);
        if (!sameFile) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
        reloadImportedFonts();
        for (Map.Entry<String, Path> entry : IMPORTED_FILES.entrySet()) {
            if (entry.getValue().equals(target.toAbsolutePath().normalize())) {
                return entry.getKey();
            }
        }
        if (!sameFile) Files.deleteIfExists(target);
        throw new IOException("Skija could not load this font");
    }

    public static synchronized void reloadImportedFonts() {
        retireTextFontResources();
        RETIRED_IMPORTED_TYPEFACES.addAll(IMPORTED_TYPEFACES.values());
        IMPORTED_TYPEFACES.clear();
        IMPORTED_FILES.clear();
        importedFontsLoaded = true;

        Path directory;
        try {
            directory = fontDirectory();
        } catch (IOException error) {
            PVPUtils.LOGGER.warn("Could not create the imported font directory", error);
            activeFont = CLIENT_FONT;
            return;
        }

        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile)
            .filter(SkijaUi::isFontFile)
            .sorted(Comparator.comparing(path -> path.getFileName().toString(),
            String.CASE_INSENSITIVE_ORDER))
            .forEach(SkijaUi::loadImportedTypeface);
        } catch (IOException error) {
            PVPUtils.LOGGER.warn("Could not inspect imported fonts in {}", directory, error);
        }
        activeFont = resolveFontName(activeFont);
    }

    public static void icon(Canvas canvas, String glyph, float x, float top, float height, int color,
    float size, IconSet iconSet) {
        Font font = iconFont(iconSet, size);
        FontMetrics metrics = font.getMetrics();
        float textHeight = metrics.getDescent() - metrics.getAscent();
        float baseline = top + (height - textHeight) * 0.5F - metrics.getAscent();
        TEXT_PAINT.setColor(color);
        canvas.drawString(glyph == null ? "" : glyph, x, baseline, font, TEXT_PAINT);
    }

    public static float iconWidth(String glyph, float size, IconSet iconSet) {
        return iconFont(iconSet, size).measureTextWidth(glyph == null ? "" : glyph);
    }

    public static synchronized void close() {
        PVP_CONTROL_FONTS.values().forEach(Font::close);
        MATERIAL_SYMBOL_FONTS.values().forEach(Font::close);
        PVP_CONTROL_FONTS.clear();
        MATERIAL_SYMBOL_FONTS.clear();
        if (PVP_CONTROLS_TYPEFACE != null) PVP_CONTROLS_TYPEFACE.close();
        if (MATERIAL_SYMBOLS_TYPEFACE != null) MATERIAL_SYMBOLS_TYPEFACE.close();
        closeTextFonts();
        TEXT_FALLBACK_FONTS.values().forEach(Font::close);
        TEXT_FALLBACK_FONTS.clear();
        releaseRetiredFontResources();
        IMPORTED_TYPEFACES.values().forEach(Typeface::close);
        IMPORTED_TYPEFACES.clear();
        IMPORTED_FILES.clear();
        if (REGULAR_TYPEFACE != null) {
            REGULAR_TYPEFACE.close();
        }
        if (BOLD_TYPEFACE != null) {
            BOLD_TYPEFACE.close();
        }
        if (TEXT_FALLBACK_TYPEFACE != null) {
            TEXT_FALLBACK_TYPEFACE.close();
        }
        GRADIENT_PAINT.close();
        SHAPE_PAINT.close();
        TEXT_PAINT.close();
    }

    private static void drawText(Canvas canvas, String text, float x, float top, float height, int color,
    boolean bold, float size) {
        drawText(canvas, text, x, top, height, color, bold, size, effectiveFontName());
    }

    private static void drawText(Canvas canvas, String text, float x, float top, float height, int color,
    boolean bold, float size, String fontName) {
        drawTextWithFallback(canvas, text, x, top, height, color, size, fontName, bold);
    }

    private static void drawTextWithFallback(Canvas canvas, String text, float x, float top,
    float height, int color, float size, String fontName) {
        drawTextWithFallback(canvas, text, x, top, height, color, size, fontName, false);
    }

    private static void drawTextWithFallback(Canvas canvas, String text, float x, float top,
    float height, int color, float size, String fontName,
    boolean bold) {
        String safeText = text == null ? "" : text;
        if (safeText.isEmpty()) return;
        Font primary = font(bold, size, fontName);
        Font fallback = fallbackFont(size);
        if (!needsFallback(safeText, primary, fallback)) {
            drawTextRun(canvas, safeText, x, top, height, color, primary);
            return;
        }
        float cursorX = x;
        for (TextRun run : fallbackRuns(safeText, primary, fallback)) {
            drawTextRun(canvas, run.text(), cursorX, top, height, color, run.font());
            cursorX += run.font().measureTextWidth(run.text());
        }
    }

    private static void drawTextRun(Canvas canvas, String text, float x, float top,
    float height, int color, Font font) {
        FontMetrics metrics = font.getMetrics();
        float textHeight = metrics.getDescent() - metrics.getAscent();
        float baseline = top + (height - textHeight) * 0.5F - metrics.getAscent();
        TEXT_PAINT.setColor(color);
        canvas.drawString(text, x, baseline, font, TEXT_PAINT);
    }

    private static List<TextRun> fallbackRuns(String safeText, Font primary, Font fallback) {
        List<TextRun> runs = new ArrayList<>();
        StringBuilder currentText = new StringBuilder();
        Font currentFont = null;
        for (int offset = 0; offset < safeText.length();) {
            int codePoint = safeText.codePointAt(offset);
            Font selected = selectFont(codePoint, primary, fallback);
            if (currentFont != null && currentFont != selected) {
                runs.add(new TextRun(currentText.toString(), currentFont));
                currentText.setLength(0);
            }
            currentFont = selected;
            currentText.appendCodePoint(codePoint);
            offset += Character.charCount(codePoint);
        }
        if (!currentText.isEmpty()) {
            runs.add(new TextRun(currentText.toString(), currentFont));
        }
        return runs;
    }

    private static boolean needsFallback(String text, Font primary, Font fallback) {
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            if (selectFont(codePoint, primary, fallback) != primary) return true;
            offset += Character.charCount(codePoint);
        }
        return false;
    }

    private static Font selectFont(int codePoint, Font primary, Font fallback) {
        if (!isCjkCodePoint(codePoint) && primary.getUTF32Glyph(codePoint) != 0) {
            return primary;
        }
        return fallback.getUTF32Glyph(codePoint) != 0 ? fallback : primary;
    }

    private static boolean isCjkCodePoint(int codePoint) {
        return codePoint >= 0x2E80 && codePoint <= 0xA4CF
        || codePoint >= 0xAC00 && codePoint <= 0xD7AF
        || codePoint >= 0xF900 && codePoint <= 0xFAFF
        || codePoint >= 0xFE30 && codePoint <= 0xFE4F
        || codePoint >= 0xFF00 && codePoint <= 0xFFEF
        || codePoint >= 0x20000 && codePoint <= 0x3134F;
    }

    private static Font font(boolean bold, float size) {
        return font(bold, size, effectiveFontName());
    }

    private static String effectiveFontName() {
        String override = FONT_OVERRIDE.get();
        return override == null ? activeFont : override;
    }

    private static synchronized Font font(boolean bold, float size, String fontName) {
        ensureImportedFontsLoaded();
        float safeSize = Math.max(1.0F, size);
        String resolved = resolveFontName(fontName == null ? activeFont : fontName);
        FontKey key = new FontKey(resolved, bold, Float.floatToIntBits(safeSize));
        return TEXT_FONTS.computeIfAbsent(key, ignored -> {
            Typeface imported = IMPORTED_TYPEFACES.get(resolved);
            Typeface typeface = imported != null ? imported : bold ? BOLD_TYPEFACE : REGULAR_TYPEFACE;
            return createFont(typeface, safeSize);
        });
    }

    private static void ensureImportedFontsLoaded() {
        if (!importedFontsLoaded) reloadImportedFonts();
    }

    private static String resolveFontName(String requested) {
        if (requested != null) {
            if (CLIENT_FONT.equalsIgnoreCase(requested)) return CLIENT_FONT;
            for (String name : IMPORTED_TYPEFACES.keySet()) {
                if (name.equalsIgnoreCase(requested)) return name;
            }
        }
        return CLIENT_FONT;
    }

    private static void loadImportedTypeface(Path file) {
        try (Data data = Data.makeFromBytes(Files.readAllBytes(file))) {
            Typeface typeface = FontMgr.getDefault().makeFromData(data);
            if (typeface == null) return;
            String name = displayFontName(file);
            if (IMPORTED_TYPEFACES.containsKey(name)) {
                name = file.getFileName().toString();
            }
            IMPORTED_TYPEFACES.put(name, typeface);
            IMPORTED_FILES.put(name, file.toAbsolutePath().normalize());
        } catch (Exception error) {
            PVPUtils.LOGGER.warn("Skipping invalid imported font {}", file, error);
        }
    }

    private static boolean isFontFile(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".ttf") || name.endsWith(".otf") || name.endsWith(".ttc");
    }

    private static String displayFontName(Path file) {
        String name = file.getFileName().toString();
        int extension = name.lastIndexOf('.');
        return extension > 0 ? name.substring(0, extension) : name;
    }

    private static void closeTextFonts() {
        TEXT_FONTS.values().forEach(Font::close);
        TEXT_FONTS.clear();
    }

    private static void retireTextFontResources() {
        RETIRED_TEXT_FONTS.addAll(TEXT_FONTS.values());
        TEXT_FONTS.clear();
        RETIRED_TEXT_FONTS.addAll(TEXT_FALLBACK_FONTS.values());
        TEXT_FALLBACK_FONTS.clear();
    }

    static synchronized void releaseRetiredFontResources() {
        RETIRED_TEXT_FONTS.forEach(Font::close);
        RETIRED_TEXT_FONTS.clear();
        RETIRED_IMPORTED_TYPEFACES.forEach(Typeface::close);
        RETIRED_IMPORTED_TYPEFACES.clear();
    }

    private static Font iconFont(IconSet iconSet, float size) {
        float safeSize = Math.max(1.0F, size);
        int key = Float.floatToIntBits(safeSize);
        Map<Integer, Font> fonts = switch (iconSet) {
            case PVP_CONTROLS -> PVP_CONTROL_FONTS;
            case MATERIAL_SYMBOLS -> MATERIAL_SYMBOL_FONTS;
        };
        Typeface typeface = switch (iconSet) {
            case PVP_CONTROLS -> PVP_CONTROLS_TYPEFACE;
            case MATERIAL_SYMBOLS -> MATERIAL_SYMBOLS_TYPEFACE;
        };
        return fonts.computeIfAbsent(key, ignored -> createFont(typeface, safeSize));
    }

    private static Font createFont(Typeface typeface, float size) {
        Font font = typeface == null ? new Font() : new Font(typeface, size);
        return font.setSize(size)
        .setSubpixel(false)
        .setBaselineSnapped(true)
        .setHinting(FontHinting.NORMAL)
        .setEdging(FontEdging.ANTI_ALIAS);
    }

    private static Font fallbackFont(float size) {
        float safeSize = Math.max(1.0F, size);
        int key = Float.floatToIntBits(safeSize);
        return TEXT_FALLBACK_FONTS.computeIfAbsent(key,
        ignored -> createFont(TEXT_FALLBACK_TYPEFACE, safeSize));
    }

    private static Typeface loadTypeface(String resource, FontStyle fallbackStyle) {
        try (InputStream stream = SkijaUi.class.getResourceAsStream(resource)) {
            if (stream != null) {
                try (Data data = Data.makeFromBytes(stream.readAllBytes())) {
                    Typeface typeface = FontMgr.getDefault().makeFromData(data);
                    if (typeface != null) {
                        return typeface;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return findTypeface(fallbackStyle);
    }

    private static Typeface findTypeface(FontStyle style) {
        FontMgr manager = FontMgr.getDefault();
        for (String family : FONT_FAMILIES) {
            Typeface typeface = manager.matchFamilyStyle(family, style);
            if (typeface != null) {
                return typeface;
            }
        }
        return null;
    }

    public static FontMetrics textMetrics(float size) {
        return font(false, size, effectiveFontName()).getMetrics();
    }

    public static FontMetrics iconMetrics(float size, IconSet iconSet) {
        return iconFont(iconSet, size).getMetrics();
    }

    public enum IconSet {
        PVP_CONTROLS,
        MATERIAL_SYMBOLS
    }

    private record FontKey(String name, boolean bold, int sizeBits) {
    }

    private record TextRun(String text, Font font) {
    }
}
