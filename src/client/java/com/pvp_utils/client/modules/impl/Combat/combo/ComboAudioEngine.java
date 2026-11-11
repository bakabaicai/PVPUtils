package com.pvp_utils.client.modules.impl.Combat.combo;

import javax.sound.sampled.AudioFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiConsumer;

final class ComboAudioEngine implements AutoCloseable {
    record Pcm(AudioFormat format, byte[] bytes) {
        Pcm {
            if (format == null || bytes == null || bytes.length == 0
                    || format.getSampleSizeInBits() != 16 || format.isBigEndian()
                    || !AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding())
                    || format.getFrameSize() <= 0 || bytes.length % format.getFrameSize() != 0) {
                throw new IllegalArgumentException("Expected nonempty signed 16-bit little-endian PCM");
            }
        }
    }

    @FunctionalInterface
    interface Loader {
        Pcm load(int tier) throws Exception;
    }

    interface Voice {

        void start();

        void stopAndClose();
    }

    @FunctionalInterface
    interface VoiceFactory {

        Voice prepare(Pcm pcm, float volume, Runnable completion) throws Exception;
    }

    private final Object lock = new Object();
    private final Loader loader;
    private final VoiceFactory voiceFactory;
    private final ExecutorService io;
    private final ExecutorService playback;
    private final BiConsumer<String, Throwable> errors;
    private final Map<Integer, Load> cache = new HashMap<>();
    private final Set<Request> pending = new HashSet<>();
    private final Set<Entry> active = new HashSet<>();
    private long generation;
    private boolean closed;

    ComboAudioEngine(Loader loader, VoiceFactory voiceFactory, ExecutorService io,
                         ExecutorService playback, BiConsumer<String, Throwable> errors) {
        this.loader = loader;
        this.voiceFactory = voiceFactory;
        this.io = io;
        this.playback = playback;
        this.errors = errors;
    }

    void preload() {
        synchronized (lock) {
            if (closed) return;
            for (int tier = 1; tier <= 5; tier++) loadLocked(tier);
        }
    }

    void play(int tier, float volume) {
        if (tier < 1 || !Float.isFinite(volume) || volume <= 0.0f) return;
        float gain = Math.min(volume, 1.0f);
        Request request;
        Load load;
        synchronized (lock) {
            if (closed) return;
            request = new Request(generation);
            pending.add(request);
            load = loadLocked(Math.min(tier, 5));
        }
        load.result.whenComplete((pcm, failure) -> {
            synchronized (lock) {
                if (failure != null || !currentLocked(request)) {
                    pending.remove(request);
                    return;
                }
                try {
                    request.job = playback.submit(() -> prepareAndStart(request, pcm, gain));
                } catch (RejectedExecutionException exception) {
                    pending.remove(request);
                    if (!closed) errors.accept("Champion sound playback submission failed", exception);
                }
            }
        });
    }

    synchronized void reset() {
        List<Future<?>> cancelled = new ArrayList<>();
        List<Voice> stopped = new ArrayList<>();
        synchronized (lock) {
            generation++;
            for (Request request : pending) {
                if (request.job != null) cancelled.add(request.job);
            }
            pending.clear();
            for (Entry entry : new ArrayList<>(active)) {
                Voice voice = finishLocked(entry);
                if (voice != null) stopped.add(voice);
            }
        }
        for (Future<?> job : cancelled) job.cancel(true);
        for (Voice voice : stopped) stop(voice);
        if (playback instanceof ThreadPoolExecutor pool) pool.purge();

    }

    @Override
    public synchronized void close() {
        synchronized (lock) {
            if (closed) return;
            closed = true;
        }
        reset();
        synchronized (lock) {
            for (Load load : cache.values()) {
                if (load.job != null) load.job.cancel(true);
                load.result.cancel(false);
            }
            cache.clear();
        }
        io.shutdownNow();
        playback.shutdownNow();
    }

    int cachedSoundCount() {
        synchronized (lock) {
            int count = 0;
            for (Load load : cache.values()) {
                if (load.result.isDone() && !load.result.isCompletedExceptionally()) count++;
            }
            return count;
        }
    }

    private Load loadLocked(int tier) {
        Load existing = cache.get(tier);
        if (existing != null) return existing;
        Load load = new Load();
        cache.put(tier, load);
        try {
            load.job = io.submit(() -> {
                try {
                    Pcm pcm = loader.load(tier);
                    if (pcm == null) throw new IllegalStateException("No PCM for sound tier " + tier);
                    load.result.complete(pcm);
                } catch (Exception | LinkageError exception) {
                    if (load.result.completeExceptionally(exception)) {
                        errors.accept("Champion sound tier " + tier + " failed to load", exception);
                    }
                }
            });
        } catch (RejectedExecutionException exception) {
            load.result.completeExceptionally(exception);
            errors.accept("Champion sound asset submission failed", exception);
        }
        return load;
    }

    private boolean currentLocked(Request request) {
        return !closed && request.generation == generation && pending.contains(request);
    }

    private void prepareAndStart(Request request, Pcm pcm, float gain) {
        synchronized (lock) {
            if (!currentLocked(request)) return;
        }
        Entry entry = new Entry();
        Voice discarded = null;
        try {

            Voice voice = voiceFactory.prepare(pcm, gain, () -> finish(entry));
            if (voice == null) throw new IllegalStateException("Sound backend returned no voice");
            synchronized (lock) {
                entry.voice = voice;
                if (!currentLocked(request) || entry.finished) {
                    discarded = finishLocked(entry);
                } else {
                    active.add(entry);

                    voice.start();
                }
                pending.remove(request);
            }
        } catch (Exception | LinkageError exception) {
            boolean report;
            synchronized (lock) {
                report = currentLocked(request);
                pending.remove(request);
                discarded = finishLocked(entry);
            }
            if (report) errors.accept("Champion sound playback failed", exception);
        } finally {
            if (discarded != null) stop(discarded);
        }
    }

    private void finish(Entry entry) {
        Voice voice;
        synchronized (lock) {
            voice = finishLocked(entry);
        }
        if (voice != null) stop(voice);
    }

    private Voice finishLocked(Entry entry) {
        active.remove(entry);
        entry.finished = true;
        if (entry.voice == null || entry.closeClaimed) return null;
        entry.closeClaimed = true;
        return entry.voice;
    }

    private void stop(Voice voice) {
        try {
            voice.stopAndClose();
        } catch (Exception | LinkageError exception) {
            errors.accept("Champion sound cleanup failed", exception);
        }
    }

    private static final class Load {
        private final CompletableFuture<Pcm> result = new CompletableFuture<>();
        private Future<?> job;
    }

    private static final class Request {
        private final long generation;
        private Future<?> job;

        private Request(long generation) {
            this.generation = generation;
        }
    }

    private static final class Entry {
        private Voice voice;
        private boolean finished;
        private boolean closeClaimed;
    }
}
