package com.pvp_utils;

import net.fabricmc.loader.api.FabricLoader;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class VictorySound {
    private static final Path EXTERNAL_PATH = FabricLoader.getInstance().getGameDir().resolve("PVPUtils/sounds");
    private static final String[] BUILTIN_SOUNDS = {"ACE.wav", "Clutch.wav", "Dominating.wav", "Rampage.wav", "Flawless.wav"};
    private static String lastPlayed = "";
    private static long lastPlayTime = 0;
    private static final long PLAY_COOLDOWN = 5000;

    public static void init() {
        try {
            if (!Files.exists(EXTERNAL_PATH)) {
                Files.createDirectories(EXTERNAL_PATH);
            }
            for (String sound : BUILTIN_SOUNDS) {
                Path target = EXTERNAL_PATH.resolve(sound);
                if (!Files.exists(target)) {
                    String internalPath = "/sounds/" + sound;
                    try (InputStream is = VictorySound.class.getResourceAsStream(internalPath)) {
                        if (is != null) {
                            Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void play() {
        if (!Config.victorySound) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPlayTime < PLAY_COOLDOWN) {
            return;
        }
        lastPlayTime = currentTime;

        File folder = EXTERNAL_PATH.toFile();
        File[] files = folder.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".wav") || name.toLowerCase().endsWith(".mp3")
        );

        if (files == null || files.length == 0) return;

        List<File> pool = new ArrayList<>();
        for (File f : files) {
            if (!f.getName().equals(lastPlayed) || files.length == 1) {
                pool.add(f);
            }
        }

        File toPlay = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        lastPlayed = toPlay.getName();

        new Thread(() -> {
            try {
                AudioInputStream stream = AudioSystem.getAudioInputStream(toPlay);
                AudioFormat format = stream.getFormat();

                if (format.getEncoding() == AudioFormat.Encoding.ULAW ||
                        format.getEncoding() == AudioFormat.Encoding.ALAW) {
                    AudioFormat newFormat = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            format.getSampleRate(), 16, format.getChannels(),
                            format.getChannels() * 2, format.getSampleRate(), false);
                    stream = AudioSystem.getAudioInputStream(newFormat, stream);
                }

                DataLine.Info info = new DataLine.Info(Clip.class, stream.getFormat());
                Clip clip = (Clip) AudioSystem.getLine(info);
                clip.open(stream);
                clip.start();
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}