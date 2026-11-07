package com.pvp_utils.client.render.MainUI;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayDeque;

import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_RGBA;

public final class MainUIVideoBackground {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int nextTextureId;
    private final Identifier textureId = Identifier.fromNamespaceAndPath("pvp_utils", "mainui_video_" + nextTextureId++);
    private DynamicTexture texture;
    private volatile DecoderSession decoder;
    private volatile DecoderSession pendingDecoder;
    private String lastError = "";
    private String failedVideo = "";
    private String requestedVideo = "";
    private String loadedVideo = "";
    private int textureW = -1;
    private int textureH = -1;

    public boolean render(GuiGraphics graphics, String selectedVideo) {
        Minecraft client = Minecraft.getInstance();
        int screenW = Math.max(1, client.getWindow().getGuiScaledWidth());
        int screenH = Math.max(1, client.getWindow().getGuiScaledHeight());
        int width = Math.max(1, client.getWindow().getWidth());
        int height = Math.max(1, client.getWindow().getHeight());
        prepare(selectedVideo, width, height);
        DecoderSession session = decoder;
        DecoderSession next = pendingDecoder;
        if (session != null) {
            session.touch(width, height);
        }
        if (next != null) next.touch(width, height);
        if (next == null || !uploadPendingFrame(next, true)) {
            if (session != null) uploadPendingFrame(session, false);
        }
        if (texture == null || textureW <= 0 || textureH <= 0) return false;
        drawCover(graphics, screenW, screenH);
        return true;
    }

    public void prepare(String selectedVideo, int width, int height) {
        String safeName;
        try {
            Path fileName = selectedVideo == null || selectedVideo.isBlank() ? null : Path.of(selectedVideo).getFileName();
            safeName = fileName == null ? "" : fileName.toString();
        } catch (InvalidPathException failure) {
            if (pendingDecoder != null) pendingDecoder.cancel();
            pendingDecoder = null;
            lastError = "Invalid video file name";
            return;
        }
        boolean changed = !safeName.equals(requestedVideo);
        if (changed) {
            requestedVideo = safeName;
            lastError = "";
            failedVideo = "";
            if (pendingDecoder != null) pendingDecoder.cancel();
            pendingDecoder = null;
        }
        if (safeName.isBlank()) {
            lastError = "No video selected";
            return;
        }
        DecoderSession next = pendingDecoder;
        if (next != null) {
            if (!next.error.isBlank()) {
                failedVideo = safeName;
                lastError = next.error;
                next.cancel();
                pendingDecoder = null;
            } else if (next.active) {
                return;
            }
        }
        DecoderSession current = decoder;
        if (safeName.equals(loadedVideo) && current != null && current.active) return;
        if (!changed && current != null && safeName.equals(current.name) && !current.error.isBlank()) {
            failedVideo = safeName;
            lastError = current.error;
        }
        if (safeName.equals(failedVideo)) return;
        DecoderSession session = new DecoderSession(safeName, Math.max(1, width), Math.max(1, height));
        pendingDecoder = session;
        session.worker.start();
    }

    private boolean uploadPendingFrame(DecoderSession session, boolean replacement) {
        DecodedFrame frame = session.take();
        if (frame == null) return false;
        try {
            uploadFrame(frame, replacement);
            if (replacement) {
                DecoderSession previous = decoder;
                decoder = session;
                loadedVideo = session.name;
                pendingDecoder = null;
                session.startPresentation();
                if (previous != null) previous.cancel();
            }
            return true;
        } catch (RuntimeException | LinkageError failure) {
            session.error = "Unable to upload video frame: " + session.name;
            if (session.name.equals(requestedVideo)) {
                failedVideo = session.name;
                lastError = session.error;
            }
            if (pendingDecoder == session) pendingDecoder = null;
            session.cancel();
            LOGGER.error("Unable to upload main-menu video {}", session.name, failure);
            return false;
        } finally {
            session.recycle(frame.pixels());
        }
    }

