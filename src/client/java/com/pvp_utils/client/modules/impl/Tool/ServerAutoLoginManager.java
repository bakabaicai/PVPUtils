package com.pvp_utils.client.modules.impl.Tool;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pvp_utils.Config;
import com.pvp_utils.client.render.MainUI.PVPUtilsMultiplayerScreen;
import com.pvp_utils.client.util.ChatUtils;
import com.pvp_utils.client.util.PasswordCipher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ServerAutoLoginManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long PROMPT_RETRY_COOLDOWN_MS = 10000L;
    private static final long MANUAL_LOGIN_SUPPRESS_MS = 30000L;
    private static boolean wasInGame = false;
    private static String lastLoggedInAddress = "";
    private static String pendingAddress = "";
    private static int ticksRemaining = -1;
    private static long lastPromptLoginMs;
    private static long lastManualLoginMs;
    private static boolean autoSending;

    private ServerAutoLoginManager() {
    }

    public static void noteOutgoingCommand(String command) {
        if (autoSending || command == null) {
            return;
        }
        String lower = command.toLowerCase(Locale.ROOT);
        if (lower.equals("login") || lower.startsWith("login ") || lower.equals("l") || lower.startsWith("l ")) {
            lastManualLoginMs = System.currentTimeMillis();
        }
    }

    private static void sendLoginCommand(Minecraft client, String password) {
        autoSending = true;
        try {
            client.player.connection.sendCommand("login " + password);
        } finally {
            autoSending = false;
        }
        ChatUtils.send(Config.isChinese ? "已自动执行服务器登录。" : "Server login command sent automatically.");
    }

    public static void tick(Minecraft client) {
        String address = currentAddress(client);
        boolean inGame = address != null && client.player != null && client.level != null;
        if (inGame && !wasInGame) {
            boolean newConnection = !address.equals(lastLoggedInAddress);
            lastLoggedInAddress = address;
            Rule rule = newConnection && Config.serverAutoLogin && Config.serverAutoLoginMode == 0 ? ruleFor(address) : null;
            if (rule != null && rule.enabled && !rule.password.isBlank()) {
                pendingAddress = address;
                ticksRemaining = Math.max(0, rule.delay) * 20;
            } else {
                ticksRemaining = -1;
            }
        } else if (!inGame) {
            if (backAtMenu(client.screen)) {
                lastLoggedInAddress = "";
            }
            pendingAddress = "";
            ticksRemaining = -1;
        }
        wasInGame = inGame;

        if (ticksRemaining < 0) {
            return;
        }
        if (client.player == null || client.level == null) {
            pendingAddress = "";
            ticksRemaining = -1;
            return;
        }
        if (ticksRemaining-- > 0) {
            return;
        }
        ticksRemaining = -1;

        String targetAddress = pendingAddress;
        pendingAddress = "";
        Rule rule = ruleFor(targetAddress);
        if (rule == null) {
            return;
        }
        String password = PasswordCipher.decrypt(rule.password);
        if (password.isBlank()) {
            return;
        }
        sendLoginCommand(client, password);
    }

    public static void onSystemChatMessage(String text) {
        if (text == null || !text.contains("/login")) {
            return;
        }
        if (Config.serverAutoLoginMode != 1 || !Config.serverAutoLogin) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        String address = currentAddress(client);
        if (address == null || client.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastPromptLoginMs < PROMPT_RETRY_COOLDOWN_MS) {
            return;
        }
        if (now - lastManualLoginMs < MANUAL_LOGIN_SUPPRESS_MS) {
            return;
        }
        Rule rule = ruleFor(address);
        if (rule == null || !rule.enabled || rule.password.isBlank()) {
            return;
        }
        String password = PasswordCipher.decrypt(rule.password);
        if (password.isBlank()) {
            return;
        }
        lastPromptLoginMs = now;
        sendLoginCommand(client, password);
        ChatUtils.send(Config.isChinese ? "检测到登录提示，已自动执行服务器登录。" : "Login prompt detected; login command sent automatically.");
    }

    public static String currentAddress(Minecraft client) {
        if (client == null) {
            return null;
        }
        ServerData server = client.getCurrentServer();
        if (server == null || server.ip == null || server.ip.isBlank()) {
            return null;
        }
        return server.ip;
    }

    private static boolean backAtMenu(Screen screen) {
        return screen instanceof TitleScreen
                || screen instanceof JoinMultiplayerScreen
                || screen instanceof PVPUtilsMultiplayerScreen
                || screen instanceof DisconnectedScreen
                || screen instanceof ConnectScreen;
    }

    public static Map<String, Rule> rules() {
        Map<String, Rule> result = new LinkedHashMap<>();
        String stored = Config.serverAutoLoginRules;
        if (stored == null || stored.isBlank()) {
            return result;
        }
        try {
            JsonElement element = JsonParser.parseString(stored);
            if (!element.isJsonObject()) {
                return result;
            }
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject object = entry.getValue().getAsJsonObject();
                Rule rule = new Rule();
                rule.enabled = object.has("enabled") && object.get("enabled").getAsBoolean();
                rule.password = object.has("password") ? object.get("password").getAsString() : "";
                rule.delay = object.has("delay") ? object.get("delay").getAsInt() : 1;
                result.put(entry.getKey(), rule);
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    public static Rule ruleFor(String address) {
        return rules().get(address);
    }

    public static void saveRules(Map<String, Rule> values) {
        JsonObject root = new JsonObject();
        for (Map.Entry<String, Rule> entry : values.entrySet()) {
            Rule rule = entry.getValue();
            if (rule == null || (!rule.enabled && rule.password.isBlank())) {
                continue;
            }
            JsonObject object = new JsonObject();
            object.addProperty("enabled", rule.enabled);
            object.addProperty("password", rule.password == null ? "" : rule.password);
            object.addProperty("delay", rule.delay);
            root.add(entry.getKey(), object);
        }
        Config.serverAutoLoginRules = root.size() == 0 ? "" : GSON.toJson(root);
        Config.save();
    }

    public static void setRule(String address, String plainPassword, int delaySeconds) {
        Map<String, Rule> values = rules();
        Rule rule = values.computeIfAbsent(address, key -> new Rule());
        rule.enabled = true;
        rule.password = PasswordCipher.encrypt(plainPassword);
        rule.delay = Math.max(0, Math.min(30, delaySeconds));
        saveRules(values);
    }

    public static boolean removeRule(String address) {
        Map<String, Rule> values = rules();
        boolean removed = values.remove(address) != null;
        if (removed) {
            saveRules(values);
        }
        return removed;
    }

    public static boolean hasPassword(String address) {
        Rule rule = ruleFor(address);
        return rule != null && !rule.password.isBlank();
    }

    public static void setPassword(String address, String plainPassword) {
        Map<String, Rule> values = rules();
        Rule rule = values.computeIfAbsent(address, key -> new Rule());
        rule.enabled = true;
        rule.password = PasswordCipher.encrypt(plainPassword);
        saveRules(values);
    }

    public static int delayOf(String address) {
        Rule rule = ruleFor(address);
        return rule == null ? 1 : Math.max(0, rule.delay);
    }

    public static void setDelay(String address, int delaySeconds) {
        Map<String, Rule> values = rules();
        Rule rule = values.get(address);
        if (rule == null) {
            return;
        }
        rule.delay = Math.max(0, Math.min(30, delaySeconds));
        saveRules(values);
    }

    public static final class Rule {
        public boolean enabled;
        public String password = "";
        public int delay = 1;
    }
}
