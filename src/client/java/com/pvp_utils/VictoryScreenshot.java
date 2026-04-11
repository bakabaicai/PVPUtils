package com.pvp_utils;

import com.pvp_utils.client.gui.NotificationOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.multiplayer.ServerData;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VictoryScreenshot {
    private static long lastShotTime = 0;
    private static final long COOLDOWN_MS = 5000;

    public static void tryCapture() {
        if (!Config.autoScreenshot) return;

        long now = System.currentTimeMillis();
        if (now - lastShotTime < COOLDOWN_MS) return;
        lastShotTime = now;

        Minecraft client = Minecraft.getInstance();
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd"));
        String serverPart = getServerPart(client);
        String customFileName = "Victory_" + date + serverPart + "_" + (now % 10000) + ".png";

        CompletableFuture.delayedExecutor(600, TimeUnit.MILLISECONDS).execute(() -> {
            client.execute(() -> {
                Screenshot.grab(client.gameDirectory, client.getMainRenderTarget(), (component) -> {
                    String fileName = extractFileName(component.getString());
                    if (fileName != null) {
                        transferFile(client, fileName, customFileName);
                    }
                });
            });
        });
    }

    private static String extractFileName(String message) {
        if (message == null) return null;
        Pattern pattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}_\\d{2}\\.\\d{2}\\.\\d{2}\\.png)");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) return matcher.group(1);
        if (message.contains("[") && message.contains("]")) {
            return message.substring(message.lastIndexOf("[") + 1, message.lastIndexOf("]"));
        }
        return null;
    }

    private static void transferFile(Minecraft client, String originalName, String newName) {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                Path source = client.gameDirectory.toPath().resolve("screenshots").resolve(originalName);
                Path desktop = getDesktopPath();
                if (desktop != null && Files.exists(source)) {
                    Path target = desktop.resolve(newName);
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    if (Files.exists(target)) {
                        Files.delete(source);
                        client.execute(() -> {
                            String msg = Config.isChinese ? "截图已保存至桌面" : "Screenshot saved to desktop";
                            NotificationOverlay.getInstance().show(msg, 0xFFFFFF);
                        });
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private static String getServerPart(Minecraft client) {
        ServerData server = client.getCurrentServer();
        if (server != null && server.ip != null) {
            return "_" + server.ip.split(":")[0].toLowerCase().replaceAll("[^a-z0-9]", "_");
        }
        return client.level != null ? "_singleplayer" : "";
    }

    private static Path getDesktopPath() {
        String home = System.getProperty("user.home");
        if (home == null) return null;
        Path[] paths = {Path.of(home, "Desktop"), Path.of(home, "桌面"), Path.of(home, "OneDrive", "Desktop"), Path.of(home, "OneDrive", "桌面")};
        for (Path p : paths) { if (Files.exists(p)) return p; }
        return null;
    }
}