package com.pvp_utils.client.NeteaseMusic;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

final class MusicCoverColors {
    private static final int SAMPLE_SIZE = 48;
    private static final int HUE_BUCKETS = 36;

    private MusicCoverColors() {
    }

    static int extract(byte[] encoded) {
        if (encoded == null || encoded.length == 0) return 0;
        try (MemoryCacheImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(encoded))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return 0;
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                int side = Math.min(width, height);
                if (side <= 0) return 0;
                int stride = Math.max(1, (side + SAMPLE_SIZE - 1) / SAMPLE_SIZE);
                ImageReadParam params = reader.getDefaultReadParam();
                params.setSourceRegion(new Rectangle((width - side) / 2, (height - side) / 2, side, side));
                params.setSourceSubsampling(stride, stride, 0, 0);
                return dominant(reader.read(0, params));
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException ignored) {
            return 0;
        }
    }

    private static int dominant(BufferedImage image) {
        if (image == null) return 0;
        double[] weights = new double[HUE_BUCKETS];
        double[] reds = new double[HUE_BUCKETS];
        double[] greens = new double[HUE_BUCKETS];
        double[] blues = new double[HUE_BUCKETS];
        float[] hsv = new float[3];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int color = image.getRGB(x, y);
                int alpha = color >>> 24;
                if (alpha < 128) continue;
                int red = color >>> 16 & 255;
                int green = color >>> 8 & 255;
                int blue = color & 255;
                Color.RGBtoHSB(red, green, blue, hsv);
                if (hsv[1] < 0.12f || hsv[2] < 0.12f) continue;
                int bucket = Math.min(HUE_BUCKETS - 1, (int) (hsv[0] * HUE_BUCKETS));
                double weight = (alpha / 255.0) * hsv[1] * hsv[1];
                weights[bucket] += weight;
                reds[bucket] += red * weight;
                greens[bucket] += green * weight;
                blues[bucket] += blue * weight;
            }
        }
        int bestBucket = -1;
        double bestWeight = 0.0;
        for (int i = 0; i < HUE_BUCKETS; i++) {
            double weight = neighbors(weights, i);
            if (weight > bestWeight) {
                bestBucket = i;
                bestWeight = weight;
            }
        }
        if (bestBucket < 0) return 0;
        int red = (int) Math.round(neighbors(reds, bestBucket) / bestWeight);
        int green = (int) Math.round(neighbors(greens, bestBucket) / bestWeight);
        int blue = (int) Math.round(neighbors(blues, bestBucket) / bestWeight);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static double neighbors(double[] values, int bucket) {
        return values[(bucket + HUE_BUCKETS - 1) % HUE_BUCKETS]
                + values[bucket] + values[(bucket + 1) % HUE_BUCKETS];
    }
}
