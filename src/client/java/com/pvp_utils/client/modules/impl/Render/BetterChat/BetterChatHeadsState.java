package com.pvp_utils.client.modules.impl.Render.BetterChat;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BetterChatHeadsState {
    private static final BetterChatHeadsState INSTANCE = new BetterChatHeadsState();

    public static BetterChatHeadsState getInstance() {
        return INSTANCE;
    }

    private GameProfile pendingSender;
    private GameProfile currentSender;
    private boolean currentLineShouldDrawAvatar;
    private PlayerSkin cachedSkin;
    private GameProfile cachedSkinProfile;
    private final Map<UUID, GameProfile> profileCache = new HashMap<>();

    private BetterChatHeadsState() {}

    public void setPendingSender(GameProfile profile) {
        this.pendingSender = profile;
        if (profile != null && profile.id() != null) {
            profileCache.put(profile.id(), profile);
        }
    }

    public GameProfile consumePendingSender() {
        GameProfile profile = pendingSender;
        pendingSender = null;
        currentSender = profile;
        return profile;
    }

    public GameProfile prepareLineProfile(boolean endOfEntry) {
        if (pendingSender != null) {
            currentSender = pendingSender;
            pendingSender = null;
            currentLineShouldDrawAvatar = true;
        } else if (currentSender != null) {
            currentLineShouldDrawAvatar = false;
        } else {
            currentLineShouldDrawAvatar = false;
        }
        GameProfile profile = currentSender;
        if (endOfEntry) {
            currentSender = null;
        }
        return profile;
    }

    public boolean shouldDrawAvatarForCurrentLine() {
        return currentLineShouldDrawAvatar;
    }

    public GameProfile getCurrentSender() {
        return currentSender;
    }

    public GameProfile findProfile(UUID uuid) {
        return uuid == null ? null : profileCache.get(uuid);
    }

    public PlayerSkin getSkin(Minecraft client, GameProfile profile) {
        if (client == null || profile == null || client.getConnection() == null) return null;
        if (cachedSkin != null && profile.equals(cachedSkinProfile)) return cachedSkin;
        try {
            cachedSkin = client.getSkinManager().createLookup(profile, false).get();
            cachedSkinProfile = profile;
            return cachedSkin;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
