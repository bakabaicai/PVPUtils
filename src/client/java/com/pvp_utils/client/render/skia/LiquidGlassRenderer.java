package com.pvp_utils.client.render.skia;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pvp_utils.Config;
import io.github.humbleui.skija.*;
import io.github.humbleui.types.IRect;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;

public final class LiquidGlassRenderer {
    private static final LiquidGlassRenderer INSTANCE = new LiquidGlassRenderer();
    private final Paint blurPaint = new Paint().setAntiAlias(true);
    private final Paint blitPaint = new Paint().setAntiAlias(true);
    private final Paint highlightPaint = new Paint().setAntiAlias(true);
    private final Paint shadowPaint = new Paint().setAntiAlias(true);
    private final Paint tintPaint = new Paint().setAntiAlias(true);

    private float highlightAngle = (float) (Math.PI * 0.25);

    private RuntimeEffect lensEffect;
    private RuntimeEffect highlightEffect;
    private ImageFilter glassLinearizeFilter;
    private ImageFilter glassVibrancyFilter;
    private ImageFilter glassBlurFilter;
    private ImageFilter glassEncodeFilter;
    private float glassFilterSigma = Float.NaN;
    private boolean effectsFailed = false;
    private final Surface[] offscreenSurfaces = new Surface[4];
    private final int[] surfaceWidths = { -1, -1, -1, -1 };
    private final int[] surfaceHeights = { -1, -1, -1, -1 };

    private static final String ROUNDED_RECT_SDF = """
        float radiusAt(float2 coord, float4 radii) {
            if (coord.x >= 0.0) {
                if (coord.y <= 0.0) return radii.y;
                else return radii.z;
            } else {
                if (coord.y <= 0.0) return radii.x;
                else return radii.w;
            }
        }

        float sdRoundedRect(float2 coord, float2 halfSize, float radius) {
            float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
            float outside = length(max(cornerCoord, float2(0.0))) - radius;
            float inside = min(max(cornerCoord.x, cornerCoord.y), 0.0);
            return outside + inside;
        }

        float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {
            float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
            if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {
                float2 m = max(cornerCoord, float2(0.0));
                float len = length(m);
                return len > 1e-5 ? sign(coord) * (m / len) : float2(0.0);
            } else {
                float gradX = step(cornerCoord.y, cornerCoord.x);
                return sign(coord) * float2(gradX, 1.0 - gradX);
            }
        }
        """;

    private static final String LENS_SHADER = """
        uniform shader content;

        uniform float2 size;
        uniform float2 offset;
        uniform float4 cornerRadii;
        uniform float refractionHeight;
        uniform float refractionAmount;
        uniform float ior;
        uniform float depthEffect;
        uniform float chromaticAberration;

        """ + ROUNDED_RECT_SDF + """

        half4 main(float2 coord) {
            float2 halfSize = size * 0.5;
            float2 centeredCoord = (coord + offset) - halfSize;
            float radius = radiusAt(coord + offset, cornerRadii);

            float sd = sdRoundedRect(centeredCoord, halfSize, radius);
            if (-sd >= refractionHeight) {
                return content.eval(coord);
            }
            sd = min(sd, 0.0);

            float depth = -sd;
            float xR = clamp(1.0 - depth / refractionHeight, 0.0, 1.0);
            float thetaI = asin(clamp(pow(xR, 2.0), 0.0, 1.0));
            float thetaT = asin(clamp(sin(thetaI) / max(ior, 1.001), 0.0, 1.0));
            float edgeFactor = -tan(thetaT - thetaI);
            float d = edgeFactor * refractionAmount;
            float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
            float2 grad = normalize(gradSdRoundedRect(centeredCoord, halfSize, gradRadius) + depthEffect * normalize(centeredCoord));

            float2 refractedCoord = coord + d * grad;
            float dispersionIntensity = chromaticAberration * ((centeredCoord.x * centeredCoord.y) / (halfSize.x * halfSize.y));
            float2 dispersedCoord = d * grad * dispersionIntensity;
            if (dot(dispersedCoord, dispersedCoord) < 0.25) {
                return content.eval(refractedCoord);
            }

            half4 color = half4(0.0);
            half4 red = content.eval(refractedCoord + dispersedCoord);
            color.r += red.r / 3.5;
            color.a += red.a / 7.0;
            half4 orange = content.eval(refractedCoord + dispersedCoord * (2.0 / 3.0));
            color.r += orange.r / 3.5;
            color.g += orange.g / 7.0;
            color.a += orange.a / 7.0;
            half4 yellow = content.eval(refractedCoord + dispersedCoord * (1.0 / 3.0));
            color.r += yellow.r / 3.5;
            color.g += yellow.g / 3.5;
            color.a += yellow.a / 7.0;
            half4 green = content.eval(refractedCoord);
            color.g += green.g / 3.5;
            color.a += green.a / 7.0;
            half4 cyan = content.eval(refractedCoord - dispersedCoord * (1.0 / 3.0));
            color.g += cyan.g / 3.5;
            color.b += cyan.b / 3.0;
            color.a += cyan.a / 7.0;
            half4 blue = content.eval(refractedCoord - dispersedCoord * (2.0 / 3.0));
            color.b += blue.b / 3.0;
            color.a += blue.a / 7.0;
            half4 purple = content.eval(refractedCoord - dispersedCoord);
            color.r += purple.r / 7.0;
            color.b += purple.b / 3.0;
            color.a += purple.a / 7.0;
            return color;
        }
        """;

