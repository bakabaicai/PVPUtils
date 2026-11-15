package com.pvp_utils.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.locale.Language;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

import java.util.HashMap;

public final class NameTagTextCache {
    private static final int MAX_ENTRIES = 2048;
    private static final HashMap<Component, Entry> ENTRIES = new HashMap<>();
    private static Object level;
    private static Language language;

    private NameTagTextCache() {}

    public static void clear() {
        ENTRIES.clear();
        level = null;
        language = null;
    }

    public static int width(Font font, Component component) {
        Entry entry = entry(font, component);
        if (entry.width < 0) {
            entry.width = font.width(component);
        }
        return entry.width;
    }

    public static void draw(Font font, Component component, float x, float y, int color, boolean shadow,
                            Matrix4f pose, MultiBufferSource buffers, Font.DisplayMode mode,
                            int background, int light) {
        Entry entry = entry(font, component);
        if (entry.obfuscated) {
            font.drawInBatch(component, x, y, color, shadow, pose, buffers, mode, background, light);
            return;
        }
        boolean seeThrough = mode == Font.DisplayMode.SEE_THROUGH;
        Layout layout = seeThrough ? entry.seeThrough : entry.normal;
        if (layout == null || !layout.matches(x, y, color, shadow, background)) {
            layout = new Layout(x, y, color, shadow, background,
                    font.prepareText(entry.sequence, x, y, color, shadow, false, background));
            if (seeThrough) {
                entry.seeThrough = layout;
            } else {
                entry.normal = layout;
            }
        }
        layout.text.visit(Font.GlyphVisitor.forMultiBufferSource(buffers, pose, mode, light));
    }

    private static Entry entry(Font font, Component component) {
        Object currentLevel = Minecraft.getInstance().level;
        Language currentLanguage = Language.getInstance();
        if (level != currentLevel || language != currentLanguage) {
            ENTRIES.clear();
            level = currentLevel;
            language = currentLanguage;
        }
        Entry entry = ENTRIES.get(component);
        if (entry == null || entry.font != font) {
            if (entry == null && ENTRIES.size() >= MAX_ENTRIES) {
                ENTRIES.clear();
            }
            Component snapshot = snapshot(component);
            entry = new Entry(font, snapshot.getVisualOrderText());
            ENTRIES.put(snapshot, entry);
        }
        return entry;
    }

    private static Component snapshot(Component component) {
        MutableComponent copy = component.plainCopy().setStyle(component.getStyle());
        for (Component sibling : component.getSiblings()) {
            copy.append(snapshot(sibling));
        }
        return copy;
    }

    private static final class Entry {
        private final Font font;
        private final FormattedCharSequence sequence;
        private final boolean obfuscated;
        private int width = -1;
        private Layout normal;
        private Layout seeThrough;

        private Entry(Font font, FormattedCharSequence sequence) {
            this.font = font;
            this.sequence = sequence;
            obfuscated = !sequence.accept((index, style, codepoint) -> !style.isObfuscated());
        }
    }

    private record Layout(float x, float y, int color, boolean shadow, int background, Font.PreparedText text) {
        private boolean matches(float x, float y, int color, boolean shadow, int background) {
            return this.x == x && this.y == y && this.color == color
                    && this.shadow == shadow && this.background == background;
        }
    }
}
