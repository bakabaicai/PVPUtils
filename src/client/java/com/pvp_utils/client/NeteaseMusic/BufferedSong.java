package com.pvp_utils.client.NeteaseMusic;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

final class BufferedSong implements AutoCloseable {
    private static final int[] MPEG1_RATES = {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320};
    private static final int[] MPEG2_RATES = {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160};
    private static final int[] SAMPLE_RATES = {44100, 48000, 32000};
    private final Path temporary;
    private final Path destination;
    private final InputStream network;
    private final RandomAccessFile writer;
    private final long expectedLength;
    private final Runnable onCached;
    private final List<Position> frames = new ArrayList<>();
    private volatile long available;
    private volatile boolean complete;
    private volatile boolean closed;
    private IOException failure;
    private long scanOffset;
    private double durationMs;
    private int readers;

    BufferedSong(InputStream network, Path destination, long expectedLength, ExecutorService executor, Runnable onCached) throws IOException {
        this.network = network;
        this.destination = destination;
        this.expectedLength = expectedLength;
        this.onCached = onCached;
        temporary = Files.createTempFile(destination.getParent(), "music-buffer-", ".part");
        writer = new RandomAccessFile(temporary.toFile(), "rw");
        executor.execute(this::download);
    }

    synchronized long bufferedMs() {
        return (long) durationMs;
    }

    synchronized Playback open(long targetMs) throws IOException {
        if (closed) throw new IOException("Audio buffer is closed");
        if (targetMs > 0L && targetMs + 250L > durationMs && !complete) {
            throw new IOException("Requested position is not buffered");
        }
        Position position = new Position(0L, 0L);
        if (targetMs > 0L && !frames.isEmpty()) {
            long time = Math.max(0L, targetMs - 100L);
            int low = 0;
            int high = frames.size() - 1;
            while (low < high) {
                int middle = (low + high + 1) >>> 1;
                if (frames.get(middle).timeMs() <= time) low = middle;
                else high = middle - 1;
            }
            position = frames.get(low);
        }
        RandomAccessFile file = new RandomAccessFile(temporary.toFile(), "r");
        file.seek(position.offset());
        readers++;
        return new Playback(new BufferedInputStream(new Reader(file), 128 * 1024), position.timeMs());
    }

    private void download() {
        try (network; writer) {
            byte[] bytes = new byte[64 * 1024];
            int count;
            while (!closed && (count = network.read(bytes)) != -1) {
                if (count == 0) continue;
                synchronized (this) {
                    if (closed) break;
                    writer.seek(available);
                    writer.write(bytes, 0, count);
                    available += count;
                    scanFrames();
                    notifyAll();
                }
            }
            synchronized (this) {
                if (!closed) {
                    if (expectedLength > 0L && available < expectedLength) throw new IOException("Incomplete audio download");
                    if (available == 0L) throw new IOException("Empty audio download");
                    complete = true;
                    notifyAll();
                }
            }
        } catch (IOException exception) {
            synchronized (this) {
                failure = exception;
                notifyAll();
            }
        } finally {
            synchronized (this) {
                if (complete && !closed) {
                    try {
                        Path staging = Files.createTempFile(destination.getParent(), "music-cache-", ".tmp");
                        try {
                            Files.copy(temporary, staging, StandardCopyOption.REPLACE_EXISTING);
                            Files.move(staging, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                            onCached.run();
                        } finally {
                            Files.deleteIfExists(staging);
                        }
                    } catch (IOException ignored) {
                    }
                }
                deleteIfUnused();
            }
        }
    }

    private void scanFrames() throws IOException {
        if (scanOffset == 0L) {
            if (available < 10L) return;
            writer.seek(0L);
            if (writer.read() == 'I' && writer.read() == 'D' && writer.read() == '3') {
                writer.seek(5L);
                int flags = writer.read();
                long size = 0L;
                for (int i = 0; i < 4; i++) size = (size << 7) | (writer.read() & 0x7f);
                scanOffset = 10L + size + ((flags & 0x10) != 0 ? 10L : 0L);
            }
        }
        while (scanOffset + 4L <= available) {
            writer.seek(scanOffset);
            int header = writer.readInt();
            int version = (header >>> 19) & 3;
            int layer = (header >>> 17) & 3;
            int bitrateIndex = (header >>> 12) & 15;
            int sampleIndex = (header >>> 10) & 3;
            if ((header & 0xffe00000) != 0xffe00000 || version == 1 || layer != 1
                    || bitrateIndex == 0 || bitrateIndex == 15 || sampleIndex == 3) {
                scanOffset++;
                continue;
            }
            int[] rates = version == 3 ? MPEG1_RATES : MPEG2_RATES;
            int sampleRate = SAMPLE_RATES[sampleIndex] / (version == 3 ? 1 : version == 2 ? 2 : 4);
            int length = (version == 3 ? 144000 : 72000) * rates[bitrateIndex] / sampleRate + ((header >>> 9) & 1);
            if (scanOffset + length > available) return;
            frames.add(new Position(scanOffset, (long) durationMs));
            durationMs += (version == 3 ? 1152d : 576d) * 1000d / sampleRate;
            scanOffset += length;
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) return;
        closed = true;
        notifyAll();
        try {
            network.close();
        } finally {
            deleteIfUnused();
        }
    }

    private void deleteIfUnused() {
        if (closed && readers == 0) try {
            Files.deleteIfExists(temporary);
        } catch (IOException ignored) {
        }
    }

    record Playback(InputStream stream, long positionMs) {
    }

    private record Position(long offset, long timeMs) {
    }

    private final class Reader extends InputStream {
        private final RandomAccessFile file;
        private boolean released;

        private Reader(RandomAccessFile file) {
            this.file = file;
        }

        @Override
        public int read() throws IOException {
            byte[] byteValue = new byte[1];
            return read(byteValue, 0, 1) == -1 ? -1 : byteValue[0] & 0xff;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            java.util.Objects.checkFromIndexSize(offset, length, bytes.length);
            if (length == 0) return 0;
            synchronized (BufferedSong.this) {
                while (!released && !closed && file.getFilePointer() >= available && !complete && failure == null) {
                    try {
                        BufferedSong.this.wait();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Audio buffering interrupted", exception);
                    }
                }
                if (released || closed) throw new IOException("Audio buffer is closed");
                long remaining = available - file.getFilePointer();
                if (remaining > 0L) return file.read(bytes, offset, (int) Math.min(length, remaining));
                if (failure != null) throw failure;
                return -1;
            }
        }

        @Override
        public void close() throws IOException {
            synchronized (BufferedSong.this) {
                if (released) return;
                released = true;
                file.close();
                readers--;
                BufferedSong.this.notifyAll();
                deleteIfUnused();
            }
        }
    }
}
