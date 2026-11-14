package com.pvp_utils.client.communication;

import com.mojang.authlib.GameProfile;
import com.pvp_utils.Config;
import com.pvp_utils.client.TranslationKeyGuard;
import com.pvp_utils.client.util.ChatUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.phys.Vec3;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EncryptedChatManager {
    private static final char[] KEY_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern ENCRYPTED_TOKEN = Pattern.compile("~[CP][\\u3400-\\u9FFF\\uAC00-\\uBFFF]{16,253}~|[\\u3400-\\u9FFF\\uAC00-\\uBFFF]{16,256}");
    private static final ThreadLocal<Boolean> OUTGOING_BYPASS = ThreadLocal.withInitial(() -> false);
    private static final ThreadPoolExecutor KEY_WORKER = new ThreadPoolExecutor(1, 1, 30L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1), task -> {
                Thread thread = new Thread(task, "pvp-utils-chat-key");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.DiscardOldestPolicy());
    private static final LinkedHashMap<String, EncryptedChatCodec.Decoded> DECODE_CACHE = new LinkedHashMap<>();
    private static final LinkedHashSet<String> SEEN_MARKERS = new LinkedHashSet<>();
    private static final LinkedHashSet<String> SENT_PRIVATE_MESSAGES = new LinkedHashSet<>();
    private static volatile String derivingPassword;
    private static volatile SecretKeySpec currentKey;
    private static volatile boolean derivationFailed;
    private static volatile boolean closed;
    private static Consumer<ReceivedMarker> markerListener = marker -> { };
    private static ClientPacketListener session;
    private static long lastFeedback;
    private static long lastChatSent;

    private EncryptedChatManager() {
    }

    public static boolean hasValidKey() {
        if (closed) return false;
        String password = Config.encryptedChatKey;
        boolean valid = password != null && password.codePointCount(0, password.length()) >= 5;
        requestKey(valid ? password : null);
        return valid;
    }

    public static String generateKey() {
        StringBuilder key = new StringBuilder(10);
        for (int i = 0; i < 10; i++) key.append(KEY_ALPHABET[RANDOM.nextInt(KEY_ALPHABET.length)]);
        return key.toString();
    }

    public static void tick() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != session) {
            resetSession();
            session = connection;
        }
        hasValidKey();
    }

    public static void resetSession() {
        DECODE_CACHE.clear();
        SEEN_MARKERS.clear();
        SENT_PRIVATE_MESSAGES.clear();
        lastFeedback = 0L;
        lastChatSent = 0L;
    }

    public static void setMarkerListener(Consumer<ReceivedMarker> listener) {
        markerListener = listener == null ? marker -> { } : listener;
    }

    public static boolean sendMarker(BlockPos position, Vec3 location, Direction face, String dimension, boolean danger, Vec3 senderPosition, int colorCode) {
        if (!Config.tacticalPing || !hasValidKey()) return false;
        if (lastChatSent != 0L && System.nanoTime() - lastChatSent < 1_200_000_000L) return false;
        SecretKeySpec key = currentKey;
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (key == null || connection == null) return false;
        try {
            String message = EncryptedChatCodec.encryptMarker(key, position, location, face, dimension, danger, senderPosition, colorCode);
            if (message.length() > 256) return false;
            rememberMarker(message);
            sendBypass(() -> connection.sendChat(message));
            return true;
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            return false;
        }
    }

    public static boolean interceptChat(ClientPacketListener connection, String message) {
        if (OUTGOING_BYPASS.get() || !Config.encryptPublicChat) return false;
        String encrypted = encryptOutgoing(message, 0);
        if (encrypted != null) sendBypass(() -> connection.sendChat(encrypted));
        return true;
    }

    public static boolean interceptCommand(ClientPacketListener connection, String command) {
        if (OUTGOING_BYPASS.get() || !Config.encryptPrivateChat) return false;
        int commandEnd = nextWhitespace(command, 0);
        if (!privateChatCommand(command)) return false;
        int targetStart = skipWhitespace(command, commandEnd);
        int targetEnd = nextWhitespace(command, targetStart);
        int bodyStart = skipWhitespace(command, targetEnd);
        if (targetStart >= targetEnd || bodyStart >= command.length()) return false;
        String prefix = command.substring(0, bodyStart);
        String encrypted = encryptOutgoing(command.substring(bodyStart), prefix.length());
        if (encrypted != null) {
            SENT_PRIVATE_MESSAGES.add(encrypted);
            if (SENT_PRIVATE_MESSAGES.size() > 128) {
                SENT_PRIVATE_MESSAGES.remove(SENT_PRIVATE_MESSAGES.iterator().next());
            }
            sendBypass(() -> connection.sendCommand(prefix + encrypted));
        }
        return true;
    }

    public static void noteChatSent() {
        lastChatSent = System.nanoTime();
    }

    public static void noteCommandSent(String command) {
        if (privateChatCommand(command)) noteChatSent();
    }

    private static boolean privateChatCommand(String command) {
        String name = command.substring(0, nextWhitespace(command, 0)).toLowerCase(java.util.Locale.ROOT);
        if (name.startsWith("minecraft:")) name = name.substring("minecraft:".length());
        return name.equals("msg") || name.equals("w") || name.equals("tell") || name.equals("whisper");
    }

    public static DisplayResult receive(Component component, GameProfile sender, String signedContent) {
        return receive(component, sender, signedContent, null);
    }

    public static DisplayResult receive(Component component, GameProfile sender, String signedContent, ChatType.Bound chatType) {
        TranslationKeyGuard.unmarkFromNetwork(component);
        if (!hasValidKey() || currentKey == null) {
            return new DisplayResult(component, false);
        }
        String visible = component.getString();
        String source = signedContent == null ? visible : signedContent;
        if (source.length() > 4096) return new DisplayResult(component, false);
        Matcher tokens = ENCRYPTED_TOKEN.matcher(source);
        for (int attempts = 0; attempts < 4 && tokens.find(); attempts++) {
            String token = tokens.group();
            EncryptedChatCodec.Decoded decoded = decode(token);
            if (decoded != null) {
                if (decoded.marker()) {
                    if (!Config.tacticalPing) continue;
                    boolean standalone = signedContent == null ? source.substring(tokens.end()).isBlank() : source.strip().equals(token);
                    if (!standalone) continue;
                    if (rememberMarker(token)) {
                        Sender identity = sender == null ? inferSender(visible, token) : new Sender(sender.id(), sender.name());
                        markerListener.accept(new ReceivedMarker(identity.id(), identity.name(), decoded.position(), decoded.dimension(), decoded.danger(), decoded.location(), decoded.face(), decoded.senderPosition(), decoded.colorCode()));
                    }
                    return new DisplayResult(component, true);
                }
                if (!visible.contains(token)) return new DisplayResult(component, false);
                boolean privateChat = SENT_PRIVATE_MESSAGES.contains(token) || privateChatType(chatType) || privateChatComponent(component);
                Component displayed = Component.empty()
                        .append(ChatUtils.prefix(privateChat ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GOLD))
                        .append(replace(component, token, decoded.text()));
                return new DisplayResult(displayed, false);
            }
        }
        return new DisplayResult(component, false);
    }

    public static Component narration(Component component) {
        TranslationKeyGuard.unmarkFromNetwork(component);
        if (!hasValidKey() || currentKey == null) return component;
        String source = component.getString();
        if (source.length() > 4096) return component;
        Matcher tokens = ENCRYPTED_TOKEN.matcher(source);
        for (int attempts = 0; attempts < 4 && tokens.find(); attempts++) {
            String token = tokens.group();
            EncryptedChatCodec.Decoded decoded = decode(token);
            if (decoded == null) continue;
            if (decoded.marker()) {
                if (Config.tacticalPing && source.substring(tokens.end()).isBlank()) return null;
            } else return replace(component, token, decoded.text());
        }
        return component;
    }

    private static synchronized void requestKey(String password) {
        if (closed) return;
        if (java.util.Objects.equals(password, derivingPassword)) return;
        derivingPassword = password;
        currentKey = null;
        derivationFailed = false;
        DECODE_CACHE.clear();
        SEEN_MARKERS.clear();
        SENT_PRIVATE_MESSAGES.clear();
        KEY_WORKER.getQueue().clear();
        if (password == null) return;
        KEY_WORKER.execute(() -> {
            if (!password.equals(derivingPassword)) return;
            try {
                SecretKeySpec key = EncryptedChatCodec.deriveKey(password);
                synchronized (EncryptedChatManager.class) {
                    if (!closed && password.equals(derivingPassword)) currentKey = key;
                }
            } catch (GeneralSecurityException exception) {
                synchronized (EncryptedChatManager.class) {
                    if (!closed && password.equals(derivingPassword)) derivationFailed = true;
                }
            }
        });
    }

    public static synchronized void close() {
        closed = true;
        derivingPassword = null;
        currentKey = null;
        KEY_WORKER.shutdownNow();
        markerListener = marker -> { };
        session = null;
        resetSession();
    }

    private static String encryptOutgoing(String message, int prefixLength) {
        if (!hasValidKey()) {
            feedback(Config.isChinese ? "请先配置至少五位的加密聊天密钥。" : "Configure an encrypted chat key of at least five characters first.");
            return null;
        }
        SecretKeySpec key = currentKey;
        if (key == null) {
            feedback(derivationFailed
                    ? (Config.isChinese ? "加密密钥初始化失败。" : "The encryption key could not be initialized.")
                    : (Config.isChinese ? "正在准备加密密钥，请稍后发送。" : "Preparing the encryption key. Please try again shortly."));
            return null;
        }
        try {
            String encrypted = EncryptedChatCodec.encryptText(key, message);
            if (encrypted.length() + prefixLength > 256) {
                feedback(Config.isChinese ? "加密后超过聊天长度限制，请缩短文本。" : "The encrypted message exceeds the chat length limit. Shorten the text.");
                return null;
            }
            return encrypted;
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            feedback(Config.isChinese ? "聊天加密失败，消息未发送。" : "Chat encryption failed. The message was not sent.");
            return null;
        }
    }

    private static void feedback(String message) {
        long now = System.nanoTime();
        if (lastFeedback == 0L || now - lastFeedback >= 1_000_000_000L) {
            lastFeedback = now;
            ChatUtils.warning(message);
        }
    }

    private static void sendBypass(Runnable sender) {
        boolean previous = OUTGOING_BYPASS.get();
        OUTGOING_BYPASS.set(true);
        try {
            sender.run();
        } finally {
            OUTGOING_BYPASS.set(previous);
        }
    }

    private static EncryptedChatCodec.Decoded decode(String token) {
        if (DECODE_CACHE.containsKey(token)) return DECODE_CACHE.get(token);
        EncryptedChatCodec.Decoded decoded = EncryptedChatCodec.decrypt(currentKey, token);
        DECODE_CACHE.put(token, decoded);
        if (DECODE_CACHE.size() > 128) DECODE_CACHE.remove(DECODE_CACHE.keySet().iterator().next());
        return decoded;
    }

    private static boolean rememberMarker(String token) {
        if (!SEEN_MARKERS.add(token)) return false;
        if (SEEN_MARKERS.size() > 256) {
            Iterator<String> iterator = SEEN_MARKERS.iterator();
            iterator.next();
            iterator.remove();
        }
        return true;
    }

    private static Sender inferSender(String visible, String token) {
        int markerIndex = visible.indexOf(token);
        String prefix = markerIndex < 0 ? "" : visible.substring(0, markerIndex);
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        PlayerInfo best = null;
        int bestIndex = -1;
        if (connection != null) {
            for (PlayerInfo info : connection.getOnlinePlayers()) {
                String name = info.getProfile().name();
                int index = prefix.lastIndexOf(name);
                if (index >= 0 && index > bestIndex
                        && (index == 0 || !nameCharacter(prefix.charAt(index - 1)))
                        && (index + name.length() == prefix.length() || !nameCharacter(prefix.charAt(index + name.length())))) {
                    best = info;
                    bestIndex = index;
                }
            }
        }
        if (best != null) return new Sender(best.getProfile().id(), best.getProfile().name());
        int open = prefix.lastIndexOf('<');
        int close = prefix.lastIndexOf('>');
        String name = open >= 0 && close > open ? prefix.substring(open + 1, close).strip() : "";
        String identity = name.isEmpty() ? prefix : name;
        return new Sender(UUID.nameUUIDFromBytes(("PVPUtils/system/" + identity).getBytes(StandardCharsets.UTF_8)), name);
    }

    private static boolean nameCharacter(char character) {
        return character >= 'a' && character <= 'z' || character >= 'A' && character <= 'Z'
                || character >= '0' && character <= '9' || character == '_';
    }

    private static boolean privateChatType(ChatType.Bound chatType) {
        return chatType != null && (chatType.chatType().is(ChatType.MSG_COMMAND_INCOMING)
                || chatType.chatType().is(ChatType.MSG_COMMAND_OUTGOING));
    }

    private static boolean privateChatComponent(Component component) {
        if (component.getContents() instanceof TranslatableContents contents) {
            String key = contents.getKey();
            if (key.equals("commands.message.display.incoming") || key.equals("commands.message.display.outgoing")) return true;
        }
        for (Component sibling : component.getSiblings()) {
            if (privateChatComponent(sibling)) return true;
        }
        return false;
    }

    private static Component replace(Component source, String token, String text) {
        int start = source.getString().indexOf(token);
        if (start < 0) return source;
        int end = start + token.length();
        List<Span> spans = new ArrayList<>();
        source.visit((style, content) -> {
            spans.add(new Span(style, content));
            return Optional.empty();
        }, Style.EMPTY);
        MutableComponent result = Component.empty();
        int offset = 0;
        boolean inserted = false;
        for (Span span : spans) {
            int spanEnd = offset + span.text().length();
            if (offset < start) {
                int kept = Math.min(span.text().length(), start - offset);
                if (kept > 0) result.append(Component.literal(span.text().substring(0, kept)).setStyle(span.style()));
            }
            if (!inserted && spanEnd > start) {
                result.append(Component.literal(text).setStyle(span.style()));
                inserted = true;
            }
            if (spanEnd > end) {
                int suffix = Math.max(0, end - offset);
                result.append(Component.literal(span.text().substring(suffix)).setStyle(span.style()));
            }
            offset = spanEnd;
        }
        return result;
    }

    private static int nextWhitespace(String text, int start) {
        int index = start;
        while (index < text.length() && !Character.isWhitespace(text.charAt(index))) index++;
        return index;
    }

    private static int skipWhitespace(String text, int start) {
        int index = start;
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) index++;
        return index;
    }

    public record ReceivedMarker(UUID sender, String senderName, BlockPos position, String dimension, boolean danger, Vec3 location, Direction face, Vec3 senderPosition, int colorCode) {
    }

    public record DisplayResult(Component component, boolean hidden) {
    }

    private record Sender(UUID id, String name) {
    }

    private record Span(Style style, String text) {
    }
}
