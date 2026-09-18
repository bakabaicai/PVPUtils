package com.pvp_utils.client.modules.impl.Render.BetterChat;

import com.mojang.authlib.GameProfile;

public interface BetterChatLineProfileAccessor {
    GameProfile pvp_utils$getOwnerProfile();
    boolean pvp_utils$shouldDrawAvatar();
}
