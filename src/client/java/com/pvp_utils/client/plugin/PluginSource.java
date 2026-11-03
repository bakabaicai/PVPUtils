package com.pvp_utils.client.plugin;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public record PluginSource(PluginManifest manifest, String code, String fingerprint) {
    public static PluginSource read(Path pluginRoot, Path directory) throws IOException {
        Path root = pluginRoot.toRealPath();
        Path realDirectory = directory.toRealPath();
        if (!realDirectory.startsWith(root) || realDirectory.equals(root)) {
            throw new IOException("Plugin directory outside root");
        }
        PluginManifest manifest = PluginManifest.read(realDirectory);
        Path entry = manifest.entry().toRealPath();
        if (!entry.startsWith(realDirectory) || !Files.isRegularFile(entry)) {
            throw new IOException("Plugin entry outside directory");
        }
        String code = readUtf8(entry, 1048576);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(manifest.toString().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(code.getBytes(StandardCharsets.UTF_8));
            return new PluginSource(manifest, code, HexFormat.of().formatHex(digest.digest()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String readUtf8(Path file, int limit) throws IOException {
        try (var input = Files.newInputStream(file)) {
            byte[] bytes = input.readNBytes(limit + 1);
            if (bytes.length > limit) throw new IOException("Plugin file exceeds size limit");
            return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
        }
    }
}
