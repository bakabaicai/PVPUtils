package com.pvp_utils.client.alt;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import com.pvp_utils.mixin.client.MinecraftAltAccessor;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class AltManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String EMPTY_TOKEN = "0";
    private static final List<Account> ACCOUNTS = new ArrayList<>();
    private static Account current;
    private static boolean initialized;

    private AltManager() {
    }

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;
        load();
        if (!Files.exists(file())) save();
    }

    public static synchronized List<Account> accounts() {
        return Collections.unmodifiableList(new ArrayList<>(ACCOUNTS));
    }

    public static synchronized Optional<Account> current() {
        return Optional.ofNullable(current);
    }

    public static synchronized Account addOffline(String name) {
        Account account = Account.offline(name);
        if (!account.valid()) return null;
        put(account);
        save();
        return account;
    }

    public static synchronized void add(Account account) {
        if (account == null || !account.valid()) return;
        put(account);
        save();
    }

    public static synchronized void remove(Account account) {
        ACCOUNTS.removeIf(value -> value.same(account));
        if (current != null && current.same(account)) current = null;
        save();
    }

    public static synchronized boolean login(Account account) {
        if (account == null || !account.valid()) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) return false;

        User user = account.user();
        UserApiService api = account.microsoft()
                ? new YggdrasilAuthenticationService(minecraft.getProxy()).createUserApiService(user.getAccessToken())
                : UserApiService.OFFLINE;

        MinecraftAltAccessor accessor = (MinecraftAltAccessor) minecraft;
        accessor.pvp_utils$setUser(user);
        accessor.pvp_utils$setUserApiService(api);
        accessor.pvp_utils$setProfileFuture(profileFuture(minecraft, account, user));
        accessor.pvp_utils$setUserPropertiesFuture(propertiesFuture(api));
        accessor.pvp_utils$setProfileKeyPairManager(account.microsoft()
                ? ProfileKeyPairManager.create(api, user, minecraft.gameDirectory.toPath())
                : ProfileKeyPairManager.EMPTY_KEY_MANAGER);

        account.lastUsed = System.currentTimeMillis();
        put(account);
        current = account;
        save();
        return true;
    }

    public static synchronized Path file() {
        Minecraft minecraft = Minecraft.getInstance();
        Path root = minecraft == null ? Path.of(".") : minecraft.gameDirectory.toPath();
        return root.resolve("PVPUtils").resolve("alts.json");
    }

    private static void put(Account account) {
        for (int i = 0; i < ACCOUNTS.size(); i++) {
            if (ACCOUNTS.get(i).same(account)) {
                ACCOUNTS.set(i, account);
                return;
            }
        }
        ACCOUNTS.add(account);
    }

    private static CompletableFuture<ProfileResult> profileFuture(Minecraft minecraft, Account account, User user) {
        if (!account.microsoft()) return CompletableFuture.completedFuture(null);
        return CompletableFuture.supplyAsync(() -> minecraft.services().sessionService().fetchProfile(user.getProfileId(), true));
    }

    private static CompletableFuture<UserApiService.UserProperties> propertiesFuture(UserApiService api) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return api.fetchProperties();
            } catch (AuthenticationException exception) {
                return UserApiService.OFFLINE_PROPERTIES;
            }
        });
    }

    private static synchronized void load() {
        Path path = file();
        if (!Files.exists(path)) return;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) return;
            JsonArray array = root.getAsJsonObject().getAsJsonArray("accounts");
            if (array == null) return;
            ACCOUNTS.clear();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                Account account = Account.fromJson(element.getAsJsonObject());
                if (account.valid()) ACCOUNTS.add(account);
            }
        } catch (Exception ignored) {
        }
    }

    private static synchronized void save() {
        try {
            Path path = file();
            Files.createDirectories(path.getParent());
            JsonObject root = new JsonObject();
            JsonArray array = new JsonArray();
            for (Account account : ACCOUNTS) if (account.valid()) array.add(account.toJson());
            root.add("accounts", array);
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
        }
    }

    private static UUID offlineUuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    public static final class Account {
        private String type = "offline";
        private String name = "";
        private String uuid = "";
        private String accessToken = EMPTY_TOKEN;
        private String xuid = "";
        private long lastUsed;

        public static Account offline(String name) {
            Account account = new Account();
            account.name = name == null ? "" : name.trim();
            account.uuid = offlineUuid(account.name).toString();
            return account;
        }

        public static Account microsoft(String name, UUID uuid, String token, String xuid) {
            Account account = new Account();
            account.type = "microsoft";
            account.name = name == null ? "" : name.trim();
            account.uuid = uuid == null ? "" : uuid.toString();
            account.accessToken = token == null ? "" : token;
            account.xuid = xuid == null ? "" : xuid;
            return account;
        }

        private static Account fromJson(JsonObject object) {
            Account account = new Account();
            account.type = text(object, "type", "offline");
            account.name = text(object, "name", "");
            account.uuid = text(object, "uuid", "");
            account.accessToken = text(object, "accessToken", EMPTY_TOKEN);
            account.xuid = text(object, "xuid", "");
            account.lastUsed = number(object, "lastUsed");
            return account;
        }

        private static String text(JsonObject object, String key, String fallback) {
            JsonElement element = object.get(key);
            return element == null || element.isJsonNull() ? fallback : element.getAsString();
        }

        private static long number(JsonObject object, String key) {
            try {
                return object.has(key) ? object.get(key).getAsLong() : 0L;
            } catch (RuntimeException exception) {
                return 0L;
            }
        }

        private JsonObject toJson() {
            JsonObject object = new JsonObject();
            object.addProperty("type", type);
            object.addProperty("name", name);
            object.addProperty("uuid", uuid);
            object.addProperty("accessToken", accessToken);
            object.addProperty("xuid", xuid);
            object.addProperty("lastUsed", lastUsed);
            return object;
        }

        private boolean same(Account other) {
            return other != null && (uuid.equalsIgnoreCase(other.uuid) || name.equalsIgnoreCase(other.name));
        }

        private boolean valid() {
            try {
                UUID.fromString(uuid);
                return !name.isBlank() && ("offline".equals(type) || !accessToken.isBlank());
            } catch (Exception exception) {
                return false;
            }
        }

        boolean microsoft() {
            return "microsoft".equalsIgnoreCase(type);
        }

        private User user() {
            return new User(name, UUID.fromString(uuid), accessToken, Optional.ofNullable(xuid.isBlank() ? null : xuid), Optional.empty());
        }

        public String name() {
            return name;
        }

        public String typeName() {
            return microsoft() ? "Microsoft" : "Offline";
        }

        public UUID uuid() {
            return UUID.fromString(uuid);
        }

        public boolean isCurrent() {
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft != null && minecraft.getUser() != null
                    && uuid().equals(minecraft.getUser().getProfileId());
        }
    }
}
