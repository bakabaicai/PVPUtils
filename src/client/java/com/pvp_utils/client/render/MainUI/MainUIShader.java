package com.pvp_utils.client.render.MainUI;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class MainUIShader {
    private static final List<String> SHADERS = List.of(
            "Galaxy.frag.glsl",
            "Planet.frag.glsl",
            "BlackHole.frag.glsl",
            "BlueGrid.frag.glsl",
            "BlueLandscape.frag.glsl",
            "Circuits.frag.glsl",
            "CubeCave.frag.glsl",
            "GreenNebula.frag.glsl",
            "GridCave.frag.glsl",
            "Matrix.frag.glsl",
            "Minecraft.frag.glsl",
            "PurpleGrid.frag.glsl",
            "RectWaves.frag.glsl",
            "RedLandscape.frag.glsl",
            "Space.frag.glsl",
            "Tube.frag.glsl"
    );
    private static String lastRandomShader = "";
    private static int nextTextureId;
    private static final String DEFAULT_VERTEX_SOURCE = """
            #version 150 core
            in vec3 position;
            void main() {
                gl_Position = vec4(position, 1.0);
            }
            """;

    private final Identifier textureId;
    private final String fragmentPath;
    private final long startTime;
    private int program;
    private int vao;
    private int vbo;
    private int fbo;
    private int colorTexture;
    private int textureW = -1;
    private int textureH = -1;
    private DynamicTexture dynamicTexture;
    private ByteBuffer readBuffer;
    private boolean failed;
    private boolean loggedLoaded;

    private MainUIShader(String fragmentPath) {
        this.textureId = Identifier.fromNamespaceAndPath("pvp_utils", "mainui_shader_" + nextTextureId++);
        this.fragmentPath = fragmentPath;
        this.startTime = System.currentTimeMillis();
    }

    public static MainUIShader random() {
        if (SHADERS.size() <= 1) return new MainUIShader(SHADERS.get(0));
        String shader;
        do {
            shader = SHADERS.get(ThreadLocalRandom.current().nextInt(SHADERS.size()));
        } while (shader.equals(lastRandomShader));
        lastRandomShader = shader;
        return new MainUIShader(shader);
    }

    public static MainUIShader named(String fragmentPath) {
        if (fragmentPath == null || fragmentPath.isBlank() || !SHADERS.contains(fragmentPath)) return random();
        return new MainUIShader(fragmentPath);
    }

    public String fragmentPath() {
        return fragmentPath;
    }

    public void render(GuiGraphics graphics, double mouseX, double mouseY) {
        ensureCompiled();
        if (failed || program == 0) {
            fallback(graphics);
            return;
        }

        Window window = Minecraft.getInstance().getWindow();
        int fbW = window.getWidth();
        int fbH = window.getHeight();
        int guiW = window.getGuiScaledWidth();
        int guiH = window.getGuiScaledHeight();
        float scale = (float) window.getGuiScale();
        float time = (System.currentTimeMillis() - startTime) / 1000f;

        RenderSystem.assertOnRenderThread();
        ensureFramebuffer(fbW, fbH);
        if (fbo == 0 || dynamicTexture == null || readBuffer == null) {
            fallback(graphics);
            return;
        }

        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        boolean depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL11.glViewport(0, 0, fbW, fbH);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColorMask(true, true, true, true);
        GL11.glClearColor(0f, 0f, 0f, 1f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        GL20.glUseProgram(program);
        setUniform1f("time", time);
        setUniform2f("mouse", (float) mouseX * scale, (float) (window.getGuiScaledHeight() - mouseY) * scale);
        setUniform2f("resolution", fbW, fbH);
        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            System.err.println("PVPUtils MainUI shader draw GL error (" + fragmentPath + "): " + error);
        }

        readBuffer.clear();
        GL11.glReadPixels(0, 0, fbW, fbH, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, readBuffer);
        readBuffer.rewind();

        GL30.glBindVertexArray(previousVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
        GL20.glUseProgram(previousProgram);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
        GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        if (depthEnabled) GL11.glEnable(GL11.GL_DEPTH_TEST); else GL11.glDisable(GL11.GL_DEPTH_TEST);
        if (cullEnabled) GL11.glEnable(GL11.GL_CULL_FACE); else GL11.glDisable(GL11.GL_CULL_FACE);
        if (blendEnabled) GL11.glEnable(GL11.GL_BLEND); else GL11.glDisable(GL11.GL_BLEND);

        GpuTexture gpuTexture = dynamicTexture.getTexture();
        RenderSystem.getDevice().createCommandEncoder()
                .writeToTexture(gpuTexture, readBuffer, NativeImage.Format.RGBA, 0, 0, 0, 0, fbW, fbH);
        graphics.blit(textureId, 0, 0, guiW, guiH, 0f, 1f, 1f, 0f);
    }

    private void ensureCompiled() {
        if (program != 0 || failed) return;
        try {
            String vertexSource = DEFAULT_VERTEX_SOURCE;
            String fragmentSource = adaptFragmentSource(readResource("shaders/" + fragmentPath));
            int vertex = compile(GL20.GL_VERTEX_SHADER, vertexSource);
            int fragment = compile(GL20.GL_FRAGMENT_SHADER, fragmentSource);
            if (failed) return;

            program = GL20.glCreateProgram();
            GL20.glAttachShader(program, vertex);
            GL20.glAttachShader(program, fragment);
            GL20.glBindAttribLocation(program, 0, "position");
            GL30.glBindFragDataLocation(program, 0, "pvpFragColor");
            GL20.glLinkProgram(program);
            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                System.err.println("PVPUtils MainUI shader link failed (" + fragmentPath + "): " + GL20.glGetProgramInfoLog(program));
                failed = true;
                return;
            }
            GL20.glDeleteShader(vertex);
            GL20.glDeleteShader(fragment);
            createQuad();
            if (!loggedLoaded) {
                System.err.println("PVPUtils MainUI shader loaded: " + fragmentPath);
                loggedLoaded = true;
            }
        } catch (Exception e) {
            System.err.println("PVPUtils MainUI shader load failed (" + fragmentPath + "): " + e.getMessage());
            failed = true;
        }
    }

    private int compile(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.err.println("PVPUtils MainUI shader compile failed (" + fragmentPath + "): " + GL20.glGetShaderInfoLog(shader));
            failed = true;
        }
        return shader;
    }

    private void createQuad() {
        float[] vertices = {-1f, -1f, 0f, 1f, -1f, 0f, -1f, 1f, 0f, 1f, 1f, 0f};
        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0L);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    private void ensureFramebuffer(int width, int height) {
        if (width <= 0 || height <= 0) return;
        if (fbo != 0 && width == textureW && height == textureH) return;

        releaseFramebuffer();
        Minecraft client = Minecraft.getInstance();
        textureW = width;
        textureH = height;

        colorTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorTexture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        fbo = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, colorTexture, 0);
        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("PVPUtils MainUI shader framebuffer incomplete: " + GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER));
            failed = true;
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

        dynamicTexture = new DynamicTexture("pvp_utils:mainui_shader", width, height, false);
        client.getTextureManager().register(textureId, dynamicTexture);
        readBuffer = MemoryUtil.memAlloc(width * height * 4);
    }

    public void close() {
        if (program != 0) {
            GL20.glDeleteProgram(program);
            program = 0;
        }
        if (vbo != 0) {
            GL15.glDeleteBuffers(vbo);
            vbo = 0;
        }
        if (vao != 0) {
            GL30.glDeleteVertexArrays(vao);
            vao = 0;
        }
        releaseFramebuffer();
    }

    private void releaseFramebuffer() {
        if (fbo != 0) {
            GL30.glDeleteFramebuffers(fbo);
            fbo = 0;
        }
        if (colorTexture != 0) {
            GL11.glDeleteTextures(colorTexture);
            colorTexture = 0;
        }
        if (dynamicTexture != null) {
            Minecraft.getInstance().getTextureManager().release(textureId);
            dynamicTexture = null;
        }
        if (readBuffer != null) {
            MemoryUtil.memFree(readBuffer);
            readBuffer = null;
        }
        textureW = -1;
        textureH = -1;
    }

    private void setUniform1f(String name, float value) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location >= 0) GL20.glUniform1f(location, value);
    }

    private void setUniform2f(String name, float x, float y) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location >= 0) GL20.glUniform2f(location, x, y);
    }

    private String readResource(String path) {
        InputStream stream = MainUIShader.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) throw new IllegalStateException(path);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new IllegalStateException(path, e);
        }
    }

    private String adaptFragmentSource(String source) {
        String adapted = stripVersionLines(source).replace("gl_FragColor", "pvpFragColor");
        return "#version 150 core\nout vec4 pvpFragColor;\n#define gl_FragColor pvpFragColor\n" + adapted;
    }

    private String stripVersionLines(String source) {
        return source.replaceAll("(?m)^\\s*#version\\s+\\d+.*\\R?", "");
    }

    private void fallback(GuiGraphics graphics) {
        int mid = Minecraft.getInstance().getWindow().getGuiScaledHeight() / 2;
        int w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int h = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        graphics.fillGradient(0, 0, w, mid, 0xFF330000, 0xFF111111);
        graphics.fillGradient(0, mid, w, h, 0xFF111111, 0xFF330000);
        graphics.fill(18, 18, Math.min(w - 18, 420), 74, 0xCC000000);
        graphics.drawString(Minecraft.getInstance().font, Component.literal("PVPUtils MainUI fallback"), 28, 28, 0xFFFF5555, false);
        graphics.drawString(Minecraft.getInstance().font, Component.literal(fragmentPath), 28, 48, 0xFFFFFFFF, false);
    }
}
