package com.pvp_utils.client.modules.impl.Combat.combo;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;

final class ComboVoiceFactory implements ComboAudioEngine.VoiceFactory {
    @Override
    public ComboAudioEngine.Voice prepare(ComboAudioEngine.Pcm pcm, float volume,
                                             Runnable completion) throws Exception {
        Clip clip = AudioSystem.getClip();
        try {

            byte[] samples = scaledSamples(pcm.bytes(), volume);
            clip.open(pcm.format(), samples, 0, samples.length);
            ClipVoice voice = new ClipVoice(clip);
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) completion.run();
            });
            return voice;
        } catch (Exception | LinkageError exception) {
            clip.close();
            throw exception;
        }
    }

    static byte[] scaledSamples(byte[] source, float volume) {
        if (volume >= 1.0f) return source;
        byte[] scaled = new byte[source.length];
        for (int index = 0; index < source.length; index += 2) {
            short value = (short) ((source[index] & 0xff) | ((source[index + 1] & 0xff) << 8));
            int adjusted = Math.round(value * volume);
            scaled[index] = (byte) adjusted;
            scaled[index + 1] = (byte) (adjusted >> 8);
        }
        return scaled;
    }

    private static final class ClipVoice implements ComboAudioEngine.Voice {
        private final Clip clip;
        private boolean closed;

        private ClipVoice(Clip clip) {
            this.clip = clip;
        }

        @Override
        public synchronized void start() {
            if (closed) return;
            clip.setFramePosition(0);
            clip.start();
        }

        @Override
        public synchronized void stopAndClose() {
            if (closed) return;
            closed = true;
            try {
                clip.stop();
            } finally {
                try {
                    clip.flush();
                } finally {
                    clip.close();
                }
            }
        }
    }
}
