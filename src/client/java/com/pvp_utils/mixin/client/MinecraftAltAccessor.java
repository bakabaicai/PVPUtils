package com.pvp_utils.mixin.client;

import com.mojang.authlib.minecraft.UserApiService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.CompletableFuture;

@Mixin(Minecraft.class)
public interface MinecraftAltAccessor {
    @Accessor("user")
    void pvp_utils$setUser(User user);

    @Accessor("userApiService")
    void pvp_utils$setUserApiService(UserApiService service);

    @Accessor("profileFuture")
    void pvp_utils$setProfileFuture(CompletableFuture<?> future);

    @Accessor("userPropertiesFuture")
    void pvp_utils$setUserPropertiesFuture(CompletableFuture<?> future);

    @Accessor("profileKeyPairManager")
    void pvp_utils$setProfileKeyPairManager(ProfileKeyPairManager manager);
}
