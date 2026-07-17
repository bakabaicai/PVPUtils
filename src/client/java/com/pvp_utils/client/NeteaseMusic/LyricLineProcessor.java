package com.pvp_utils.client.NeteaseMusic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LyricLineProcessor {
    private static final Pattern LINE_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})](.*)");

    private LyricLineProcessor() {
    }

    public static List<LyricLine> parse(String text) {
        List<LyricLine> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return lines;
        }

        for (String line : text.replace("\\n", "\n").split("\\R")) {
            Matcher matcher = LINE_PATTERN.matcher(line);
            if (!matcher.matches()) {
                continue;
            }

            long minutes = Long.parseLong(matcher.group(1));
            long seconds = Long.parseLong(matcher.group(2));
            long fraction = Long.parseLong(matcher.group(3));
            if (matcher.group(3).length() == 2) {
                fraction *= 10L;
            }
            lines.add(new LyricLine(matcher.group(4), minutes * 60_000L + seconds * 1000L + fraction));
        }

        lines.sort(Comparator.comparingLong(LyricLine::timeMs));
        return lines;
    }

    public static int currentIndex(List<LyricLine> lyrics, long timeMs) {
        int index = -1;
        for (int i = 0; i < lyrics.size(); i++) {
            if (lyrics.get(i).timeMs() > timeMs) {
                break;
            }
            index = i;
        }
        return index;
    }
}
