package com.pvp_utils.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.resources.server.DownloadedPackSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.nio.file.Path;
import java.util.UUID;

/**
 * 服务器资源包下载缓存按账号隔离，避免跨账号共享缓存被用作设备指纹。
 */
@Mixin(DownloadedPackSource.class)
public class DownloadedPackSourceMixin {

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private Path pvp_utils$isolatePackCache(Path cacheDir, Minecraft minecraft, Path originalPath, GameConfig.UserData userData) {
        if (cacheDir == null || userData == null || userData.user == null) {
            return cacheDir;
        }
        UUID profileId = userData.user.getProfileId();
        if (profileId == null) {
            return cacheDir;
        }
        return cacheDir.resolve(profileId.toString());
    }
}
