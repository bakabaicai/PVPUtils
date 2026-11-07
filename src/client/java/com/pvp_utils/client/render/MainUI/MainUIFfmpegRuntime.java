package com.pvp_utils.client.render.MainUI;

import com.mojang.logging.LogUtils;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

final class MainUIFfmpegRuntime {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static CompletableFuture<Void> loading;

    private MainUIFfmpegRuntime() {
    }

    static void ensureLoaded() throws InterruptedException, InitializationException {
        try {
            initialization().get();
        } catch (ExecutionException failure) {
            throw new InitializationException(failure.getCause());
        }
    }

    private static synchronized CompletableFuture<Void> initialization() {
        if (loading != null) return loading;
        CompletableFuture<Void> pending = new CompletableFuture<>();
        loading = pending;
        Thread worker = new Thread(() -> initialize(pending), "pvp-utils-ffmpeg-init");
        worker.setDaemon(true);
        worker.setContextClassLoader(MainUIFfmpegRuntime.class.getClassLoader());
        worker.start();
        return pending;
    }

    private static void initialize(CompletableFuture<Void> pending) {
        try {
            Loader.load(org.bytedeco.ffmpeg.global.avutil.class);
            Loader.load(org.bytedeco.ffmpeg.global.swresample.class);
            Loader.load(org.bytedeco.ffmpeg.global.avcodec.class);
            Loader.load(org.bytedeco.ffmpeg.global.avformat.class);
            Loader.load(org.bytedeco.ffmpeg.global.swscale.class);
            Loader.load(org.bytedeco.ffmpeg.global.avdevice.class);
            FFmpegFrameGrabber.tryLoad();
            pending.complete(null);
        } catch (Throwable failure) {
            LOGGER.error("Unable to initialize FFmpeg native libraries for main-menu video", failure);
            pending.completeExceptionally(failure);
        }
    }

    static final class InitializationException extends Exception {
        private InitializationException(Throwable cause) {
            super("FFmpeg native libraries could not be initialized", cause);
        }
    }
}
