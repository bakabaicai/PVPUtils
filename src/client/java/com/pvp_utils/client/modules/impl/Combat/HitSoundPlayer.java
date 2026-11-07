package com.pvp_utils.client.modules.impl.Combat;

import com.pvp_utils.Config;
import com.pvp_utils.PVPUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class HitSoundPlayer {
    private static final int MAX_BYTES = 8 * 1024 * 1024;
    private static final LinkedHashMap<SoundKey, Pcm> CACHE = new LinkedHashMap<>(8, 0.75f, true);
    private static final List<Clip> ACTIVE = new ArrayList<>();
    private static final ThreadPoolExecutor WORKER = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(8), runnable -> {
                Thread thread = new Thread(runnable, "pvp-utils-hit-sound");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.DiscardPolicy());
    private static String lastFailure;
    private static volatile boolean closed;

    private HitSoundPlayer() {
    }

    public static void play(boolean ranged) {
        if (!Config.hitSound || closed || !(ranged ? Config.hitSoundRangedEnabled : Config.hitSoundMeleeEnabled)) return;
        Config.HitSoundType type = ranged ? Config.hitSoundRangedType : Config.hitSoundMeleeType;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (type != Config.HitSoundType.CUSTOM) {
            client.getSoundManager().play(SimpleSoundInstance.forUI(
                    type == Config.HitSoundType.EXPERIENCE ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.PLAYER_ATTACK_CRIT,
                    type == Config.HitSoundType.EXPERIENCE ? 0.5f : 1f, 1f));
            return;
        }
        String file = ranged ? Config.hitSoundRangedFile : Config.hitSoundMeleeFile;
        float volume = client.options.getFinalSoundSourceVolume(SoundSource.UI);
        if (file == null || file.isBlank() || volume <= 0f) return;
        WORKER.execute(() -> playFile(file, volume));
    }

    private static void playFile(String file, float volume) {
        Clip clip = null;
        try {
            if (closed) return;
            Path path = Path.of(file).toAbsolutePath().normalize();
            if (!Files.isRegularFile(path) || Files.size(path) > MAX_BYTES) {
                throw new IllegalArgumentException("Sound file is missing or exceeds 8 MiB");
            }
            SoundKey key = new SoundKey(path, Files.getLastModifiedTime(path).toMillis(), Files.size(path));
            Pcm pcm = CACHE.get(key);
            if (pcm == null) {
                pcm = decode(path);
                CACHE.entrySet().removeIf(entry -> entry.getKey().path().equals(path));
                while (CACHE.size() >= 4) CACHE.remove(CACHE.keySet().iterator().next());
                CACHE.put(key, pcm);
            }
            if (closed) return;
            clip = AudioSystem.getClip();
            Clip playing = clip;
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    playing.close();
                    synchronized (ACTIVE) { ACTIVE.remove(playing); }
                }
            });
            clip.open(pcm.format(), pcm.bytes(), 0, pcm.bytes().length);
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                gain.setValue(Math.clamp((float) (20.0 * Math.log10(volume)), gain.getMinimum(), gain.getMaximum()));
            }
            Clip retired = null;
            boolean stopped;
            synchronized (ACTIVE) {
                stopped = closed;
                if (!stopped) {
                    if (ACTIVE.size() >= 8) retired = ACTIVE.removeFirst();
                    ACTIVE.add(clip);
                }
            }
            if (retired != null) retired.close();
            if (stopped) {
                clip.close();
                return;
            }
            clip.start();
            lastFailure = null;
        } catch (Exception failure) {
            if (clip != null) clip.close();
            if (!file.equals(lastFailure)) {
                lastFailure = file;
                PVPUtils.LOGGER.warn("Unable to play custom hit sound {}", file, failure);
            }
        }
    }

    private static Pcm decode(Path path) throws Exception {
        try (AudioInputStream source = AudioSystem.getAudioInputStream(path.toFile())) {
            AudioFormat original = source.getFormat();
            float rate = original.getSampleRate();
            int channels = original.getChannels();
            if (!Float.isFinite(rate) || rate < 8000f || rate > 192000f || channels < 1 || channels > 2) {
                throw new IllegalArgumentException("Unsupported sound format");
            }
            AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, rate, 16,
                    channels, channels * 2, rate, false);
            try (AudioInputStream decoded = AudioSystem.getAudioInputStream(format, source)) {
                byte[] bytes = decoded.readNBytes(MAX_BYTES + format.getFrameSize());
                if (bytes.length == 0 || bytes.length > MAX_BYTES) throw new IllegalArgumentException("Sound is empty or too long");
                return new Pcm(format, bytes);
            }
        }
    }

    public static void close() {
        closed = true;
        WORKER.shutdownNow();
        List<Clip> clips;
        synchronized (ACTIVE) {
            clips = List.copyOf(ACTIVE);
            ACTIVE.clear();
        }
        clips.forEach(Clip::close);
    }

    private record SoundKey(Path path, long modified, long size) {
    }

    private record Pcm(AudioFormat format, byte[] bytes) {
    }
}
