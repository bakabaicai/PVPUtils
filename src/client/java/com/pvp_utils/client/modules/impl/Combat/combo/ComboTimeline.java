package com.pvp_utils.client.modules.impl.Combat.combo;

public final class ComboTimeline {
    public static final long DISPLAY_NANOS = 2_600_000_000L;
    public static final long DISPLAY_MILLIS = 2_600L;
    public static final float SCALE = 0.85f * 0.95f;
    public static final float EMBLEM_SIZE = 116.0f * 0.9f * 0.6f;
    public static final float FRAME_WIDTH = 116.0f * 0.8f;
    public static final float FRAME_HEIGHT = 116.0f;
    public static final float BAR_SIZE = 32.0f;
    public static final float BAR_RADIUS = 36.0f;
    public static final float BASE_PARTICLE_SIZE = 112.0f;
    public static final float BASE_PARTICLE_ANCHOR_Y = 206.0f / 256.0f;
    public static final float HALO_RADIUS = 30.0f;
    public static final float HALO_WIDTH = 1.5f;
    public static final int HALO_SEGMENTS = 72;
    public static final int PARTICLE_COLOR = 0x947046;
    public static final int FLASH_COLOR = 0xFF2A36;

    private static final float[][] ANGLES = {
        {0.0f},
        {-90.0f, 90.0f},
        {0.0f, -120.0f, 120.0f},
        {0.0f, -90.0f, 90.0f, 180.0f},
        {0.0f, -72.0f, 72.0f, -144.0f, 144.0f},
        {0.0f, -60.0f, 60.0f, -120.0f, 120.0f, 180.0f}
    };

    public enum Texture {
        EMBLEM("killicon_valorant_champions_2021_emblem.png", 132, 132, 1, 100),
        FRAME("killicon_valorant_base_frame.png", 200, 256, 1, 100),
        BAR("killicon_valorant_champions_2021_bar.png", 76, 36, 1, 100),
        BASE("killicon_valorant_particle_base_t1.png", 256, 12544, 49, 25),
        HERO("killicon_valorant_particle_hero_flame.png", 200, 4480, 20, 29),
        SPARKS("killicon_valorant_particle_large_sparks.png", 256, 13312, 52, 25);

        public final String fileName;
        public final int width;
        public final int height;
        public final int frames;
        public final int intervalMillis;

        Texture(String fileName, int width, int height, int frames, int intervalMillis) {
            this.fileName = fileName;
            this.width = width;
            this.height = height;
            this.frames = frames;
            this.intervalMillis = intervalMillis;
        }

        public int frameHeight() {
            return height / frames;
        }

        public int frameIndex(long elapsedMillis) {
            return (int) Math.min(Math.max(0L, elapsedMillis) / intervalMillis, frames - 1L);
        }

        public float v0(long elapsedMillis) {
            return (float) (frameIndex(elapsedMillis) * frameHeight()) / height;
        }

        public float v1(long elapsedMillis) {
            return (float) ((frameIndex(elapsedMillis) + 1) * frameHeight()) / height;
        }
    }

    public record Frame(long elapsedMillis, int graphicTier, boolean visible,
                        float alpha, float entryOffsetY, float flashAlpha,
                        float barTravel, float barScale, float barRotation) {
        public boolean largeSparks() {
            return graphicTier >= 5;
        }
    }

    private ComboTimeline() {
    }

    public static int graphicTier(int count) {
        return Math.max(1, Math.min(count, 6));
    }

    public static float layoutAngle(int tier, int index) {
        return ANGLES[graphicTier(tier) - 1][index];
    }

    public static float centerX(int guiWidth) {
        return Math.round(guiWidth * 0.5f);
    }

    public static float centerY(int guiHeight) {
        return guiHeight - 80.0f + 6.0f;
    }

    public static Frame sample(long elapsedNanos, int count, float spinDirection) {
        long t = Math.max(0L, elapsedNanos) / 1_000_000L;
        int tier = graphicTier(count);
        float alpha = tier >= 5 ? easeOutQuad(t / 300.0f) : 1.0f;
        if (t > DISPLAY_MILLIS - 220L) {
            alpha *= 1.0f - clamp((t - (DISPLAY_MILLIS - 220L)) / 220.0f);
        }
        float entry = t >= 100L ? 0.0f : lerp(easeOutCubic(t / 100.0f), -16.0f, 0.0f);
        float barScale = t >= 180L ? 1.0f : lerp(easeOutCubic(t / 180.0f), 1.6f, 1.0f);
        float rotation = 0.0f;
        if (tier > 1 && t > 750L) {
            float duration = tier >= 5 ? 1000.0f : 700.0f;
            float angle = tier >= 5 ? 360.0f : 180.0f;
            rotation = angle * easeOutCubic((t - 750L) / duration) * (spinDirection < 0.0f ? -1.0f : 1.0f);
        }
        return new Frame(t, tier, t <= DISPLAY_MILLIS, alpha, entry,
                flashAlpha(t), barTravel(t), barScale, rotation);
    }

    public static float haloAngle(long elapsedMillis) {
        return (Math.max(0L, elapsedMillis) % 2200L) / 2200.0f * (float) (Math.PI * 2.0);
    }

    public static float heroWidth() {
        return EMBLEM_SIZE * 8.0f * (Texture.HERO.width / 1729.0f);
    }

    public static float heroHeight() {
        return EMBLEM_SIZE * 8.0f * (Texture.HERO.frameHeight() / 1729.0f);
    }

    public static float sparksSize() {
        return EMBLEM_SIZE * (6.8f * 2.2f) * (Texture.SPARKS.width / 1729.0f);
    }

    private static float flashAlpha(long t) {

        if (t >= 500L) {
            return 0.0f;
        }
        long phase = t % 140L;
        if (phase >= 80L) {
            return 0.0f;
        }
        float pulse = phase < 20L ? phase / 20.0f : phase > 60L ? (80L - phase) / 20.0f : 1.0f;
        return clamp(pulse) * 0.9f;
    }

    private static float barTravel(long t) {
        if (t < 20L || t >= 620L) {
            return 0.0f;
        }
        if (t <= 180L) {
            float p = (t - 20L) / 160.0f;
            return 9.0f * lerp(0.2f, easeOutQuad(p), easeOutCubic(p));
        }
        if (t <= 270L) {
            return 9.0f;
        }
        float p = clamp((t - 270L) / 350.0f);
        return lerp(p * p * p * p, 9.0f, 0.0f);
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(value, 1.0f));
    }

    private static float easeOutQuad(float value) {
        float inverse = 1.0f - clamp(value);
        return 1.0f - inverse * inverse;
    }

    private static float easeOutCubic(float value) {
        float inverse = 1.0f - clamp(value);
        return 1.0f - inverse * inverse * inverse;
    }

    private static float lerp(float amount, float start, float end) {
        return start + amount * (end - start);
    }
}
