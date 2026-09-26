package com.pvp_utils.client.NeteaseMusic;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class Mp3LyricExtractor {

    public static List<LyricParser.LyricLine> extract(Path mp3Path) {
        List<LyricParser.LyricLine> lines = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(mp3Path.toFile(), "r")) {
            if (raf.length() < 10) return lines;
            byte[] header = new byte[10];
            raf.readFully(header);
            if (!(header[0] == 'I' && header[1] == 'D' && header[2] == '3')) return lines;

            int tagSize = ((header[6] & 0x7F) << 21) | ((header[7] & 0x7F) << 14) | ((header[8] & 0x7F) << 7) | (header[9] & 0x7F);
            byte[] tagData = new byte[tagSize];
            raf.readFully(tagData);

            int offset = 0;
            while (offset < tagData.length - 10) {
                String frameId = new String(tagData, offset, 4, StandardCharsets.ISO_8859_1);
                int frameSize = ((tagData[offset + 4] & 0xFF) << 24) | ((tagData[offset + 5] & 0xFF) << 16) | ((tagData[offset + 6] & 0xFF) << 8) | (tagData[offset + 7] & 0xFF);
                if (frameSize <= 0 || offset + 10 + frameSize > tagData.length) break;

                if (frameId.equals("USLT") || frameId.equals("SYLT")) {
                    String lyricData = new String(tagData, offset + 10, frameSize, StandardCharsets.UTF_8);
                    parseLyricText(lyricData, lines);
                }
                offset += 10 + frameSize;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    private static void parseLyricText(String text, List<LyricParser.LyricLine> lines) {
        String[] parts = text.split("\\r?\\n");
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]").matcher(part);
            if (m.find()) {
                int min = Integer.parseInt(m.group(1));
                int sec = Integer.parseInt(m.group(2));
                int ms = Integer.parseInt(m.group(3));
                if (m.group(3).length() == 2) ms *= 10;
                long timeMs = min * 60_000L + sec * 1000L + ms;
                String lyric = part.substring(m.end()).trim();
                if (!lyric.isEmpty()) {
                    lines.add(new LyricParser.LyricLine(timeMs, lyric));
                }
            }
        }
    }
}
