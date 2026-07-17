package com.pvp_utils.client.NeteaseMusic;

import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class NeteaseMusicLocalService {
    private static final Object LOCK = new Object();
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 3000;
    private static final String RESOURCE_ROOT = "/assets/pvp_utils/music-service/";
    private static final String API_ARCHIVE = RESOURCE_ROOT + "netease-cloud-music-api.zip";
    private static final String SERVICE_VERSION = "netease-4.32.0-node-22.13.1-v1";
    private static final Duration HEALTH_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READY_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration READY_INTERVAL = Duration.ofMillis(500);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(HEALTH_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static Process process;
    private static volatile boolean lastAvailable;
    private static volatile long lastHealthCheckNanos;

    private NeteaseMusicLocalService() {
    }

    public static String baseUrl() {
        return "http://" + HOST + ":" + PORT;
    }

    public static void start() {
        synchronized (LOCK) {
            if (process != null && process.isAlive()) {
                return;
            }
            if (checkServiceAvailable(true)) {
                return;
            }

            try {
                ProcessBuilder builder = processBuilder();
                process = builder.start();
                drain(process.getInputStream(), "stdout");
                drain(process.getErrorStream(), "stderr");
                watch(process);
                waitUntilReady(process);
            } catch (IOException exception) {
                process = null;
                lastAvailable = false;
                System.err.println("PVPUtils Netease Music service failed to start: " + exception.getMessage());
            }
        }
    }

    public static void stop() {
        Process current;
        synchronized (LOCK) {
            current = process;
            process = null;
        }

        if (current == null || !current.isAlive()) {
            return;
        }

        current.destroy();
        try {
            if (!current.waitFor(5L, TimeUnit.SECONDS)) {
                current.destroyForcibly();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            current.destroyForcibly();
        }
    }

    public static boolean isServiceAvailable() {
        long now = System.nanoTime();
        if (now - lastHealthCheckNanos < Duration.ofSeconds(2).toNanos()) {
            return lastAvailable;
        }
        return checkServiceAvailable(false);
    }

    private static boolean checkServiceAvailable(boolean force) {
        long now = System.nanoTime();
        if (!force && now - lastHealthCheckNanos < Duration.ofSeconds(2).toNanos()) {
            return lastAvailable;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl() + "/"))
                    .timeout(HEALTH_TIMEOUT)
                    .GET()
                    .header("User-Agent", "PVPUtils/1.0")
                    .build();
            int status = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            lastAvailable = status >= 200 && status < 500;
        } catch (Exception ignored) {
            lastAvailable = false;
        }
        lastHealthCheckNanos = now;
        return lastAvailable;
    }

    private static ProcessBuilder processBuilder() throws IOException {
        Installation installation = prepareBundledInstallation();
        ProcessBuilder builder = new ProcessBuilder(installation.nodeExecutable().toString(), installation.appScript().toString());
        builder.directory(installation.workingDirectory().toFile());
        builder.environment().put("HOST", HOST);
        builder.environment().put("PORT", Integer.toString(PORT));
        return builder;
    }

    private static Installation prepareBundledInstallation() throws IOException {
        Path installDirectory = installDirectory();
        Path marker = installDirectory.resolve(".version");
        Path apiDirectory = installDirectory.resolve("api/node_modules/NeteaseCloudMusicApi");
        Path appScript = apiDirectory.resolve("app.js");
        Path nodeDirectory = installDirectory.resolve("node");
        Path nodeExecutable = nodeExecutable(nodeDirectory);

        if (Files.isRegularFile(marker)
                && SERVICE_VERSION.equals(Files.readString(marker, StandardCharsets.UTF_8))
                && Files.isRegularFile(appScript)
                && Files.isRegularFile(nodeExecutable)) {
            return new Installation(nodeExecutable, appScript, apiDirectory);
        }

        deleteRecursively(installDirectory);
        Files.createDirectories(installDirectory);
        extractZipResource(API_ARCHIVE, installDirectory, false);
        extractZipResource(RESOURCE_ROOT + nodeArchiveName(), nodeDirectory, true);
        Files.writeString(marker, SERVICE_VERSION, StandardCharsets.UTF_8);

        nodeExecutable = nodeExecutable(nodeDirectory);
        if (!Files.isRegularFile(appScript)) {
            throw new IOException("Bundled NeteaseCloudMusicApi app.js was not found");
        }
        if (!Files.isRegularFile(nodeExecutable)) {
            throw new IOException("Bundled Node.js executable was not found");
        }
        nodeExecutable.toFile().setExecutable(true, false);
        return new Installation(nodeExecutable, appScript, apiDirectory);
    }

    private static void extractZipResource(String resource, Path targetDirectory, boolean stripFirstDirectory) throws IOException {
        try (InputStream raw = resourceStream(resource);
             ZipInputStream input = new ZipInputStream(raw)) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                Path relative = safeRelativePath(entry.getName(), stripFirstDirectory);
                if (relative == null) {
                    continue;
                }

                Path output = safeOutputPath(targetDirectory, relative);
                if (entry.isDirectory()) {
                    Files.createDirectories(output);
                } else {
                    Files.createDirectories(output.getParent());
                    Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING);
                }
                input.closeEntry();
            }
        }
    }

    private static InputStream resourceStream(String resource) throws IOException {
        InputStream stream = NeteaseMusicLocalService.class.getResourceAsStream(resource);
        if (stream == null) {
            throw new IOException("Missing bundled resource: " + resource);
        }
        return stream;
    }

    private static Path safeRelativePath(String name, boolean stripFirstDirectory) {
        Path path = Path.of(name.replace('\\', '/')).normalize();
        if (path.isAbsolute() || path.startsWith("..")) {
            return null;
        }
        if (!stripFirstDirectory) {
            return path;
        }
        return path.getNameCount() > 1 ? path.subpath(1, path.getNameCount()) : null;
    }

    private static Path safeOutputPath(Path targetDirectory, Path relativePath) throws IOException {
        Path output = targetDirectory.resolve(relativePath).normalize();
        Path normalizedTarget = targetDirectory.toAbsolutePath().normalize();
        Path normalizedOutput = output.toAbsolutePath().normalize();
        if (!normalizedOutput.startsWith(normalizedTarget)) {
            throw new IOException("Archive entry escapes target directory: " + relativePath);
        }
        return output;
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            for (Path entry : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(entry);
            }
        }
    }

    private static Path installDirectory() {
        return FabricLoader.getInstance().getGameDir().resolve("PVPUtils").resolve("music-service").resolve(SERVICE_VERSION);
    }

    private static Path nodeExecutable(Path nodeDirectory) {
        return nodeDirectory.resolve("node.exe");
    }

    private static String nodeArchiveName() throws IOException {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            throw new IOException("Bundled Netease music service currently supports Windows only");
        }
        return "node-windows-x64.zip";
    }

    private static void waitUntilReady(Process startedProcess) {
        Thread thread = new Thread(() -> {
            long deadline = System.nanoTime() + READY_TIMEOUT.toNanos();
            while (startedProcess.isAlive() && System.nanoTime() < deadline) {
                if (checkServiceAvailable(true)) {
                    return;
                }
                try {
                    Thread.sleep(READY_INTERVAL.toMillis());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "PVPUtils-NeteaseMusic-Ready");
        thread.setDaemon(true);
        thread.start();
    }

    private static void watch(Process startedProcess) {
        Thread thread = new Thread(() -> {
            try {
                int exitCode = startedProcess.waitFor();
                synchronized (LOCK) {
                    if (process == startedProcess) {
                        process = null;
                    }
                }
                if (exitCode != 0) {
                    System.err.println("PVPUtils Netease Music service exited with code " + exitCode);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, "PVPUtils-NeteaseMusic-Watch");
        thread.setDaemon(true);
        thread.start();
    }

    private static void drain(InputStream stream, String name) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                while (reader.readLine() != null) {
                    // Drain only. The API is too noisy for normal client logs.
                }
            } catch (IOException ignored) {
            }
        }, "PVPUtils-NeteaseMusic-" + name);
        thread.setDaemon(true);
        thread.start();
    }

    private record Installation(Path nodeExecutable, Path appScript, Path workingDirectory) {
    }
}
