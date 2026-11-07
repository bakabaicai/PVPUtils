package com.pvp_utils.client.render.skia;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pvp_utils.Config;
import com.pvp_utils.PVPUtils;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.lwjgl.opengl.GL33.*;

public final class LiquidGlassRenderer {
    public static final int MAX_SHAPES = 16;
    private static final LiquidGlassRenderer INSTANCE = new LiquidGlassRenderer();
    private static final GlassStyle DEFAULT_STYLE = new GlassStyle();
    private static final String VERTEX_SOURCE = """
            #version 330 core
            void main() {
                vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
                gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
            }
            """;
    private final float[] rects = new float[MAX_SHAPES * 4];
    private final float[] radii = new float[MAX_SHAPES * 4];
    private final float[] optics = new float[MAX_SHAPES * 4];
    private final float[] tints = new float[MAX_SHAPES * 4];
    private GlassGlState state;
    private int program;
    private int vertexArray;
    private int inputTexture;
    private int inputFramebuffer;
    private int inputWidth;
    private int inputHeight;
    private int metaUniform;
    private int global0Uniform;
    private int global1Uniform;
    private int global2Uniform;
    private int shadowOffUniform;
    private int shadowColorUniform;
    private int rectsUniform;
    private int radiiUniform;
    private int opticsUniform;
    private int tintsUniform;
    private boolean failed;
    private boolean drawing;

    private LiquidGlassRenderer() {
    }

    public static LiquidGlassRenderer getInstance() {
        return INSTANCE;
    }

    public boolean renderPanel(Minecraft client, float x, float y, float width, float height, float radius,
                               int tintColor, GlassStyle style) {
        return renderShapes(client, List.of(GlassShape.rounded(x, y, width, height, radius, tintColor)), style);
    }

    public boolean renderShapes(Minecraft client, List<GlassShape> shapes, GlassStyle style) {
        return renderShapes(client, shapes, style, GlassShadow.NONE);
    }

