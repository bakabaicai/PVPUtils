package com.pvp_utils.client.plugin;

import java.util.LinkedHashMap;
import java.util.Map;

final class PluginApprovalGate {
    record Request(long ticket, PluginSource source, PluginSecurityAudit.Report report) {
        String id() {
            return source.manifest().id();
        }
    }

    private final Map<String, Request> requests = new LinkedHashMap<>();
    private long sequence;

    Request submit(PluginSource source, PluginSecurityAudit.Report report) {
        if (report.failed() || !report.requiresApproval()) throw new IllegalArgumentException("Invalid approval report");
        if (!requests.containsKey(source.manifest().id()) && requests.size() >= 64) {
            throw new IllegalStateException("Approval queue limit");
        }
        Request request = new Request(++sequence, source, report);
        requests.put(request.id(), request);
        return request;
    }

    Request next() {
        return requests.values().stream().findFirst().orElse(null);
    }

    boolean pending(String id) {
        return requests.containsKey(id);
    }

    boolean current(Request request) {
        return requests.get(request.id()) == request;
    }

    boolean approve(Request request, PluginSource latest) {
        if (!current(request)) return false;
        requests.remove(request.id());
        return request.id().equals(latest.manifest().id())
                && request.source().fingerprint().equals(latest.fingerprint());
    }

    void cancel(String id) {
        requests.remove(id);
    }

    void cancel(Request request) {
        if (current(request)) requests.remove(request.id());
    }

    void clear() {
        requests.clear();
    }
}
