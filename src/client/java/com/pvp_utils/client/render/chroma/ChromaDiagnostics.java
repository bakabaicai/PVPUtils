package com.pvp_utils.client.render.chroma;

import com.pvp_utils.PVPUtils;

import java.util.LinkedHashMap;
import java.util.Map;

final class ChromaDiagnostics {
    private static final long REPEAT_INTERVAL = 10_000_000_000L;
    private static final long TRANSIENT_INTERVAL = 2_000_000_000L;
    private static final Map<String, Notice> NOTICES = new LinkedHashMap<>();

    private ChromaDiagnostics() {
    }

    static void warn(String event, Object source, String detail, Throwable failure) {
        report(event, source, detail, failure, false, false);
    }

    static void delayedWarn(String event, Object source, String detail) {
        report(event, source, detail, null, false, true);
    }

    static void error(String event, Object source, String detail, Throwable failure) {
        report(event, source, detail, failure, true, false);
    }

    static synchronized void resolved(String event, Object source) {
        if (NOTICES.isEmpty()) return;
        String name = sourceName(source);
        Notice notice = NOTICES.remove(event + ':' + name);
        if (notice != null && notice.logged) {
            PVPUtils.LOGGER.info("Chroma Graphics [{}] source={} recovered; suppressed={}",
                    event, name, notice.suppressed);
        }
    }

    static String sourceName(Object source) {
        if (source == null) return "framework";
        if (!(source instanceof Class<?> type)) return source.toString();
        String name = type.getName();
        int lambda = name.indexOf("$$Lambda");
        return lambda >= 0 ? name.substring(0, lambda) : name;
    }

    private static synchronized void report(String event, Object source, String detail, Throwable failure,
                                            boolean error, boolean delayed) {
        String name = sourceName(source);
        String key = event + ':' + name;
        long now = System.nanoTime();
        Notice notice = NOTICES.get(key);
        if (notice == null) {
            if (NOTICES.size() >= 128) NOTICES.remove(NOTICES.keySet().iterator().next());
            notice = new Notice(now);
            NOTICES.put(key, notice);
        } else {
            notice.suppressed++;
        }
        if (delayed && !notice.logged && now - notice.firstSeen < TRANSIENT_INTERVAL) return;
        boolean newTrace = failure != null && !notice.traced;
        boolean newError = error && !notice.errorLogged;
        if (notice.logged && !newTrace && !newError && now - notice.lastLogged < REPEAT_INTERVAL) return;
        Throwable trace = newTrace ? failure : null;
        if (error) {
            PVPUtils.LOGGER.error("Chroma Graphics [{}] source={} {}; suppressed={}",
                    event, name, detail, notice.suppressed, trace);
        } else {
            PVPUtils.LOGGER.warn("Chroma Graphics [{}] source={} {}; suppressed={}",
                    event, name, detail, notice.suppressed, trace);
        }
        notice.logged = true;
        notice.traced |= newTrace;
        notice.errorLogged |= error;
        notice.lastLogged = now;
        notice.suppressed = 0;
    }

    private static final class Notice {
        private final long firstSeen;
        private long lastLogged;
        private int suppressed;
        private boolean logged;
        private boolean traced;
        private boolean errorLogged;

        private Notice(long firstSeen) {
            this.firstSeen = firstSeen;
        }
    }
}
