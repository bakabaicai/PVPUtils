package com.pvp_utils.client.modules.impl.Combat.combo;

import com.pvp_utils.PVPUtils;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libc.LibCStdlib;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

final class ComboAudio implements AutoCloseable {
    private volatile Throwable failure;
    private final ComboAudioEngine engine = new ComboAudioEngine(
            ComboAudio::decode, new ComboVoiceFactory(),
            Executors.newSingleThreadExecutor(threads("pvp-utils-combo-audio-load-")),
            Executors.newCachedThreadPool(threads("pvp-utils-combo-audio-play-")),
            this::recordFailure);

    private synchronized void recordFailure(String message, Throwable error) {
        if (failure != null) return;
        failure = error;
        PVPUtils.LOGGER.warn("Combo effects: {}", message, error);
    }

    void preload() {
        engine.preload();
    }

    void play(int tier, float volume) {
        engine.play(tier, volume);
    }

    void reset() {
        engine.reset();
    }

    Throwable failure() {
        return failure;
    }

    @Override
    public void close() {
        engine.close();
    }

    private static ComboAudioEngine.Pcm decode(int tier) throws IOException {
        String path = "/assets/pvp_utils/combo/sounds/valorant_champions2021kill_" + tier + ".ogg";
        byte[] encoded;
        try (InputStream input = ComboAudio.class.getResourceAsStream(path)) {
            if (input == null) throw new IOException("Missing combo sound: " + path);
            encoded = input.readAllBytes();
        }
        ByteBuffer vorbis = MemoryUtil.memAlloc(encoded.length);
        ShortBuffer decoded = null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            vorbis.put(encoded).flip();
            IntBuffer channels = stack.mallocInt(1);
            IntBuffer sampleRate = stack.mallocInt(1);
            decoded = STBVorbis.stb_vorbis_decode_memory(vorbis, channels, sampleRate);
            if (decoded == null || decoded.remaining() == 0 || channels.get(0) <= 0 || sampleRate.get(0) <= 0) {
                throw new IOException("Invalid combo Vorbis data: " + path);
            }
            byte[] pcm = new byte[Math.multiplyExact(decoded.remaining(), Short.BYTES)];
            for (int index = 0; index < pcm.length; index += 2) {
                short sample = decoded.get(index / Short.BYTES);
                pcm[index] = (byte) sample;
                pcm[index + 1] = (byte) (sample >> 8);
            }
            return new ComboAudioEngine.Pcm(new AudioFormat(sampleRate.get(0), 16, channels.get(0), true, false), pcm);
        } finally {
            if (decoded != null) LibCStdlib.free(decoded);
            MemoryUtil.memFree(vorbis);
        }
    }

    private static ThreadFactory threads(String prefix) {
        AtomicInteger sequence = new AtomicInteger();
        return task -> {
            Thread thread = new Thread(task, prefix + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
