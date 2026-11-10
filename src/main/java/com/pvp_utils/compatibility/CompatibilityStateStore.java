package com.pvp_utils.compatibility;

import java.io.IOException;
import java.io.Reader;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.UnaryOperator;

public final class CompatibilityStateStore {
    private static final Object FILE_MONITOR = new Object();
    private final Path directory;
    private final Path stateFile;
    private final Path overrideFile;
    private final Path lockFile;

    public CompatibilityStateStore(Path directory) {
        this.directory = directory.toAbsolutePath().normalize();
        stateFile = this.directory.resolve("pvp-utils-compatibility.properties");
        overrideFile = this.directory.resolve("pvp-utils-compatibility-once.properties");
        lockFile = this.directory.resolve("pvp-utils-compatibility.lock");
    }

    public State load() throws IOException {
        return locked(this::readState);
    }

    public State update(UnaryOperator<State> change) throws IOException {
        return locked(() -> {
            State updated = change.apply(readState());
            StringBuilder contents = new StringBuilder("format=1\n");
            contents.append("fixNoticeAcknowledged=").append(updated.fixNoticeAcknowledged()).append('\n');
            for (String id : new TreeSet<>(updated.ignoredModIds())) {
                validateModId(id);
                contents.append("ignoredMod.").append(id).append("=true\n");
            }
            atomicWrite(stateFile, contents.toString());
            return updated;
        });
    }

    public void grantNextLaunch(long issuerPid) throws IOException {
        locked(() -> {
            atomicWrite(overrideFile, "format=1\nissuerPid=" + issuerPid + "\ntoken=" + UUID.randomUUID() + "\n");
            return null;
        });
    }

    public boolean consumeNextLaunch(long processPid) throws IOException {
        return locked(() -> {
            if (!Files.exists(overrideFile)) {
                return false;
            }
            Properties properties = readProperties(overrideFile);
            verifyFormat(properties, overrideFile);
            long issuer;
            try {
                issuer = Long.parseLong(properties.getProperty("issuerPid", ""));
                UUID.fromString(properties.getProperty("token", ""));
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid one-time compatibility override: " + overrideFile, exception);
            }
            if (issuer == processPid) {
                return false;
            }
            Files.delete(overrideFile);
            return true;
        });
    }

    public Path stateFile() {
        return stateFile;
    }

    public void writeReport(String contents) throws IOException {
        locked(() -> {
            atomicWrite(directory.resolve("pvp-utils-compatibility-report.txt"), contents);
            return null;
        });
    }

    private State readState() throws IOException {
        if (Files.notExists(stateFile)) {
            return State.defaults();
        }
        Properties properties = readProperties(stateFile);
        verifyFormat(properties, stateFile);
        boolean acknowledged = readBoolean(properties, "fixNoticeAcknowledged", false);
        Set<String> ignored = new HashSet<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("ignoredMod.")) {
                String id = key.substring("ignoredMod.".length());
                validateModId(id);
                if (readBoolean(properties, key, false)) {
                    ignored.add(id);
                }
            }
        }
        return new State(acknowledged, ignored);
    }

    private static Properties readProperties(Path path) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid compatibility state: " + path, exception);
        }
        return properties;
    }

    private static void verifyFormat(Properties properties, Path path) throws IOException {
        if (!"1".equals(properties.getProperty("format"))) {
            throw new IOException("Unsupported compatibility state format: " + path);
        }
    }

    private static boolean readBoolean(Properties properties, String key, boolean fallback) throws IOException {
        String value = properties.getProperty(key);
        if (value == null) {
            return fallback;
        }
        if (!value.equals("true") && !value.equals("false")) {
            throw new IOException("Invalid compatibility setting: " + key);
        }
        return Boolean.parseBoolean(value);
    }

    private static void validateModId(String id) throws IOException {
        if (!id.matches("[a-z][a-z0-9_-]{1,63}")) {
            throw new IOException("Invalid mod ID in compatibility state");
        }
    }

    private void atomicWrite(Path destination, String contents) throws IOException {
        Path temporary = Files.createTempFile(directory, "pvp-utils-compatibility-", ".tmp");
        try {
            Files.writeString(temporary, contents, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            move(temporary, destination, true);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void move(Path source, Path destination, boolean replace) throws IOException {
        try {
            if (replace) {
                Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
            }
        } catch (AtomicMoveNotSupportedException exception) {
            if (replace) {
                Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(source, destination);
            }
        }
    }

    private <T> T locked(IoOperation<T> operation) throws IOException {
        synchronized (FILE_MONITOR) {
            Files.createDirectories(directory);
            try (FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock ignored = channel.lock()) {
                return operation.run();
            }
        }
    }

    private interface IoOperation<T> {
        T run() throws IOException;
    }

    public record State(boolean fixNoticeAcknowledged, Set<String> ignoredModIds) {
        public State {
            ignoredModIds = Set.copyOf(ignoredModIds);
        }

        public static State defaults() {
            return new State(false, Set.of());
        }

        public State acknowledgeFixNotice() {
            return new State(true, ignoredModIds);
        }

        public State ignoreMod(String id) {
            Set<String> updated = new HashSet<>(ignoredModIds);
            updated.add(id);
            return new State(fixNoticeAcknowledged, updated);
        }
    }
}
