package com.pvp_utils.client.modules.impl.Render.motionblur;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pvp_utils.Config;
import com.pvp_utils.mixin.client.PostChainAccessor;
import com.pvp_utils.mixin.client.PostPassAccessor;
import com.pvp_utils.mixin.client.ShaderManagerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MotionBlurManager {
    private static final FrameTimer frameTimer = new FrameTimer();
    private static final CameraState cameraState = new CameraState();
    private static final BlurStrengthCalculator strengthCalc = new BlurStrengthCalculator();
    private static final Set<String> loadErrorLogged = new HashSet<>();

    private static GraphicsResourceAllocator frameAllocator = null;
    private static PostChain cachedPreProcessor = null;
    private static PostChain cachedF5Processor = null;
    private static PostChain cachedPostProcessor = null;
    private static final int UBO_SIZE = 304;
    private static final ManagedUniformBuffer preEntityUBO = new ManagedUniformBuffer("PreEntityBlurUniforms", UBO_SIZE);
    private static final ManagedUniformBuffer f5EntityUBO = new ManagedUniformBuffer("PreEntityBlurUniforms", UBO_SIZE);
    private static final ManagedUniformBuffer postRenderUBO = new ManagedUniformBuffer("PostRenderBlurUniforms", UBO_SIZE);

    private enum BlurPass { NORMAL_PRE, SPECIAL_F5, NORMAL_POST }

    private MotionBlurManager() {}

    public static void captureAllocator(GraphicsResourceAllocator allocator) { frameAllocator = allocator; }
    public static void clearFrameAllocator() { frameAllocator = null; }
    public static void beginFrame() { frameTimer.beginFrame(); }

    public static void invalidate() {
        preEntityUBO.reset();
        f5EntityUBO.reset();
        postRenderUBO.reset();
        FrameBlendingManager.invalidate();
        cachedPreProcessor = null;
        cachedF5Processor = null;
        cachedPostProcessor = null;
        loadErrorLogged.clear();
    }

    public static boolean shouldRun() {
        return Config.dynamicMotionBlur && Config.dynamicMotionBlurStrength != 0.0f;
    }

    public static void setFrameMotionBlur(Matrix4f modelView, Matrix4f prevModelView, Matrix4f projection, Matrix4f prevProjection, float dx, float dy, float dz) {
        cameraState.setFrame(modelView, prevModelView, projection, prevProjection, dx, dy, dz);
    }

    public static void applyPreEntityBlur() {
        if (shouldRun() && usesVelocityBlur()) applyBlurInternal(BlurPass.NORMAL_PRE);
    }

    public static void applyF5EntityRideBlur() {
        if (shouldRun() && usesVelocityBlur()) applyBlurInternal(BlurPass.SPECIAL_F5);
    }

    public static void applyPostRenderVelocityOnly() {
        if (shouldRun() && usesVelocityBlur()) applyBlurInternal(BlurPass.NORMAL_POST);
    }

    public static void applyTemporalBlur() {
        if (frameAllocator == null || !shouldRun()) return;

        switch (Config.motionBlurAlgorithm) {
            case FRAME_BLENDING, HYBRID_BLENDING -> FrameBlendingManager.applyFrameBlending(frameAllocator, frameTimer.getFPS(), frameTimer.getRefreshRate());
            case ACCUMULATION_MAX -> FrameBlendingManager.applyAccumulationMax(frameAllocator, Config.dynamicMotionBlurStrength);
            case ACCUMULATION_MIX -> FrameBlendingManager.applyAccumulationMix(frameAllocator, Config.dynamicMotionBlurStrength);
            default -> {}
        }
    }

    private static boolean usesVelocityBlur() {
        return Config.motionBlurAlgorithm == Config.MotionBlurAlgorithm.VELOCITY_BASED || Config.motionBlurAlgorithm == Config.MotionBlurAlgorithm.HYBRID_BLENDING;
    }

    private static void applyBlurInternal(BlurPass pass) {
        if (frameAllocator == null) return;

        Minecraft client = Minecraft.getInstance();
        BlurStrengthCalculator.Result blur = strengthCalc.calculate(
                Config.dynamicMotionBlurStrength,
                frameTimer.getFPS(),
                frameTimer.getRefreshRate(),
                Config.dynamicMotionBlurRefreshRateScaling);
        float viewW = client.getMainRenderTarget().width;
        float viewH = client.getMainRenderTarget().height;

        switch (pass) {
            case NORMAL_PRE -> {
                PostChain processor = getPreProcessor(client);
                if (processor != null) writeAndRun(processor, "PreEntityBlurUniforms", preEntityUBO, blur.strength(), viewW, viewH, blur.sampleAmount(), client);
            }
            case SPECIAL_F5 -> {
                PostChain processor = getF5Processor(client);
                if (processor != null) writeAndRun(processor, "PreEntityBlurUniforms", f5EntityUBO, blur.strength(), viewW, viewH, blur.sampleAmount(), client);
            }
            case NORMAL_POST -> {
                PostChain processor = getPostProcessor(client);
                if (processor != null) writeAndRun(processor, "PostRenderBlurUniforms", postRenderUBO, blur.strength(), viewW, viewH, blur.sampleAmount(), client);
            }
        }
    }

    private static PostChain getPreProcessor(Minecraft client) {
        PostChain result = loadProcessor(client, "velocity_pre", "pre-entity");
        if (result == null) { cachedPreProcessor = null; return null; }
        if (result != cachedPreProcessor) cachedPreProcessor = result;
        return cachedPreProcessor;
    }

    private static PostChain getF5Processor(Minecraft client) {
        PostChain result = loadProcessor(client, "velocity_f5", "F5/entity-riding");
        if (result == null) { cachedF5Processor = null; return null; }
        if (result != cachedF5Processor) cachedF5Processor = result;
        return cachedF5Processor;
    }

    private static PostChain getPostProcessor(Minecraft client) {
        PostChain result = loadProcessor(client, "velocity_post", "post-render");
        if (result == null) { cachedPostProcessor = null; return null; }
        if (result != cachedPostProcessor) cachedPostProcessor = result;
        return cachedPostProcessor;
    }

    private static PostChain loadProcessor(Minecraft client, String shaderName, String displayName) {
        try {
            net.minecraft.client.renderer.ShaderManager.CompilationCache cache = ((ShaderManagerAccessor) client.getShaderManager()).getCompilationCache();
            if (cache == null) return null;
            PostChain chain = cache.getOrLoadPostChain(Identifier.fromNamespaceAndPath("pvp_utils", shaderName), LevelTargetBundle.MAIN_TARGETS);
            loadErrorLogged.remove(shaderName);
            return chain;
        } catch (Exception e) {
            if (loadErrorLogged.add(shaderName)) {
                System.err.println("[PVPUtils] Failed to load dynamic blur " + displayName + " shader: " + e.getMessage());
            }
            return null;
        }
    }

    private static void writeAndRun(PostChain processor, String uboKey, ManagedUniformBuffer managedUBO, float blendFactor, float viewW, float viewH, int sampleAmount, Minecraft client) {
        List<PostPass> passes = ((PostChainAccessor) processor).getPasses();
        if (passes.isEmpty()) return;

        Map<String, GpuBuffer> uniformBuffers = ((PostPassAccessor) passes.getFirst()).getCustomUniforms();
        if (!uniformBuffers.containsKey(uboKey)) return;

        GpuBuffer ubo = managedUBO.put(processor, uniformBuffers, uboKey);
        try {
            try (GpuBuffer.MappedView view = RenderSystem.getDevice().createCommandEncoder().mapBuffer(ubo, false, true)) {
                Std140Builder b = Std140Builder.intoBuffer(view.data());
                b.putMat4f(cameraState.getMvInverse());
                b.putMat4f(cameraState.getProjInverse());
                b.putMat4f(cameraState.getPrevModelView());
                b.putMat4f(cameraState.getPrevProjection());
                b.putVec3(cameraState.getDx(), cameraState.getDy(), cameraState.getDz());
                b.putVec2(viewW, viewH);
                b.putFloat(blendFactor);
                b.putInt(sampleAmount);
                b.putInt(0);
                b.putInt(1);
            }
            processor.process(client.getMainRenderTarget(), frameAllocator);
        } catch (RuntimeException e) {
            if (managedUBO.resetIfClosed(e)) return;
            throw e;
        }
    }
}
