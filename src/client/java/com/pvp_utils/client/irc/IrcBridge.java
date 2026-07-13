package com.pvp_utils.client.irc;

import com.pvp_utils.client.util.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;
import java.util.UUID;

public final class IrcBridge {
    public static final String MISSING_CORE_MESSAGE = "The irc core file is missing.";

    private static final String CLIENT_CLASS = "com.pvp_utils.client.irc.network.PVPUtilsIrcClient";
    private static final String CHAT_SERVICE_CLASS = "com.pvp_utils.client.irc.chat.IrcChatService";
    private static final String TAB_LIST_SERVICE_CLASS = "com.pvp_utils.client.irc.tablist.IrcTabListService";

    private IrcBridge() {
    }

    public static boolean available() {
        return findClass(CLIENT_CLASS) != null;
    }

    public static void tick(Minecraft client) {
        Object instance = clientInstance();
        if (instance == null) {
            return;
        }
        invoke(instance, "tick", new Class<?>[]{Minecraft.class}, client);
    }

    public static String status() {
        Object instance = clientInstance();
        if (instance == null) {
            return "MISSING";
        }
        Object state = invoke(instance, "state", new Class<?>[0]);
        return state == null ? "UNKNOWN" : state.toString();
    }

    public static String serverAddress() {
        Object instance = clientInstance();
        if (instance == null) {
            return "";
        }
        Object address = invoke(instance, "serverAddress", new Class<?>[0]);
        return address == null ? "" : address.toString();
    }

    public static void connect() {
        Object instance = clientInstance();
        if (instance == null) {
            missingCore();
            return;
        }
        invoke(instance, "connect", new Class<?>[0]);
    }

    public static void reconnect() {
        Object instance = clientInstance();
        if (instance == null) {
            missingCore();
            return;
        }
        invoke(instance, "reconnect", new Class<?>[0]);
    }

    public static void disconnect() {
        Object instance = clientInstance();
        if (instance == null) {
            missingCore();
            return;
        }
        invoke(instance, "disconnect", new Class<?>[0]);
    }

    public static void login(String username, String password) {
        Object instance = clientInstance();
        if (instance == null) {
            missingCore();
            return;
        }
        invoke(instance, "login", new Class<?>[]{String.class, String.class}, username, password);
    }

    public static void sendChat(String message) {
        Class<?> chatService = findClass(CHAT_SERVICE_CLASS);
        if (chatService == null) {
            missingCore();
            return;
        }
        invokeStatic(chatService, "sendChat", new Class<?>[]{String.class}, message);
    }

    public static void sendPrivateMessage(String target, String message) {
        Class<?> chatService = findClass(CHAT_SERVICE_CLASS);
        if (chatService == null) {
            missingCore();
            return;
        }
        invokeStatic(chatService, "sendPrivateMessage", new Class<?>[]{String.class, String.class}, target, message);
    }

    public static Component decorateName(Component original, UUID minecraftUuid) {
        Class<?> tabListService = findClass(TAB_LIST_SERVICE_CLASS);
        if (tabListService == null || original == null || minecraftUuid == null) {
            return original;
        }
        Object decorated = invokeStatic(tabListService, "decorateName", new Class<?>[]{Component.class, UUID.class}, original, minecraftUuid);
        return decorated instanceof Component component ? component : original;
    }

    public static void missingCore() {
        ChatUtils.error(MISSING_CORE_MESSAGE);
    }

    private static Object clientInstance() {
        Class<?> clientClass = findClass(CLIENT_CLASS);
        if (clientClass == null) {
            return null;
        }
        return invokeStatic(clientClass, "getInstance", new Class<?>[0]);
    }

    private static Class<?> findClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private static Object invoke(Object target, String name, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = target.getClass().getMethod(name, parameterTypes);
            return method.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to call IRC core method: " + name, e);
        }
    }

    private static Object invokeStatic(Class<?> target, String name, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = target.getMethod(name, parameterTypes);
            return method.invoke(null, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to call IRC core method: " + name, e);
        }
    }
}
