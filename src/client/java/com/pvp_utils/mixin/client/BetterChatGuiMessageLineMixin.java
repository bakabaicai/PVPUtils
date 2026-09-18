package com.pvp_utils.mixin.client;

import com.mojang.authlib.GameProfile;
import com.pvp_utils.client.modules.impl.Render.BetterChat.BetterChatHeadsState;
import com.pvp_utils.client.modules.impl.Render.BetterChat.BetterChatLineProfileAccessor;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMessage.Line.class)
public abstract class BetterChatGuiMessageLineMixin implements BetterChatLineProfileAccessor {
    @Unique private GameProfile pvp_utils$ownerProfile;
    @Unique private boolean pvp_utils$drawAvatar;

    @Inject(method = "<init>(ILnet/minecraft/util/FormattedCharSequence;Lnet/minecraft/client/GuiMessageTag;Z)V", at = @At("RETURN"))
    private void pvp_utils$onInit(int addedTime, FormattedCharSequence content, GuiMessageTag tag, boolean endOfEntry, CallbackInfo ci) {
        BetterChatHeadsState state = BetterChatHeadsState.getInstance();
        this.pvp_utils$ownerProfile = state.prepareLineProfile(endOfEntry);
        this.pvp_utils$drawAvatar = state.shouldDrawAvatarForCurrentLine();
    }

    @Override
    public GameProfile pvp_utils$getOwnerProfile() {
        return pvp_utils$ownerProfile;
    }

    @Override
    public boolean pvp_utils$shouldDrawAvatar() {
        return pvp_utils$drawAvatar;
    }
}
