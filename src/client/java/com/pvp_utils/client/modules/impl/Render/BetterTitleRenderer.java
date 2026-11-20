package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.chroma.ChromaDraw;
import com.pvp_utils.client.render.chroma.ChromaGraphics;
import io.github.humbleui.skija.BlendMode;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorFilter;
import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.ImageFilter;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Shader;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BetterTitleRenderer {
    private final Channel title = new Channel();
    private final Channel subtitle = new Channel();
    private final LinkedHashMap<TextKey, Layout> layouts = new LinkedHashMap<>(16, 0.75f, true);
    private long fontRevision = -1L;
    private boolean betterFont;
    private boolean retiring;
    private float animationSpeed = 1f;
    private float fadeInSeconds;
    private float expiresAt = Float.NEGATIVE_INFINITY;
    private long frameStamp;

    public void setTitle(Component text, Component subtitleText, int fadeIn, int stay, int fadeOut, float tick) {
        if (text == null) {
            dismiss(tick);
            return;
        }
        configureTiming(fadeIn, stay, fadeOut);
        this.expiresAt = tick + Math.max(0L, (long) fadeIn + stay + fadeOut);
        this.retiring = false;
        this.title.update(text, tick / 20f, this.animationSpeed, this.fadeInSeconds);
        this.subtitle.update(subtitleText, tick / 20f, this.animationSpeed, this.fadeInSeconds);
    }

    public void setSubtitle(Component text, float tick) {
        if (this.retiring || tick > this.expiresAt) return;
        this.subtitle.update(text, tick / 20f, this.animationSpeed, this.fadeInSeconds);
    }

    public void dismiss(float tick) {
        this.expiresAt = tick;
        beginDismiss(tick / 20f, 0.35f / this.animationSpeed);
    }

    private void beginDismiss(float seconds, float duration) {
        this.retiring = true;
        this.title.dismiss(seconds, duration);
        this.subtitle.dismiss(seconds, duration);
    }

    private void configureTiming(int fadeIn, int stay, int fadeOut) {
        this.animationSpeed = Math.max(0.05f, Config.betterTitleAnimationSpeed);
        this.fadeInSeconds = Math.max(0, fadeIn) / 20f;
    }

    public void render(Component titleText, Component subtitleText, float remaining, int fadeIn, int stay,
                       int fadeOut, float tick, int width, int height) {
        float seconds = tick / 20f;
        boolean active = titleText != null && remaining > 0f;
        if (active) {
            float expires = tick + remaining;
            if (expires > this.expiresAt + 1f) this.retiring = false;
            this.expiresAt = expires;
        } else if (tick > this.expiresAt + 7f / this.animationSpeed) {
            clear();
            return;
        }
        if (active && !this.retiring) {
            configureTiming(fadeIn, stay, fadeOut);
            this.title.update(titleText, seconds, this.animationSpeed, this.fadeInSeconds);
            this.subtitle.update(subtitleText, seconds, this.animationSpeed, this.fadeInSeconds);
            if (fadeOut > 0 && remaining <= fadeOut) beginDismiss(seconds, Math.max(0.01f, remaining / 20f));
        } else if (!active && !this.retiring) {
            dismiss(tick);
        }
        List<Line> titles = this.title.snapshot(seconds);
        List<Line> subtitles = this.subtitle.snapshot(seconds);
        if (titles.isEmpty() && subtitles.isEmpty()) {
            if (!active) clear();
            return;
        }
        float scale = Config.betterTitleScale;
        refreshFont();
        this.frameStamp++;
        for (Line line : titles) layout(line.text(), 38f * scale, true);
        for (Line line : subtitles) layout(line.text(), 16f * scale, false);
        ChromaGraphics.submit("BetterTitle", canvas -> {
            float centerX = width * 0.5f;
            float centerY = height * 0.5f;
            try (MinecraftTitleText.Atlases atlases = new MinecraftTitleText.Atlases()) {
                for (Line line : titles) {
                    drawLine(canvas, line, centerX, centerY - 18f * scale,
                            width, height, 38f * scale, seconds, false, atlases);
                }
                for (Line line : subtitles) {
                    drawLine(canvas, line, centerX, centerY + 38f * scale,
                            width, height, 16f * scale, seconds, true, atlases);
                }
            }
        });
    }

    public void clear() {
        this.title.clear();
        this.subtitle.clear();
        this.retiring = false;
        this.expiresAt = Float.NEGATIVE_INFINITY;
        if (!this.layouts.isEmpty()) {
            this.layouts.values().forEach(Layout::close);
            this.layouts.clear();
        }
    }

    private void refreshFont() {
        long revision = Config.betterTitleBetterFont ? ChromaDraw.textFontRevision() : -1L;
        if (revision == this.fontRevision && this.betterFont == Config.betterTitleBetterFont) return;
        this.layouts.values().forEach(Layout::close);
        this.layouts.clear();
        this.fontRevision = revision;
        this.betterFont = Config.betterTitleBetterFont;
    }

    private void drawLine(Canvas canvas, Line line, float x, float y, int screenWidth, float travel,
                          float size, float seconds, boolean secondary, MinecraftTitleText.Atlases atlases) {
        float alpha = line.pose().alpha();
        if (alpha <= 0.002f) return;
        Layout layout = layout(line.text(), size, !secondary);
        if (layout.width() <= 0f) return;
        float fit = Math.min(1f, screenWidth * 0.82f / layout.width());
        Pose pose = line.pose();
        int save = canvas.save();
        try {
            canvas.translate(x, y + pose.y() * travel);
            canvas.rotate(pose.angle());
            canvas.scale(fit * pose.scale(), fit * pose.scale());
            float left = -layout.width() * 0.5f;
            float top = -size * 0.58f;
            float phase = seconds * 1.8f;
            float blurSigma = size * pose.blur();
            int shade = Math.round(clamp(pose.brightness()) * 255f);
            try (Shader shader = Shader.makeLinearGradient(left, 0f, left + layout.width(), 0f,
                    new int[]{gradientColor(phase, secondary), gradientColor(phase + 1.6f, secondary),
                            gradientColor(phase + 3.2f, secondary)}, new float[]{0f, 0.5f, 1f});
                 ColorFilter dim = shade == 255 ? null : ColorFilter.makeBlend(
                         0xFF000000 | (shade << 16) | (shade << 8) | shade, BlendMode.MODULATE);
                 ImageFilter softened = blurSigma <= 0.001f ? null
                         : ImageFilter.makeBlur(blurSigma, blurSigma, FilterTileMode.DECAL);
                 Paint paint = new Paint().setAntiAlias(true)) {
                if (Config.betterTitleGradient) paint.setShader(shader);
                paint.setColorFilter(dim);
                paint.setImageFilter(softened);
                if (Config.betterTitleGlow) {
                    float broadSigma = (float) Math.hypot(size * 0.48f, blurSigma);
                    float tightSigma = (float) Math.hypot(size * 0.12f, blurSigma);
                    try (ImageFilter broad = ImageFilter.makeBlur(broadSigma, broadSigma, FilterTileMode.DECAL);
                         ImageFilter tight = ImageFilter.makeBlur(tightSigma, tightSigma, FilterTileMode.DECAL)) {
                        paint.setImageFilter(broad);
                        layout.draw(canvas, left, top, alpha * 0.48f, paint, Config.betterTitleGradient, atlases,
                                this.frameStamp);
                        paint.setImageFilter(tight);
                        layout.draw(canvas, left, top, alpha * 0.65f, paint, Config.betterTitleGradient, atlases,
                                this.frameStamp);
                        paint.setImageFilter(softened);
                    }
                }
                layout.draw(canvas, left, top, alpha, paint, Config.betterTitleGradient, atlases, this.frameStamp);
            }
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private Layout layout(Component component, float size, boolean bold) {
        TextKey key = new TextKey(component, size, bold);
        Layout existing = this.layouts.get(key);
        if (existing != null && (existing.nativeText() == null || existing.nativeText().isValid())) return existing;
        if (existing != null) existing.close();
        if (!Config.betterTitleBetterFont || obfuscated(component)) {
            MinecraftTitleText text = new MinecraftTitleText(component, size, bold);
            return cacheLayout(key, new Layout(List.of(), text.width(), size, text));
        }
        List<Run> runs = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        Style[] previous = {Style.EMPTY};
        component.getVisualOrderText().accept((index, style, codePoint) -> {
            if (!previous[0].equals(style) && !value.isEmpty()) {
                runs.add(run(value.toString(), previous[0], size, bold));
                value.setLength(0);
            }
            previous[0] = style;
            value.appendCodePoint(codePoint == '\n' || codePoint == '\r' ? ' ' : codePoint);
            return true;
        });
        if (!value.isEmpty()) runs.add(run(value.toString(), previous[0], size, bold));
        Layout created = new Layout(List.copyOf(runs),
                (float) runs.stream().mapToDouble(run -> run.text().width()).sum(), size, null);
        return cacheLayout(key, created);
    }

    private Layout cacheLayout(TextKey key, Layout created) {
        this.layouts.put(key, created);
        while (this.layouts.size() > 24) {
            Map.Entry<TextKey, Layout> eldest = this.layouts.pollFirstEntry();
            if (eldest != null) eldest.getValue().close();
        }
        return created;
    }

    private static Run run(String value, Style style, float size, boolean bold) {
        return new Run(ChromaDraw.prepareText(value, size, bold || style.isBold()), style);
    }

    private static boolean obfuscated(Component component) {
        return !component.getVisualOrderText().accept((index, style, codePoint) -> !style.isObfuscated());
    }

    private static float clamp(float value) {
        return Math.clamp(value, 0f, 1f);
    }

    private static float mix(float from, float to, float progress) {
        return from + (to - from) * progress;
    }

    private static int gradientColor(float phase, boolean secondary) {
        float mix = 0.5f + 0.5f * (float) Math.sin(phase);
        int from = secondary ? 0xB895FF : 0x9BF5E7;
        int to = secondary ? 0x6C76D9 : 0x2462FF;
        int red = Math.round(((from >> 16) & 255) * (1f - mix) + ((to >> 16) & 255) * mix);
        int green = Math.round(((from >> 8) & 255) * (1f - mix) + ((to >> 8) & 255) * mix);
        int blue = Math.round((from & 255) * (1f - mix) + (to & 255) * mix);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private record TextKey(Component text, float size, boolean bold) {}
    private record Line(Component text, Pose pose) {}
    private record Pose(float y, float angle, float scale, float alpha, float brightness, float blur) {}
    private record Run(ChromaDraw.PreparedText text, Style style) {}
    private record Departing(Component text, Pose from, float started, float duration, boolean rapid) {}

    private record Layout(List<Run> runs, float width, float size, MinecraftTitleText nativeText) implements AutoCloseable {
        void draw(Canvas canvas, float x, float top, float opacity, Paint paint, boolean gradient,
                  MinecraftTitleText.Atlases atlases, long stamp) {
            if (nativeText != null) {
                nativeText.draw(canvas, x, top, opacity, paint, gradient, atlases, stamp);
                return;
            }
            for (Run run : runs) {
                Style style = run.style();
                int rgb = gradient || style.getColor() == null ? 0xFFFFFF : style.getColor().getValue();
                int color = (Math.round(clamp(opacity) * 255f) << 24) | rgb;
                int save = canvas.save();
                try {
                    if (style.isItalic()) {
                        canvas.translate(0f, top);
                        canvas.skew(-0.16f, 0f);
                        canvas.translate(0f, -top);
                    }
                    run.text().drawTop(canvas, x, top, color, paint);
                    paint.setColor(color);
                    if (style.isUnderlined()) {
                        canvas.drawRect(io.github.humbleui.types.Rect.makeXYWH(x, top + size, run.text().width(),
                                Math.max(0.6f, size * 0.045f)), paint);
                    }
                    if (style.isStrikethrough()) {
                        canvas.drawRect(io.github.humbleui.types.Rect.makeXYWH(x, top + size * 0.55f,
                                run.text().width(), Math.max(0.6f, size * 0.045f)), paint);
                    }
                } finally {
                    canvas.restoreToCount(save);
                }
                x += run.text().width();
            }
        }

        @Override
        public void close() {
            runs.forEach(run -> run.text().close());
        }
    }

    private static final class Channel {
        private final List<Departing> departing = new ArrayList<>();
        private Component current;
        private float started;
        private float speed;
        private float fade;
        private boolean rapid;
        private int direction = 1;

        void dismiss(float now, float duration) {
            if (this.current == null) return;
            boolean fast = this.rapid || this.departing.stream().anyMatch(Departing::rapid);
            this.departing.add(new Departing(this.current, pose(now), now,
                    fast ? Math.min(duration, 0.3f / this.speed) : duration, fast));
            if (this.departing.size() > 8) this.departing.removeFirst();
            this.current = null;
        }

        void update(Component text, float now, float animationSpeed, float fadeIn) {
            if (text != null && text.getString().isBlank()) text = null;
            if (java.util.Objects.equals(this.current, text)) return;
            if (this.current != null && text != null && this.current.getString().equals(text.getString())) {
                this.current = text.copy();
                return;
            }
            this.departing.removeIf(line -> now - line.started() >= line.duration());
            float interval = this.current == null ? 0.12f : Math.max(0f, now - this.started);
            boolean fast = this.current != null && interval * animationSpeed < 0.45f
                    || this.departing.stream().anyMatch(Departing::rapid);
            float fastDuration = Math.clamp(interval * animationSpeed * 2.5f, 0.16f, 0.3f) / animationSpeed;
            Pose previous = this.current == null ? null : pose(now);
            if (fast) accelerateDepartures(now, fastDuration);
            if (this.current != null) {
                this.departing.add(new Departing(this.current, previous, now,
                        fast ? fastDuration : 0.9f / animationSpeed, fast));
                if (this.departing.size() > 8) this.departing.removeFirst();
            }
            this.current = text == null ? null : text.copy();
            this.started = now;
            this.speed = animationSpeed;
            this.fade = this.departing.isEmpty() ? fadeIn : 0.12f;
            this.direction = -this.direction;
            this.rapid = fast;
        }

        private void accelerateDepartures(float now, float duration) {
            for (int i = 0; i < this.departing.size(); i++) {
                Departing line = this.departing.get(i);
                if (line.rapid()) continue;
                float remaining = line.duration() - (now - line.started());
                this.departing.set(i, new Departing(line.text(), departurePose(line, now), now,
                        Math.max(0.001f, Math.min(duration, remaining)), true));
            }
        }

        private Pose pose(float now) {
            float age = Math.max(0f, now - this.started);
            float duration = 0.9f / Math.max(0.05f, this.speed);
            float progress = clamp(age / duration);
            float eased = 1f - (1f - progress) * (1f - progress) * (1f - progress);
            return new Pose(0.9f * (1f - eased), this.direction * 70f * (1f - eased),
                    1f + 0.025f * (1f - eased),
                    this.fade <= 0f ? 1f : clamp(age / this.fade), 1f, 0f);
        }

        private static Pose departurePose(Departing line, float now) {
            float age = Math.max(0f, now - line.started());
            float progress = clamp(age / line.duration());
            float move = progress * progress * (3f - 2f * progress);
            Pose from = line.from();
            float alpha = from.alpha() * (1f - progress * progress);
            if (!line.rapid()) {
                return new Pose(mix(from.y(), Math.min(-0.68f, from.y() - 0.65f), move), from.angle() + 5f * move,
                        from.scale(), alpha, from.brightness(), from.blur());
            }
            float soften = clamp(age / Math.min(0.065f, line.duration() * 0.4f));
            soften = soften * soften * (3f - 2f * soften);
            float travel = 1f - (1f - progress) * (1f - progress);
            return new Pose(mix(from.y(), Math.min(-0.68f, from.y() - 0.45f), travel),
                    mix(from.angle(), Math.signum(from.angle()) * 6f, travel), from.scale(), alpha,
                    mix(from.brightness(), 0.28f, soften), mix(from.blur(), 0.13f, soften));
        }

        List<Line> snapshot(float now) {
            List<Line> lines = new ArrayList<>();
            this.departing.removeIf(line -> now - line.started() >= line.duration());
            for (Departing line : this.departing) {
                lines.add(new Line(line.text(), departurePose(line, now)));
            }
            if (this.current != null) lines.add(new Line(this.current, pose(now)));
            return List.copyOf(lines);
        }

        void clear() {
            this.current = null;
            this.departing.clear();
            this.rapid = false;
            this.direction = 1;
        }
    }
}
