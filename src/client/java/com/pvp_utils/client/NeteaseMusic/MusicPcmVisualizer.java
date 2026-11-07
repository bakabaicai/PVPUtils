package com.pvp_utils.client.NeteaseMusic;

import javax.sound.sampled.AudioFormat;
import java.util.Arrays;

public final class MusicPcmVisualizer {
    public static final Snapshot EMPTY = new Snapshot(0L, 0f, 0f, 0f, 0f, 0f, 0f, 0f);
    private static final int MAX_FRAMES = 512;
    private static final double[] FREQUENCIES = {90.0, 180.0, 360.0, 720.0, 1440.0, 2880.0, 5760.0};

    private MusicPcmVisualizer() {
    }

    public static Snapshot analyze(byte[] pcmData, AudioFormat format, long timestampMs) {
        if (pcmData == null || format == null || !AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding())) return EMPTY;
        int bits = format.getSampleSizeInBits();
        int channels = format.getChannels();
        int frameSize = format.getFrameSize();
        float sampleRate = format.getSampleRate();
        if ((bits != 8 && bits != 16) || channels < 1 || channels > 8 || frameSize < channels * (bits / 8)
                || !Float.isFinite(sampleRate) || sampleRate <= 0f) return EMPTY;
        int frameCount = Math.min(MAX_FRAMES, pcmData.length / frameSize);
        if (frameCount < 8) return EMPTY;
        int firstFrame = pcmData.length / frameSize - frameCount;
        int sampleBytes = bits / 8;
        boolean bigEndian = format.isBigEndian();
        double[] coefficients = new double[FREQUENCIES.length];
        double[] energy = new double[FREQUENCIES.length];
        double[] previous = new double[FREQUENCIES.length];
        double[] beforePrevious = new double[FREQUENCIES.length];
        double[] window = new double[frameCount];
        double windowSum = 0.0;
        for (int band = 0; band < coefficients.length; band++) {
            double frequency = Math.min(FREQUENCIES[band], sampleRate * 0.42);
            coefficients[band] = 2.0 * Math.cos(2.0 * Math.PI * frequency / sampleRate);
        }
        for (int frame = 0; frame < frameCount; frame++) {
            window[frame] = 0.5 - 0.5 * Math.cos(2.0 * Math.PI * frame / (frameCount - 1));
            windowSum += window[frame];
        }
        for (int channel = 0; channel < channels; channel++) {
            Arrays.fill(previous, 0.0);
            Arrays.fill(beforePrevious, 0.0);
            double mean = 0.0;
            for (int frame = 0; frame < frameCount; frame++) {
                int offset = (firstFrame + frame) * frameSize + channel * sampleBytes;
                mean += sample(pcmData, offset, bits, bigEndian);
            }
            mean /= frameCount;
            for (int frame = 0; frame < frameCount; frame++) {
                int offset = (firstFrame + frame) * frameSize + channel * sampleBytes;
                double value = (sample(pcmData, offset, bits, bigEndian) - mean) * window[frame];
                for (int band = 0; band < coefficients.length; band++) {
                    double current = value + coefficients[band] * previous[band] - beforePrevious[band];
                    beforePrevious[band] = previous[band];
                    previous[band] = current;
                }
            }
            for (int band = 0; band < energy.length; band++) {
                energy[band] += Math.max(0.0, previous[band] * previous[band]
                        + beforePrevious[band] * beforePrevious[band]
                        - coefficients[band] * previous[band] * beforePrevious[band]);
            }
        }
        double normalization = 2.0 / windowSum / Math.sqrt(channels);
        return new Snapshot(timestampMs,
                level(energy[0], normalization), level(energy[1], normalization),
                level(energy[2], normalization), level(energy[3], normalization),
                level(energy[4], normalization), level(energy[5], normalization),
                level(energy[6], normalization));
    }

    private static double sample(byte[] pcmData, int offset, int bits, boolean bigEndian) {
        if (bits == 8) return pcmData[offset] / 128.0;
        int value = bigEndian
                ? (pcmData[offset] << 8) | (pcmData[offset + 1] & 0xFF)
                : (pcmData[offset + 1] << 8) | (pcmData[offset] & 0xFF);
        return value / 32768.0;
    }

    private static float level(double energy, double normalization) {
        double amplitude = Math.sqrt(energy) * normalization;
        return (float) Math.clamp(Math.sqrt(amplitude) * 1.2, 0.0, 1.0);
    }

    public record Snapshot(long timestampMs, float band0, float band1, float band2, float band3,
                           float band4, float band5, float band6) {
        public float level(int index) {
            return switch (index) {
                case 0 -> band0;
                case 1 -> band1;
                case 2 -> band2;
                case 3 -> band3;
                case 4 -> band4;
                case 5 -> band5;
                case 6 -> band6;
                default -> 0f;
            };
        }
    }
}
