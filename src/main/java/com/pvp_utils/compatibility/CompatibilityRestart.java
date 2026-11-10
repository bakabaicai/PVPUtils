package com.pvp_utils.compatibility;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class CompatibilityRestart {
    private static final String HELPER_CLASS = "com.pvp_utils.compatibility.CompatibilityRestartHelper";
    private static final String HELPER_FILE = "com/pvp_utils/compatibility/CompatibilityRestartHelper.class";

    private CompatibilityRestart() {
    }

    public static Plan prepare(Path configDirectory) {
        Path config = configDirectory.toAbsolutePath().normalize();
        Path root = config.resolve("pvp-utils-compatibility-restart").resolve(UUID.randomUUID().toString());
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
        Path java = Path.of(System.getProperty("java.home"), "bin", windows ? "javaw.exe" : "java");
        return new Plan(config, Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize(), root,
                JavaLaunchCommand.queryCommand(), List.of(java.toString(), "-cp", root.toString(), HELPER_CLASS));
    }

    public static String displayCommand(List<String> command) {
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
        return (windows ? "& " : "") + String.join(" ", command.stream()
                .map(argument -> "'" + argument.replace("'", windows ? "''" : "'\"'\"'") + "'").toList());
    }

    public static void request(Plan plan) throws IOException {
        List<String> command = JavaLaunchCommand.current(plan.queryCommand(), plan.workingDirectory());
        Path configDirectory = plan.configDirectory();
        Path root = plan.root();
        Path helperFile = root.resolve(HELPER_FILE);
        Files.createDirectories(helperFile.getParent());
        try (InputStream resource = CompatibilityRestart.class.getResourceAsStream("/" + HELPER_FILE)) {
            if (resource == null) throw new IOException("Compatibility restart helper is missing");
            Files.copy(resource, helperFile);
        }
        Process helper = new ProcessBuilder(plan.helperCommand())
                .directory(plan.workingDirectory().toFile())
                .redirectError(ProcessBuilder.Redirect.DISCARD).start();
        boolean ready = false;
        try {
            try (DataOutputStream output = new DataOutputStream(helper.getOutputStream())) {
                output.writeInt(0x50565052);
                output.writeLong(ProcessHandle.current().pid());
                write(output, plan.workingDirectory().toString());
                write(output, root.toString());
                write(output, configDirectory.toAbsolutePath().normalize().toString());
                output.writeInt(command.size());
                for (String argument : command) write(output, argument);
            }
            byte[] handshake = CompletableFuture.supplyAsync(() -> {
                try {
                    return helper.getInputStream().readNBytes(6);
                } catch (IOException exception) {
                    return new byte[0];
                }
            }).get(4, TimeUnit.SECONDS);
            if (!"READY\n".equals(new String(handshake, StandardCharsets.US_ASCII))) {
                throw new IOException("Compatibility restart helper did not become ready");
            }
            ready = true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Compatibility restart interrupted", exception);
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException exception) {
            throw new IOException("Compatibility restart helper timed out", exception);
        } finally {
            if (!ready) {
                helper.destroy();
                cleanup(root, configDirectory);
            }
            helper.getInputStream().close();
        }
    }

    private static void write(DataOutputStream output, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    public static void cleanupAfterBoot(Path configDirectory) {
        String value = System.getProperty("pvp-utils.compatibility.restart.cleanup");
        if (value == null) return;
        try {
            cleanup(Path.of(value), configDirectory);
        } catch (IOException exception) {
            System.getLogger("PVPUtils-Compatibility").log(System.Logger.Level.WARNING,
                    "Cannot clean compatibility restart files", exception);
        }
    }

    private static void cleanup(Path root, Path configDirectory) throws IOException {
        Path base = configDirectory.toAbsolutePath().normalize().resolve("pvp-utils-compatibility-restart");
        Path resolved = root.toAbsolutePath().normalize();
        if (!resolved.startsWith(base) || !resolved.getParent().equals(base)) return;
        try {
            UUID.fromString(resolved.getFileName().toString());
        } catch (IllegalArgumentException exception) {
            return;
        }
        Files.deleteIfExists(resolved.resolve("launch.args"));
        Path file = resolved.resolve(HELPER_FILE);
        Files.deleteIfExists(file);
        for (Path directory = file.getParent(); directory != null && directory.startsWith(resolved); directory = directory.getParent()) {
            Files.deleteIfExists(directory);
        }
    }

    public record Plan(Path configDirectory, Path workingDirectory, Path root, List<String> queryCommand,
                       List<String> helperCommand) {
        public Plan {
            queryCommand = List.copyOf(queryCommand);
            helperCommand = List.copyOf(helperCommand);
        }
    }
}
