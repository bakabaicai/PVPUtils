package com.pvp_utils.client.gui.clickgui.theme;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.UiText;

import java.awt.Color;
import java.util.List;

public final class AccentColors {
    public enum Tag {
        RED("红色", "Red", 0xFFFF3232), ORANGE("橙色", "Orange", 0xFFFF8032),
        YELLOW("黄色", "Yellow", 0xFFFFFF32), LIME("青柠", "Lime", 0xFF80FF32),
        DARK_GREEN("绿色", "Green", 0xFF328032), AQUA("青色", "Aqua", 0xFF32C8FF),
        DARK_BLUE("蓝色", "Blue", 0xFF3264C8), PURPLE("紫色", "Purple", 0xFF8032FF),
        PINK("粉色", "Pink", 0xFFFF80FF), GRAY("灰色", "Gray", 0xFF64646E);

        private final String chinese;
        private final String english;
        public final int color;

        Tag(String chinese, String english, int color) {
            this.chinese = chinese;
            this.english = english;
            this.color = color;
        }

        public String label() {
            return UiText.t(chinese, english);
        }
    }

    public enum Preset {
        AUBERGINE("Aubergine", new int[]{new Color(170, 7, 107).getRGB(), new Color(97, 4, 95).getRGB()}, Tag.PURPLE, Tag.RED),
        AQUA("Aqua", new int[]{new Color(185, 250, 255).getRGB(), new Color(79, 199, 200).getRGB()}, Tag.AQUA),
        BANANA("Banana", new int[]{new Color(253, 236, 177).getRGB(), new Color(255, 255, 255).getRGB()}, Tag.YELLOW),
        BLEND("Blend", new int[]{new Color(71, 148, 253).getRGB(), new Color(71, 253, 160).getRGB()}, Tag.AQUA, Tag.LIME),
        BLOSSOM("Blossom", new int[]{new Color(226, 208, 249).getRGB(), new Color(49, 119, 115).getRGB()}, Tag.PINK, Tag.GRAY),
        BUBBLEGUM("Bubblegum", new int[]{new Color(243, 145, 216).getRGB(), new Color(152, 165, 243).getRGB()}, Tag.PINK, Tag.PURPLE),
        CANDY_CANE("Candy Cane", new int[]{new Color(255, 0, 0).getRGB(), new Color(255, 255, 255).getRGB()}, Tag.RED),
        CHERRY("Cherry", new int[]{new Color(187, 55, 125).getRGB(), new Color(251, 211, 233).getRGB()}, Tag.RED, Tag.PURPLE, Tag.PINK),
        CHRISTMAS("Christmas", new int[]{new Color(255, 64, 64).getRGB(), new Color(255, 255, 255).getRGB(), new Color(64, 255, 64).getRGB()}, Tag.RED, Tag.LIME),
        CORAL("Coral", new int[]{new Color(244, 168, 150).getRGB(), new Color(52, 133, 151).getRGB()}, Tag.PINK, Tag.ORANGE, Tag.DARK_BLUE),
        DIGITAL_HORIZON("Digital Horizon", new int[]{new Color(95, 195, 228).getRGB(), new Color(229, 93, 135).getRGB()}, Tag.AQUA, Tag.RED, Tag.PINK),
        EXPRESS("Express", new int[]{new Color(173, 83, 137).getRGB(), new Color(60, 16, 83).getRGB()}, Tag.PURPLE, Tag.PINK),
        LIME_WATER("Lime Water", new int[]{new Color(18, 255, 247).getRGB(), new Color(179, 255, 171).getRGB()}, Tag.AQUA, Tag.LIME),
        LUSH("Lush", new int[]{new Color(168, 224, 99).getRGB(), new Color(86, 171, 47).getRGB()}, Tag.LIME, Tag.DARK_GREEN),
        HALOGEN("Halogen", new int[]{new Color(255, 65, 108).getRGB(), new Color(255, 75, 43).getRGB()}, Tag.RED, Tag.ORANGE),
        HYPER("Hyper", new int[]{new Color(236, 110, 173).getRGB(), new Color(52, 148, 230).getRGB()}, Tag.PINK, Tag.DARK_BLUE, Tag.AQUA),
        MAGIC("Magic", new int[]{new Color(74, 0, 224).getRGB(), new Color(142, 45, 226).getRGB()}, Tag.DARK_BLUE, Tag.PURPLE),
        MAY("May", new int[]{new Color(238, 79, 238).getRGB(), new Color(253, 219, 245).getRGB()}, Tag.PINK, Tag.PURPLE),
        ORANGE_JUICE("Orange Juice", new int[]{new Color(252, 74, 26).getRGB(), new Color(247, 183, 51).getRGB()}, Tag.ORANGE, Tag.YELLOW),
        PASTEL("Pastel", new int[]{new Color(243, 155, 178).getRGB(), new Color(207, 196, 243).getRGB()}, Tag.PINK),
        PUMPKIN("Pumpkin", new int[]{new Color(241, 166, 98).getRGB(), new Color(255, 216, 169).getRGB(), new Color(227, 139, 42).getRGB()}, Tag.ORANGE),
        SATIN("Satin", new int[]{new Color(215, 60, 67).getRGB(), new Color(140, 23, 39).getRGB()}, Tag.RED),
        SNOWY_SKY("Snowy Sky", new int[]{new Color(1, 171, 179).getRGB(), new Color(234, 234, 234).getRGB(), new Color(18, 232, 232).getRGB()}, Tag.AQUA, Tag.GRAY),
        STEEL_FADE("Steel Fade", new int[]{new Color(66, 134, 244).getRGB(), new Color(55, 59, 68).getRGB()}, Tag.DARK_BLUE, Tag.GRAY),
        SUNDAE("Sundae", new int[]{new Color(206, 74, 126).getRGB(), new Color(122, 44, 77).getRGB()}, Tag.PINK, Tag.PURPLE, Tag.RED),
        SUNKIST("Sunkist", new int[]{new Color(242, 201, 76).getRGB(), new Color(242, 153, 74).getRGB()}, Tag.YELLOW, Tag.ORANGE),
        WATER("Water", new int[]{new Color(12, 232, 199).getRGB(), new Color(12, 163, 232).getRGB()}, Tag.AQUA, Tag.DARK_BLUE),
        LEGACY("Legacy", new int[]{new Color(7393023).getRGB(), new Color(7393023).getRGB()}, Tag.AQUA, Tag.DARK_BLUE),
        WINTER("Winter", new int[]{Color.WHITE.getRGB(), Color.WHITE.getRGB()}, Tag.GRAY, Tag.GRAY),
        PEONY("Peony", new int[]{new Color(226, 208, 249).getRGB(), new Color(207, 171, 255).getRGB()}, Tag.PINK, Tag.GRAY),
        SHADOW("Shadow", new int[]{new Color(97, 131, 255).getRGB(), new Color(206, 212, 255).getRGB()}, Tag.AQUA),
        WOOD("Wood", new int[]{new Color(79, 109, 81).getRGB(), new Color(170, 139, 87).getRGB(), new Color(240, 235, 206).getRGB()}, Tag.DARK_GREEN),
        CREIDA("Creida", new int[]{new Color(-11644304).brighter().brighter().getRGB(), new Color(-11644304).darker().getRGB()}, Tag.GRAY),
        CREIDA_TWO("Creida Two", new int[]{new Color(-6632725).getRGB(), new Color(-8406042).darker().getRGB()}, Tag.GRAY),
        GOTHIC("Gothic", new int[]{new Color(31, 30, 30).getRGB(), new Color(196, 190, 190).getRGB()}, Tag.GRAY),
        SEN("Rue", new int[]{new Color(234, 118, 176).getRGB(), new Color(31, 30, 30).getRGB()}, Tag.PINK),
        PURPLE("Purple", new int[]{new Color(5391249).getRGB(), new Color(5391249).brighter().getRGB()}),
        TEST("Rainbow", new int[]{new Color(255, 50, 50).getRGB(), new Color(50, 200, 255).getRGB()}),
        NORD("Nord", new int[]{new Color(143, 188, 187).getRGB(), new Color(163, 190, 140).getRGB(), new Color(236, 239, 244).getRGB()}, Tag.AQUA, Tag.GRAY);

