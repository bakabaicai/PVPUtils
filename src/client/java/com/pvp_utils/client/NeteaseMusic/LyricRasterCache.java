package com.pvp_utils.client.NeteaseMusic;

import com.pvp_utils.client.render.skia.SkijaUi;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.BlendMode;
import io.github.humbleui.skija.ColorFilter;
import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.ImageFilter;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.types.Rect;

import java.util.LinkedHashMap;
import java.util.Map;

public final class LyricRasterCache implements AutoCloseable {
    private static final int MAX_ENTRIES = 96;
    private static final long MAX_PIXELS = 4_194_304L;
    private static final float BLUR = 2.5f;
    private static final float MARGIN = 12f;
    private final Map<Key, Entry> entries = new LinkedHashMap<>(32, 0.75f, true);
    private final Map<Integer, ColorFilter> colorFilters = new LinkedHashMap<>(16, 0.75f, true);
    private final Paint imagePaint = new Paint().setAntiAlias(true);
    private long pixels;
    private boolean closed;

    public void draw(Canvas canvas, SkijaUi.PreparedText text, float size, float x, float top,
                     int color, float blur, float resolution) {
        if ((color >>> 24) == 0) return;
        int scale = Math.clamp(Math.round(resolution), 1, 4);
        int width = Math.max(1, (int) Math.ceil((text.width() + MARGIN * 2f) * scale));
        int height = Math.max(1, (int) Math.ceil((size * 2f + MARGIN * 2f) * scale));
        long required = 2L * width * height;
        if (width > 8192 || required > MAX_PIXELS / 2L) {
            text.drawTop(canvas, x, top, color);
            return;
        }
        Key key = new Key(text, scale);
        Entry entry = entries.get(key);
        if (entry == null) {
            while (!entries.isEmpty() && (entries.size() >= MAX_ENTRIES || pixels + required > MAX_PIXELS)) {
                var oldest = entries.entrySet().iterator();
                Entry evicted = oldest.next().getValue();
                oldest.remove();
                pixels -= evicted.pixels();
                evicted.close();
            }
            entry = create(text, scale, width, height);
            entries.put(key, entry);
            pixels += required;
        }
        imagePaint.setColorFilter(colorFilter(color));
        float alpha = (color >>> 24) / 255f;
        float blend = Math.clamp(blur / BLUR, 0f, 1f);
        Rect destination = Rect.makeXYWH(x - MARGIN, top - MARGIN, width / (float) scale, height / (float) scale);
        if (blend > 0.001f) {
            imagePaint.setAlphaf(alpha * blend);
            canvas.drawImageRect(entry.blurred(), Rect.makeWH(width, height), destination, SamplingMode.LINEAR, imagePaint, true);
        }
        if (blend < 0.999f) {
            imagePaint.setAlphaf(alpha * (1f - blend) / Math.max(0.001f, 1f - alpha * blend));
            canvas.drawImageRect(entry.sharp(), Rect.makeWH(width, height), destination, SamplingMode.LINEAR, imagePaint, true);
        }
    }

    private Entry create(SkijaUi.PreparedText text, int scale, int width, int height) {
        Image sharp = null;
        try (Surface surface = Surface.makeRaster(ImageInfo.makeN32Premul(width, height))) {
            Canvas canvas = surface.getCanvas();
            canvas.clear(0);
            canvas.scale(scale, scale);
            text.drawTop(canvas, MARGIN, MARGIN, 0xFFFFFFFF);
            sharp = surface.makeImageSnapshot();
            try (Surface filtered = Surface.makeRaster(ImageInfo.makeN32Premul(width, height));
                 ImageFilter filter = ImageFilter.makeBlur(BLUR * scale, BLUR * scale, FilterTileMode.DECAL);
                 Paint paint = new Paint().setImageFilter(filter)) {
                filtered.getCanvas().clear(0);
                filtered.getCanvas().drawImage(sharp, 0f, 0f, paint);
                return new Entry(sharp, filtered.makeImageSnapshot(), 2L * width * height);
            }
        } catch (RuntimeException | Error e) {
            if (sharp != null) sharp.close();
            throw e;
        }
    }

    private ColorFilter colorFilter(int color) {
        int rgb = color | 0xFF000000;
        ColorFilter filter = colorFilters.get(rgb);
        if (filter != null) return filter;
        filter = ColorFilter.makeBlend(rgb, BlendMode.SRC_IN);
        colorFilters.put(rgb, filter);
        if (colorFilters.size() > 32) {
            var oldest = colorFilters.entrySet().iterator();
            oldest.next().getValue().close();
            oldest.remove();
        }
        return filter;
    }

    public void clear() {
        if (closed) return;
        entries.values().forEach(Entry::close);
        entries.clear();
        pixels = 0L;
        imagePaint.setColorFilter(null);
        colorFilters.values().forEach(ColorFilter::close);
        colorFilters.clear();
    }

    @Override
    public void close() {
        if (closed) return;
        clear();
        imagePaint.close();
        closed = true;
    }

    private record Key(SkijaUi.PreparedText text, int scale) {
    }

    private record Entry(Image sharp, Image blurred, long pixels) implements AutoCloseable {
        @Override
        public void close() {
            sharp.close();
            blurred.close();
        }
    }
}
