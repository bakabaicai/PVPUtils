package com.pvp_utils.compatibility;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class CompatibilityRestartHelper {
    private CompatibilityRestartHelper() {
    }

    public static void main(String[] arguments) {
        Path root = null;
        Path config = null;
        boolean launched = false;
        boolean argumentFile = false;
        boolean validRoot = false;
        try (DataInputStream input = new DataInputStream(System.in)) {
            if (input.readInt() != 0x50565052) return;
            long parent = input.readLong();
            Path workingDirectory = Path.of(read(input)).toAbsolutePath().normalize();
            root = Path.of(read(input)).toAbsolutePath().normalize();
            config = Path.of(read(input)).toAbsolutePath().normalize();
            Path expectedBase = config.resolve("pvp-utils-compatibility-restart");
            if (!root.getParent().equals(expectedBase)) return;
            UUID.fromString(root.getFileName().toString());
            validRoot = true;
            int count = input.readInt();
            if (count < 2 || count > 8192) return;
            List<String> command = new ArrayList<>();
            for (int index = 0; index < count; index++) command.add(read(input));
            command.add(1, "-Dpvp-utils.compatibility.restart.cleanup=" + root);
            command.add(1, "--disable-@files");
            System.out.print("READY\n");
            System.out.flush();
            ProcessHandle previous = ProcessHandle.of(parent).orElse(null);
            if (previous != null) previous.onExit().get(120, TimeUnit.SECONDS);
            boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
            int length = command.stream().mapToInt(value -> value.length() * 2 + 3).sum();
            if (windows && length > 28000) {
                Path file = root.resolve("launch.args");
                StringBuilder contents = new StringBuilder("--disable-@files\n");
                for (int index = 1; index < command.size(); index++) {
                    contents.append('"').append(command.get(index).replace("\\", "\\\\").replace("\"", "\\\"")
                            .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t").replace("\f", "\\f"))
                            .append("\"\n");
                }
                Charset encoding = Charset.forName(System.getProperty("native.encoding", Charset.defaultCharset().name()));
                Files.writeString(file, contents, encoding);
                command = new ArrayList<>(List.of(command.getFirst(), "@" + file));
                argumentFile = true;
            }
            new ProcessBuilder(command).directory(workingDirectory.toFile())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD).start();
            launched = true;
        } catch (Exception exception) {
            if (config != null) {
                try {
                    Files.writeString(config.resolve("pvp-utils-compatibility-restart-error.txt"),
                            "Automatic restart failed: " + exception.getClass().getSimpleName()
                                    + "\nRestart the instance from its launcher. A granted one-time override remains pending.\n",
                            StandardCharsets.UTF_8);
                } catch (IOException ignored) {
                }
            }
        } finally {
            if (validRoot && root != null && config != null && root.getParent().equals(config.resolve("pvp-utils-compatibility-restart"))) {
                try {
                    if (!launched || !argumentFile) Files.deleteIfExists(root.resolve("launch.args"));
                    Path file = root.resolve("com/pvp_utils/compatibility/CompatibilityRestartHelper.class");
                    Files.deleteIfExists(file);
                    for (Path directory = file.getParent(); directory != null && directory.startsWith(root); directory = directory.getParent()) {
                        if (directory.equals(root) && launched && argumentFile) break;
                        Files.deleteIfExists(directory);
                    }
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static String read(DataInputStream input) throws IOException {
        int size = input.readInt();
        if (size < 0 || size > 16_777_216) throw new IOException("Invalid restart argument size");
        byte[] bytes = input.readNBytes(size);
        if (bytes.length != size) throw new IOException("Incomplete restart request");
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