        public final String label;
        public final int[] colors;
        private final List<Tag> tags;

        Preset(String label, int[] colors, Tag... tags) {
            this.label = label;
            this.colors = colors;
            this.tags = List.of(tags);
        }

        public boolean contains(Tag tag) {
            return tag == null || tags.contains(tag);
        }
    }

    private static long lastSampleNanos;
    private static double elapsedSeconds;
    private static float previousSpeed = 1f;
    private static boolean previouslyAnimated;

    private AccentColors() {
    }

    public static Preset current() {
        try {
            return Preset.valueOf(Config.accentPreset);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return Preset.BLEND;
        }
    }

    public static void select(Preset preset) {
        Config.accentPreset = preset.name();
        Config.accentCustomColors = false;
        Config.save();
    }

    public static int accent() {
        return accent(0f, 0f);
    }

    public static int accent(float x, float y) {
        if (Config.accentCustomColors) {
            return mix(Config.accentPrimaryColor, Config.accentSecondaryColor, blend(x, y));
        }
        return sample(current(), x, y);
    }

    public static int secondary() {
        return accent(0f, 50f);
    }

    public static int sample(Preset preset, float x, float y) {
        if (preset == Preset.TEST) {
            double time = animationTime() * 0.15;
            return Color.HSBtoRGB((float) ((time + (x + y) * 0.0015) % 1.0), 0.75f, 1f);
        }
        int[] colors = preset.colors;
        float factor = blend(x, y);
        if (colors.length == 3) {
            return factor <= 0.5f
                    ? mix(colors[0], colors[1], factor * 2f)
                    : mix(colors[1], colors[2], (factor - 0.5f) * 2f);
        }
        return mix(colors[0], colors[Math.min(1, colors.length - 1)], factor);
    }

    private static float blend(float x, float y) {
        double time = animationTime() / 0.6;
        return (float) (Math.sin(time + x * 0.005 + y * 0.06) * 0.5 + 0.5);
    }

    private static float speed() {
        return Float.isFinite(Config.accentSpeed) ? Math.clamp(Config.accentSpeed, 0.1f, 3f) : 1f;
    }

    private static double animationTime() {
        long now = System.nanoTime();
        if (lastSampleNanos != 0L && previouslyAnimated) {
            elapsedSeconds += Math.max(0L, now - lastSampleNanos) * 1.0e-9 * previousSpeed;
        }
        lastSampleNanos = now;
        previousSpeed = speed();
        previouslyAnimated = Config.accentAnimated;
        return elapsedSeconds;
    }

    public static int mix(int first, int second, float amount) {
        float t = Math.clamp(amount, 0f, 1f);
        int a = Math.round((first >>> 24) + ((second >>> 24) - (first >>> 24)) * t);
        int r = Math.round((first >> 16 & 255) + ((second >> 16 & 255) - (first >> 16 & 255)) * t);
        int g = Math.round((first >> 8 & 255) + ((second >> 8 & 255) - (first >> 8 & 255)) * t);
        int b = Math.round((first & 255) + ((second & 255) - (first & 255)) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static int withAlpha(int color, float opacity) {
        return Math.round((color >>> 24) * Math.clamp(opacity, 0f, 1f)) << 24 | color & 0x00FFFFFF;
    }
}
