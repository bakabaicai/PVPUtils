package com.pvp_utils.client.render.MainUI;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.jcodec.api.FrameGrab;
import org.jcodec.api.PictureWithMetadata;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.lwjgl.system.MemoryUtil;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

public final class MainUIVideoBackground {
    private static int nextTextureId;
    private static final long FALLBACK_FRAME_INTERVAL_NANOS = 16_666_667L;

    private final Identifier textureId = Identifier.fromNamespaceAndPath("pvp_utils", "mainui_video_" + nextTextureId++);
    private final AtomicReference<byte[]> pendingFrame = new AtomicReference<>();

    private DynamicTexture texture;
    private Thread decoderThread;
    private volatile boolean running;
    private volatile int pendingWidth = -1;
    private volatile int pendingHeight = -1;
    private volatile String lastError = "";
    private String loadedVideo = "";
    private String failedVideo = "";
    private int textureW = -1;
    private int textureH = -1;

    public boolean render(GuiGraphics graphics, String selectedVideo) {
        Minecraft client = Minecraft.getInstance();
        int screenW = Math.max(1, client.getWindow().getGuiScaledWidth());
        int screenH = Math.max(1, client.getWindow().getGuiScaledHeight());
        if (!ensureVideo(selectedVideo)) {
            return false;
        }

        uploadPendingFrame();
        if (texture == null || textureW <= 0 || textureH <= 0) {
            graphics.fill(0, 0, screenW, screenH, 0xFF000000);
            return true;
        }

        drawCover(graphics, screenW, screenH);
        return true;
    }

    private boolean ensureVideo(String selectedVideo) {
        String safeName = selectedVideo == null ? "" : Path.of(selectedVideo).getFileName().toString();
        if (safeName.isBlank()) {
            close();
            lastError = "No video selected";
            return false;
        }
        if (safeName.equals(loadedVideo) && isDecoderAlive()) {
            return true;
        }
        if (safeName.equals(failedVideo)) {
            lastError = "JCodec failed to decode video";
            return false;
        }

        close();
        Path videoPath = MainUIBackgrounds.resolveVideo(safeName);
        if (!Files.exists(videoPath)) {
            lastError = "Video file not found: " + safeName;
            return false;
        }

        loadedVideo = safeName;
        lastError = "";
        running = true;
        decoderThread = new Thread(() -> decodeLoop(videoPath, safeName), "pvp-utils-mainui-jcodec");
        decoderThread.setDaemon(true);
        decoderThread.start();
        return true;
    }

    private boolean isDecoderAlive() {
        return running && decoderThread != null && decoderThread.isAlive();
    }

    private void decodeLoop(Path videoPath, String safeName) {
        while (running) {
            try (SeekableByteChannel channel = NIOUtils.readableChannel(videoPath.toFile())) {
                FrameGrab grab = FrameGrab.createFrameGrab(channel);
                long fallbackInterval = resolveFrameIntervalNanos(grab);
                long nextFrameAt = System.nanoTime();
                PictureWithMetadata frame;
                while (running && (frame = grab.getNativeFrameWithMetadata()) != null) {
                    byte[] rgba = convertPictureToRgbaFrame(frame.getPicture());
                    if (rgba != null) {
                        pendingFrame.set(rgba);
                    }

                    long interval = resolveFrameIntervalNanos(frame, fallbackInterval);
                    nextFrameAt += interval;
                    sleepUntil(nextFrameAt);
                    long now = System.nanoTime();
                    if (nextFrameAt < now - interval) {
                        nextFrameAt = now;
                    }
                }
            } catch (Throwable t) {
                if (running) {
                    t.printStackTrace();
                    failedVideo = safeName;
                    lastError = "JCodec failed to decode video";
                }
                break;
            }
        }
        running = false;
    }

