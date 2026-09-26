package com.pvp_utils.client.NeteaseMusic;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LyricParser {
    private static final Pattern TIME_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]");

    public static List<LyricLine> parse(Path mp3Path) {
        List<LyricLine> lines = new ArrayList<>();
        Path lrcPath = mp3Path.resolveSibling(mp3Path.getFileName().toString().replace(".mp3", ".lrc"));
        if (!Files.exists(lrcPath)) return lines;

        try (BufferedReader reader = Files.newBufferedReader(lrcPath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = TIME_PATTERN.matcher(line);
                while (matcher.find()) {
                    int min = Integer.parseInt(matcher.group(1));
                    int sec = Integer.parseInt(matcher.group(2));
                    int ms = Integer.parseInt(matcher.group(3));
                    if (matcher.group(3).length() == 2) ms *= 10;
                    long timeMs = min * 60_000L + sec * 1000L + ms;
                    String text = line.substring(matcher.end()).trim();
                    if (!text.isEmpty()) {
                        lines.add(new LyricLine(timeMs, text));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        lines.sort((a, b) -> Long.compare(a.timeMs(), b.timeMs()));
        return lines;
    }

    public record LyricLine(long timeMs, String text) {}
}
