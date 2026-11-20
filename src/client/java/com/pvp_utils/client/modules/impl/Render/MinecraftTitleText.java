package com.pvp_utils.client.modules.impl.Render;

import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pvp_utils.client.render.chroma.ChromaGraphics;
import io.github.humbleui.skija.BlendMode;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Shader;
import io.github.humbleui.types.Point;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class MinecraftTitleText {
    private static final short[] INDICES = {0, 1, 2, 0, 2, 3};
    private final Font font;
    private final FormattedCharSequence sequence;
    private final List<Quad> quads = new ArrayList<>();
    private final float width;
    private final float padding;
    private final float scale;
    private final boolean obfuscated;
    private long bakedAt;
    private float left;
    private float top;
    private float right;
    private float bottom;

    MinecraftTitleText(Component component, float size, boolean bold) {
        this.font = Minecraft.getInstance().font;
        this.scale = size / 9f;
        this.padding = size * 2f;
        this.sequence = sink -> component.getVisualOrderText().accept((index, style, codePoint) ->
                sink.accept(index, bold ? style.withBold(true) : style,
                        codePoint == '\n' || codePoint == '\r' ? ' ' : codePoint));
        this.obfuscated = !this.sequence.accept((index, style, codePoint) -> !style.isObfuscated());
        this.width = this.font.width(this.sequence) * this.scale;
        bake();
    }

    private void bake() {
        this.quads.clear();
        this.left = 0f;
        this.top = 0f;
        this.right = 0f;
        this.bottom = 0f;
        Collector collector = new Collector(this.scale);
        this.font.prepareText(this.sequence, 0f, 0f, 0xFFFFFFFF, false, false, 0).visit(new Font.GlyphVisitor() {
            @Override public void acceptGlyph(TextRenderable.Styled glyph) { collector.collect(glyph); }
            @Override public void acceptEffect(TextRenderable effect) { collector.collect(effect); }
        });
    }

    float width() {
        return this.width;
    }

    boolean isValid() {
        return this.font == Minecraft.getInstance().font
                && this.quads.stream().noneMatch(quad -> quad.texture().isClosed());
    }

    void draw(Canvas canvas, float x, float y, float opacity, Paint effect, boolean gradient, Atlases atlases,
              long stamp) {
        if (this.obfuscated && this.bakedAt != stamp) {
            this.bakedAt = stamp;
            bake();
        }
        Rect bounds = Rect.makeLTRB(x + this.left - this.padding, y + this.top - this.padding,
                x + this.right + this.padding, y + this.bottom + this.padding);
        try (Paint layerPaint = effect.makeClone().setShader(null)
                .setColor((Math.round(Math.clamp(opacity, 0f, 1f) * 255f) << 24) | 0xFFFFFF);
             Paint glyphPaint = new Paint().setAntiAlias(false)) {
            int layer = canvas.saveLayer(bounds, layerPaint);
            try {
                int position = canvas.save();
                try {
                    canvas.translate(x, y);
                    for (Quad quad : this.quads) {
                        Shader atlas = atlases.shader(quad.texture());
                        if (atlas == null) continue;
                        glyphPaint.setShader(atlas);
                        canvas.drawTriangles(quad.positions(), quad.colors(), quad.coordinates(), INDICES,
                                BlendMode.MODULATE, glyphPaint);
                    }
                } finally {
                    canvas.restoreToCount(position);
                }
                if (gradient) {
                    try (Paint color = effect.makeClone().setImageFilter(null).setColorFilter(null)
                            .setColor(0xFFFFFFFF).setBlendMode(BlendMode.SRC_IN)) {
                        canvas.drawRect(bounds, color);
                    }
                }
            } finally {
                canvas.restoreToCount(layer);
            }
        }
    }

    static final class Atlases implements AutoCloseable {
        private final Map<GpuTextureView, Atlas> textures = new HashMap<>();

        Shader shader(GpuTextureView view) {
            if (this.textures.containsKey(view)) {
                Atlas atlas = this.textures.get(view);
                return atlas == null ? null : atlas.shader();
            }
            ChromaGraphics.BorrowedImage image = ChromaGraphics.borrowGlyphTexture(view);
            if (image == null) {
                this.textures.put(view, null);
                return null;
            }
            Shader shader = null;
            try {
                shader = image.image().makeShader();
                if (view.texture().getFormat() == TextureFormat.RED8) {
                    Shader mask = shader;
                    shader = null;
                    try (mask; Shader white = Shader.makeColor(0xFFFFFFFF)) {
                        shader = Shader.makeBlend(BlendMode.SRC_IN, mask, white);
                    }
                }
                this.textures.put(view, new Atlas(image, shader));
                return shader;
            } catch (Throwable error) {
                if (shader != null) shader.close();
                image.close();
                throw error;
            }
        }

        @Override
        public void close() {
            for (Atlas atlas : this.textures.values()) {
                if (atlas == null) continue;
                atlas.shader().close();
                atlas.image().close();
            }
            this.textures.clear();
        }
    }

    private record Atlas(ChromaGraphics.BorrowedImage image, Shader shader) {}
    private record Quad(GpuTextureView texture, Point[] positions, Point[] coordinates, int[] colors) {}

    private static final class Vertex {
        float x;
        float y;
        float u;
        float v;
        int color = 0xFFFFFFFF;
    }

    private final class Collector implements VertexConsumer {
        private final float scale;
        private final List<Vertex> vertices = new ArrayList<>();
        private final Matrix4f identity = new Matrix4f();
        private Vertex current;

        Collector(float scale) {
            this.scale = scale;
        }

        void collect(TextRenderable renderable) {
            this.vertices.clear();
            renderable.render(this.identity, this, 0xF000F0, false);
            GpuTextureView texture = renderable.textureView();
            for (int first = 0; first + 3 < this.vertices.size(); first += 4) {
                Point[] positions = new Point[4];
                Point[] coordinates = new Point[4];
                int[] colors = new int[4];
                for (int i = 0; i < 4; i++) {
                    Vertex vertex = this.vertices.get(first + i);
                    float x = vertex.x * this.scale;
                    float y = vertex.y * this.scale;
                    positions[i] = new Point(x, y);
                    coordinates[i] = new Point(vertex.u * texture.getWidth(0), vertex.v * texture.getHeight(0));
                    colors[i] = vertex.color;
                    left = Math.min(left, x);
                    top = Math.min(top, y);
                    right = Math.max(right, x);
                    bottom = Math.max(bottom, y);
                }
                quads.add(new Quad(texture, positions, coordinates, colors));
            }
        }

        @Override public VertexConsumer addVertex(float x, float y, float z) {
            this.current = new Vertex();
            this.current.x = x;
            this.current.y = y;
            this.vertices.add(this.current);
            return this;
        }

        @Override public VertexConsumer setColor(int r, int g, int b, int a) {
            return setColor((a << 24) | (r << 16) | (g << 8) | b);
        }

        @Override public VertexConsumer setColor(int color) { this.current.color = color; return this; }
        @Override public VertexConsumer setUv(float u, float v) { this.current.u = u; this.current.v = v; return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
        @Override public VertexConsumer setLineWidth(float width) { return this; }
    }
}
