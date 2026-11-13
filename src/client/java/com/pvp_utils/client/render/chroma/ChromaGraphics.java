package com.pvp_utils.client.render.chroma;

import io.github.humbleui.types.RRect;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.pvp_utils.mixin.client.TextureManagerAccessor;
import io.github.humbleui.skija.BackendRenderTarget;
import io.github.humbleui.skija.BackendTexture;
import io.github.humbleui.skija.ColorAlphaType;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorSpace;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.ContentChangeMode;
import io.github.humbleui.skija.DirectContext;
import io.github.humbleui.skija.FramebufferFormat;
import io.github.humbleui.skija.GLTextureInfo;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Path;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.SurfaceOrigin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class ChromaGraphics {
    private static DirectContext context;
    private static BackendRenderTarget renderTarget;
    private static Surface surface;
    private static final java.util.Map<Integer, SurfaceTarget> surfaces = new java.util.HashMap<>();

    private static int targetWidth = -1;
    private static int targetHeight = -1;
    private static int targetFramebuffer = -1;
    private static int targetSamples = -1;
    private static int targetStencilBits = -1;
    private static int themedFramebuffer = -1;
    private static GlTexture themedColorTexture;
    private static long nextMainGlCheck;
    private static long nextOverlayGlCheck;
    private static int skiaScratchTextureUnit = -1;
    private static Image frameBackdropSnapshot;
    private static boolean backdropRequested;

    private static Canvas activeCanvas;
    private static String activePass = "none";
    private static Object activeSource;
    private static Object failedSource;
    private static final java.util.List<DrawSubmission> submitted = new java.util.ArrayList<>();
    private static final java.util.List<DrawSubmission> mainSubmitted = new java.util.ArrayList<>();
    private static boolean failed;
    private static boolean backdropBlurFailed;

    private ChromaGraphics() {
    }

    public static void renderOverlay(java.util.function.Consumer<Canvas> painter) {
        if (Minecraft.getInstance().getWindow().isMinimized()) return;
        boolean captureBackdrop = backdropRequested;
        backdropRequested = false;
        paint(painter, captureBackdrop);
    }

    public static void renderMainTarget(java.util.function.Consumer<Canvas> painter) {
        if (activeCanvas != null) {
            draw(painter);
            return;
        }
        if (!isMainTargetAvailable()) return;

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget target = minecraft.getMainRenderTarget();
        Window window = minecraft.getWindow();
        paint(target.width, target.height, themedFramebuffer,
        window.getGuiScaledWidth(), window.getGuiScaledHeight(), painter, true);
    }

    public static boolean isMainTargetAvailable() {
        if (failed) return false;
        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        if (!(target.getColorTexture() instanceof GlTexture colorTexture)) {
            ChromaDiagnostics.warn("main-target-unavailable", "main-target",
                    "OpenGL color texture required; actual=" + (target.getColorTexture() == null
                            ? "null" : target.getColorTexture().getClass().getName()), null);
            return false;
        }
        if (colorTexture.isClosed() || target.width <= 0 || target.height <= 0) {
            ChromaDiagnostics.delayedWarn("main-target-unavailable", "main-target",
                    "closed=" + colorTexture.isClosed() + "; size=" + target.width + 'x' + target.height);
            return false;
        }
        try {
            boolean available = ensureThemedFramebuffer(colorTexture) > 0;
            if (available) ChromaDiagnostics.resolved("main-target-unavailable", "main-target");
            return available;
        } catch (Throwable error) {
            ChromaDiagnostics.error("main-target-unavailable", "main-target",
                    "framebuffer setup failed; texture=" + colorTexture.glId()
                            + "; size=" + target.width + 'x' + target.height, error);
            return false;
        }
    }

    private static void paint(java.util.function.Consumer<Canvas> painter, boolean captureBackdrop) {
        if (failed) return;
        Window window = Minecraft.getInstance().getWindow();
        int width = window.getWidth();
        int height = window.getHeight();
        if (width <= 0 || height <= 0 || window.isMinimized()) return;
        paint(width, height, 0, window.getGuiScaledWidth(),
        window.getGuiScaledHeight(), painter, captureBackdrop);
    }

    private static void paint(int width, int height, int framebuffer,
    float guiWidth, float guiHeight,
    java.util.function.Consumer<Canvas> painter) {
        paint(width, height, framebuffer, guiWidth, guiHeight, painter, false);
    }

    private static void paint(int width, int height, int framebuffer,
    float guiWidth, float guiHeight,
    java.util.function.Consumer<Canvas> painter,
    boolean captureBackdrop) {
        GlState previous = null;
        Image backdrop = null;
        activePass = framebuffer == 0 ? "overlay" : "main-target";
        failedSource = null;
        String step = "state-capture";
        long now = System.nanoTime();
        boolean inspectGl = now >= (framebuffer == 0 ? nextOverlayGlCheck : nextMainGlCheck);
        if (inspectGl) {
            if (framebuffer == 0) nextOverlayGlCheck = now + 1_000_000_000L;
            else nextMainGlCheck = now + 1_000_000_000L;
        }
        try {
            if (inspectGl) reportGlErrors("incoming-gl-state", activePass);
            previous = GlState.capture();
            step = "surface-setup";
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            canonicalizePixelStore();
            ensureSurface(width, height, framebuffer, 0, 0);

            context.resetGLAll();
            surface.notifyContentWillChange(ContentChangeMode.RETAIN);
            step = "backdrop-capture";
            if (captureBackdrop && !backdropBlurFailed) {
                SurfaceTarget target = surfaces.get(framebuffer);
                if (target == null) throw new IllegalStateException("Missing Chroma Graphics surface target");
                try {
                    backdrop = target.backdrop().capture(context, framebuffer, width, height);
                    frameBackdropSnapshot = backdrop;
                } catch (Throwable error) {
                    backdropBlurFailed = true;
                    ChromaDiagnostics.warn("backdrop-capture", activePass,
                            "background blur disabled; framebuffer=" + framebuffer + "; size=" + width + 'x' + height, error);
                }
            }
            Canvas canvas = surface.getCanvas();
            int save = canvas.save();
            try {
                canvas.scale(width / guiWidth, height / guiHeight);
                activeCanvas = canvas;
                step = "painter";
                painter.accept(canvas);
            } finally {
                canvas.restoreToCount(save);
            }
            step = "command-submit";
            context.flushAndSubmit(surface);
            if (inspectGl) reportGlErrors("submitted-gl-state", activePass);
            step = "font-resource-retirement";
            ChromaDraw.releaseRetiredFontResources();
        } catch (Throwable throwable) {
            failed = true;
            ChromaDiagnostics.error("render-pass", failedSource == null ? activeSource : failedSource,
                    "renderer disabled; pass=" + activePass + "; step=" + step + "; framebuffer=" + framebuffer
                            + "; size=" + width + 'x' + height + "; gui=" + guiWidth + 'x' + guiHeight, throwable);
        } finally {
            activeCanvas = null;
            activeSource = null;
            frameBackdropSnapshot = null;
            try {
                if (backdrop != null) backdrop.close();
            } catch (Throwable error) {
                ChromaDiagnostics.error("backdrop-release", activePass, "could not release snapshot", error);
            } finally {
                try {
                    if (previous != null) previous.restore();
                    if (inspectGl) reportGlErrors("restored-gl-state", activePass);
                } catch (Throwable error) {
                    failed = true;
                    ChromaDiagnostics.error("state-restore", activePass, "renderer disabled", error);
                }
                activePass = "none";
                failedSource = null;
            }
        }
    }

    private static int ensureThemedFramebuffer(GlTexture colorTexture) {
        if (themedFramebuffer != -1 && themedColorTexture == colorTexture) {
            return themedFramebuffer;
        }
        GlState previous = GlState.capture();
        try {
            reportGlErrors("incoming-gl-state", "main-framebuffer-setup");
            if (themedFramebuffer != -1) {
                SurfaceTarget previousTarget = surfaces.remove(themedFramebuffer);
                if (previousTarget != null) previousTarget.close();
                if (targetFramebuffer == themedFramebuffer) {
                    surface = null;
                    renderTarget = null;
                    targetFramebuffer = -1;
                }
                GL30.glDeleteFramebuffers(themedFramebuffer);
            }
            themedFramebuffer = GL30.glGenFramebuffers();
            themedColorTexture = null;
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, themedFramebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, colorTexture.glId(), 0);
            int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
            reportGlErrors("submitted-gl-state", "main-framebuffer-setup");
            if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
                GL30.glDeleteFramebuffers(themedFramebuffer);
                themedFramebuffer = -1;
                ChromaDiagnostics.error("main-target-unavailable", "main-target",
                        "incomplete framebuffer; status=0x" + Integer.toHexString(status)
                                + "; texture=" + colorTexture.glId() + "; size=" + colorTexture.getWidth(0)
                                + 'x' + colorTexture.getHeight(0), null);
            } else {
                themedColorTexture = colorTexture;
                ChromaDiagnostics.resolved("main-target-unavailable", "main-target");
            }
            return themedFramebuffer;
        } finally {
            previous.restore();
            reportGlErrors("restored-gl-state", "main-framebuffer-setup");
        }
    }

    public static void draw(java.util.function.Consumer<Canvas> painter) {
        draw(painter.getClass(), painter);
    }

    public static void draw(String source, java.util.function.Consumer<Canvas> painter) {
        draw(source == null ? painter.getClass() : (Object) source, painter);
    }

    private static void draw(Object source, java.util.function.Consumer<Canvas> painter) {
        if (failed) return;
        if (activeCanvas == null) {
            mainSubmitted.add(new DrawSubmission(source, painter));
            return;
        }
        Object previousSource = activeSource;
        activeSource = source;
        int save = activeCanvas.save();
        try {
            painter.accept(activeCanvas);
        } catch (Throwable error) {
            if (failedSource == null) failedSource = source;
            throw error;
        } finally {
            try {
                activeCanvas.restoreToCount(save);
            } finally {
                activeSource = previousSource;
            }
        }
    }

    public static void submit(java.util.function.Consumer<Canvas> painter) {
        submitted.add(new DrawSubmission(painter.getClass(), painter));
    }

    public static void submit(String source, java.util.function.Consumer<Canvas> painter) {
        submitted.add(new DrawSubmission(source == null ? painter.getClass() : source, painter));
    }

    public static void beginFrame() {
        submitted.clear();
        mainSubmitted.clear();
    }

    public static void renderMainSubmitted() {
        if (mainSubmitted.isEmpty()) return;
        java.util.List<DrawSubmission> painters = java.util.List.copyOf(mainSubmitted);
        mainSubmitted.clear();
        renderMainTarget(canvas -> {
            for (DrawSubmission painter : painters) draw(painter.source(), painter.painter());
        });
    }

    public static void renderSubmitted() {
        for (DrawSubmission painter : java.util.List.copyOf(submitted)) draw(painter.source(), painter.painter());
        submitted.clear();
    }

    public static boolean isDrawing() {
        return activeCanvas != null;
    }

    public static boolean hasFailed() {
        return failed;
    }

    public static void drawBlurredBackdrop(Canvas canvas, RRect clip,
    float x, float y, float width, float height,
    float strength) {
        drawBlurredBackdrop(canvas, clip == null ? null : target -> target.clipRRect(clip, true),
        x, y, width, height, strength);
    }

    public static void drawBlurredBackdrop(Canvas canvas, Path clip,
    float x, float y, float width, float height,
    float strength) {
        drawBlurredBackdrop(canvas, clip == null ? null : target -> target.clipPath(clip, true),
        x, y, width, height, strength);
    }

    private static void drawBlurredBackdrop(Canvas canvas,
    java.util.function.Consumer<Canvas> clipper,
    float x, float y, float width, float height,
    float strength) {
        if (backdropBlurFailed || strength <= 0.01F
        || width <= 1.0F || height <= 1.0F || targetWidth <= 0 || targetHeight <= 0) {
            return;
        }
        if (canvas == null || clipper == null || !Float.isFinite(x) || !Float.isFinite(y)
                || !Float.isFinite(width) || !Float.isFinite(height) || !Float.isFinite(strength)) {
            ChromaDiagnostics.warn("backdrop-arguments", activeSource,
                    "invalid canvas, clip or bounds; pass=" + activePass + "; bounds=" + x + ',' + y
                            + ',' + width + ',' + height + "; strength=" + strength, null);
            return;
        }
        ChromaDiagnostics.resolved("backdrop-arguments", activeSource);
        backdropRequested = true;
        if (frameBackdropSnapshot == null) {
            ChromaDiagnostics.delayedWarn("backdrop-missing", activeSource,
                    "snapshot unavailable; pass=" + activePass + "; framebuffer=" + targetFramebuffer);
            return;
        }
        ChromaDiagnostics.resolved("backdrop-missing", activeSource);

        Window window = Minecraft.getInstance().getWindow();
        float guiWidth = window.getGuiScaledWidth();
        float guiHeight = window.getGuiScaledHeight();
        if (guiWidth <= 0.0F || guiHeight <= 0.0F) return;

        try {
            ChromaBackdrop.draw(canvas, frameBackdropSnapshot, clipper,
            x, y, width, height, guiWidth, guiHeight, strength);
        } catch (Throwable throwable) {
            backdropBlurFailed = true;
            ChromaDiagnostics.warn("backdrop-draw", activeSource,
                    "background blur disabled; pass=" + activePass + "; bounds=" + x + ',' + y
                            + ',' + width + ',' + height + "; strength=" + strength, throwable);
        }
    }

    public static BorrowedImage borrowTexture(Identifier identifier) {
        if (context == null) return null;
        if (identifier == null) {
            ChromaDiagnostics.warn("texture-arguments", activeSource, "texture identifier is null", null);
            return null;
        }
        BackendTexture backend = null;
        try {
            TextureManager manager = Minecraft.getInstance().getTextureManager();
            AbstractTexture texture = ((TextureManagerAccessor) manager).pvp_utils$getLoadedTextures().get(identifier);
            if (texture == null) {
                GlState previous = GlState.capture();
                try {
                    GL13.glActiveTexture(GL13.GL_TEXTURE0);
                    GlStateManager._activeTexture(GL13.GL_TEXTURE0);
                    GlStateManager._bindTexture(previous.texture2d()[0]);
                    texture = manager.getTexture(identifier);
                } finally {
                    try {
                        previous.restore();
                    } finally {
                        canonicalizePixelStore();
                        context.resetGLAll();
                    }
                }
            }
            if (!(texture.getTexture() instanceof GlTexture glTexture) || glTexture.isClosed()) {
                ChromaDiagnostics.delayedWarn("texture-borrow", identifier,
                        "OpenGL texture is unavailable or closed; pass=" + activePass);
                return null;
            }
            int width = glTexture.getWidth(0);
            int height = glTexture.getHeight(0);
            if (width <= 0 || height <= 0) {
                ChromaDiagnostics.delayedWarn("texture-borrow", identifier,
                        "invalid texture size=" + width + 'x' + height + "; pass=" + activePass);
                return null;
            }

            backend = BackendTexture.makeGL(width, height, false,
            new GLTextureInfo(GL11.GL_TEXTURE_2D, glTexture.glId(), GL11.GL_RGBA8));
            Image image = Image.borrowTextureFrom(context, backend, SurfaceOrigin.TOP_LEFT,
            ColorType.RGBA_8888, ColorAlphaType.UNPREMUL, ColorSpace.getSRGB(), () -> { });
            if (image == null) {
                backend.close();
                ChromaDiagnostics.warn("texture-borrow", identifier,
                        "Skia rejected texture=" + glTexture.glId() + "; size=" + width + 'x' + height
                                + "; pass=" + activePass, null);
                return null;
            }
            ChromaDiagnostics.resolved("texture-borrow", identifier);
            return new BorrowedImage(image, backend);
        } catch (Throwable throwable) {
            if (backend != null) {
                try {
                    backend.close();
                } catch (Throwable cleanupError) {
                    throwable.addSuppressed(cleanupError);
                }
            }
            ChromaDiagnostics.warn("texture-borrow", identifier,
                    "texture wrapper creation failed; pass=" + activePass
                            + "; source=" + ChromaDiagnostics.sourceName(activeSource), throwable);
            return null;
        }
    }

    public static void close() {
        backdropRequested = false;
        frameBackdropSnapshot = null;
        closeSurface();
        if (themedFramebuffer != -1) {
            GL30.glDeleteFramebuffers(themedFramebuffer);
            themedFramebuffer = -1;
            themedColorTexture = null;
        }
        if (context != null) {
            ChromaDraw.close();
            context.close();
        }
        context = null;
        skiaScratchTextureUnit = -1;
    }

    private static void reportGlErrors(String event, String source) {
        int error = GL11.glGetError();
        if (error == GL11.GL_NO_ERROR) {
            ChromaDiagnostics.resolved(event, source);
            return;
        }
        StringBuilder codes = new StringBuilder();
        int count = 0;
        while (error != GL11.GL_NO_ERROR && count++ < 8) {
            if (!codes.isEmpty()) codes.append(',');
            codes.append("0x").append(Integer.toHexString(error));
            error = GL11.glGetError();
        }
        ChromaDiagnostics.warn(event, source, "OpenGL errors=" + codes, null);
    }

    private static void ensureSurface(int width, int height, int framebuffer, int samples, int stencilBits) {
        if (context == null) context = DirectContext.makeGL();
        SurfaceTarget target = surfaces.get(framebuffer);
        if (target != null && (target.width() != width || target.height() != height
        || target.samples() != samples || target.stencilBits() != stencilBits)) {
            target.close();
            surfaces.remove(framebuffer);
            target = null;
        }
        if (target == null) {
            BackendRenderTarget backend = BackendRenderTarget.makeGL(width, height, samples, stencilBits,
            framebuffer, FramebufferFormat.GR_GL_RGBA8);
            Surface created;
            try {
                created = ChromaBackdrop.wrap(context, backend);
                if (created == null) throw new IllegalStateException("Failed to wrap framebuffer " + framebuffer);
            } catch (Throwable error) {
                backend.close();
                throw error;
            }
            target = new SurfaceTarget(width, height, samples, stencilBits, backend, created, new ChromaBackdrop());
            surfaces.put(framebuffer, target);
        }
        renderTarget = target.backend();
        surface = target.surface();
        targetWidth = width;
        targetHeight = height;
        targetFramebuffer = framebuffer;
        targetSamples = samples;
        targetStencilBits = stencilBits;
    }

    private static void closeSurface() {
        surfaces.values().forEach(SurfaceTarget::close);
        surfaces.clear();
        surface = null;
        renderTarget = null;
        targetWidth = -1;
        targetHeight = -1;
        targetFramebuffer = -1;
        targetSamples = -1;
        targetStencilBits = -1;
    }

    private record DrawSubmission(Object source, java.util.function.Consumer<Canvas> painter) {
    }

    private record SurfaceTarget(int width, int height, int samples, int stencilBits,
    BackendRenderTarget backend, Surface surface, ChromaBackdrop backdrop) implements AutoCloseable {
        @Override
        public void close() {
            backdrop.close();
            surface.close();
            backend.close();
        }
    }

    public static final class BorrowedImage implements AutoCloseable {
        private final Image image;
        private final BackendTexture backend;

        private BorrowedImage(Image image, BackendTexture backend) {
            this.image = image;
            this.backend = backend;
        }

        public Image image() {
            return image;
        }

        @Override
        public void close() {
            try {
                image.close();
            } finally {
                backend.close();
            }
        }
    }

    private static void setEnabled(int capability, boolean enabled) {
        if (enabled) {
            GL11.glEnable(capability);
        } else {
            GL11.glDisable(capability);
        }
    }

    private static void canonicalizePixelStore() {
        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);

        GL11.glPixelStorei(GL11.GL_PACK_SWAP_BYTES, GL11.GL_FALSE);
        GL11.glPixelStorei(GL11.GL_PACK_LSB_FIRST, GL11.GL_FALSE);
        GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL12.GL_PACK_IMAGE_HEIGHT, 0);
        GL11.glPixelStorei(GL12.GL_PACK_SKIP_IMAGES, 0);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);

        GL11.glPixelStorei(GL11.GL_UNPACK_SWAP_BYTES, GL11.GL_FALSE);
        GL11.glPixelStorei(GL11.GL_UNPACK_LSB_FIRST, GL11.GL_FALSE);
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL12.GL_UNPACK_IMAGE_HEIGHT, 0);
        GL11.glPixelStorei(GL12.GL_UNPACK_SKIP_IMAGES, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
    }

    private record GlState(
    int program,
    int vertexArray,
    int arrayBuffer,
    int elementArrayBuffer,
    int pixelPackBuffer,
    int pixelUnpackBuffer,
    int renderbuffer,
    int drawFramebuffer,
    int readFramebuffer,
    int activeTexture,
    int[] textureUnits,
    int[] texture2d,
    int[] samplers,
    int[] viewport,
    int[] scissorBox,
    boolean blend,
    boolean depthTest,
    boolean scissorTest,
    boolean stencilTest,
    boolean cullFace,
    boolean framebufferSrgb,
    boolean polygonOffsetFill,
    boolean multisample,
    boolean programPointSize,
    boolean dither,
    int polygonMode,
    float polygonOffsetFactor,
    float polygonOffsetUnits,
    float lineWidth,
    int blendSrcRgb,
    int blendDstRgb,
    int blendSrcAlpha,
    int blendDstAlpha,
    int blendEquationRgb,
    int blendEquationAlpha,
    boolean depthMask,
    int depthFunc,
    int cullFaceMode,
    int frontFace,
    int stencilClearValue,
    StencilFaceState frontStencil,
    StencilFaceState backStencil,
    boolean[] colorMask,
    int[] packStore,
    int[] unpackStore) {
        private static GlState capture() {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer viewportBuffer = stack.mallocInt(4);
                IntBuffer scissorBuffer = stack.mallocInt(4);
                IntBuffer polygonModeBuffer = stack.mallocInt(2);
                ByteBuffer colorMaskBuffer = stack.malloc(4);
                GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewportBuffer);
                GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBuffer);
                GL11.glGetIntegerv(GL11.GL_POLYGON_MODE, polygonModeBuffer);
                GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, colorMaskBuffer);

                int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
                if (skiaScratchTextureUnit < 0) {
                    skiaScratchTextureUnit = Math.min(32, GL11.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS)) - 1;
                }
                int activeUnit = activeTexture - GL13.GL_TEXTURE0;
                int baseUnits = Math.min(12, skiaScratchTextureUnit + 1);
                boolean extraScratch = skiaScratchTextureUnit >= baseUnits;
                boolean extraActive = activeUnit >= baseUnits && activeUnit != skiaScratchTextureUnit;
                int[] textureUnits = new int[baseUnits + (extraScratch ? 1 : 0) + (extraActive ? 1 : 0)];
                for (int i = 0; i < baseUnits; i++) textureUnits[i] = i;
                if (extraScratch) textureUnits[baseUnits] = skiaScratchTextureUnit;
                if (extraActive) textureUnits[textureUnits.length - 1] = activeUnit;
                int[] textures = new int[textureUnits.length];
                int[] samplers = new int[textureUnits.length];
                for (int unit = 0; unit < textures.length; unit++) {
                    GL13.glActiveTexture(GL13.GL_TEXTURE0 + textureUnits[unit]);
                    textures[unit] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
                    samplers[unit] = GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);
                }
                GL13.glActiveTexture(activeTexture);

                return new GlState(
                GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
                GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING),
                GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING),
                GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING),
                GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING),
                GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING),
                GL11.glGetInteger(GL30.GL_RENDERBUFFER_BINDING),
                GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),
                GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),
                activeTexture,
                textureUnits,
                textures,
                samplers,
                new int[]{viewportBuffer.get(0), viewportBuffer.get(1), viewportBuffer.get(2), viewportBuffer.get(3)},
                new int[]{scissorBuffer.get(0), scissorBuffer.get(1), scissorBuffer.get(2), scissorBuffer.get(3)},
                GL11.glIsEnabled(GL11.GL_BLEND),
                GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                GL11.glIsEnabled(GL11.GL_SCISSOR_TEST),
                GL11.glIsEnabled(GL11.GL_STENCIL_TEST),
                GL11.glIsEnabled(GL11.GL_CULL_FACE),
                GL11.glIsEnabled(GL30.GL_FRAMEBUFFER_SRGB),
                GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL),
                GL11.glIsEnabled(GL13.GL_MULTISAMPLE),
                GL11.glIsEnabled(GL32.GL_PROGRAM_POINT_SIZE),
                GL11.glIsEnabled(GL11.GL_DITHER),
                polygonModeBuffer.get(0),
                GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_FACTOR),
                GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_UNITS),
                GL11.glGetFloat(GL11.GL_LINE_WIDTH),
                GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
                GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA),
                GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB),
                GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA),
                GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
                GL11.glGetInteger(GL11.GL_DEPTH_FUNC),
                GL11.glGetInteger(GL11.GL_CULL_FACE_MODE),
                GL11.glGetInteger(GL11.GL_FRONT_FACE),
                GL11.glGetInteger(GL11.GL_STENCIL_CLEAR_VALUE),
                captureStencilFace(false),
                captureStencilFace(true),
                new boolean[]{
                    colorMaskBuffer.get(0) != 0,
                    colorMaskBuffer.get(1) != 0,
                    colorMaskBuffer.get(2) != 0,
                    colorMaskBuffer.get(3) != 0
                },
                capturePackStore(),
                captureUnpackStore());
            }
        }

        private static int[] capturePackStore() {
            return new int[]{
                GL11.glGetInteger(GL11.GL_PACK_SWAP_BYTES),
                GL11.glGetInteger(GL11.GL_PACK_LSB_FIRST),
                GL11.glGetInteger(GL11.GL_PACK_ROW_LENGTH),
                GL11.glGetInteger(GL11.GL_PACK_SKIP_ROWS),
                GL11.glGetInteger(GL11.GL_PACK_SKIP_PIXELS),
                GL11.glGetInteger(GL12.GL_PACK_IMAGE_HEIGHT),
                GL11.glGetInteger(GL12.GL_PACK_SKIP_IMAGES),
                GL11.glGetInteger(GL11.GL_PACK_ALIGNMENT)
            };
        }

        private static int[] captureUnpackStore() {
            return new int[]{
                GL11.glGetInteger(GL11.GL_UNPACK_SWAP_BYTES),
                GL11.glGetInteger(GL11.GL_UNPACK_LSB_FIRST),
                GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH),
                GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS),
                GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS),
                GL11.glGetInteger(GL12.GL_UNPACK_IMAGE_HEIGHT),
                GL11.glGetInteger(GL12.GL_UNPACK_SKIP_IMAGES),
                GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT)
            };
        }

        private static StencilFaceState captureStencilFace(boolean back) {
            return new StencilFaceState(
            GL11.glGetInteger(back ? GL20.GL_STENCIL_BACK_FUNC : GL11.GL_STENCIL_FUNC),
            GL11.glGetInteger(back ? GL20.GL_STENCIL_BACK_REF : GL11.GL_STENCIL_REF),
            GL11.glGetInteger(back
            ? GL20.GL_STENCIL_BACK_VALUE_MASK : GL11.GL_STENCIL_VALUE_MASK),
            GL11.glGetInteger(back
            ? GL20.GL_STENCIL_BACK_WRITEMASK : GL11.GL_STENCIL_WRITEMASK),
            GL11.glGetInteger(back ? GL20.GL_STENCIL_BACK_FAIL : GL11.GL_STENCIL_FAIL),
            GL11.glGetInteger(back
            ? GL20.GL_STENCIL_BACK_PASS_DEPTH_FAIL
            : GL11.GL_STENCIL_PASS_DEPTH_FAIL),
            GL11.glGetInteger(back
            ? GL20.GL_STENCIL_BACK_PASS_DEPTH_PASS
            : GL11.GL_STENCIL_PASS_DEPTH_PASS));
        }

        private void restore() {
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
            GL20.glUseProgram(program);
            GL30.glBindVertexArray(vertexArray);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, elementArrayBuffer);
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, pixelPackBuffer);
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, pixelUnpackBuffer);
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, renderbuffer);
            restorePixelStore();
            for (int unit = 0; unit < texture2d.length; unit++) {
                int index = textureUnits[unit];
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + index);
                if (index < 12) GlStateManager._activeTexture(GL13.GL_TEXTURE0 + index);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture2d[unit]);
                if (index < 12) GlStateManager._bindTexture(texture2d[unit]);
                GL33.glBindSampler(index, samplers[unit]);
            }
            GL13.glActiveTexture(activeTexture);
            GlStateManager._activeTexture(activeTexture);
            GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            GL11.glScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
            GL14.glBlendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
            GL20.glBlendEquationSeparate(blendEquationRgb, blendEquationAlpha);
            GL11.glDepthMask(depthMask);
            GL11.glDepthFunc(depthFunc);
            GlStateManager._blendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
            GlStateManager._depthMask(depthMask);
            GlStateManager._depthFunc(depthFunc);
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, polygonMode);
            GL11.glPolygonOffset(polygonOffsetFactor, polygonOffsetUnits);
            GlStateManager._polygonOffset(polygonOffsetFactor, polygonOffsetUnits);
            GL11.glLineWidth(lineWidth);
            GL11.glCullFace(cullFaceMode);
            GL11.glFrontFace(frontFace);
            GL11.glClearStencil(stencilClearValue);
            restoreStencilFace(GL11.GL_FRONT, frontStencil);
            restoreStencilFace(GL11.GL_BACK, backStencil);
            GL11.glColorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
            GlStateManager._colorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
            setEnabled(GL11.GL_BLEND, blend);
            setEnabled(GL11.GL_DEPTH_TEST, depthTest);
            setEnabled(GL11.GL_SCISSOR_TEST, scissorTest);
            setEnabled(GL11.GL_STENCIL_TEST, stencilTest);
            setEnabled(GL11.GL_CULL_FACE, cullFace);
            setEnabled(GL30.GL_FRAMEBUFFER_SRGB, framebufferSrgb);
            setEnabled(GL11.GL_POLYGON_OFFSET_FILL, polygonOffsetFill);
            setEnabled(GL13.GL_MULTISAMPLE, multisample);
            setEnabled(GL32.GL_PROGRAM_POINT_SIZE, programPointSize);
            setEnabled(GL11.GL_DITHER, dither);
            if (blend) GlStateManager._enableBlend(); else GlStateManager._disableBlend();
            if (depthTest) GlStateManager._enableDepthTest(); else GlStateManager._disableDepthTest();
            if (scissorTest) GlStateManager._enableScissorTest(); else GlStateManager._disableScissorTest();
            if (cullFace) GlStateManager._enableCull(); else GlStateManager._disableCull();
            if (polygonOffsetFill) GlStateManager._enablePolygonOffset(); else GlStateManager._disablePolygonOffset();
        }

        private static void restoreStencilFace(int face, StencilFaceState state) {
            GL20.glStencilFuncSeparate(face, state.function(), state.reference(), state.valueMask());
            GL20.glStencilMaskSeparate(face, state.writeMask());
            GL20.glStencilOpSeparate(face, state.stencilFail(), state.depthFail(), state.depthPass());
        }

        private void restorePixelStore() {
            GL11.glPixelStorei(GL11.GL_PACK_SWAP_BYTES, packStore[0]);
            GL11.glPixelStorei(GL11.GL_PACK_LSB_FIRST, packStore[1]);
            GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, packStore[2]);
            GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS, packStore[3]);
            GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS, packStore[4]);
            GL11.glPixelStorei(GL12.GL_PACK_IMAGE_HEIGHT, packStore[5]);
            GL11.glPixelStorei(GL12.GL_PACK_SKIP_IMAGES, packStore[6]);
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, packStore[7]);

            GL11.glPixelStorei(GL11.GL_UNPACK_SWAP_BYTES, unpackStore[0]);
            GL11.glPixelStorei(GL11.GL_UNPACK_LSB_FIRST, unpackStore[1]);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, unpackStore[2]);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, unpackStore[3]);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, unpackStore[4]);
            GL11.glPixelStorei(GL12.GL_UNPACK_IMAGE_HEIGHT, unpackStore[5]);
            GL11.glPixelStorei(GL12.GL_UNPACK_SKIP_IMAGES, unpackStore[6]);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, unpackStore[7]);
        }
    }

    private record StencilFaceState(int function, int reference, int valueMask,
    int writeMask, int stencilFail,
    int depthFail, int depthPass) {
    }
}
