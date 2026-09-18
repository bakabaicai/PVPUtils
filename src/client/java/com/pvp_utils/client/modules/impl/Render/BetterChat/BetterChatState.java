package com.pvp_utils.client.modules.impl.Render.BetterChat;

import com.pvp_utils.Config;
import com.pvp_utils.mixin.client.ChatHudAccessor;
import com.pvp_utils.mixin.client.ChatHudLineAccessor;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public final class BetterChatState {
    private static final BetterChatState INSTANCE = new BetterChatState();

    public static BetterChatState getInstance() {
        return INSTANCE;
    }

    private final List<Long> messageTimestamps = new ArrayList<>();
    private int chatDisplacementY;
    private boolean chatScreenOpenedLastFrame;
    private boolean chatScreenClosing;
    private long chatScreenAnimationStart;
    private float chatScreenOffsetY;

    private BetterChatState() {}

    public void recordMessage() {
        messageTimestamps.addFirst(System.currentTimeMillis());
    }

    public void trimMessageCount(int maxCount) {
        while (messageTimestamps.size() > maxCount) {
            messageTimestamps.removeLast();
        }
    }

    public int calculateChatDisplacementY(int lineHeight, int chatScrollbarPos) {
        if (!Config.betterChat || !Config.betterChatMessageAnimation || messageTimestamps.isEmpty()) {
            chatDisplacementY = 0;
            return 0;
        }
        if (chatScrollbarPos != 0) {
            chatDisplacementY = 0;
            return 0;
        }
        long timeAlive = System.currentTimeMillis() - messageTimestamps.getFirst();
        int fadeTime = Math.max(1, Config.betterChatMessageFadeTime);
        if (timeAlive >= fadeTime) {
            chatDisplacementY = 0;
            return 0;
        }
        float maxDisplacement = lineHeight * 0.8f;
        chatDisplacementY = (int) (maxDisplacement - (((float) timeAlive / fadeTime) * maxDisplacement));
        return chatDisplacementY;
    }

    public void beginChatScreenIfNeeded(Minecraft client) {
        if (client == null || client.player == null || client.player.isSleeping()) return;
        if (!chatScreenOpenedLastFrame) {
            chatScreenOpenedLastFrame = true;
            chatScreenAnimationStart = System.currentTimeMillis();
            chatScreenClosing = false;
        }
    }

    public float calculateChatScreenOffsetY(Minecraft client) {
        if (!Config.betterChat || !Config.betterChatInputAnimation) {
            chatScreenOffsetY = 0;
            return 0;
        }
        if (client == null) {
            chatScreenOffsetY = 0;
            return 0;
        }
        float elapsed = System.currentTimeMillis() - chatScreenAnimationStart;
        int fadeTime = Math.max(1, Config.betterChatInputFadeTime);
        float alpha = chatScreenClosing ? elapsed / fadeTime : 1 - (elapsed / fadeTime);
        alpha = Math.max(0, Math.min(1, alpha));
        float eased = (float) (1.70158f + 1f) * alpha * alpha * alpha - 1.70158f * alpha * alpha;
        float screenFactor = (float) client.getWindow().getScreenHeight() / 1080f;
        chatScreenOffsetY = eased * 10f * screenFactor;
        return chatScreenOffsetY;
    }

    public void startClosing() {
        if (!Config.betterChatInputAnimation) return;
        chatScreenClosing = true;
        chatScreenAnimationStart = System.currentTimeMillis();
    }

    public boolean isChatScreenClosing() {
        return chatScreenClosing;
    }

    public boolean shouldCloseChatScreen() {
        if (!chatScreenClosing) return false;
        int fadeTime = Math.max(1, Config.betterChatInputFadeTime);
        return (System.currentTimeMillis() - chatScreenAnimationStart) >= fadeTime;
    }

    public void resetChatScreenState() {
        chatScreenOpenedLastFrame = false;
        chatScreenClosing = false;
        chatScreenAnimationStart = 0L;
        chatScreenOffsetY = 0f;
    }

    public boolean hasActiveChatMessages(Minecraft client) {
        if (client == null || client.gui == null || client.gui.getChat() == null) return false;
        try {
            List<?> messages = ((ChatHudAccessor) client.gui.getChat()).getVisibleMessages();
            int ticks = client.gui.getGuiTicks();
            final int fadeTicks = 200;
            for (Object msg : messages) {
                if (msg instanceof GuiMessage line) {
                    int creationTick = ((ChatHudLineAccessor) (Object) line).getCreationTick();
                    if (ticks - creationTick < fadeTicks) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }
}