    private long resolveFrameIntervalNanos(FrameGrab grab) {
        try {
            DemuxerTrackMeta meta = grab.getVideoTrack().getMeta();
            if (meta != null && meta.getTotalDuration() > 0.0 && meta.getTotalFrames() > 0) {
                double fps = meta.getTotalFrames() / meta.getTotalDuration();
                if (fps >= 1.0 && fps <= 240.0) {
                    return Math.max(1_000_000L, Math.round(1_000_000_000.0 / fps));
                }
            }
        } catch (Throwable ignored) {
        }
        return FALLBACK_FRAME_INTERVAL_NANOS;
    }

    private long resolveFrameIntervalNanos(PictureWithMetadata frame, long fallbackInterval) {
        double duration = frame.getDuration();
        if (duration > 0.0 && duration < 1.0) {
            return Math.max(1_000_000L, Math.round(duration * 1_000_000_000.0));
        }
        return fallbackInterval;
    }

    private byte[] convertPictureToRgbaFrame(Picture picture) {
        if (picture == null) {
            return null;
        }
        BufferedImage image = AWTUtil.toBufferedImage(picture);
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }

        int[] argb = image.getRGB(0, 0, width, height, null, 0, width);
        byte[] rgba = new byte[width * height * 4];
        for (int i = 0, p = 0; i < argb.length; i++, p += 4) {
            int color = argb[i];
            rgba[p] = (byte) ((color >> 16) & 0xFF);
            rgba[p + 1] = (byte) ((color >> 8) & 0xFF);
            rgba[p + 2] = (byte) (color & 0xFF);
            rgba[p + 3] = (byte) ((color >> 24) & 0xFF);
        }
        pendingWidth = width;
        pendingHeight = height;
        return rgba;
    }

    private void uploadPendingFrame() {
        byte[] frame = pendingFrame.getAndSet(null);
        int width = pendingWidth;
        int height = pendingHeight;
        if (frame == null || width <= 0 || height <= 0 || frame.length != width * height * 4) {
            return;
        }

        ensureTexture(width, height);
        if (texture == null) {
            return;
        }

        ByteBuffer buffer = MemoryUtil.memAlloc(frame.length);
        try {
            buffer.put(frame);
            buffer.flip();
            GpuTexture gpuTexture = texture.getTexture();
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToTexture(gpuTexture, buffer, NativeImage.Format.RGBA, 0, 0, 0, 0, width, height);
        } finally {
            MemoryUtil.memFree(buffer);
        }
    }

    private void ensureTexture(int width, int height) {
        if (texture != null && textureW == width && textureH == height) {
            return;
        }
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(textureId);
        }
        texture = new DynamicTexture("pvp_utils:mainui_video", width, height, false);
        Minecraft.getInstance().getTextureManager().register(textureId, texture);
        textureW = width;
        textureH = height;
    }

    private void drawCover(GuiGraphics graphics, int screenW, int screenH) {
        float screenAspect = screenW / (float) screenH;
        float textureAspect = textureW / (float) textureH;
        int sourceW = textureW;
        int sourceH = textureH;
        int sourceX = 0;
        int sourceY = 0;

        if (textureAspect > screenAspect) {
            sourceW = Math.max(1, Math.round(textureH * screenAspect));
            sourceX = Math.max(0, (textureW - sourceW) / 2);
        } else if (textureAspect < screenAspect) {
            sourceH = Math.max(1, Math.round(textureW / screenAspect));
            sourceY = Math.max(0, (textureH - sourceH) / 2);
        }

        graphics.blit(RenderPipelines.GUI_TEXTURED, textureId, 0, 0, sourceX, sourceY,
                screenW, screenH, sourceW, sourceH, textureW, textureH);
    }

    private void sleepUntil(long targetNanos) {
        while (running) {
            long delay = targetNanos - System.nanoTime();
            if (delay <= 0L) {
                return;
            }
            try {
                Thread.sleep(delay / 1_000_000L, (int) (delay % 1_000_000L));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public String getLastError() {
        return lastError;
    }

    public void close() {
        running = false;
        if (decoderThread != null) {
            decoderThread.interrupt();
            decoderThread = null;
        }
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(textureId);
            texture = null;
        }
        pendingFrame.set(null);
        pendingWidth = -1;
        pendingHeight = -1;
        loadedVideo = "";
        textureW = -1;
        textureH = -1;
    }
}