    private void uploadFrame(DecodedFrame frame, boolean replacement) {
        if (!replacement && texture != null && textureW == frame.width() && textureH == frame.height()) {
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture.getTexture(),
                    frame.pixels(), NativeImage.Format.RGBA, 0, 0, 0, 0, frame.width(), frame.height());
            return;
        }
        DynamicTexture next = new DynamicTexture("pvp_utils:mainui_video", frame.width(), frame.height(), false);
        try {
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(next.getTexture(),
                    frame.pixels(), NativeImage.Format.RGBA, 0, 0, 0, 0, frame.width(), frame.height());
            Minecraft.getInstance().getTextureManager().register(textureId, next);
            texture = next;
            textureW = frame.width();
            textureH = frame.height();
        } catch (RuntimeException | LinkageError failure) {
            next.close();
            throw failure;
        }
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

    public String getLastError() {
        return lastError;
    }

    public void close() {
        DecoderSession session = decoder;
        DecoderSession next = pendingDecoder;
        decoder = null;
        pendingDecoder = null;
        if (session != null) session.cancel();
        if (next != null) next.cancel();
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(textureId);
            texture = null;
        }
        loadedVideo = "";
        requestedVideo = "";
        failedVideo = "";
        lastError = "";
        textureW = -1;
        textureH = -1;
    }

    private record DecodedFrame(ByteBuffer pixels, int width, int height) {
    }

    private final class DecoderSession {
        private final String name;
        private final Thread worker;
        private final ArrayDeque<ByteBuffer> reusable = new ArrayDeque<>(3);
        private volatile boolean active = true;
        private volatile boolean presenting;
        private volatile String error = "";
        private volatile long lastUseNanos = System.nanoTime();
        private long presentationStartedNanos;
        private volatile int targetWidth;
        private volatile int targetHeight;
        private DecodedFrame pending;

        private DecoderSession(String name, int width, int height) {
            this.name = name;
            targetWidth = width;
            targetHeight = height;
            worker = new Thread(this::decode, "pvp-utils-mainui-ffmpeg");
            worker.setDaemon(true);
        }

        private void decode() {
            try {
                Path path = MainUIBackgrounds.resolveVideo(name);
                if (!active) return;
                if (!Files.isRegularFile(path)) {
                    error = "Video file not found: " + name;
                    return;
                }
                MainUIFfmpegRuntime.ensureLoaded();
                if (!active) return;
                decodeVideo(path);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (MainUIFfmpegRuntime.InitializationException e) {
                if (active) {
                    error = "FFmpeg initialization failed; restart the client after checking the log";
                }
            } catch (Exception | LinkageError e) {
                if (active) {
                    error = "Unable to decode video: " + name;
                    LOGGER.error("Unable to decode main-menu video {}", name, e);
                }
            } finally {
                cancel();
            }
        }

        private void decodeVideo(Path path) throws Exception {
            try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(path.toFile())) {
                grabber.setPixelFormat(AV_PIX_FMT_RGBA);
                grabber.setVideoOption("threads", "2");
                grabber.start();
                int sourceWidth = grabber.getImageWidth();
                int sourceHeight = grabber.getImageHeight();
                if (sourceWidth <= 0 || sourceHeight <= 0) throw new IllegalStateException("No video stream");
                long baseTime = 0L;
                long firstTimestamp = -1L;
                boolean emptyLoop = false;
                boolean presentationStarted = false;
                while (active && (!presenting || System.nanoTime() - lastUseNanos < 2_000_000_000L)) {
                    float reduction = Math.min(1f, Math.max(targetWidth / (float) sourceWidth, targetHeight / (float) sourceHeight));
                    grabber.setImageWidth(Math.max(1, Math.round(sourceWidth * reduction)));
                    grabber.setImageHeight(Math.max(1, Math.round(sourceHeight * reduction)));
                    Frame frame = grabber.grabImage();
                    if (frame == null) {
                        if (emptyLoop) throw new IllegalStateException("Video stream contains no frames");
                        emptyLoop = true;
                        grabber.setTimestamp(0L);
                        firstTimestamp = -1L;
                        continue;
                    }
                    emptyLoop = false;
                    if (firstTimestamp < 0L) {
                        firstTimestamp = frame.timestamp;
                        baseTime = System.nanoTime();
                    }
                    long due = baseTime + Math.max(0L, frame.timestamp - firstTimestamp) * 1000L;
                    await(due);
                    if (!active) break;
                    if (System.nanoTime() - due > 120_000_000L) continue;
                    int rowBytes = Math.multiplyExact(frame.imageWidth, 4);
                    int bytes = Math.multiplyExact(rowBytes, frame.imageHeight);
                    if (bytes > 256 * 1024 * 1024 || frame.image == null || !(frame.image[0] instanceof ByteBuffer input)) {
                        throw new IllegalStateException("Unsupported video frame dimensions or pixel data");
                    }
                    ByteBuffer pixels = acquire(bytes);
                    if (pixels == null) break;
                    try {
                        pixels.clear();
                        ByteBuffer source = input.duplicate();
                        if (frame.imageStride == rowBytes) {
                            source.position(0).limit(bytes);
                            pixels.put(source);
                        } else {
                            for (int row = 0; row < frame.imageHeight; row++) {
                                source.limit(input.limit()).position(row * frame.imageStride).limit(row * frame.imageStride + rowBytes);
                                pixels.put(source);
                            }
                        }
                        pixels.flip();
                        publish(new DecodedFrame(pixels, frame.imageWidth, frame.imageHeight));
                        pixels = null;
                    } finally {
                        if (pixels != null) recycle(pixels);
                    }
                    if (!presentationStarted) {
                        baseTime = awaitPresentation();
                        presentationStarted = true;
                    }
                }
            }
        }

        private void await(long deadline) throws InterruptedException {
            while (active) {
                if (presenting && System.nanoTime() - lastUseNanos >= 2_000_000_000L) {
                    active = false;
                    return;
                }
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0L) return;
                Thread.sleep(Math.min(remaining / 1_000_000L, 100L), (int) (remaining % 1_000_000L));
            }
        }

        private void touch(int width, int height) {
            lastUseNanos = System.nanoTime();
            targetWidth = width;
            targetHeight = height;
        }

        private synchronized void startPresentation() {
            presentationStartedNanos = System.nanoTime();
            lastUseNanos = presentationStartedNanos;
            presenting = true;
            notifyAll();
        }

        private synchronized long awaitPresentation() throws InterruptedException {
            while (active && !presenting) wait();
            return presentationStartedNanos;
        }

        private synchronized ByteBuffer acquire(int size) {
            if (!active) return null;
            ByteBuffer buffer;
            while ((buffer = reusable.pollFirst()) != null) {
                if (buffer.capacity() == size) return buffer;
                MemoryUtil.memFree(buffer);
            }
            return MemoryUtil.memAlloc(size);
        }

        private synchronized void recycle(ByteBuffer buffer) {
            if (active && reusable.size() < 3) reusable.addLast(buffer);
            else MemoryUtil.memFree(buffer);
        }

        private synchronized void publish(DecodedFrame frame) {
            if (!active) {
                MemoryUtil.memFree(frame.pixels());
                return;
            }
            if (pending != null) recycle(pending.pixels());
            pending = frame;
        }

        private synchronized DecodedFrame take() {
            DecodedFrame frame = pending;
            pending = null;
            return frame;
        }

        private synchronized void cancel() {
            active = false;
            notifyAll();
            if (Thread.currentThread() != worker) worker.interrupt();
            if (pending != null) {
                MemoryUtil.memFree(pending.pixels());
                pending = null;
            }
            ByteBuffer buffer;
            while ((buffer = reusable.pollFirst()) != null) MemoryUtil.memFree(buffer);
        }
    }
}
