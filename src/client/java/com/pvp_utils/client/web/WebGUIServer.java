package com.pvp_utils.client.web;

import com.pvp_utils.Config;
import com.pvp_utils.client.util.ChatUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class WebGUIServer {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String HTML_RESOURCE = "/web/WebGUI.html";

    private static HttpServer server;
    private static ExecutorService executor;
    private static String token;
    private static int port;

    private WebGUIServer() {
    }

    public static synchronized boolean isEnabled() {
        return server != null;
    }

    public static synchronized void setEnabled(boolean enabled) {
        if (enabled) {
            open();
        } else {
            stop();
        }
    }

    public static synchronized void open() {
        try {
            ensureStarted();
            openBrowser(URI.create("http://127.0.0.1:" + port + "/?token=" + token));
            ChatUtils.success(Config.isChinese ? "WebGUI 已打开。" : "WebGUI opened.");
        } catch (Exception e) {
            ChatUtils.error((Config.isChinese ? "WebGUI 打开失败：" : "Failed to open WebGUI: ") + e.getMessage());
        }
    }

    public static synchronized void stop() {
        if (server == null) {
            return;
        }
        server.stop(0);
        server = null;
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        token = null;
        port = 0;
        ChatUtils.success(Config.isChinese ? "WebGUI 已关闭。" : "WebGUI closed.");
    }

    private static void ensureStarted() throws IOException {
        if (server != null) {
            return;
        }

        token = createToken();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.createContext("/", WebGUIServer::handleIndex);
        server.createContext("/api/ping", WebGUIServer::handlePing);
        server.createContext("/api/categoriesList", WebGUIServer::handleCategoriesList);
        server.createContext("/api/modulesList", WebGUIServer::handleModulesList);
        server.createContext("/api/setStatus", WebGUIServer::handleSetStatus);
        server.createContext("/api/getModuleSetting", WebGUIServer::handleGetModuleSetting);
        server.createContext("/api/setModuleSettingValue", WebGUIServer::handleSetModuleSettingValue);
        server.createContext("/api/commandsList", WebGUIServer::handleCommandsList);
        server.createContext("/api/executeCommand", WebGUIServer::handleExecuteCommand);
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "pvp-utils-webgui");
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(executor);
        server.start();
    }

    private static void handleIndex(HttpExchange exchange) throws IOException {
        if (!isAuthorized(exchange)) {
            send(exchange, 403, "text/plain; charset=utf-8", "Forbidden");
            return;
        }
        send(exchange, 200, "text/html; charset=utf-8", loadHtml());
    }

    private static void handlePing(HttpExchange exchange) throws IOException {
        if (!isAuthorized(exchange)) {
            send(exchange, 403, "application/json; charset=utf-8", "{\"ok\":false}");
            return;
        }
        send(exchange, 200, "application/json; charset=utf-8", "{\"ok\":true,\"name\":\"PVPUtils WebGUI\",\"isChinese\":" + Config.isChinese + "}");
    }

    private static void handleCategoriesList(HttpExchange exchange) throws IOException {
        if (!isAuthorized(exchange)) {
            send(exchange, 403, "application/json; charset=utf-8", "{\"success\":false,\"reason\":\"Forbidden\"}");
            return;
        }
        send(exchange, 200, "application/json; charset=utf-8", WebGUIModules.categoriesJson());
    }

    private static void handleModulesList(HttpExchange exchange) throws IOException {
        if (!isAuthorized(exchange)) {
            send(exchange, 403, "application/json; charset=utf-8", "{\"success\":false,\"reason\":\"Forbidden\"}");
            return;
        }
        Map<String, String> query = query(exchange);
        send(exchange, 200, "application/json; charset=utf-8", WebGUIModules.modulesJson(query.getOrDefault("category", "Combat")));
    }

    private static void handleSetStatus(HttpExchange exchange) throws IOException {
        if (!isAuthorized(exchange)) {
            send(exchange, 403, "application/json; charset=utf-8", "{\"success\":false,\"reason\":\"Forbidden\"}");
            return;
        }
        Map<String, String> query = query(exchange);
        String module = query.getOrDefault("module", "");
        boolean state = Boolean.parseBoolean(query.getOrDefault("state", "false"));
        send(exchange, 200, "application/json; charset=utf-8", WebGUIModules.setStatusJson(module, state));
    }

    private static void handleGetModuleSetting(HttpExchange exchange) throws IOException {
        if (!isAuthorized(exchange)) {
            send(exchange, 403, "application/json; charset=utf-8", "{\"success\":false,\"reason\":\"Forbidden\"}");
            return;
        }
        Map<String, String> query = query(exchange);
        send(exchange, 200, "application/json; charset=utf-8", WebGUIModules.settingsJson(query.getOrDefault("module", "")));
    }

    private static void handleSetModuleSettingValue(HttpExchange exchange) throws IOException {
        if (!isAuthorized(exchange)) {
            send(exchange, 403, "application/json; charset=utf-8", "{\"success\":false,\"reason\":\"Forbidden\"}");
            return;
        }
        Map<String, String> query = query(exchange);
        send(exchange, 200, "application/json; charset=utf-8",
                WebGUIModules.setSettingJson(
                        query.getOrDefault("module", ""),
                        query.getOrDefault("name", ""),
                        query.getOrDefault("value", "")));
    }

    private static void handleCommandsList(HttpExchange exchange) throws IOException {
        if (!isAuthorized(exchange)) {
            send(exchange, 403, "application/json; charset=utf-8", "{\"success\":false,\"reason\":\"Forbidden\"}");
            return;
        }
        send(exchange, 200, "application/json; charset=utf-8", WebGUICommands.commandsJson());
    }

    private static void handleExecuteCommand(HttpExchange exchange) throws IOException {
        if (!isAuthorized(exchange)) {
            send(exchange, 403, "application/json; charset=utf-8", "{\"success\":false,\"reason\":\"Forbidden\"}");
            return;
        }
        Map<String, String> query = query(exchange);
        send(exchange, 200, "application/json; charset=utf-8",
                WebGUICommands.executeJson(query.getOrDefault("id", ""), query.getOrDefault("value", "")));
    }

    private static boolean isAuthorized(HttpExchange exchange) {
        return token != null && token.equals(query(exchange).get("token"));
    }

    private static Map<String, String> query(HttpExchange exchange) {
        Map<String, String> result = new HashMap<>();
        String rawQuery = exchange.getRequestURI().getRawQuery();
        if (rawQuery == null || rawQuery.isBlank()) return result;
        for (String part : rawQuery.split("&")) {
            int split = part.indexOf('=');
            if (split <= 0) continue;
            String key = decode(part.substring(0, split));
            String value = decode(part.substring(split + 1));
            result.put(key, value);
        }
        return result;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static void send(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static String loadHtml() {
        try (InputStream input = WebGUIServer.class.getResourceAsStream(HTML_RESOURCE)) {
            if (input == null) {
                return fallbackHtml();
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return fallbackHtml();
        }
    }

    private static String fallbackHtml() {
        return "<!doctype html><meta charset=\"utf-8\"><title>PVPUtils WebGUI</title><h1>PVPUtils WebGUI</h1><p>HTML resource not found.</p>";
    }

    private static String createToken() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static void openBrowser(URI uri) throws IOException {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(uri);
                return;
            } catch (Exception ignored) {
            }
        }
        new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", uri.toString()).start();
    }
}
