package com.pvp_utils.client.plugin;

import com.pvp_utils.Config;

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
    private static final Map<String, Audit> CACHE = new ConcurrentHashMap<>();

    private record Rule(Pattern pattern, int score, boolean critical, String label) {
    }

    private static final List<Rule> RULES = List.of(
            rule(3, true, "反射加载类（Class.forName）",
                    "Class\\s*\\.\\s*forName\\s*\\("),
            rule(3, true, "直接访问 Java 类（java.*）",
                    "\\bjava\\s*\\.\\s*(?:lang|io|nio|net|security|util)\\s*\\."),
            rule(3, true, "获取宿主类对象（getClass）",
                    "\\bgetClass\\s*\\(\\s*\\)"),
            rule(3, true, "动态类型加载（JavaAdapter/importClass/loadClass/getClassLoader）",
                    "\\b(?:JavaAdapter|importClass|importPackage|loadClass|getClassLoader|defineClass)\\b"),
            rule(3, true, "进程或系统调用（Runtime/ProcessBuilder/System.exit）",
                    "\\b(?:Runtime\\s*\\.\\s*getRuntime|ProcessBuilder|System\\s*\\.\\s*exit)\\b"),
            rule(3, true, "内部 API 访问（Unsafe/AccessController/sun.misc）",
                    "\\b(?:Unsafe|AccessController|sun\\s*\\.\\s*misc|jdk\\s*\\.\\s*internal)\\b"),
            rule(1, false, "动态执行代码（eval）",
                    "\\beval\\s*\\("),
            rule(1, false, "运行时构造函数（new Function）",
                    "\\bnew\\s+Function\\s*\\("),
            rule(1, false, "网络访问（fetch/XMLHttpRequest/WebSocket）",
                    "\\b(?:fetch\\s*\\(|XMLHttpRequest|WebSocket)\\b"),
            rule(1, false, "字符串编解码（atob/btoa/fromCharCode）",
                    "\\b(?:atob|btoa|fromCharCode)\\b"),
            rule(2, false, "超长 Base64 数据",
                    "[A-Za-z0-9+/]{512,}={0,2}"),
            rule(2, false, "十六进制混淆字符串",
                    "(?:\\\\x[0-9a-fA-F]{2}){24,}")
    );

    private PluginSecurity() {
    }

    public record Audit(String level, int score, List<String> reasons) {
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
            return String.join("; ", reasons);
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
            Audit audit = scan(Files.readString(entry, StandardCharsets.UTF_8), size);
            if (CACHE.size() > 256) {
                CACHE.clear();
            }
            CACHE.put(key, audit);
            return audit;
        } catch (IOException | RuntimeException error) {
            return new Audit(SUSPICIOUS, 1, List.of("入口脚本无法读取，无法完成安全检查"));
        }
    }

    static Audit scan(String source, long size) {
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        int score = 0;
        boolean critical = false;
        for (Rule rule : RULES) {
            if (!rule.pattern().matcher(source).find()) {
                continue;
            }
            score += rule.score();
            critical |= rule.critical();
            reasons.add(rule.label());
        }
        if (size > MAX_SCRIPT_BYTES) {
            score += 2;
            reasons.add("脚本体积异常（超过 512 KiB）");
        }
        if (hasLongLine(source)) {
            score += 1;
            reasons.add("存在超长单行代码（可能用于混淆）");
        }
        String level = critical || score >= 4 ? MALICIOUS : score > 0 ? SUSPICIOUS : CLEAN;
        if (reasons.size() > 6) {
            reasons = new LinkedHashSet<>(new ArrayList<>(reasons).subList(0, 6));
        }
        return new Audit(level, score, List.copyOf(reasons));
    }

    private static boolean hasLongLine(String source) {
        for (String line : source.split("\n", -1)) {
            if (line.length() > MAX_LINE_LENGTH) {
                return true;
            }
        }
        return false;
    }

    private static Rule rule(int score, boolean critical, String label, String regex) {
        return new Rule(Pattern.compile(regex), score, critical, label);
    }
}
