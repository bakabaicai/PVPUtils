package com.pvp_utils.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.pvp_utils.client.security.ExploitGuard;
import net.minecraft.util.HttpUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.Map;

/**
 * 阻断服务器下发的资源包 URL 指向本机 / 内网地址的情况（含重定向绕过）。
 */
@Mixin(HttpUtil.class)
public class HttpUtilMixin {
    private static final int MAX_REDIRECTS = 5;

    @WrapOperation(method = "downloadFile",
            at = @At(value = "INVOKE", target = "Ljava/net/HttpURLConnection;getInputStream()Ljava/io/InputStream;"))
    private static InputStream pvp_utils$guardResourceDownload(HttpURLConnection connection, Operation<InputStream> original,
                                                               @Local(argsOnly = true) Map<String, String> requestProperties,
                                                               @Local(argsOnly = true) Proxy proxy) throws IOException {
        HttpURLConnection current = connection;
        current.setInstanceFollowRedirects(false);
        for (int hop = 0; ; hop++) {
            URL url = current.getURL();
            if (ExploitGuard.isBlockedTarget(url)) {
                throw new IOException("Blocked resource pack request to local address: " + url);
            }
            int status = current.getResponseCode();
            String location = current.getHeaderField("Location");
            boolean redirect = location != null
                    && (status == 305 || status == 307 || (status > 299 && status < 304));
            if (!redirect) {
                return original.call(current);
            }
            if (hop >= MAX_REDIRECTS) {
                throw new IOException("Server redirected too many times (" + MAX_REDIRECTS + ")");
            }
            URL next;
            try {
                next = new URL(location);
            } catch (MalformedURLException e) {
                next = new URL(url, location);
            }
            if (!url.getProtocol().equalsIgnoreCase(next.getProtocol())) {
                return original.call(current);
            }
            HttpURLConnection nextConnection = (HttpURLConnection) next.openConnection(proxy);
            nextConnection.setInstanceFollowRedirects(false);
            if (requestProperties != null) {
                requestProperties.forEach(nextConnection::setRequestProperty);
            }
            current = nextConnection;
        }
    }
}
