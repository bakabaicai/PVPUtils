package com.pvp_utils.client.NeteaseMusic;

import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.ImageFilter;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.types.Rect;

final class MusicBlurredCover implements AutoCloseable {
    private static final int MAX_SIZE = 256;
    private Image source;
    private Image blurred;
    private float viewportWidth;
    private float viewportHeight;

    Image image(Image cover, float width, float height) {
        if (cover == null || cover.isClosed() || width <= 0f || height <= 0f) return null;
        if (source == cover && blurred != null && viewportWidth == width && viewportHeight == height) return blurred;
        int sourceWidth = cover.getWidth();
        int sourceHeight = cover.getHeight();
        if (sourceWidth <= 0 || sourceHeight <= 0) return null;
        float reduction = Math.min(1f, MAX_SIZE / (float) Math.max(sourceWidth, sourceHeight));
        int cacheWidth = Math.max(1, Math.round(sourceWidth * reduction));
        int cacheHeight = Math.max(1, Math.round(sourceHeight * reduction));
        float displayScale = Math.max(width / cacheWidth, height / cacheHeight) * 1.2f;
        float sigma = 26f / displayScale;
        Rect bounds = Rect.makeWH(cacheWidth, cacheHeight);
        Image generated;
        try (Surface reduced = Surface.makeRaster(ImageInfo.makeN32Premul(cacheWidth, cacheHeight))) {
            reduced.getCanvas().drawImageRect(cover, Rect.makeWH(sourceWidth, sourceHeight), bounds,
                    SamplingMode.LINEAR, null, true);
            try (Image small = reduced.makeImageSnapshot();
                 Surface filtered = Surface.makeRaster(ImageInfo.makeN32Premul(cacheWidth, cacheHeight));
                 ImageFilter blur = ImageFilter.makeBlur(sigma, sigma, FilterTileMode.CLAMP);
                 Paint paint = new Paint().setImageFilter(blur)) {
                filtered.getCanvas().drawImageRect(small, bounds, bounds, SamplingMode.LINEAR, paint, true);
                generated = filtered.makeImageSnapshot();
            }
        }
        close();
        source = cover;
        blurred = generated;
        viewportWidth = width;
        viewportHeight = height;
        return blurred;
    }

    @Override
    public void close() {
        if (blurred != null) blurred.close();
        blurred = null;
        source = null;
    }
}