    public boolean renderShapes(Minecraft client, List<GlassShape> shapes, GlassStyle style, GlassShadow shadow) {
        if (failed || drawing || client == null || client.getWindow() == null || shapes == null || shapes.isEmpty()) return false;
        if (!(RenderSystem.getDevice() instanceof GlDevice)) return false;
        GlassStyle actualStyle = style == null ? DEFAULT_STYLE : style;
        actualStyle.sanitize();
        GlassShadow actualShadow = shadow == null ? GlassShadow.NONE : shadow;
        int count = Math.min(shapes.size(), MAX_SHAPES);
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < count; i++) {
            GlassShape shape = shapes.get(i);
            if (shape == null || !shape.valid()) return false;
            minX = Math.min(minX, shape.x());
            minY = Math.min(minY, shape.y());
            maxX = Math.max(maxX, shape.x() + shape.width());
            maxY = Math.max(maxY, shape.y() + shape.height());
        }
        boolean active = SkijaRenderer.isDrawing();
        RenderTarget mainTarget = client.getMainRenderTarget();
        if (!active && mainTarget == null) return false;
        int targetWidth = active ? SkijaRenderer.currentTargetWidth() : mainTarget.width;
        int targetHeight = active ? SkijaRenderer.currentTargetHeight() : mainTarget.height;
        if (targetWidth <= 0 || targetHeight <= 0) return false;
        int guiWidth = client.getWindow().getGuiScaledWidth();
        int guiHeight = client.getWindow().getGuiScaledHeight();
        if (guiWidth <= 0 || guiHeight <= 0) return false;
        float scaleX = (float) targetWidth / guiWidth;
        float scaleY = (float) targetHeight / guiHeight;
        float padding = 2f;
        if (actualShadow.strength() > 0f) {
            padding += actualShadow.expand() * 6f + Math.max(Math.abs(actualShadow.offsetX()), Math.abs(actualShadow.offsetY())) + 32f;
        }
        int left = Math.clamp((int) Math.floor(minX * scaleX - padding), 0, targetWidth);
        int right = Math.clamp((int) Math.ceil(maxX * scaleX + padding), 0, targetWidth);
        int bottom = Math.clamp((int) Math.floor(targetHeight - maxY * scaleY - padding), 0, targetHeight);
        int top = Math.clamp((int) Math.ceil(targetHeight - minY * scaleY + padding), 0, targetHeight);
        if (left >= right || bottom >= top) return false;
        if (active) SkijaRenderer.flush();
        if (state == null) state = new GlassGlState(glGetInteger(GL_MAJOR_VERSION) * 100 + glGetInteger(GL_MINOR_VERSION) * 10);
        state.push();
        drawing = true;
        try {
            int framebuffer = active ? SkijaRenderer.currentFramebuffer() : mainFramebuffer(mainTarget);
            if (framebuffer < 0) return false;
            ensureProgram();
            ensureInput(targetWidth, targetHeight);
            glDisable(GL_FRAMEBUFFER_SRGB);
            glDisable(GL_SCISSOR_TEST);
            glBindFramebuffer(GL_READ_FRAMEBUFFER, framebuffer);
            glReadBuffer(framebuffer == 0 ? GL_BACK : GL_COLOR_ATTACHMENT0);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, inputFramebuffer);
            glDrawBuffer(GL_COLOR_ATTACHMENT0);
            glBlitFramebuffer(0, 0, targetWidth, targetHeight, 0, 0, targetWidth, targetHeight, GL_COLOR_BUFFER_BIT, GL_NEAREST);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, framebuffer);
            glDrawBuffer(framebuffer == 0 ? GL_BACK : GL_COLOR_ATTACHMENT0);
            glViewport(0, 0, targetWidth, targetHeight);
            glEnable(GL_SCISSOR_TEST);
            glScissor(left, bottom, right - left, top - bottom);
            glDisable(GL_DEPTH_TEST);
            glDisable(GL_STENCIL_TEST);
            glDisable(GL_CULL_FACE);
            glDisable(GL_PRIMITIVE_RESTART);
            glDepthMask(false);
            glColorMask(true, true, true, true);
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            glEnable(GL_BLEND);
            glBlendEquationSeparate(GL_FUNC_ADD, GL_FUNC_ADD);
            glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
            glUseProgram(program);
            glBindVertexArray(vertexArray);
            glActiveTexture(GL_TEXTURE0);
            glBindSampler(0, 0);
            glBindTexture(GL_TEXTURE_2D, inputTexture);
            glUniform4f(metaUniform, targetWidth, targetHeight, count, 0f);
            glUniform4f(global0Uniform, actualStyle.refraction, actualStyle.dispersion, actualStyle.rim, actualStyle.blur);
            glUniform4f(global1Uniform, actualStyle.alpha, actualStyle.glow, actualShadow.expand(), actualShadow.strength());
            glUniform4f(global2Uniform, actualStyle.thickness, 0f, 0f, 0f);
            glUniform4f(shadowOffUniform, actualShadow.offsetX(), actualShadow.offsetY(), 0f, 0f);
            int shadowColor = actualShadow.color();
            glUniform4f(shadowColorUniform, channel(shadowColor, 16), channel(shadowColor, 8), channel(shadowColor, 0), channel(shadowColor, 24));
            for (int i = 0; i < count; i++) fillShape(i, shapes.get(i), scaleX, scaleY, targetHeight);
            glUniform4fv(rectsUniform, rects);
            glUniform4fv(radiiUniform, radii);
            glUniform4fv(opticsUniform, optics);
            glUniform4fv(tintsUniform, tints);
            glDrawArrays(GL_TRIANGLES, 0, 3);
            return true;
        } catch (RuntimeException | IOException error) {
            failed = true;
            PVPUtils.LOGGER.error("Liquid glass rendering failed", error);
            return false;
        } finally {
            drawing = false;
            state.pop();
            if (active) SkijaRenderer.restoreContext();
        }
    }

    private void fillShape(int index, GlassShape shape, float scaleX, float scaleY, int targetHeight) {
        int offset = index * 4;
        rects[offset] = shape.x() * scaleX;
        rects[offset + 1] = targetHeight - (shape.y() + shape.height()) * scaleY;
        rects[offset + 2] = shape.width() * scaleX;
        rects[offset + 3] = shape.height() * scaleY;
        float maxRadius = Math.min(shape.width(), shape.height()) * 0.5f;
        float radiusScale = Math.min(scaleX, scaleY);
        radii[offset] = Math.clamp(shape.bottomLeft(), 0f, maxRadius) * radiusScale;
        radii[offset + 1] = Math.clamp(shape.bottomRight(), 0f, maxRadius) * radiusScale;
        radii[offset + 2] = Math.clamp(shape.topRight(), 0f, maxRadius) * radiusScale;
        radii[offset + 3] = Math.clamp(shape.topLeft(), 0f, maxRadius) * radiusScale;
        optics[offset] = Math.max(0f, shape.smoothness());
        optics[offset + 1] = 0f;
        optics[offset + 2] = 0f;
        optics[offset + 3] = Math.clamp(shape.opacity(), 0f, 1f);
        tints[offset] = channel(shape.tint(), 16);
        tints[offset + 1] = channel(shape.tint(), 8);
        tints[offset + 2] = channel(shape.tint(), 0);
        tints[offset + 3] = channel(shape.tint(), 24);
    }

    private static float channel(int color, int shift) {
        return ((color >>> shift) & 255) / 255f;
    }

    private static int mainFramebuffer(RenderTarget target) {
        if (target.getColorTexture() instanceof GlTexture texture && RenderSystem.getDevice() instanceof GlDevice device) {
            return texture.getFbo(device.directStateAccess(), target.getDepthTexture());
        }
        return -1;
    }

    private void ensureProgram() throws IOException {
        if (program != 0) return;
        String fragmentSource;
        try (InputStream input = LiquidGlassRenderer.class.getResourceAsStream("/assets/pvp_utils/shaders/liquid_glass.fsh")) {
            if (input == null) throw new IOException("Missing liquid glass shader");
            fragmentSource = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        int vertex = compile(GL_VERTEX_SHADER, VERTEX_SOURCE);
        int fragment = 0;
        int linkedProgram = 0;
        try {
            fragment = compile(GL_FRAGMENT_SHADER, fragmentSource);
            linkedProgram = glCreateProgram();
            glAttachShader(linkedProgram, vertex);
            glAttachShader(linkedProgram, fragment);
            glLinkProgram(linkedProgram);
            if (glGetProgrami(linkedProgram, GL_LINK_STATUS) == GL_FALSE) {
                throw new IllegalStateException(glGetProgramInfoLog(linkedProgram));
            }
            program = linkedProgram;
            linkedProgram = 0;
        } finally {
            glDeleteShader(vertex);
            if (fragment != 0) glDeleteShader(fragment);
            if (linkedProgram != 0) glDeleteProgram(linkedProgram);
        }
        vertexArray = glGenVertexArrays();
        metaUniform = glGetUniformLocation(program, "Meta");
        global0Uniform = glGetUniformLocation(program, "Global0");
        global1Uniform = glGetUniformLocation(program, "Global1");
        global2Uniform = glGetUniformLocation(program, "Global2");
        shadowOffUniform = glGetUniformLocation(program, "ShadowOff");
        shadowColorUniform = glGetUniformLocation(program, "ShadowColor");
        rectsUniform = glGetUniformLocation(program, "Rects[0]");
        radiiUniform = glGetUniformLocation(program, "Rads[0]");
        opticsUniform = glGetUniformLocation(program, "Optics[0]");
        tintsUniform = glGetUniformLocation(program, "Tints[0]");
        glUseProgram(program);
        glUniform1i(glGetUniformLocation(program, "Background"), 0);
    }

    private static int compile(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new IllegalStateException(log);
        }
        return shader;
    }

    private void ensureInput(int width, int height) {
        if (inputTexture != 0 && inputWidth == width && inputHeight == height) return;
        if (inputFramebuffer == 0) inputFramebuffer = glGenFramebuffers();
        if (inputTexture == 0) inputTexture = glGenTextures();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, inputTexture);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, inputFramebuffer);
        glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, inputTexture, 0);
        if (glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Incomplete liquid glass input framebuffer");
        }
        inputWidth = width;
        inputHeight = height;
    }

    public record GlassShape(float x, float y, float width, float height, float topLeft, float topRight,
                             float bottomRight, float bottomLeft, float smoothness, float opacity, int tint) {
        public static GlassShape rounded(float x, float y, float width, float height, float radius, int tint) {
            return new GlassShape(x, y, width, height, radius, radius, radius, radius, 0.3f, 1f, tint);
        }

        private boolean valid() {
            return Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(width) && Float.isFinite(height)
                    && width > 0f && height > 0f && Float.isFinite(topLeft) && Float.isFinite(topRight)
                    && Float.isFinite(bottomRight) && Float.isFinite(bottomLeft) && Float.isFinite(smoothness)
                    && Float.isFinite(opacity);
        }
    }

    public record GlassShadow(float expand, float strength, float offsetX, float offsetY, int color) {
        public static final GlassShadow NONE = new GlassShadow(0f, 0f, 0f, 0f, 0);

        public GlassShadow {
            expand = Float.isFinite(expand) ? Math.clamp(expand, 0f, 80f) : 0f;
            strength = Float.isFinite(strength) ? Math.clamp(strength, 0f, 1f) : 0f;
            offsetX = Float.isFinite(offsetX) ? Math.clamp(offsetX, -60f, 60f) : 0f;
            offsetY = Float.isFinite(offsetY) ? Math.clamp(offsetY, -60f, 60f) : 0f;
        }
    }

    public static int panelTint() {
        return Config.hudTheme == Config.HudTheme.LIGHT ? 0x33FFFFFF : 0x2E0B0F17;
    }

    public void destroy() {
        if (inputTexture != 0) glDeleteTextures(inputTexture);
        if (inputFramebuffer != 0) glDeleteFramebuffers(inputFramebuffer);
        if (vertexArray != 0) glDeleteVertexArrays(vertexArray);
        if (program != 0) glDeleteProgram(program);
        inputTexture = 0;
        inputFramebuffer = 0;
        vertexArray = 0;
        program = 0;
        inputWidth = 0;
        inputHeight = 0;
        failed = false;
    }
}
