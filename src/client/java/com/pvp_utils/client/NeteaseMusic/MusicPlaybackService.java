package com.pvp_utils.client.NeteaseMusic;

import com.pvp_utils.PVPUtils;
import com.pvp_utils.Config;
import com.goxr3plus.streamplayer.enums.Status;
import com.goxr3plus.streamplayer.stream.StreamPlayer;
import com.goxr3plus.streamplayer.stream.StreamPlayerEvent;
import com.goxr3plus.streamplayer.stream.StreamPlayerListener;
import net.fabricmc.loader.api.FabricLoader;

import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.BooleanControl;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MusicPlaybackService {
    private static final int MAX_CACHED_SONGS = 15;

    static {
        Logger.getLogger("com.goxr3plus.streamplayer").setLevel(Level.WARNING);
        Logger.getLogger("com.goxr3plus.streamplayer.stream").setLevel(Level.WARNING);
    }

    public static final MusicPlaybackService INSTANCE = new MusicPlaybackService();

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "PVPUtils-NeteaseMusic");
        thread.setDaemon(true);
        return thread;
    });
    private final ExecutorService downloads = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "PVPUtils-MusicDownload");
        thread.setDaemon(true);
        return thread;
    });
    private final ExecutorService lyricExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "PVPUtils-MusicLyrics");
        thread.setDaemon(true);
        return thread;
    });
    private final ExecutorService audioExecutor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "PVPUtils-MusicAudio");
        thread.setDaemon(true);
        return thread;
    });
    private final ExecutorService eventExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "PVPUtils-MusicEvents");
        thread.setDaemon(true);
        return thread;
    });
    private volatile StreamPlayer player;
    private InputStream activeStream;
    private InputStream fadingStream;
    private volatile BufferedSong activeBuffer;
    private BufferedSong fadingBuffer;
    private final ExecutorService bufferDownloads = Executors.newFixedThreadPool(3, runnable -> {
        Thread thread = new Thread(runnable, "PVPUtils-MusicBuffer");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicLong seekRevision = new AtomicLong();
    private volatile PendingSeek pendingSeek;

    private record PendingSeek(long request, long revision, StreamPlayer source, long targetMs) {
    }
    private StreamPlayer fadingPlayer;
    private float activeGain = 1f;
    private float fadingGain;
    private long fadeStartedNanos;
    private long fadeDurationNanos;
    private volatile long activeRevision;
    private volatile Song pendingSong;
    private long retryAfterNanos;
    private final List<Song> playlist = Collections.synchronizedList(new ArrayList<>());
    private final List<LyricLine> lyrics = Collections.synchronizedList(new ArrayList<>());
    private volatile List<LyricLine> lyricSnapshot = List.of();
    private final AtomicLong playbackRevision = new AtomicLong();
    private volatile Song currentSong;
    private volatile File currentFile;
    private volatile boolean playing;
    private volatile boolean changingSong;
    private volatile long basePositionMs;
    private volatile long playStartedAtMs;
    private volatile long totalDurationMs;
    private volatile long progressOffsetMs;
    private volatile float volume = Math.max(0.0F, Math.min(1.0F, Config.neteaseMusicVolume));
    private volatile String status = "Idle";
    private volatile PlaybackMode playbackMode = PlaybackMode.LIST;
    private volatile int currentIndex;
    private volatile SourceDataLine visualizerLine;
    private volatile MusicPcmVisualizer.Snapshot visualizerSnapshot = MusicPcmVisualizer.EMPTY;

    private MusicPlaybackService() {
        executor.scheduleAtFixedRate(this::updatePlayback, 0L, 20L, TimeUnit.MILLISECONDS);
    }

    public void setPlaylist(List<Song> songs, int startIndex) {
        synchronized (playlist) {
            playlist.clear();
            if (songs != null) {
                playlist.addAll(songs);
            }
            currentIndex = Math.max(0, Math.min(startIndex, Math.max(0, playlist.size() - 1)));
        }
    }

    public List<Song> getQueue() {
        synchronized (playlist) {
            return List.copyOf(playlist);
        }
    }

    public void addToQueue(Song song, boolean playNext) {
        if (song == null) return;
        synchronized (playlist) {
            int index = playNext && !playlist.isEmpty()
                    ? Math.min(playlist.size(), currentIndex + 1) : playlist.size();
            playlist.add(index, song);
        }
    }

    public void playQueueIndex(int index) {
        Song song;
        synchronized (playlist) {
            if (index < 0 || index >= playlist.size()) return;
            song = playlist.get(index);
        }
        playSong(song);
    }

    public void removeQueueIndex(int index) {
        Song replacement = null;
        boolean empty;
        synchronized (playlist) {
            if (index < 0 || index >= playlist.size()) return;
            Song removed = playlist.remove(index);
            boolean removedCurrent = index == currentIndex && removed.equals(currentSong);
            empty = playlist.isEmpty();
            if (index < currentIndex) currentIndex--;
            currentIndex = Math.max(0, Math.min(currentIndex, playlist.size() - 1));
            if (removedCurrent && !empty) replacement = playlist.get(currentIndex);
        }
        if (empty) {
            clearQueue();
        } else if (replacement != null) {
            playSong(replacement);
        }
    }

    public void clearQueue() {
        synchronized (playlist) {
            playlist.clear();
            currentIndex = 0;
        }
        stop();
        currentSong = null;
        currentFile = null;
        totalDurationMs = 0L;
        synchronized (lyrics) {
            lyrics.clear();
            lyricSnapshot = List.of();
        }
    }

    public void playSong(Song song) {
        requestSong(song, false);
    }

    private void requestSong(Song song, boolean automatic) {
        if (song == null) return;
        long revision = playbackRevision.incrementAndGet();
        pendingSong = song;
        changingSong = true;
        status = "Loading " + song.name();
        downloads.execute(() -> {
            try {
                if (revision != playbackRevision.get()) return;
                SongFile songFile = NeteaseMusicApi.getSongFile(song.id());
                if (revision != playbackRevision.get()) return;
                if (!songFile.isPlayable()) throw new IllegalStateException("Song is unavailable");
                Path cachedFile = cachePath(song);
                boolean cached = Files.isRegularFile(cachedFile) && Files.size(cachedFile) > 0L
                        && (songFile.size() <= 0L || Files.size(cachedFile) >= songFile.size());
                prepareSong(song, cached ? cachedFile.toFile() : null, songFile.url(), revision, automatic);
            } catch (Exception exception) {
                executor.execute(() -> failRequest(song, revision, exception));
            }
        });
    }

    private void prepareSong(Song song, File file, String url, long revision, boolean automatic) throws Exception {
        StreamPlayer incoming = new StreamPlayer(Logger.getLogger("com.goxr3plus.streamplayer"), audioExecutor, eventExecutor);
        incoming.addStreamPlayerListener(new PlaybackListener(incoming));
        InputStream stream = null;
        BufferedSong buffer = null;
        try {
            if (file != null) {
                incoming.open(file);
                touchCacheFile(file.toPath());
            } else {
                URLConnection connection = URI.create(url).toURL().openConnection();
                connection.setConnectTimeout(10_000);
                connection.setReadTimeout(15_000);
                InputStream network = connection.getInputStream();
                try {
                    Path destination = cachePath(song);
                    buffer = new BufferedSong(network, destination, connection.getContentLengthLong(), bufferDownloads,
                            () -> downloads.execute(() -> trimSongCache(destination)));
                } catch (Exception exception) {
                    closeStream(network);
                    throw exception;
                }
                stream = buffer.open(0L).stream();
                incoming.open(stream);
            }
            InputStream preparedStream = stream;
            BufferedSong preparedBuffer = buffer;
            executor.execute(() -> startPreparedSong(song, file, incoming, preparedStream, preparedBuffer, revision, automatic));
        } catch (Exception exception) {
            incoming.stop();
            closeStream(stream);
            closeBuffer(buffer);
            throw exception;
        }
    }

    private void startPreparedSong(Song song, File file, StreamPlayer incoming, InputStream stream, BufferedSong buffer,
                                   long revision, boolean automatic) {
        if (revision != playbackRevision.get()) {
            incoming.stop();
            closeStream(stream);
            closeBuffer(buffer);
            return;
        }
        if (automatic && (!playing && player != null && player.isPaused()
                || playing && totalDurationMs - positionMs() > Math.round(crossfadeSeconds() * 1000f))) {
            executor.schedule(() -> startPreparedSong(song, file, incoming, stream, buffer, revision, true), 100L, TimeUnit.MILLISECONDS);
            return;
        }
        try {
            boolean overlap = playing && player != null && crossfadeSeconds() > 0f;
            incoming.play();
            setPlayerGain(incoming, overlap ? 0f : volume);
            if (revision != playbackRevision.get()) {
                incoming.stop();
                closeStream(stream);
                closeBuffer(buffer);
                return;
            }
            finishFade();
            StreamPlayer outgoing = player;
            InputStream outgoingStream = activeStream;
            BufferedSong outgoingBuffer = activeBuffer;
            player = incoming;
            activeStream = stream;
            activeBuffer = buffer;
            activeRevision = revision;
            fadingPlayer = overlap ? outgoing : null;
            fadingStream = overlap ? outgoingStream : null;
            fadingBuffer = overlap ? outgoingBuffer : null;
            activeGain = overlap ? 0f : 1f;
            fadingGain = overlap ? 1f : 0f;
            fadeStartedNanos = System.nanoTime();
            fadeDurationNanos = (long) (crossfadeSeconds() * 1_000_000_000d);
            if (!overlap) {
                if (outgoing != null) outgoing.stop();
                closeStream(outgoingStream);
                closeBuffer(outgoingBuffer);
            }
            currentSong = song;
            currentFile = file;
            basePositionMs = 0L;
            progressOffsetMs = 0L;
            playStartedAtMs = System.currentTimeMillis();
            totalDurationMs = Math.max(0L, song.durationMs());
            pendingSeek = null;
            visualizerLine = incoming.getSourceDataLine();
            visualizerSnapshot = MusicPcmVisualizer.EMPTY;
            playing = true;
            changingSong = false;
            pendingSong = null;
            status = "Playing";
            synchronized (playlist) {
                int index = playlist.indexOf(song);
                if (index >= 0) currentIndex = index;
            }
            synchronized (lyrics) {
                lyrics.clear();
                lyricSnapshot = List.of();
            }
            applyVolumeToPlayer();
            lyricExecutor.execute(() -> loadLyrics(song, revision));
            if (file != null) trimSongCache(file.toPath());
        } catch (Exception exception) {
            incoming.stop();
            closeStream(stream);
            closeBuffer(buffer);
            failRequest(song, revision, exception);
        }
    }

    private void loadLyrics(Song song, long revision) {
        try {
            List<LyricLine> loaded = List.copyOf(NeteaseMusicApi.getLyric(song.id()));
            executor.execute(() -> {
                if (activeRevision != revision || currentSong != song) return;
                synchronized (lyrics) {
                    lyrics.clear();
                    lyrics.addAll(loaded);
                    lyricSnapshot = loaded;
                }
            });
        } catch (Exception exception) {
            PVPUtils.LOGGER.error("[LyricsDisplay] Failed to load lyrics id={} reason={}", song.id(), cleanMessage(exception));
        }
    }

    private void failRequest(Song song, long revision, Exception exception) {
        if (revision != playbackRevision.get()) return;
        changingSong = false;
        pendingSong = null;
        retryAfterNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        status = "Load failed: " + cleanMessage(exception);
        PVPUtils.LOGGER.error("[NeteaseMusic] Failed to play song id={} name={} reason={}",
                song.id(), song.name(), cleanMessage(exception), exception);
    }

    public static float crossfadeSeconds() {
        float value = Config.neteaseMusicCrossfadeSeconds;
        return Float.isFinite(value) ? Math.clamp(value, 0f, 10f) : 3f;
    }

    private void updatePlayback() {
        try {
            if (fadingPlayer != null && playing) {
                float progress = Math.clamp((System.nanoTime() - fadeStartedNanos) / (float) Math.max(1L, fadeDurationNanos), 0f, 1f);
                activeGain = progress;
                fadingGain = 1f - progress;
                applyVolumeToPlayer();
                if (progress >= 1f) finishFade();
            }
            if (playing && !changingSong && System.nanoTime() >= retryAfterNanos && totalDurationMs > 0L
                    && totalDurationMs - positionMs() <= Math.max(15_000L, Math.round(crossfadeSeconds() * 1000f))) {
                Song next = nextSong();
                if (next != null) requestSong(next, true);
            }
        } catch (Exception exception) {
            PVPUtils.LOGGER.error("[NeteaseMusic] Playback update failed", exception);
        }
    }

    private Song nextSong() {
        synchronized (playlist) {
            if (playlist.isEmpty()) return null;
            int index = switch (playbackMode) {
                case LOOP -> currentIndex;
                case RANDOM -> (int) (Math.random() * playlist.size());
                case LIST -> (currentIndex + 1) % playlist.size();
            };
            return playlist.get(index);
        }
    }

    private void finishFade() {
        StreamPlayer outgoing = fadingPlayer;
        fadingPlayer = null;
        activeGain = 1f;
        fadingGain = 0f;
        if (outgoing != null) outgoing.stop();
        closeStream(fadingStream);
        fadingStream = null;
        closeBuffer(fadingBuffer);
        fadingBuffer = null;
        applyVolumeToPlayer();
    }

    private static void closeStream(InputStream stream) {
        if (stream != null) try {
            stream.close();
        } catch (Exception ignored) {
        }
    }

    private static void closeBuffer(BufferedSong buffer) {
        if (buffer != null) try {
            buffer.close();
        } catch (Exception ignored) {
        }
    }

    public void toggle() {
        if (currentSong == null) {
            status = "No song selected";
            return;
        }
        if (player == null || player.isStopped()) {
            playSong(currentSong);
            return;
        }
        if (playing) {
            if (changingSong) {
                playbackRevision.incrementAndGet();
                changingSong = false;
                pendingSong = null;
            }
            long revision = playbackRevision.get();
            basePositionMs = positionMs();
            playStartedAtMs = 0L;
            playing = false;
            status = "Paused";
            visualizerSnapshot = MusicPcmVisualizer.EMPTY;
            executor.execute(() -> {
                try {
                    if (revision != playbackRevision.get()) return;
                    finishFade();
                    player.pause();
                } catch (Exception ignored) {
                }
            });
            return;
        }
        playStartedAtMs = System.currentTimeMillis();
        playing = true;
        status = "Playing";
        long revision = playbackRevision.get();
        executor.execute(() -> {
            try {
                if (revision != playbackRevision.get()) return;
                player.resume();
                applyVolumeToPlayer();
            } catch (Exception ignored) {
            }
        });
    }

    public void stop() {
        long revision = playbackRevision.incrementAndGet();
        pendingSeek = null;
        visualizerSnapshot = MusicPcmVisualizer.EMPTY;
        playing = false;
        changingSong = false;
        pendingSong = null;
        basePositionMs = 0L;
        playStartedAtMs = 0L;
        progressOffsetMs = 0L;
        status = "Stopped";
        executor.execute(() -> {
            if (revision != playbackRevision.get()) return;
            finishFade();
            StreamPlayer stopped = player;
            player = null;
            if (stopped != null) stopped.stop();
            closeStream(activeStream);
            activeStream = null;
            closeBuffer(activeBuffer);
            activeBuffer = null;
            playing = false;
            basePositionMs = 0L;
            playStartedAtMs = 0L;
            progressOffsetMs = 0L;
            status = "Stopped";
            if (currentSong == null) {
                currentFile = null;
                totalDurationMs = 0L;
                synchronized (lyrics) {
                    lyrics.clear();
                    lyricSnapshot = List.of();
                }
            }
        });
    }

    public void playNext() {
        Song next;
        synchronized (playlist) {
            if (playlist.isEmpty()) {
                status = "No playlist";
                return;
            }
            int selected = pendingSong == null ? currentIndex : playlist.indexOf(pendingSong);
            if (selected < 0) selected = currentIndex;
            int nextIndex = switch (playbackMode) {
                case LOOP -> selected;
                case RANDOM -> (int) (Math.random() * playlist.size());
                case LIST -> (selected + 1) % playlist.size();
            };
            next = playlist.get(nextIndex);
        }
        playSong(next);
    }

    public void playPrevious() {
        Song previous;
        synchronized (playlist) {
            if (playlist.isEmpty()) {
                status = "No playlist";
                return;
            }
            int selected = pendingSong == null ? currentIndex : playlist.indexOf(pendingSong);
            if (selected < 0) selected = currentIndex;
            int previousIndex = switch (playbackMode) {
                case LOOP -> selected;
                case RANDOM -> (int) (Math.random() * playlist.size());
                case LIST -> selected <= 0 ? playlist.size() - 1 : selected - 1;
            };
            previous = playlist.get(previousIndex);
        }
        playSong(previous);
    }

    public void cyclePlaybackMode() {
        PlaybackMode[] values = PlaybackMode.values();
        playbackMode = values[(playbackMode.ordinal() + 1) % values.length];
        if (changingSong) {
            playbackRevision.incrementAndGet();
            changingSong = false;
            pendingSong = null;
        }
    }

    public Song currentSong() {
        return currentSong;
    }

    public boolean isPlaying() {
        return playing;
    }

    public MusicPcmVisualizer.Snapshot visualizerSnapshot() {
        MusicPcmVisualizer.Snapshot snapshot = visualizerSnapshot;
        if (!playing || !Config.dynamicIsland || !Config.dynamicIslandLyrics
                || System.currentTimeMillis() - snapshot.timestampMs() > 750L) return MusicPcmVisualizer.EMPTY;
        return snapshot;
    }

    public long positionMs() {
        PendingSeek seek = pendingSeek;
        if (seek != null && seek.source() == player && seek.revision() == playbackRevision.get()) {
            return seek.targetMs();
        }
        if (!playing || playStartedAtMs <= 0L) {
            return Math.min(basePositionMs, Math.max(0L, totalDurationMs));
        }
        long position = basePositionMs + (System.currentTimeMillis() - playStartedAtMs);
        if (totalDurationMs > 0L && position >= totalDurationMs) {
            return totalDurationMs;
        }
        return position;
    }

    public long totalDurationMs() {
        return totalDurationMs;
    }

    public float volume() {
        return volume;
    }

    public void setVolume(float volume) {
        setVolume(volume, true);
    }

    public void setVolume(float volume, boolean persist) {
        this.volume = Math.max(0.0F, Math.min(1.0F, volume));
        if (persist) {
            Config.neteaseMusicVolume = this.volume;
            Config.save();
        }
        executor.execute(() -> {
            try {
                applyVolumeToPlayer();
            } catch (Exception ignored) {
            }
        });
    }

    public int currentIndex() {
        return currentIndex;
    }

    public void seekToProgress(float progress) {
        if (!Float.isFinite(progress) || player == null || totalDurationMs <= 0L) return;
        progress = Math.max(0.0F, Math.min(1.0F, progress));
        long targetMs = Math.round(totalDurationMs * progress);
        if (totalDurationMs > 0L) {
            targetMs = Math.min(targetMs, Math.max(0L, totalDurationMs - 500L));
        }
        if (activeBuffer != null) {
            seekBuffered(targetMs);
            return;
        }
        if (currentFile == null) return;
        long seekTargetMs = targetMs;
        long revision = playbackRevision.get();
        PendingSeek seek = new PendingSeek(seekRevision.incrementAndGet(), revision, player, targetMs);
        pendingSeek = seek;
        executor.execute(() -> {
            try {
                if (revision == playbackRevision.get() && seek.request() == seekRevision.get()
                        && seek.source() == player) {
                    finishFade();
                    trySeek(seekTargetMs);
                }
            } finally {
                if (pendingSeek == seek) pendingSeek = null;
            }
        });
    }

    public String status() {
        return status;
    }

    public PlaybackMode playbackMode() {
        return playbackMode;
    }

    public List<LyricLine> lyricsSnapshot() {
        return lyricSnapshot;
    }

    private static String cleanMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private final class PlaybackListener implements StreamPlayerListener {
        private final StreamPlayer source;

        private PlaybackListener(StreamPlayer source) {
            this.source = source;
        }

        @Override
        public void opened(Object dataSource, java.util.Map<String, Object> properties) {
        }

        @Override
        public void progress(int encodedBytes, long microsecondPosition, byte[] pcmData, java.util.Map<String, Object> properties) {
            if (source != player) return;
            long request = seekRevision.get();
            if (pendingSeek == null) {
                executor.execute(() -> {
                    if (source != player || pendingSeek != null || request != seekRevision.get()) return;
                    basePositionMs = Math.min(Math.max(0L, totalDurationMs), progressOffsetMs + microsecondPosition / 1000L);
                    if (playing) playStartedAtMs = System.currentTimeMillis();
                });
            }
            if (!playing || !Config.dynamicIsland || !Config.dynamicIslandLyrics) {
                visualizerSnapshot = MusicPcmVisualizer.EMPTY;
                return;
            }
            long now = System.currentTimeMillis();
            if (now - visualizerSnapshot.timestampMs() < 25L) return;
            SourceDataLine line = visualizerLine;
            if (line != null && line.isOpen()) {
                visualizerSnapshot = MusicPcmVisualizer.analyze(pcmData, line.getFormat(), now);
            }
        }

        @Override
        public void statusUpdated(StreamPlayerEvent event) {
            executor.execute(() -> handleStatus(source, event));
        }
    }

    public float bufferedProgress() {
        BufferedSong buffer = activeBuffer;
        if (buffer == null) return currentFile == null ? 0f : 1f;
        return totalDurationMs > 0L ? Math.clamp(buffer.bufferedMs() / (float) totalDurationMs, 0f, 1f) : 0f;
    }

    private void seekBuffered(long targetMs) {
        BufferedSong buffer = activeBuffer;
        if (buffer == null) return;
        if (targetMs > 0L && targetMs + 250L > buffer.bufferedMs()) {
            status = Config.isChinese ? "目标位置尚未缓冲" : "Requested position is not buffered yet";
            return;
        }
        long revision = playbackRevision.get();
        long request = seekRevision.incrementAndGet();
        StreamPlayer previous = player;
        PendingSeek seek = new PendingSeek(request, revision, previous, targetMs);
        pendingSeek = seek;
        downloads.execute(() -> {
            StreamPlayer replacement = new StreamPlayer(Logger.getLogger("com.goxr3plus.streamplayer"), audioExecutor, eventExecutor);
            replacement.addStreamPlayerListener(new PlaybackListener(replacement));
            InputStream stream = null;
            try {
                BufferedSong.Playback playback = buffer.open(targetMs);
                stream = playback.stream();
                replacement.open(stream);
                InputStream prepared = stream;
                executor.execute(() -> {
                    if (revision != playbackRevision.get() || request != seekRevision.get()
                            || previous != player || activeBuffer != buffer) {
                        replacement.stop();
                        closeStream(prepared);
                        if (pendingSeek == seek) pendingSeek = null;
                        return;
                    }
                    boolean resume = playing;
                    try {
                        finishFade();
                        setPlayerGain(previous, 0f);
                        if (resume) previous.pause();
                        SourceDataLine previousLine = previous.getSourceDataLine();
                        if (previousLine != null && previousLine.isOpen()) {
                            previousLine.stop();
                            previousLine.flush();
                        }
                        replacement.play();
                        setPlayerGain(replacement, 0f);
                        if (!resume) replacement.pause();
                        InputStream outgoing = activeStream;
                        activeStream = prepared;
                        progressOffsetMs = playback.positionMs();
                        basePositionMs = playback.positionMs();
                        playStartedAtMs = resume ? System.currentTimeMillis() : 0L;
                        playing = resume;
                        visualizerLine = replacement.getSourceDataLine();
                        visualizerSnapshot = MusicPcmVisualizer.EMPTY;
                        player = replacement;
                        previous.stop();
                        closeStream(outgoing);
                        status = resume ? "Playing" : "Paused";
                        applyVolumeToPlayer();
                    } catch (Exception exception) {
                        replacement.stop();
                        closeStream(prepared);
                        if (player == previous) {
                            try {
                                if (resume && previous.isPaused()) previous.resume();
                            } catch (Exception ignored) {
                            }
                            applyVolumeToPlayer();
                        }
                        status = "Seek failed: " + cleanMessage(exception);
                    } finally {
                        if (pendingSeek == seek) pendingSeek = null;
                    }
                });
            } catch (Exception exception) {
                replacement.stop();
                closeStream(stream);
                executor.execute(() -> {
                    if (request == seekRevision.get() && previous == player) status = "Seek failed: " + cleanMessage(exception);
                    if (pendingSeek == seek) pendingSeek = null;
                });
            }
        });
    }

    private void handleStatus(StreamPlayer source, StreamPlayerEvent event) {
        if (source != player) return;
        Status playerStatus = event.getPlayerStatus();
        if (playerStatus == Status.PLAYING || playerStatus == Status.RESUMED) {
            if (source.isPaused() || source.isStopped()) return;
            playing = true;
            status = "Playing";
            applyVolumeToPlayer();
        } else if (playerStatus == Status.PAUSED) {
            if (!source.isPaused()) return;
            playing = false;
            status = "Paused";
            visualizerSnapshot = MusicPcmVisualizer.EMPTY;
        } else if (playerStatus == Status.STOPPED || playerStatus == Status.EOM) {
            visualizerSnapshot = MusicPcmVisualizer.EMPTY;
            long endToleranceMs = Math.min(1500L, Math.max(250L, totalDurationMs / 100L));
            boolean ended = playerStatus == Status.EOM
                    || totalDurationMs > 0L && basePositionMs >= totalDurationMs - endToleranceMs;
            playing = false;
            playStartedAtMs = 0L;
            status = ended ? "Ended" : "Playback interrupted";
            if (ended) basePositionMs = totalDurationMs;
            if (ended && !changingSong) {
                playNext();
            }
        }
    }

    private void trySeek(long targetMs) {
        try {
            int totalSeconds = (int) Math.max(1L, totalDurationMs / 1000L);
            int targetSeconds = (int) Math.max(0L, Math.min(totalSeconds - 1L, targetMs / 1000L));
            player.seekTo(targetSeconds);
            basePositionMs = targetSeconds * 1000L;
            progressOffsetMs = basePositionMs;
            playStartedAtMs = playing ? System.currentTimeMillis() : 0L;
            status = playing ? "Playing" : "Paused";
        } catch (Exception exception) {
            status = "Seek failed: " + cleanMessage(exception);
        }
    }

    private void applyVolumeToPlayer() {
        setPlayerGain(player, volume * activeGain);
        setPlayerGain(fadingPlayer, volume * fadingGain);
    }

    private static void setPlayerGain(StreamPlayer source, float gain) {
        if (source == null) return;
        try {
            SourceDataLine line = source.getSourceDataLine();
            if (line == null || !line.isOpen()) return;
            gain = Float.isFinite(gain) ? Math.clamp(gain, 0f, 1f) : 0f;
            if (line.isControlSupported(BooleanControl.Type.MUTE)) {
                ((BooleanControl) line.getControl(BooleanControl.Type.MUTE)).setValue(gain == 0f);
            }
            if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl control = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                float decibels = gain > 0f ? (float) (20d * Math.log10(gain)) : control.getMinimum();
                control.setValue(Math.clamp(decibels, control.getMinimum(), control.getMaximum()));
            }
        } catch (Exception ignored) {
        }
    }

    private static Path cachePath(Song song) throws Exception {
        Path cacheDir = cacheDirectory();
        Files.createDirectories(cacheDir);
        return cacheDir.resolve(song.id() + ".mp3");
    }

    private static Path cacheDirectory() {
        return FabricLoader.getInstance().getGameDir().resolve("PVPUtils").resolve("music-cache");
    }

    private static void touchCacheFile(Path cachedFile) {
        try {
            Files.setLastModifiedTime(cachedFile, FileTime.fromMillis(System.currentTimeMillis()));
        } catch (Exception ignored) {
        }
    }

    private static void trimSongCache(Path activeFile) {
        try (var stream = Files.list(cacheDirectory())) {
            List<Path> cachedSongs = stream
                    .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".mp3"))
                    .sorted((left, right) -> {
                        try {
                            return Files.getLastModifiedTime(right).compareTo(Files.getLastModifiedTime(left));
                        } catch (Exception ignored) {
                            return 0;
                        }
                    })
                    .toList();
            for (int i = MAX_CACHED_SONGS; i < cachedSongs.size(); i++) {
                Path path = cachedSongs.get(i);
                if (!path.equals(activeFile)) {
                    Files.deleteIfExists(path);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static String formatTime(long ms) {
        long seconds = Math.max(0L, ms) / 1000L;
        return String.format("%02d:%02d", seconds / 60L, seconds % 60L);
    }

    public enum PlaybackMode {
        LOOP("Loop"),
        LIST("List"),
        RANDOM("Random");

        private final String label;

        PlaybackMode(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
