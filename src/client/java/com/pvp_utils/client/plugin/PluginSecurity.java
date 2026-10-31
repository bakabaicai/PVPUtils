package com.pvp_utils.client.plugin;

import com.pvp_utils.Config;
import com.pvp_utils.client.gui.clickgui.UiText;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class PluginSecurity {
    public static final String CLEAN = "clean";
    public static final String SUSPICIOUS = "suspicious";
    public static final String MALICIOUS = "malicious";

    private static final long MAX_SCRIPT_BYTES = 512L * 1024L;
    private static final int MAX_LINE_LENGTH = 2000;
    private static final int MAX_REASONS = 6;
    private static final Map<String, Audit> CACHE = new ConcurrentHashMap<>();

    public record Reason(String zh, String en) {
        String text() {
            return UiText.t(zh, en);
        }
    }

    private record Rule(Pattern pattern, int score, boolean critical, Reason reason, boolean codeOnly) {
    }

    private static final List<Rule> RULES = List.of(
            codeRule(3, true, "反射加载类（Class.forName）", "Reflective class loading (Class.forName)",
                    "Class\\s*\\.\\s*forName\\s*\\("),
            codeRule(3, true, "直接访问 Java 类（java.*）", "Direct Java class access (java.*)",
                    "\\bjava\\s*\\.\\s*(?:lang|io|nio|net|security|util)\\s*\\."),
            codeRule(3, true, "获取宿主类对象（getClass）", "Host class access (getClass)",
                    "\\bgetClass\\s*\\(\\s*\\)"),
            codeRule(3, true, "动态类型加载（JavaAdapter/importClass/loadClass/getClassLoader）",
                    "Dynamic type loading (JavaAdapter/importClass/loadClass/getClassLoader)",
                    "\\b(?:JavaAdapter|importClass|importPackage|loadClass|getClassLoader|defineClass)\\b"
                            + "|\\bJava\\s*\\.\\s*type\\s*\\(|\\bPackages\\s*\\."),
            codeRule(3, true, "进程或系统调用（Runtime/ProcessBuilder/System.exit）",
                    "Process or system calls (Runtime/ProcessBuilder/System.exit)",
                    "\\b(?:Runtime\\s*\\.\\s*getRuntime|ProcessBuilder|System\\s*\\.\\s*exit)\\b"),
            codeRule(3, true, "内部 API 访问（Unsafe/AccessController/sun.misc）",
                    "Internal API access (Unsafe/AccessController/sun.misc)",
                    "\\b(?:Unsafe|AccessController|sun\\s*\\.\\s*misc|jdk\\s*\\.\\s*internal)\\b"),
            codeRule(2, false, "动态执行代码（eval）", "Dynamic code execution (eval)",
                    "\\beval\\s*\\("),
            codeRule(2, false, "运行时构造函数（new Function）", "Runtime constructor (new Function)",
                    "\\bnew\\s+Function\\s*\\("),
            codeRule(1, false, "网络访问（fetch/XMLHttpRequest/WebSocket）",
                    "Network access (fetch/XMLHttpRequest/WebSocket)",
                    "\\b(?:fetch\\s*\\(|XMLHttpRequest|WebSocket)\\b"),
            codeRule(1, false, "字符串编解码（atob/btoa/fromCharCode）",
                    "String encoding helpers (atob/btoa/fromCharCode)",
                    "\\b(?:atob|btoa|fromCharCode)\\b"),
            literalRule(2, "超长 Base64 数据", "Very long Base64 payload",
                    "[A-Za-z0-9+/]{512,}={0,2}"),
            literalRule(2, "十六进制混淆字符串", "Hex-obfuscated string",
                    "(?:\\\\x[0-9a-fA-F]{2}){24,}")
    );

    private static final Pattern DYNAMIC_EXECUTION =
            Pattern.compile("\\beval\\s*\\(|\\bnew\\s+Function\\s*\\(");
    private static final Pattern PAYLOAD_DECODING =
            Pattern.compile("\\b(?:atob|btoa|fromCharCode)\\b|[A-Za-z0-9+/]{512,}={0,2}");

    private PluginSecurity() {
    }

    public record Audit(String level, int score, List<Reason> reasons) {
        public boolean malicious() {
            return MALICIOUS.equals(level);
        }

        public boolean suspicious() {
            return SUSPICIOUS.equals(level) || MALICIOUS.equals(level);
        }

        public boolean blocked() {
            return malicious() && Config.pluginSecurity;
        }

        public String detail() {
            List<String> texts = new ArrayList<>(reasons.size());
            for (Reason reason : reasons) {
                texts.add(reason.text());
            }
            return String.join("; ", texts);
        }
    }

    public static Audit audit(PluginManifest manifest) {
        Path entry = manifest.entry();
        try {
            long size = Files.size(entry);
            String key = entry + "|" + size + "|" + Files.getLastModifiedTime(entry).toMillis();
            Audit cached = CACHE.get(key);
            if (cached != null) {
                return cached;
            }
            if (size > MAX_SCRIPT_BYTES) {
                return cache(key, new Audit(SUSPICIOUS, 2, List.of(
                        new Reason("脚本体积异常（超过 512 KiB）", "Abnormal script size (over 512 KiB)"))));
            }
            return cache(key, scan(Files.readString(entry, StandardCharsets.UTF_8)));
        } catch (IOException | RuntimeException error) {
            return new Audit(SUSPICIOUS, 1, List.of(new Reason(
                    "入口脚本无法读取，无法完成安全检查",
                    "Entry script cannot be read; security check incomplete")));
        }
    }

    private static Audit cache(String key, Audit audit) {
        if (CACHE.size() > 256) {
            CACHE.clear();
        }
        CACHE.put(key, audit);
        return audit;
    }

    static Audit scan(String source) {
        String code = stripCommentsAndStrings(source);
        LinkedHashSet<Reason> reasons = new LinkedHashSet<>();
        int score = 0;
        boolean critical = false;
        for (Rule rule : RULES) {
            String text = rule.codeOnly() ? code : source;
            if (!rule.pattern().matcher(text).find()) {
                continue;
            }
            score += rule.score();
            critical |= rule.critical();
            reasons.add(rule.reason());
        }
        if (hasLongLine(source)) {
            score += 1;
            reasons.add(new Reason("存在超长单行代码（可能用于混淆）",
                    "Line longer than 2000 characters (possible obfuscation)"));
        }
        if (DYNAMIC_EXECUTION.matcher(code).find() && PAYLOAD_DECODING.matcher(source).find()) {
            critical = true;
            reasons.add(new Reason("动态执行与编码解码同时出现（典型混淆载荷）",
                    "Dynamic execution combined with decoding (typical obfuscated payload)"));
        }
        String level = critical || score >= 4 ? MALICIOUS : score > 0 ? SUSPICIOUS : CLEAN;
        List<Reason> limited = reasons.size() > MAX_REASONS
                ? List.copyOf(new ArrayList<>(reasons).subList(0, MAX_REASONS))
                : List.copyOf(reasons);
        return new Audit(level, score, limited);
    }

    private static String stripCommentsAndStrings(String source) {
        StringBuilder code = new StringBuilder(source.length());
        int index = 0;
        int length = source.length();
        char previous = ' ';
        while (index < length) {
            char current = source.charAt(index);
            if (current == '/' && index + 1 < length && source.charAt(index + 1) == '/') {
                while (index < length && source.charAt(index) != '\n') {
                    index++;
                }
                continue;
            }
            if (current == '/' && index + 1 < length && source.charAt(index + 1) == '*') {
                index += 2;
                while (index + 1 < length && !(source.charAt(index) == '*' && source.charAt(index + 1) == '/')) {
                    index++;
                }
                index = Math.min(length, index + 2);
                code.append(' ');
                continue;
            }
            if (current == '/' && startsRegex(previous)) {
                index = skipRegex(source, index);
                code.append("''");
                previous = '\'';
                continue;
            }
            if (current == '"' || current == '\'') {
                index = skipQuoted(source, index, current);
                code.append("''");
                previous = '\'';
                continue;
            }
            if (current == '`') {
                index++;
                while (index < length) {
                    char next = source.charAt(index);
                    if (next == '\\') {
                        index += 2;
                        continue;
                    }
                    if (next == '`') {
                        index++;
                        break;
                    }
                    if (next == '$' && index + 1 < length && source.charAt(index + 1) == '{') {
                        int start = index + 2;
                        int end = interpolationEnd(source, start);
                        code.append(' ').append(stripCommentsAndStrings(source.substring(start, end))).append(' ');
                        index = Math.min(length, end + 1);
                        continue;
                    }
                    index++;
                }
                code.append("''");
                previous = '\'';
                continue;
            }
            code.append(current);
            if (!Character.isWhitespace(current)) {
                previous = current;
            }
            index++;
        }
        return code.toString();
    }

    private static boolean startsRegex(char previous) {
        return switch (previous) {
            case ' ', '\t', '\n', '\r', '(', '[', '{', ',', ';', ':', '=', '!', '&', '|', '?',
                 '+', '-', '*', '%', '~', '^', '<', '>', '/' -> true;
            default -> false;
        };
    }

    private static int skipRegex(String source, int start) {
        int index = start + 1;
        int length = source.length();
        boolean inClass = false;
        while (index < length) {
            char current = source.charAt(index);
            if (current == '\\') {
                index += 2;
                continue;
            }
            if (current == '\n') {
                return index;
            }
            index++;
            if (current == '[') {
                inClass = true;
            } else if (current == ']') {
                inClass = false;
            } else if (current == '/' && !inClass) {
                return index;
            }
        }
        return length;
    }

    private static int skipQuoted(String source, int start, char quote) {
        int index = start + 1;
        int length = source.length();
        while (index < length) {
            char current = source.charAt(index);
            if (current == '\\') {
                index += 2;
                continue;
            }
            index++;
            if (current == quote) {
                break;
            }
        }
        return index;
    }

    private static int interpolationEnd(String source, int start) {
        int depth = 1;
        int index = start;
        int length = source.length();
        while (index < length) {
            char current = source.charAt(index);
            if (current == '\\') {
                index += 2;
                continue;
            }
            if (current == '"' || current == '\'' || current == '`') {
                index = skipQuoted(source, index, current);
                continue;
            }
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
            index++;
        }
        return length;
    }

    private static boolean hasLongLine(String source) {
        for (String line : source.split("\n", -1)) {
            if (line.length() > MAX_LINE_LENGTH) {
                return true;
            }
        }
        return false;
    }

    private static Rule codeRule(int score, boolean critical, String zh, String en, String regex) {
        return new Rule(Pattern.compile(regex), score, critical, new Reason(zh, en), true);
    }

    private static Rule literalRule(int score, String zh, String en, String regex) {
        return new Rule(Pattern.compile(regex), score, false, new Reason(zh, en), false);
    }
}
