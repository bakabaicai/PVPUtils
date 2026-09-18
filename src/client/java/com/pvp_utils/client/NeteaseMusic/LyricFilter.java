package com.pvp_utils.client.NeteaseMusic;

import java.util.ArrayList;
import java.util.List;

public final class LyricFilter {
    private static final String DEFAULT_RULES = """
            ^%SONGNAME%-*
            ^*-%SONGNAME*
            ^%SONGNAME%
            Drums{：/:/ :}*
            Piano{：/:/ :}*
            Producers{：/:/ :}*
            Backing Vocals & Choirs{：/:/ :}*
            Lead Vocals{：/:/ :}*
            Lead Guitar{：/:/ :}*
            Rhythm Guitar{：/:/ :}*
            Bass Guitar{：/:/ :}*
            Synthesizer Programming{：/:/ :}*
            Lyric{：/:/ :}*
            编曲{：/:/ :}*
            作曲{：/:/ :}*
            演唱{：/:/ :}*
            键盘{：/:/ :}*
            主唱{：/:/ :}*
            混音{：/:/ :}*
            混音助理{：/:/ :}*
            母带处理{：/:/ :}*
            鸣谢{：/:/ :}*
            歌词{：/:/ :}*
            歌词制作{：/:/ :}*
            词{：/:/ :}*
            曲{：/:/ :}*
            翻译{：/:/ :}*
            词*{：/:/ :}*
            曲*{：/:/ :}*
            作词*{：/:/ :}*
            钢琴*{：/:/ :}*
            录音*{：/:/ :}*
            录音棚*{：/:/ :}*
            作曲*{：/:/ :}*
            编曲*{：/:/ :}*
            调声*{：/:/ :}*
            弦乐*{：/:/ :}*
            吉他*{：/:/ :}*
            贝斯*{：/:/ :}*
            鼓*{：/:/ :}*
            和声*{：/:/ :}*
            音频*{：/:/ :}*
            混音*{：/:/ :}*
            母带*{：/:/ :}*
            制作*{：/:/ :}*
            监制*{：/:/ :}*
            歌词制作*{：/:/ :}*
            歌词翻译*{：/:/ :}*
            时间轴*{：/:/ :}*
            音效*{：/:/ :}*
            伴唱*{：/:/ :}*
            音乐监制*{：/:/ :}*
            执行监制*{：/:/ :}*
            出品*{：/:/ :}*
            后期*{：/:/ :}*
            企划*{：/:/ :}*
            统筹*{：/:/ :}*
            原唱*{：/:/ :}*
            演唱*{：/:/ :}*
            翻唱*{：/:/ :}*
            混缩*{：/:/ :}*
            人声*{：/:/ :}*
            封面*{：/:/ :}*
            海报*{：/:/ :}*
            版权*{：/:/ :}*
            发行{：/:/ :}*
            录音室{：/:/ :}*
            混音室{：/:/ :}*
            母带室{：/:/ :}*
            母带工程师{：/:/ :}*
            混音工程师{：/:/ :}*
            录音工程师{：/:/ :}*
            音乐制作人{：/:/ :}*
            配唱制作{：/:/ :}*
            宣发{：/:/ :}*
            特别鸣谢{：/:/ :}*
            艺人{：/:/ :}*
            歌手{：/:/ :}*
            制谱*{：/:/ :}*
            OP*{：/:/ :}*
            SP*{：/:/ :}*
            ^*{{ - }}*
            """;

    private static List<FilterRule> cachedRules;
    private static String cachedRulesText;

    private LyricFilter() {
    }

    public static boolean shouldFilter(String line, String songName) {
        return shouldFilter(line, songName, DEFAULT_RULES);
    }

    public static boolean shouldFilter(String line, String songName, String rulesText) {
        if (line == null || line.isBlank()) return true;
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return true;

        List<FilterRule> rules = parseRules(rulesText);
        for (FilterRule rule : rules) {
            if (rule.matches(trimmed, songName)) {
                return true;
            }
        }
        return false;
    }

    private static List<FilterRule> parseRules(String rulesText) {
        if (rulesText != null && rulesText.equals(cachedRulesText) && cachedRules != null) {
            return cachedRules;
        }
        List<FilterRule> rules = new ArrayList<>();
        if (rulesText == null || rulesText.isBlank()) {
            cachedRules = rules;
            cachedRulesText = rulesText;
            return rules;
        }
        for (String rawLine : rulesText.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("//")) continue;
            FilterRule rule = parseRule(line);
            if (rule != null) {
                rules.add(rule);
            }
        }
        cachedRules = rules;
        cachedRulesText = rulesText;
        return rules;
    }

    private static FilterRule parseRule(String line) {
        boolean firstLineOnly = false;
        String pattern = line;
        if (pattern.startsWith("^")) {
            firstLineOnly = true;
            pattern = pattern.substring(1);
        }
        return new FilterRule(pattern, firstLineOnly);
    }

    private record FilterRule(String pattern, boolean firstLineOnly) {
        boolean matches(String text, String songName) {
            String expanded = pattern.replace("%SONGNAME%", songName != null ? songName : "");
            return matchPattern(expanded, text, 0, 0);
        }

        private boolean matchPattern(String pattern, String text, int pi, int ti) {
            while (pi < pattern.length() && ti < text.length()) {
                char pc = pattern.charAt(pi);
                if (pc == '*') {
                    pi++;
                    if (pi >= pattern.length()) return true;
                    for (int i = ti; i <= text.length(); i++) {
                        if (matchPattern(pattern, text, pi, i)) return true;
                    }
                    return false;
                } else if (pc == '{') {
                    int close = pattern.indexOf('}', pi + 1);
                    if (close < 0) return false;
                    String content = pattern.substring(pi + 1, close);
                    if (content.startsWith("{{") && content.endsWith("}}")) {
                        String literal = content.substring(2, content.length() - 2);
                        if (text.startsWith(literal, ti)) {
                            return matchPattern(pattern, text, close + 1, ti + literal.length());
                        }
                    } else {
                        String[] options = splitOptions(content);
                        for (String opt : options) {
                            if (!opt.isEmpty() && text.startsWith(opt, ti)) {
                                if (matchPattern(pattern, text, close + 1, ti + opt.length())) {
                                    return true;
                                }
                            }
                        }
                    }
                    return false;
                } else {
                    if (pc != text.charAt(ti)) return false;
                    pi++;
                    ti++;
                }
            }
            while (pi < pattern.length() && pattern.charAt(pi) == '*') pi++;
            return pi >= pattern.length() && ti >= text.length();
        }

        private String[] splitOptions(String content) {
            List<String> parts = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                if (c == '/' && !isEscaped(content, i)) {
                    parts.add(current.toString());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }
            parts.add(current.toString());
            return parts.toArray(new String[0]);
        }

        private boolean isEscaped(String s, int i) {
            int backslashes = 0;
            int j = i - 1;
            while (j >= 0 && s.charAt(j) == '\\') {
                backslashes++;
                j--;
            }
            return backslashes % 2 == 1;
        }
    }
}