    private static final String HIGHLIGHT_SHADER = """
        uniform float2 size;
        uniform float4 cornerRadii;
        uniform float4 color;
        uniform float angle;
        uniform float falloff;
        uniform float taper;

        """ + ROUNDED_RECT_SDF + """

        half4 main(float2 coord) {
            float2 halfSize = size * 0.5;
            float2 centeredCoord = coord - halfSize;
            float radius = radiusAt(coord, cornerRadii);

            float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
            float2 grad = gradSdRoundedRect(centeredCoord, halfSize, gradRadius);
            float2 normal = float2(cos(angle), sin(angle));
            float d = dot(grad, normal);
            float intensity = pow(abs(d), falloff);
            if (taper > 0.0) {
                float2 tangent = float2(-normal.y, normal.x);
                float along = dot(centeredCoord, tangent) / max(halfSize.x, halfSize.y);
                intensity *= exp(-pow(along * taper, 2.0));
            }
            return half4(color * intensity);
        }
        """;

    private LiquidGlassRenderer() {}

    public static LiquidGlassRenderer getInstance() {
        return INSTANCE;
    }

    public void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null && client.getWindow() != null) {
            updateHighlightAngle(client);
        }
    }

    public boolean renderPanel(Minecraft client, float x, float y, float width, float height, float radius,
                               int tintColor, boolean shadow, boolean highlight, float highlightTaper, int slot) {
        if (width <= 0f || height <= 0f) return false;
        if (client == null || client.getWindow() == null || client.getMainRenderTarget() == null) return false;
        int framebufferId = mainFramebufferId(client);
        if (framebufferId == 0) return false;

        SkiaBlurRenderer blur = SkiaBlurRenderer.getInstance();
        Canvas canvas = blur.beginFrame(framebufferId);
        if (canvas == null) return false;
        try {
            DirectContext context = blur.context();
            if (!ensureRuntimeEffects()) return false;
            drawGlass(canvas, context, client, slot, x, y, width, height, radius, tintColor, shadow, highlight, highlightTaper);
            return true;
        } finally {
            blur.endFrame();
        }
    }

    private void drawGlass(Canvas canvas, DirectContext context, Minecraft client, int slot,
                           float x, float y, float width, float height, float radius,
                           int tintColor, boolean shadow, boolean highlight, float highlightTaper) {
        float blurSigma = Math.max(0.001f, Config.liquidGlassBlur * 10.5f / 2f);
        float padding = Math.max(18f, blurSigma * 2f);

        SkiaBlurRenderer.Capture capture = SkiaBlurRenderer.getInstance().capture(client, x, y, width, height, padding);
        if (capture.image == null) return;

        Image glass = buildGlass(context, capture, slot, width, height, radius, blurSigma, padding);
        if (glass == null) {
            capture.image.close();
            return;
        }

        if (shadow) {
            renderShadow(canvas, x, y, width, height, radius);
        }

        float scale = capture.dstW > 0f ? capture.width / capture.dstW : 1f;
        float renderScale = scale * renderPrecision();

        canvas.save();
        try {
            canvas.translate(x, y);
            canvas.clipRRect(RRect.makeXYWH(0, 0, width, height, radius), true);
            canvas.drawImageRect(glass,
                    Rect.makeXYWH(padding * renderScale, padding * renderScale, width * renderScale, height * renderScale),
                    Rect.makeXYWH(0, 0, width, height),
                    SamplingMode.LINEAR,
                    blitPaint,
                    true);

            tintPaint.setColor(tintColor);
            canvas.drawRRect(RRect.makeXYWH(0, 0, width, height, radius), tintPaint);

            if (highlight) {
                renderHighlight(canvas, width, height, radius, highlightTaper);
            }
        } finally {
            canvas.restore();
            glass.close();
            capture.image.close();
        }
    }

    private Image buildGlass(DirectContext context, SkiaBlurRenderer.Capture capture, int slot,
                             float width, float height, float radius, float blurSigma, float padding) {
        float baseScale = capture.dstW > 0f ? capture.width / capture.dstW : 1f;
        float precision = renderPrecision();
        float scale = baseScale * precision;

        int reqW = Math.max(1, Math.round(capture.width * precision));
        int reqH = Math.max(1, Math.round(capture.height * precision));
        ensureOffscreenSurface(context, slot, reqW, reqH);
        Surface surface = offscreenSurfaces[slot];
        if (surface == null) return null;

        float padPx = padding * scale;

        ImageFilter encodeFilter = ensureGlassFilters(blurSigma);
        if (encodeFilter == null) return null;

        float minSide = Math.min(width, height);
        float ior = Math.max(1.001f, Config.liquidGlassIoR);
        float refHeight = Math.min(minSide * Math.max(0f, Config.liquidGlassRefractionHeight), radius);
        float refAmount = minSide * Math.max(0f, Config.liquidGlassRefractionAmount);
        refAmount = Math.min(refAmount, refHeight * 0.9f / Math.max(1f, ior));

        float radiusPx = radius * scale;
        RuntimeEffectBuilder builder = new RuntimeEffectBuilder(lensEffect);
        builder.setUniform("size", width * scale, height * scale);
        builder.setUniform("offset", -padPx, -padPx);
        builder.setUniform("cornerRadii", radiusPx, radiusPx, radiusPx, radiusPx);
        builder.setUniform("refractionHeight", refHeight * scale);
        builder.setUniform("refractionAmount", -refAmount * scale);
        builder.setUniform("ior", ior);
        builder.setUniform("depthEffect", Math.max(0f, Config.liquidGlassDepthEffect));
        builder.setUniform("chromaticAberration", Math.max(0f, Config.liquidGlassDispersion));
        ImageFilter lensFilter = ImageFilter.makeRuntimeShader(builder, "content", encodeFilter);

        Canvas offCanvas = surface.getCanvas();
        offCanvas.clear(0);
        blurPaint.setImageFilter(lensFilter);
        offCanvas.drawImageRect(
                capture.image,
                Rect.makeXYWH(0, 0, capture.width, capture.height),
                Rect.makeXYWH(0, 0, reqW, reqH),
                SamplingMode.LINEAR,
                blurPaint,
                true);
        blurPaint.setImageFilter(null);

        context.flush();
        Image glass = surface.makeImageSnapshot(IRect.makeXYWH(0, 0, reqW, reqH));

        builder.close();
        lensFilter.close();
        return glass;
    }

    private ImageFilter ensureGlassFilters(float sigma) {
        if (glassLinearizeFilter == null) {
            ColorMatrix vibrancy = new ColorMatrix(
                    1.5f, 0, 0, 0, 0,
                    0, 1.5f, 0, 0, 0,
                    0, 0, 1.5f, 0, 0,
                    -0.25f, -0.25f, -0.25f, 1, 0);
            glassLinearizeFilter = ImageFilter.makeColorFilter(ColorFilter.getSRGBToLinearGamma(), null);
            glassVibrancyFilter = ImageFilter.makeColorFilter(ColorFilter.makeMatrix(vibrancy), glassLinearizeFilter);
        }
        if (glassEncodeFilter != null && Math.abs(glassFilterSigma - sigma) < 0.001f) {
            return glassEncodeFilter;
        }
        if (glassBlurFilter != null) {
            glassBlurFilter.close();
            glassBlurFilter = null;
        }
        if (glassEncodeFilter != null) {
            glassEncodeFilter.close();
            glassEncodeFilter = null;
        }
        glassBlurFilter = ImageFilter.makeBlur(sigma, sigma, FilterTileMode.CLAMP, glassVibrancyFilter, (Rect) null);
        glassEncodeFilter = ImageFilter.makeColorFilter(ColorFilter.getLinearToSRGBGamma(), glassBlurFilter);
        glassFilterSigma = sigma;
        return glassEncodeFilter;
    }

    private float renderPrecision() {
        return Math.max(0.5f, Math.min(2f, Config.liquidGlassRenderPrecision));
    }

    private void renderHighlight(Canvas canvas, float width, float height, float radius, float taper) {
        if (highlightEffect == null) return;

        float radiusPx = radius;
        float angle = highlightAngle;
        float highlightAlpha = Config.hudTheme == Config.HudTheme.LIGHT ? 0.55f : 0.7f;
        RuntimeEffectBuilder builder = new RuntimeEffectBuilder(highlightEffect);
        builder.setUniform("size", width, height);
        builder.setUniform("cornerRadii", radiusPx, radiusPx, radiusPx, radiusPx);
        builder.setUniform("color", 1f, 1f, 1f, highlightAlpha);
        builder.setUniform("angle", angle);
        builder.setUniform("falloff", 1.5f);
        builder.setUniform("taper", taper);

        Shader highlightShader = builder.makeShader();
        highlightPaint.setShader(highlightShader);
        highlightPaint.setMode(PaintMode.STROKE);
        highlightPaint.setStrokeWidth(1.5f);
        highlightPaint.setBlendMode(BlendMode.PLUS);

        canvas.drawRRect(RRect.makeXYWH(0, 0, width, height, radius), highlightPaint);

        highlightPaint.setShader(null);
        highlightPaint.setMode(PaintMode.FILL);
        highlightPaint.setBlendMode(BlendMode.SRC_OVER);

        builder.close();
        highlightShader.close();
    }

    private float updateHighlightAngle(Minecraft client) {
        float target = (float) (Math.PI * 0.25);
        if (Config.liquidGlassHighlightFollowView && client.player != null) {
            target -= (float) Math.toRadians(client.player.getYRot());
            target += (float) Math.toRadians(client.player.getXRot()) * 0.5f;
        }
        float delta = wrapAngle(target - highlightAngle);
        highlightAngle = wrapAngle(highlightAngle + delta * 0.2f);
        return highlightAngle;
    }

    private static float wrapAngle(float angle) {
        float twoPi = (float) (Math.PI * 2);
        angle = angle % twoPi;
        if (angle > Math.PI) {
            angle -= twoPi;
        } else if (angle < -Math.PI) {
            angle += twoPi;
        }
        return angle;
    }

    private void renderShadow(Canvas canvas, float x, float y, float width, float height, float radius) {
        int color = Config.hudTheme == Config.HudTheme.LIGHT ? 0x1A000000 : 0x38000000;
        ImageFilter shadow = ImageFilter.makeDropShadowOnly(0, 4f, 8f, 8f, color);
        shadowPaint.setImageFilter(shadow);
        canvas.drawRRect(RRect.makeXYWH(x, y, width, height, radius), shadowPaint);
        shadowPaint.setImageFilter(null);
        shadow.close();
    }

    public static int panelTint() {
        return Config.hudTheme == Config.HudTheme.LIGHT ? 0x33FFFFFF : 0x2E0B0F17;
    }

    private void ensureOffscreenSurface(DirectContext context, int slot, int reqW, int reqH) {
        if (offscreenSurfaces[slot] != null && surfaceWidths[slot] >= reqW && surfaceHeights[slot] >= reqH) return;

        if (offscreenSurfaces[slot] != null) {
            offscreenSurfaces[slot].close();
            offscreenSurfaces[slot] = null;
        }

        ImageInfo info = new ImageInfo(reqW, reqH, ColorType.RGBA_8888, ColorAlphaType.PREMUL, ColorSpace.getSRGB());
        offscreenSurfaces[slot] = Surface.makeRenderTarget(context, false, info);
        surfaceWidths[slot] = reqW;
        surfaceHeights[slot] = reqH;
    }

    private boolean ensureRuntimeEffects() {
        if (effectsFailed) return false;
        try {
            if (lensEffect == null) {
                lensEffect = RuntimeEffect.makeForShader(LENS_SHADER);
            }
            if (highlightEffect == null) {
                highlightEffect = RuntimeEffect.makeForShader(HIGHLIGHT_SHADER);
            }
            return true;
        } catch (Throwable t) {
            effectsFailed = true;
            return false;
        }
    }

    private int mainFramebufferId(Minecraft client) {
        if (client.getMainRenderTarget().getColorTexture() instanceof GlTexture texture
                && RenderSystem.getDevice() instanceof GlDevice device) {
            return texture.getFbo(device.directStateAccess(), client.getMainRenderTarget().getDepthTexture());
        }
        return SkiaBlurRenderer.currentDrawFramebufferId();
    }

    public void destroy() {
        for (int i = 0; i < offscreenSurfaces.length; i++) {
            if (offscreenSurfaces[i] != null) {
                offscreenSurfaces[i].close();
                offscreenSurfaces[i] = null;
            }
        }
        if (lensEffect != null) {
            lensEffect.close();
            lensEffect = null;
        }
        if (highlightEffect != null) {
            highlightEffect.close();
            highlightEffect = null;
        }
        if (glassEncodeFilter != null) {
            glassEncodeFilter.close();
            glassEncodeFilter = null;
        }
        if (glassBlurFilter != null) {
            glassBlurFilter.close();
            glassBlurFilter = null;
        }
        if (glassVibrancyFilter != null) {
            glassVibrancyFilter.close();
            glassVibrancyFilter = null;
        }
        if (glassLinearizeFilter != null) {
            glassLinearizeFilter.close();
            glassLinearizeFilter = null;
        }
        glassFilterSigma = Float.NaN;
    }
}
