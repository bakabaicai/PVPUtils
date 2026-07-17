package com.pvp_utils.client.NeteaseMusic;

import com.pvp_utils.PVPUtils;
import com.goxr3plus.streamplayer.enums.Status;
import com.goxr3plus.streamplayer.stream.StreamPlayer;
import com.goxr3plus.streamplayer.stream.StreamPlayerEvent;
import com.goxr3plus.streamplayer.stream.StreamPlayerListener;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MusicPlaybackService implements StreamPlayerListener {
    public static final MusicPlaybackService INSTANCE = new MusicPlaybackService();

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "PVPUtils-NeteaseMusic");
        thread.setDaemon(true);
        return thread;
    });
    private final StreamPlayer player = new StreamPlayer();
    private final List<Song> playlist = Collections.synchronizedList(new ArrayList<>());
    private final List<LyricLine> lyrics = Collections.synchronizedList(new ArrayList<>());
    private volatile Song currentSong;
    private volatile File currentFile;
    private volatile boolean playing;
    private volatile boolean changingSong;
    private volatile long basePositionMs;
    private volatile long playStartedAtMs;
    private volatile long totalDurationMs;
    private volatile long progressOffsetMs;
    private volatile float volume = 1.0F;
    private volatile String status = "Idle";
    private volatile PlaybackMode playbackMode = PlaybackMode.LIST;
    private volatile int currentIndex;

    private MusicPlaybackService() {
        player.addStreamPlayerListener(this);
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

    public void playSong(Song song) {
        if (song == null) {
            return;
        }
        synchronized (playlist) {
            int index = playlist.indexOf(song);
            if (index >= 0) {
                currentIndex = index;
            }
        }
        currentSong = song;
        basePositionMs = 0L;
        playStartedAtMs = 0L;
        progressOffsetMs = 0L;
        totalDurationMs = Math.max(0L, song.durationMs());
        playing = false;
        changingSong = true;
        status = "Loading " + song.name();
        synchronized (lyrics) {
            lyrics.clear();
        }
        executor.execute(() -> {
            try {
                SongFile songFile = NeteaseMusicApi.getSongFile(song.id());
                if (!songFile.isPlayable()) {
                    status = "Song is unavailable";
                    return;
                }
                Path cachedFile = cachePath(song);
                if (!Files.exists(cachedFile) || (songFile.size() > 0L && Files.size(cachedFile) < songFile.size())) {
                    status = "Downloading " + song.name();
                    Files.write(cachedFile, NeteaseMusicApi.getBytes(songFile.url()));
                }
                List<LyricLine> loadedLyrics;
                try {
                    loadedLyrics = NeteaseMusicApi.getLyric(song.id());
                } catch (Exception ignored) {
                    loadedLyrics = List.of();
                    PVPUtils.LOGGER.warn("[LyricsDisplay] lyric-load-failed id={} reason={}", song.id(), cleanMessage(ignored));
                }
                synchronized (lyrics) {
                    lyrics.clear();
                    lyrics.addAll(loadedLyrics);
                }
                currentFile = cachedFile.toFile();
                player.stop();
                player.open(currentFile);
                player.setGain(volume);
                basePositionMs = 0L;
                progressOffsetMs = 0L;
                playStartedAtMs = System.currentTimeMillis();
                playing = true;
                status = "Playing";
                player.play();
            } catch (Exception exception) {
                status = "Load failed: " + cleanMessage(exception);
                playing = false;
            } finally {
                changingSong = false;
            }
        });
    }

    public void toggle() {
        if (currentSong == null) {
            status = "No song selected";
            return;
        }
        if (playing) {
            basePositionMs = positionMs();
            playStartedAtMs = 0L;
            playing = false;
            status = "Paused";
            executor.execute(() -> {
                try {
                    player.pause();
                } catch (Exception ignored) {
                }
            });
            return;
        }
        playStartedAtMs = System.currentTimeMillis();
        playing = true;
        status = "Playing";
        executor.execute(() -> {
            try {
                player.resume();
            } catch (Exception ignored) {
            }
        });
    }

    public void stop() {
        playing = false;
        basePositionMs = 0L;
        playStartedAtMs = 0L;
        progressOffsetMs = 0L;
        status = "Stopped";
        executor.execute(player::stop);
    }

    public void playNext() {
        Song next;
        synchronized (playlist) {
            if (playlist.isEmpty()) {
                status = "No playlist";
                return;
            }
            currentIndex = switch (playbackMode) {
                case LOOP -> currentIndex;
                case RANDOM -> (int) (Math.random() * playlist.size());
                case LIST -> (currentIndex + 1) % playlist.size();
            };
            next = playlist.get(currentIndex);
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
            currentIndex = switch (playbackMode) {
                case LOOP -> currentIndex;
                case RANDOM -> (int) (Math.random() * playlist.size());
                case LIST -> currentIndex <= 0 ? playlist.size() - 1 : currentIndex - 1;
            };
            previous = playlist.get(currentIndex);
        }
        playSong(previous);
    }

    public void cyclePlaybackMode() {
        PlaybackMode[] values = PlaybackMode.values();
        playbackMode = values[(playbackMode.ordinal() + 1) % values.length];
    }

    public Song currentSong() {
        return currentSong;
    }

    public boolean isPlaying() {
        return playing;
    }

    public long positionMs() {
        if (!playing || playStartedAtMs <= 0L) {
            return Math.min(basePositionMs, Math.max(0L, totalDurationMs));
        }
        long position = basePositionMs + (System.currentTimeMillis() - playStartedAtMs);
        if (totalDurationMs > 0L && position >= totalDurationMs) {
            if (playbackMode == PlaybackMode.LOOP) {
                basePositionMs = 0L;
                playStartedAtMs = System.currentTimeMillis();
                return 0L;
            }
            playing = false;
            basePositionMs = totalDurationMs;
            playStartedAtMs = 0L;
            status = "Ended";
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
        this.volume = Math.max(0.0F, Math.min(1.0F, volume));
        executor.execute(() -> {
            try {
                player.setGain(this.volume);
            } catch (Exception ignored) {
            }
        });
    }

    public int currentIndex() {
        return currentIndex;
    }

    public void seekToProgress(float progress) {
        progress = Math.max(0.0F, Math.min(1.0F, progress));
        long targetMs = Math.round(totalDurationMs * progress);
        basePositionMs = targetMs;
        progressOffsetMs = targetMs;
        playStartedAtMs = playing ? System.currentTimeMillis() : 0L;
        executor.execute(() -> trySeek(targetMs));
    }

    public String status() {
        return status;
    }

    public PlaybackMode playbackMode() {
        return playbackMode;
    }

    public List<LyricLine> lyricsSnapshot() {
        synchronized (lyrics) {
            return List.copyOf(lyrics);
        }
    }

    private static String cleanMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    @Override
    public void opened(Object dataSource, java.util.Map<String, Object> properties) {
    }

    @Override
    public void progress(int encodedBytes, long microsecondPosition, byte[] pcmData, java.util.Map<String, Object> properties) {
        basePositionMs = Math.min(Math.max(0L, totalDurationMs), progressOffsetMs + microsecondPosition / 1000L);
        if (playing) {
            playStartedAtMs = System.currentTimeMillis();
        }
    }

    @Override
    public void statusUpdated(StreamPlayerEvent event) {
        Status playerStatus = event.getPlayerStatus();
        if (playerStatus == Status.PLAYING || playerStatus == Status.RESUMED) {
            playing = true;
            status = "Playing";
        } else if (playerStatus == Status.PAUSED) {
            playing = false;
            status = "Paused";
        } else if (playerStatus == Status.STOPPED) {
            boolean ended = totalDurationMs > 0L && basePositionMs >= totalDurationMs * 0.90F;
            playing = false;
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
            status = playing ? "Playing" : "Paused";
        } catch (Exception exception) {
            status = "Seek failed: " + cleanMessage(exception);
        }
    }

    private static Path cachePath(Song song) throws Exception {
        Path cacheDir = FabricLoader.getInstance().getGameDir().resolve("PVPUtils").resolve("music-cache");
        Files.createDirectories(cacheDir);
        return cacheDir.resolve(song.id() + ".mp3");
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
